package io.github.ddayoung03.safetyleague.core.calculator

import io.github.ddayoung03.safetyleague.core.data.SeasonDrivers
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
        val result = rank(trip(88.0))
        assertEquals(2, result.rank)
        assertEquals(88.0, result.neighbors.first().avg, 0.001)
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
}
