package io.github.ddayoung03.safetyleague.ui

import android.view.View
import io.github.ddayoung03.safetyleague.core.calculator.LeagueCalculator
import io.github.ddayoung03.safetyleague.core.calculator.RouteNormalizer
import io.github.ddayoung03.safetyleague.core.data.DemoData
import io.github.ddayoung03.safetyleague.databinding.ViewResultLowBinding
import io.github.ddayoung03.safetyleague.ui.view.AsphaltDrawable
import java.time.LocalDate
import kotlin.math.roundToInt

internal fun MainActivity.showResultLow() {
    val b = ViewResultLowBinding.inflate(layoutInflater, binding.screenContainer, true)
    b.root.background = AsphaltDrawable(this, streak = true)
    b.tvScore.dull = true
    b.routeBg.isBackgroundMode = true
    val t = realTrip
    b.kerbTop.isMuted = true
    b.kerbBottom.isMuted = true
    b.tvScore.text = (t?.trip?.score?.roundToInt() ?: DemoData.lowDayScore).toString()
    b.tvKm.text = "${t?.trip?.km ?: DemoData.lowDayKm}km"
    // L3·L6: 연속 기록은 사람 기준(모든 리그의 트립)이고 트립 날짜까지 센다. 전·후가 같은 범위다.
    val streakBefore = if (t != null) LeagueCalculator.streakAt(tripsBefore(t, repository.getAllTrips()), t.trip.date) else DemoData.lowDayStreakBefore
    b.tvStreakChange.text = "${streakBefore}일 → 0일"
    t?.let { b.routeBg.routePoints = RouteNormalizer.normalize(it.route) }
    b.tvDemoNote.visibility = if (t == null) View.VISIBLE else View.GONE
    if (t != null) {
        b.tvTopTitle.text = "$myModel 리그 · ${t.trip.date}"
        b.tvSeasonLabel.text = "${LocalDate.now().monthValue}월 시즌 순위"
        b.tvStreakChange.text = "${streakBefore}일 → ${repository.getStreak(t.trip.date)}일"
    }
    if (t != null && !t.isEligible) {
        // 3km 미만은 집계 제외: 점수·연속 기록에 영향 없음
        b.tvScore.text = "–"
        b.tvStreakChange.text = "${streakBefore}일 그대로"
        b.tvPitWord.text = "다음에!"
        b.tvPitTip.text = "3km 이상 달리면 기록돼요"
        b.tvPitReason.text = "이번 주행 ${t.trip.km}km"
        b.tvPitGain.text = ""
        b.btnReplay.setOnClickListener { show(Screen.REPLAY) }
        b.layoutTowerRows.removeAllViews()
        b.btnLeague.visibility = View.VISIBLE
        b.btnLeague.setOnClickListener { show(Screen.LEAGUE) }
        return
    }

    // 실제 트립은 PitBoard(세부 점수 기준), 시연 64점 카드만 DemoData.lowDayDetail을 쓴다
    val pit = pitBoard(t?.events ?: DemoData.lowDayEvents, if (t == null) DemoData.lowDayDetail else t.trip.detailScore, demo = t == null)
    b.tvPitWord.text = pit.word ?: when { pit.tip.contains("깜빡이") -> "깜빡이!"; pit.tip.contains("멈출") -> "천천히!"; pit.tip.contains("벨트") -> "벨트!"; else -> "부드럽게!" }
    b.tvPitTip.text = pit.tip
    b.tvPitReason.text = pit.reason
    b.tvPitGain.text = pit.gain

    if (t != null) {
        realRows(b.layoutTowerRows, standing(), rankChange(t))
        b.layoutTowerRows.addView(note(promotionText(standing())))
        b.btnLeague.visibility = View.VISIBLE
        b.btnLeague.setOnClickListener { show(Screen.LEAGUE) }
        b.btnReplay.setOnClickListener { show(Screen.REPLAY) }
        return
    }

    val rows = DemoData.lowBoard
    val me = rows.first { it.name == "나" }
    val target = rows.first { it.rank == DemoData.lowDayRankBefore }
    b.layoutTowerRows.removeAllViews()
    for (r in rows) {
        val isMe = r == me
        val isTarget = r == target
        b.layoutTowerRows.addView(towerRow(r, isMe, nameSuffix = if (isMe) " ▼${me.rank - DemoData.lowDayRankBefore}" else "",
            tag = if (isTarget) "목표" else null, target = isTarget, dim = !isMe && !isTarget, size = rowLow))
        if (isTarget) b.layoutTowerRows.addView(note("${LeagueCalculator.round1(target.avg - me.avg)}점 차, 한 번만 잘 달리면 되찾아요"))
    }
    b.btnReplay.setOnClickListener { show(Screen.REPLAY) }
}
