package io.github.ddayoung03.safetyleague.core.calculator

import io.github.ddayoung03.safetyleague.core.model.Point2D
import kotlin.math.cos

/** GPS 경로(경도 x, 위도 y)를 화면용 100×60 상자에 북쪽이 위로 오게 비율을 유지해 맞춘다. */
object RouteNormalizer {
    private const val W = 100.0
    private const val H = 60.0

    /** 그리기용 점 수를 줄인다. 100×60 상자에 수천 점은 필요 없고, 재생 프레임마다 경로를 다시 자르므로 끊긴다. */
    fun thin(points: List<Point2D>, max: Int = 300): List<Point2D> =
        if (points.size <= max) points else List(max) { points[it * (points.size - 1) / (max - 1)] }

    fun normalize(points: List<Point2D>): List<Point2D> {
        if (points.isEmpty()) return emptyList()
        // 경도 1°는 위도에 따라 짧아지므로 cos(위도)로 보정한 평면 좌표로 바꾼다
        val k = cos(Math.toRadians(points.map { it.y }.average()))
        val xs = points.map { it.x * k }
        val ys = points.map { it.y }
        val spanX = xs.max() - xs.min()
        val spanY = ys.max() - ys.min()
        if (spanX == 0.0 && spanY == 0.0) return points.map { Point2D(W / 2, H / 2) }
        val scale = minOf(if (spanX > 0) W / spanX else Double.MAX_VALUE, if (spanY > 0) H / spanY else Double.MAX_VALUE)
        val offX = (W - spanX * scale) / 2
        val offY = (H - spanY * scale) / 2
        val minX = xs.min()
        val maxY = ys.max()
        return points.indices.map { i ->
            // 부동소수 오차로 상자 밖(예: 60.0000001)으로 나가지 않게 잘라 둔다
            Point2D((offX + (xs[i] - minX) * scale).coerceIn(0.0, W), (offY + (maxY - ys[i]) * scale).coerceIn(0.0, H))
        }
    }
}
