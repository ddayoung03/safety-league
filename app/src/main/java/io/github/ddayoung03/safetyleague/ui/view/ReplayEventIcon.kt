package io.github.ddayoung03.safetyleague.ui.view

import io.github.ddayoung03.safetyleague.R

fun replayEventIcon(kind: String): Int = when (kind) {
    "lane" -> R.drawable.ic_event_lane
    "brake" -> R.drawable.ic_event_brake
    else -> R.drawable.ic_event_accel
}
