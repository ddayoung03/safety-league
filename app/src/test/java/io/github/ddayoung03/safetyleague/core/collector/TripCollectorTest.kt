package io.github.ddayoung03.safetyleague.core.collector

import io.github.ddayoung03.safetyleague.core.calculator.LeagueCalculator
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
    fun reversingIsNotHardBrakingOrAcceleration() {
        val c = collector()
        c.depart(1_000)
        c.onGearChanged(GearState.REVERSE, 2_000)
        c.onSpeedChanged(-2f, 3_000)
        c.onSpeedChanged(-2f, 3_600)
        c.onSpeedChanged(0f, 4_200)
        c.onOdometerChanged(105.0, 5_000)
        c.onGearChanged(GearState.PARK, 5_000)
        assertTrue(trips.single().events.isEmpty())
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
    fun hardBrakeCountsOncePerStop() {
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

    // ---- 점검 보고서(2026-11-05) 재현 값 ----

    @Test fun h1ScoreJustBelow70IsStoredAsTheWholeNumberThatIsShown() {
        // 10km, 차선 변경 3회 중 깜빡이 없음 2회, 연속 5일, 전비 없음 → 30 / 8.3 / 10 / 15 / 6.5 = 69.8
        val c = collector(streakDays = 5)
        c.depart(1_000, speed = 20f)
        c.onTurnSignalChanged(TurnSignalState(left = true), 2_000)
        c.steer(2_100L to 0f, 2_500L to 6f, 3_500L to 6f, 4_500L to 1f)
        c.onTurnSignalChanged(TurnSignalState(), 4_600)
        c.steer(20_000L to 0f, 20_500L to 6f, 21_500L to 6f, 22_500L to 1f)
        c.steer(40_000L to 0f, 40_500L to -6f, 41_500L to -6f, 42_500L to -1f)
        c.park(60_000, 110.0)
        val trip = trips.single().trip
        assertEquals(69.8, trip.detailScore!!.total, 0.001)
        assertEquals(70.0, trip.score, 0.001)  // 화면의 "70"과 같은 값
        assertEquals(1, LeagueCalculator.streakAt(listOf(trip), trip.date)) // 70점이므로 연속 기록이 이어진다
    }

    @Test fun h3EcoUsesTheTripAverageNotTheLastInstantValue() {
        // 트립 내내 7.0 km/kWh, 주차 직전에 3.0 한 번 → 트립 평균은 7.0에 가깝다 → 에코 20 (지금은 0)
        val c = collector()
        c.depart(1_000, speed = 10f)
        c.onEfficiencyChanged(7.0, 1_100)
        for (s in 1..1_000) c.onSpeedChanged(10f, 1_100L + s * 1_000L)
        c.onEfficiencyChanged(3.0, 1_001_200)
        c.onSpeedChanged(2f, 1_001_600)
        c.park(1_002_000, 110.0)
        assertEquals(20.0, trips.single().trip.detailScore!!.eco, 0.001)
    }

    @Test fun h3EnergyWeightedAverageAndInvalidValuesAreIgnored() {
        // 5km를 4.0, 5km를 8.0으로 달림 → 10km / (1.25 + 0.625)kWh = 5.33 → 비율 0.92 → 에코 6.0
        val c = collector()
        c.depart(1_000, speed = 10f)
        c.onEfficiencyChanged(4.0, 1_100)
        for (s in 1..500) c.onSpeedChanged(10f, 1_100L + s * 1_000L)
        c.onEfficiencyChanged(8.0, 501_100)
        c.onEfficiencyChanged(0.0, 501_200)        // 0 이하는 버린다
        c.onEfficiencyChanged(Double.NaN, 501_300) // 비정상 값도 버린다
        for (s in 501..1_000) c.onSpeedChanged(10f, 1_100L + s * 1_000L)
        c.park(1_005_000, 110.0)
        assertEquals(6.0, trips.single().trip.detailScore!!.eco, 0.001)
    }

    @Test fun h4DepartureFromStandstillIsNotInflated() {
        // 정차 0 → D → 0.6(t) → 1.3(t+250) → 2.0(t+500): 실제 2.8 m/s² (지금은 2.0/0.5 = 4.0으로 급가속)
        val c = collector()
        c.onOdometerChanged(100.0, 0)
        c.onSpeedChanged(0f, 500)
        c.onGearChanged(GearState.DRIVE, 1_000)
        c.onSpeedChanged(0.6f, 2_000)
        c.onSpeedChanged(1.3f, 2_250)
        c.onSpeedChanged(2.0f, 2_500)
        c.onSpeedChanged(2.7f, 2_750)
        c.onSpeedChanged(3.4f, 3_000)
        c.park(60_000, 110.0)
        assertEquals(0, trips.single().events.count { it.kind == "accel" })
    }

    @Test fun m1WarningOnBeforeDepartureIsCounted() {
        // 정차 중 경고 켬 → 출발(1.1초) → 61.1초에 해결: 60초 → -4 → 11 (지금은 15)
        val c = collector()
        c.onTelltaleChanged(true, 0)
        c.depart(1_000)
        c.onTelltaleChanged(false, 61_100)
        c.park(120_000, 110.0)
        assertEquals(11.0, trips.single().trip.detailScore!!.safety, 0.001)
    }

    @Test fun m1WarningResolvedBeforeDepartureIsNotCounted() {
        val c = collector()
        c.onTelltaleChanged(true, 0)
        c.onTelltaleChanged(false, 500)
        c.depart(1_000)
        c.park(120_000, 110.0)
        assertEquals(15.0, trips.single().trip.detailScore!!.safety, 0.001)
    }

    @Test fun m3RegenerativeDecelerationWithoutPedalIsAHardBrake() {
        // 원페달 회생제동 -5 m/s², 브레이크 페달은 밟지 않음
        val c = collector()
        c.depart(1_000, speed = 20f)
        var v = 20f
        for (i in 1..10) { v -= 1f; c.onSpeedChanged(v, 2_000L + i * 200) }
        c.park(60_000, 110.0)
        assertEquals(1, trips.single().events.count { it.kind == "brake" })
    }

    @Test fun m3NormalStopIsNotAHardBrake() {
        val c = collector()
        c.depart(1_000, speed = 20f)
        var v = 20f
        for (i in 1..20) { v -= 0.5f; c.onSpeedChanged(v, 2_000L + i * 200) } // -2.5 m/s²
        c.park(60_000, 110.0)
        assertEquals(0, trips.single().events.count { it.kind == "brake" })
    }

    // ---------- 점검 보고서 L1, 「트립 중 날짜 변경」 ----------

    @Test fun l1ShortTripDistanceIsRoundedDownSoItNeverReads3km() {
        val c = collector()
        c.depart(1_000, odometer = 100.0); c.park(60_000, 102.96)
        val r = trips.single()
        assertFalse(r.isEligible)
        assertEquals(2.9, r.trip.km, 0.0) // "3.0km"이면서 "3km 이상 달리면 기록돼요"가 같이 나오지 않게
        assertEquals(0.0, r.trip.score, 0.0)
    }

    @Test fun l1EligibleTripDistanceIsStillRounded() {
        val c = collector()
        c.depart(1_000, odometer = 100.0); c.park(60_000, 103.06)
        assertEquals(3.1, trips.single().trip.km, 0.0)
        assertTrue(trips.single().isEligible)
    }

    @Test fun tripKeepsTheDateItStartedOnAcrossMidnight() {
        // 서비스는 출발할 때(onTripStarted)만 날짜를 정한다. 자정을 넘겨 주차해도 출발한 날의 트립이다.
        lateinit var c: TripCollector
        c = TripCollector(date = "2026-11-06", streakDays = 12, leagueAvgEfficiency = 5.8,
            onTripStarted = { c.date = "2026-11-07" }) { trips += it }
        c.depart(1_000); c.park(60_000, 110.0)
        assertEquals("2026-11-07", trips.single().trip.date)
    }
}
