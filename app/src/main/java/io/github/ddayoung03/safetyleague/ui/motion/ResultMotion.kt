package io.github.ddayoung03.safetyleague.ui.motion

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import io.github.ddayoung03.safetyleague.databinding.ViewResultLowBinding
import io.github.ddayoung03.safetyleague.databinding.ViewResultWinBinding
import io.github.ddayoung03.safetyleague.ui.MainActivity
import io.github.ddayoung03.safetyleague.ui.TimingRow
import io.github.ddayoung03.safetyleague.ui.dp
import io.github.ddayoung03.safetyleague.ui.showFlagLoader
import io.github.ddayoung03.safetyleague.ui.view.Pose

/** 결과 화면 연출의 크기. BIG은 막 끝난 이긴 날, QUICK은 3km 미만과 다시 연 아쉬운 날에 쓴다. */
enum class ResultIntro { BIG, CALM, QUICK, NONE }

fun resultIntro(eligible: Boolean, win: Boolean, justFinished: Boolean, animations: Boolean): ResultIntro = when {
    !animations -> ResultIntro.NONE
    !eligible || (!win && !justFinished) -> ResultIntro.QUICK
    win && justFinished -> ResultIntro.BIG
    else -> ResultIntro.CALM
}

/** 내 줄이 차례로 거쳐 갈 순위. 오른 경우에만 있고, 마지막 [max]칸만 움직인다. */
fun overtakeRanks(before: Int?, after: Int?, max: Int = 3): List<Int> {
    if (before == null || after == null || before <= after) return emptyList()
    return (minOf(before - after, max) - 1 downTo 0).map { after + it }
}

/** 점수 글자 크기(sp). 세 자리는 두 자리와 폭이 같게 줄여 오른쪽 끝을 지킨다. */
fun scoreTextSp(score: Int): Float = if (score >= 100) 190f else 262f

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

internal fun View.motionSequence(scope: MotionScope, finish: () -> Unit) =
    MotionSequence(scope, { r, ms -> postDelayed(r, ms) }, { removeCallbacks(it) }, finish)

/** 화면을 누르면 건너뛰는 순서기. 숨은 버튼은 터치를 받지 않고, 나타난 뒤부터 자기 클릭을 받는다. */
internal fun MainActivity.skippableSequence(root: View, settle: () -> Unit): MotionSequence {
    val seq = root.motionSequence(motion) {
        root.setOnClickListener(null)
        root.isClickable = false
        settle()
    }
    root.setOnClickListener { motion.finishAll() }
    return seq
}

internal fun View.settleShown() {
    animate().cancel()
    visibility = View.VISIBLE
    alpha = 1f
    translationX = 0f; translationY = 0f
    scaleX = 1f; scaleY = 1f
}

