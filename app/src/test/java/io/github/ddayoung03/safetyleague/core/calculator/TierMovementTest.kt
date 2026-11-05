package io.github.ddayoung03.safetyleague.core.calculator

import io.github.ddayoung03.safetyleague.core.model.Trip
import org.junit.Assert.*
import org.junit.Test
import java.time.YearMonth

class TierMovementTest {
    private val oct = YearMonth.of(2026, 10)
    private fun trip(score: Double, date: String = "2026-10-15") = Trip(date, score, 5.0)

    @Test fun exactThirtyPercentBoundary() {
        // 10명: 상위 30% = 3위까지 승급, 하위 30% = 8위부터 강등
        assertEquals(TierMove.PROMOTED, TierMovement.move("실버", 3, 10))
        assertEquals(TierMove.STAYED, TierMovement.move("실버", 4, 10))
        assertEquals(TierMove.STAYED, TierMovement.move("실버", 7, 10))
        assertEquals(TierMove.DEMOTED, TierMovement.move("실버", 8, 10))
    }

    @Test fun leagueSizeBoundaryMatchesLeagueScreen() {
        assertEquals(TierMove.PROMOTED, TierMovement.move("골드", 119, 397))
        assertEquals(TierMove.STAYED, TierMovement.move("골드", 120, 397))
        assertEquals(TierMove.STAYED, TierMovement.move("골드", 278, 397))
        assertEquals(TierMove.DEMOTED, TierMovement.move("골드", 279, 397))
    }

    @Test fun fewDrivers() {
        assertEquals(TierMove.STAYED, TierMovement.move("실버", 1, 1))
        assertEquals(TierMove.PROMOTED, TierMovement.move("실버", 1, 2))
        assertEquals(TierMove.DEMOTED, TierMovement.move("실버", 2, 2))
        assertEquals(TierMove.PROMOTED, TierMovement.move("실버", 1, 3))
        assertEquals(TierMove.STAYED, TierMovement.move("실버", 2, 3))
        assertEquals(TierMove.DEMOTED, TierMovement.move("실버", 3, 3))
        assertEquals(TierMove.STAYED, TierMovement.move("실버", 1, 0))
    }

    @Test fun noTripsStays() {
        assertEquals(TierMove.STAYED, TierMovement.move("골드", null, 397))
        val r = TierMovement.settle(emptyList(), oct, "골드")
        assertEquals("골드", r.toTier)
        assertNull(r.rank)
        assertTrue(TierMovement.message(r).contains("기록이 없어"))
    }

    @Test fun edgesDoNotMove() {
        assertEquals(TierMove.STAYED, TierMovement.move("다이아", 1, 397))
        assertEquals(TierMove.STAYED, TierMovement.move("브론즈", 397, 397))
        assertEquals("다이아", TierMovement.apply("다이아", TierMove.PROMOTED))
        assertEquals("브론즈", TierMovement.apply("브론즈", TierMove.DEMOTED))
        assertEquals(TierMove.STAYED, TierMovement.move("알수없음", 1, 397))
    }

    @Test fun settleUsesSeasonRankingForThatMonthOnly() {
        val top = TierMovement.settle(listOf(trip(100.0)), oct, "브론즈")
        assertEquals(1, top.rank)
        assertEquals("실버", top.toTier)
        val low = TierMovement.settle(listOf(trip(0.0)), oct, "실버")
        assertEquals(397, low.rank)
        assertEquals("브론즈", low.toTier)
        // 다른 달 트립은 이 시즌 정산에 들어가지 않는다
        assertNull(TierMovement.settle(listOf(trip(100.0, "2026-11-01")), oct, "실버").rank)
    }

    @Test fun settleSinceRunsEachSkippedMonthOnce() {
        val trips = listOf(trip(100.0, "2026-09-10"), trip(100.0, "2026-10-10"))
        val results = TierMovement.settleSince(trips, YearMonth.of(2026, 9), YearMonth.of(2026, 11), "브론즈")
        assertEquals(listOf("실버", "골드"), results.map { it.toTier })
        val summary = TierMovement.summarize(results)!!
        assertEquals("브론즈", summary.fromTier)
        assertEquals("골드", summary.toTier)
        assertEquals(TierMove.PROMOTED, summary.move)
        assertEquals(oct, summary.season)
    }

    @Test fun sameOrPastSeasonSettlesNothing() {
        assertTrue(TierMovement.settleSince(listOf(trip(100.0)), oct, oct, "브론즈").isEmpty())
        assertTrue(TierMovement.settleSince(listOf(trip(100.0)), oct, YearMonth.of(2026, 9), "브론즈").isEmpty())
        assertNull(TierMovement.summarize(emptyList()))
    }

    @Test fun promotionThenEmptyMonthStillReportsPromotion() {
        val results = TierMovement.settleSince(listOf(trip(100.0)), oct, YearMonth.of(2026, 12), "브론즈")
        val summary = TierMovement.summarize(results)!!
        assertEquals(TierMove.PROMOTED, summary.move)
        assertEquals("실버", summary.toTier)
    }

    @Test fun messagesAreEncouraging() {
        val demoted = SeasonResult(oct, "실버", "브론즈", TierMove.DEMOTED, 300)
        assertEquals("10월 시즌 결과 · 브론즈 리그에서 새로 출발해요. 차근차근 다시 올라가 봐요", TierMovement.message(demoted))
        assertTrue(TierMovement.message(SeasonResult(oct, "브론즈", "실버", TierMove.PROMOTED, 1)).contains("승급"))
        assertTrue(TierMovement.message(SeasonResult(oct, "다이아", "다이아", TierMove.STAYED, 1)).contains("지켰어요"))
    }
}
