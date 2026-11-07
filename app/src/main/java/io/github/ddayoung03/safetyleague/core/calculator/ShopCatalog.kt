package io.github.ddayoung03.safetyleague.core.calculator

import io.github.ddayoung03.safetyleague.core.model.Powertrain
import java.util.Locale

/** [powertrain]이 null이면 모든 차가 대상이다. */
data class ShopItem(val id: String, val name: String, val price: Int, val powertrain: Powertrain?)

/** 시연용 상품. 1P = 10원 가치로 둔 가정이고, 실제 쿠폰은 발급되지 않는다. */
object ShopCatalog {
    val items = listOf(
        ShopItem("charge_10k", "충전 크레딧 1만 원", 1_000, Powertrain.EV),
        ShopItem("service_10k", "정비 쿠폰 1만 원", 1_000, null),
        ShopItem("service_50k", "정비 쿠폰 5만 원", 5_000, null),
        ShopItem("engine_oil", "엔진오일 교환권", 10_000, Powertrain.ICE)
    )

    fun itemsFor(powertrain: Powertrain) = items.filter { it.powertrain == null || it.powertrain == powertrain }

    fun item(id: String) = items.firstOrNull { it.id == id }

    fun talk(balance: Int, target: ShopItem?): String = when {
        target == null -> "어서 와요!"
        balance >= target.price -> "${target.name}, 지금 교환할 수 있어요!"
        else -> "${target.name}까지 ${points(target.price - balance)}P 남았어요."
    }

    fun points(value: Int): String = String.format(Locale.US, "%,d", value)
}
