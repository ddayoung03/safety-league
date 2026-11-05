package io.github.ddayoung03.safetyleague.core.repository

import io.github.ddayoung03.safetyleague.core.model.CompletedTrip
import io.github.ddayoung03.safetyleague.core.model.Trip
import org.junit.Assert.assertEquals
import org.junit.Test

/** 점검 보고서 「테스트가 없는 점수·순위 로직」의 TripRepository 항목(getAllTrips 필터, assignUnassignedTrips, getStreak 범위 L3). */
class TripQueriesTest {
    private fun trip(date: String, score: Double, km: Double = 10.0, eligible: Boolean = km >= 3.0, league: String? = "ioniq5") =
        CompletedTrip(Trip(date, score, km), emptyList(), emptyList(), "8:00", "8:30", eligible, league)

    private val trips = listOf(
        trip("2026-11-01", 80.0, league = null),        // 차종을 고르기 전
        trip("2026-11-02", 85.0),
        trip("2026-11-03", 0.0, km = 2.9),              // 3km 미만: 제외
        trip("2026-11-04", 90.0, league = "ev6"),       // 다른 차종 리그
        trip("2026-11-05", 75.0, km = 3.0, eligible = false) // 저장된 판정이 제외면 제외
    )

    @Test fun eligibleFiltersByLeagueAndDistance() {
        assertEquals(listOf("2026-11-02"), TripQueries.eligible(trips, "ioniq5").map { it.date })
        assertEquals(listOf("2026-11-04"), TripQueries.eligible(trips, "ev6").map { it.date })
        assertEquals(listOf("2026-11-01", "2026-11-02", "2026-11-04"), TripQueries.eligible(trips).map { it.date })
    }

    @Test fun assigningLeagueOnlyFillsTripsWithoutOne() {
        val assigned = trips.map { TripQueries.withLeague(it, "ioniq5") }
        assertEquals(listOf("ioniq5", "ioniq5", "ioniq5", "ev6", "ioniq5"), assigned.map { it.leagueKey })
        assertEquals(listOf("2026-11-01", "2026-11-02"), TripQueries.eligible(assigned, "ioniq5").map { it.date })
    }

    @Test fun streakCountsEveryLeague() {
        // L3: 차종을 바꿔도 연속 기록은 사람 기준. 11-01(리그 없음)·11-02·11-04(다른 리그) = 3일
        assertEquals(3, TripQueries.streak(trips, "2026-11-05"))
        assertEquals(2, TripQueries.streak(trips, "2026-11-03"))
    }

    @Test fun shortTripDoesNotBreakOrExtendTheStreak() {
        val withShortLow = trips + trip("2026-11-06", 0.0, km = 1.0)
        assertEquals(3, TripQueries.streak(withShortLow, "2026-11-06"))
    }
}
