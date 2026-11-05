package io.github.ddayoung03.safetyleague.ui

import android.view.View
import android.widget.TextView
import io.github.ddayoung03.safetyleague.R
import io.github.ddayoung03.safetyleague.core.calculator.LeagueCalculator
import io.github.ddayoung03.safetyleague.core.calculator.SeasonRanking
import io.github.ddayoung03.safetyleague.core.data.DemoData
import io.github.ddayoung03.safetyleague.core.data.SeasonDrivers
import io.github.ddayoung03.safetyleague.databinding.ViewLeagueBinding
import io.github.ddayoung03.safetyleague.ui.view.AsphaltDrawable
import java.time.LocalDate

internal fun MainActivity.showLeague() {
    val b = ViewLeagueBinding.inflate(layoutInflater, binding.screenContainer, true)
    b.root.background = AsphaltDrawable(this)
    b.tvTabModel.setOnClickListener {
        if (leagueTab == LeagueTab.MODEL) show(Screen.VEHICLE) else { leagueTab = LeagueTab.MODEL; show(Screen.LEAGUE) }
    }
    b.tvTabRegion.setOnClickListener { leagueTab = LeagueTab.REGION; show(Screen.LEAGUE) }
    b.tvTabFriends.setOnClickListener { leagueTab = LeagueTab.FRIENDS; show(Screen.LEAGUE) }
    b.tvTabRegion.text = myRegion
    for ((tab, kind) in listOf(b.tvTabModel to LeagueTab.MODEL, b.tvTabRegion to LeagueTab.REGION, b.tvTabFriends to LeagueTab.FRIENDS)) {
        tab.minHeight = dp(76)
        tab.setBackgroundColor(color(if (kind == leagueTab) R.color.white else R.color.cell))
        tab.setTextColor(color(if (kind == leagueTab) R.color.asphalt else R.color.grey))
    }
    if (leagueTab != LeagueTab.MODEL) { showGroupBoard(b); return }
    if (!demoMode && demoResultWin == null) {
        val now = LocalDate.now()
        val s = standing()
        repository.latestCompletedTrip.value?.let { latest ->
            b.btnLeagueBack.visibility = View.VISIBLE
            b.btnLeagueBack.setOnClickListener { showResult(latest) }
        }
        b.btnLeagueReport.visibility = View.VISIBLE
        b.btnLeagueReport.setOnClickListener { show(Screen.REPORT) }
        b.tvSeasonTitle.text = "${now.monthValue}월 시즌 · $myTier 리그"
        b.tvDaysLeft.text = "${now.lengthOfMonth() - now.dayOfMonth}일 남음"
        b.tvTabModel.text = myModel
        b.tvRankCaption.text = "내 순위 · $myTier 리그 397명 중"
        b.tvBigRank.text = s.rank?.let { "P$it" } ?: "–"
        b.tvToLine.text = promotionText(s)
        if (s.rank == null) b.tvToLine.visibility = View.GONE
        b.tvToLine.textSize = 24f
        (b.tvSaved.parent.parent as View).visibility = View.GONE
        for (i in 0 until b.tierBar.childCount) {
            (b.tierBar.getChildAt(i) as TextView).apply {
                setBackgroundColor(color(if (text == myTier) R.color.yellow else R.color.cell))
                setTextColor(color(if (text == myTier) R.color.asphalt else R.color.grey))
            }
        }
        realRows(b.layoutLeagueRows, s)
        // 새 시즌 첫 주행 전까지 지난 시즌 결과를 한 줄로 알린다
        if (s.rank == null) seasonStore.notice()?.let { b.layoutLeagueRows.addView(note(it), 0) }
        if (s.rank != null) b.layoutLeagueRows.addView(note("승급 P${s.promotionRank} · 강등 P${s.demotionRank}"))
        return
    }
    val day = DemoData.today
    val l = DemoData.league
    val me = DemoData.ranks.getValue(day)
    val line = LeagueCalculator.promotionRank(l.total)
    // 순위표는 1·2위, 승급 라인 앞뒤, 내 앞뒤만 보여 준다(92점 카드의 추월 세 명 줄은 카드에서만)
    val rows = DemoData.neighbors().filter { it.rank <= 2 || it.rank in (line - 1)..line || it.rank in (me - 1)..(me + 1) }
    val lineAvg = rows.first { it.rank == line }.avg
    b.tvSeasonTitle.text = "11월 시즌 · ${l.tier} 리그"
    b.tvDaysLeft.text = "${DemoData.daysLeft(day)}일 남음"
    b.tvTabModel.text = l.model
    b.tvRankCaption.text = "내 순위 · ${l.tier} 리그 ${l.total}명 중"
    b.tvBigRank.text = "P$me"
    b.tvToLine.text = "승급 라인까지 ${me - line}계단 · ${LeagueCalculator.round1(lineAvg - DemoData.seasonAverage(day))}점"
    b.tvSaved.text = DemoData.savedWon
    b.tvBrakesCut.text = DemoData.brakesCut
    b.tvBrakesCaption.text = DemoData.brakesCaption

    b.layoutLeagueRows.removeAllViews()
    var last = 0
    for (r in rows) {
        if (last > 0 && r.rank > last + 1) b.layoutLeagueRows.addView(dots())
        b.layoutLeagueRows.addView(towerRow(r, r.name == "나", top = r.rank <= 3, size = rowLeague))
        if (r.rank == line) b.layoutLeagueRows.addView(zoneLine("승급 라인 · 상위 30%", R.color.yellow))
        last = r.rank
    }
    b.layoutLeagueRows.addView(dots())
    b.layoutLeagueRows.addView(zoneLine("강등 라인 · 하위 30%", R.color.red))
}

