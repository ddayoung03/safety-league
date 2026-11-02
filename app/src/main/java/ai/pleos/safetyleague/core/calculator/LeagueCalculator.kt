package ai.pleos.safetyleague.core.calculator

import ai.pleos.safetyleague.core.model.DriverRank
import ai.pleos.safetyleague.core.model.Point2D
import ai.pleos.safetyleague.core.model.Trip
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

object LeagueCalculator {

    /**
     * 소수점 첫째 자리 반올림
     */
    fun round1(x: Double): Double {
        return Math.round(x * 10.0) / 10.0
    }

    /**
     * 특정 날짜 범위의 트립 평균 점수 계산 (소수점 1자리 반올림)
     */
    fun average(trips: List<Trip>, fromDate: String, toDate: String): Double {
        val scores = trips
            .filter { it.date in fromDate..toDate }
            .map { it.score }
        if (scores.isEmpty()) return 0.0
        return round1(scores.sum() / scores.size)
    }

    /**
     * 시즌 시작부터 지정 일자까지의 누적 시즌 평균
     */
    fun seasonAverage(trips: List<Trip>, seasonStart: String, untilDate: String): Double {
        return average(trips, seasonStart, untilDate)
    }

    /**
     * 연속 안전 주행 일수 계산
     * - 트립 점수 threshold(기본 70점) 이상인 운전일이 이어진 일수
     * - 운전 안 한 날은 건너뜀 (끊기지도 늘지도 않음)
     * - 그날 트립 하나라도 threshold 미만이면 연속 기록이 0으로 리셋됨
     */
    fun streakAt(trips: List<Trip>, untilDate: String, threshold: Double = 70.0): Int {
        val byDay = mutableMapOf<String, MutableList<Double>>()
        for (t in trips) {
            if (t.date <= untilDate) {
                byDay.getOrPut(t.date) { mutableListOf() }.add(t.score)
            }
        }

        var streak = 0
        // 날짜 역순(최신 날짜부터 과거로 탐색)
        val sortedDates = byDay.keys.sortedDescending()
        for (d in sortedDates) {
            val dayScores = byDay[d] ?: continue
            if (dayScores.any { it < threshold }) {
                break
            }
            streak++
        }
        return streak
    }

    /**
     * 승급 라인 순위 (상위 30%)
     */
    fun promotionRank(totalDrivers: Int): Int {
        return Math.round(totalDrivers * 0.3f)
    }

    /**
     * 강등 라인 순위 (하위 30%)
     */
    fun demotionRank(totalDrivers: Int): Int {
        return totalDrivers - Math.round(totalDrivers * 0.3f) + 1
    }

    /**
     * 시즌 종료일까지 남은 일수 계산
     */
    fun daysLeft(seasonEnd: String, currentDate: String): Int {
        val end = LocalDate.parse(seasonEnd)
        val current = LocalDate.parse(currentDate)
        return ChronoUnit.DAYS.between(current, end).toInt()
    }

    /**
     * 경로 선상에서 전체 이동거리 비율 t(0.0..1.0)에 해당하는 2D 좌표 계산
     */
    fun pointAt(points: List<Point2D>, t: Double): Point2D {
        if (points.isEmpty()) return Point2D(0.0, 0.0)
        if (points.size == 1 || t <= 0.0) return points[0]
        if (t >= 1.0) return points.last()

        val segmentLengths = mutableListOf<Double>()
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            segmentLengths.add(hypot(p2.x - p1.x, p2.y - p1.y))
        }

        val totalLength = segmentLengths.sum()
        var left = t * totalLength

        for (i in segmentLengths.indices) {
            val segLen = segmentLengths[i]
            if (left <= segLen || i == segmentLengths.size - 1) {
                val k = if (segLen > 0.0) min(left / segLen, 1.0) else 0.0
                val p1 = points[i]
                val p2 = points[i + 1]
                return Point2D(
                    x = p1.x + (p2.x - p1.x) * k,
                    y = p1.y + (p2.y - p1.y) * k
                )
            }
            left -= segLen
        }

        return points[0]
    }

    /**
     * 트립 인정 최소 거리 (3km 미만 제외)
     */
    fun isEligibleTrip(km: Double): Boolean {
        return km >= 3.0
    }
}
