package io.github.ddayoung03.safetyleague.ui.view

import org.junit.Assert.*
import org.junit.Test

class DemoBarcodeTest {
    @Test fun barsAreStableForId() {
        assertEquals(barcodeBars("a"), barcodeBars("a"))
        assertNotEquals(barcodeBars("a"), barcodeBars("b"))
        assertEquals(40, barcodeBars("a").size)
        assertTrue(barcodeBars("a").all { it in 1..4 })
    }
}
