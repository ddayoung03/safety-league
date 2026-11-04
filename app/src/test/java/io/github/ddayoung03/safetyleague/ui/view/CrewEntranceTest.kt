package io.github.ddayoung03.safetyleague.ui.view

import io.github.ddayoung03.safetyleague.weather.MotionGear
import org.junit.Assert.*
import org.junit.Test

class CrewEntranceTest {
    @Test fun entranceRequiresParkResumedAndUnrestricted() {
        assertTrue(canAnimateCrew(MotionGear.PARK, 0f, true, false))
        for (gear in MotionGear.entries.filter { it != MotionGear.PARK })
            assertFalse(canAnimateCrew(gear, 0f, true, false))
        assertFalse(canAnimateCrew(MotionGear.PARK, 0f, false, false))
        assertFalse(canAnimateCrew(MotionGear.PARK, 0f, true, true))
        assertFalse(canAnimateCrew(MotionGear.PARK, 1f, true, false))
        assertFalse(canAnimateCrew(MotionGear.PARK, -1f, true, false))
    }
}