/** 지역·친구 순위표. 다른 운전자는 가상 데이터이고, 내 행은 실제 모드면 실제 기록, 시연 모드면 시연 기록이다. */
internal fun MainActivity.showGroupBoard(b: ViewLeagueBinding) {
    val real = !demoMode && demoResultWin == null
    val region = leagueTab == LeagueTab.REGION
    val drivers = if (region) SeasonDrivers.region else SeasonDrivers.friends
    val radius = if (region) 2 else drivers.size
    val day = DemoData.today
    val s = if (real) SeasonRanking.calculate(leagueTrips(), LocalDate.now(), myTier, drivers, radius)
        else SeasonRanking.rank(DemoData.seasonAverage(day), DemoData.league.tier, drivers, radius)
    if (real) {
        val now = LocalDate.now()
        repository.latestCompletedTrip.value?.let { latest ->
            b.btnLeagueBack.visibility = View.VISIBLE
            b.btnLeagueBack.setOnClickListener { showResult(latest) }
        }
        b.tvSeasonTitle.text = "${now.monthValue}월 시즌 · $myTier 리그"
        b.tvDaysLeft.text = "${now.lengthOfMonth() - now.dayOfMonth}일 남음"
        b.tvTabModel.text = myModel
        (b.tvSaved.parent.parent as View).visibility = View.GONE
    } else {
        b.tvSeasonTitle.text = "11월 시즌 · ${DemoData.league.tier} 리그"
        b.tvDaysLeft.text = "${DemoData.daysLeft(day)}일 남음"
        b.tvTabModel.text = DemoData.league.model
        b.tvSaved.text = DemoData.savedWon
        b.tvBrakesCut.text = DemoData.brakesCut
        b.tvBrakesCaption.text = DemoData.brakesCaption
    }
    b.tvRankCaption.text = "내 순위 · ${if (region) myRegion else "친구"} ${s.total}명 중"
    b.tvBigRank.text = s.rank?.let { "P$it" } ?: "–"
    b.tvToLine.visibility = View.GONE
    b.tierBar.visibility = View.GONE
    realRows(b.layoutLeagueRows, s)
}
