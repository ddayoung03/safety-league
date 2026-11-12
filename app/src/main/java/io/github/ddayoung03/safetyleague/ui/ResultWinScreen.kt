package io.github.ddayoung03.safetyleague.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.TextView
import io.github.ddayoung03.safetyleague.R
import io.github.ddayoung03.safetyleague.core.calculator.LeagueCalculator
import io.github.ddayoung03.safetyleague.core.data.DemoData
import io.github.ddayoung03.safetyleague.databinding.ViewResultWinBinding
import io.github.ddayoung03.safetyleague.ui.motion.animationsEnabled
import io.github.ddayoung03.safetyleague.ui.motion.playResultWin
import io.github.ddayoung03.safetyleague.ui.motion.promotionProgress
import io.github.ddayoung03.safetyleague.ui.motion.resultIntro
import io.github.ddayoung03.safetyleague.ui.motion.scoreTextSp
import io.github.ddayoung03.safetyleague.ui.view.Pose
import java.time.LocalDate
import kotlin.math.roundToInt

internal fun MainActivity.showResultWin() {
    val b = ViewResultWinBinding.inflate(layoutInflater, binding.screenContainer, true)
    b.rabbit.pose = Pose.CHEER
    b.checkerStrip.background = checkerTile()
    b.rabbitMat.background = checkerTile()
    b.scoreUnderline.background = skew(R.color.yellow)
    b.promotionTrack.background = skew(R.color.ink_glow)
    b.progressFill.background = skew(R.color.yellow)
    b.btnLeague.background = skew(R.color.ink_glow)
    b.btnReplay.background = skew(R.color.yellow)
    b.btnScore.background = skew(R.color.ink_glow)
    listOf(b.btnScore, b.btnLeague, b.btnReplay).forEach { it.pressFeedback() }
    b.btnScore.setOnClickListener { show(Screen.SCORE, it) }
    b.btnLeague.setOnClickListener { show(Screen.LEAGUE, it) }
    b.btnReplay.setOnClickListener { show(Screen.REPLAY) }

    val day = DemoData.today
    val t = realTrip
    val score = (t?.trip?.score ?: DemoData.trips.first { it.date == day }.score).roundToInt()
    b.tvScore.text = "$score"
    val ascent = b.tvScore.paint.fontMetricsInt.ascent
    b.tvScore.textSize = scoreTextSp(score)
    b.tvScore.setPadding(0, b.tvScore.paint.fontMetricsInt.ascent - ascent, 0, 0) // 글자가 줄어도 기준선은 그대로
    fixScoreWidth(b.tvScore)
    b.tvKm.text = "${t?.trip?.km ?: DemoData.trips.first { it.date == day }.km}km"
    b.tvStreak.text = "연속 안전 주행 ${if (t != null) repository.getStreak(t.trip.date) else DemoData.streakAt(day)}일"

    val rows: List<TimingRow>
    val before: Int?
    val after: Int?
    val promotion: String
    val progress: Float
    if (t != null) {
        val s = standing()
        val change = rankChange(t)
        val move = rankMove(t)
        before = move?.first
        after = move?.second
        rows = s.neighbors.map { timingRow(it, it.name == "나", nameSuffix = if (it.name == "나") " $change" else "") }
        b.tvTowerNote.text = towerNote(s)
        b.tvTopTitle.text = "$myModel 리그 · ${t.trip.date}"
        b.tvSeasonLabel.text = "${LocalDate.now().monthValue}월 시즌 순위"
        b.tvRankChange.text = rankDelta(before, after, change)
        b.tvRadioMessage.text = "안전하게 완주했어요! 다음 레이스도 함께해요"
        promotion = promotionText(s).substringBefore(" ·")
        progress = if (s.rank == null) 0f else promotionProgress(s.stepsToPromotion)
    } else {
        val me = DemoData.ranks.getValue(day)
        val previous = DemoData.ranks.getValue(LocalDate.parse(day).minusDays(1).toString())
        before = previous
        after = me
        val steps = me - LeagueCalculator.promotionRank(DemoData.league.total)
        val board = DemoData.neighbors().filter { it.rank in (me - 2)..(me + 3) }
        val above = board.first { it.rank == me - 1 }
        val myAvg = board.first { it.name == "나" }.avg
        rows = board.map { r ->
            val isMe = r.name == "나"
            val passed = r.rank in (me + 1)..previous
            timingRow(r, isMe, nameSuffix = if (isMe) " ▲${previous - me}" else "",
                tag = if (passed) "추월" else null, dim = !isMe && !passed && kotlin.math.abs(r.rank - me) >= 2)
        }
        b.tvTowerNote.text = "${LeagueCalculator.round1(above.avg - myAvg)}점 차, 다음 주행에서 따라잡을 수 있어요"
        b.tvRankChange.text = rankDelta(previous, me, "")
        b.tvRadioMessage.text = "멋진 레이스였어요! 오늘 ${previous - me}계단 올라섰어요"
        promotion = "승급까지 ${steps}계단"
        progress = promotionProgress(steps)
    }
    rows.forEach { b.layoutTowerRows.addView(it.view) }
    // "승급까지 6계단"은 양쪽으로 나누고, "승급권에 진입했어요" 같은 문장은 왼쪽에 그대로 둔다.
    b.tvToPromotion.text = if (promotion.endsWith("계단")) promotion.substringBeforeLast(" ") else promotion
    b.tvPromotionSteps.text = if (promotion.endsWith("계단")) promotion.substringAfterLast(" ") else ""
    b.progressFill.scaleX = progress

    val intro = resultIntro(t?.isEligible ?: true, true, resultJustFinished, animationsEnabled())
    playResultWin(b, intro, rows, before, after, score, progress)
}

private fun rankDelta(before: Int?, after: Int?, fallback: String): String {
    val up = if (before != null && after != null) before - after else 0
    return if (up > 0) "▲ $up 오늘 ${up}명을 제쳤어요" else fallback
}

/** 기울임이 글자 칸 밖으로 나오는 만큼 넓히고, 너비를 고정해 카운트 중에 다시 재지 않게 한다. 100점도 들어간다. */
internal fun MainActivity.fixScoreWidth(score: TextView) {
    score.layoutParams.width = score.paint.measureText(score.text.toString()).roundToInt() + dp(52)
}

/** 상단 체커 띠. 6dp 칸을 되풀이한다. */
internal fun MainActivity.checkerTile(): Drawable {
    val cell = dp(6)
    val tile = Bitmap.createBitmap(cell * 2, cell * 2, Bitmap.Config.ARGB_8888)
    val dark = Paint().apply { color = color(R.color.ink) }
    Canvas(tile).apply {
        drawColor(color(R.color.ink_text))
        drawRect(0f, 0f, cell.toFloat(), cell.toFloat(), dark)
        drawRect(cell.toFloat(), cell.toFloat(), cell * 2f, cell * 2f, dark)
    }
    return BitmapDrawable(resources, tile).apply { setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT) }
}
