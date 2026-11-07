package io.github.ddayoung03.safetyleague.core.calculator

import io.github.ddayoung03.safetyleague.core.model.Trip
import org.junit.Assert.*
import org.junit.Test
import java.time.YearMonth

class SettlementCardTest {
    private val oct = YearMonth.of(2026, 10)
    private val nov = YearMonth.of(2026, 11)
    private fun result(s: YearMonth, from: String, to: String, move: TierMove) = SeasonResult(s, from, to, move, 10)

    @Test fun oneSeasonKeepsBreakdown() {
        val c = SettlementCard.of(listOf(Trip("2026-10-05", 85.0, 640.0)), listOf(result(oct, "골드", "골드", TierMove.STAYED)), 2172)!!
        assertEquals("10월 시즌", c.seasonLabel)
        assertEquals(1248, c.points)
        assertEquals(3420, c.balance)
        assertEquals(640, c.km)
        assertEquals(30, c.bonusPercent)
        assertEquals(150, c.multiplierPercent)
        assertEquals(1, c.seasons)
        assertNull(c.promotedTo)
    }

    @Test fun tierOfThatSeasonIsUsedAfterPromotion() {
        val c = SettlementCard.of(listOf(Trip("2026-10-05", 85.0, 100.0)), listOf(result(oct, "골드", "플래티넘", TierMove.PROMOTED)), 0)!!
        assertEquals(195, c.points)
        assertEquals("플래티넘", c.promotedTo)
    }

    @Test fun skippedSeasonsAddUpEachWithItsTier() {
        val trips = listOf(Trip("2026-10-05", 85.0, 100.0), Trip("2026-11-05", 85.0, 100.0))
        val c = SettlementCard.of(trips, listOf(result(oct, "골드", "플래티넘", TierMove.PROMOTED), result(nov, "플래티넘", "플래티넘", TierMove.STAYED)), 0)!!
        assertEquals(195 + 260, c.points)
        assertEquals(2, c.seasons)
        assertEquals("2개 시즌 합산", c.seasonLabel)
    }

    @Test fun cardShowsDistanceThatWasCounted() {
        val c = SettlementCard.of(listOf(Trip("2026-10-05", 95.0, 1200.0)), listOf(result(oct, "다이아", "다이아", TierMove.STAYED)), 0)!!
        assertEquals(1000, c.km)
        assertEquals(3750, c.points)
    }

    @Test fun noPointsNoCard() {
        assertNull(SettlementCard.of(emptyList(), listOf(result(oct, "골드", "골드", TierMove.STAYED)), 100))
    }

    @Test fun encodeRoundTripAndBrokenText() {
        val c = SettlementCard("10월 시즌", "골드", 1248, 3420, 640, 86.4, 30, 150, 1, null, "안내")
        assertEquals(c, SettlementCard.decode(c.encode()))
        assertEquals(c.copy(average = null, promotedTo = "다이아"), SettlementCard.decode(c.copy(average = null, promotedTo = "다이아").encode()))
        assertNull(SettlementCard.decode("망가진 값"))
        assertNull(SettlementCard.decode(null))
    }
}
