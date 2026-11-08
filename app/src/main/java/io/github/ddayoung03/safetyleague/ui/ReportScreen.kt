package io.github.ddayoung03.safetyleague.ui

import android.animation.ValueAnimator
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import androidx.core.view.children
import io.github.ddayoung03.safetyleague.R
import io.github.ddayoung03.safetyleague.core.calculator.LeagueCalculator
import io.github.ddayoung03.safetyleague.core.calculator.WeeklyReportCalculator
import io.github.ddayoung03.safetyleague.core.data.DemoData
import io.github.ddayoung03.safetyleague.databinding.ViewReportBinding
import io.github.ddayoung03.safetyleague.ui.motion.MotionTokens
import io.github.ddayoung03.safetyleague.ui.motion.animationsEnabled
import io.github.ddayoung03.safetyleague.ui.motion.countUp
import io.github.ddayoung03.safetyleague.ui.motion.settleShown
import io.github.ddayoung03.safetyleague.ui.motion.skippableSequence
import java.time.LocalDate
import kotlin.math.roundToInt
import io.github.ddayoung03.safetyleague.ui.view.Pose

internal fun MainActivity.showReport() {
    val b = ViewReportBinding.inflate(layoutInflater, binding.screenContainer, true)
    b.rabbit.pose = Pose.CLIP
    b.checkerStrip.background = checkerTile()
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
    b.btnReportBack.visibility = View.VISIBLE
    b.btnReportBack.pressFeedback()
    b.btnReportBack.setOnClickListener { show(Screen.HOME) }
    playReport(b, avg)
}

/** 실제 모드: 이번 주(월~일) 리그 트립으로 채운다. 트립이 없으면 빈 상태. */
internal fun MainActivity.showRealReport(b: ViewReportBinding) {
    val r = WeeklyReportCalculator.calculate(leagueTrips(), LocalDate.now(), myTier)
    b.btnReportBack.visibility = View.VISIBLE
    b.btnReportBack.pressFeedback()
    b.btnReportBack.setOnClickListener { show(Screen.HOME) }
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
    r.average?.let { playReport(b, it) } // 트립이 없는 주는 빈 상태 그대로 둔다
}

/**
 * 막대가 자라고 순위 선이 그려지는 동안 평균이 올라가고, 지난주 비교와 순위 변동이 뒤따른다.
 * 뷰에는 이미 최종 값이 들어 있고, 화면을 누르거나 화면이 바뀌면 motion.finishAll()이 최종 상태를 맞춘다.
 */
private fun MainActivity.playReport(b: ViewReportBinding, average: Double) {
    if (!animationsEnabled()) return
    val columns = b.layoutScoreBars.children.map { it as ViewGroup }.toList()
    val numbers = columns.map { it.getChildAt(0) }
    val bars = columns.map { it.getChildAt(1) }
    val late = listOf(b.tvWeekChange, b.tvLastWeek, b.tvRankMove).filter { it.visibility == View.VISIBLE }
    val draw = ValueAnimator.ofFloat(0f, 1f).setDuration(MotionTokens.COUNT)
    val seq = skippableSequence(b.root) {
        draw.cancel()
        b.positionChart.progress = 1f
        (numbers + bars + late).forEach { it.settleShown() }
    }
    val curve = DecelerateInterpolator(2f)
    fun pop(v: View) {
        v.animate().alpha(1f).translationX(0f).setDuration(MotionTokens.BASE).setInterpolator(curve).start()
    }

    b.positionChart.progress = 0f
    val fromX = -dp(26).toFloat()
    for (v in late) { v.alpha = 0f; v.translationX = fromX }
    // 막대가 자라는 동안 숫자는 막대 머리에 얹혀 함께 올라간다
    bars.forEachIndexed { i, bar ->
        val number = numbers[i]
        bar.scaleY = 0f
        number.alpha = 0f
        number.translationY = bar.layoutParams.height.toFloat()
        seq.at(MotionTokens.BASE + i * MotionTokens.STAGGER) {
            bar.animate().scaleY(1f).setDuration(MotionTokens.SLOW).setInterpolator(curve).start()
            number.animate().alpha(1f).translationY(0f).setDuration(MotionTokens.SLOW).setInterpolator(curve).start()
        }
    }
    b.tvWeekAvg.countUp((average * 10).roundToInt(), MotionTokens.COUNT, motion) { "${it / 10}.${it % 10}" }

    draw.addUpdateListener { b.positionChart.progress = it.animatedValue as Float }
    val lineAt = MotionTokens.BASE * 2
    seq.at(lineAt) { draw.start() }
    seq.at(MotionTokens.COUNT) { pop(b.tvWeekChange) }
    seq.at(MotionTokens.COUNT + MotionTokens.STAGGER) { pop(b.tvLastWeek) }
    val lineEnd = lineAt + MotionTokens.COUNT
    seq.at(lineEnd) { pop(b.tvRankMove) }
    seq.endAt(lineEnd + MotionTokens.BASE)
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
        col.addView(text(s?.toString() ?: "휴식", if (s == null) R.color.ink_muted else R.color.ink_text, number = s != null).apply { gravity = Gravity.CENTER })
        val barHeight = if (s == null) dp(4) else dp(((s - 60) / 40f * 150).roundToInt().coerceIn(16, 150))
        col.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, barHeight)
            pivotY = barHeight.toFloat() // 바닥에서 자란다
            background = skew(when { s == null -> R.color.ink_glow; s == best -> R.color.yellow; else -> R.color.ink_muted })
        })
        col.addView(text(day, R.color.ink_muted).apply { gravity = Gravity.CENTER; setPadding(0, dp(6), 0, 0) })
        container.addView(col)
    }
}
