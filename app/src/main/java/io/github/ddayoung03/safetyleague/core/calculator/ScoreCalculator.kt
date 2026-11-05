package io.github.ddayoung03.safetyleague.core.calculator

import io.github.ddayoung03.safetyleague.core.model.DetailScore
import io.github.ddayoung03.safetyleague.core.model.TripStats

/**
 * 트립 안전 운전 점수(100점) = 부드러움 30 · 매너 25 · 에코 20 · 안전 기본기 15 · 꾸준함 10
 * 3km 미만 트립은 0점(집계 제외). 감점 계수와 근거는 docs/app-dev-log.md에 기록.
 */
object ScoreCalculator {

    // 부드러움: 10km당 급가속 1회 -3, 급제동 1회 -4 (급제동이 사고 위험이 더 커서 더 무겁게)
    const val ACCEL_PENALTY_PER_10KM = 3.0
    const val BRAKE_PENALTY_PER_10KM = 4.0

    // 에코: 리그 평균 대비 80% 이하 0점, 120% 이상 만점, 평균이면 절반
    private const val ECO_FLOOR_RATIO = 0.8
    private const val ECO_FULL_RATIO = 1.2

    // 안전 기본기: 출발 전 미착용 -8, 경고를 30초 방치할 때마다 -2, 주행 중 도어 열림 1회 -3
    private const val BELT_PENALTY = 8.0
    private const val WARNING_PENALTY_PER_30S = 2.0
    private const val DOOR_PENALTY = 3.0

    // 꾸준함: 연속 기록 7점(10일이면 만점) + 주행거리 3점(10km면 만점). 기본점 없음.
    private const val STREAK_POINTS = 7.0
    private const val STREAK_FULL_DAYS = 10.0
    private const val DISTANCE_POINTS = 3.0
    private const val DISTANCE_FULL_KM = 10.0

    fun calculate(s: TripStats): DetailScore {
        if (!LeagueCalculator.isEligibleTrip(s.km)) return DetailScore(0.0, 0.0, 0.0, 0.0, 0.0)
        val per10km = s.km / 10.0

        val smoothness = 30.0 - (s.hardAccels * ACCEL_PENALTY_PER_10KM + s.hardBrakes * BRAKE_PENALTY_PER_10KM) / per10km

        val manner = if (s.laneChanges == 0) 25.0
        else 25.0 * (1.0 - s.unindicatedLaneChanges.toDouble() / s.laneChanges)

        val ratio = s.efficiency?.takeIf { it.isFinite() && it > 0 && s.leagueAvgEfficiency > 0 }
            ?.let { it / s.leagueAvgEfficiency } ?: 1.0
        val eco = 20.0 * (ratio - ECO_FLOOR_RATIO) / (ECO_FULL_RATIO - ECO_FLOOR_RATIO)

        val safety = 15.0 -
            (if (s.allBeltsFastened) 0.0 else BELT_PENALTY) -
            (s.unresolvedWarningSeconds / 30) * WARNING_PENALTY_PER_30S -
            s.doorOpenWhileMoving * DOOR_PENALTY

        val consistency = STREAK_POINTS * minOf(s.streakDays / STREAK_FULL_DAYS, 1.0) +
            DISTANCE_POINTS * minOf(s.km / DISTANCE_FULL_KM, 1.0)

        return DetailScore(
            smoothness = clampRound(smoothness, 30.0),
            manner = clampRound(manner, 25.0),
            eco = clampRound(eco, 20.0),
            safety = clampRound(safety, 15.0),
            consistency = clampRound(consistency, 10.0)
        )
    }

    /**
     * 저장·판정·표시에 쓰는 트립 점수. 결과 카드가 정수로 보여 주므로 정수로 반올림해 저장한다.
     * 그래야 "70"으로 보이는 트립이 연속 안전 주행(70점 이상)에서도 70점으로 판정된다.
     */
    fun tripScore(d: DetailScore): Double = Math.round(LeagueCalculator.round1(d.total)).toDouble()

    private fun clampRound(v: Double, max: Double) = LeagueCalculator.round1(v.coerceIn(0.0, max))
}
