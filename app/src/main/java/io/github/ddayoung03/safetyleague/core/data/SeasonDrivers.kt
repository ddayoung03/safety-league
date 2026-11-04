package io.github.ddayoung03.safetyleague.core.data

import io.github.ddayoung03.safetyleague.core.calculator.LeagueCalculator
import io.github.ddayoung03.safetyleague.core.model.DriverRank

/** 시연용 가상 운전자 396명. 브론즈 기준 60~88점, 다른 티어도 검증 전까지 같은 분포. */
object SeasonDrivers {
    private val names = listOf("부드러운출근길", "깜빡이장인", "조용한퇴근", "주말여행러")
    val drivers = List(396) { i ->
        DriverRank(i + 1, "${names[i % names.size]}${i + 1}", LeagueCalculator.round1(88.0 - 28.0 * i / 395))
    }
}
