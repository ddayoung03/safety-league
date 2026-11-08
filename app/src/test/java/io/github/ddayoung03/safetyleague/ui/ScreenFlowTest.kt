package io.github.ddayoung03.safetyleague.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScreenFlowTest {
    @Test fun restrictedStaysOnDriving() {
        assertEquals(Resume.STAY, resumeAction(Screen.DRIVING, restricted = true, tracking = true, unseenResult = true))
    }

    @Test fun runningTripGoesBackToDrivingEvenAtARedLight() {
        assertEquals(Resume.DRIVING, resumeAction(Screen.LEAGUE, restricted = false, tracking = true, unseenResult = false))
        assertEquals(Resume.STAY, resumeAction(Screen.DRIVING, restricted = false, tracking = true, unseenResult = false))
    }

    @Test fun finishedTripOpensItsResultOnce() {
        assertEquals(Resume.RESULT, resumeAction(Screen.DRIVING, restricted = false, tracking = false, unseenResult = true))
        assertEquals(Resume.RESULT, resumeAction(Screen.LEAGUE, restricted = false, tracking = false, unseenResult = true))
    }

    @Test fun neverStuckOnDrivingAfterTheTripEnded() {
        assertEquals(Resume.HOME, resumeAction(Screen.DRIVING, restricted = false, tracking = false, unseenResult = false))
    }

    @Test fun otherScreensAreLeftAlone() {
        assertEquals(Resume.STAY, resumeAction(Screen.SHOP, restricted = false, tracking = false, unseenResult = false))
    }

    @Test fun backGoesHomeFromSideScreens() {
        for (s in listOf(Screen.REPORT, Screen.SHOP, Screen.GUIDE, Screen.LEAGUE)) assertEquals(Screen.HOME, backTarget(s, hasProfile = true))
    }

    @Test fun vehicleBackNeedsAProfile() {
        assertEquals(Screen.HOME, backTarget(Screen.VEHICLE, hasProfile = true))
        assertNull(backTarget(Screen.VEHICLE, hasProfile = false))
    }

    @Test fun backDoesNothingSpecialOnMainScreens() {
        for (s in listOf(Screen.HOME, Screen.DRIVING, Screen.WIN, Screen.LOW, Screen.SETTLEMENT)) assertNull(backTarget(s, hasProfile = true))
    }
}
