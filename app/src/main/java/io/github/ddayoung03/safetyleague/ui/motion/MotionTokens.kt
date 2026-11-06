package io.github.ddayoung03.safetyleague.ui.motion

import android.animation.ValueAnimator

object MotionTokens {
    const val FAST = 120L
    const val BASE = 240L
    const val SLOW = 420L
    const val STAGGER = 40L
    const val LOADER_MAX = 1500L
    const val LIGHT_STEP = 300L
    const val COUNT = 1000L
    const val SPEED_LINES = 1100L
    const val OVERTAKE_STEP = 300L
    const val TYPE_STEP = 24L
    const val WAVE_HALF = 350L
}

/** 시스템 애니메이션 설정이 꺼져 있으면 false. */
fun animationsEnabled(): Boolean = ValueAnimator.areAnimatorsEnabled()
