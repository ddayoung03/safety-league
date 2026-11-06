package io.github.ddayoung03.safetyleague.ui.motion

import android.animation.ValueAnimator

object MotionTokens {
    const val FAST = 120L
    const val BASE = 240L
    const val SLOW = 420L
    const val STAGGER = 40L
    const val LOADER_MAX = 1500L
}

/** 시스템 애니메이션 설정이 꺼져 있으면 false. */
fun animationsEnabled(): Boolean = ValueAnimator.areAnimatorsEnabled()
