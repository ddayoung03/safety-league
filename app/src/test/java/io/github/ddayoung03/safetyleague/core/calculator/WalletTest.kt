package io.github.ddayoung03.safetyleague.core.calculator

import io.github.ddayoung03.safetyleague.core.model.Powertrain
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class WalletTest {
    private val day = LocalDate.of(2026, 11, 7)
    private val small = ShopCatalog.item("service_10k")!!

    @Test fun exchangeTakesPointsAndClearsGoal() {
        val w = Wallet(1500, "service_10k").exchange(small, day)!!
        assertEquals(500, w.balance)
        assertNull(w.goalId)
        assertEquals("service_10k", w.coupons.single().itemId)
    }

    @Test fun exchangeKeepsOtherGoal() {
        assertEquals("service_50k", Wallet(1500, "service_50k").exchange(small, day)!!.goalId)
    }

    @Test fun exchangeTwiceNeedsEnoughBalance() {
        val once = Wallet(1000).exchange(small, day)!!
        assertNull(once.exchange(small, day))
        val twice = Wallet(2000).exchange(small, day)!!.exchange(small, day)!!
        assertEquals(2, twice.coupons.map { it.id }.distinct().size)
    }

    @Test fun couponExpiresAfterNinetyDays() {
        val c = Coupon("a", "service_10k", day)
        assertEquals(day.plusDays(90), c.expiresOn)
        assertFalse(c.isExpired(day.plusDays(90)))
        assertTrue(c.isExpired(day.plusDays(91)))
    }

    @Test fun goalLeavesWhenItemUnavailable() {
        val w = Wallet(0, "charge_10k", listOf(Coupon("a", "charge_10k", day)))
            .withoutUnavailableGoal(ShopCatalog.itemsFor(Powertrain.ICE))
        assertNull(w.goalId)
        assertEquals(1, w.coupons.size)
    }

    @Test fun targetIsGoalThenCheapestUnclaimedThenCheapest() {
        val items = ShopCatalog.itemsFor(Powertrain.ICE)
        assertEquals("service_50k", Wallet(0, "service_50k").target(items)!!.id)
        assertEquals("service_10k", Wallet().target(items)!!.id)
        assertEquals("service_50k", Wallet(0, null, listOf(Coupon("a", "service_10k", day))).target(items)!!.id)
        val all = items.mapIndexed { i, it -> Coupon("$i", it.id, day) }
        assertEquals("service_10k", Wallet(0, null, all).target(items)!!.id)
    }

    @Test fun plusAddsPoints() {
        assertEquals(1248, Wallet().plus(1248).balance)
    }

    @Test fun encodeRoundTrip() {
        val w = Wallet(3420, "service_50k", listOf(Coupon("service_10k-1", "service_10k", day)))
        assertEquals(w, Wallet.decode(w.encode()))
        assertEquals(Wallet(), Wallet.decode(Wallet().encode()))
    }

    @Test fun decodeIgnoresIndentAddedByPrefsFile() {
        assertEquals(Wallet(18), Wallet.decode("18\n    "))
    }

    @Test fun decodeBrokenTextGivesEmptyWallet() {
        assertEquals(Wallet(), Wallet.decode(null))
        assertEquals(Wallet(), Wallet.decode("망가진 값"))
    }
}
