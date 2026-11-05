package io.github.ddayoung03.safetyleague.core

import io.github.ddayoung03.safetyleague.core.calculator.LeagueCalculator
import io.github.ddayoung03.safetyleague.core.calculator.ScoreCalculator
import io.github.ddayoung03.safetyleague.core.calculator.SeasonRanking
import io.github.ddayoung03.safetyleague.core.data.SeasonDrivers
import io.github.ddayoung03.safetyleague.core.model.DriverRank
import io.github.ddayoung03.safetyleague.core.model.TripStats
import java.time.DayOfWeek
import java.time.LocalDate
import io.github.ddayoung03.safetyleague.core.model.toMinutes
import io.github.ddayoung03.safetyleague.core.data.DemoData
import io.github.ddayoung03.safetyleague.core.model.Point2D
import io.github.ddayoung03.safetyleague.core.model.Trip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * mockups/test/demo-data.test.js의 단위 테스트를 Kotlin으로 100% 동일하게 재현 및 확장한 테스트
 */
class DemoDataTest {

    @Test
    fun testSeasonAverage() {
        assertEquals(88.7, DemoData.seasonAverage("2026-11-07"), 0.001)
        assertEquals(88.3, DemoData.seasonAverage("2026-11-08"), 0.001)
        assertEquals(88.8, DemoData.seasonAverage("2026-11-09"), 0.001)
    }

    @Test
    fun testWeeklyAverages() {
        assertEquals(88.8, DemoData.average("2026-11-03", "2026-11-09"), 0.001)
        assertEquals(85.7, DemoData.average("2026-10-27", "2026-11-02"), 0.001)
    }

    @Test
    fun testStreakCountsDriveDaysAbove70AndSkipsNoDriveDays() {
        assertEquals(10, DemoData.streakAt("2026-11-06"))
        assertEquals(11, DemoData.streakAt("2026-11-07"))
        assertEquals(13, DemoData.streakAt("2026-11-09"))
        assertEquals(9, DemoData.streakAt("2026-11-05")) // 11/5는 운전 안 한 날 (휴식) -> 이전 날의 9일 유지
    }

    @Test
    fun testStreakBreaksUnder70AndOnAnyLowTripThatDay() {
        // 10/24는 66점으로 70점 미만 -> 연속 기록 0
        assertEquals(0, DemoData.streakAt("2026-10-24"))

        // 하루에 90점 트립과 60점 트립이 둘 다 있으면 그날은 끊김 (0일)
        val mixedDayTrips = listOf(
            Trip(date = "2026-12-01", score = 90.0),
            Trip(date = "2026-12-01", score = 60.0)
        )
        assertEquals(0, LeagueCalculator.streakAt(mixedDayTrips, "2026-12-01"))
    }

    @Test
    fun testLeagueLines() {
        assertEquals(119, LeagueCalculator.promotionRank(397))
        assertEquals(279, LeagueCalculator.demotionRank(397))
        assertEquals(21, DemoData.daysLeft("2026-11-09"))
    }

    @Test
    fun testNeighborsAreSortedAndIncludeMe() {
        for (d in listOf(DemoData.today)) {
            val rows = DemoData.neighbors()
            val me = rows.find { it.name == "나" }
            assertTrue("내가 순위표에 포함되어 있어야 함", me != null)
            assertEquals(DemoData.ranks[d], me!!.rank)
            assertEquals(DemoData.seasonAverage(d), me.avg, 0.001)

            for (i in 1 until rows.size) {
                val prev = rows[i - 1]
                val curr = rows[i]
                assertTrue(
                    "$d 행 $i: 이전 운전자 avg(${prev.avg}) >= 현재 avg(${curr.avg})",
                    prev.avg >= curr.avg
                )
                assertTrue(
                    "$d 행 $i: 이전 운전자 rank(${prev.rank}) < 현재 rank(${curr.rank})",
                    prev.rank < curr.rank
                )
            }
        }
    }

