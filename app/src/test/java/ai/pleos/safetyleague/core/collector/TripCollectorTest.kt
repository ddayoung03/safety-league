package ai.pleos.safetyleague.core.collector

import ai.pleos.safetyleague.core.model.CompletedTrip
import ai.pleos.safetyleague.core.model.Point2D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripCollectorTest {

    @Test
    fun testTripLifecycleOnGearChange() {
        var completedTrip: CompletedTrip? = null
        val collector = TripCollector(
            date = "2026-11-07",
            streakDays = 12,
            efficiencyRatio = 1.0,
            onTripCompleted = { completedTrip = it }
        )

        assertFalse("초기에는 트립 기록 중이 아니어야 함", collector.isTracking)

        // 전 좌석 안전벨트 체결
        collector.onSeatBeltChanged(mapOf(0 to true, 1 to true), 1000L)

        // P단 -> D단 변경: 트립 시작!
        collector.onGearChanged(GearState.DRIVE, 1000L)
        assertTrue("D단 진입 시 트립 기록이 시작되어야 함", collector.isTracking)

        // 주행 거리 18.4km 주행 시뮬레이션
        collector.onOdometerChanged(100.0, 1000L)
        collector.onOdometerChanged(118.4, 2000L)

        // D단 -> P단 변경: 트립 종료 및 결과 카드 트리거!
        collector.onGearChanged(GearState.PARK, 2000L)
        assertFalse("P단 복귀 시 트립 기록이 종료되어야 함", collector.isTracking)

        assertNotNull("트립 완료 콜백이 호출되어야 함", completedTrip)
        val result = completedTrip!!
        assertEquals("2026-11-07", result.trip.date)
        assertEquals(18.4, result.trip.km, 0.001)
        assertTrue("18.4km는 3km 이상이므로 유효 트립이어야 함", result.isEligible)
        assertTrue("기본 안전 주행 시 70점 이상이어야 함", result.trip.score >= 70.0)
    }

    @Test
    fun testHardAccelerationAndBrakeDetection() {
        var completedTrip: CompletedTrip? = null
        val collector = TripCollector(
            date = "2026-11-07",
            streakDays = 12,
            efficiencyRatio = 1.0,
            onTripCompleted = { completedTrip = it }
        )

        collector.onSeatBeltChanged(mapOf(0 to true), 1000L)
        collector.onGearChanged(GearState.DRIVE, 1000L)
        collector.onOdometerChanged(0.0, 1000L)

        // 1초 동안 속도 5 m/s -> 10 m/s 급가속 (가속도 = +5.0 m/s² > +3.0)
        collector.onSpeedChanged(5f, 1000L)
        collector.onSpeedChanged(10f, 2000L)

        // 브레이크 밟고 1초 동안 속도 10 m/s -> 5 m/s 급감속 (감속도 = -5.0 m/s² < -3.5)
        collector.onBrakePedalChanged(true, 3000L)
        collector.onSpeedChanged(10f, 3000L)
        collector.onSpeedChanged(5f, 4000L)

        collector.onOdometerChanged(10.0, 5000L)
        collector.onGearChanged(GearState.PARK, 5000L)

        val result = completedTrip!!
        assertEquals(1, collector.hardAccels)
        assertEquals(1, collector.hardBrakes)
        assertEquals(2, result.events.size)
        assertEquals("accel", result.events[0].kind)
        assertEquals("brake", result.events[1].kind)
    }

    @Test
    fun testUnindicatedLaneChangeDetection() {
        var completedTrip: CompletedTrip? = null
        val collector = TripCollector(
            date = "2026-11-07",
            streakDays = 12,
            onTripCompleted = { completedTrip = it }
        )

        collector.onSeatBeltChanged(mapOf(0 to true), 1000L)
        collector.onGearChanged(GearState.DRIVE, 1000L)
        collector.onOdometerChanged(0.0, 1000L)
        collector.onSpeedChanged(15f, 1000L) // 54 km/h

        // 케이스 1: 깜빡이 없이 급격한 조향 (0도 -> 20도 -> 0도)
        collector.onTurnSignalChanged(TurnSignalState(left = false, right = false), 2000L)
        collector.onSteeringWheelAngleChanged(0f, 2000L)
        collector.onSteeringWheelAngleChanged(20f, 2500L)
        collector.onSteeringWheelAngleChanged(0f, 3000L)

        // 케이스 2: 좌측 깜빡이 켜고 차선 변경 (0도 -> -20도 -> 0도) -> 정상 주행으로 감점 없어야 함
        collector.onTurnSignalChanged(TurnSignalState(left = true, right = false), 4000L)
        collector.onSteeringWheelAngleChanged(0f, 4000L)
        collector.onSteeringWheelAngleChanged(-20f, 4500L)
        collector.onSteeringWheelAngleChanged(0f, 5000L)

        collector.onOdometerChanged(10.0, 6000L)
        collector.onGearChanged(GearState.PARK, 6000L)

        val result = completedTrip!!
        assertEquals("깜빡이 없는 차선 변경은 정확히 1회만 카운트되어야 함", 1, collector.unindicatedLaneChanges)
        val laneEvents = result.events.filter { it.kind == "lane" }
        assertEquals(1, laneEvents.size)
    }

    @Test
    fun testSeatBeltSafetyCheckAtDeparture() {
        // 케이스 A: 미착용 좌석이 있는 상태에서 D단 출발
        var tripA: CompletedTrip? = null
        val collectorA = TripCollector(
            date = "2026-11-07",
            onTripCompleted = { tripA = it }
        )
        collectorA.onSeatBeltChanged(mapOf(0 to true, 1 to false), 1000L) // 동승석 미착용
        collectorA.onGearChanged(GearState.DRIVE, 1000L)
        collectorA.onOdometerChanged(0.0, 1000L)
        collectorA.onOdometerChanged(10.0, 2000L)
        collectorA.onGearChanged(GearState.PARK, 2000L)

        assertFalse(collectorA.allBeltsFastenedBeforeDrive)
        assertEquals(7.0, tripA!!.trip.detailScore!!.safety, 0.001) // 15 - 8 = 7점

        // 케이스 B: 전 좌석 착용 완료 후 D단 출발
        var tripB: CompletedTrip? = null
        val collectorB = TripCollector(
            date = "2026-11-07",
            onTripCompleted = { tripB = it }
        )
        collectorB.onSeatBeltChanged(mapOf(0 to true, 1 to true), 1000L) // 전원 착용
        collectorB.onGearChanged(GearState.DRIVE, 1000L)
        collectorB.onOdometerChanged(0.0, 1000L)
        collectorB.onOdometerChanged(10.0, 2000L)
        collectorB.onGearChanged(GearState.PARK, 2000L)

        assertTrue(collectorB.allBeltsFastenedBeforeDrive)
        assertEquals(15.0, tripB!!.trip.detailScore!!.safety, 0.001) // 15점 만점
    }

    @Test
    fun testShortTripExclusion() {
        var completedTrip: CompletedTrip? = null
        val collector = TripCollector(
            date = "2026-11-07",
            onTripCompleted = { completedTrip = it }
        )

        collector.onSeatBeltChanged(mapOf(0 to true), 1000L)
        collector.onGearChanged(GearState.DRIVE, 1000L)
        collector.onOdometerChanged(100.0, 1000L)
        collector.onOdometerChanged(102.5, 2000L) // 2.5km 주행 (3km 미만)
        collector.onGearChanged(GearState.PARK, 2000L)

        val result = completedTrip!!
        assertEquals(2.5, result.trip.km, 0.001)
        assertFalse("3km 미만 트립은 기록 미인정(isEligible == false)이어야 함", result.isEligible)
        assertEquals(0.0, result.trip.score, 0.001) // 3km 미만 점수 제외(0점)
    }

    @Test
    fun testLowDayScenarioReplication() {
        // 아쉬운 날 시나리오: 22.1km, 급가속 1, 급제동 2, 깜빡이 누락 4 -> 총 7건 이벤트
        var completedTrip: CompletedTrip? = null
        val collector = TripCollector(
            date = "2026-11-07",
            streakDays = 13,
            efficiencyRatio = 0.8,
            onTripCompleted = { completedTrip = it }
        )

        collector.onSeatBeltChanged(mapOf(0 to true), 1000L)
        collector.onGearChanged(GearState.DRIVE, 1000L)
        collector.onOdometerChanged(0.0, 1000L)

        // 이벤트 1: 급가속
        collector.triggerEvent("accel", 1500L, Point2D(10.0, 20.0))

        // 이벤트 2: 차선 변경 누락
        collector.triggerEvent("lane", 2000L, Point2D(20.0, 25.0))

        // 이벤트 3: 급제동
        collector.triggerEvent("brake", 2500L, Point2D(30.0, 30.0))

        // 이벤트 4: 차선 변경 누락
        collector.triggerEvent("lane", 3000L, Point2D(40.0, 35.0))

        // 이벤트 5: 차선 변경 누락
        collector.triggerEvent("lane", 3500L, Point2D(50.0, 40.0))

        // 이벤트 6: 급제동
        collector.triggerEvent("brake", 4000L, Point2D(60.0, 45.0))

        // 이벤트 7: 차선 변경 누락
        collector.triggerEvent("lane", 4500L, Point2D(70.0, 50.0))

        collector.onOdometerChanged(22.1, 5000L)
        collector.onGearChanged(GearState.PARK, 5000L)

        val result = completedTrip!!
        assertEquals(22.1, result.trip.km, 0.001)
        assertEquals(7, result.events.size)
        assertEquals(4, result.events.filter { it.kind == "lane" }.size)
        assertEquals(2, result.events.filter { it.kind == "brake" }.size)
        assertEquals(1, result.events.filter { it.kind == "accel" }.size)

        // 이벤트의 t 비율이 0..1 사이로 고르게 분포하는지 확인
        for (i in 1 until result.events.size) {
            assertTrue("이벤트의 t 비율은 순차적으로 증가해야 함", result.events[i].t >= result.events[i - 1].t)
        }
    }
}
