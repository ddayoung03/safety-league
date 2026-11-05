package io.github.ddayoung03.safetyleague.core.calculator

import io.github.ddayoung03.safetyleague.core.model.DetailScore
import io.github.ddayoung03.safetyleague.core.model.ReplayEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PitBoardTest {

    private fun events(vararg kinds: Pair<String, Int>) =
        kinds.flatMap { (k, n) -> List(n) { ReplayEvent("6:10", k, 0.5) } }

    @Test fun noEventsButLostSafetyDoesNotBlameBelts() {
        // M7 재현: 사건 0, 벨트 정상, 경고 3분 방치 → 30 + 25 + 20 + 3 + 10. 지금은 "벨트!"
        val a = PitBoard.pick(DetailScore(30.0, 25.0, 20.0, 3.0, 10.0), emptyList())
        assertEquals("기본기!", a.word)
        assertEquals("안전 기본기 +12점 가능", a.gain)
    }

    @Test fun picksTheCategoryThatLostTheMostPointsNotTheMostEvents() {
        // 깜빡이 없는 차선 변경 2회(매너 -6.3) vs 벨트 미착용(안전 -8): 횟수가 아니라 감점으로 고른다
        val a = PitBoard.pick(DetailScore(30.0, 18.8, 20.0, 7.0, 10.0), events("lane" to 2))
        assertEquals("기본기!", a.word)
        assertEquals("안전 기본기 +8점 가능", a.gain)
    }

    @Test fun brakeGainCountsOnlyTheBrakeShareOfSmoothness() {
        // 22.1km, 급가속 1(-1.36) · 급제동 2(-3.62) → 부드러움 25.0. 급제동 몫만 +4 (지금은 +5)
        val a = PitBoard.pick(DetailScore(25.0, 25.0, 20.0, 15.0, 10.0), events("accel" to 1, "brake" to 2))
        assertEquals("천천히!", a.word)
        assertEquals("오늘 급제동 2회", a.reason)
        assertEquals("부드러움 +4점 가능", a.gain)
    }

    @Test fun lowDayScenarioPointsAtSignals() {
        // 64점 날: 25 / 0 / 14 / 15 / 10 → 매너 +25
        val a = PitBoard.pick(DetailScore(25.0, 0.0, 14.0, 15.0, 10.0), events("accel" to 1, "brake" to 2, "lane" to 4))
        assertEquals("깜빡이!", a.word)
        assertEquals("오늘 깜빡이 없이 차선 변경 4회", a.reason)
        assertEquals("매너 +25점 가능", a.gain)
    }

    @Test fun ecoLossIsReportedAsEco() {
        val a = PitBoard.pick(DetailScore(30.0, 25.0, 0.0, 15.0, 3.0), emptyList())
        assertEquals("에코!", a.word)
        assertEquals("에코 +20점 가능", a.gain)
    }

    @Test fun cleanTripHasNoGainAndNoBeltAdvice() {
        // 꾸준함(연속 기록·거리)은 이번 트립에서 고칠 수 있는 것이 아니라 고르지 않는다
        val a = PitBoard.pick(DetailScore(30.0, 25.0, 20.0, 15.0, 3.0), emptyList())
        assertEquals("", a.gain)
        assertFalse(a.tip.contains("벨트"))
    }
}
