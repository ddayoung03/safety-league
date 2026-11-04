package io.github.ddayoung03.safetyleague.core.calculator

import io.github.ddayoung03.safetyleague.core.data.SeasonDrivers
import io.github.ddayoung03.safetyleague.core.model.DriverRank
import io.github.ddayoung03.safetyleague.core.model.Trip
import java.time.LocalDate

data class SeasonStanding(
    val tier: String,
    val rank: Int?,
    val average: Double?,
    val stepsToPromotion: Int,
    val pointsToPromotion: Double,
    val neighbors: List<DriverRank>,
    val promotionRank: Int = LeagueCalculator.promotionRank(397),
    val demotionRank: Int = LeagueCalculator.demotionRank(397)
)

object SeasonRanking {
    fun calculate(trips: List<Trip>, today: LocalDate, tier: String): SeasonStanding {
        val from = today.withDayOfMonth(1).toString()
        val to = today.withDayOfMonth(today.lengthOfMonth()).toString()
        val eligible = trips.filter { it.km >= 3.0 && it.date in from..to }
        if (eligible.isEmpty()) return SeasonStanding(tier, null, null, 0, 0.0, emptyList())
        val average = LeagueCalculator.average(eligible, from, to)
        // 동점은 기존 가상 운전자 뒤. 표시 평균과 정렬 평균이 같고 실행마다 순서가 같다.
        val board = (SeasonDrivers.drivers + DriverRank(0, "나", average))
            .sortedByDescending { it.avg }.mapIndexed { index, driver -> driver.copy(rank = index + 1) }
        val rank = board.first { it.name == "나" }.rank
        val line = LeagueCalculator.promotionRank(board.size)
        return SeasonStanding(tier, rank, average, (rank - line).coerceAtLeast(0),
            LeagueCalculator.round1((board[line - 1].avg - average).coerceAtLeast(0.0)),
            board.filter { it.rank in (rank - 2)..(rank + 2) })
    }
}
