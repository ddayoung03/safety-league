package io.github.ddayoung03.safetyleague.core.calculator

import io.github.ddayoung03.safetyleague.core.model.Trip
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth

data class SeasonPoints(
    val season: YearMonth,
    val tier: String,
    val km: Double,
    val average: Double?,
    val bonusPercent: Int,
    val multiplierPercent: Int,
    val points: Int
)

/** 월 정산 포인트 = 거리 포인트(1km당 1P, 월 1,000km까지) × (1 + 점수 가산) × 티어 배율 */
object PointCalculator {
    private const val MAX_KM = 1000.0
    private val multipliers = mapOf("브론즈" to 100, "실버" to 120, "골드" to 150, "플래티넘" to 200, "다이아" to 250)

    fun bonusPercent(average: Double?): Int = when {
        average == null || average < 70.0 -> 0
        average < 80.0 -> 10
        average < 90.0 -> 30
        else -> 50
    }

    fun multiplierPercent(tier: String): Int = multipliers[tier] ?: 100

    /** 곱셈을 모두 끝낸 뒤 한 번만 반올림한다. 0.5는 올린다. */
    fun points(km: Double, average: Double?, tier: String): Int =
        BigDecimal.valueOf(minOf(km, MAX_KM))
            .multiply(BigDecimal(100 + bonusPercent(average)))
            .multiply(BigDecimal(multiplierPercent(tier)))
            .divide(BigDecimal(10_000))
            .setScale(0, RoundingMode.HALF_UP).toInt()

    /** 유효 트립과 평균은 시즌 순위(SeasonRanking)와 같은 기준이다. */
    fun forSeason(trips: List<Trip>, season: YearMonth, tier: String): SeasonPoints {
        val from = season.atDay(1).toString()
        val to = season.atEndOfMonth().toString()
        val eligible = trips.filter { it.km >= 3.0 && it.date in from..to }
        val km = eligible.sumOf { it.km }
        val average = if (eligible.isEmpty()) null else LeagueCalculator.average(eligible, from, to)
        return SeasonPoints(season, tier, km, average, bonusPercent(average), multiplierPercent(tier), points(km, average, tier))
    }
}
