package io.github.ddayoung03.safetyleague.weather

import org.junit.Assert.*
import org.junit.Test

class WeatherStateTest {
    @Test fun missingSignalsDefaultToSun() = assertEquals(Weather.SUN, inferWeather(WeatherSignals()))
    @Test fun wipersMeanRain() = assertEquals(Weather.RAIN, inferWeather(WeatherSignals(wipers = true)))
    @Test fun freezingWipersMeanSnow() {
        for (t in listOf(-12f, 0f)) assertEquals(Weather.SNOW, inferWeather(WeatherSignals(wipers = true, temperatureC = t)))
        assertEquals(Weather.RAIN, inferWeather(WeatherSignals(wipers = true, temperatureC = .1f)))
    }
    @Test fun coldWithoutWipersIsNotSnow() = assertEquals(Weather.SUN, inferWeather(WeatherSignals(temperatureC = -10f)))
    @Test fun lightsOrNightFallbackMeanNight() {
        assertEquals(Weather.NIGHT, inferWeather(WeatherSignals(headlights = true)))
        assertEquals(Weather.NIGHT, inferWeather(WeatherSignals(), afterSunset = true))
    }
    @Test fun precipitationWinsOverNight() {
        assertEquals(Weather.RAIN, inferWeather(WeatherSignals(wipers = true, headlights = true), true))
        assertEquals(Weather.SNOW, inferWeather(WeatherSignals(wipers = true, temperatureC = 0f), true))
    }
    @Test fun invalidTemperatureCannotCreateSnow() {
        for (t in listOf(Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY))
            assertEquals(Weather.RAIN, inferWeather(WeatherSignals(wipers = true, temperatureC = t)))
    }
    @Test fun speedOverridesNeutralAndPark() {
        for (g in listOf(MotionGear.PARK, MotionGear.NEUTRAL, MotionGear.DRIVE, MotionGear.UNKNOWN)) {
            assertFalse(isAmbientMotionAllowed(.3f, g))
            assertFalse(isAmbientMotionAllowed(-1f, g))
            assertTrue(isAmbientMotionAllowed(0f, g))
        }
    }
    @Test fun stoppedSensorNoiseAllowsAmbientMotionBelowThreshold() {
        for (gear in MotionGear.entries) {
            for (speed in listOf(0f, .01f, -.01f, .299f, -.299f)) {
                assertTrue("speed=$speed gear=$gear", isAmbientMotionAllowed(speed, gear))
            }
            for (speed in listOf(.3f, -.3f, .301f, -.301f, 1f, -1f)) {
                assertFalse("speed=$speed gear=$gear", isAmbientMotionAllowed(speed, gear))
            }
        }
    }
    @Test fun unknownSpeedOnlyAllowsKnownParkOrNeutral() {
        for (s in listOf(null, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertTrue(isAmbientMotionAllowed(s, MotionGear.PARK))
            assertTrue(isAmbientMotionAllowed(s, MotionGear.NEUTRAL))
            assertFalse(isAmbientMotionAllowed(s, MotionGear.DRIVE))
            assertFalse(isAmbientMotionAllowed(s, MotionGear.UNKNOWN))
        }
    }
    @Test fun transitionsAreLimitedToOnePerMinute() {
        val gate = WeatherTransitionGate()
        assertTrue(gate.allow(1000))
        assertFalse(gate.allow(1001))
        assertFalse(gate.allow(60999))
        assertTrue(gate.allow(61000))
    }
    @Test fun sunTransitionsAnimateButParkedSunDoesNotLoop() {
        assertTrue(shouldAnimateWeather(true, true, false, true, Weather.SUN))
        assertFalse(shouldAnimateWeather(true, true, true, false, Weather.SUN))
        assertFalse(shouldAnimateWeather(false, true, false, true, Weather.SUN))
    }
    @Test fun parkedAnimationNeedsVisibleResumedView() {
        assertTrue(shouldAnimateWeather(true, true, true, false))
        assertFalse(shouldAnimateWeather(true, false, true, true))
        assertFalse(shouldAnimateWeather(false, true, true, true))
        assertFalse(shouldAnimateWeather(true, true, false, false))
        assertTrue(shouldAnimateWeather(true, true, false, true))
    }
    @Test fun allWeatherStopsWhileDrivingExceptBoundedTransition() {
        for (weather in Weather.entries) {
            assertFalse(shouldAnimateWeather(true, true, false, false, weather))
            assertTrue(shouldAnimateWeather(true, true, false, true, weather))
            assertEquals(weather != Weather.SUN, shouldAnimateWeather(true, true, true, false, weather))
            assertFalse(shouldAnimateWeather(true, false, true, true, weather))
            assertFalse(shouldAnimateWeather(false, true, true, true, weather))
        }
    }
}
