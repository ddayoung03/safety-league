package io.github.ddayoung03.safetyleague.core.calculator

import io.github.ddayoung03.safetyleague.core.model.DetailScore
import io.github.ddayoung03.safetyleague.core.model.ReplayEvent
import io.github.ddayoung03.safetyleague.core.model.TripStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoreBreakdownTest {

    private fun stats(
        km: Double = 3.8, accels: Int = 0, brakes: Int = 0, lanes: Int = 0, unindicated: Int = 0,
        efficiency: Double? = null, belts: Boolean = true, warningSec: Long = 0, doors: Int = 0, streak: Int = 0
    ) = TripStats(km, accels, brakes, lanes, unindicated, efficiency, 5.0, belts, warningSec, doors, streak)

    @Test fun rowsAddUpToTheTripScoreAndKeepTheSpecOrder() {
        val s = stats(brakes = 1, belts = false)
        val d = ScoreCalculator.calculate(s)
        val rows = ScoreBreakdown.rows(d, emptyList(), s.km, s)
        assertEquals(listOf("부드러움", "매너", "에코", "안전 기본기", "꾸준함"), rows.map { it.label })
        assertEquals(listOf(30.0, 25.0, 20.0, 15.0, 10.0), rows.map { it.max })
        assertEquals(d.total, rows.sumOf { it.got }, 0.001)
    }

    @Test fun explainsWhyOneHardBrakeOnAShortTripCostsSoMuch() {
        // 실제 사례: 3.8km, 급제동 1회, 벨트 미착용, 전비 신호 없음, 첫 주행 → 63점
        val s = stats(brakes = 1, belts = false)
        val d = ScoreCalculator.calculate(s)
        assertEquals(63.0, ScoreCalculator.tripScore(d), 0.0)
        val notes = ScoreBreakdown.rows(d, emptyList(), s.km, s).map { it.note }
        assertEquals("급제동 1회 · 3.8km 주행이라 10km당 2.6회꼴로 환산", notes[0])
        assertEquals("깜빡이 없는 차선 변경 없음", notes[1])
        assertEquals("전비·연비 신호가 없어 평균(10점)으로 계산", notes[2])
        assertEquals("출발할 때 벨트 미착용", notes[3])
        assertEquals("이전까지 연속 안전 주행 0일(10일이면 7점) · 주행 3.8km(10km면 3점)", notes[4])
    }

    @Test fun listsEverySafetyDeductionAndEcoRatio() {
        val s = stats(km = 20.0, accels = 2, lanes = 5, unindicated = 2, efficiency = 5.5, belts = false, warningSec = 95, doors = 1, streak = 12)
        val notes = ScoreBreakdown.rows(ScoreCalculator.calculate(s), emptyList(), s.km, s).map { it.note }
        assertEquals("급가속 2회 · 20.0km 주행이라 10km당 1.0회꼴로 환산", notes[0])
        assertEquals("차선 변경 5회 중 2회가 깜빡이 없이", notes[1])
        assertEquals("같은 차종 평균의 110%", notes[2])
        assertEquals("출발할 때 벨트 미착용 · 경고등 95초 방치 · 주행 중 문 열림 1회", notes[3])
    }

    @Test fun perfectTripSaysSo() {
        val s = stats(km = 12.0, efficiency = 6.0, streak = 10)
        val rows = ScoreBreakdown.rows(ScoreCalculator.calculate(s), emptyList(), s.km, s)
        assertEquals("급가속·급제동 없음", rows[0].note)
        assertEquals("벨트·경고등·문 모두 이상 없음", rows[3].note)
        assertEquals(100.0, rows.sumOf { it.got }, 0.001)
    }

    @Test fun oldTripsAndDemoCardsWithoutStatsFallBackToEvents() {
        val events = listOf("accel", "lane", "brake", "lane", "brake").map { ReplayEvent("6:10", it, 0.5) }
        val rows = ScoreBreakdown.rows(DetailScore(25.0, 0.0, 14.0, 15.0, 10.0), events, 22.1, null)
        assertEquals("급가속 1회 · 급제동 2회 · 22.1km 주행이라 10km당 1.4회꼴로 환산", rows[0].note)
        assertEquals("깜빡이 없이 차선 변경 2회", rows[1].note)
        assertTrue(rows[2].note.isNotEmpty() && rows[4].note.isNotEmpty())
        assertEquals("벨트·경고등·문 모두 이상 없음", rows[3].note)
    }
}
