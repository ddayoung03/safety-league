package io.github.ddayoung03.safetyleague.ui

import android.widget.LinearLayout
import io.github.ddayoung03.safetyleague.core.calculator.LeagueCalculator
import io.github.ddayoung03.safetyleague.core.calculator.SeasonRanking
import io.github.ddayoung03.safetyleague.core.calculator.SeasonStanding
import io.github.ddayoung03.safetyleague.core.model.CompletedTrip
import java.time.LocalDate

internal fun MainActivity.leagueTrips() = repository.getAllTrips(profileStore.load()?.leagueKey)
internal fun MainActivity.standing(trips: List<io.github.ddayoung03.safetyleague.core.model.Trip> = leagueTrips()) =
    SeasonRanking.calculate(trips, LocalDate.now(), myTier)

internal fun MainActivity.tripsBefore(trip: CompletedTrip, trips: List<io.github.ddayoung03.safetyleague.core.model.Trip> = leagueTrips()) = trips.toMutableList().apply {
    if (trip.isEligible) { val index = indexOfLast { it == trip.trip }; if (index >= 0) removeAt(index) }
}

/** 이번 트립 전후 순위 (전, 후). 둘 중 하나라도 없으면 null. */
internal fun MainActivity.rankMove(trip: CompletedTrip): Pair<Int, Int>? {
    val now = standing().rank ?: return null
    val before = standing(tripsBefore(trip)).rank ?: return null
    return before to now
}

internal fun MainActivity.rankChange(trip: CompletedTrip): String {
    if (standing().rank == null) return "순위 대기"
    val (before, now) = rankMove(trip) ?: return "첫 순위"
    val diff = before - now
    return when { diff > 0 -> "▲$diff"; diff < 0 -> "▼${-diff}"; else -> "변동 없음" }
}

internal fun MainActivity.promotionText(s: SeasonStanding): String = when {
    s.rank == null -> "첫 주행을 마치면 순위가 나와요"
    s.stepsToPromotion == 0 -> "승급권에 진입했어요"
    else -> "승급까지 ${s.stepsToPromotion}계단 · ${s.pointsToPromotion}점"
}

/** 결과 카드 순위표 아래 한 줄. 앞 순위가 없으면 빈 글자. */
internal fun towerNote(s: SeasonStanding): String {
    val above = s.neighbors.firstOrNull { it.rank == (s.rank ?: 0) - 1 }
    val gap = above?.let { LeagueCalculator.round1(it.avg - s.average!!) } ?: 0.0
    return if (s.rank == null) "첫 주행을 마치면 순위가 나와요" else if (gap > 0.0) "앞 순위까지 ${gap}점" else ""
}

internal fun MainActivity.realRows(container: LinearLayout, s: SeasonStanding) {
    container.removeAllViews()
    if (s.rank == null) { container.addView(note("첫 주행을 마치면 순위가 나와요")); return }
    for (r in s.neighbors) {
        container.addView(towerRow(r, r.name == "나"))
        val gap = LeagueCalculator.round1(r.avg - s.average!!)
        if (r.rank == s.rank - 1 && gap > 0.0) container.addView(note("앞 순위까지 ${gap}점"))
    }
}
