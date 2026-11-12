package io.github.ddayoung03.safetyleague.ui.view

import kotlin.math.ceil

/** 칸은 1km마다 채우고, 자동차는 0.1km마다 칸 안에서 조금씩 움직인다([carAt]은 칸 단위 위치). */
data class DrivingTrackState(val cells: Int, val filled: Int, val hasFinish: Boolean, val carAt: Float = filled.toFloat()) {
    fun cellGap(availableWidth: Float, preferred: Float): Float = minOf(preferred, availableWidth / (cells * 2 - 1))

    companion object {
        fun from(km: Double, destination: Double?): DrivingTrackState {
            val cells = destination?.let { ceil(it).toInt().coerceAtLeast(3) } ?: maxOf(10, km.toInt() + 1)
            val tenths = (km * 10).toInt().coerceIn(0, cells * 10)
            return DrivingTrackState(cells, km.toInt().coerceIn(0, cells), destination != null, tenths / 10f)
        }
    }
}
