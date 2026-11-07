package io.github.ddayoung03.safetyleague.core.calculator

import io.github.ddayoung03.safetyleague.core.model.Trip
import org.junit.Assert.*
import org.junit.Test
import java.time.YearMonth

class PointCalculatorTest {
    @Test fun specExamples() {
        assertEquals(550, PointCalculator.points(500.0, 75.0, "브론즈"))
        assertEquals(75, PointCalculator.points(50.0, 95.0, "브론즈"))
        assertEquals(750, PointCalculator.points(500.0, 95.0, "브론즈"))
        assertEquals(1375, PointCalculator.points(500.0, 75.0, "다이아"))
        assertEquals(188, PointCalculator.points(50.0, 95.0, "다이아"))
        assertEquals(1248, PointCalculator.points(640.0, 88.8, "골드"))
        assertEquals(3750, PointCalculator.points(1200.0, 95.0, "다이아"))
        assertEquals(300, PointCalculator.points(300.0, 60.0, "브론즈"))
    }

    @Test fun bonusBoundaries() {
        assertEquals(0, PointCalculator.bonusPercent(null))
        assertEquals(0, PointCalculator.bonusPercent(69.9))
        assertEquals(10, PointCalculator.bonusPercent(70.0))
        assertEquals(10, PointCalculator.bonusPercent(79.9))
        assertEquals(30, PointCalculator.bonusPercent(80.0))
        assertEquals(30, PointCalculator.bonusPercent(89.9))
        assertEquals(50, PointCalculator.bonusPercent(90.0))
    }

    @Test fun multipliers() {
        assertEquals(listOf(100, 120, 150, 200, 250), TierMovement.tiers.map(PointCalculator::multiplierPercent))
        assertEquals(100, PointCalculator.multiplierPercent("없는 티어"))
    }

    @Test fun seasonUsesEligibleTripsOfThatMonthOnly() {
        val trips = listOf(Trip("2026-10-01", 80.0, 100.0), Trip("2026-10-31", 80.0, 2.9), Trip("2026-11-01", 100.0, 500.0))
        val s = PointCalculator.forSeason(trips, YearMonth.of(2026, 10), "골드")
        assertEquals(100.0, s.km, 0.0)
        assertEquals(80.0, s.average!!, 0.0)
        assertEquals(30, s.bonusPercent)
        assertEquals(150, s.multiplierPercent)
        assertEquals(195, s.points)
    }

    @Test fun emptySeasonIsZero() {
        val s = PointCalculator.forSeason(emptyList(), YearMonth.of(2026, 10), "골드")
        assertEquals(0, s.points)
        assertNull(s.average)
    }
}
