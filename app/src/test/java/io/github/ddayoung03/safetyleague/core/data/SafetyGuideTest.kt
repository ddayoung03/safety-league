package io.github.ddayoung03.safetyleague.core.data

import io.github.ddayoung03.safetyleague.weather.Weather
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyGuideTest {
    @Test fun everyCardHasStepsAndASource() {
        for (c in SafetyGuide.cards) {
            assertTrue(c.id, c.steps.size in 2..5)
            assertTrue(c.id, c.source.isNotBlank())
            assertFalse(c.id, c.steps.any { it.isBlank() })
        }
    }

    @Test fun cardIdsAreUnique() {
        assertEquals(SafetyGuide.cards.size, SafetyGuide.cards.map { it.id }.distinct().size)
    }

    @Test fun everyCategoryHasCards() {
        assertEquals(listOf(4, 6, 5), GuideCategory.entries.map { SafetyGuide.of(it).size })
    }

    @Test fun rainAndSnowComeFirstInSituations() {
        assertEquals("rain", SafetyGuide.of(GuideCategory.SITUATION, Weather.RAIN).first().id)
        assertEquals("snow", SafetyGuide.of(GuideCategory.SITUATION, Weather.SNOW).first().id)
        assertEquals("rain", SafetyGuide.of(GuideCategory.SITUATION, Weather.SUN).first().id)
    }

    @Test fun weatherDoesNotReorderOtherCategories() {
        assertEquals(SafetyGuide.of(GuideCategory.DISASTER), SafetyGuide.of(GuideCategory.DISASTER, Weather.SNOW))
    }

    @Test fun onlyRainAndSnowSuggestACard() {
        assertEquals("rain", SafetyGuide.suggested(Weather.RAIN)?.id)
        assertEquals("snow", SafetyGuide.suggested(Weather.SNOW)?.id)
        assertNull(SafetyGuide.suggested(Weather.SUN))
        assertNull(SafetyGuide.suggested(Weather.NIGHT))
        assertNull(SafetyGuide.suggested(null))
    }

    @Test fun rightTurnObeysItsOwnSignalFirst() {
        assertTrue(SafetyGuide.cards.first { it.id == "right" }.steps.first().contains("우회전 신호등"))
    }

    @Test fun floodMovesTheCarBeforeLeavingIt() {
        val steps = SafetyGuide.cards.first { it.id == "flood" }.steps
        assertTrue(steps.indexOfFirst { it.contains("옮겨요") } < steps.indexOfFirst { it.contains("대피") })
    }

    @Test fun unverifiedAdviceStaysOut() {
        val all = SafetyGuide.cards.flatMap { it.steps }.joinToString()
        assertFalse(all.contains("나눠서"))
        assertFalse(all.contains("30m를 가요"))
    }

    @Test fun laneCardPromisesNoScoreItCannotMeasure() {
        assertNull(SafetyGuide.cards.first { it.id == "lane" }.score)
    }

    @Test fun legalNumbersMatchSources() {
        val rain = SafetyGuide.cards.first { it.id == "rain" }.steps.joinToString()
        assertTrue(rain.contains("20%"))
        assertTrue(rain.contains("50%"))
        val signal = SafetyGuide.cards.first { it.id == "signal" }.steps.joinToString()
        assertTrue(signal.contains("30m"))
        assertTrue(signal.contains("100m"))
    }
}
