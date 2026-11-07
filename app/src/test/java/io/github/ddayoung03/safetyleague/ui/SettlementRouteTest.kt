package io.github.ddayoung03.safetyleague.ui

import org.junit.Assert.*
import org.junit.Test

class SettlementRouteTest {
    @Test fun redirectsOnlyRealLeagueWithCard() {
        assertTrue(redirectToSettlement(Screen.LEAGUE, hasCard = true, demoMode = false))
        assertFalse(redirectToSettlement(Screen.LEAGUE, hasCard = false, demoMode = false))
        assertFalse(redirectToSettlement(Screen.LEAGUE, hasCard = true, demoMode = true))
        assertFalse(redirectToSettlement(Screen.WIN, hasCard = true, demoMode = false))
        assertFalse(redirectToSettlement(Screen.DRIVING, hasCard = true, demoMode = false))
    }
}
