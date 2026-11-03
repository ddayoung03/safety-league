package io.github.ddayoung03.safetyleague.core.collector

import io.github.ddayoung03.safetyleague.core.model.Point2D
import org.junit.Assert.assertEquals
import org.junit.Test

class LocationFallbackTest {

    /** 시험용 위치 출처. available=false면 시작 자체가 안 되고, failLater()로 동작 중 실패를 낸다. */
    private class Fake(override val name: String, private val available: Boolean) : LocationSource {
        var started = 0; var stopped = 0
        private var fail: ((String) -> Unit)? = null
        private var emit: ((Point2D) -> Unit)? = null
        override fun start(onPoint: (Point2D) -> Unit, onFailed: (String) -> Unit): Boolean {
            if (!available) return false
            started++; emit = onPoint; fail = onFailed; return true
        }
        override fun stop() { stopped++ }
        fun failLater() = fail!!("broken")
        fun send(p: Point2D) = emit!!(p)
    }

    @Test
    fun firstAvailableSourceIsUsed() {
        val fused = Fake("FusedLocation", available = false)
        val navi = Fake("NaviHelper", available = true)
        val os = Fake("LocationManager", available = true)
        val points = mutableListOf<Point2D>()
        val chain = LocationFallback(listOf(fused, navi, os)) { points += it }
        chain.start()
        assertEquals("NaviHelper", chain.activeName)
        assertEquals(0, os.started)
        navi.send(Point2D(1.0, 2.0))
        assertEquals(listOf(Point2D(1.0, 2.0)), points)
    }

    @Test
    fun runtimeFailureMovesToTheNextSource() {
        val fused = Fake("FusedLocation", available = true)
        val navi = Fake("NaviHelper", available = true)
        val chain = LocationFallback(listOf(fused, navi)) {}
        chain.start()
        fused.failLater()
        assertEquals(1, fused.stopped)
        assertEquals("NaviHelper", chain.activeName)
        assertEquals(listOf("FusedLocation: broken"), chain.problems)
    }

    @Test
    fun noSourceLeftIsReported() {
        val chain = LocationFallback(listOf(Fake("FusedLocation", false), Fake("LocationManager", false))) {}
        chain.start()
        assertEquals(null, chain.activeName)
        assertEquals(listOf("Location: 쓸 수 있는 위치 출처 없음"), chain.problems)
    }
}
