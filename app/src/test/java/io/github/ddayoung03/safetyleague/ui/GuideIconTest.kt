package io.github.ddayoung03.safetyleague.ui

import io.github.ddayoung03.safetyleague.R
import io.github.ddayoung03.safetyleague.core.data.SafetyGuide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class GuideIconTest {
    @Test fun shopIconsFollowTheItem() {
        assertEquals(R.drawable.shop_charge, shopIcon("charge_10k"))
        assertEquals(R.drawable.shop_ticket, shopIcon("service_10k"))
        assertEquals(R.drawable.shop_ticket_gold, shopIcon("service_50k"))
    }

    @Test fun everyCardHasItsOwnIcon() {
        val icons = SafetyGuide.cards.map { guideIcon(it.id) }
        icons.forEach { assertNotEquals(0, it) }
        assertEquals(icons.size, icons.distinct().size)
    }
}
