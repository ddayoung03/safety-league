package io.github.ddayoung03.safetyleague.core.model

/**
 * 트립 기록 데이터 모델
 * @param date YYYY-MM-DD
 * @param score 트립 안전운전 점수 (0..100)
 * @param km 주행 거리 (km)
 * @param detailScore 점수 5대 구성 요소 (선택적)
 */
data class Trip(
    val date: String,
    val score: Double,
    val km: Double = 0.0,
    val detailScore: DetailScore? = null
)

/**
 * 점수 세부 배점 (총합 100점)
 * - 부드러움 30점
 * - 매너 25점
 * - 에코 20점
 * - 안전 기본기 15점
 * - 꾸준함 10점
 */
data class DetailScore(
    val smoothness: Double, // 부드러움 (급가속/급감속) Max 30
    val manner: Double,     // 매너 (깜빡이 없는 차선 변경) Max 25
    val eco: Double,        // 에코 (전비/연비) Max 20
    val safety: Double,     // 안전 기본기 (전좌석 벨트, 경고 해결 등) Max 15
    val consistency: Double // 꾸준함 (연속 안전 주행 및 거리) Max 10
) {
    val total: Double
        get() = smoothness + manner + eco + safety + consistency
}

/**
 * 리그 정보
 */
data class League(
    val total: Int = 397,
    val tier: String = "골드",
    val model: String = "아이오닉 5",
    val region: String = "부산",
    val seasonStart: String = "2026-11-01",
    val seasonEnd: String = "2026-11-30"
)

/**
 * 순위표 운전자 항목
 */
data class DriverRank(
    val rank: Int,
    val name: String,
    val avg: Double
)

/**
 * 레이스 리플레이 사건 이벤트
 * @param at 발생 시각 (예: "6:21")
 * @param kind 사건 종류 ("accel", "brake", "lane")
 * @param t 전체 경로 거리 상의 상대적 위치 (0.0..1.0)
 */
data class ReplayEvent(
    val at: String,
    val kind: String,
    val t: Double
) {
    /** 자정부터 분. "10:05"처럼 자릿수가 다른 시각도 바르게 비교하려고 쓴다. */
    val minutes: Int get() = toMinutes(at)
}

fun toMinutes(hhmm: String): Int = hhmm.split(":").let { it[0].toInt() * 60 + it[1].toInt() }

/**
 * 2차원 좌표 (경로 polyline 점)
 */
data class Point2D(
    val x: Double,
    val y: Double
)

/**
 * 완료된 트립 전체 결과 데이터
 */
data class CompletedTrip(
    val trip: Trip,
    val events: List<ReplayEvent>,
    val route: List<Point2D>,
    val startTime: String,
    val endTime: String,
    val isEligible: Boolean
)


/**
 * 점수 계산에 들어가는 한 트립의 집계값
 * @param efficiency 이 트립 전비(km/kWh) 또는 연비(km/L). 신호가 없으면 null
 * @param leagueAvgEfficiency 같은 차종(같은 동력 방식) 리그 평균
 * @param streakDays 이 트립 전까지의 연속 안전 주행 일수
 */
data class TripStats(
    val km: Double,
    val hardAccels: Int,
    val hardBrakes: Int,
    val laneChanges: Int,
    val unindicatedLaneChanges: Int,
    val efficiency: Double?,
    val leagueAvgEfficiency: Double,
    val allBeltsFastened: Boolean,
    val unresolvedWarningSeconds: Long,
    val doorOpenWhileMoving: Int,
    val streakDays: Int
)

/** 동력 방식. 전비/연비 기준이 달라 리그를 나눈다. */
enum class Powertrain { EV, ICE }
