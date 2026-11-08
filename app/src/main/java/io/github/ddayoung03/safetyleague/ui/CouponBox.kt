package io.github.ddayoung03.safetyleague.ui

import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import io.github.ddayoung03.safetyleague.R
import io.github.ddayoung03.safetyleague.core.calculator.Coupon
import io.github.ddayoung03.safetyleague.core.calculator.ShopCatalog
import io.github.ddayoung03.safetyleague.core.calculator.ShopItem
import io.github.ddayoung03.safetyleague.core.data.DemoData
import io.github.ddayoung03.safetyleague.databinding.ViewShopBinding
import io.github.ddayoung03.safetyleague.ui.motion.Fx
import io.github.ddayoung03.safetyleague.ui.view.DemoBarcodeView
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dayFormat = DateTimeFormatter.ofPattern("yyyy.MM.dd")

private val MainActivity.shopToday: LocalDate
    get() = if (demoMode) LocalDate.parse(DemoData.today) else LocalDate.now()

/** 교환은 앱 안의 패널에서 한 번 더 확인한다. 저장에 실패하면 잔액을 바꾸지 않는다. */
internal fun MainActivity.openConfirm(b: ViewShopBinding, item: ShopItem, items: List<ShopItem>) {
    b.tvConfirmName.text = item.name
    b.tvConfirmPrice.text = "${ShopCatalog.points(item.price)}P"
    b.tvConfirmAfter.text = "교환 뒤 내 포인트 ${ShopCatalog.points(wallet.balance - item.price)}P"
    b.btnConfirm.background = skew(R.color.yellow)
    b.btnConfirmCancel.background = skew(R.color.ink_glow)
    b.btnConfirm.isEnabled = true
    b.confirmPanel.visibility = View.VISIBLE
    b.btnConfirmCancel.setOnClickListener { b.confirmPanel.visibility = View.GONE }
    b.btnConfirm.setOnClickListener {
        it.isEnabled = false
        b.confirmPanel.visibility = View.GONE
        val next = wallet.exchange(item, shopToday)
        if (next == null || !saveWallet(next)) {
            b.tvTalk.text = "지금은 교환할 수 없어요"
            return@setOnClickListener
        }
        shopTab = ShopTab.COUPONS
        paintShop(b, items)
        b.tvTalk.text = "축하해요! 쿠폰함에 넣었어요"
        if (requestSound()) sound.play(Fx.WIN)
    }
}

internal fun MainActivity.addCoupons(b: ViewShopBinding, coupons: List<Coupon>) {
    if (coupons.isEmpty()) {
        b.layoutItems.addView(text("아직 받은 쿠폰이 없어요. 상점에서 첫 쿠폰을 골라 봐요", R.color.ink_muted, 24f).apply { setPadding(dp(28), dp(24), 0, 0) })
        return
    }
    coupons.asReversed().forEach { b.layoutItems.addView(couponRow(it)) }
}

private fun MainActivity.couponRow(c: Coupon): View {
    val expired = c.isExpired(shopToday)
    val row = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(28), dp(14), dp(28), dp(14))
        background = skew(R.color.ink_glow)
        alpha = if (expired) .55f else 1f
    }
    row.addView(ImageView(this).apply { setImageResource(shopIcon(c.itemId)) }, LinearLayout.LayoutParams(dp(80), dp(80)).apply { marginEnd = dp(16) })
    val words = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    words.addView(text(ShopCatalog.item(c.itemId)?.name ?: "쿠폰", R.color.ink_text, 24f))
    words.addView(text(if (expired) "기간 만료" else "${c.expiresOn.format(dayFormat)}까지", R.color.ink_muted, 19f))
    row.addView(words, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
    row.addView(DemoBarcodeView(this).apply { couponId = c.id }, LinearLayout.LayoutParams(dp(200), dp(72)))
    row.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(112)).apply { bottomMargin = dp(12) }
    return row
}
