package io.github.ddayoung03.safetyleague.core.data

import io.github.ddayoung03.safetyleague.core.calculator.LeagueCalculator
import io.github.ddayoung03.safetyleague.core.model.DriverRank

/** 시연용 가상 운전자 396명. 브론즈 기준 60~88점, 다른 티어도 검증 전까지 같은 분포. */
object SeasonDrivers {
    private val names = listOf("부드러운출근길", "깜빡이장인", "조용한퇴근", "주말여행러")
    val drivers = List(396) { i ->
        DriverRank(i + 1, "${names[i % names.size]}${i + 1}", LeagueCalculator.round1(88.0 - 28.0 * i / 395))
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
