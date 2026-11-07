package io.github.ddayoung03.safetyleague.ui

import android.view.View
import androidx.core.content.res.ResourcesCompat
import io.github.ddayoung03.safetyleague.R
import io.github.ddayoung03.safetyleague.core.calculator.LeagueCalculator
import io.github.ddayoung03.safetyleague.core.data.DemoData
import io.github.ddayoung03.safetyleague.databinding.ViewResultLowBinding
import io.github.ddayoung03.safetyleague.ui.motion.animationsEnabled
import io.github.ddayoung03.safetyleague.ui.motion.playResultLow
import io.github.ddayoung03.safetyleague.ui.motion.resultIntro
import io.github.ddayoung03.safetyleague.ui.view.Pose
import java.time.LocalDate
import kotlin.math.roundToInt

internal fun MainActivity.showResultLow() {
    val b = ViewResultLowBinding.inflate(layoutInflater, binding.screenContainer, true)
    b.checkerStrip.background = checkerTile()
    b.btnLeague.background = skew(R.color.ink_glow)
    b.btnReplay.background = skew(R.color.ink_text)
    listOf(b.btnLeague, b.btnReplay).forEach { it.pressFeedback() }
    b.btnLeague.setOnClickListener { show(Screen.LEAGUE, it) }
    b.btnReplay.setOnClickListener { show(Screen.REPLAY) }
    val t = realTrip
    val intro = resultIntro(t?.isEligible ?: true, false, resultJustFinished, animationsEnabled())
    val score = t?.trip?.score?.roundToInt() ?: DemoData.lowDayScore
    b.tvScore.text = "$score"
    b.tvKm.text = "${t?.trip?.km ?: DemoData.lowDayKm}km"
    // L3·L6: 연속 기록은 사람 기준(모든 리그의 트립)이고 트립 날짜까지 센다. 전·후가 같은 범위다.
    val streakBefore = if (t != null) LeagueCalculator.streakAt(tripsBefore(t, repository.getAllTrips()), t.trip.date) else DemoData.lowDayStreakBefore
    b.tvStreakChange.text = "${streakBefore}일 → 0일"
    b.tvDemoNote.visibility = if (t == null) View.VISIBLE else View.GONE
    if (t != null) {
        b.tvTopTitle.text = "$myModel 리그 · ${t.trip.date}"
        b.tvSeasonLabel.text = "${LocalDate.now().monthValue}월 시즌 순위"
        b.tvStreakChange.text = "${streakBefore}일 → ${repository.getStreak(t.trip.date)}일"
    }
    if (t != null && !t.isEligible) {
        // 3km 미만은 집계 제외: 점수·연속 기록에 영향 없음
        b.tvScore.text = "기록 안 됨"
        b.tvScore.typeface = ResourcesCompat.getFont(this, R.font.pretendard_bold)
        b.tvScore.textSize = 64f
        b.tvScore.setPadding(0, dp(150), 0, 0)
        b.tvScoreUnit.visibility = View.GONE
        b.pitGroup.translationY = -dp(24).toFloat()
        b.tvStreakChange.text = "${streakBefore}일 그대로"
        b.rabbit.pose = Pose.WELCOME
        b.tvPitTip.text = SHORT_TRIP_TIP
        b.tvPitReason.text = "이번 주행 ${t.trip.km}km"
        b.tvPitGain.text = ""
        b.layoutSeasonHeader.visibility = View.GONE
        b.tvStreakHint.visibility = View.GONE
        playResultLow(b, intro, emptyList(), score)
        return
    }
    fixScoreWidth(b.tvScore)

    // 실제 트립은 PitBoard(세부 점수 기준), 시연 64점 카드만 DemoData.lowDayDetail을 쓴다
    val pit = pitBoard(t?.events ?: DemoData.lowDayEvents, if (t == null) DemoData.lowDayDetail else t.trip.detailScore, demo = t == null)
    b.rabbit.pose = Pose.PIT
    b.rabbit.word = pit.word ?: when { pit.tip.contains("깜빡이") -> "깜빡이!"; pit.tip.contains("멈출") -> "천천히!"; pit.tip.contains("벨트") -> "벨트!"; else -> "부드럽게!" }
    b.tvPitTip.text = pit.tip
    b.tvPitReason.text = pit.reason
    b.tvPitGain.text = pit.gain

    val rows: List<TimingRow>
    if (t != null) {
        val s = standing()
        val change = rankChange(t)
        rows = s.neighbors.map { timingRow(it, it.name == "나", nameSuffix = if (it.name == "나") " $change" else "", quiet = true) }
        b.tvTowerNote.text = towerNote(s).ifEmpty { promotionText(s) }
    } else {
        val board = DemoData.lowBoard
        val me = board.first { it.name == "나" }
        val target = board.first { it.rank == DemoData.lowDayRankBefore }
        rows = board.map { r ->
            val isMe = r == me
            val isTarget = r == target
            timingRow(r, isMe, nameSuffix = if (isMe) " ▼${me.rank - DemoData.lowDayRankBefore}" else "",
                tag = if (isTarget) "목표" else null, dim = !isMe && !isTarget, target = isTarget, quiet = true)
        }
        b.tvTowerNote.text = "${LeagueCalculator.round1(target.avg - me.avg)}점 차, 한 번만 잘 달리면 되찾아요"
    }
    rows.forEach { b.layoutTowerRows.addView(it.view) }
    playResultLow(b, intro, rows, score)
}
