package io.github.ddayoung03.safetyleague.core.calculator

import java.time.LocalDate

data class Coupon(val id: String, val itemId: String, val issuedOn: LocalDate) {
    val expiresOn: LocalDate get() = issuedOn.plusDays(90)
    fun isExpired(today: LocalDate) = today.isAfter(expiresOn)
}

/** 내 포인트, 목표 쿠폰, 받은 쿠폰. 저장은 org.json 없이 단위 테스트할 수 있게 줄 단위 문자열로 한다. */
data class Wallet(val balance: Int = 0, val goalId: String? = null, val coupons: List<Coupon> = emptyList()) {
    fun plus(points: Int) = copy(balance = balance + points)

    fun withGoal(id: String?) = copy(goalId = id)

    /** 잔액이 모자라면 null. 목표였던 쿠폰을 받으면 목표를 푼다. */
    fun exchange(item: ShopItem, today: LocalDate): Wallet? {
        if (balance < item.price) return null
        val coupon = Coupon("${item.id}-$today-${coupons.size + 1}", item.id, today)
        return copy(balance = balance - item.price, goalId = goalId.takeIf { it != item.id }, coupons = coupons + coupon)
    }

    fun withoutUnavailableGoal(items: List<ShopItem>) = if (items.none { it.id == goalId }) copy(goalId = null) else this

    /** 게이지와 말풍선이 보여 줄 쿠폰. 목표가 없으면 아직 안 받은 가장 싼 쿠폰. */
    fun target(items: List<ShopItem>): ShopItem? = items.firstOrNull { it.id == goalId }
        ?: items.filter { item -> coupons.none { it.itemId == item.id } }.minByOrNull { it.price }
        ?: items.minByOrNull { it.price }

    fun encode(): String = (listOf(balance.toString(), goalId.orEmpty()) +
        coupons.map { "${it.id}|${it.itemId}|${it.issuedOn}" }).joinToString("\n")

    companion object {
        fun decode(text: String?): Wallet = runCatching {
            val lines = text!!.split("\n")
            // 쿠폰 한 줄이 깨져도 잔액은 살린다. 다음 저장이 빈 지갑으로 덮어쓰면 포인트가 영영 사라진다.
            Wallet(lines[0].toInt(), lines[1].trim().ifEmpty { null }, lines.drop(2).mapNotNull { line ->
                runCatching {
                    val (id, itemId, issuedOn) = line.split("|")
                    Coupon(id, itemId, LocalDate.parse(issuedOn))
                }.getOrNull()
            })
        }.getOrDefault(Wallet())
    }
}
