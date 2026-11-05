package io.github.ddayoung03.safetyleague.ui

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.github.ddayoung03.safetyleague.core.calculator.LeagueCalculator
import io.github.ddayoung03.safetyleague.core.calculator.RouteNormalizer
import io.github.ddayoung03.safetyleague.core.data.DemoData
import io.github.ddayoung03.safetyleague.databinding.ViewResultWinBinding
import io.github.ddayoung03.safetyleague.ui.view.AsphaltDrawable
import java.time.LocalDate
import kotlin.math.roundToInt

internal fun MainActivity.showResultWin() {
    val b = ViewResultWinBinding.inflate(layoutInflater, binding.screenContainer, true)
    b.root.background = AsphaltDrawable(this, streak = true)
    b.routeBg.isBackgroundMode = true
    b.root.viewTreeObserver.addOnGlobalLayoutListener {
        val bounds = mutableListOf<android.graphics.RectF>()
        fun protect(view: View) {
            if (view.visibility != View.VISIBLE) return
            if (view is TextView || view.isClickable || view.background != null && view !== b.root) {
                val rect = android.graphics.Rect()
                view.getDrawingRect(rect)
                b.root.offsetDescendantRectToMyCoords(view, rect)
                rect.offset(-b.flameScoreView.left, -b.flameScoreView.top)
                rect.inset(-dp(8), -dp(8))
                bounds.add(android.graphics.RectF(rect))
            }
            if (view is ViewGroup) for (i in 0 until view.childCount) protect(view.getChildAt(i))
        }
        protect(b.root)
        b.flameScoreView.protectedBounds = bounds
    }
    val day = DemoData.today
    val t = realTrip
    val me = DemoData.ranks.getValue(day)
    val before = DemoData.ranks.getValue(LocalDate.parse(day).minusDays(1).toString())
    b.tvScore.text = (t?.trip?.score ?: DemoData.trips.first { it.date == day }.score).roundToInt().toString()
    b.tvKm.text = "${t?.trip?.km ?: DemoData.trips.first { it.date == day }.km}km"
    b.tvStreak.text = "${if (t != null) repository.getStreak(t.trip.date) else DemoData.streakAt(day)}일"
    b.tvRadioMessage.text = if (t == null) "멋진 레이스였어요!\n오늘 ${before - me}계단 올라섰어요" else "안전하게 완주했어요!\n다음 레이스도 함께해요"
    b.tvRankChange.text = "▲${before - me}"
    b.tvToPromotion.text = "승급 라인까지 ${me - LeagueCalculator.promotionRank(DemoData.league.total)}계단"
    t?.let { b.routeBg.routePoints = RouteNormalizer.normalize(it.route) }

    if (t != null) {
        b.tvTopTitle.text = "$myModel 리그 · ${t.trip.date}"
        b.tvSeasonLabel.text = "${LocalDate.now().monthValue}월 시즌 순위"
        b.tvRankChange.text = rankChange(t)
        b.tvToPromotion.text = promotionText(standing()).substringBefore(" ·")
        realRows(b.layoutTowerRows, standing(), rankChange(t))
        b.btnLeague.visibility = View.VISIBLE
        b.btnLeague.setOnClickListener { show(Screen.LEAGUE) }
        b.btnReplay.setOnClickListener { show(Screen.REPLAY) }
        return
    }

    val rows = DemoData.neighbors().filter { it.rank in (me - 2)..(me + 3) }
    val above = rows.firstOrNull { it.rank == me - 1 }
    val myAvg = rows.first { it.name == "나" }.avg
    b.layoutTowerRows.removeAllViews()
    for (r in rows) {
        val isMe = r.name == "나"
        val passed = r.rank in (me + 1)..before
        b.layoutTowerRows.addView(towerRow(r, isMe, nameSuffix = if (isMe) " ▲${before - me}" else "",
            tag = if (passed) "추월" else null, dim = !isMe && !passed && kotlin.math.abs(r.rank - me) >= 2))
        if (r == above) b.layoutTowerRows.addView(note("${LeagueCalculator.round1(above.avg - myAvg)}점 차, 다음 주행에서 따라잡을 수 있어요"))
    }
    b.btnReplay.setOnClickListener { show(Screen.REPLAY) }
}