    @Test
    fun testPointAtWalksTheRouteByDistance() {
        val line = listOf(
            Point2D(0.0, 0.0),
            Point2D(10.0, 0.0),
            Point2D(10.0, 10.0)
        )
        val p0 = LeagueCalculator.pointAt(line, 0.0)
        assertEquals(0.0, p0.x, 0.001)
        assertEquals(0.0, p0.y, 0.001)

        val pHalf = LeagueCalculator.pointAt(line, 0.5)
        assertEquals(10.0, pHalf.x, 0.001)
        assertEquals(0.0, pHalf.y, 0.001)

        val p75 = LeagueCalculator.pointAt(line, 0.75)
        assertEquals(10.0, p75.x, 0.001)
        assertEquals(5.0, p75.y, 0.001)

        val p1 = LeagueCalculator.pointAt(line, 1.0)
        assertEquals(10.0, p1.x, 0.001)
        assertEquals(10.0, p1.y, 0.001)
    }

    @Test
    fun testLowDayReplayEventsMatchThePitBoard() {
        val ev = DemoData.lowDayEvents
        val laneEvents = ev.filter { it.kind == "lane" }
        assertEquals(4, laneEvents.size) // 깜빡이 없는 차선 변경 4회

        for (i in 1 until ev.size) {
            assertTrue("사건은 시간순으로 정렬되어 있어야 함", ev[i - 1].minutes < ev[i].minutes)
        }

        for (e in ev) {
            assertTrue("t 비율은 0과 1 사이여야 함", e.t > 0.0 && e.t < 1.0)
            assertTrue("사건 발생 시각은 시작 시각 이후여야 함", e.minutes > toMinutes(DemoData.lowDayStart))
            assertTrue("사건 발생 시각은 종료 시각 이전이어야 함", e.minutes < toMinutes(DemoData.lowDayEnd))
        }

        assertTrue("가상 경로는 최소 8개 이상의 점이어야 함", DemoData.route.size >= 8)
    }

    // ---------- 2026-11-05 시연 숫자 정리 (점검 보고서 C1·C2·C4·M4) ----------

    @Test
    fun winCardLeagueAndReportShareOneDay() {
        val today = DemoData.today
        val win = DemoData.trips.single { it.date == today }
        assertEquals(92.0, win.score, 0.001)
        assertEquals(18.4, win.km, 0.001)
        val me = DemoData.ranks.getValue(today)
        val before = DemoData.ranks.getValue(LocalDate.parse(today).minusDays(1).toString())
        assertEquals(125, me)
        assertEquals(3, before - me) // 카드 ▲3
        assertEquals(13, DemoData.streakAt(today)) // 카드 연속 기록 (11-09 기준, 시안 result-win과 같은 13일)
        // 카드·리그·리포트 모두 같은 내 평균
        val myAvg = DemoData.neighbors().single { it.name == "나" }.avg
        assertEquals(88.8, myAvg, 0.001)
        assertEquals(DemoData.seasonAverage(today), myAvg, 0.001)
        assertEquals(88.8, DemoData.average("2026-11-03", today), 0.001)
        // 추월한 세 명(P126~P128)은 어제 내 평균(88.3)보다 높고 오늘 내 평균보다 낮다
        val yesterday = DemoData.seasonAverage(LocalDate.parse(today).minusDays(1).toString())
        val passed = DemoData.neighbors().filter { it.rank in (me + 1)..before }
        assertEquals(3, passed.size)
        assertTrue(passed.all { it.avg > yesterday && it.avg < myAvg })
        assertEquals(LeagueCalculator.round1(89.1 - 88.8), LeagueCalculator.round1(DemoData.neighbors().single { it.rank == me - 1 }.avg - myAvg), 0.001)
    }

    @Test
    fun oneNameHasOneAverageAcrossDemoBoards() {
        val all = DemoData.neighbors() + DemoData.lowBoard
        for ((name, rows) in all.filter { it.name != "나" }.groupBy { it.name }) {
            assertEquals("$name 평균은 한 값이어야 함", 1, rows.map { it.avg }.distinct().size)
        }
    }

    @Test
    fun lowDayRankAndAverageFollowFromTheScore() {
        val others = DemoData.lowBoard.filter { it.name != "나" }
        fun rankWith(avg: Double): Int {
            val firstRank = DemoData.lowBoard.minOf { it.rank }
            return firstRank + others.count { it.avg >= avg }
        }
        assertEquals(88.3, DemoData.lowDayAvgAfter, 0.001)
        assertEquals(DemoData.lowDayRankBefore, rankWith(DemoData.lowDayAvgBefore))
        val me = DemoData.lowBoard.single { it.name == "나" }
        assertEquals(126, me.rank)
        assertEquals(rankWith(DemoData.lowDayAvgAfter), me.rank)
        assertEquals(1, me.rank - DemoData.lowDayRankBefore) // 카드 ▼1
        assertEquals(64, DemoData.lowDayScore)
    }

