package io.github.ddayoung03.safetyleague.ui

import io.github.ddayoung03.safetyleague.R
import io.github.ddayoung03.safetyleague.core.calculator.TierMovement
import org.junit.Assert.assertEquals
import org.junit.Test

class TierBeltTest {
    @Test fun everyTierHasItsOwnBelt() {
        assertEquals(5, TierMovement.tiers.map(::tierBelt).distinct().size)
        assertEquals(R.drawable.tier_belt_diamond, tierBelt("다이아"))
    }

    @Test fun unknownTierFallsBackToBronze() {
        assertEquals(R.drawable.tier_belt_bronze, tierBelt("없는 티어"))
    }
}
