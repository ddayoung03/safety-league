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
import android.content.Context
import android.util.Log
import io.github.ddayoung03.safetyleague.core.model.Point2D

/**
 * 실제 차량 신호: Pleos Vehicle SDK(기어·브레이크·조향·방향지시등·주행거리·벨트·경고등·도어·전비/연비),
 * AAOS CarPropertyManager(속도), Pleos FusedLocation(경로).
 * 실패한 도메인은 [problems]에 남겨 화면에 드러낸다 (조용히 삼키지 않는다).
 */
class PleosVehicleDataSource(private val context: Context) : VehicleDataSource {

    private val tag = "PleosVehicleData"
    private var vehicle: Vehicle? = null
    private var car: Car? = null
    private var location: FusedLocation? = null
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
        props.registerCallback(object : CarPropertyManager.CarPropertyEventCallback {
            override fun onChangeEvent(value: CarPropertyValue<*>) {
                (value.value as? Float)?.let { l.onSpeedChanged(it, now()) }
            }
            override fun onErrorEvent(propId: Int, zone: Int) {
                problems += "Speed: property error $propId"
            }
        }, VehiclePropertyIds.PERF_VEHICLE_SPEED, CarPropertyManager.SENSOR_RATE_FAST)
    }

    private fun startLocation(l: VehicleSignalListener) {
        try {
            val loc = FusedLocation(context).also { it.initialize() }
            if (!loc.isSupported()) { problems += "Location: FusedLocation not supported"; return }
            loc.registerFusedLocationCallback(object : FusedLocationListener {
                override fun onChanged(d: LocationData?) { d?.let { l.onLocationChanged(Point2D(it.longitude, it.latitude), it.timestampMillis) } }
                override fun onError(e: Exception) = fail("Location", e)
            })
            location = loc
        } catch (e: Exception) {
            fail("Location", e)
        }
    }

    override fun stopListening() {
        runCatching { vehicle?.release() }
        runCatching { location?.release() }
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
