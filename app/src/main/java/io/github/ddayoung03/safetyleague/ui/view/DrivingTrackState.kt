package io.github.ddayoung03.safetyleague.ui.view

import kotlin.math.ceil

data class DrivingTrackState(val cells: Int, val filled: Int, val hasFinish: Boolean) {
    fun cellGap(availableWidth: Float, preferred: Float): Float = minOf(preferred, availableWidth / (cells * 2 - 1))

    companion object {
        fun from(km: Double, destination: Double?): DrivingTrackState {
            val cells = destination?.let { ceil(it).toInt().coerceAtLeast(3) } ?: maxOf(10, km.toInt() + 1)
            return DrivingTrackState(cells, km.toInt().coerceIn(0, cells), destination != null)
        }
    }
}
