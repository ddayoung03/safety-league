package ai.pleos.safetyleague.core.model

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
)

/**
 * 2차원 좌표 (경로 polyline 점)
 */
data class Point2D(
    val x: Double,
    val y: Double
)
