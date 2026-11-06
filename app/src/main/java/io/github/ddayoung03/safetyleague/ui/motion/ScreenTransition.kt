package io.github.ddayoung03.safetyleague.ui.motion

import android.animation.Animator
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.PathInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.view.OneShotPreDrawListener
import androidx.core.view.doOnPreDraw
import androidx.core.view.drawToBitmap
import kotlin.math.hypot
import kotlin.math.max

object ScreenTransition

/** 새 화면(container의 자식)을 페이드와 아래에서 위 이동으로 들인다. transform과 alpha만 쓴다. */
fun ScreenTransition.enter(container: ViewGroup, scope: MotionScope, durationMs: Long = MotionTokens.SLOW) {
    if (!animationsEnabled()) return
    val lift = 24 * container.resources.displayMetrics.density
    for (i in 0 until container.childCount) {
        val v: View = container.getChildAt(i)
        v.alpha = 0f
        v.translationY = lift
        val settle = { v.alpha = 1f; v.translationY = 0f }
        val handle = scope.add(onFinish = settle, cancel = { v.animate().cancel() })
        v.animate().alpha(1f).translationY(0f).setDuration(durationMs)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction { settle(); handle.done() }.start()
    }
}

/** 시작점에서 가장 먼 모서리까지의 거리. 이만큼 퍼지면 화면을 다 덮는다. */
fun revealRadius(width: Int, height: Int, x: Int, y: Int): Float =
    hypot(max(x, width - x).toFloat(), max(y, height - y).toFloat())

/**
 * 버튼 [origin]에서 원이 퍼지며 새 화면이 열리는 전환을 준비한다. 옛 화면이 아직 있을 때 불러 모습을 찍어 두고,
 * 새 화면을 넣은 뒤 돌려받은 함수를 부른다. 옛 모습은 그림일 뿐이라 터치는 처음부터 새 화면이 받는다.
 */
fun ScreenTransition.revealFrom(origin: View, container: ViewGroup, scope: MotionScope): () -> Unit {
    if (!animationsEnabled()) return {}
    val at = Rect(0, 0, origin.width, origin.height)
    (container.getChildAt(0) as ViewGroup).offsetDescendantRectToMyCoords(origin, at)
    val behind = BitmapDrawable(container.resources, container.drawToBitmap())
    return {
        val v = container.getChildAt(0)
        container.background = behind
        fun clear() { if (container.background === behind) container.background = null }
        var waiting: OneShotPreDrawListener? = null
        var circle: Animator? = null
        val handle = scope.add(onFinish = ::clear, cancel = { waiting?.removeListener(); circle?.end() })
        waiting = v.doOnPreDraw { // 크기가 정해진 뒤, 첫 그림 전에 시작해야 새 화면이 한 번 비치지 않는다
            circle = ViewAnimationUtils.createCircularReveal(v, at.centerX(), at.centerY(), 0f,
                revealRadius(v.width, v.height, at.centerX(), at.centerY())).apply {
                duration = MotionTokens.SLOW
                interpolator = PathInterpolator(.7f, 0f, .2f, 1f)
                doOnEnd { clear(); handle.done() }
                start()
            }
        }
    }
}

/** 줄을 [startMs]부터 40ms 간격으로 오른쪽에서 들인다. 숨은 뷰는 건너뛴다. */
fun ScreenTransition.stagger(root: View, views: List<View>, scope: MotionScope, startMs: Long = 0L) {
    if (!animationsEnabled()) return
    val shown = views.filter { it.visibility == View.VISIBLE }
    val seq = root.motionSequence(scope) { shown.forEach { it.settleShown() } }
    val from = 44 * root.resources.displayMetrics.density
    shown.forEachIndexed { i, v ->
        v.alpha = 0f
        v.translationX = from
        seq.at(startMs + i * MotionTokens.STAGGER) {
            v.animate().alpha(1f).translationX(0f).setDuration(MotionTokens.SLOW)
                .setInterpolator(DecelerateInterpolator(2f)).start()
        }
    }
    seq.endAt(startMs + shown.size * MotionTokens.STAGGER + MotionTokens.SLOW)
}
