package io.github.ddayoung03.safetyleague.core.collector

import io.github.ddayoung03.safetyleague.core.calculator.LeagueCalculator
import io.github.ddayoung03.safetyleague.core.calculator.ScoreCalculator
import io.github.ddayoung03.safetyleague.core.model.CompletedTrip
import io.github.ddayoung03.safetyleague.core.model.Point2D
import io.github.ddayoung03.safetyleague.core.model.ReplayEvent
import io.github.ddayoung03.safetyleague.core.model.Trip
import io.github.ddayoung03.safetyleague.core.model.TripStats
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * 차량 신호를 받아 트립을 기록하고, P단에서 점수를 매겨 onTripCompleted로 넘긴다.
 * 트립은 P가 아닌 기어에서 실제로 움직였을 때 시작한다 (P→R→P 주차는 트립 아님).
 */
class TripCollector(
    var date: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
    var streakDays: Int = 0,
    var leagueAvgEfficiency: Double = 0.0,
    val onTripStarted: () -> Unit = {},
    val onTripCompleted: (CompletedTrip) -> Unit = {}
) : VehicleSignalListener {

    companion object {
        const val HARD_ACCEL_MPS2 = 3.0     // 일반 출발(약 1.5~2.5 m/s²)보다 확실히 센 가속
        const val HARD_BRAKE_MPS2 = -3.5    // 일반 정지(약 -2 m/s²)보다 확실히 센 감속
        const val MOVING_MPS = 0.5f
        const val LANE_MIN_MPS = 8.3f       // 30 km/h 미만은 차선 변경을 보지 않는다
        const val FAST_MPS = 16.7f          // 60 km/h 이상은 조향을 더 작게 해도 차선이 바뀐다
        const val LANE_ANGLE_SLOW_DEG = 10f // 조향 휠 각도 기준
        const val LANE_ANGLE_FAST_DEG = 4f
        const val LANE_ANGLE_MAX_DEG = 45f  // 이보다 크게 꺾으면 회전
        const val LANE_MIN_MS = 1_000L
        const val LANE_MAX_MS = 6_000L      // 이보다 길게 꺾고 있으면 회전·램프·곡선
        const val SIGNAL_GRACE_MS = 5_000L  // 깜빡이가 자동으로 꺼진 직후의 조향도 정상
        const val ACCEL_WINDOW_MS = 500L    // 실제 속도 신호는 잘게 계단식으로 와서, 가속도는 0.5초 이상 간격으로 잰다
    }

    private var gear = GearState.PARK
    private var tripAvgEfficiency = leagueAvgEfficiency
    var isTracking = false
        private set

    private var startTimeMs = 0L
    private var startOdometerKm = -1.0
    private var currentOdometerKm = -1.0

    var hardAccels = 0; private set
    var hardBrakes = 0; private set
    var laneChanges = 0; private set
    var unindicatedLaneChanges = 0; private set
    private var doorOpenWhileMoving = 0
    private var unresolvedWarningSeconds = 0L
    private var allBeltsFastened = true

    private var warningStartMs = 0L
    private var brakePressed = false
    private var turnSignal = TurnSignalState()
    private var lastSignalOnMs = Long.MIN_VALUE / 2
    private var seatBelts: Map<Int, Boolean> = emptyMap()
    private var efficiency: Double? = null

    private var lastSpeed = 0f
    private var lastSpeedMs = 0L
    private var anchorSpeed = 0f
    private var anchorMs = 0L
    private var integratedMeters = 0.0
    private var inHardAccel = false
    private var inHardBrake = false

    private var inManeuver = false
    private var maneuverStartMs = 0L
    private var maneuverSignalled = false
    private var maneuverPeakDeg = 0f

    /** 이번 트립에서 지금까지 달린 거리 (주행 중 화면용) */
    val currentTripKm: Double
        get() {
            if (!isTracking) return 0.0
            val odoKm = if (startOdometerKm >= 0.0 && currentOdometerKm >= startOdometerKm) currentOdometerKm - startOdometerKm else 0.0
            return if (odoKm > 0.0) odoKm else integratedMeters / 1000.0
        }

    private val rawEvents = mutableListOf<Pair<Long, String>>()
    val route = mutableListOf<Point2D>()

    private fun startTrip(t: Long) {
        onTripStarted()
        tripAvgEfficiency = leagueAvgEfficiency
        isTracking = true
        startTimeMs = t
        startOdometerKm = currentOdometerKm
        hardAccels = 0; hardBrakes = 0; laneChanges = 0; unindicatedLaneChanges = 0
        doorOpenWhileMoving = 0; unresolvedWarningSeconds = 0L; warningStartMs = 0L
        inHardAccel = false; inHardBrake = false; inManeuver = false
        anchorSpeed = lastSpeed; anchorMs = t; integratedMeters = 0.0
        efficiency = null
        rawEvents.clear(); route.clear()
        allBeltsFastened = seatBelts.isEmpty() || seatBelts.values.all { it }
    }

    override fun onGearChanged(gear: GearState, timestampMs: Long) {
        this.gear = gear
        if (gear == GearState.PARK && isTracking) finishTrip(timestampMs)
    }

    override fun onSpeedChanged(speedMps: Float, timestampMs: Long) {
        if (!isTracking && gear != GearState.PARK && speedMps > MOVING_MPS) startTrip(timestampMs)
        if (isTracking && lastSpeedMs > 0L) {
            // 오도미터가 안 움직이는 환경(에뮬레이터)을 위해 속도 × 시간도 쌓아 둔다
            val gap = (timestampMs - lastSpeedMs) / 1000.0
            if (gap in 0.0..5.0) integratedMeters += (lastSpeed + speedMps) / 2.0 * gap

            val dt = (timestampMs - anchorMs) / 1000.0
            if (dt > 3.0) {
                anchorSpeed = speedMps; anchorMs = timestampMs
                inHardAccel = false; inHardBrake = false
            } else if (timestampMs - anchorMs >= ACCEL_WINDOW_MS) {
                val accel = (speedMps - anchorSpeed) / dt
                // 임계값을 넘은 구간 하나가 사건 하나. 기준의 절반 아래로 확실히 떨어져야 끝난 것으로 본다.
                if (accel > HARD_ACCEL_MPS2) {
                    if (!inHardAccel) { hardAccels++; rawEvents += timestampMs to "accel" }
                    inHardAccel = true
                } else if (accel < HARD_ACCEL_MPS2 / 2) inHardAccel = false
                if (accel < HARD_BRAKE_MPS2 && brakePressed) {
                    if (!inHardBrake) { hardBrakes++; rawEvents += timestampMs to "brake" }
                    inHardBrake = true
                } else if (accel > HARD_BRAKE_MPS2 / 2) inHardBrake = false
                anchorSpeed = speedMps; anchorMs = timestampMs
            }
        }
        if (!isTracking) { anchorSpeed = speedMps; anchorMs = timestampMs }
        lastSpeed = speedMps
        lastSpeedMs = timestampMs
    }

    override fun onBrakePedalChanged(isPressed: Boolean, timestampMs: Long) {
        brakePressed = isPressed
    }

    override fun onTurnSignalChanged(turnSignal: TurnSignalState, timestampMs: Long) {
        // 꺼지는 순간까지 켜져 있던 것으로 본다
        if (turnSignal.isAnyOn || this.turnSignal.isAnyOn) lastSignalOnMs = timestampMs
        this.turnSignal = turnSignal
        if (turnSignal.isAnyOn && inManeuver) maneuverSignalled = true
    }

    override fun onSteeringWheelAngleChanged(angleDeg: Float, timestampMs: Long) {
        if (!isTracking) return
        if (lastSpeed < LANE_MIN_MPS) { inManeuver = false; return }
        val threshold = if (lastSpeed >= FAST_MPS) LANE_ANGLE_FAST_DEG else LANE_ANGLE_SLOW_DEG
        val a = abs(angleDeg)
        if (!inManeuver && a >= threshold) {
            inManeuver = true
            maneuverStartMs = timestampMs
            maneuverPeakDeg = a
            maneuverSignalled = turnSignal.isAnyOn || timestampMs - lastSignalOnMs <= SIGNAL_GRACE_MS
        } else if (inManeuver) {
            maneuverPeakDeg = maxOf(maneuverPeakDeg, a)
            if (a < threshold / 2) {
                inManeuver = false
                val duration = timestampMs - maneuverStartMs
                if (duration in LANE_MIN_MS..LANE_MAX_MS && maneuverPeakDeg <= LANE_ANGLE_MAX_DEG) {
                    laneChanges++
                    if (!maneuverSignalled) { unindicatedLaneChanges++; rawEvents += timestampMs to "lane" }
                }
            }
        }
    }

    override fun onSeatBeltChanged(seatBuckledMap: Map<Int, Boolean>, timestampMs: Long) {
        seatBelts = seatBuckledMap
    }

    override fun onTelltaleChanged(warningActive: Boolean, timestampMs: Long) {
        if (!isTracking) return
        if (warningActive && warningStartMs == 0L) warningStartMs = timestampMs
        if (!warningActive && warningStartMs > 0L) closeWarning(timestampMs)
    }

    private fun closeWarning(t: Long) {
        unresolvedWarningSeconds += (t - warningStartMs) / 1000L
        warningStartMs = 0L
    }

    override fun onDoorChanged(doorOpen: Boolean, timestampMs: Long) {
        if (isTracking && doorOpen && lastSpeed > MOVING_MPS) doorOpenWhileMoving++
    }

    override fun onOdometerChanged(distanceKm: Double, timestampMs: Long) {
        currentOdometerKm = distanceKm
        if (isTracking && startOdometerKm < 0.0) startOdometerKm = distanceKm
    }

    override fun onLocationChanged(point: Point2D, timestampMs: Long) {
        if (isTracking) route += point
    }

    override fun onEfficiencyChanged(value: Double, timestampMs: Long) {
        if (isTracking) efficiency = value
    }

    private fun finishTrip(endMs: Long) {
        isTracking = false
        if (warningStartMs > 0L) closeWarning(endMs)
        val odoKm = if (startOdometerKm >= 0.0 && currentOdometerKm >= startOdometerKm) currentOdometerKm - startOdometerKm else 0.0
        // 오도미터가 있으면 그것을 믿고, 안 움직이면(에뮬레이터) 속도 적분값을 쓴다
        val km = if (odoKm > 0.0) odoKm else integratedMeters / 1000.0
        val detail = ScoreCalculator.calculate(
            TripStats(
                km, hardAccels, hardBrakes, laneChanges, unindicatedLaneChanges, efficiency,
                tripAvgEfficiency, allBeltsFastened, unresolvedWarningSeconds, doorOpenWhileMoving, streakDays
            )
        )
        val eligible = LeagueCalculator.isEligibleTrip(km)
        val duration = maxOf(1L, endMs - startTimeMs).toDouble()
        val events = rawEvents.map { (t, kind) ->
            ReplayEvent(formatTime(t), kind, ((t - startTimeMs) / duration).coerceIn(0.01, 0.99))
        }
        onTripCompleted(
            CompletedTrip(
                trip = Trip(date, if (eligible) LeagueCalculator.round1(detail.total) else 0.0, LeagueCalculator.round1(km), detail),
                events = events,
                route = route.toList(),
                startTime = formatTime(startTimeMs),
                endTime = formatTime(endMs),
                isEligible = eligible
            )
        )
    }

    private fun formatTime(timeMs: Long): String = SimpleDateFormat("H:mm", Locale.getDefault()).format(Date(timeMs))
}
