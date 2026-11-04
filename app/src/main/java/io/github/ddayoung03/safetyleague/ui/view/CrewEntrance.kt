package io.github.ddayoung03.safetyleague.ui.view

import io.github.ddayoung03.safetyleague.weather.MotionGear
import io.github.ddayoung03.safetyleague.weather.isAmbientMotionAllowed

internal fun canAnimateCrew(gear: MotionGear, speedMps: Float?, resumed: Boolean, restricted: Boolean) =
    resumed && !restricted && gear == MotionGear.PARK && isAmbientMotionAllowed(speedMps, gear)
