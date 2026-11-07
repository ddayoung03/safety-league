package io.github.ddayoung03.safetyleague.core.calculator

import io.github.ddayoung03.safetyleague.core.model.Trip
import kotlin.math.roundToInt

/** 시상대 화면이 보여 줄 정산 결과. [seasons]가 2 이상이면 달마다 티어가 달라 내역 대신 합계만 쓴다. */
data class SettlementCard(
    val seasonLabel: String,
    val tier: String,
    val points: Int,
    val balance: Int,
    val km: Int,
    val average: Double?,
    val bonusPercent: Int,
    val multiplierPercent: Int,
    val seasons: Int,
    val promotedTo: String?,
    val message: String
) {
    fun encode(): String = listOf(seasonLabel, tier, points, balance, km, average ?: "", bonusPercent,
        multiplierPercent, seasons, promotedTo.orEmpty(), message).joinToString("\n")

    companion object {
        /** 지급할 포인트가 없으면 null. 시즌마다 그 시즌을 달린 티어로 계산한다. */
        fun of(trips: List<Trip>, results: List<SeasonResult>, balanceBefore: Int): SettlementCard? {
            val parts = results.map { PointCalculator.forSeason(trips, it.season, it.fromTier) }
            val points = parts.sumOf { it.points }
            val summary = TierMovement.summarize(results)
            if (points == 0 || summary == null) return null
            val last = parts.last()
            return SettlementCard(
                if (parts.size > 1) "${parts.size}개 시즌 합산" else "${last.season.monthValue}월 시즌",
                last.tier, points, balanceBefore + points, parts.sumOf { minOf(it.km, PointCalculator.MAX_KM) }.roundToInt(), last.average,
                last.bonusPercent, last.multiplierPercent, parts.size,
                summary.toTier.takeIf { summary.move == TierMove.PROMOTED }, TierMovement.message(summary)
            )
        }

        fun decode(text: String?): SettlementCard? = runCatching {
            val f = text!!.split("\n")
            SettlementCard(f[0], f[1], f[2].toInt(), f[3].toInt(), f[4].toInt(), f[5].toDoubleOrNull(),
                f[6].toInt(), f[7].toInt(), f[8].toInt(), f[9].ifEmpty { null }, f[10])
        }.getOrNull()
    }
}
