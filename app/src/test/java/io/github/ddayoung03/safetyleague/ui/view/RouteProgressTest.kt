package io.github.ddayoung03.safetyleague.ui.view

import org.junit.Assert.assertEquals
import org.junit.Test

class RouteProgressTest {
    private val positions = listOf(0f, 0.2f, 0.5f, 0.9f)

    @Test
    fun noEventsBeforeStart() {
        assertEquals(0, visibleEvents(positions, 0f))
    }

    @Test
    fun allEventsAtEnd() {
        assertEquals(4, visibleEvents(positions, 1f))
    }

    @Test
    fun eventsAppearInOrder() {
        assertEquals(listOf(1, 2, 2, 3, 4), listOf(0.1f, 0.2f, 0.49f, 0.5f, 0.95f).map { visibleEvents(positions, it) })
    }
}
