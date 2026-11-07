package io.github.ddayoung03.safetyleague.core.calculator

import io.github.ddayoung03.safetyleague.core.model.Point2D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class RouteThinTest {
    private val long = List(3600) { Point2D(it.toDouble(), it * 2.0) }

    @Test fun shortRouteIsUntouched() {
        val short = long.take(300)
        assertSame(short, RouteNormalizer.thin(short))
    }

    @Test fun longRouteKeepsAtMostMaxPoints() {
        assertEquals(300, RouteNormalizer.thin(long).size)
    }

    @Test fun thinKeepsBothEnds() {
        val thin = RouteNormalizer.thin(long)
        assertEquals(long.first(), thin.first())
        assertEquals(long.last(), thin.last())
    }
}
