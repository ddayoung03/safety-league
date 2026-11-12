package io.github.ddayoung03.safetyleague.ui

import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import io.github.ddayoung03.safetyleague.R
import io.github.ddayoung03.safetyleague.core.calculator.ScoreBreakdown
import io.github.ddayoung03.safetyleague.core.data.DemoData
import io.github.ddayoung03.safetyleague.databinding.ViewScoreDetailBinding
import kotlin.math.roundToInt

/** 19.5는 그대로, 25.0은 25로 적는다. */
internal fun pointText(v: Double) = if (v == Math.rint(v)) "${v.toInt()}" else "$v"

/** 결과 카드의 점수가 어떻게 나왔는지 항목별로 보여 준다. 결과 카드와 같은 트립(없으면 시연 카드)을 쓴다. */
internal fun MainActivity.showScoreDetail() {
    val b = ViewScoreDetailBinding.inflate(layoutInflater, binding.screenContainer, true)
    b.checkerStrip.background = checkerTile()
    b.btnBack.pressFeedback()
    b.btnBack.setOnClickListener { backToResult() }

    val t = realTrip
    val win = demoResultWin == true
    val detail = if (t != null) t.trip.detailScore else if (win) DemoData.winDayDetail else DemoData.lowDayDetail
    val events = t?.events ?: if (win) DemoData.winDayEvents else DemoData.lowDayEvents
    val km = t?.trip?.km ?: if (win) DemoData.winDayKm else DemoData.lowDayKm
    b.tvTotal.text = "${t?.trip?.score?.roundToInt() ?: detail?.total?.roundToInt() ?: 0}"
    if (detail == null) { b.tvScoreNote.text = "이 주행은 세부 점수가 저장되지 않았어요"; return }
    ScoreBreakdown.rows(detail, events, km, t?.stats).forEach { b.layoutRows.addView(scoreRow(it), b.layoutRows.childCount - 1) } // 안내 줄 위에
    b.tvScoreNote.text = "다섯 항목을 더한 값이 주행 점수예요 · 급가속·급제동은 10km 기준으로 환산해요"
}

private fun MainActivity.scoreRow(r: ScoreBreakdown.Row): View {
    val full = r.got >= r.max
    val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    line.addView(text(r.label, R.color.ink_text, 26f), LinearLayout.LayoutParams(dp(LABEL_DP), LinearLayout.LayoutParams.WRAP_CONTENT))
    // 막대 길이는 배점(30·25·20·15·10)에 비례한다. 채운 만큼이 받은 점수다.
    val lane = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; weightSum = 30f }
    val track = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; background = skew(R.color.ink_glow); weightSum = r.max.toFloat() }
    if (r.got > 0) track.addView(View(this).apply { background = skew(R.color.yellow) }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, r.got.toFloat()))
    lane.addView(track, LinearLayout.LayoutParams(0, dp(20), r.max.toFloat()))
    line.addView(lane, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
    line.addView(text("${pointText(r.got)} / ${pointText(r.max)}", if (full) R.color.ink_muted else R.color.ink_text, 32f, number = true).apply { gravity = Gravity.END },
        LinearLayout.LayoutParams(dp(190), LinearLayout.LayoutParams.WRAP_CONTENT))

    return LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        contentDescription = "${r.label} ${pointText(r.max)}점 중 ${pointText(r.got)}점. ${r.note}"
        addView(line)
        addView(text(r.note, if (full) R.color.ink_muted else R.color.yellow, 21f).apply { setPadding(dp(LABEL_DP), 0, 0, 0) })
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(18) }
    }
}

private const val LABEL_DP = 200
