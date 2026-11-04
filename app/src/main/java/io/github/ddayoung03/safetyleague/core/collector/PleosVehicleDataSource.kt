package io.github.ddayoung03.safetyleague.core.collector

import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.model.LocationData
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.telltale.Telltales
import ai.pleos.playground.vehicle.constant.area.DoorValue
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.area.TurnSignalValue
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.constant.telltale.TelltaleState
import ai.pleos.playground.vehicle.listener.BrakePedalStatusListener
import ai.pleos.playground.vehicle.listener.DistanceDrivenListener
import ai.pleos.playground.vehicle.listener.DoorPositionListener
import ai.pleos.playground.vehicle.listener.EvBatteryEfficiencyListener
import ai.pleos.playground.vehicle.listener.FuelEconomyListener
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import ai.pleos.playground.vehicle.listener.SeatBeltBuckledStateListener
import ai.pleos.playground.vehicle.listener.SeatOccupancyListener
import ai.pleos.playground.vehicle.listener.SteeringWheelAngleListener
import ai.pleos.playground.vehicle.listener.TelltaleInfoListener
import ai.pleos.playground.vehicle.listener.TurnSignalStateListener
import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import io.github.ddayoung03.safetyleague.core.model.Point2D
import io.github.ddayoung03.safetyleague.core.repository.LocationConsentStore

/**
 * 실제 차량 신호: Pleos Vehicle SDK(기어·브레이크·조향·방향지시등·주행거리·벨트·경고등·도어·전비/연비),
 * AAOS CarPropertyManager(속도), 경로는 FusedLocation → NaviHelper → LocationManager 순으로 대체([LocationFallback]).
 * 실패한 도메인은 [problems]에 남겨 화면에 드러낸다 (조용히 삼키지 않는다).
 */
class PleosVehicleDataSource(private val context: Context) : VehicleDataSource {

    private val tag = "PleosVehicleData"
    private var vehicle: Vehicle? = null
    private var car: Car? = null
    private var location: LocationFallback? = null
    private val main = Handler(Looper.getMainLooper())
    private var listener: VehicleSignalListener? = null
    private val seats = listOf(SeatValue.ROW_1_LEFT, SeatValue.ROW_1_RIGHT, SeatValue.ROW_2_LEFT, SeatValue.ROW_2_CENTER, SeatValue.ROW_2_RIGHT)
    private val occupied = mutableMapOf<Int, Boolean>()
    private val buckled = mutableMapOf<Int, Boolean>()
    private val doorsOpen = mutableMapOf<Int, Boolean>()

    /** 연결에 실패한 신호 목록 (예: "TurnSignal: ..."). 비어 있으면 모두 연결됨. */
    val problems = mutableListOf<String>()

    private fun now() = System.currentTimeMillis()
    private fun fail(domain: String, e: Exception) {
        Log.e(tag, "$domain failed", e)
        problems += "$domain: ${e.message}"
    }

    override fun startListening(listener: VehicleSignalListener) {
        this.listener = listener
        problems.clear()
        try {
            vehicle = Vehicle(context).also { it.initialize(); register(it, listener) }
        } catch (e: Exception) {
            fail("Vehicle SDK", e)
        }
        try {
            car = Car.createCar(context).also { registerSpeed(it, listener) }
        } catch (e: Exception) {
            fail("Speed", e)
        }
        startLocation(listener)
    }

