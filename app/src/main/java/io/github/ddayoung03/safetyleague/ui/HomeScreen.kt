package io.github.ddayoung03.safetyleague.ui

import io.github.ddayoung03.safetyleague.core.calculator.LeagueCalculator
import io.github.ddayoung03.safetyleague.core.calculator.ShopCatalog
import io.github.ddayoung03.safetyleague.core.calculator.WeeklyReportCalculator
import io.github.ddayoung03.safetyleague.core.data.DemoData
import io.github.ddayoung03.safetyleague.core.data.SafetyGuide
import io.github.ddayoung03.safetyleague.databinding.ViewHomeBinding
import io.github.ddayoung03.safetyleague.ui.motion.ScreenTransition
import io.github.ddayoung03.safetyleague.ui.motion.stagger
import io.github.ddayoung03.safetyleague.ui.view.Pose
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt

/** 홈. 시즌 카드 숫자는 리그·리포트·상점 화면과 같은 계산을 쓴다. 시연 모드는 시연 데이터. */
internal fun MainActivity.showHome() {
    val b = ViewHomeBinding.inflate(layoutInflater, binding.screenContainer, true)
    b.rabbit.pose = Pose.WELCOME
    b.checkerStrip.background = checkerTile()
    val real = !demoMode && demoResultWin == null
    val points = "포인트 ${ShopCatalog.points(wallet.balance)}P"
    val hour = LocalTime.now().hour
    homeStamp = homeStampNow()

    if (real) {
        val now = LocalDate.now()
        val trips = leagueTrips()
        val s = standing(trips)
        val latest = repository.latestCompletedTrip.value
        val today = latest?.takeIf { it.isEligible && it.trip.date == now.toString() }?.trip?.score
        b.tvSeasonTitle.text = "${now.monthValue}월 시즌 · $myModel 리그"
        b.ivTierBelt.setImageResource(tierBelt(myTier))
        b.tvRank.text = s.rank?.let { "P$it" } ?: "–"
        b.tvTier.text = myTier
        b.tvPromotion.text = promotionText(s)
        val last = when {
            latest == null -> "아직 주행 기록이 없어요"
            !latest.isEligible -> "지난 주행은 3km 미만"
            else -> "지난 주행 ${latest.trip.score.roundToInt()}점"
        }
        b.tvSeasonMeta.text = "$last · $points · ${now.lengthOfMonth() - now.dayOfMonth}일 남음"
        b.tvGreeting.text = homeGreeting(hour, s.rank != null && s.stepsToPromotion == 0, today)
        b.tvTileLeague.text = if (s.rank != null) "${myTier} 리그 ${s.total}명" else promotionText(s)
        val avg = WeeklyReportCalculator.calculate(trips, now, myTier).average
        b.tvTileReport.text = avg?.let { String.format("이번 주 평균 %.1f점", it) } ?: "이번 주 주행 기록 없음"
    } else {
        val l = DemoData.league
        val me = DemoData.ranks.getValue(DemoData.today)
        val line = LeagueCalculator.promotionRank(l.total)
        val todayTrip = DemoData.trips.last { it.date == DemoData.today }
        val from = LocalDate.parse(DemoData.today).minusDays(6).toString()
        b.tvSeasonTitle.text = "11월 시즌 · ${l.model} 리그"
        b.ivTierBelt.setImageResource(tierBelt(l.tier))
        b.tvRank.text = "P$me"
        b.tvTier.text = l.tier
        b.tvPromotion.text = "승급 라인까지 ${me - line}계단"
        b.tvSeasonMeta.text = "지난 주행 ${todayTrip.score.roundToInt()}점 · $points · ${DemoData.daysLeft(DemoData.today)}일 남음"
        b.tvGreeting.text = homeGreeting(hour, me <= line, todayTrip.score)
        b.tvTileLeague.text = "${l.tier} 리그 ${l.total}명"
        b.tvTileReport.text = String.format("이번 주 평균 %.1f점", DemoData.average(from, DemoData.today))
    }
    val coupons = wallet.coupons.size
    b.tvTileShop.text = if (coupons > 0) "쿠폰 ${coupons}장 · $points" else points
    b.tvTileGuide.text = SafetyGuide.suggested(currentWeather)?.let { "오늘은 ${it.title} 카드" } ?: "재난 · 상황 · 초보 꿀팁"

    listOf(b.tileLeague, b.tileReport, b.tileShop, b.tileGuide).forEach { it.pressFeedback() }
    b.tileLeague.setOnClickListener { show(Screen.LEAGUE, it) }
    b.tileReport.setOnClickListener { show(Screen.REPORT, it) }
    b.tileShop.setOnClickListener { shopTab = ShopTab.SHOP; show(Screen.SHOP, it) }
    b.tileGuide.setOnClickListener { show(Screen.GUIDE, it) }

    ScreenTransition.stagger(b.root, listOf(b.tvGreeting, b.cardSeason, b.tileLeague, b.tileReport, b.tileShop, b.tileGuide), motion)
}

internal fun homeStampNow(): String = "${LocalDate.now()} ${LocalTime.now().hour}"
