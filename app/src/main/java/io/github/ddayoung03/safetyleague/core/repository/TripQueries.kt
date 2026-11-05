package io.github.ddayoung03.safetyleague.core.repository

import io.github.ddayoung03.safetyleague.core.calculator.LeagueCalculator
import io.github.ddayoung03.safetyleague.core.model.CompletedTrip
import io.github.ddayoung03.safetyleague.core.model.Trip

/** [TripRepository]의 판단만 모은 순수 함수. 파일·JSON 없이 단위 테스트한다. */
object TripQueries {
    /** 점수 집계에 쓰는 실제 유효 트립. [leagueKey]가 null이면 모든 리그(연속 기록처럼 사람 기준). */
    fun eligible(trips: List<CompletedTrip>, leagueKey: String? = null): List<Trip> = trips.filter {
        it.isEligible && it.trip.km >= 3.0 && (leagueKey == null || it.leagueKey == leagueKey)
    }.map { it.trip }

    /** 차종을 정하기 전 트립(리그 없음)만 그 리그에 넣는다. 이미 리그가 있는 트립은 그대로. */
    fun withLeague(trip: CompletedTrip, leagueKey: String): CompletedTrip =
        if (trip.leagueKey == null) trip.copy(leagueKey = leagueKey) else trip

    /** 연속 기록은 리그와 상관없이 모든 유효 트립으로 센다. */
    fun streak(trips: List<CompletedTrip>, untilDate: String): Int = LeagueCalculator.streakAt(eligible(trips), untilDate)
}
