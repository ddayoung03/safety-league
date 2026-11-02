package ai.pleos.safetyleague.core.collector

import ai.pleos.safetyleague.core.model.Point2D

/**
 * 기어 상태
 */
enum class GearState {
    PARK,
    DRIVE,
    REVERSE,
    NEUTRAL
}

/**
 * 방향지시등 상태
 */
data class TurnSignalState(
    val left: Boolean = false,
    val right: Boolean = false
) {
    val isAnyOn: Boolean get() = left || right
}

/**
 * 차량 신호 이벤트 수신 리스너
 */
interface VehicleSignalListener {
    fun onSpeedChanged(speedMps: Float, timestampMs: Long)
    fun onBrakePedalChanged(isPressed: Boolean, timestampMs: Long)
    fun onSteeringWheelAngleChanged(angleDeg: Float, timestampMs: Long)
    fun onTurnSignalChanged(turnSignal: TurnSignalState, timestampMs: Long)
    fun onGearChanged(gear: GearState, timestampMs: Long)
    fun onSeatBeltChanged(seatBuckledMap: Map<Int, Boolean>, timestampMs: Long)
    fun onTelltaleChanged(warningActive: Boolean, timestampMs: Long)
    fun onDoorChanged(doorOpen: Boolean, timestampMs: Long)
    fun onOdometerChanged(distanceKm: Double, timestampMs: Long)
    fun onLocationChanged(point: Point2D, timestampMs: Long)
}

/**
 * 차량 신호 데이터 소스 인터페이스
 */
interface VehicleDataSource {
    fun startListening(listener: VehicleSignalListener)
    fun stopListening()
}
