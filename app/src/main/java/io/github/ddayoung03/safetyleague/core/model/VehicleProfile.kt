package io.github.ddayoung03.safetyleague.core.model

import io.github.ddayoung03.safetyleague.core.data.DemoData
import java.util.Locale

data class VehicleProfile(val model: String, val powertrain: Powertrain, val tier: String = "브론즈", val recognition: String = "manual") {
    val leagueKey get() = "$model:${powertrain.name}"
    /** 같은 차종 리그의 평균 전비/연비. 표에 없는 차종은 동력 방식별 기본값. */
    val averageEfficiency get() = VehicleCatalog.averageEfficiency[leagueKey]
        ?: if (powertrain == Powertrain.EV) DemoData.leagueAvgEfficiency else DemoData.leagueAvgFuelEconomy
}

data class SupportedVehicle(val make: String, val model: String, val englishModel: String, val powertrain: Powertrain)

object VehicleCatalog {
    val vehicles = listOf(
        SupportedVehicle("현대", "아이오닉 5", "IONIQ 5", Powertrain.EV),
        SupportedVehicle("현대", "아이오닉 6", "IONIQ 6", Powertrain.EV),
        SupportedVehicle("현대", "아반떼", "AVANTE", Powertrain.ICE),
        SupportedVehicle("현대", "쏘나타", "SONATA", Powertrain.ICE),
        SupportedVehicle("현대", "투싼", "TUCSON", Powertrain.ICE),
        SupportedVehicle("기아", "EV3", "EV3", Powertrain.EV),
        SupportedVehicle("기아", "EV6", "EV6", Powertrain.EV),
        SupportedVehicle("기아", "스포티지", "SPORTAGE", Powertrain.ICE),
        SupportedVehicle("제네시스", "GV60", "GV60", Powertrain.EV),
        SupportedVehicle("제네시스", "G80", "G80", Powertrain.ICE)
    )

    /**
     * 차종별 평균 전비(km/kWh, EV)·연비(km/L, 내연). 실제 리그 평균 데이터가 없어 둔 시연용 가상 기준이다.
     * 키는 VehicleProfile.leagueKey. 아이오닉 5는 시연 리그 기준(5.8)과 같다.
     */
    val averageEfficiency = mapOf(
        "아이오닉 5:EV" to 5.8,
        "아이오닉 6:EV" to 6.4,
        "EV3:EV" to 6.0,
        "EV6:EV" to 5.6,
        "GV60:EV" to 5.2,
        "아반떼:ICE" to 14.5,
        "쏘나타:ICE" to 12.5,
        "투싼:ICE" to 11.5,
        "스포티지:ICE" to 11.8,
        "G80:ICE" to 9.5
    )

    private fun normalize(value: String?) = value.orEmpty().filterNot { it.isWhitespace() }.lowercase(Locale.ROOT)

    fun recognize(make: String?, model: String?, fuels: List<Int>?): VehicleProfile? {
        val brand = when (normalize(make)) {
            "hyundai", "현대" -> "현대"
            "kia", "기아" -> "기아"
            "genesis", "제네시스" -> "제네시스"
            else -> return null
        }
        val match = vehicles.firstOrNull { it.make == brand && normalize(model) in listOf(normalize(it.model), normalize(it.englishModel)) } ?: return null
        // Android FuelType.ELECTRIC = 10. 순수 함수가 Android 런타임에 의존하지 않게 값으로 판정한다.
        val powertrain = if (fuels.isNullOrEmpty()) match.powertrain else if (10 in fuels) Powertrain.EV else Powertrain.ICE
        return VehicleProfile(match.model, powertrain, recognition = "auto")
    }

    fun manual(name: String, powertrain: Powertrain): VehicleProfile? = name.trim()
        .takeIf { it.isNotEmpty() && it.length <= 20 }?.let { VehicleProfile(it, powertrain) }
}
