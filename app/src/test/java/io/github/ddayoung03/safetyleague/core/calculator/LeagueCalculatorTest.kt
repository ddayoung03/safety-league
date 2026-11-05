package io.github.ddayoung03.safetyleague.core.calculator

import io.github.ddayoung03.safetyleague.core.model.Trip
import org.junit.Assert.assertEquals
import org.junit.Test

/** 점검 보고서 「테스트가 없는 점수·순위 로직」의 LeagueCalculator 항목. */
class LeagueCalculatorTest {

    @Test fun linesForSmallLeagues() {
        // 인원: 승급선(상위 30% 반올림), 강등선(하위 30%). 작은 리그 보정은 SeasonRanking.rank가 한다.
        val expected = mapOf(1 to (0 to 2), 2 to (1 to 2), 3 to (1 to 3), 4 to (1 to 4), 5 to (2 to 4), 6 to (2 to 5), 397 to (119 to 279))
        for ((n, lines) in expected) {
            assertEquals("승급선 ${n}명", lines.first, LeagueCalculator.promotionRank(n))
            assertEquals("강등선 ${n}명", lines.second, LeagueCalculator.demotionRank(n))
        }
    }

    @Test fun averageRoundsHalfUpToOneDecimal() {
        // 706.8 / 8 = 88.35 → 88.4 (부동소수 오차로 88.3이 되지 않는다)
        val trips = List(7) { Trip("2026-11-0${it + 1}", 88.0) } + Trip("2026-11-08", 90.8)
        assertEquals(88.4, LeagueCalculator.average(trips, "2026-11-01", "2026-11-30"), 0.0)
        // 정수 점수 4개 353 / 4 = 88.25 → 88.3
        val ints = listOf(88.0, 88.0, 88.0, 89.0).mapIndexed { i, s -> Trip("2026-11-1$i", s) }
        assertEquals(88.3, LeagueCalculator.average(ints, "2026-11-01", "2026-11-30"), 0.0)
        assertEquals(0.0, LeagueCalculator.average(emptyList(), "2026-11-01", "2026-11-30"), 0.0)
    }

    @Test fun daysLeftIsZeroOnTheLastDay() {
        assertEquals(29, LeagueCalculator.daysLeft("2026-11-30", "2026-11-01"))
        assertEquals(1, LeagueCalculator.daysLeft("2026-11-30", "2026-11-29"))
        assertEquals(0, LeagueCalculator.daysLeft("2026-11-30", "2026-11-30"))
    }

    @Test fun streakIs70OrMoreOnEveryTripOfTheDay() {
        val trips = listOf(Trip("2026-11-01", 70.0), Trip("2026-11-02", 69.0), Trip("2026-11-03", 70.0), Trip("2026-11-03", 95.0))
        assertEquals(1, LeagueCalculator.streakAt(trips, "2026-11-01"))
        assertEquals(0, LeagueCalculator.streakAt(trips, "2026-11-02"))
        assertEquals(1, LeagueCalculator.streakAt(trips, "2026-11-03"))
        assertEquals(1, LeagueCalculator.streakAt(trips, "2026-11-04")) // 운전 안 한 날은 그대로
    }

    @Test fun eligibleTripIsAtLeast3km() {
        assertEquals(false, LeagueCalculator.isEligibleTrip(2.96))
        assertEquals(true, LeagueCalculator.isEligibleTrip(3.0))
    }
}
