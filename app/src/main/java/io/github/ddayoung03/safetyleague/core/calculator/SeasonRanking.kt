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
    val demotionRank: Int = LeagueCalculator.demotionRank(397),
    val total: Int = 397
)

object SeasonRanking {
    fun calculate(
        trips: List<Trip>, today: LocalDate, tier: String,
        drivers: List<DriverRank> = SeasonDrivers.drivers, radius: Int = 2
    ): SeasonStanding {
        val from = today.withDayOfMonth(1).toString()
        val to = today.withDayOfMonth(today.lengthOfMonth()).toString()
        val eligible = trips.filter { it.km >= 3.0 && it.date in from..to }
        if (eligible.isEmpty()) return SeasonStanding(tier, null, null, 0, 0.0, emptyList(),
            total = drivers.size + 1)
        return rank(LeagueCalculator.average(eligible, from, to), tier, drivers, radius)
    }

    /** 내 시즌 평균을 가상 운전자 목록 사이에 넣어 순위를 매긴다. 차종·지역·친구 순위표가 함께 쓴다. */
    fun rank(average: Double, tier: String, drivers: List<DriverRank>, radius: Int = 2): SeasonStanding {
        // 동점은 기존 가상 운전자 뒤. 표시 평균과 정렬 평균이 같고 실행마다 순서가 같다.
        val board = (drivers + DriverRank(0, "나", average))
            .sortedByDescending { it.avg }.mapIndexed { index, driver -> driver.copy(rank = index + 1) }
        val rank = board.first { it.name == "나" }.rank
        // 작은 리그(친구 등): 승급선은 최소 P1, 강등선은 승급선 뒤(1명이면 P2 = 강등 없음)
        val line = LeagueCalculator.promotionRank(board.size).coerceAtLeast(1)
        val demotion = LeagueCalculator.demotionRank(board.size).coerceAtLeast(line + 1)
        return SeasonStanding(tier, rank, average, (rank - line).coerceAtLeast(0),
            LeagueCalculator.round1((board[line - 1].avg - average).coerceAtLeast(0.0)),
            board.filter { it.rank in (rank - radius)..(rank + radius) },
            line, demotion, board.size)
    }
}
