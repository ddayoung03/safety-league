package io.github.ddayoung03.safetyleague.ui

import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import io.github.ddayoung03.safetyleague.R
import io.github.ddayoung03.safetyleague.core.calculator.ShopCatalog
import io.github.ddayoung03.safetyleague.core.calculator.ShopItem
import io.github.ddayoung03.safetyleague.core.calculator.Wallet
import io.github.ddayoung03.safetyleague.core.model.Powertrain
import io.github.ddayoung03.safetyleague.databinding.ViewShopBinding
import io.github.ddayoung03.safetyleague.ui.view.Pose

internal enum class ShopTab { SHOP, COUPONS }

/** 시연 리그는 아이오닉 5이므로 시연 상점은 전기차 기준이다. */
internal val MainActivity.shopPowertrain: Powertrain
    get() = if (demoMode) Powertrain.EV else profileStore.load()?.powertrain ?: Powertrain.EV

internal fun MainActivity.showShop() {
    val items = ShopCatalog.itemsFor(shopPowertrain)
    wallet.withoutUnavailableGoal(items).let { if (it != wallet) saveWallet(it) }
    val b = ViewShopBinding.inflate(layoutInflater, binding.screenContainer, true)
    b.rabbit.pose = Pose.WELCOME
    b.counterChecker.background = checkerTile()
    b.goalTrack.background = skew(R.color.ink)
    b.goalFill.background = skew(R.color.yellow)
    b.btnShopBack.background = skew(R.color.ink_glow)
    listOf(b.btnShopBack, b.tvTabShop, b.tvTabCoupons).forEach { it.pressFeedback() }
    b.btnShopBack.setOnClickListener { show(Screen.LEAGUE) }
    b.tvTabShop.setOnClickListener { shopTab = ShopTab.SHOP; paintShop(b, items) }
    b.tvTabCoupons.setOnClickListener { shopTab = ShopTab.COUPONS; paintShop(b, items) }
    paintShop(b, items)
}

internal fun MainActivity.paintShop(b: ViewShopBinding, items: List<ShopItem>) {
    val w = wallet
    val target = w.target(items)
    b.tvTalk.text = ShopCatalog.talk(w.balance, target)
    b.tvBalance.text = ShopCatalog.points(w.balance)
    if (target != null) {
        b.tvGoalCaption.text = "${if (target.id == w.goalId) "내 목표" else "다음 쿠폰"} · ${target.name}"
        b.tvGoalPrice.text = "/ ${ShopCatalog.points(target.price)}P"
        b.goalFill.layoutParams.width = dp(588) * minOf(w.balance, target.price) / target.price
        b.goalFill.requestLayout()
    }
    b.tvTabCoupons.text = "쿠폰함 ${w.coupons.size}"
    for ((tab, view) in listOf(ShopTab.SHOP to b.tvTabShop, ShopTab.COUPONS to b.tvTabCoupons)) {
        view.background = skew(if (tab == shopTab) R.color.yellow else R.color.ink_glow)
        view.setTextColor(color(if (tab == shopTab) R.color.ink else R.color.ink_muted))
    }
    b.layoutItems.removeAllViews()
    if (shopTab == ShopTab.SHOP) items.forEach { b.layoutItems.addView(itemRow(it, w, target, b, items)) }
}

private fun MainActivity.itemRow(item: ShopItem, w: Wallet, target: ShopItem?, b: ViewShopBinding, items: List<ShopItem>): View {
    val mine = item.id == target?.id
    val canBuy = w.balance >= item.price
    val left = "${ShopCatalog.points(item.price - w.balance)}P 더"
    val note = when {
        canBuy -> "지금 교환할 수 있어요"
        mine && item.id == w.goalId -> "내 목표 · $left"
        else -> left
    }
    val row = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(28), dp(14), dp(24), dp(14))
        background = skew(if (mine) R.color.yellow else R.color.ink_glow)
        isClickable = true
        isFocusable = true
        contentDescription = "${item.name} ${item.price}포인트 $note"
        pressFeedback()
        setOnClickListener {
            saveWallet(wallet.withGoal(item.id))
            paintShop(b, items)
        }
    }
    val words = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    words.addView(text(item.name, if (mine) R.color.ink else R.color.ink_text, 24f))
    words.addView(text(note, if (mine) R.color.ink else R.color.ink_muted, 19f))
    row.addView(words, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
    val priceColor = if (mine) R.color.ink else if (canBuy) R.color.yellow else R.color.ink_muted
    row.addView(text("${ShopCatalog.points(item.price)}P", priceColor, 36f, number = true))
    row.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(104)).apply { bottomMargin = dp(12) }
    return row
}
