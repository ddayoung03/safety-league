package io.github.ddayoung03.safetyleague.ui

import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import io.github.ddayoung03.safetyleague.R
import io.github.ddayoung03.safetyleague.core.calculator.LeagueCalculator
import io.github.ddayoung03.safetyleague.core.calculator.WeeklyReportCalculator
import io.github.ddayoung03.safetyleague.core.data.DemoData
import io.github.ddayoung03.safetyleague.databinding.ViewReportBinding
import io.github.ddayoung03.safetyleague.ui.view.AsphaltDrawable
import java.time.LocalDate
import kotlin.math.roundToInt

internal fun MainActivity.showReport() {
    val b = ViewReportBinding.inflate(layoutInflater, binding.screenContainer, true)
    b.root.background = AsphaltDrawable(this)
    if (!demoMode && demoResultWin == null) { showRealReport(b); return }
    val from = LocalDate.parse(DemoData.today).minusDays(6) // 기준일까지 7일: 11.03 ~ 11.09
    val week = (0..6).map { from.plusDays(it.toLong()).toString() }
    val scores = week.map { d -> DemoData.trips.firstOrNull { it.date == d }?.score?.roundToInt() }
    val avg = DemoData.average(week.first(), week.last())
    val last = DemoData.average(from.minusDays(7).toString(), from.minusDays(1).toString())
    val ranks = week.map { DemoData.ranks.getValue(it) }
    val line = LeagueCalculator.promotionRank(DemoData.league.total)
    val lineAvg = DemoData.neighbors().first { it.rank == line }.avg

    b.tvReportTitle.text = "11월 2주차 리포트"
    b.tvReportRange.text = "11.03 ~ 11.09 · 주행 ${scores.count { it != null }}일"
    b.tvWeekAvg.text = String.format("%.1f", avg)
    b.tvWeekChange.text = "지난주보다 ${if (avg >= last) "+" else ""}${LeagueCalculator.round1(avg - last)}"
    b.tvLastWeek.text = "지난주 ${last}점"
    val move = ranks.first() - ranks.last()
    b.tvRankMove.text = "P${ranks.first()} → P${ranks.last()} ${if (move >= 0) "▲$move" else "▼${-move}"}"
    b.tvNextPlan.text = "깜빡이 누락만 줄이면 승급 라인까지 ${ranks.last() - line}계단, 평균 +${LeagueCalculator.round1(lineAvg - DemoData.seasonAverage(week.last()))}점이면 돼요"
    b.positionChart.promotionLine = line
    // 11.03(화) ~ 11.09(월)이라 요일은 날짜에서 계산한다
    val days = week.map { weekdayNames[LocalDate.parse(it).dayOfWeek.value - 1] }
    b.positionChart.days = days
    b.positionChart.ranks = ranks
    scoreBars(b.layoutScoreBars, scores, days)
}

/** 실제 모드: 이번 주(월~일) 리그 트립으로 채운다. 트립이 없으면 빈 상태. */
internal fun MainActivity.showRealReport(b: ViewReportBinding) {
    val r = WeeklyReportCalculator.calculate(leagueTrips(), LocalDate.now(), myTier)
    b.btnReportBack.visibility = View.VISIBLE
    b.btnReportBack.setOnClickListener { show(Screen.LEAGUE) }
    b.tvReportTitle.text = WeeklyReportCalculator.title(r.monday)
    b.tvReportRange.text = "${WeeklyReportCalculator.range(r.monday, r.sunday)} · 주행 ${r.drivingDays}일" +
        if (r.km > 0.0) " · ${r.km}km" else ""
    b.tvWeekAvg.text = r.average?.let { String.format("%.1f", it) } ?: "–"
    b.tvWeekChange.visibility = if (r.change == null) View.GONE else View.VISIBLE
    r.change?.let { b.tvWeekChange.text = "지난주보다 ${if (it >= 0.0) "+" else ""}$it" }
    b.tvLastWeek.text = r.lastWeekAverage?.let { "지난주 ${it}점" } ?: "지난주 기록 없음"
    b.tvRankCaption.text = "순위 변동 · $myTier 리그"
    val move = WeeklyReportCalculator.rankMove(r.dailyRanks)
    b.tvRankMove.text = move ?: "순위 대기"
    b.positionChart.promotionLine = r.standing.promotionRank
    b.positionChart.ranks = r.dailyRanks
    b.positionChart.visibility = if (move == null) View.INVISIBLE else View.VISIBLE
    b.tvNextPlan.text = WeeklyReportCalculator.nextPlan(r)
    scoreBars(b.layoutScoreBars, r.dailyScores)
}

private val weekdayNames = listOf("월", "화", "수", "목", "금", "토", "일")

internal fun MainActivity.scoreBars(container: LinearLayout, scores: List<Int?>, names: List<String> = weekdayNames) {
    val best = scores.filterNotNull().maxOrNull()
    container.removeAllViews()
    names.forEachIndexed { i, day ->
        val s = scores[i]
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply { setMargins(dp(7), 0, dp(7), 0) }
        }
        // 칸 230dp = 숫자 약 34 + 막대 최대 150 + 요일 약 34 (막대가 요일을 밀어내지 않게)
        col.addView(text(s?.toString() ?: "휴식", if (s == null) R.color.grey else R.color.white, number = s != null).apply { gravity = Gravity.CENTER })
        col.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                if (s == null) dp(4) else dp(((s - 60) / 40f * 150).roundToInt().coerceIn(16, 150)))
            setBackgroundColor(when { s == null -> color(R.color.bar_rest); s == best -> color(R.color.yellow); else -> color(R.color.bar_normal) })
        })
        col.addView(text(day, R.color.grey).apply { gravity = Gravity.CENTER; setPadding(0, dp(6), 0, 0) })
        container.addView(col)
    }
}
