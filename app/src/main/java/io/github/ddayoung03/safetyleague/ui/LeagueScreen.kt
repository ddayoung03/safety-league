package io.github.ddayoung03.safetyleague.ui

import android.view.View
import android.widget.TextView
import androidx.core.view.children
import io.github.ddayoung03.safetyleague.R
import io.github.ddayoung03.safetyleague.core.calculator.LeagueCalculator
import io.github.ddayoung03.safetyleague.core.calculator.SeasonRanking
import io.github.ddayoung03.safetyleague.core.calculator.ShopCatalog
import io.github.ddayoung03.safetyleague.core.data.DemoData
import io.github.ddayoung03.safetyleague.core.data.SeasonDrivers
import io.github.ddayoung03.safetyleague.databinding.ViewLeagueBinding
import io.github.ddayoung03.safetyleague.ui.motion.MotionTokens
import io.github.ddayoung03.safetyleague.ui.motion.ScreenTransition
import io.github.ddayoung03.safetyleague.ui.motion.animationsEnabled
import io.github.ddayoung03.safetyleague.ui.motion.motionSequence
import io.github.ddayoung03.safetyleague.ui.motion.stagger
import java.time.LocalDate
import io.github.ddayoung03.safetyleague.ui.view.Pose

internal fun MainActivity.showLeague() {
    val b = ViewLeagueBinding.inflate(layoutInflater, binding.screenContainer, true)
    b.rabbit.pose = Pose.POINT
    val real = !demoMode && demoResultWin == null
    b.checkerStrip.background = checkerTile()
    b.tvToLine.background = skew(R.color.yellow)
    b.tvTabRegion.text = myRegion
    listOf(b.btnLeagueReport, b.btnLeagueBack, b.tvTabModel, b.tvTabRegion, b.tvTabFriends).forEach { it.pressFeedback() }
    b.btnLeagueReport.setOnClickListener { show(Screen.REPORT) }
    b.btnShop.text = "내 포인트 ${ShopCatalog.points(wallet.balance)}P · 상점"
    b.btnShop.background = skew(R.color.ink_glow)
    b.btnShop.pressFeedback()
    b.btnShop.setOnClickListener { shopTab = ShopTab.SHOP; show(Screen.SHOP) }
    b.btnGuide.background = skew(R.color.ink_glow)
    b.btnGuide.pressFeedback()
    b.btnGuide.setOnClickListener { show(Screen.GUIDE) }
    if (real) {
        val now = LocalDate.now()
        repository.latestCompletedTrip.value?.let { latest ->
            b.btnLeagueBack.visibility = View.VISIBLE
            b.btnLeagueBack.setOnClickListener { showResult(latest) }
        }
        b.tvSeasonTitle.text = "${now.monthValue}월 시즌 · $myTier 리그"
        b.tvDaysLeft.text = "${now.lengthOfMonth() - now.dayOfMonth}일 남음"
        b.tvTabModel.text = myModel
        b.statsBlock.visibility = View.GONE
    } else {
        b.tvSeasonTitle.text = "11월 시즌 · ${DemoData.league.tier} 리그"
        b.tvDaysLeft.text = "${DemoData.daysLeft(DemoData.today)}일 남음"
        b.tvTabModel.text = DemoData.league.model
        b.tvSaved.text = DemoData.savedWon
        b.tvBrakesCut.text = DemoData.brakesCut
        b.tvBrakesCaption.text = DemoData.brakesCaption
    }
    val tier = if (real) myTier else DemoData.league.tier
    b.ivTierBelt.setImageResource(tierBelt(tier))
    for (cell in b.tierBar.children.map { it as TextView }) {
        cell.background = skew(if (cell.text == tier) R.color.yellow else R.color.ink_glow)
        cell.setTextColor(color(if (cell.text == tier) R.color.ink else R.color.ink_muted))
    }

    var swapping = false
    // 줄은 언제나 통째로 다시 만든다. 나가는 동안 다른 탭을 누르면 탭만 바꿔 두고, 끝날 때 마지막 탭을 그린다.
    fun pick(tab: LeagueTab) {
        if (tab == leagueTab) return
        leagueTab = tab
        paintTabs(b)
        if (swapping) return
        motion.finishAll()
        if (!animationsEnabled()) { bindBoard(b, real); return }
        swapping = true
        val seq = b.root.motionSequence(motion) {
            swapping = false
            bindBoard(b, real)
            ScreenTransition.stagger(b.root, b.layoutLeagueRows.children.toList(), motion)
        }
        for (row in b.layoutLeagueRows.children) {
            row.animate().alpha(0f).translationX(-dp(24).toFloat()).setDuration(MotionTokens.FAST).start()
        }
        seq.endAt(MotionTokens.FAST)
    }
    b.tvTabModel.setOnClickListener { if (leagueTab == LeagueTab.MODEL) show(Screen.VEHICLE) else pick(LeagueTab.MODEL) }
    b.tvTabRegion.setOnClickListener { pick(LeagueTab.REGION) }
    b.tvTabFriends.setOnClickListener { pick(LeagueTab.FRIENDS) }

    paintTabs(b)
    bindBoard(b, real)
    ScreenTransition.stagger(b.root, b.layoutLeagueRows.children.toList(), motion, MotionTokens.BASE)
}

