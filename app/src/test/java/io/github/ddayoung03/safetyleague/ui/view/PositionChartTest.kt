package io.github.ddayoung03.safetyleague.ui.view

import org.junit.Assert.assertEquals
import org.junit.Test

class PositionChartTest {
    @Test
    fun nothingShownBeforeStart() {
        assertEquals(-1f, chartHead(0, 6, 0f))
    }

    @Test
    fun endsExactlyOnLastPoint() {
        assertEquals(6f, chartHead(0, 6, 1f))
        assertEquals(4f, chartHead(2, 4, 1f))
    }

    @Test
    fun startsFromFirstKnownDay() {
        assertEquals(3f, chartHead(2, 4, 0.5f))
    }

    @Test
    fun singlePointAppearsOnceStarted() {
        assertEquals(3f, chartHead(3, 3, 0.01f))
    }
}
