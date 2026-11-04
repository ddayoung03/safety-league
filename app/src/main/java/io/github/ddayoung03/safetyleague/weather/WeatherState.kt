package io.github.ddayoung03.safetyleague.weather

enum class Weather(val label: String) { SUN("맑음"), RAIN("비"), SNOW("눈"), NIGHT("밤") }
enum class MotionGear { PARK, NEUTRAL, DRIVE, UNKNOWN }

data class WeatherSignals(
    val wipers: Boolean? = null,
    val temperatureC: Float? = null,
    val headlights: Boolean? = null,
    val speedMps: Float? = null,
    val gear: MotionGear = MotionGear.UNKNOWN
)

fun inferWeather(signals: WeatherSignals, afterSunset: Boolean = false): Weather = when {
    signals.wipers == true -> if (signals.temperatureC?.let { it.isFinite() && it <= 0 } == true) Weather.SNOW else Weather.RAIN
    signals.headlights == true || afterSunset -> Weather.NIGHT
    else -> Weather.SUN
}

/** Speeds below 0.3 m/s are stopped sensor noise; movement overrides the gear, including reverse. */
fun isAmbientMotionAllowed(speedMps: Float?, gear: MotionGear): Boolean =
    if (speedMps != null && speedMps.isFinite()) kotlin.math.abs(speedMps) < .3f
    else gear == MotionGear.PARK || gear == MotionGear.NEUTRAL

fun shouldAnimateWeather(visible: Boolean, resumed: Boolean, still: Boolean, transitioning: Boolean, weather: Weather = Weather.RAIN) =
    visible && resumed && ((still && weather != Weather.SUN) || transitioning)

/** Shared across driving-view recreation so kilometre changes cannot reset the limit. */
class WeatherTransitionGate {
    private var lastMs: Long? = null
    fun allow(nowMs: Long): Boolean {
        if (lastMs?.let { nowMs - it < 60_000 } == true) return false
        lastMs = nowMs
        return true
    }
}
