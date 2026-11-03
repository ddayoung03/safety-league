package io.github.ddayoung03.safetyleague.core.collector

import io.github.ddayoung03.safetyleague.core.calculator.LeagueCalculator
import io.github.ddayoung03.safetyleague.core.calculator.ScoreCalculator
import io.github.ddayoung03.safetyleague.core.model.CompletedTrip
import io.github.ddayoung03.safetyleague.core.model.Point2D
import io.github.ddayoung03.safetyleague.core.model.ReplayEvent
import io.github.ddayoung03.safetyleague.core.model.Trip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

/**
 * 실시간 차량 신호를 수집하여 트립 기록 및 점수를 산출하는 코어 수집기
 */
class TripCollector(
    val date: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
    var streakDays: Int = 0,
    var efficiencyRatio: Double = 1.0,
    val onTripCompleted: (CompletedTrip) -> Unit = {}
) : VehicleSignalListener {

    var isTracking: Boolean = false
        private set

    var startTimeMs: Long = 0L
        private set
    var endTimeMs: Long = 0L
        private set

    private var startOdometerKm: Double = -1.0
    private var currentOdometerKm: Double = 0.0

    var hardAccels: Int = 0
        private set
    var hardBrakes: Int = 0
        private set
    var unindicatedLaneChanges: Int = 0
        private set

    var allBeltsFastenedBeforeDrive: Boolean = true
        private set
    var unresolvedWarningSeconds: Long = 0L
        private set

    private var warningStartMs: Long = 0L
    private var isBrakePressed: Boolean = false
    private var currentTurnSignal: TurnSignalState = TurnSignalState()
    private var currentSeatBeltMap: Map<Int, Boolean> = emptyMap()

    private var lastSpeedMps: Float = 0f
    private var lastSpeedTimeMs: Long = 0L

    // 조향각 기반 차선 변경 감지 상태 머신
    private var isSteeringManeuver: Boolean = false
    private var steeringTurnSignalUsed: Boolean = false
    private var steeringStartTimeMs: Long = 0L

    // 이벤트 및 주행 경로
    private val rawEvents = mutableListOf<RawEvent>()
    val route = mutableListOf<Point2D>()

    private data class RawEvent(
        val timestampMs: Long,
        val kind: String,
        val point: Point2D? = null
    )

    override fun onGearChanged(gear: GearState, timestampMs: Long) {
        if (gear != GearState.PARK && !isTracking) {
            // 트립 시작!
            isTracking = true
            startTimeMs = timestampMs
            hardAccels = 0
            hardBrakes = 0
            unindicatedLaneChanges = 0
            unresolvedWarningSeconds = 0L
            rawEvents.clear()
            route.clear()

            // 출발 시점 전 좌석 안전벨트 착용 확인
            allBeltsFastenedBeforeDrive = if (currentSeatBeltMap.isNotEmpty()) {
                currentSeatBeltMap.values.all { it }
            } else {
                true
            }
        } else if (gear == GearState.PARK && isTracking) {
            // 트립 종료!
            endTimeMs = timestampMs
            isTracking = false

            val tripDistanceKm = if (startOdometerKm >= 0.0) {
                max(0.0, currentOdometerKm - startOdometerKm)
            } else {
                0.0
            }

            // 5대 배점 종합 계산
            val detailScore = ScoreCalculator.calculateTripScore(
                km = tripDistanceKm,
                hardAccels = hardAccels,
                hardBrakes = hardBrakes,
                unindicatedLaneChanges = unindicatedLaneChanges,
                efficiencyRatio = efficiencyRatio,
                allBeltsFastenedBeforeDrive = allBeltsFastenedBeforeDrive,
                unresolvedWarningSeconds = unresolvedWarningSeconds,
                streakDays = streakDays
            )

            val isEligible = LeagueCalculator.isEligibleTrip(tripDistanceKm)
            val finalScore = if (isEligible) detailScore.total else 0.0

            val trip = Trip(
                date = date,
                score = finalScore,
                km = LeagueCalculator.round1(tripDistanceKm),
                detailScore = detailScore
            )

            // 사건들의 상대 위치 t(0.0..1.0) 계산
            val durationMs = max(1L, endTimeMs - startTimeMs)
            val replayEvents = rawEvents.map { raw ->
                val ratio = (raw.timestampMs - startTimeMs).toDouble() / durationMs.toDouble()
                val clampedRatio = max(0.01, minOf(0.99, LeagueCalculator.round1(ratio * 100.0) / 100.0))
                ReplayEvent(
                    at = formatTime(raw.timestampMs),
                    kind = raw.kind,
                    t = clampedRatio
                )
            }.sortedBy { it.t }

            val completed = CompletedTrip(
                trip = trip,
                events = replayEvents,
                route = route.toList(),
                startTime = formatTime(startTimeMs),
                endTime = formatTime(endTimeMs),
                isEligible = isEligible
            )

            onTripCompleted(completed)
        }
    }

    override fun onOdometerChanged(distanceKm: Double, timestampMs: Long) {
        if (startOdometerKm < 0.0) {
            startOdometerKm = distanceKm
        }
        currentOdometerKm = distanceKm
    }

    override fun onSpeedChanged(speedMps: Float, timestampMs: Long) {
        if (!isTracking) {
            lastSpeedMps = speedMps
            lastSpeedTimeMs = timestampMs
            return
        }

        if (lastSpeedTimeMs > 0L) {
            val dt = (timestampMs - lastSpeedTimeMs) / 1000.0
            if (dt in 0.1..3.0) {
                val accel = (speedMps - lastSpeedMps) / dt
                if (accel > 3.0) { // 급가속 (> 3.0 m/s²)
                    hardAccels++
                    rawEvents.add(RawEvent(timestampMs, "accel", route.lastOrNull()))
                } else if (accel < -3.5 && isBrakePressed) { // 급감속/급제동 (< -3.5 m/s² + 브레이크)
                    hardBrakes++
                    rawEvents.add(RawEvent(timestampMs, "brake", route.lastOrNull()))
                }
            }
        }

        lastSpeedMps = speedMps
        lastSpeedTimeMs = timestampMs
    }

    override fun onBrakePedalChanged(isPressed: Boolean, timestampMs: Long) {
        isBrakePressed = isPressed
    }

    override fun onTurnSignalChanged(turnSignal: TurnSignalState, timestampMs: Long) {
        currentTurnSignal = turnSignal
        if (turnSignal.isAnyOn && isSteeringManeuver) {
            steeringTurnSignalUsed = true
        }
    }

    override fun onSteeringWheelAngleChanged(angleDeg: Float, timestampMs: Long) {
        if (!isTracking) return

        // 30km/h(8.3m/s) 이상 주행 중에만 차선 변경 감지
        if (lastSpeedMps >= 8.3f) {
            val absAngle = abs(angleDeg)
            if (!isSteeringManeuver && absAngle >= 15f) {
                // 조향 각도 변화 시작
                isSteeringManeuver = true
                steeringStartTimeMs = timestampMs
                steeringTurnSignalUsed = currentTurnSignal.isAnyOn
            } else if (isSteeringManeuver && absAngle < 5f) {
                // 조향 복귀 (차선 변경 완료)
                val maneuverDuration = timestampMs - steeringStartTimeMs
                if (maneuverDuration in 300..4000) {
                    if (!steeringTurnSignalUsed && !currentTurnSignal.isAnyOn) {
                        unindicatedLaneChanges++
                        rawEvents.add(RawEvent(timestampMs, "lane", route.lastOrNull()))
                    }
                }
                isSteeringManeuver = false
                steeringTurnSignalUsed = false
            }
        } else {
            isSteeringManeuver = false
        }
    }

    override fun onSeatBeltChanged(seatBuckledMap: Map<Int, Boolean>, timestampMs: Long) {
        currentSeatBeltMap = seatBuckledMap
    }

    override fun onTelltaleChanged(warningActive: Boolean, timestampMs: Long) {
        if (!isTracking) return

        if (warningActive) {
            if (warningStartMs == 0L) {
                warningStartMs = timestampMs
            }
        } else {
            if (warningStartMs > 0L) {
                val durationSec = (timestampMs - warningStartMs) / 1000L
                unresolvedWarningSeconds += durationSec
                warningStartMs = 0L
            }
        }
    }

    override fun onDoorChanged(doorOpen: Boolean, timestampMs: Long) {
        // 주행 중 도어 열림 발생 시 안전 기본기 감점 요소
    }

    override fun onLocationChanged(point: Point2D, timestampMs: Long) {
        if (isTracking) {
            route.add(point)
        }
    }

    /**
     * 시뮬레이터 및 테스트용 직접 이벤트 주입 메서드
     */
    fun triggerEvent(kind: String, timestampMs: Long, point: Point2D? = null) {
        when (kind) {
            "accel" -> hardAccels++
            "brake" -> hardBrakes++
            "lane" -> unindicatedLaneChanges++
        }
        rawEvents.add(RawEvent(timestampMs, kind, point))
        if (point != null) {
            route.add(point)
        }
    }

    private fun formatTime(timeMs: Long): String {
        return SimpleDateFormat("H:mm", Locale.getDefault()).format(Date(timeMs))
    }
}
