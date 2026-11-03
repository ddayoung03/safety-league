package io.github.ddayoung03.safetyleague.core.calculator

import io.github.ddayoung03.safetyleague.core.model.TripStats
import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreCalculatorTest {

    private fun stats(
        km: Double = 18.4,
        hardAccels: Int = 0,
        hardBrakes: Int = 0,
        laneChanges: Int = 0,
        unindicatedLaneChanges: Int = 0,
        efficiency: Double? = null,
        leagueAvgEfficiency: Double = 5.8,
        allBeltsFastened: Boolean = true,
        unresolvedWarningSeconds: Long = 0,
        doorOpenWhileMoving: Int = 0,
        streakDays: Int = 0
    ) = TripStats(
        km, hardAccels, hardBrakes, laneChanges, unindicatedLaneChanges, efficiency, leagueAvgEfficiency,
        allBeltsFastened, unresolvedWarningSeconds, doorOpenWhileMoving, streakDays
    )

    @Test
    fun shortTripScoresZero() {
        assertEquals(0.0, ScoreCalculator.calculate(stats(km = 2.8, streakDays = 5)).total, 0.001)
    }

    @Test
    fun perfectTripScores100() {
        val s = ScoreCalculator.calculate(stats(laneChanges = 3, efficiency = 7.0, streakDays = 13))
        assertEquals(30.0, s.smoothness, 0.001)
        assertEquals(25.0, s.manner, 0.001)
        assertEquals(20.0, s.eco, 0.001)
        assertEquals(15.0, s.safety, 0.001)
        assertEquals(10.0, s.consistency, 0.001)
        assertEquals(100.0, s.total, 0.001)
    }

    @Test
    fun smoothnessIsPer10km() {
        // 20km에 급가속 2, 급제동 1 → 10km당 1, 0.5 → 30 - 3 - 2 = 25
        assertEquals(25.0, ScoreCalculator.calculate(stats(km = 20.0, hardAccels = 2, hardBrakes = 1)).smoothness, 0.001)
    }

    @Test
    fun mannerIsTheShareOfIndicatedLaneChanges() {
        assertEquals(12.5, ScoreCalculator.calculate(stats(laneChanges = 4, unindicatedLaneChanges = 2)).manner, 0.001)
        assertEquals(25.0, ScoreCalculator.calculate(stats(laneChanges = 0)).manner, 0.001)
    }

    @Test
    fun ecoComparesWithTheLeagueAverage() {
        assertEquals(10.0, ScoreCalculator.calculate(stats(efficiency = 5.8)).eco, 0.001)
        assertEquals(0.0, ScoreCalculator.calculate(stats(efficiency = 5.8 * 0.8)).eco, 0.001)
        assertEquals(20.0, ScoreCalculator.calculate(stats(efficiency = 5.8 * 1.2)).eco, 0.001)
        // 전비 신호가 없거나 이상하면 리그 평균으로 본다
        assertEquals(10.0, ScoreCalculator.calculate(stats(efficiency = null)).eco, 0.001)
        assertEquals(10.0, ScoreCalculator.calculate(stats(efficiency = Double.NaN)).eco, 0.001)
        assertEquals(10.0, ScoreCalculator.calculate(stats(efficiency = 6.0, leagueAvgEfficiency = 0.0)).eco, 0.001)
    }

    @Test
    fun consistencyHasNoFreePointsAndCountsDistance() {
        // 연속 0일, 5km → 거리 요소만 3 × 5/10 = 1.5
        assertEquals(1.5, ScoreCalculator.calculate(stats(km = 5.0, streakDays = 0)).consistency, 0.001)
        // 연속 5일, 10km 이상 → 7 × 5/10 + 3 = 6.5
        assertEquals(6.5, ScoreCalculator.calculate(stats(km = 12.0, streakDays = 5)).consistency, 0.001)
    }

    @Test
    fun safetyNeverGoesBelowZero() {
        val s = ScoreCalculator.calculate(
            stats(allBeltsFastened = false, unresolvedWarningSeconds = 65, doorOpenWhileMoving = 2)
        )
        assertEquals(0.0, s.safety, 0.001)
        // 벨트만 미착용 → 15 - 8 = 7, 경고 65초 → 30초당 2점 → -4
        assertEquals(7.0, ScoreCalculator.calculate(stats(allBeltsFastened = false)).safety, 0.001)
        assertEquals(11.0, ScoreCalculator.calculate(stats(unresolvedWarningSeconds = 65)).safety, 0.001)
        assertEquals(12.0, ScoreCalculator.calculate(stats(doorOpenWhileMoving = 1)).safety, 0.001)
    }

    @Test
    fun totalStaysWithin0To100() {
        val worst = ScoreCalculator.calculate(
            stats(km = 3.0, hardAccels = 50, hardBrakes = 50, laneChanges = 9, unindicatedLaneChanges = 9,
                efficiency = 0.1, allBeltsFastened = false, unresolvedWarningSeconds = 9999, doorOpenWhileMoving = 9)
        )
        assertEquals(0.9, worst.total, 0.001) // 거리 요소 3 × 3/10만 남음
    }
}
