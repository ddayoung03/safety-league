package ai.pleos.safetyleague.core

import ai.pleos.safetyleague.core.calculator.LeagueCalculator
import ai.pleos.safetyleague.core.calculator.ScoreCalculator
import ai.pleos.safetyleague.core.data.DemoData
import ai.pleos.safetyleague.core.model.Point2D
import ai.pleos.safetyleague.core.model.Trip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * mockups/test/demo-data.test.js의 단위 테스트를 Kotlin으로 100% 동일하게 재현 및 확장한 테스트
 */
class DemoDataTest {

    @Test
    fun testSeasonAverage() {
        assertEquals(88.5, DemoData.seasonAverage("2026-11-07"), 0.001)
        assertEquals(88.8, DemoData.seasonAverage("2026-11-09"), 0.001)
    }

    @Test
    fun testWeeklyAverages() {
        assertEquals(88.8, DemoData.average("2026-11-03", "2026-11-09"), 0.001)
        assertEquals(85.7, DemoData.average("2026-10-27", "2026-11-02"), 0.001)
    }

    @Test
    fun testStreakCountsDriveDaysAbove70AndSkipsNoDriveDays() {
        assertEquals(12, DemoData.streakAt("2026-11-06"))
        assertEquals(13, DemoData.streakAt("2026-11-07"))
        assertEquals(15, DemoData.streakAt("2026-11-09"))
        assertEquals(11, DemoData.streakAt("2026-11-05")) // 11/5는 운전 안 한 날 (휴식) -> 이전 날의 11일 유지
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
        for (d in listOf("2026-11-07", "2026-11-09")) {
            val rows = DemoData.neighbors(d)
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
            assertTrue("사건은 시간순으로 정렬되어 있어야 함", ev[i - 1].at < ev[i].at)
        }

        for (e in ev) {
            assertTrue("t 비율은 0과 1 사이여야 함", e.t > 0.0 && e.t < 1.0)
            assertTrue("사건 발생 시각은 시작 시각 이후여야 함", e.at > DemoData.lowDayStart)
            assertTrue("사건 발생 시각은 종료 시각 이전이어야 함", e.at < DemoData.lowDayEnd)
        }

        assertTrue("가상 경로는 최소 8개 이상의 점이어야 함", DemoData.route.size >= 8)
    }

    @Test
    fun testScoreCalculatorRules() {
        // 3km 미만 트립은 기록 및 점수 제외
        val shortTrip = ScoreCalculator.calculateTripScore(
            km = 2.8,
            hardAccels = 0,
            hardBrakes = 0,
            unindicatedLaneChanges = 0,
            efficiencyRatio = 1.0,
            allBeltsFastenedBeforeDrive = true,
            unresolvedWarningSeconds = 0,
            streakDays = 5
        )
        assertEquals(0.0, shortTrip.total, 0.001)

        // 완벽한 18.4km 주행 시 만점 계산
        val perfectTrip = ScoreCalculator.calculateTripScore(
            km = 18.4,
            hardAccels = 0,
            hardBrakes = 0,
            unindicatedLaneChanges = 0,
            efficiencyRatio = 1.2, // 120% 전비
            allBeltsFastenedBeforeDrive = true,
            unresolvedWarningSeconds = 0,
            streakDays = 13
        )
        assertEquals(30.0, perfectTrip.smoothness, 0.001)
        assertEquals(25.0, perfectTrip.manner, 0.001)
        assertEquals(20.0, perfectTrip.eco, 0.001)
        assertEquals(15.0, perfectTrip.safety, 0.001)
        assertEquals(10.0, perfectTrip.consistency, 0.001)
        assertEquals(100.0, perfectTrip.total, 0.001)
    }
}
