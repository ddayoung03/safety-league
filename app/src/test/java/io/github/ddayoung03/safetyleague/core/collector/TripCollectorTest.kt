package io.github.ddayoung03.safetyleague.core.collector

import io.github.ddayoung03.safetyleague.core.model.CompletedTrip
import io.github.ddayoung03.safetyleague.core.model.Point2D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripCollectorTest {

    private val trips = mutableListOf<CompletedTrip>()
    private fun collector(streakDays: Int = 12) =
        TripCollector(date = "2026-11-07", streakDays = streakDays, leagueAvgEfficiency = 5.8) { trips += it }

    /** D단으로 넣고 실제로 출발시킨다 (트립은 움직여야 시작된다). */
    private fun TripCollector.depart(t: Long, speed: Float = 10f, odometer: Double = 100.0) {
        onOdometerChanged(odometer, t)
        onGearChanged(GearState.DRIVE, t)
        onSpeedChanged(speed, t + 100)
    }

    private fun TripCollector.park(t: Long, odometer: Double) {
        onSpeedChanged(0f, t - 100)
        onOdometerChanged(odometer, t)
        onGearChanged(GearState.PARK, t)
    }

    /** 일정 속도에서 조향각을 시간 순서대로 넣는다. */
    private fun TripCollector.steer(vararg pts: Pair<Long, Float>) = pts.forEach { (t, a) -> onSteeringWheelAngleChanged(a, t) }

    @Test
    fun tripLifecycleOnGearAndMovement() {
        val c = collector()
        c.onSeatBeltChanged(mapOf(0 to true, 1 to true), 0)
        c.depart(1_000)
        assertTrue(c.isTracking)
        c.park(60_000, 118.4)
        assertFalse(c.isTracking)
        val r = trips.single()
        assertEquals(18.4, r.trip.km, 0.001)
        assertTrue(r.isEligible)
    }

    @Test
    fun secondTripDistanceStartsFromItsOwnOdometer() {
        val c = collector()
        c.depart(1_000, odometer = 100.0); c.park(60_000, 118.0)
        c.depart(100_000, odometer = 118.0); c.park(200_000, 125.0)
        assertEquals(7.0, trips[1].trip.km, 0.001)
    }

    @Test
    fun odometerKnownBeforeDriveIsUsedAsStart() {
        val c = collector()
        c.onOdometerChanged(50.0, 0)          // 주차 중에 이미 들어온 값
        c.onGearChanged(GearState.DRIVE, 1_000)
        c.onSpeedChanged(10f, 1_100)
        c.park(60_000, 54.0)
        assertEquals(4.0, trips.single().trip.km, 0.001)
    }

    @Test
    fun shiftingThroughReverseWithoutMovingIsNotATrip() {
        val c = collector()
        c.onGearChanged(GearState.REVERSE, 1_000)
        c.onGearChanged(GearState.PARK, 2_000)
        assertTrue(trips.isEmpty())
        assertFalse(c.isTracking)
    }

    @Test
    fun oneSustainedHardAccelerationCountsOnce() {
        val c = collector()
        c.depart(1_000, speed = 1f)
        // 5Hz로 3초 동안 4 m/s² (샘플마다 +0.8 m/s)
        var v = 1f
        for (i in 1..15) { v += 0.8f; c.onSpeedChanged(v, 1_100L + i * 200) }
        c.onSpeedChanged(v, 5_000) // 가속 끝
        c.park(60_000, 110.0)
        assertEquals(1, trips.single().events.count { it.kind == "accel" })
    }

    @Test
    fun hardBrakeNeedsThePedalAndCountsOncePerStop() {
        val c = collector()
        c.depart(1_000, speed = 20f)
        c.onBrakePedalChanged(true, 2_000)
        var v = 20f
        for (i in 1..10) { v -= 1f; c.onSpeedChanged(v, 2_000L + i * 200) } // -5 m/s²
        c.onBrakePedalChanged(false, 5_000)
        c.park(60_000, 110.0)
        assertEquals(1, trips.single().events.count { it.kind == "brake" })
    }

    @Test
    fun highwayLaneChangeWithoutSignalIsDetected() {
        val c = collector()
        c.depart(1_000, speed = 27.8f) // 100 km/h
        c.steer(2_000L to 0f, 2_500L to 6f, 3_500L to 6f, 4_500L to 1f)
        c.park(60_000, 110.0)
        assertEquals(1, trips.single().events.count { it.kind == "lane" })
        assertEquals(1, c.laneChanges)
    }

    @Test
    fun intersectionTurnIsNotALaneChange() {
        val c = collector()
        c.depart(1_000, speed = 9.7f) // 35 km/h
        c.steer(2_000L to 0f, 2_500L to 90f, 6_000L to 120f, 10_000L to 2f)
        c.park(60_000, 110.0)
        assertEquals(0, trips.single().events.count { it.kind == "lane" })
        assertEquals(0, c.laneChanges)
    }

    @Test
    fun signalThatSwitchedOffJustBeforeSteeringCountsAsIndicated() {
        val c = collector()
        c.depart(1_000, speed = 27.8f)
        c.onTurnSignalChanged(TurnSignalState(left = true), 2_000)
        c.onTurnSignalChanged(TurnSignalState(), 4_000) // 3번 깜빡이고 자동으로 꺼짐
        c.steer(5_000L to 0f, 5_500L to -6f, 6_500L to -6f, 7_500L to -1f)
        c.park(60_000, 110.0)
        assertEquals(0, trips.single().events.count { it.kind == "lane" })
        assertEquals(1, c.laneChanges)
    }

    @Test
    fun warningStillOnAtParkingIsCounted() {
        val c = collector()
        c.depart(1_000)
        c.onTelltaleChanged(true, 10_000)
        c.park(75_000, 110.0)
        assertEquals(11.0, trips.single().trip.detailScore!!.safety, 0.001) // 65초 → -4
    }

    @Test
    fun doorOpenedWhileMovingCostsSafety() {
        val c = collector()
        c.depart(1_000, speed = 5f)
        c.onDoorChanged(true, 2_000)
        c.onDoorChanged(false, 3_000)
        c.park(60_000, 110.0)
        assertEquals(12.0, trips.single().trip.detailScore!!.safety, 0.001)
    }

    @Test
    fun seatBeltsAreCheckedAtDeparture() {
        val c = collector()
        c.onSeatBeltChanged(mapOf(0 to true, 1 to false), 0)
        c.depart(1_000); c.park(60_000, 110.0)
        assertEquals(7.0, trips.single().trip.detailScore!!.safety, 0.001)
    }

    @Test
    fun shortTripIsExcluded() {
        val c = collector()
        c.depart(1_000); c.park(60_000, 102.5)
        val r = trips.single()
        assertFalse(r.isEligible)
        assertEquals(0.0, r.trip.score, 0.001)
    }

    @Test
    fun lowDayEventsComeFromSensorsAndCarRoutePositions() {
        val c = collector(streakDays = 13)
        c.depart(1_000, speed = 1f, odometer = 0.0)
        // 급가속 1
        var v = 1f
        for (i in 1..5) { v += 1f; c.onSpeedChanged(v, 1_100L + i * 200); c.onLocationChanged(Point2D(i.toDouble(), 0.0), 1_100L + i * 200) }
        c.onSpeedChanged(27.8f, 10_000)
        // 깜빡이 없는 차선 변경 4
        for (k in 0 until 4) {
            val t = 20_000L + k * 20_000
            c.onLocationChanged(Point2D(10.0 + k, 5.0), t)
            c.steer(t to 0f, t + 500 to 6f, t + 1_500 to 6f, t + 2_500 to 1f)
        }
        // 급제동 2
        for (k in 0 until 2) {
            val t = 110_000L + k * 30_000
            c.onBrakePedalChanged(true, t)
            var s = 27.8f
            for (i in 1..5) { s -= 1f; c.onSpeedChanged(s, t + i * 200) }
            c.onBrakePedalChanged(false, t + 2_000)
            c.onSpeedChanged(27.8f, t + 10_000)
        }
        c.park(200_000, 22.1)
        val r = trips.single()
        assertEquals(4, r.events.count { it.kind == "lane" })
        assertEquals(2, r.events.count { it.kind == "brake" })
        assertEquals(1, r.events.count { it.kind == "accel" })
        assertTrue(r.route.isNotEmpty())
        for (i in 1 until r.events.size) assertTrue(r.events[i].t >= r.events[i - 1].t)
    }

    @Test
    fun jitteryAccelerationFromRealSignalsCountsOnce() {
        // 에뮬레이터 실측: 10Hz로 들어오는 속도가 계단처럼 올라 표본 사이 가속도가 기준 위아래로 흔들린다
        val c = collector()
        c.depart(1_000, speed = 1f)
        var v = 1f
        for (i in 1..30) { v += if (i % 2 == 0) 0.45f else 0.28f; c.onSpeedChanged(v, 1_100L + i * 100) }
        c.park(60_000, 110.0)
        assertEquals(1, trips.single().events.count { it.kind == "accel" })
    }

    @Test
    fun distanceFallsBackToSpeedWhenOdometerDoesNotMove() {
        // 에뮬레이터 실측: 주행해도 오도미터가 그대로다. 그때는 속도를 시간으로 적분한다.
        val c = collector()
        c.depart(1_000, speed = 10f, odometer = 500.0)
        for (s in 1..400) c.onSpeedChanged(10f, 1_100L + s * 1_000L)
        c.onOdometerChanged(500.0, 402_000)
        c.onGearChanged(GearState.PARK, 402_000)
        assertEquals(4.0, trips.single().trip.km, 0.1)
    }

    @Test
    fun noTripBeforeAnyDrive() {
        val c = collector()
        c.onGearChanged(GearState.PARK, 0)
        assertNull(trips.firstOrNull())
        assertNotNull(c)
    }

    @Test fun efficiencyBaselineChangesOnlyOnNextTrip() {
        val c = collector()
        c.depart(1_000)
        c.onEfficiencyChanged(5.8, 2_000)
        c.leagueAvgEfficiency = 12.0
        c.park(60_000, 110.0)
        assertEquals(10.0, trips.last().trip.detailScore!!.eco, 0.001)
        c.depart(70_000, odometer = 110.0)
        c.onEfficiencyChanged(12.0, 71_000)
        c.park(120_000, 120.0)
        assertEquals(10.0, trips.last().trip.detailScore!!.eco, 0.001)
    }
}
