package io.github.ddayoung03.safetyleague.ui

import android.view.View
import io.github.ddayoung03.safetyleague.R
import io.github.ddayoung03.safetyleague.core.calculator.PointCalculator
import io.github.ddayoung03.safetyleague.core.calculator.SettlementCard
import io.github.ddayoung03.safetyleague.core.calculator.ShopCatalog
import io.github.ddayoung03.safetyleague.core.data.DemoData
import io.github.ddayoung03.safetyleague.databinding.ViewSettlementBinding
import io.github.ddayoung03.safetyleague.ui.motion.Fx
import io.github.ddayoung03.safetyleague.ui.motion.MotionTokens
import io.github.ddayoung03.safetyleague.ui.motion.animationsEnabled
import io.github.ddayoung03.safetyleague.ui.motion.countUp
import io.github.ddayoung03.safetyleague.ui.view.Pose

/** 리그로 가는 길은 모두 show()를 지나므로, 아직 안 본 실제 정산이 있으면 여기서 정산 화면으로 돌린다. */
internal fun redirectToSettlement(target: Screen, hasCard: Boolean, demoMode: Boolean) =
    target == Screen.LEAGUE && hasCard && !demoMode

internal fun MainActivity.showSettlement() {
    val c = if (demoMode) DemoData.settlementCard else requireNotNull(seasonStore.card())
    val b = ViewSettlementBinding.inflate(layoutInflater, binding.screenContainer, true)
    b.rabbit.pose = Pose.WIN
    b.checkerStrip.background = checkerTile()
    b.btnSettleShop.background = skew(R.color.yellow)
    b.btnSettleLeague.background = skew(R.color.ink_glow)
    listOf(b.btnSettleShop, b.btnSettleLeague).forEach { it.pressFeedback() }
    b.tvSettleTitle.text = if (c.seasons > 1) "${c.seasonLabel} 정산" else "${c.seasonLabel} 정산 · ${c.tier} 리그"
    b.tvPoints.text = pointsLabel(c.points)
    val tag = tierTag(c)
    b.tvTierTag.text = tag
    b.tvTierTag.visibility = if (tag == null) View.GONE else View.VISIBLE
    b.tvDetail.text = detailText(c)
    b.btnSettleShop.setOnClickListener {
        if (!demoMode) seasonStore.clearCard()
        shopTab = ShopTab.SHOP
        show(Screen.SHOP)
    }
    b.btnSettleLeague.setOnClickListener {
        if (!demoMode) seasonStore.clearCard()
        show(Screen.LEAGUE)
    }
    if (animationsEnabled()) b.tvPoints.countUp(c.points, MotionTokens.COUNT, motion, ::pointsLabel)
    if (requestSound()) sound.play(Fx.WIN)
}

private fun pointsLabel(points: Int) = "+${ShopCatalog.points(points)}"

private fun rate(c: SettlementCard) = "${c.tier} 배율 ${c.multiplierPercent / 100.0}배"

private fun tierTag(c: SettlementCard) = when {
    c.promotedTo != null -> "${c.promotedTo} 리그로 승급!"
    c.seasons > 1 -> null
    else -> "${rate(c)} 적용"
}

private fun detailText(c: SettlementCard): String {
    val lines = mutableListOf<String>()
    if (c.seasons == 1) {
        val bonus = if (c.bonusPercent > 0) "평균 ${c.average}점 보너스 +${c.bonusPercent}%" else "다음 달엔 보너스에 도전해 봐요"
        val distance = if (c.km >= PointCalculator.MAX_KM) "1,000km까지 인정됐어요" else "${c.km}km 안전하게 달렸어요"
        lines += "$distance · $bonus" + if (c.promotedTo != null) " · ${rate(c)}" else ""
    }
    if (c.promotedTo == null) lines += c.message
    lines += "내 포인트 ${ShopCatalog.points(c.balance)}P"
    return lines.joinToString("\n")
}