    private fun register(v: Vehicle, l: VehicleSignalListener) {
        v.getGear().registerGearSelection(object : GearSelectionListener {
            override fun onSelected(gear: VehicleGear?) { gear?.let { l.onGearChanged(it.toGearState(), now()) } }
            override fun onFailed(e: Exception) = fail("Gear", e)
        })
        v.getBrake().registerBrakePedalStatus(object : BrakePedalStatusListener {
            override fun onBrakePedalStatusUpdated(pressed: Boolean?) { pressed?.let { l.onBrakePedalChanged(it, now()) } }
            override fun onFailed(e: Exception) = fail("Brake", e)
        })
        v.getSteeringWheel().registerSteeringWheelAngle(object : SteeringWheelAngleListener {
            override fun onSteeringWheelAngleUpdated(deg: Float?) { deg?.let { l.onSteeringWheelAngleChanged(it, now()) } }
            override fun onFailed(e: Exception) = fail("SteeringWheel", e)
        })
        v.getTurnSignal().registerTurnSignalState(object : TurnSignalStateListener {
            override fun onTurnSignalStateUpdated(value: TurnSignalValue?) =
                l.onTurnSignalChanged(TurnSignalState(left = value == TurnSignalValue.LEFT, right = value == TurnSignalValue.RIGHT), now())
            override fun onFailed(e: Exception) = fail("TurnSignal", e)
        })
        // 트립 시작 시점 값과 비교하므로 누적 주행거리(오도미터)를 쓴다
        v.getOdometer().registerDistanceDriven(object : DistanceDrivenListener {
            override fun onDistanceDrivenUpdated(km: Float?) { km?.let { l.onOdometerChanged(it.toDouble(), now()) } }
            override fun onFailed(e: Exception) = fail("Odometer", e)
        })
        seats.forEachIndexed { i, seat ->
            v.getSeat().registerSeatOccupancy(SeatArea(seat), object : SeatOccupancyListener {
                override fun onSeatOccupancyUpdated(on: Boolean?) { occupied[i] = on == true; publishBelts(l) }
                override fun onFailed(e: Exception) = fail("SeatOccupancy", e)
            })
            v.getSeat().registerSeatBeltBuckledState(SeatArea(seat), object : SeatBeltBuckledStateListener {
                override fun onSeatBeltBuckledStateUpdated(on: Boolean?) { buckled[i] = on == true; publishBelts(l) }
                override fun onFailed(e: Exception) = fail("SeatBelt", e)
            })
        }
        v.getSafety().registerTelltales(object : TelltaleInfoListener {
            override fun onTelltaleInfoUpdated(t: Telltales?) =
                l.onTelltaleChanged(t?.telltales.orEmpty().any { it.state != TelltaleState.OFF }, now())
            override fun onFailed(e: Exception) = fail("Telltales", e)
        })
        listOf(DoorValue.ROW_1_LEFT, DoorValue.ROW_1_RIGHT, DoorValue.ROW_2_LEFT, DoorValue.ROW_2_RIGHT, DoorValue.REAR)
            .forEachIndexed { i, door ->
                v.getDoor().registerDoorPosition(DoorArea(door), object : DoorPositionListener {
                    override fun onPositionUpdated(pos: Int?) {
                        doorsOpen[i] = (pos ?: 0) > 0
                        l.onDoorChanged(doorsOpen.values.any { it }, now())
                    }
                    override fun onFailed(e: Exception) = fail("Door", e)
                })
            }
        // EV는 전비, 내연기관은 연비. 지원하지 않는 쪽은 onFailed로 오지만 문제로 치지 않는다.
        v.getEvBattery().registerCurrentEvEfficiency(object : EvBatteryEfficiencyListener {
            override fun onEvBatteryEfficiencyUpdated(value: Float?) { value?.let { l.onEfficiencyChanged(it.toDouble(), now()) } }
            override fun onFailed(e: Exception) { Log.i(tag, "EV efficiency unavailable: ${e.message}") }
        })
        v.getFuel().registerCurrentFuelEconomy(object : FuelEconomyListener {
            override fun onFuelEconomyUpdated(value: Float?) { value?.let { l.onEfficiencyChanged(it.toDouble(), now()) } }
            override fun onFailed(e: Exception) { Log.i(tag, "Fuel economy unavailable: ${e.message}") }
        })
    }

    /** 사람이 앉은 좌석의 벨트만 본다. 탑승 정보가 아직 없으면 운전석만. */
    private fun publishBelts(l: VehicleSignalListener) {
        val seated = seats.indices.filter { occupied[it] ?: (it == 0) }
        l.onSeatBeltChanged(seated.associateWith { buckled[it] ?: false }, now())
    }

    private fun registerSpeed(car: Car, l: VehicleSignalListener) {
        val props = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
        val connected = props.registerCallback(object : CarPropertyManager.CarPropertyEventCallback {
            override fun onChangeEvent(value: CarPropertyValue<*>) {
                (value.value as? Float)?.let { l.onSpeedChanged(it, now()) }
            }
            override fun onErrorEvent(propId: Int, zone: Int) {
                problems += "Speed: property error $propId"
            }
        }, VehiclePropertyIds.PERF_VEHICLE_SPEED, CarPropertyManager.SENSOR_RATE_FAST)
        if (!connected) problems += "Speed: 구독 실패 (CAR_SPEED 권한 확인)"
    }

    /** 위치 동의가 바뀌었으면 위치 수집을 켜거나 끈다. 서비스가 onStartCommand마다 부른다. */
    fun syncLocation() {
        val l = listener ?: return
        val wanted = LocationPolicy.sourceOrder(LocationConsentStore(context).load()).isNotEmpty()
        // 켜진 출처가 없으면(권한을 막 받았거나 모두 실패) 처음부터 다시 시도한다
        if (wanted && location?.activeName == null) { runCatching { location?.stop() }; location = null; startLocation(l) }
        else if (!wanted && location != null) { runCatching { location?.stop() }; location = null }
    }