private fun springTo(v: View, property: DynamicAnimation.ViewProperty, to: Float) = SpringAnimation(v, property, to).apply {
    spring.stiffness = SPRING_STIFFNESS
    spring.dampingRatio = SPRING_DAMPING
    start()
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
        b.statsBlock, b.radioPill, b.tower, b.tvTowerNote, b.btnLeague, b.btnReplay, b.rabbit)
    val message = b.tvRadioMessage.text.toString()
    val springs = mutableListOf<SpringAnimation>()
    val waves = mutableListOf<ValueAnimator>()

    fun settle() {
        springs.forEach { it.cancel() }
        waves.forEach { it.cancel() }
        b.hold.animate().cancel()
        b.hold.visibility = View.GONE
        b.startLights.lit = 0
        b.speedLines.stop()
        staged.forEach { it.settleShown() }
        b.progressFill.animate().cancel()
        b.progressFill.scaleX = progress
        for (row in rows) {
            row.view.translationY = 0f
            row.rank.text = "P${row.data.rank}"
            row.tag?.alpha = 1f
        }
        b.wave.animate().cancel()
        b.wave.visibility = View.INVISIBLE
        b.tvRadioMessage.text = message
        b.rabbit.pose = Pose.WIN
    }

    fun pop(v: View, fromX: Float = 0f, fromY: Float = 0f, curve: Interpolator = OvershootInterpolator(1.5f)) {
        v.visibility = View.VISIBLE
        v.alpha = 0f
        v.translationX = fromX; v.translationY = fromY
        v.animate().alpha(1f).translationX(0f).translationY(0f).setDuration(MotionTokens.SLOW)
            .setInterpolator(curve).start()
    }

    fun grow(v: View, to: Float = 1f, ms: Long = MotionTokens.SLOW) {
        v.visibility = View.VISIBLE
        v.scaleX = 0f
        v.animate().scaleX(to).setDuration(ms).setInterpolator(DecelerateInterpolator(2f)).start()
    }

    fun spring(v: View, property: DynamicAnimation.ViewProperty, to: Float) = springTo(v, property, to).also { springs += it }

    val seq = skippableSequence(b.root, ::settle)
    val audible = requestSound()
    val voice = if (audible) motion.add(cancel = sound::stopAll) else null
    fun beep(fx: Fx) { if (audible) sound.play(fx) }
    staged.forEach { it.visibility = View.INVISIBLE }
    b.hold.visibility = View.VISIBLE
    showFlagLoader()

    // 1. 신호등이 켜지고, 로딩이 끝나는 순간 꺼진다
    for (i in 0 until LIGHT_COUNT) seq.at(i * MotionTokens.LIGHT_STEP) { b.startLights.lit = i + 1; beep(Fx.LIGHT) }
    var t = MotionTokens.LOADER_MAX

    // 2. 소등과 함께 속도선과 점수 카운트
    seq.at(t) {
        b.startLights.lit = 0
        beep(Fx.GO)
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
        }
        pop(b.tower, fromX = dp(60).toFloat(), curve = DecelerateInterpolator(2f))
        b.rabbit.visibility = View.VISIBLE
        b.rabbit.translationY = dp(120).toFloat()
        spring(b.rabbit, DynamicAnimation.TRANSLATION_Y, 0f)
    }

    // 4. 내 줄 추월: 한 칸씩 올라가며 순위 숫자도 바뀐다. 내 줄은 스프링 하나의 목표만 옮겨 움직임이 끊기지 않는다.
    var mySpring: SpringAnimation? = null
    ranks.forEachIndexed { step, rank ->
        seq.at(t + (step + 1) * MotionTokens.OVERTAKE_STEP) {
            b.rabbit.pose = Pose.OVERTAKE
            val slot = me + ranks.size - step - 1
            val passed = rows[slot + 1]
            spring(passed.view, DynamicAnimation.TRANSLATION_Y, 0f)
            passed.rank.text = "P${passed.data.rank}"
            passed.tag?.alpha = 1f
            val target = tops[slot] - tops[me]
            mySpring?.animateToFinalPosition(target) ?: run { mySpring = spring(rows[me].view, DynamicAnimation.TRANSLATION_Y, target) }
            rows[me].rank.text = "P$rank"
            beep(Fx.OVERTAKE)
        }
    }
    t += maxOf(ranks.size, 1) * MotionTokens.OVERTAKE_STEP + MotionTokens.BASE
    seq.at(t) { b.rabbit.pose = Pose.WIN }

    // 5. 정보 등장
    val infoFromX = -dp(26).toFloat()
    seq.at(t) { beep(Fx.WIN); pop(b.tvRankChange, fromX = infoFromX); pop(b.tvTowerNote) }
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
    val end = t + MotionTokens.STAGGER * 2 + MotionTokens.SLOW
    if (voice != null) seq.at(end) { voice.done(); sound.stopAll() }
    seq.endAt(end)
}

/**
 * 아쉬운 날 화면의 등장. 신호등도 속도선도 없이 점수가 차분히 오르고, 피트 보드가 한 번 올라온 뒤 나머지가 조용히 나타난다.
 * 뷰에는 이미 최종 값이 들어 있고, 화면을 누르면 settle()이 최종 상태를 맞춘다.
 */
internal fun MainActivity.playResultLow(b: ViewResultLowBinding, intro: ResultIntro, rows: List<TimingRow>, score: Int) {
    if (intro == ResultIntro.NONE) return
    if (intro != ResultIntro.CALM) { ScreenTransition.enter(binding.screenContainer, motion, MotionTokens.BASE); return }

    val board = listOf(b.rabbit, b.pitBoard)
    val info = (listOf(b.layoutSeasonHeader) + rows.map { it.view } + listOf(b.tvTowerNote, b.statsBlock, b.buttons, b.tvDemoNote))
        .filter { it.visibility == View.VISIBLE }
    val springs = mutableListOf<SpringAnimation>()

    fun settle() {
        springs.forEach { it.cancel() }
        (board + info).forEach { it.settleShown() }
    }

    val seq = skippableSequence(b.root, ::settle)
    (board + info).forEach { it.visibility = View.INVISIBLE }
    showFlagLoader(MotionTokens.COUNT)
    b.tvScore.countUp(score, MotionTokens.COUNT, motion)

    var t = MotionTokens.COUNT
    seq.at(t) {
        for (v in board) {
            v.visibility = View.VISIBLE
            v.alpha = 0f
            v.translationY = dp(36).toFloat()
            v.animate().alpha(1f).setDuration(MotionTokens.BASE).start()
            springs += springTo(v, DynamicAnimation.TRANSLATION_Y, 0f)
        }
    }
    t += MotionTokens.SLOW
    for (v in info) {
        seq.at(t) {
            v.visibility = View.VISIBLE
            v.alpha = 0f
            v.animate().alpha(1f).setDuration(MotionTokens.SLOW).setInterpolator(DecelerateInterpolator()).start()
        }
        t += MotionTokens.STAGGER
    }
    seq.endAt(t + MotionTokens.SLOW)
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
internal const val SPRING_STIFFNESS = 300f
internal const val SPRING_DAMPING = 0.7f
private val WAVE_DELAYS = longArrayOf(0, 3, 6, 2)
