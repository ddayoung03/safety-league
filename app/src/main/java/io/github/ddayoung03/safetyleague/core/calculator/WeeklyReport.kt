package io.github.ddayoung03.safetyleague.core.calculator

import io.github.ddayoung03.safetyleague.core.model.DetailScore
import io.github.ddayoung03.safetyleague.core.model.Trip
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import kotlin.math.roundToInt

/** 점수 항목 한 개의 이번 주 평균과 지난주 대비 변화. 지난주 기록이 없으면 change는 null. */
data class CategoryChange(val key: String, val max: Double, val average: Double, val change: Double?)

/**
 * 주간 리포트 화면 값. 한 주는 월요일~일요일.
 * @param dailyScores 월~일 그날 트립 평균(반올림). 주행이 없는 날은 null
 * @param dailyRanks 월~일 그날까지의 시즌 순위. 순위가 없는 날(시즌 첫 트립 전, 오늘 이후)은 null
 * @param focus 다음 주에 신경 쓸 항목 key. 세부 점수가 없으면 null
 */
data class WeeklyReport(
    val monday: LocalDate,
    val sunday: LocalDate,
    val dailyScores: List<Int?>,
    val drivingDays: Int,
    val average: Double?,
    val lastWeekAverage: Double?,
    val change: Double?,
    val km: Double,
    val categories: List<CategoryChange>,
    val dailyRanks: List<Int?>,
    val standing: SeasonStanding,
    val focus: String?
) {
    val isEmpty get() = average == null
}

object WeeklyReportCalculator {

    private val week = WeekFields.of(DayOfWeek.MONDAY, 1)

    /** 항목 key, 만점, 꺼내는 법 (배점 순서) */
    private val categoryDefs: List<Triple<String, Double, (DetailScore) -> Double>> = listOf(
        Triple("smoothness", 30.0, DetailScore::smoothness),
        Triple("manner", 25.0, DetailScore::manner),
        Triple("eco", 20.0, DetailScore::eco),
        Triple("safety", 15.0, DetailScore::safety),
        Triple("consistency", 10.0, DetailScore::consistency)
    )

    /** trips는 이미 리그·인정 거리로 거른 트립. 3km 미만은 여기서도 뺀다. */
    fun calculate(trips: List<Trip>, today: LocalDate, tier: String): WeeklyReport {
        val eligible = trips.filter { it.km >= 3.0 && it.date <= today.toString() }
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val sunday = monday.plusDays(6)
        val days = (0..6).map { monday.plusDays(it.toLong()) }
        val thisWeek = eligible.filter { it.date in monday.toString()..sunday.toString() }
        val lastWeek = eligible.filter { it.date in monday.minusDays(7).toString()..monday.minusDays(1).toString() }

        val dailyScores = days.map { d ->
            thisWeek.filter { it.date == d.toString() }.map { it.score }.takeIf { it.isNotEmpty() }?.average()?.roundToInt()
        }
        val average = avg(thisWeek)
        val lastAverage = avg(lastWeek)
        val change = if (average != null && lastAverage != null) LeagueCalculator.round1(average - lastAverage) else null
        val categories = categoryChanges(thisWeek, lastWeek)
        val dailyRanks = days.map { d ->
            if (d > today) null else SeasonRanking.calculate(eligible.filter { it.date <= d.toString() }, d, tier).rank
        }
        return WeeklyReport(
            monday, sunday, dailyScores, dailyScores.count { it != null }, average, lastAverage, change,
            LeagueCalculator.round1(thisWeek.sumOf { it.km }), categories, dailyRanks,
            SeasonRanking.calculate(eligible, today, tier), focus(categories)
        )
    }

    /** "11월 2주차 리포트". 월요일이 속한 달, 1일이 든 주가 1주차. */
    fun title(monday: LocalDate) = "${monday.monthValue}월 ${monday.get(week.weekOfMonth())}주차 리포트"

    /** "11.03 ~ 11.09" */
    fun range(monday: LocalDate, sunday: LocalDate) =
        "%02d.%02d ~ %02d.%02d".format(monday.monthValue, monday.dayOfMonth, sunday.monthValue, sunday.dayOfMonth)

    /** "P131 → P125 ▲6". 순위가 하나도 없으면 null */
    fun rankMove(ranks: List<Int?>): String? {
        val known = ranks.filterNotNull()
        if (known.isEmpty()) return null
        val first = known.first()
        val last = known.last()
        if (known.size == 1) return "P$last"
        val move = first - last
        return "P$first → P$last ${when { move > 0 -> "▲$move"; move < 0 -> "▼${-move}"; else -> "변동 없음" }}"
    }

    /** 하단 「다음 주 작전」 문구 */
    fun nextPlan(r: WeeklyReport): String {
        if (r.isEmpty) return "이번 주 기록이 아직 없어요. 3km 이상 달리면 리포트가 채워져요"
        val action = when (r.focus) {
            "smoothness" -> "급가속·급제동만 줄이면 "
            "manner" -> "깜빡이 누락만 줄이면 "
            "eco" -> "페달만 부드럽게 밟으면 "
            "safety" -> "벨트와 경고만 챙기면 "
            "consistency" -> "꾸준히만 달리면 "
            else -> ""
        }
        val s = r.standing
        return when {
            s.rank == null -> "${action}이번 시즌 첫 주행을 마치면 순위가 나와요"
            s.stepsToPromotion == 0 -> "${action}승급권을 지킬 수 있어요"
            else -> "${action}승급 라인까지 ${s.stepsToPromotion}계단, 평균 +${s.pointsToPromotion}점이면 돼요"
        }
    }

    private fun avg(trips: List<Trip>): Double? =
        if (trips.isEmpty()) null else LeagueCalculator.round1(trips.sumOf { it.score } / trips.size)

    private fun categoryChanges(thisWeek: List<Trip>, lastWeek: List<Trip>): List<CategoryChange> {
        val now = thisWeek.mapNotNull { it.detailScore }
        val before = lastWeek.mapNotNull { it.detailScore }
        if (now.isEmpty()) return emptyList()
        return categoryDefs.map { (key, max, pick) ->
            val a = LeagueCalculator.round1(now.map(pick).average())
            val b = before.takeIf { it.isNotEmpty() }?.let { LeagueCalculator.round1(it.map(pick).average()) }
            CategoryChange(key, max, a, b?.let { LeagueCalculator.round1(a - it) })
        }
    }

    /** 지난주보다 만점 대비 가장 많이 떨어진 항목. 떨어진 항목이 없으면 만점 대비 가장 낮은 항목. 같으면 배점 순서. */
    private fun focus(categories: List<CategoryChange>): String? {
        if (categories.isEmpty()) return null
        val dropped = categories.filter { (it.change ?: 0.0) < 0.0 }
        return (dropped.minByOrNull { it.change!! / it.max } ?: categories.minBy { it.average / it.max }).key
    }
}
