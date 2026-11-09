package io.github.ddayoung03.safetyleague.weather

import android.car.Car
import android.car.VehicleGear
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.util.Log
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.constant.area.WindowValue
import ai.pleos.playground.vehicle.constant.control.WiperState
import ai.pleos.playground.vehicle.constant.control.LightState
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.listener.WiperStateListener
import ai.pleos.playground.vehicle.listener.LightStateListener

/** Read-only, optional signals. No dependency on trip collection or scoring. */
class VehicleWeatherSource(private val context: Context, private val changed: (WeatherSignals) -> Unit) {
    private var car: Car? = null
    private var manager: CarPropertyManager? = null
    private var signals = WeatherSignals()
    private var active = false
    private var vehicle: Vehicle? = null
    private var generation = 0
    private val main = Handler(Looper.getMainLooper())
    private val pleos = Handler(pleosThread.looper)
    val problems = mutableListOf<String>()
    // Not in the compileSdk 34 car stubs; newer cars may expose it at runtime.
    private val wipersId = runCatching { VehiclePropertyIds::class.java.getField("WINDSHIELD_WIPERS_STATE").getInt(null) }.getOrNull()
    private val callback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            if (active) accept(value)
        }
        override fun onErrorEvent(propertyId: Int, areaId: Int) {
            if (active) {
                clear(propertyId)
                problem("${VehiclePropertyIds.toString(propertyId)}: unavailable")
            }
        }
    }

    fun start() {
        if (active) return
        active = true
        signals = WeatherSignals()
        problems.clear()
        changed(signals)
        startPleosWiper()
        runCatching {
            car = Car.createCar(context)
            val m = car?.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager
                ?: error("CarPropertyManager unavailable")
            manager = m
            val supported = m.propertyList.associateBy { it.propertyId }
            if (wipersId == null) problem("WINDSHIELD_WIPERS_STATE: runtime unsupported")
            val ids = listOfNotNull(wipersId, VehiclePropertyIds.ENV_OUTSIDE_TEMPERATURE,
                VehiclePropertyIds.HEADLIGHTS_STATE, VehiclePropertyIds.PERF_VEHICLE_SPEED, VehiclePropertyIds.GEAR_SELECTION)
            for (id in ids) {
                val name = VehiclePropertyIds.toString(id)
                val config = supported[id]
                if (config == null) { problem("$name: unsupported or permission denied"); continue }
                // Windshield area only; rear-window wiping must not imply front precipitation.
                val area = if (id == wipersId) config.areaIds.firstOrNull { it and 1 != 0 } else config.areaIds.firstOrNull() ?: 0
                if (area == null) { problem("$name: no front windshield area"); continue }
                runCatching {
                    if (!m.registerCallback(callback, id, CarPropertyManager.SENSOR_RATE_UI)) error("subscription rejected")
                    m.getProperty<Any>(id, area)?.let(::accept)
                }.onFailure { clear(id); problem("$name: ${it.javaClass.simpleName}") }
            }
        }.onFailure { problem("Vehicle weather: ${it.javaClass.simpleName}") }
    }

    private fun startPleosWiper() {
        val token = ++generation
        fun deliver(state: WiperState?) = main.post {
            if (active && generation == token) {
                signals = signals.copy(wipers = when (state) { WiperState.ON -> true; WiperState.OFF -> false; else -> null })
                changed(signals)
            }
        }
        fun failed(e: Exception) { main.post {
            if (active && generation == token) {
                problem("Pleos Wiper: ${e.javaClass.simpleName}")
                signals = signals.copy(wipers = null)
                changed(signals)
            }
        } }
        // Pleos 지원 여부 확인이 2초쯤 걸려서 메인 스레드에서 하면 앱을 켤 때마다 화면이 멈춘다.
        // vehicle은 이 스레드에서만 만지고 놓는다.
        pleos.post { runCatching {
            val v = Vehicle(context).also { vehicle = it; it.initialize() }
            startPleosLight(v, token)
            v.getWiper().checkWiperStateCapabilities({ config, supported ->
                main.post {
                    if (!active || generation != token) return@post
                    val area = supported.keys.firstOrNull { WindowValue.FRONT_WINDSHIELD in it.values &&
                        config.accessType in listOf(AccessType.READ, AccessType.READ_WRITE) }
                    if (area == null) problem("Pleos Wiper: front windshield unsupported")
                    else runCatching {
                        v.getWiper().registerWiperState(area, object : WiperStateListener {
                            override fun onWiperStateUpdated(state: WiperState?) { deliver(state) }
                            override fun onFailed(e: Exception) = failed(e)
                        })
                        v.getWiper().getCurrentWiperState(area, { deliver(it) }, ::failed)
                        Log.i("VehicleWeather", "Pleos front wiper subscribed")
                    }.onFailure { problem("Pleos Wiper: ${it.javaClass.simpleName}") }
                }
            }, ::failed)
        }.onFailure { e -> main.post { if (active && generation == token) problem("Pleos Wiper: ${e.javaClass.simpleName}") } } }
    }

    private fun startPleosLight(v: Vehicle, token: Int) {
        fun deliver(state: LightState?) { main.post {
            if (active && generation == token) {
                signals = signals.copy(headlights = state?.let { it == LightState.ON })
                changed(signals)
            }
        } }
        fun failed(e: Exception) { main.post {
            if (active && generation == token) {
                signals = signals.copy(headlights = null)
                changed(signals)
                problem("Pleos LowBeam: ${e.javaClass.simpleName}")
            }
        } }
        v.getLight().checkLowBeamLightStateCapabilities({ config, states ->
            main.post {
                if (!active || generation != token) return@post
                if (states.isEmpty() || config.accessType !in listOf(AccessType.READ, AccessType.READ_WRITE)) {
                    problem("Pleos LowBeam: unsupported")
                } else runCatching {
                    v.getLight().registerLowBeamLightState(object : LightStateListener {
                        override fun onLightStateUpdated(state: LightState?) = deliver(state)
                        override fun onFailed(e: Exception) = failed(e)
                    })
                    v.getLight().getCurrentLowBeamLightState(::deliver, ::failed)
                    Log.i("VehicleWeather", "Pleos low beam subscribed")
                }.onFailure { problem("Pleos LowBeam: ${it.javaClass.simpleName}") }
            }
        }, ::failed)
    }

    private fun accept(value: CarPropertyValue<*>) {
        val id = value.propertyId
        if (id == wipersId && value.areaId and 1 == 0) return
        if (value.status != CarPropertyValue.STATUS_AVAILABLE) { clear(id); return }
        val number = value.value as? Number
        signals = when (id) {
            wipersId -> signals.copy(wipers = when (number?.toInt()) { 1 -> false; 2 -> true; else -> null })
            VehiclePropertyIds.ENV_OUTSIDE_TEMPERATURE -> signals.copy(temperatureC = number?.toFloat()?.takeIf { it.isFinite() })
            VehiclePropertyIds.HEADLIGHTS_STATE -> signals.copy(headlights = when (number?.toInt()) { 0, 2 -> false; 1 -> true; else -> null })
            VehiclePropertyIds.PERF_VEHICLE_SPEED -> signals.copy(speedMps = number?.toFloat()?.takeIf { it.isFinite() })
            VehiclePropertyIds.GEAR_SELECTION -> signals.copy(gear = when (number?.toInt()) {
                VehicleGear.GEAR_PARK -> MotionGear.PARK
                VehicleGear.GEAR_NEUTRAL -> MotionGear.NEUTRAL
                VehicleGear.GEAR_DRIVE, VehicleGear.GEAR_REVERSE -> MotionGear.DRIVE
                else -> MotionGear.UNKNOWN
            })
            else -> signals
        }
        changed(signals)
    }

    private fun clear(id: Int) {
        signals = when (id) {
            wipersId -> signals.copy(wipers = null)
            VehiclePropertyIds.ENV_OUTSIDE_TEMPERATURE -> signals.copy(temperatureC = null)
            VehiclePropertyIds.HEADLIGHTS_STATE -> signals.copy(headlights = null)
            VehiclePropertyIds.PERF_VEHICLE_SPEED -> signals.copy(speedMps = null, gear = MotionGear.UNKNOWN)
            VehiclePropertyIds.GEAR_SELECTION -> signals.copy(gear = MotionGear.UNKNOWN)
            else -> signals
        }
        changed(signals)
    }

    private fun problem(message: String) {
        if (message !in problems) { problems += message; Log.i("VehicleWeather", message) }
    }

    fun stop() {
        active = false
        generation++
        pleos.post {
            runCatching { vehicle?.release() }
            vehicle = null
        }
        runCatching { manager?.unregisterCallback(callback) }
        runCatching { car?.disconnect() }
        manager = null
        car = null
    }

    private companion object {
        // 화면이 다시 만들어질 때마다 스레드가 늘지 않게 하나를 같이 쓴다.
        val pleosThread by lazy { HandlerThread("vehicle-weather").apply { start() } }
    }
}
