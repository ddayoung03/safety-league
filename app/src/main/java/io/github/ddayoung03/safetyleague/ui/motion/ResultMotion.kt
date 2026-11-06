package io.github.ddayoung03.safetyleague.ui.motion

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import io.github.ddayoung03.safetyleague.databinding.ViewResultWinBinding
import io.github.ddayoung03.safetyleague.ui.MainActivity
import io.github.ddayoung03.safetyleague.ui.TimingRow
import io.github.ddayoung03.safetyleague.ui.avgText
import io.github.ddayoung03.safetyleague.ui.dp
import io.github.ddayoung03.safetyleague.ui.showFlagLoader

/** 결과 화면 연출의 크기. BIG은 막 끝난 이긴 날에만 쓴다. */
enum class ResultIntro { BIG, CALM, QUICK, NONE }

fun resultIntro(eligible: Boolean, win: Boolean, justFinished: Boolean, animations: Boolean): ResultIntro = when {
    !animations -> ResultIntro.NONE
    !eligible -> ResultIntro.QUICK
    win && justFinished -> ResultIntro.BIG
    else -> ResultIntro.CALM
}

/** 내 줄이 차례로 거쳐 갈 순위. 오른 경우에만 있고, 마지막 [max]칸만 움직인다. */
fun overtakeRanks(before: Int?, after: Int?, max: Int = 3): List<Int> {
    if (before == null || after == null || before <= after) return emptyList()
    return (minOf(before - after, max) - 1 downTo 0).map { after + it }
}

/** 승급 막대는 승급 라인 앞 30계단을 보여 준다. */
fun promotionProgress(steps: Int): Float = (1f - steps / 30f).coerceIn(0f, 1f)

/**
 * 시간표대로 단계를 돌리는 작은 순서기. [finish]는 최종 상태를 맞추는 함수로,
 * [endAt]의 시각이나 scope.finishAll() 중 먼저 오는 쪽에서 한 번만 불린다. 그 뒤 남은 단계는 돌지 않는다.
 */
class MotionSequence(
    scope: MotionScope,
    private val post: (Runnable, Long) -> Unit,
    private val remove: (Runnable) -> Unit,
    private val finish: () -> Unit
) {
    private val pending = mutableListOf<Runnable>()
    private var live = true
    private val handle = scope.add(onFinish = finish, cancel = ::stop)

    private fun stop() {
        live = false
        pending.forEach(remove)
        pending.clear()
    }

    fun at(ms: Long, step: () -> Unit) {
        lateinit var runnable: Runnable
        runnable = Runnable { pending.remove(runnable); if (live) step() }
        pending.add(runnable)
        post(runnable, ms)
    }

    fun endAt(ms: Long) = at(ms) { stop(); handle.done(); finish() }
}

/**
 * 이긴 날 화면의 등장. 뷰에는 이미 최종 값이 들어 있고, 연출은 보이는 모습만 바꾼다.
 * 화면을 누르거나 화면이 바뀌면 motion.finishAll()이 settle()로 최종 상태를 맞춘다.
 */
