package io.github.ddayoung03.safetyleague.core.collector

import io.github.ddayoung03.safetyleague.core.data.DemoData
import io.github.ddayoung03.safetyleague.core.model.Point2D
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 시연 및 테스트용 가상 차량 신호 생성기
 */
class MockVehicleDataSource(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : VehicleDataSource {

    private var listener: VehicleSignalListener? = null
    private var simulationJob: Job? = null

    /** 각본 주행이 진행 중인지 */
    val isRunning: Boolean get() = simulationJob?.isActive == true

    override fun startListening(listener: VehicleSignalListener) {
        this.listener = listener
    }

    override fun stopListening() {
        simulationJob?.cancel()
        simulationJob = null
        this.listener = null
    }

    /**
     * 이긴 날 (11/7 토 18.4km) 시뮬레이션 재생.
     * 실제 TripCollector·ScoreCalculator로 계산하면 92점: 부드러움 30 / 매너 25 / 에코 12 / 안전 15 / 꾸준함 10
     * (아이오닉 5 리그 평균 전비 5.8, 각본 전 연속 기록 10일 이상일 때. MockVehicleDataSourceTest로 고정)
     */
    fun simulateWinDay(speedMultiplier: Long = 100L) {
        val l = listener ?: return
        simulationJob?.cancel()

        simulationJob = scope.launch {
            val now = System.currentTimeMillis()
            fun speed(mps: Float, at: Long) = l.onSpeedChanged(mps, now + at)

            // 1. 출발 전: P단, 전 좌석 안전벨트 착용 완료
            l.onGearChanged(GearState.PARK, now)
            l.onSeatBeltChanged(mapOf(0 to true, 1 to true), now + 100)
            l.onOdometerChanged(1000.0, now + 100)
            delay(10 * speedMultiplier)

            // 2. 출발: D단 진입, 움직이면 트립 시작
            l.onGearChanged(GearState.DRIVE, now + 500)
            speed(2f, 1000)
            l.onEfficiencyChanged(6.032, now + 1000) // 리그 평균 5.8의 104% → 에코 12
            l.onLocationChanged(DemoData.route[0], now + 1000)
            delay(10 * speedMultiplier)

            // 3. 부드러운 가속 (초당 2~2.5 m/s, 급가속 기준 3.0 아래)
            speed(4f, 2000); speed(6.5f, 3000); speed(9f, 4000); speed(11.5f, 5000)
            delay(10 * speedMultiplier)
            speed(14f, 6000); speed(16f, 7000); speed(18f, 8000) // 약 65km/h
            l.onLocationChanged(DemoData.route[4], now + 8000)
            speed(18f, 9000)
            delay(10 * speedMultiplier)

            // 깜빡이 켜고 차선 변경 (정상 매너 운전, 조향 1.5초 유지)
            l.onTurnSignalChanged(TurnSignalState(left = true), now + 9500)
            l.onSteeringWheelAngleChanged(-16f, now + 10000)
            delay(5 * speedMultiplier)
            l.onSteeringWheelAngleChanged(0f, now + 11500)
            l.onTurnSignalChanged(TurnSignalState(left = false), now + 12000)
            l.onLocationChanged(DemoData.route[8], now + 12000)
            speed(18f, 12000)
            delay(10 * speedMultiplier)

            // 4. 도착 지점 접근 및 완만한 감속 (초당 2.5 m/s, 급제동 기준 3.5 아래)
            l.onLocationChanged(DemoData.route.last(), now + 13000)
            l.onBrakePedalChanged(true, now + 13000)
            speed(15.5f, 13000); speed(13f, 14000); speed(10.5f, 15000); speed(8f, 16000)
            delay(5 * speedMultiplier)
            speed(5.5f, 17000); speed(3f, 18000); speed(0.5f, 19000); speed(0f, 19500)
            l.onBrakePedalChanged(false, now + 19500)
            l.onOdometerChanged(1018.4, now + 19500) // 18.4km 완료
            delay(5 * speedMultiplier)

            // 5. P단 주차 (트립 종료 및 결과 카드 트리거!)
            l.onGearChanged(GearState.PARK, now + 20000)
        }
    }

    /**
     * 아쉬운 날 (22.1km, 깜빡이 누락 4회, 급제동 2회, 급가속 1회) 시뮬레이션 재생.
     * 실제 TripCollector·ScoreCalculator로 계산하면 64점: 부드러움 25 / 매너 0 / 에코 14 / 안전 15 / 꾸준함 10
     * (아이오닉 5 리그 평균 전비 5.8, 각본 전 연속 기록 10일 이상일 때. MockVehicleDataSourceTest로 고정)
     */
    fun simulateLowDay(speedMultiplier: Long = 100L) {
        val l = listener ?: return
        simulationJob?.cancel()

        simulationJob = scope.launch {
            val now = System.currentTimeMillis()
            fun speed(mps: Float, at: Long) = l.onSpeedChanged(mps, now + at)
            fun steer(deg: Float, at: Long) = l.onSteeringWheelAngleChanged(deg, now + at)

            l.onGearChanged(GearState.PARK, now)
            l.onSeatBeltChanged(mapOf(0 to true), now + 100)
            l.onOdometerChanged(2000.0, now + 100)
            delay(5 * speedMultiplier)

            // D단 출발
            l.onGearChanged(GearState.DRIVE, now + 500)
            speed(2f, 1000)
            l.onEfficiencyChanged(6.264, now + 1000) // 리그 평균 5.8의 108% → 에코 14
            speed(4f, 2000); speed(6f, 3000)
            delay(5 * speedMultiplier)

            // 1) 급가속: 0.5초마다 +2 m/s (4 m/s²)
            speed(8f, 3500); speed(10f, 4000); speed(12f, 4500); speed(13f, 5000); speed(14f, 6000)
            l.onLocationChanged(Point2D(14.0, 50.0), now + 6000)
            delay(5 * speedMultiplier)

            // 2) 깜빡이 없는 차선 변경 1 (조향 1.5초 유지)
            l.onTurnSignalChanged(TurnSignalState(false, false), now + 6200)
            steer(18f, 6500); steer(0f, 8000)
            l.onLocationChanged(Point2D(26.0, 34.0), now + 8000)
            speed(18f, 9000)
            delay(5 * speedMultiplier)

            // 3) 급제동 1: 0.5초마다 -3 m/s (6 m/s²)
            l.onBrakePedalChanged(true, now + 10000)
            speed(18f, 10000); speed(15f, 10500); speed(12f, 11000); speed(10f, 11500); speed(9f, 12000)
            l.onBrakePedalChanged(false, now + 12000)
            speed(9f, 13000)
            l.onLocationChanged(Point2D(46.0, 30.0), now + 13000)
            delay(5 * speedMultiplier)

            // 4) 깜빡이 없는 차선 변경 2
            steer(-20f, 13500)
            speed(11f, 14000)
            steer(0f, 15000)
            speed(13f, 15000); speed(15f, 16000)
            l.onLocationChanged(Point2D(58.0, 14.0), now + 16000)
            delay(5 * speedMultiplier)

            // 5) 깜빡이 없는 차선 변경 3
            steer(16f, 16500)
            speed(15f, 17000)
            steer(0f, 18000)
            speed(15f, 18000)
            l.onLocationChanged(Point2D(80.0, 12.0), now + 18000)
            delay(5 * speedMultiplier)

            // 6) 급제동 2
            l.onBrakePedalChanged(true, now + 19000)
            speed(15f, 19000); speed(12f, 19500); speed(9.5f, 20000)
            l.onBrakePedalChanged(false, now + 20000)
            speed(9f, 21000)
            l.onLocationChanged(Point2D(88.0, 20.0), now + 21000)
            delay(5 * speedMultiplier)

            // 7) 깜빡이 없는 차선 변경 4
            steer(-17f, 21500)
            speed(9f, 22000)
            steer(0f, 23000)
            speed(9f, 23000)
            l.onLocationChanged(Point2D(90.0, 48.0), now + 23000)
            delay(5 * speedMultiplier)

            // 도착: 완만하게 멈추고 P단 주차
            speed(6.5f, 24000); speed(4f, 25000); speed(1.5f, 26000); speed(0f, 27000)
            l.onOdometerChanged(2022.1, now + 27000) // 22.1km
            delay(5 * speedMultiplier)

            l.onGearChanged(GearState.PARK, now + 27500)
        }
    }
}
