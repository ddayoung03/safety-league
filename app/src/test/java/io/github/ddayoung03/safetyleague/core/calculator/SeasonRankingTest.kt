package io.github.ddayoung03.safetyleague.core.calculator

import io.github.ddayoung03.safetyleague.core.data.SeasonDrivers
import io.github.ddayoung03.safetyleague.core.model.DriverRank
import io.github.ddayoung03.safetyleague.core.model.Trip
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class SeasonRankingTest {
    private val today = LocalDate.of(2026, 11, 5)
    private fun rank(vararg trips: Trip) = SeasonRanking.calculate(trips.toList(), today, "브론즈")
    private fun trip(score: Double, km: Double = 3.0, date: String = "2026-11-05") = Trip(date, score, km)

    @Test fun higherAverageImprovesRank() {
        val before = rank(trip(65.0))
        val after = rank(trip(65.0), trip(95.0))
        assertTrue(after.rank!! < before.rank!!)
        assertEquals(80.0, after.average!!, 0.001)
        assertTrue(after.stepsToPromotion < before.stepsToPromotion)
    }

    @Test fun tiedDriverFollowsExistingDriversDeterministically() {
        val result = rank(trip(96.4)) // P1 광안대교출근과 동점
        assertEquals(2, result.rank)
        assertEquals(96.4, result.neighbors.first().avg, 0.001)
        assertEquals("나", result.neighbors[1].name)
    }

    @Test fun firstAndLastHaveBoundedNeighbors() {
        val first = rank(trip(100.0))
        val last = rank(trip(0.0))
        assertEquals(1, first.rank)
        assertEquals(397, last.rank)
        assertEquals(0, first.stepsToPromotion)
        assertEquals(0.0, first.pointsToPromotion, 0.001)
        assertTrue(first.neighbors.all { it.rank in 1..397 })
        assertTrue(last.neighbors.all { it.rank in 1..397 })
        assertEquals(119, last.promotionRank)
        assertEquals(279, last.demotionRank)
    }

    @Test fun excludesShortAndOtherMonthTrips() {
        val result = rank(trip(80.0), trip(0.0, 2.99), trip(0.0, date = "2026-10-31"), trip(0.0, date = "2026-12-01"))
        assertEquals(rank(trip(80.0)), result)
    }

    @Test fun noEligibleTripsHasNoRankOrAverage() {
        assertNull(rank().rank)
        assertNull(rank(trip(90.0, 2.0)).average)
        assertTrue(rank().neighbors.isEmpty())
    }

    @Test fun sameInputHasSameResult() {
        assertEquals(rank(trip(77.0)), rank(trip(77.0)))
    }

    @Test fun regionBoardUsesRegionDriversAndOwnTotal() {
        val result = SeasonRanking.calculate(listOf(trip(88.8)), today, "브론즈", SeasonDrivers.region)
        assertEquals(149, result.total)
        assertEquals(29, result.rank)
        assertEquals(5, result.neighbors.size)
        assertTrue(result.neighbors.filter { it.name != "나" }.all { d -> SeasonDrivers.region.any { it.name == d.name } })
        assertEquals(LeagueCalculator.promotionRank(149), result.promotionRank)
    }

    @Test fun friendsBoardShowsEveryFriend() {
        val result = SeasonRanking.rank(88.8, "골드", SeasonDrivers.friends, SeasonDrivers.friends.size)
        assertEquals(6, result.total)
        assertEquals(3, result.rank)
        assertEquals(listOf("주말캠핑", "새벽배송", "나", "동네한바퀴", "카풀메이트", "세차는주말에"), result.neighbors.map { it.name })
        assertEquals((1..6).toList(), result.neighbors.map { it.rank })
    }

    @Test fun groupBoardWithoutTripsHasNoRankButKnowsTotal() {
        val result = SeasonRanking.calculate(emptyList(), today, "브론즈", SeasonDrivers.friends)
        assertNull(result.rank)
        assertEquals(6, result.total)
    }

    @Test fun boardWithOnlyMeDoesNotCrash() {
        val result = SeasonRanking.rank(70.0, "브론즈", emptyList())
        assertEquals(1, result.rank)
        assertEquals(1, result.total)
    }

    @Test fun modelBoardKeepsDefaultDriversAndLines() {
        val result = SeasonRanking.calculate(listOf(trip(80.0)), today, "브론즈")
        assertEquals(SeasonRanking.rank(80.0, "브론즈", SeasonDrivers.drivers), result)
        assertEquals(397, result.total)
        assertEquals(119, result.promotionRank)
        assertEquals(279, result.demotionRank)
    }

    @Test fun smallBoardsHaveValidSeparateLines() {
        for (others in 0..5) {
            val drivers = List(others) { DriverRank(it + 1, "친구$it", 90.0 - it) }
            for (avg in listOf(100.0, 85.0, 0.0)) {
                val s = SeasonRanking.rank(avg, "실버", drivers, drivers.size)
                assertEquals(others + 1, s.total)
                assertTrue("승급선은 1..총원", s.promotionRank in 1..s.total)
                assertTrue("강등선은 승급선 뒤", s.demotionRank > s.promotionRank)
                assertTrue(s.pointsToPromotion >= 0.0)
            }
        }
        val two = SeasonRanking.rank(80.0, "실버", listOf(DriverRank(1, "친구", 90.0)))
        assertEquals(1, two.promotionRank)
        assertEquals(2, two.demotionRank)
        assertEquals(1, two.stepsToPromotion)
        assertEquals(10.0, two.pointsToPromotion, 0.001)
    }

    // ---------- 점검 보고서 「테스트가 없는 점수·순위 로직」 ----------

    @Test fun midRankStepsAndPointsAreTheRealValues() {
        // 기본 분포(시연 순위표를 지남): 88.8 → P125, 승급선 P119 동래젠틀맨 89.7 → 6계단 · 0.9점
        val s = SeasonRanking.rank(88.8, "골드", SeasonDrivers.drivers)
        assertEquals(125, s.rank)
        assertEquals(6, s.stepsToPromotion)
        assertEquals(0.9, s.pointsToPromotion, 0.0)
    }

    @Test fun tieWithPromotionLineShowsAtLeastOneTenth() {
        // L7: 승급선과 동점이면 내가 뒤(P(line+1))다. "1계단 · 0.0점" 대신 "1계단 · 0.1점"
        val drivers = List(9) { DriverRank(it + 1, "운전자$it", 95.0 - it) } // 95.0 .. 87.0, 10명 → 승급선 P3
        val lineAvg = 93.0
        val s = SeasonRanking.rank(lineAvg, "실버", drivers)
        assertEquals(3, s.promotionRank)
        assertEquals(4, s.rank)
        assertEquals(1, s.stepsToPromotion)
        assertEquals(0.1, s.pointsToPromotion, 0.0)
        // 승급권 안이면 0점 그대로
        val inside = SeasonRanking.rank(93.5, "실버", drivers)
        assertEquals(0, inside.stepsToPromotion)
        assertEquals(0.0, inside.pointsToPromotion, 0.0)
    }
}
