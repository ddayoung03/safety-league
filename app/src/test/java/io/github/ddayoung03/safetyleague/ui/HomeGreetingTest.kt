package io.github.ddayoung03.safetyleague.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HomeGreetingTest {
    private val promo = "승급권이에요! 이대로만 가요"
    private val praise = "오늘 주행 멋졌어요! 이 기세로 가요"
    private val gentle = "오늘도 수고했어요. 다음엔 더 부드럽게 가 봐요"
    private val morning = "좋은 아침이에요! 오늘도 안전하게 가 봐요"
    private val day = "어서 와요! 오늘도 안전하게 가 봐요"
    private val evening = "오늘 하루도 수고했어요. 천천히 가요"

    @Test fun missingSpeedPermissionWinsOverEverything() {
        // 속도를 못 받으면 트립이 기록되지 않으니 무엇보다 먼저 알린다
        assertEquals("속도 권한이 없어 주행을 기록할 수 없어요. 눌러서 켜 주세요",
            homeGreeting(8, inPromotionZone = true, todayScore = 90.0, canRecord = false))
    }

    @Test fun promotionWinsOverEverything() {
        assertEquals(promo, homeGreeting(8, inPromotionZone = true, todayScore = 50.0))
    }

    @Test fun todayScoreAboveSeventyPraises() {
        assertEquals(praise, homeGreeting(20, false, 70.0))
        assertEquals(praise, homeGreeting(8, false, 92.0))
    }

    @Test fun lowScoreIsGentle() {
        val g = homeGreeting(12, false, 69.9)
        assertEquals(gentle, g)
        assertFalse(g.contains("!"))
        assertFalse(g.contains("감점"))
    }

    @Test fun timeOfDayWhenNoTripToday() {
        assertEquals(morning, homeGreeting(5, false, null))
        assertEquals(morning, homeGreeting(10, false, null))
        assertEquals(day, homeGreeting(11, false, null))
        assertEquals(day, homeGreeting(17, false, null))
        assertEquals(evening, homeGreeting(18, false, null))
        assertEquals(evening, homeGreeting(4, false, null))
    }
}
