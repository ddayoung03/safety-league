package io.github.ddayoung03.safetyleague.core.collector

import io.github.ddayoung03.safetyleague.core.model.CompletedTrip
import io.github.ddayoung03.safetyleague.core.model.Powertrain
import io.github.ddayoung03.safetyleague.core.model.VehicleProfile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * C3: 시연 각본 신호를 실제 TripCollector·ScoreCalculator로 계산하면 화면의 92점·64점이 나와야 한다.
 * 조건: 시연 리그 차종(아이오닉 5 EV), 각본 전 연속 기록(이긴 날 12일, 아쉬운 날 13일).
 */
class MockVehicleDataSourceTest {

    private fun drive(win: Boolean, streakDays: Int): CompletedTrip {
        val trips = mutableListOf<CompletedTrip>()
        val collector = TripCollector(
            date = "2026-11-07",
            streakDays = streakDays,
            leagueAvgEfficiency = VehicleProfile("아이오닉 5", Powertrain.EV).averageEfficiency
        ) { trips += it }
        runBlocking {
            val mock = MockVehicleDataSource(this)
            mock.startListening(collector)
            if (win) mock.simulateWinDay(speedMultiplier = 0L) else mock.simulateLowDay(speedMultiplier = 0L)
        }
        return trips.single()
    }

    @Test fun winDayScriptScores92() {
        val r = drive(win = true, streakDays = 12)
        val d = r.trip.detailScore!!
        assertEquals(30.0, d.smoothness, 0.001)
        assertEquals(25.0, d.manner, 0.001)
        assertEquals(12.0, d.eco, 0.001)
        assertEquals(15.0, d.safety, 0.001)
        assertEquals(10.0, d.consistency, 0.001)
        assertEquals(92.0, r.trip.score, 0.0)
        assertEquals(18.4, r.trip.km, 0.001)
        assertEquals(0, r.events.size)
    }

    @Test fun lowDayScriptScores64() {
        val r = drive(win = false, streakDays = 13)
        val d = r.trip.detailScore!!
        assertEquals(25.0, d.smoothness, 0.001)
        assertEquals(0.0, d.manner, 0.001)
        assertEquals(14.0, d.eco, 0.001)
        assertEquals(15.0, d.safety, 0.001)
        assertEquals(10.0, d.consistency, 0.001)
        assertEquals(64.0, r.trip.score, 0.0)
        assertEquals(22.1, r.trip.km, 0.001)
        assertEquals(1, r.events.count { it.kind == "accel" })
        assertEquals(2, r.events.count { it.kind == "brake" })
        assertEquals(4, r.events.count { it.kind == "lane" })
    }
}
