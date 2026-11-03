package io.github.ddayoung03.safetyleague.core.calculator

import io.github.ddayoung03.safetyleague.core.model.Point2D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteNormalizerTest {

    @Test
    fun fitsGpsRouteIntoTheBoxWithNorthUp() {
        // 경도(x) 0.03°, 위도(y) 0.01° 범위: 가로가 상자 비율보다 길다
        val gps = listOf(Point2D(129.10, 35.15), Point2D(129.13, 35.16))
        val out = RouteNormalizer.normalize(gps)
        for (p in out) assertTrue(p.x in 0.0..100.0 && p.y in 0.0..60.0)
        // 북쪽(위도가 큰 점)이 화면 위(y가 작음)
        assertTrue(out[1].y < out[0].y)
        // 가로세로 비율 유지: 가로가 더 길면 가로를 꽉 채운다
        assertEquals(100.0, out[1].x - out[0].x, 0.5)
    }

    @Test
    fun emptyAndSinglePointRoutesAreSafe() {
        assertTrue(RouteNormalizer.normalize(emptyList()).isEmpty())
        assertEquals(listOf(Point2D(50.0, 30.0)), RouteNormalizer.normalize(listOf(Point2D(129.1, 35.1))))
    }
}
