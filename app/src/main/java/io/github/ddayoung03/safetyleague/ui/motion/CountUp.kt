package io.github.ddayoung03.safetyleague.ui.motion

import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import android.widget.TextView
import androidx.core.animation.doOnEnd
import kotlin.math.pow
import kotlin.math.roundToInt

/** 처음에 빠르고 끝에서 느려진다. fraction 1이면 정확히 [to]. */
fun countValue(to: Int, fraction: Float): Int = (to * (1 - (1 - fraction).pow(4))).roundToInt()

/** 뷰에 이미 들어 있는 최종 글자까지 0부터 센다. 끝나거나 finishAll이 오면 최종 글자로 돌아간다. */
fun TextView.countUp(to: Int, durationMs: Long, scope: MotionScope) {
    val finalText = text
    val animator = ValueAnimator.ofFloat(0f, 1f).setDuration(durationMs)
    val handle = scope.add(onFinish = { text = finalText }, cancel = { animator.cancel() })
    var shown = 0
    text = "0"
    animator.interpolator = LinearInterpolator()
    animator.addUpdateListener {
        val value = countValue(to, it.animatedFraction)
        if (value != shown) { shown = value; text = value.toString() }
    }
    animator.doOnEnd { text = finalText; handle.done() }
    animator.start()
}
