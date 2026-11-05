package io.github.ddayoung03.safetyleague.core.calculator

import io.github.ddayoung03.safetyleague.core.model.DetailScore
import io.github.ddayoung03.safetyleague.core.model.Trip
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class WeeklyReportTest {
    // 2026-11-05 목요일. 이번 주 11.02(월) ~ 11.08(일), 지난주 10.26 ~ 11.01
    private val thursday = LocalDate.of(2026, 11, 5)
    private fun report(vararg trips: Trip, today: LocalDate = thursday) =
        WeeklyReportCalculator.calculate(trips.toList(), today, "브론즈")
    private fun trip(date: String, score: Double, km: Double = 10.0, detail: DetailScore? = null) = Trip(date, score, km, detail)

    @Test fun weekRunsMondayToSundayAndAveragesItsTrips() {
        val r = report(
            trip("2026-11-01", 50.0),            // 지난주 일요일
            trip("2026-11-02", 80.0, km = 12.5),
            trip("2026-11-04", 90.0, km = 4.0),
            trip("2026-11-04", 70.0, km = 3.0)
        )
        assertEquals(LocalDate.of(2026, 11, 2), r.monday)
        assertEquals(LocalDate.of(2026, 11, 8), r.sunday)
        assertEquals(80.0, r.average!!, 0.001)
        assertEquals(50.0, r.lastWeekAverage!!, 0.001)
        assertEquals(30.0, r.change!!, 0.001)
        assertEquals(19.5, r.km, 0.001)
        assertEquals(listOf(80, null, 80, null, null, null, null), r.dailyScores)
        assertEquals(2, r.drivingDays)
        assertFalse(r.isEmpty)
    }

    @Test fun shortAndFutureTripsAreIgnored() {
        val r = report(trip("2026-11-03", 40.0, km = 2.9), trip("2026-11-06", 99.0), trip("2026-11-03", 75.0))
        assertEquals(75.0, r.average!!, 0.001)
        assertEquals(10.0, r.km, 0.001)
        assertEquals(1, r.drivingDays)
    }

    @Test fun emptyWeekKeepsLastWeekButHasNoAverage() {
        val r = report(trip("2026-10-28", 85.7))
        assertTrue(r.isEmpty)
        assertNull(r.average)
        assertNull(r.change)
        assertEquals(85.7, r.lastWeekAverage!!, 0.001)
        assertEquals(List(7) { null }, r.dailyScores)
        assertEquals(0, r.drivingDays)
        assertEquals(0.0, r.km, 0.001)
        assertEquals("이번 주 기록이 아직 없어요. 3km 이상 달리면 리포트가 채워져요", WeeklyReportCalculator.nextPlan(r))
    }

    @Test fun noTripsAtAll() {
        val r = report()
        assertTrue(r.isEmpty)
        assertNull(r.lastWeekAverage)
        assertEquals(List(7) { null }, r.dailyRanks)
        assertNull(WeeklyReportCalculator.rankMove(r.dailyRanks))
        assertTrue(r.categories.isEmpty())
        assertNull(r.focus)
    }

    @Test fun dailyRanksFollowSeasonUpToEachDayAndStopToday() {
        val r = report(trip("2026-11-03", 60.0), trip("2026-11-05", 100.0))
        assertNull(r.dailyRanks[0])                       // 월: 시즌 첫 트립 전
        val tue = r.dailyRanks[1]!!
        assertEquals(tue, r.dailyRanks[2])                // 수: 주행 없음, 순위 그대로
        assertTrue(r.dailyRanks[3]!! < tue)               // 목: 평균이 올라 순위 상승
        assertEquals(listOf(null, null, null), r.dailyRanks.subList(4, 7))
        assertEquals(r.standing.rank, r.dailyRanks[3])
    }

    @Test fun newSeasonResetsRanksMidWeek() {
        // 10.26(월) ~ 11.01(일) 주, 11.01에 새 시즌
        val r = report(trip("2026-10-27", 80.0), today = LocalDate.of(2026, 11, 1))
        assertNull(r.dailyRanks[0])
        assertNotNull(r.dailyRanks[1])
        assertNull(r.dailyRanks[6])
        assertNull(r.standing.rank)
        assertEquals("이번 시즌 첫 주행을 마치면 순위가 나와요", WeeklyReportCalculator.nextPlan(r))
    }

    @Test fun categoryChangesAndFocusPickBiggestDrop() {
        val last = DetailScore(25.0, 20.0, 15.0, 15.0, 8.0)
        val now = DetailScore(27.0, 15.0, 15.0, 15.0, 8.0)
        val r = report(trip("2026-10-30", 83.0, detail = last), trip("2026-11-03", 60.0, detail = now))
        assertEquals(listOf("smoothness", "manner", "eco", "safety", "consistency"), r.categories.map { it.key })
        assertEquals(2.0, r.categories[0].change!!, 0.001)
        assertEquals(-5.0, r.categories[1].change!!, 0.001)
        assertEquals(15.0, r.categories[1].average, 0.001)
        assertEquals("manner", r.focus)
        assertTrue(WeeklyReportCalculator.nextPlan(r).startsWith("깜빡이 누락만 줄이면 승급 라인까지 "))
    }

    @Test fun focusWithoutDropIsLowestShareOfMax() {
        // 만점 대비: 부드러움 0.9, 매너 0.8, 에코 0.5, 안전 1.0, 꾸준함 0.6
        val r = report(trip("2026-11-03", 75.0, detail = DetailScore(27.0, 20.0, 10.0, 15.0, 6.0)))
        assertTrue(r.categories.all { it.change == null })
        assertEquals("eco", r.focus)
    }

    @Test fun tripsWithoutDetailHaveNoFocus() {
        val r = report(trip("2026-11-03", 75.0))
        assertNull(r.focus)
        assertTrue(WeeklyReportCalculator.nextPlan(r).startsWith("승급 라인까지 "))
    }

    @Test fun nextPlanInsidePromotionZone() {
        val r = report(trip("2026-11-03", 100.0))
        assertEquals(0, r.standing.stepsToPromotion)
        assertEquals("승급권을 지킬 수 있어요", WeeklyReportCalculator.nextPlan(r))
    }

    @Test fun nextPlanMatchesDemoWording() {
        val r = report(trip("2026-11-03", 60.0, detail = DetailScore(25.0, 10.0, 12.0, 8.0, 5.0)))
        val s = r.standing
        assertEquals("깜빡이 누락만 줄이면 승급 라인까지 ${s.stepsToPromotion}계단, 평균 +${s.pointsToPromotion}점이면 돼요",
            WeeklyReportCalculator.nextPlan(r))
    }

    @Test fun titleAndRangeMatchDemoWeek() {
        val mon = LocalDate.of(2026, 11, 3)
        assertEquals("11월 2주차 리포트", WeeklyReportCalculator.title(LocalDate.of(2026, 11, 2)))
        assertEquals("11.03 ~ 11.09", WeeklyReportCalculator.range(mon, mon.plusDays(6)))
        assertEquals("10월 5주차 리포트", WeeklyReportCalculator.title(LocalDate.of(2026, 10, 26)))
        assertEquals("10.26 ~ 11.01", WeeklyReportCalculator.range(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 1)))
    }

    @Test fun rankMoveText() {
        assertEquals("P131 → P125 ▲6", WeeklyReportCalculator.rankMove(listOf(131, 130, 129, 128, 127, 126, 125)))
        assertEquals("P120 → P125 ▼5", WeeklyReportCalculator.rankMove(listOf(null, 120, 125, null)))
        assertEquals("P200 → P200 변동 없음", WeeklyReportCalculator.rankMove(listOf(200, 200)))
        assertEquals("P351", WeeklyReportCalculator.rankMove(listOf(null, 351, null)))
    }
}
