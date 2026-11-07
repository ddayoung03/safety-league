package io.github.ddayoung03.safetyleague.ui

import io.github.ddayoung03.safetyleague.core.data.SafetyGuide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class GuideIconTest {
    @Test fun everyCardHasItsOwnIcon() {
        val icons = SafetyGuide.cards.map { guideIcon(it.id) }
        icons.forEach { assertNotEquals(0, it) }
        assertEquals(icons.size, icons.distinct().size)
    }
}
