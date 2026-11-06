package io.github.ddayoung03.safetyleague.ui.motion

import io.github.ddayoung03.safetyleague.weather.MotionGear
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundPolicyTest {
    @Test fun parkedWithFocusPlays() = assertTrue(canPlaySound(MotionGear.PARK, 0f, true, false, true))

    @Test fun driveNeverPlays() {
        for (gear in MotionGear.values().filter { it != MotionGear.PARK })
            assertFalse(canPlaySound(gear, 0f, true, false, true))
    }

    @Test fun restrictedNeverPlays() = assertFalse(canPlaySound(MotionGear.PARK, 0f, true, true, true))

    @Test fun movingNeverPlays() = assertFalse(canPlaySound(MotionGear.PARK, 1f, true, false, true))

    @Test fun noFocusStaysSilent() = assertFalse(canPlaySound(MotionGear.PARK, 0f, true, false, false))
}
