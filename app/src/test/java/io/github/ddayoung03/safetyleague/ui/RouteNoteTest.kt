package io.github.ddayoung03.safetyleague.ui

import io.github.ddayoung03.safetyleague.core.model.Point2D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RouteNoteTest {

    @Test
    fun noPointsMeansRouteWasNotRecorded() {
        assertEquals("경로를 기록하지 않았어요", routeNote(emptyList()))
    }

    @Test
    fun onePlaceMeansNoMovement() {
        val p = Point2D(129.0, 35.0)
        assertEquals("위치 변화가 없어 경로를 그리지 못했어요", routeNote(listOf(p, p)))
    }

    @Test
    fun movedRouteNeedsNoNote() {
        assertNull(routeNote(listOf(Point2D(129.0, 35.0), Point2D(129.1, 35.0))))
    }
}
