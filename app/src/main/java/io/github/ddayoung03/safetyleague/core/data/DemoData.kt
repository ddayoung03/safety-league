package io.github.ddayoung03.safetyleague.core.data

import io.github.ddayoung03.safetyleague.core.calculator.LeagueCalculator
import io.github.ddayoung03.safetyleague.core.model.DriverRank
import io.github.ddayoung03.safetyleague.core.model.League
import io.github.ddayoung03.safetyleague.core.model.Point2D
import io.github.ddayoung03.safetyleague.core.model.ReplayEvent
import io.github.ddayoung03.safetyleague.core.model.Trip

/**
 * 시연용 가상 운전자 "나"의 데이터 (mockups/demo-data.js와 100% 동일)
 */
object DemoData {
    /** 시연용 아이오닉 5 리그 평균 전비(km/kWh). 가상값. */
    const val leagueAvgEfficiency = 5.8


    val league = League(
        total = 397,
        tier = "골드",
        model = "아이오닉 5",
        region = "부산",
        seasonStart = "2026-11-01",
        seasonEnd = "2026-11-30"
    )

    val trips = listOf(
        Trip(date = "2026-10-24", score = 66.0),
        Trip(date = "2026-10-25", score = 80.0),
        Trip(date = "2026-10-26", score = 81.0),
        Trip(date = "2026-10-27", score = 82.0),
        Trip(date = "2026-10-28", score = 85.0),
        Trip(date = "2026-10-29", score = 84.0),
        Trip(date = "2026-10-30", score = 86.0),
        Trip(date = "2026-10-31", score = 86.0),
        Trip(date = "2026-11-01", score = 88.0),
        Trip(date = "2026-11-02", score = 89.0),
        Trip(date = "2026-11-03", score = 84.0),
        Trip(date = "2026-11-04", score = 88.0),
        Trip(date = "2026-11-06", score = 90.0),
        Trip(date = "2026-11-07", score = 92.0, km = 18.4),
        Trip(date = "2026-11-08", score = 86.0),
        Trip(date = "2026-11-09", score = 93.0)
    )

    val ranks = mapOf(
        "2026-11-03" to 131,
        "2026-11-04" to 130,
        "2026-11-05" to 130,
        "2026-11-06" to 128,
        "2026-11-07" to 125,
        "2026-11-08" to 126,
        "2026-11-09" to 125
    )

    // 순위표 이웃 데이터 (가상 별명)
    // '나'의 avg는 동적으로 seasonAverage로 채워짐
    val rawBoard = mapOf(
        "2026-11-07" to listOf(
            Triple(123, "부드러운출근길", 89.1),
            Triple(124, "깜빡이장인", 88.8),
            Triple(125, "나", 0.0),
            Triple(126, "해운대드라이버", 88.4),
            Triple(127, "조용한퇴근", 88.3),
            Triple(128, "주말여행러", 88.2)
        ),
        "2026-11-09" to listOf(
            Triple(1, "광안대교출근", 96.4),
            Triple(2, "부드러운출근길", 95.8),
            Triple(118, "금정구모범생", 89.8),
            Triple(119, "동래젠틀맨", 89.7),
            Triple(124, "깜빡이장인", 89.1),
            Triple(125, "나", 0.0),
            Triple(126, "해운대드라이버", 88.7)
        )
    )

    // 가상의 출퇴근 경로 (100x60 상자 안 좌표)
    val route = listOf(
        Point2D(6.0, 52.0),
        Point2D(14.0, 50.0),
        Point2D(22.0, 44.0),
        Point2D(26.0, 34.0),
        Point2D(34.0, 28.0),
        Point2D(46.0, 30.0),
        Point2D(54.0, 24.0),
        Point2D(58.0, 14.0),
        Point2D(68.0, 10.0),
        Point2D(80.0, 12.0),
        Point2D(88.0, 20.0),
        Point2D(92.0, 30.0),
        Point2D(86.0, 38.0),
        Point2D(90.0, 48.0)
    )

    // 아쉬운 날(64점 시나리오) 리플레이 데이터
    val lowDayStart = "6:08"
    val lowDayEnd = "6:42"
    val lowDayKm = 22.1
    val lowDayEvents = listOf(
        ReplayEvent(at = "6:12", kind = "accel", t = 0.10),
        ReplayEvent(at = "6:17", kind = "lane", t = 0.21),
        ReplayEvent(at = "6:21", kind = "brake", t = 0.34),
        ReplayEvent(at = "6:26", kind = "lane", t = 0.47),
        ReplayEvent(at = "6:31", kind = "lane", t = 0.61),
        ReplayEvent(at = "6:34", kind = "brake", t = 0.70),
        ReplayEvent(at = "6:38", kind = "lane", t = 0.86)
    )

    fun seasonAverage(untilDate: String): Double {
        return LeagueCalculator.seasonAverage(trips, league.seasonStart, untilDate)
    }

    fun average(fromDate: String, toDate: String): Double {
        return LeagueCalculator.average(trips, fromDate, toDate)
    }

    fun streakAt(untilDate: String, threshold: Double = 70.0): Int {
        return LeagueCalculator.streakAt(trips, untilDate, threshold)
    }

    fun daysLeft(currentDate: String): Int {
        return LeagueCalculator.daysLeft(league.seasonEnd, currentDate)
    }

    fun neighbors(date: String): List<DriverRank> {
        val raw = rawBoard[date] ?: emptyList()
        val myAvg = seasonAverage(date)
        return raw.map { (rank, name, avg) ->
            DriverRank(
                rank = rank,
                name = name,
                avg = if (name == "나") myAvg else avg
            )
        }
    }
}
