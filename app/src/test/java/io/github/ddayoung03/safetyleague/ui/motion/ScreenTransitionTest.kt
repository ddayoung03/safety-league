package io.github.ddayoung03.safetyleague.ui.motion

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.hypot

class ScreenTransitionTest {
    @Test fun buttonNearCornerReachesOppositeCorner() =
        assertEquals(hypot(1000f, 600f), revealRadius(1280, 720, 1000, 600), 0.01f)

    @Test fun centerReachesAnyCorner() =
        assertEquals(hypot(640f, 360f), revealRadius(1280, 720, 640, 360), 0.01f)

    @Test fun topLeftOriginReachesBottomRight() =
        assertEquals(hypot(1280f, 720f), revealRadius(1280, 720, 0, 0), 0.01f)
}
