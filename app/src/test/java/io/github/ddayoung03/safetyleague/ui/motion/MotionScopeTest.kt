package io.github.ddayoung03.safetyleague.ui.motion

import org.junit.Assert.assertEquals
import org.junit.Test

class MotionScopeTest {
    @Test fun finishAllRunsEachFinalOnce() {
        val scope = MotionScope()
        var a = 0; var b = 0
        scope.add(onFinish = { a++ }, cancel = {})
        scope.add(onFinish = { b++ }, cancel = {})
        scope.finishAll()
        assertEquals(1, a); assertEquals(1, b)
    }

    @Test fun finishedItemIsNotFinalizedAgain() {
        val scope = MotionScope()
        var n = 0
        scope.add(onFinish = { n++ }, cancel = {}).done()
        scope.finishAll()
        assertEquals(0, n)
    }

    @Test fun finishAllTwiceDoesNothing() {
        val scope = MotionScope()
        var n = 0
        scope.add(onFinish = { n++ }, cancel = {})
        scope.finishAll(); scope.finishAll()
        assertEquals(1, n)
    }

    @Test fun itemAddedInsideFinishIsAlsoFinished() {
        val scope = MotionScope()
        var inner = 0
        scope.add(onFinish = { scope.add(onFinish = { inner++ }, cancel = {}) }, cancel = {})
        scope.finishAll()
        assertEquals(1, inner)
        scope.finishAll()
        assertEquals(1, inner)
    }
}