    @Test
    fun lowDayDetailIsWhatScoreCalculatorGivesForTheReplay() {
        val ev = DemoData.lowDayEvents
        val stats = TripStats(
            km = DemoData.lowDayKm, hardAccels = ev.count { it.kind == "accel" }, hardBrakes = ev.count { it.kind == "brake" },
            laneChanges = 4, unindicatedLaneChanges = ev.count { it.kind == "lane" }, efficiency = 5.8 * 1.08,
            leagueAvgEfficiency = DemoData.leagueAvgEfficiency, allBeltsFastened = true, unresolvedWarningSeconds = 0,
            doorOpenWhileMoving = 0, streakDays = DemoData.lowDayStreakBefore
        )
        val detail = ScoreCalculator.calculate(stats)
        assertEquals(DemoData.lowDayDetail, detail)
        assertEquals(DemoData.lowDayScore.toDouble(), detail.total, 0.001)
        assertEquals(25.0, 25.0 - detail.manner, 0.001) // 피트 보드 "매너 +25점 가능"
    }

    @Test
    fun winDayScoreBreakdownGives92WithoutPenaltyEvents() {
        val stats = TripStats(
            km = DemoData.winDayKm, hardAccels = 0, hardBrakes = 0, laneChanges = 1, unindicatedLaneChanges = 0,
            efficiency = 5.8 * 1.04, leagueAvgEfficiency = DemoData.leagueAvgEfficiency, allBeltsFastened = true,
            unresolvedWarningSeconds = 0, doorOpenWhileMoving = 0,
            streakDays = DemoData.streakAt(LocalDate.parse(DemoData.today).minusDays(1).toString())
        )
        assertEquals(DemoData.winDayDetail, ScoreCalculator.calculate(stats))
        assertEquals(92.0, ScoreCalculator.calculate(stats).total, 0.001)
        assertTrue(DemoData.winDayEvents.isEmpty())
    }

    @Test
    fun winDayReplayIsTheWinTripNotTheLowDay() {
        assertEquals(18.4, DemoData.winDayKm, 0.001)
        assertTrue(toMinutes(DemoData.winDayStart) < toMinutes(DemoData.winDayEnd))
        assertTrue(DemoData.winDayKm != DemoData.lowDayKm)
    }

    @Test
    fun realModeDriversPassThroughTheDemoBoard() {
        val d = SeasonDrivers.drivers
        assertEquals(396, d.size)
        for (i in 1 until d.size) assertTrue("가상 운전자 평균은 내림차순", d[i - 1].avg >= d[i].avg)
        val s = SeasonRanking.rank(DemoData.seasonAverage(DemoData.today), DemoData.league.tier, d, radius = 400)
        assertEquals(DemoData.league.total, s.total)
        assertEquals(DemoData.ranks.getValue(DemoData.today), s.rank)
        for (row in DemoData.neighbors()) {
            assertEquals(row, s.neighbors.single { it.rank == row.rank }.let { if (it.name == "나") it.copy(avg = row.avg) else it })
        }
        assertEquals(89.7, s.neighbors.single { it.rank == s.promotionRank }.avg, 0.001)
        // 같은 평균이면 실제 모드도 시연과 같은 순위
        val real = SeasonRanking.calculate(listOf(Trip(DemoData.today, 88.8, 10.0)), LocalDate.parse(DemoData.today), "골드")
        assertEquals(125, real.rank)
        assertEquals(6, real.stepsToPromotion)
        assertEquals(0.9, real.pointsToPromotion, 0.001)
    }

    @Test
    fun demoReportWeekLabelsComeFromDates() {
        val from = LocalDate.parse(DemoData.today).minusDays(6)
        assertEquals(LocalDate.of(2026, 11, 3), from)
        assertEquals(DayOfWeek.TUESDAY, from.dayOfWeek) // 첫 칸은 "화"
        assertEquals(DayOfWeek.THURSDAY, LocalDate.of(2026, 11, 5).dayOfWeek) // 휴식일
        assertTrue(DemoData.trips.none { it.date == "2026-11-05" })
        assertEquals(DayOfWeek.MONDAY, LocalDate.parse(DemoData.today).dayOfWeek) // 마지막 칸 "월"
        assertEquals(85.7, DemoData.average(from.minusDays(7).toString(), from.minusDays(1).toString()), 0.001)
    }
}
