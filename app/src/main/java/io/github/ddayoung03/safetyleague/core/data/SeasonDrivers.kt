package io.github.ddayoung03.safetyleague.core.data

import io.github.ddayoung03.safetyleague.core.calculator.LeagueCalculator
import io.github.ddayoung03.safetyleague.core.model.DriverRank

/**
 * 차종 리그 가상 운전자 396명(나를 넣으면 397명). 실제 모드와 시연 모드가 같은 분포를 쓴다:
 * 시연 순위표([DemoData.board], 11-09 기준 P1 96.4 · P119 89.7 · 내 앞뒤 P124 89.1 / P126 88.7)를 그대로 지나고,
 * 그 사이는 직선으로 채운다. 맨 아래(P397)는 62점. 모든 티어가 검증 전까지 같은 분포.
 */
object SeasonDrivers {
    private val names = listOf("부드러운출근길", "깜빡이장인", "조용한퇴근", "주말여행러")
    private const val BOTTOM_AVG = 62.0
    val drivers: List<DriverRank> = run {
        val myRank = DemoData.ranks.getValue(DemoData.today)
        // 가상 운전자 목록 안의 위치(0부터) → 시연 순위표의 그 사람. 내 아래는 한 칸씩 당겨진다.
        val anchors = DemoData.board.filter { it.name != "나" }
            .associateBy { if (it.rank < myRank) it.rank - 1 else it.rank - 2 }
            .toMutableMap()
        anchors[395] = anchors[395] ?: DriverRank(0, "${names[395 % names.size]}396", BOTTOM_AVG)
        val keys = anchors.keys.sorted()
        List(396) { i ->
            anchors[i]?.let { return@List it.copy(rank = i + 1) }
            val hi = keys.last { it < i }
            val lo = keys.first { it > i }
            val a = anchors.getValue(hi).avg
            val b = anchors.getValue(lo).avg
            DriverRank(i + 1, "${names[i % names.size]}${i + 1}", LeagueCalculator.round1(a + (b - a) * (i - hi) / (lo - hi)))
        }
    }

    /** 지역 리그 가상 운전자 148명. 차종과 관계없이 같은 지역, 62~95점. */
    private val regionNames = listOf("광안리산책", "서면출근", "해운대드라이버", "동래젠틀맨", "금정구모범생")
    val region = List(148) { i ->
        DriverRank(i + 1, "${regionNames[i % regionNames.size]}${i + 1}", LeagueCalculator.round1(95.0 - 33.0 * i / 147))
    }

    /** 친구 대결 가상 친구 5명. */
    val friends = listOf(
        DriverRank(1, "주말캠핑", 91.2),
        DriverRank(2, "새벽배송", 89.4),
        DriverRank(3, "동네한바퀴", 87.0),
        DriverRank(4, "카풀메이트", 84.5),
        DriverRank(5, "세차는주말에", 79.8)
    )
}
