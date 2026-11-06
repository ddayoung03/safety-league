package io.github.ddayoung03.safetyleague.ui.motion

import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator

object ScreenTransition

/** 새 화면(container의 자식)을 페이드와 아래에서 위 이동으로 들인다. transform과 alpha만 쓴다. */
fun ScreenTransition.enter(container: ViewGroup, scope: MotionScope) {
    if (!animationsEnabled()) return
    val lift = 24 * container.resources.displayMetrics.density
    for (i in 0 until container.childCount) {
        val v: View = container.getChildAt(i)
        v.alpha = 0f
        v.translationY = lift
        val settle = { v.alpha = 1f; v.translationY = 0f }
        val handle = scope.add(onFinish = settle, cancel = { v.animate().cancel() })
        v.animate().alpha(1f).translationY(0f).setDuration(MotionTokens.SLOW)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction { settle(); handle.done() }.start()
    }
}