internal fun MainActivity.playResultWin(
    b: ViewResultWinBinding, intro: ResultIntro, rows: List<TimingRow>, before: Int?, after: Int?, score: Int, progress: Float
) {
    if (intro == ResultIntro.NONE) return
    if (intro != ResultIntro.BIG) { ScreenTransition.enter(binding.screenContainer, motion, MotionTokens.BASE); return }

    val me = rows.indexOfFirst { it.data.name == "나" }
    val ranks = if (me < 0) emptyList() else overtakeRanks(before, after, minOf(3, rows.size - 1 - me))
    val staged = listOf(b.checkerStrip, b.tvTopTitle, b.scoreBlock, b.scoreUnderline, b.tvRankChange, b.promotionBlock,
        b.statsBlock, b.radioPill, b.tower, b.tvTowerNote, b.btnLeague, b.btnReplay)
    val message = b.tvRadioMessage.text.toString()
    val springs = mutableListOf<SpringAnimation>()
    val waves = mutableListOf<ValueAnimator>()

    fun settle() {
        springs.forEach { it.cancel() }
        waves.forEach { it.cancel() }
        b.root.setOnClickListener(null)
        b.root.isClickable = false
        b.hold.animate().cancel()
        b.hold.visibility = View.GONE
        b.startLights.lit = 0
        b.speedLines.stop()
        for (v in staged) {
            v.animate().cancel()
            v.visibility = View.VISIBLE
            v.alpha = 1f
            v.translationX = 0f; v.translationY = 0f
            v.scaleX = 1f; v.scaleY = 1f
        }
        b.progressFill.animate().cancel()
        b.progressFill.scaleX = progress
        for (row in rows) {
            row.view.translationY = 0f
            row.rank.text = "P${row.data.rank}"
            row.avg.text = avgText(row.data.avg)
            row.tag?.alpha = 1f
        }
        b.wave.animate().cancel()
        b.wave.visibility = View.INVISIBLE
        b.tvRadioMessage.text = message
    }

    fun pop(v: View, fromX: Float = 0f, fromY: Float = 0f) {
        v.visibility = View.VISIBLE
        v.alpha = 0f
        v.translationX = fromX; v.translationY = fromY
        v.animate().alpha(1f).translationX(0f).translationY(0f).setDuration(MotionTokens.SLOW)
            .setInterpolator(OvershootInterpolator(1.5f)).start()
    }

    fun grow(v: View, to: Float = 1f, ms: Long = MotionTokens.SLOW) {
        v.visibility = View.VISIBLE
        v.scaleX = 0f
        v.animate().scaleX(to).setDuration(ms).setInterpolator(DecelerateInterpolator(2f)).start()
    }

    fun spring(v: View, property: DynamicAnimation.ViewProperty, to: Float) = SpringAnimation(v, property, to).apply {
        spring.stiffness = SPRING_STIFFNESS
        spring.dampingRatio = SPRING_DAMPING
        springs += this
        start()
    }

    val seq = MotionSequence(motion, { r, ms -> b.root.postDelayed(r, ms) }, { b.root.removeCallbacks(it) }, ::settle)
    staged.forEach { it.visibility = View.INVISIBLE }
    b.hold.visibility = View.VISIBLE
    b.root.setOnClickListener { motion.finishAll() } // 숨은 버튼은 터치를 받지 않고, 나타난 뒤부터 자기 클릭을 받는다
    showFlagLoader()

    // 1. 신호등이 켜지고, 로딩이 끝나는 순간 꺼진다
    for (i in 0 until LIGHT_COUNT) seq.at(i * MotionTokens.LIGHT_STEP) { b.startLights.lit = i + 1 }
    var t = MotionTokens.LOADER_MAX

    // 2. 소등과 함께 속도선과 점수 카운트
    seq.at(t) {
        b.startLights.lit = 0
        b.hold.animate().alpha(0f).setDuration(MotionTokens.BASE).withEndAction { b.hold.visibility = View.GONE }.start()
        grow(b.checkerStrip)
        b.tvTopTitle.visibility = View.VISIBLE
        b.scoreBlock.visibility = View.VISIBLE
        b.speedLines.start(MotionTokens.SPEED_LINES)
        b.tvScore.countUp(score, MotionTokens.COUNT, motion)
    }
    t += MotionTokens.COUNT

    // 3. 착지: 점수가 밀렸다 돌아오고 순위표가 들어온다. 줄은 추월 전 순서로 시작한다.
    val tops = FloatArray(rows.size)
    seq.at(t) {
        b.scoreBlock.translationX = -dp(16).toFloat()
        spring(b.scoreBlock, DynamicAnimation.TRANSLATION_X, 0f)
        grow(b.scoreUnderline)
        rows.forEachIndexed { i, row -> tops[i] = row.view.top.toFloat() }
        if (ranks.isNotEmpty()) {
            val n = ranks.size
            for (i in 1..n) rows[me + i].run {
                view.translationY = tops[me + i - 1] - tops[me + i]
                rank.text = "P${data.rank - 1}"
                tag?.alpha = 0f
            }
            rows[me].view.translationY = tops[me + n] - tops[me]
            rows[me].rank.text = "P${ranks.first() + 1}"
            rows[me].avg.text = avgText(rows[me + n].data.avg - 0.1)
        }
        pop(b.tower, fromX = dp(60).toFloat())
    }

    // 4. 내 줄 추월: 한 칸씩 올라가며 순위 숫자도 바뀐다. 내 줄은 스프링 하나의 목표만 옮겨 움직임이 끊기지 않는다.
    var mySpring: SpringAnimation? = null
    ranks.forEachIndexed { step, rank ->
        seq.at(t + (step + 1) * MotionTokens.OVERTAKE_STEP) {
            val slot = me + ranks.size - step - 1
            val passed = rows[slot + 1]
            spring(passed.view, DynamicAnimation.TRANSLATION_Y, 0f)
            passed.rank.text = "P${passed.data.rank}"
            passed.tag?.alpha = 1f
            val target = tops[slot] - tops[me]
            mySpring?.animateToFinalPosition(target) ?: run { mySpring = spring(rows[me].view, DynamicAnimation.TRANSLATION_Y, target) }
            rows[me].rank.text = "P$rank"
            rows[me].avg.text = avgText(if (slot == me) rows[me].data.avg else passed.data.avg + 0.1)
        }
    }
    t += maxOf(ranks.size, 1) * MotionTokens.OVERTAKE_STEP + MotionTokens.BASE

    // 5. 정보 등장
    val infoFromX = -dp(26).toFloat()
    seq.at(t) { pop(b.tvRankChange, fromX = infoFromX); pop(b.tvTowerNote) }
    t += MotionTokens.STAGGER * 2
    seq.at(t) { pop(b.promotionBlock, fromX = infoFromX); grow(b.progressFill, progress, MotionTokens.SLOW * 2) }
    t += MotionTokens.STAGGER * 2
    seq.at(t) { pop(b.statsBlock, fromX = infoFromX) }
    t += MotionTokens.BASE

    // 6. 팀 라디오: 파형이 움직이는 동안 글자가 찍힌다. 자리는 처음부터 잡혀 있어 알약이 늘어나지 않는다.
    val hidden = ForegroundColorSpan(Color.TRANSPARENT)
    val typed = b.tvRadioMessage
    seq.at(t) {
        typed.setText(message, TextView.BufferType.SPANNABLE)
        (typed.text as Spannable).setSpan(hidden, 0, message.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        pop(b.radioPill, fromY = dp(14).toFloat())
        b.wave.visibility = View.VISIBLE
        waves += startWave(b)
    }
    t += MotionTokens.BASE
    for (i in 1 until message.length) seq.at(t + i * MotionTokens.TYPE_STEP) {
        (typed.text as Spannable).setSpan(hidden, i, message.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    t += message.length * MotionTokens.TYPE_STEP
    seq.at(t) {
        (typed.text as Spannable).removeSpan(hidden)
        b.wave.animate().alpha(0f).setDuration(MotionTokens.BASE).start()
    }

    // 7. 버튼 등장
    val buttonFromX = dp(30).toFloat()
    seq.at(t) { pop(b.btnLeague, fromX = buttonFromX) }
    seq.at(t + MotionTokens.STAGGER * 2) { pop(b.btnReplay, fromX = buttonFromX) }
    seq.endAt(t + MotionTokens.STAGGER * 2 + MotionTokens.SLOW)
}

private fun startWave(b: ViewResultWinBinding): List<ValueAnimator> = List(b.wave.childCount) { i ->
    b.wave.getChildAt(i).scaleY = 0.25f
    ObjectAnimator.ofFloat(b.wave.getChildAt(i), View.SCALE_Y, 0.25f, 1f).apply {
        duration = MotionTokens.WAVE_HALF
        startDelay = WAVE_DELAYS[i] * MotionTokens.STAGGER
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        start()
    }
}

private const val LIGHT_COUNT = 5
private const val SPRING_STIFFNESS = 300f
private const val SPRING_DAMPING = 0.7f
private val WAVE_DELAYS = longArrayOf(0, 3, 6, 2)
