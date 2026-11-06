package io.github.ddayoung03.safetyleague.ui.motion

import io.github.ddayoung03.safetyleague.ui.motion.ResultIntro.BIG
import io.github.ddayoung03.safetyleague.ui.motion.ResultIntro.CALM
import io.github.ddayoung03.safetyleague.ui.motion.ResultIntro.NONE
import io.github.ddayoung03.safetyleague.ui.motion.ResultIntro.QUICK
import org.junit.Assert.assertEquals
import org.junit.Test

class ResultMotionTest {
    @Test fun introFollowsEligibilityWinAndFreshness() {
        assertEquals(BIG, resultIntro(true, true, true, true))
        assertEquals(CALM, resultIntro(true, true, false, true))
        assertEquals(CALM, resultIntro(true, false, true, true))
        assertEquals(QUICK, resultIntro(false, false, true, true))
        assertEquals(NONE, resultIntro(true, true, true, false))
    }

    @Test fun overtakeMovesAtMostThreeRanksUpOnly() {
        assertEquals(listOf(127, 126, 125), overtakeRanks(128, 125))
        assertEquals(listOf(127, 126, 125), overtakeRanks(140, 125))
        assertEquals(listOf(126, 125), overtakeRanks(128, 125, max = 2))
        assertEquals(emptyList<Int>(), overtakeRanks(125, 125))
        assertEquals(emptyList<Int>(), overtakeRanks(125, 126))
        assertEquals(emptyList<Int>(), overtakeRanks(null, 125))
        assertEquals(emptyList<Int>(), overtakeRanks(128, 125, max = 0))
    }

    @Test fun promotionBarFillsAsStepsRunOut() {
        assertEquals(0.8f, promotionProgress(6), 0.001f)
        assertEquals(1f, promotionProgress(0), 0f)
        assertEquals(0f, promotionProgress(200), 0f)
    }

    @Test fun countStartsAtZeroAndLandsExactly() {
        assertEquals(0, countValue(92, 0f))
        assertEquals(92, countValue(92, 1f))
        assertEquals(100, countValue(100, 1f))
    }

    private class FakeClock {
        val queue = mutableListOf<Pair<Long, Runnable>>()
        fun post(r: Runnable, ms: Long) { queue.add(ms to r) }
        fun remove(r: Runnable) { queue.removeAll { it.second === r } }
        fun runAll() { queue.sortedBy { it.first }.forEach { it.second.run() } }
    }

    @Test fun sequenceRunsStepsThenFinishesOnce() {
        val scope = MotionScope()
        val clock = FakeClock()
        val log = mutableListOf<String>()
        val seq = MotionSequence(scope, clock::post, clock::remove) { log += "final" }
        seq.at(10) { log += "a" }
        seq.at(20) { log += "b" }
        seq.endAt(30)
        clock.runAll()
        scope.finishAll()
        assertEquals(listOf("a", "b", "final"), log)
    }

    @Test fun finishAllDropsPendingStepsAndAppliesFinalOnce() {
        val scope = MotionScope()
        val clock = FakeClock()
        val log = mutableListOf<String>()
        val seq = MotionSequence(scope, clock::post, clock::remove) { log += "final" }
        seq.at(10) { log += "a" }
        seq.endAt(30)
        scope.finishAll()
        scope.finishAll()
        assertEquals(0, clock.queue.size)
        assertEquals(listOf("final"), log)
    }

    @Test fun stepThatAlreadyFiredAfterFinishDoesNothing() {
        val scope = MotionScope()
        val log = mutableListOf<String>()
        val kept = mutableListOf<Runnable>()
        val seq = MotionSequence(scope, { r, _ -> kept += r }, {}) { log += "final" }
        seq.at(10) { log += "late" }
        scope.finishAll()
        kept.forEach { it.run() }
        assertEquals(listOf("final"), log)
    }
}
