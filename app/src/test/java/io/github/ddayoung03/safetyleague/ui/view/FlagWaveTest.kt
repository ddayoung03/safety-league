package io.github.ddayoung03.safetyleague.ui.view

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class FlagWaveTest {
    private val w = 104f

    @Test fun poleSideDoesNotMove() {
        for (t in listOf(0f, 0.3f, 1.7f, 9f)) assertEquals(0f, flagWave(0f, w, t), 0f)
    }

    @Test fun freeEndMovesOverTime() {
        val a = flagWave(w, w, 0f)
        assertTrue((0..40).any { abs(flagWave(w, w, it * 0.05f) - a) > 1f })
    }

    @Test fun waveStaysInsideLimit() {
        for (i in 0..104) for (t in 0..200) assertTrue(abs(flagWave(i.toFloat(), w, t * 0.05f)) <= 14f)
    }
}
