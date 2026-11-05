package io.github.ddayoung03.safetyleague.core.data

import io.github.ddayoung03.safetyleague.core.calculator.LeagueCalculator
import io.github.ddayoung03.safetyleague.core.model.DetailScore
import io.github.ddayoung03.safetyleague.core.model.DriverRank
import io.github.ddayoung03.safetyleague.core.model.League
import io.github.ddayoung03.safetyleague.core.model.Point2D
import io.github.ddayoung03.safetyleague.core.model.ReplayEvent
import io.github.ddayoung03.safetyleague.core.model.Trip

/**
 * 시연용 가상 운전자 "나"의 데이터.
 * 2026-11-05 정리: 92점 카드·리그·리포트가 모두 기준일 [today](11-09) 하나를 쓴다.
 * 그래서 11-07/11-09 점수를 맞바꿨고 mockups/demo-data.js와는 이 부분이 다르다.
 */
object DemoData {
    /** 시연의 "오늘". 92점 트립을 달린 날이자 리그·리포트 기준일. */
    const val today = "2026-11-09"

    /** 시연용 아이오닉 5 리그 평균 전비(km/kWh). 가상값. */
    const val leagueAvgEfficiency = 5.8
    /** 시연용 내연기관 리그 평균 연비(km/L). 가상값. */
    const val leagueAvgFuelEconomy = 12.0

    /**
     * 아쉬운 날(별도 시나리오, 시즌 후반의 예시): 이 트립 전까지 [lowDayTripsBefore]트립 평균 [lowDayAvgBefore]로 P125였다가
     * 64점을 받아 평균이 88.3이 되고 해운대드라이버에게 한 계단 밀린 날. 트립이 많아야 64점 한 번에 0.5점만 내려간다.
     * (11-09 기준 8트립이면 64점 뒤 86.0이라 P126 ▼1이 될 수 없다.)
     */
    const val lowDayScore = 64
    const val lowDayStreakBefore = 13
    const val lowDayRankBefore = 125
    const val lowDayTripsBefore = 49
    const val lowDayAvgBefore = 88.8
    val lowDayAvgAfter = LeagueCalculator.round1((lowDayAvgBefore * lowDayTripsBefore + lowDayScore) / (lowDayTripsBefore + 1))

    /** 아쉬운 날 순위표: 나를 제친 해운대드라이버가 목표 */
    val lowBoard = listOf(
        DriverRank(124, "깜빡이장인", 89.1),
        DriverRank(125, "해운대드라이버", 88.7),
        DriverRank(126, "나", lowDayAvgAfter),
        DriverRank(127, "조용한퇴근", 88.2)
    )

    /**
     * 아쉬운 날 64점 내역. 리플레이 사건(22.1km, 급가속 1, 급제동 2, 깜빡이 없는 차선 변경 4/4)과
     * 전비 6.264(리그 평균 5.8의 108%), 벨트·경고 정상, 연속 13일로 ScoreCalculator가 내는 값이다.
     */
    val lowDayDetail = DetailScore(smoothness = 25.0, manner = 0.0, eco = 14.0, safety = 15.0, consistency = 10.0)

    /** 리그 화면 안전운전 효용 (시연용 가상값) */
    const val savedWon = "₩8,400"
    const val brakesCut = "3,120회"
    const val brakesCaption = "이번 주, 12% 감소"


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
        Trip(date = "2026-11-07", score = 93.0),
        Trip(date = "2026-11-08", score = 86.0),
        Trip(date = "2026-11-09", score = 92.0, km = 18.4)
    )

    val ranks = mapOf(
        "2026-11-03" to 131,
        "2026-11-04" to 130,
        "2026-11-05" to 130,
        "2026-11-06" to 128,
        "2026-11-07" to 126,
        "2026-11-08" to 128,
        "2026-11-09" to 125
    )

    /**
     * [today] 순위표의 이웃 (가상 별명). '나'의 avg는 seasonAverage(today)로 채운다.
     * P126~P128은 11-08(평균 88.3, P128)에는 내 앞이었고 92점 트립으로 추월한 세 명이다.
     * 실제 모드의 가상 운전자(SeasonDrivers.drivers)도 이 줄들을 그대로 지난다.
     */
    val board = listOf(
        DriverRank(1, "광안대교출근", 96.4),
        DriverRank(2, "부드러운출근길", 95.8),
        DriverRank(118, "금정구모범생", 89.8),
        DriverRank(119, "동래젠틀맨", 89.7),
        DriverRank(123, "수영강변러", 89.2),
        DriverRank(124, "깜빡이장인", 89.1),
        DriverRank(125, "나", 0.0),
        DriverRank(126, "해운대드라이버", 88.7),
        DriverRank(127, "주말여행러", 88.6),
        DriverRank(128, "퇴근길여유", 88.5)
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

    // 이긴 날(92점, [today]) 리플레이 데이터: 감점 사건 없이 달린 날
    val winDayStart = "8:02"
    val winDayEnd = "8:31"
    val winDayKm get() = trips.first { it.date == today }.km
    val winDayEvents = emptyList<ReplayEvent>()

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

    /** [today] 순위표 (내 평균을 채운 것) */
    fun neighbors(): List<DriverRank> {
        val myAvg = seasonAverage(today)
        return board.map { if (it.name == "나") it.copy(avg = myAvg) else it }
    }
}
