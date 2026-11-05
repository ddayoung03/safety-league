package io.github.ddayoung03.safetyleague.core.calculator

import io.github.ddayoung03.safetyleague.core.data.SeasonDrivers
import io.github.ddayoung03.safetyleague.core.model.Trip
import java.time.YearMonth

enum class TierMove { PROMOTED, STAYED, DEMOTED }

data class SeasonResult(
    val season: YearMonth,
    val fromTier: String,
    val toTier: String,
    val move: TierMove,
    val rank: Int?
)

/** 월간 시즌 종료 티어 이동. 승급은 같은 티어 상위 30%, 강등은 하위 30%. */
object TierMovement {
    val tiers = listOf("브론즈", "실버", "골드", "플래티넘", "다이아")

    /** 순위가 없거나(이번 시즌 유효 트립 없음) 인원 범위 밖이면 유지. 브론즈 아래, 다이아 위로는 움직이지 않는다. */
    fun move(tier: String, rank: Int?, total: Int): TierMove {
        val index = tiers.indexOf(tier)
        if (rank == null || rank !in 1..total || index < 0) return TierMove.STAYED
        return when {
            rank <= LeagueCalculator.promotionRank(total) && index < tiers.lastIndex -> TierMove.PROMOTED
            rank >= LeagueCalculator.demotionRank(total) && index > 0 -> TierMove.DEMOTED
            else -> TierMove.STAYED
        }
    }

    fun apply(tier: String, move: TierMove): String {
        val index = tiers.indexOf(tier)
        if (index < 0) return tier
        return when (move) {
            TierMove.PROMOTED -> tiers[(index + 1).coerceAtMost(tiers.lastIndex)]
            TierMove.DEMOTED -> tiers[(index - 1).coerceAtLeast(0)]
            TierMove.STAYED -> tier
        }
    }

    /** 한 시즌 정산. 순위는 시즌 순위 계산(SeasonRanking)을 그대로 쓴다. */
    fun settle(trips: List<Trip>, season: YearMonth, tier: String): SeasonResult {
        val rank = SeasonRanking.calculate(trips, season.atEndOfMonth(), tier).rank
        val move = move(tier, rank, SeasonDrivers.drivers.size + 1)
        return SeasonResult(season, tier, apply(tier, move), move, rank)
    }

    /** 마지막으로 본 시즌부터 지금 시즌 직전까지 차례로 정산한다. 지금 시즌이 같거나 이전이면 빈 목록. */
    fun settleSince(trips: List<Trip>, lastSeason: YearMonth, current: YearMonth, tier: String): List<SeasonResult> {
        val results = mutableListOf<SeasonResult>()
        var season = lastSeason
        var t = tier
        while (season.isBefore(current)) {
            val r = settle(trips, season, t)
            results += r
            t = r.toTier
            season = season.plusMonths(1)
        }
        return results
    }

    /** 여러 시즌을 건너뛰었으면 처음 티어와 마지막 티어로 한 줄 결과를 만든다. */
    fun summarize(results: List<SeasonResult>): SeasonResult? {
        if (results.isEmpty()) return null
        val from = results.first().fromTier
        val to = results.last().toTier
        val diff = tiers.indexOf(to) - tiers.indexOf(from)
        val move = when { diff > 0 -> TierMove.PROMOTED; diff < 0 -> TierMove.DEMOTED; else -> TierMove.STAYED }
        return results.last().copy(fromTier = from, move = move)
    }

    /** 새 시즌 첫 화면 안내. 강등도 격려하는 말투로. */
    fun message(r: SeasonResult): String {
        val season = "${r.season.monthValue}월 시즌"
        return when {
            r.move == TierMove.PROMOTED -> "$season 결과 · ${r.toTier} 리그로 승급했어요. 축하해요!"
            r.move == TierMove.DEMOTED -> "$season 결과 · ${r.toTier} 리그에서 새로 출발해요. 차근차근 다시 올라가 봐요"
            r.rank == null -> "${season}은 기록이 없어 ${r.toTier} 리그를 유지해요. 이번 달 첫 주행을 기다릴게요"
            r.toTier == tiers.last() -> "$season 결과 · ${r.toTier} 리그를 지켰어요. 이번 달도 지금처럼 달려요"
            else -> "$season 결과 · ${r.toTier} 리그를 지켰어요. 이번 달엔 승급권에 도전해 봐요"
        }
    }
}
