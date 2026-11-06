package io.github.ddayoung03.safetyleague.ui.motion

import io.github.ddayoung03.safetyleague.ui.view.canAnimateCrew
import io.github.ddayoung03.safetyleague.weather.MotionGear

/** 효과음은 연출과 같은 주차 조건에 오디오 포커스까지 있어야 낸다. */
internal fun canPlaySound(gear: MotionGear, speedMps: Float?, resumed: Boolean, restricted: Boolean, focus: Boolean) =
    focus && canAnimateCrew(gear, speedMps, resumed, restricted)
