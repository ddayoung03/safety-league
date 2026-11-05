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

internal fun MainActivity.rankChange(trip: CompletedTrip): String {
    val now = standing().rank ?: return "순위 대기"
    val before = standing(tripsBefore(trip)).rank ?: return "첫 순위"
    val diff = before - now
    return when { diff > 0 -> "▲$diff"; diff < 0 -> "▼${-diff}"; else -> "변동 없음" }
}

internal fun MainActivity.promotionText(s: SeasonStanding): String = when {
    s.rank == null -> "첫 주행을 마치면 순위가 나와요"
    s.stepsToPromotion == 0 -> "승급권에 진입했어요"
    else -> "승급까지 ${s.stepsToPromotion}계단 · ${s.pointsToPromotion}점"
}

internal fun MainActivity.realRows(container: LinearLayout, s: SeasonStanding, change: String = "") {
    container.removeAllViews()
    if (s.rank == null) { container.addView(note("첫 주행을 마치면 순위가 나와요")); return }
    for (r in s.neighbors) {
        container.addView(towerRow(r, r.name == "나", nameSuffix = if (r.name == "나") " $change" else ""))
        val gap = LeagueCalculator.round1(r.avg - s.average!!)
        if (r.rank == s.rank - 1 && gap > 0.0) container.addView(note("앞 순위까지 ${gap}점"))
    }
}