    private fun startLocation(l: VehicleSignalListener) {
        // 동의하지 않았으면 위치 출처를 하나도 켜지 않는다 (점수·리그는 위치 없이 동작)
        val order = LocationPolicy.sourceOrder(LocationConsentStore(context).load())
        if (order.isEmpty()) { Log.i(tag, "Location: 경로 기록 동의 안 함"); return }
        val sources = order.map {
            when (it) {
                LocationProvider.FUSED -> fusedSource()
                LocationProvider.NAVI -> naviSource()
                LocationProvider.OS -> osSource()
            }
        }
        val chain = LocationFallback(sources, problems) { p ->
            if (p.x != 0.0 || p.y != 0.0) l.onLocationChanged(p, now())
        }
        location = chain
        chain.start()
        Log.i(tag, "Location source: ${chain.activeName}")
    }

    /** 시작 후 [ms] 동안 위치가 한 번도 안 오면 실패로 보고 다음 출처로 넘긴다. */
    private fun watchdog(ms: Long, onFailed: (String) -> Unit): () -> Unit {
        var got = false
        val r = Runnable { if (!got) onFailed("${ms / 1000}초 동안 위치 없음") }
        main.postDelayed(r, ms)
        return { got = true; main.removeCallbacks(r) }
    }

    private fun fusedSource() = object : LocationSource {
        override val name = "FusedLocation"
        private var loc: FusedLocation? = null
        private var cancel: (() -> Unit)? = null
        override fun start(onPoint: (Point2D) -> Unit, onFailed: (String) -> Unit): Boolean {
            val f = FusedLocation(context).also { it.initialize() }
            if (!f.isSupported()) { runCatching { f.release() }; return false }
            loc = f
            val seen = watchdog(15_000, onFailed).also { cancel = it }
            f.registerFusedLocationCallback(object : FusedLocationListener {
                override fun onChanged(d: LocationData?) { d?.let { seen(); onPoint(Point2D(it.longitude, it.latitude)) } }
                override fun onError(e: Exception) = onFailed(e.message ?: "error")
            })
            return true
        }
        override fun stop() { cancel?.invoke(); runCatching { loc?.release() }; loc = null }
    }

    /** NaviHelper는 요청할 때만 현재 위치를 돌려주므로 2초마다 묻는다. */
    private fun naviSource() = object : LocationSource {
        override val name = "NaviHelper"
        private var navi: NaviHelper? = null
        private var cancel: (() -> Unit)? = null
        private val poll = object : Runnable {
            override fun run() { runCatching { navi?.getCurrentLocationInfo() }; main.postDelayed(this, 2_000) }
        }
        override fun start(onPoint: (Point2D) -> Unit, onFailed: (String) -> Unit): Boolean {
            val n = NaviHelper(context)
            navi = n
            val seen = watchdog(15_000, onFailed).also { cancel = it }
            n.addListener(object : NaviHelperEventListener {
                // 응답이 binder 스레드로 오므로 다른 신호처럼 메인 스레드에서 넘긴다
                override fun onCurrentLocationInfo(info: CurrentLocationInfo) {
                    main.post { seen(); onPoint(Point2D(info.longitude, info.latitude)) }
                }
            })
            n.initialize({ main.post(poll) }, { e -> onFailed(e.message ?: "initialize failed") })
            return true
        }
        override fun stop() {
            cancel?.invoke(); main.removeCallbacks(poll)
            runCatching { navi?.release() }; navi = null
        }
    }

    /** 마지막 대체: 안드로이드 기본 위치 (위치 권한 필요). */
    private fun osSource() = object : LocationSource {
        override val name = "LocationManager"
        private val lm = context.getSystemService(LocationManager::class.java)
        private var cb: LocationListener? = null
        override fun start(onPoint: (Point2D) -> Unit, onFailed: (String) -> Unit): Boolean {
            if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return false
            val provider = listOf(LocationManager.FUSED_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                .firstOrNull { it in lm.allProviders && lm.isProviderEnabled(it) } ?: return false
            val c = LocationListener { onPoint(Point2D(it.longitude, it.latitude)) }
            lm.requestLocationUpdates(provider, 1_000L, 0f, c, Looper.getMainLooper())
            cb = c
            return true
        }
        override fun stop() { cb?.let { lm.removeUpdates(it) }; cb = null }
    }

    override fun stopListening() {
        runCatching { vehicle?.release() }
        runCatching { location?.stop() }
        runCatching { car?.disconnect() }
        vehicle = null; location = null; car = null; listener = null
    }

    private fun VehicleGear.toGearState() = when (this) {
        VehicleGear.GEAR_PARK -> GearState.PARK
        VehicleGear.GEAR_REVERSE -> GearState.REVERSE
        VehicleGear.GEAR_NEUTRAL, VehicleGear.GEAR_UNKNOWN -> GearState.NEUTRAL
        else -> GearState.DRIVE
    }
}