private fun MainActivity.paintTabs(b: ViewLeagueBinding) {
    for ((tab, kind) in listOf(b.tvTabModel to LeagueTab.MODEL, b.tvTabRegion to LeagueTab.REGION, b.tvTabFriends to LeagueTab.FRIENDS)) {
        tab.background = skew(if (kind == leagueTab) R.color.yellow else R.color.ink_glow)
        tab.setTextColor(color(if (kind == leagueTab) R.color.ink else R.color.ink_muted))
    }
}

/** 지금 탭의 순위 칸과 줄을 채운다. 탭을 바꿀 때 같은 화면에서 다시 불리므로 보임·숨김을 모두 정한다. */
private fun MainActivity.bindBoard(b: ViewLeagueBinding, real: Boolean) {
    val model = leagueTab == LeagueTab.MODEL
    val modelOnly = if (model) View.VISIBLE else View.GONE
    b.tierBar.visibility = modelOnly
    b.tvToLine.visibility = modelOnly
    b.btnLeagueReport.visibility = if (real) modelOnly else View.GONE
    when {
        !model -> showGroupBoard(b, real)
        real -> showRealBoard(b)
        else -> showDemoBoard(b)
    }
}

private fun MainActivity.showRealBoard(b: ViewLeagueBinding) {
    val s = standing()
    b.tvRankCaption.text = "내 순위 · $myTier 리그 397명 중"
    b.tvBigRank.text = s.rank?.let { "P$it" } ?: "–"
    b.tvToLine.text = promotionText(s)
    if (s.rank == null) b.tvToLine.visibility = View.GONE
    realRows(b.layoutLeagueRows, s)
    // 새 시즌 첫 주행 전까지 지난 시즌 결과를 한 줄로 알린다
    if (s.rank == null) seasonStore.notice()?.let { b.layoutLeagueRows.addView(note(it), 0) }
    if (s.rank != null) b.layoutLeagueRows.addView(note("승급 P${s.promotionRank} · 강등 P${s.demotionRank}"))
}

private fun MainActivity.showDemoBoard(b: ViewLeagueBinding) {
    val day = DemoData.today
    val l = DemoData.league
    val me = DemoData.ranks.getValue(day)
    val line = LeagueCalculator.promotionRank(l.total)
    // 순위표는 1·2위, 승급 라인 앞뒤, 내 앞뒤만 보여 준다(92점 카드의 추월 세 명 줄은 카드에서만)
    val rows = DemoData.neighbors().filter { it.rank <= 2 || it.rank in (line - 1)..line || it.rank in (me - 1)..(me + 1) }
    val lineAvg = rows.first { it.rank == line }.avg
    b.tvRankCaption.text = "내 순위 · ${l.tier} 리그 ${l.total}명 중"
    b.tvBigRank.text = "P$me"
    b.tvToLine.text = "승급 라인까지 ${me - line}계단 · ${LeagueCalculator.round1(lineAvg - DemoData.seasonAverage(day))}점"

    b.layoutLeagueRows.removeAllViews()
    var last = 0
    for (r in rows) {
        if (last > 0 && r.rank > last + 1) b.layoutLeagueRows.addView(dots())
        b.layoutLeagueRows.addView(timingRow(r, r.name == "나").view)
        if (r.rank == line) b.layoutLeagueRows.addView(zoneLine("승급 라인 · 상위 30%", R.color.yellow))
        last = r.rank
    }
    b.layoutLeagueRows.addView(dots())
    b.layoutLeagueRows.addView(zoneLine("강등 라인 · 하위 30%", R.color.ink_muted))
}

/** 지역·친구 순위표. 다른 운전자는 가상 데이터이고, 내 행은 실제 모드면 실제 기록, 시연 모드면 시연 기록이다. */
private fun MainActivity.showGroupBoard(b: ViewLeagueBinding, real: Boolean) {
    val region = leagueTab == LeagueTab.REGION
    val drivers = if (region) SeasonDrivers.region else SeasonDrivers.friends
    val radius = if (region) 2 else drivers.size
    val s = if (real) SeasonRanking.calculate(leagueTrips(), LocalDate.now(), myTier, drivers, radius)
        else SeasonRanking.rank(DemoData.seasonAverage(DemoData.today), DemoData.league.tier, drivers, radius)
    b.tvRankCaption.text = "내 순위 · ${if (region) myRegion else "친구"} ${s.total}명 중"
    b.tvBigRank.text = s.rank?.let { "P$it" } ?: "–"
    realRows(b.layoutLeagueRows, s)
}
