package io.github.ddayoung03.safetyleague.core.calculator

import io.github.ddayoung03.safetyleague.core.model.DetailScore
import kotlin.math.max
import kotlin.math.min

/**
 * 5대 영역 안전 운전 점수(100점 만점) 계산기
 * - 부드러움 30점: 10km당 급가속·급감속
 * - 매너 25점: 10km당 깜빡이 없는 차선 변경
 * - 에코 20점: 동급 차종 대비 전비/연비 달성도
 * - 안전 기본기 15점: 출발 전 전좌석 벨트, 경고 해결 시간, 도어 닫힘
 * - 꾸준함 10점: 연속 안전 주행 일수 및 거리 기여도
 */
object ScoreCalculator {

    /**
     * 트립 데이터로부터 5대 배점 및 종합 점수 산출
     */
    fun calculateTripScore(
        km: Double,
        hardAccels: Int,
        hardBrakes: Int,
        unindicatedLaneChanges: Int,
        efficiencyRatio: Double, // 기준 대비 달성 비율 (1.0 = 100%)
        allBeltsFastenedBeforeDrive: Boolean,
        unresolvedWarningSeconds: Long,
        streakDays: Int
    ): DetailScore {
        // 3km 미만 트립은 점수 산정 대상 제외 (0점 처리 또는 미반영)
        if (!LeagueCalculator.isEligibleTrip(km)) {
            return DetailScore(0.0, 0.0, 0.0, 0.0, 0.0)
        }

        val distanceFactor = max(km / 10.0, 0.3) // 10km 기준 환산 배수

        // 1. 부드러움 (최대 30점)
        // 10km당 급가속 1회당 -3점, 급제동 1회당 -4점
        val accelRate = hardAccels / distanceFactor
        val brakeRate = hardBrakes / distanceFactor
        val smoothnessDeduction = (accelRate * 3.0) + (brakeRate * 4.0)
        val smoothness = max(0.0, LeagueCalculator.round1(30.0 - smoothnessDeduction))

        // 2. 매너 (최대 25점)
        // 10km당 깜빡이 누락 1회당 -5점
        val laneRate = unindicatedLaneChanges / distanceFactor
        val mannerDeduction = laneRate * 5.0
        val manner = max(0.0, LeagueCalculator.round1(25.0 - mannerDeduction))

        // 3. 에코 (최대 20점)
        // 기준 전비/연비 대비 100% 이상 시 20점, 그 이하는 비율 반영
        val eco = min(20.0, max(0.0, LeagueCalculator.round1(20.0 * efficiencyRatio)))

        // 4. 안전 기본기 (최대 15점)
        // 출발 전 벨트 미착용 출발: -8점, 경고등 방치 30초 초과당 -2점
        var safetyScore = 15.0
        if (!allBeltsFastenedBeforeDrive) {
            safetyScore -= 8.0
        }
        val warningDeduction = (unresolvedWarningSeconds / 30) * 2.0
        safetyScore -= warningDeduction
        val safety = max(0.0, LeagueCalculator.round1(safetyScore))

        // 5. 꾸준함 (최대 10점)
        // 연속 안전 주행 10일 이상 시 만점(7점 기본 + 일수 비례 3점)
        val streakScore = min(10.0, 5.0 + (streakDays * 0.5))
        val consistency = LeagueCalculator.round1(streakScore)

        return DetailScore(
            smoothness = min(30.0, smoothness),
            manner = min(25.0, manner),
            eco = min(20.0, eco),
            safety = min(15.0, safety),
            consistency = min(10.0, consistency)
        )
    }
}
