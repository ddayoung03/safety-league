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
     * 이긴 날 (11/7 금 92점 18.4km 주행) 시뮬레이션 재생
     */
    fun simulateWinDay(speedMultiplier: Long = 100L) {
        val l = listener ?: return
        simulationJob?.cancel()

        simulationJob = scope.launch {
            val now = System.currentTimeMillis()
            // 1. 출발 전: P단, 전 좌석 안전벨트 착용 완료
            l.onGearChanged(GearState.PARK, now)
            l.onSeatBeltChanged(mapOf(0 to true, 1 to true), now + 100)
            l.onOdometerChanged(1000.0, now + 100)
            delay(10 * speedMultiplier)

            // 2. 출발: D단 진입 (트립 시작)
            l.onGearChanged(GearState.DRIVE, now + 500)
            delay(10 * speedMultiplier)

            // 3. 부드러운 가속 및 주행
            l.onSpeedChanged(10f, now + 1000)
            l.onLocationChanged(DemoData.route[0], now + 1000)
            delay(10 * speedMultiplier)

            l.onSpeedChanged(18f, now + 2000) // 약 65km/h 정속 주행
            l.onLocationChanged(DemoData.route[4], now + 2000)
            delay(10 * speedMultiplier)

            // 깜빡이 켜고 차선 변경 (정상 매너 운전)
            l.onTurnSignalChanged(TurnSignalState(left = true), now + 3000)
            l.onSteeringWheelAngleChanged(-16f, now + 3200)
            delay(5 * speedMultiplier)
            l.onSteeringWheelAngleChanged(0f, now + 3600)
            l.onTurnSignalChanged(TurnSignalState(left = false), now + 3800)
            l.onLocationChanged(DemoData.route[8], now + 3800)
            delay(10 * speedMultiplier)

            // 4. 도착 지점 접근 및 감속
            l.onLocationChanged(DemoData.route.last(), now + 4500)
            l.onBrakePedalChanged(true, now + 4500)
            l.onSpeedChanged(5f, now + 4800)
            l.onSpeedChanged(0f, now + 5000)
            l.onBrakePedalChanged(false, now + 5000)
            l.onOdometerChanged(1018.4, now + 5000) // 18.4km 완료
            delay(5 * speedMultiplier)

            // 5. P단 주차 (트립 종료 및 결과 카드 트리거!)
            l.onGearChanged(GearState.PARK, now + 5200)
        }
    }

    /**
     * 아쉬운 날 (64점 22.1km, 깜빡이 누락 4회, 급제동 2회, 급가속 1회) 시뮬레이션 재생
     */
    fun simulateLowDay(speedMultiplier: Long = 100L) {
        val l = listener ?: return
        simulationJob?.cancel()

        simulationJob = scope.launch {
            val now = System.currentTimeMillis()
            l.onGearChanged(GearState.PARK, now)
            l.onSeatBeltChanged(mapOf(0 to true), now + 100)
            l.onOdometerChanged(2000.0, now + 100)
            delay(5 * speedMultiplier)

            // D단 출발
            l.onGearChanged(GearState.DRIVE, now + 500)
            delay(5 * speedMultiplier)

            // 1) 급가속
            l.onSpeedChanged(5f, now + 1000)
            l.onSpeedChanged(12f, now + 1500) // accel > 3.0 m/s²
            l.onLocationChanged(Point2D(14.0, 50.0), now + 1500)
            delay(5 * speedMultiplier)

            // 2) 깜빡이 없는 차선 변경 1
            l.onTurnSignalChanged(TurnSignalState(false, false), now + 2000)
            l.onSteeringWheelAngleChanged(18f, now + 2200)
            l.onSteeringWheelAngleChanged(0f, now + 2600)
            l.onLocationChanged(Point2D(26.0, 34.0), now + 2600)
            delay(5 * speedMultiplier)

            // 3) 급제동 1
            l.onBrakePedalChanged(true, now + 3000)
            l.onSpeedChanged(18f, now + 3000)
            l.onSpeedChanged(8f, now + 3500) // 감속 > -3.5 m/s²
            l.onBrakePedalChanged(false, now + 3600)
            l.onLocationChanged(Point2D(46.0, 30.0), now + 3600)
            delay(5 * speedMultiplier)

            // 4) 깜빡이 없는 차선 변경 2
            l.onSteeringWheelAngleChanged(-20f, now + 4000)
            l.onSteeringWheelAngleChanged(0f, now + 4400)
            l.onLocationChanged(Point2D(58.0, 14.0), now + 4400)
            delay(5 * speedMultiplier)

            // 5) 깜빡이 없는 차선 변경 3
            l.onSteeringWheelAngleChanged(16f, now + 5000)
            l.onSteeringWheelAngleChanged(0f, now + 5400)
            l.onLocationChanged(Point2D(80.0, 12.0), now + 5400)
            delay(5 * speedMultiplier)

            // 6) 급제동 2
            l.onBrakePedalChanged(true, now + 6000)
            l.onSpeedChanged(15f, now + 6000)
            l.onSpeedChanged(6f, now + 6500)
            l.onBrakePedalChanged(false, now + 6600)
            l.onLocationChanged(Point2D(88.0, 20.0), now + 6600)
            delay(5 * speedMultiplier)

            // 7) 깜빡이 없는 차선 변경 4
            l.onSteeringWheelAngleChanged(-17f, now + 7000)
            l.onSteeringWheelAngleChanged(0f, now + 7400)
            l.onLocationChanged(Point2D(90.0, 48.0), now + 7400)
            delay(5 * speedMultiplier)

            // 도착 및 P단 주차
            l.onSpeedChanged(0f, now + 8000)
            l.onOdometerChanged(2022.1, now + 8000) // 22.1km
            delay(5 * speedMultiplier)

            l.onGearChanged(GearState.PARK, now + 8500)
        }
    }
}
