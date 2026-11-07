package io.github.ddayoung03.safetyleague.core.calculator

import io.github.ddayoung03.safetyleague.core.model.Powertrain
import org.junit.Assert.*
import org.junit.Test

class ShopCatalogTest {
    @Test fun catalogFollowsPowertrain() {
        assertEquals(listOf("charge_10k", "service_10k", "service_50k"), ShopCatalog.itemsFor(Powertrain.EV).map { it.id })
        assertEquals(listOf("service_10k", "service_50k", "engine_oil"), ShopCatalog.itemsFor(Powertrain.ICE).map { it.id })
    }

    @Test fun pricesMatchDesign() {
        assertEquals(listOf(1000, 1000, 5000, 10000), ShopCatalog.items.map { it.price })
    }

    @Test fun talkShowsWhatIsLeft() {
        val big = ShopCatalog.item("service_50k")!!
        assertEquals("정비 쿠폰 5만 원까지 1,580P 남았어요.", ShopCatalog.talk(3420, big))
        assertEquals("정비 쿠폰 5만 원, 지금 교환할 수 있어요!", ShopCatalog.talk(5000, big))
        assertEquals("어서 와요!", ShopCatalog.talk(0, null))
    }
}
