package io.github.ddayoung03.safetyleague.ui

import android.Manifest
import android.car.Car
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import io.github.ddayoung03.safetyleague.R
import io.github.ddayoung03.safetyleague.core.calculator.LeagueCalculator
import io.github.ddayoung03.safetyleague.core.calculator.RouteNormalizer
import io.github.ddayoung03.safetyleague.core.data.DemoData
import io.github.ddayoung03.safetyleague.core.model.CompletedTrip
import io.github.ddayoung03.safetyleague.core.model.DriverRank
import io.github.ddayoung03.safetyleague.core.model.ReplayEvent
import io.github.ddayoung03.safetyleague.core.repository.TripRepository
import io.github.ddayoung03.safetyleague.databinding.ActivityMainBinding
import io.github.ddayoung03.safetyleague.databinding.ViewDrivingBinding
import io.github.ddayoung03.safetyleague.databinding.ViewLeagueBinding
import io.github.ddayoung03.safetyleague.databinding.ViewReplayBinding
import io.github.ddayoung03.safetyleague.databinding.ViewReportBinding
import io.github.ddayoung03.safetyleague.databinding.ViewResultLowBinding
import io.github.ddayoung03.safetyleague.databinding.ViewResultWinBinding
import io.github.ddayoung03.safetyleague.service.TripTrackingService
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * 화면 6종을 하나의 액티비티에서 바꿔 끼운다.
 * - 주행 중(CarUxRestrictions가 최적화 요구)에는 주행 중 화면만 보이고 다른 화면으로 갈 수 없다.
 * - P단에서 트립이 끝나면 서비스가 결과를 넘기고, 결과 카드(점수 70 이상 이긴 날 / 미만 아쉬운 날)를 연다.
 * - 시연 모드(왼쪽 위 모서리 길게 누르기)에서만 화면 전환 바와 각본 주행 버튼이 보인다.
 */
class MainActivity : AppCompatActivity() {

    private enum class Screen { WIN, LOW, REPLAY, LEAGUE, REPORT, DRIVING }

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: TripRepository
    private var service: TripTrackingService? = null
    private var car: Car? = null
    private var uxManager: CarUxRestrictionsManager? = null

    private var restricted = false
    private var screen = Screen.LEAGUE
    private var demoMode = false
    /** 시연 모드에서 각본 주행을 돌렸으면 결과 카드는 각본 화면(92점/64점)으로 보여 준다. */
    private var demoResultWin: Boolean? = null
    /** 결과 카드와 리플레이가 보여 줄 실제 트립 (없으면 시연 데이터) */
    private var shownTrip: CompletedTrip? = null
    private var drivingKmShown = -1

    private val handler = Handler(Looper.getMainLooper())
    private val drivingTick = object : Runnable {
        override fun run() {
            if (screen == Screen.DRIVING) {
                val km = service?.currentTripKm?.toInt() ?: 0
                if (km != drivingKmShown) showDriving() // 1km 단위로만 다시 그린다
                handler.postDelayed(this, 5_000)
            }
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = (binder as TripTrackingService.LocalBinder).getService().also { s ->
                s.onResult = { trip -> runOnUiThread { onTripResult(trip) } }
            }
            updateSdkStatus()
        }
        override fun onServiceDisconnected(name: ComponentName?) { service = null }
    }

    private val permissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { startTracking() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository = TripRepository.getInstance(this)
        setupDemoControls()
        requestPermissionsThenTrack()
        if (!handleResultIntent(intent)) {
            val latest = repository.latestCompletedTrip.value
            if (latest != null) showResult(latest) else show(Screen.LEAGUE)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleResultIntent(intent)
    }

    private fun handleResultIntent(i: Intent?): Boolean {
        if (i?.getBooleanExtra(TripTrackingService.EXTRA_SHOW_RESULT_CARD, false) != true) return false
        repository.latestCompletedTrip.value?.let { showResult(it) } ?: return false
        return true
    }

    override fun onStart() {
        super.onStart()
        bindService(Intent(this, TripTrackingService::class.java), connection, Context.BIND_AUTO_CREATE)
        connectUxRestrictions()
    }

    override fun onStop() {
        service?.onResult = null
        runCatching { unbindService(connection) }
        service = null
        uxManager?.unregisterListener()
        car?.disconnect()
        car = null; uxManager = null
        handler.removeCallbacks(drivingTick)
        super.onStop()
    }

    // ---------- 권한과 서비스 ----------

    private fun requestPermissionsThenTrack() {
        val wanted = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add("pleos.car.permission.FUSED_LOCATION")
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.filter { ContextCompat.checkSelfPermission(this, it) != android.content.pm.PackageManager.PERMISSION_GRANTED }
        if (wanted.isEmpty()) startTracking() else permissionRequest.launch(wanted.toTypedArray())
    }

    private fun startTracking() {
        ContextCompat.startForegroundService(this, Intent(this, TripTrackingService::class.java))
    }

    // ---------- 주행 중 제한 ----------

    private fun connectUxRestrictions() {
        runCatching {
            car = Car.createCar(this)
            uxManager = (car?.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as? CarUxRestrictionsManager)?.also { m ->
                applyRestriction(m.currentCarUxRestrictions?.isRequiresDistractionOptimization == true)
                m.registerListener { r -> runOnUiThread { applyRestriction(r.isRequiresDistractionOptimization) } }
            }
        }.onFailure { binding.tvSdkStatus.text = "주행 제한 정보 연결 실패: ${it.message}" }
    }

    private fun applyRestriction(on: Boolean) {
        if (on == restricted) return
        restricted = on
        binding.demoBar.visibility = if (!on && demoMode) View.VISIBLE else View.GONE
        if (on) show(Screen.DRIVING)
        else if (screen == Screen.DRIVING) {
            // 멈췄을 때는 트립이 끝났으면 결과, 아니면 리그로
            repository.latestCompletedTrip.value?.takeIf { it == shownTrip || shownTrip == null }?.let { showResult(it) }
                ?: show(Screen.LEAGUE)
        }
    }

    // ---------- 시연 모드 ----------

    private fun setupDemoControls() {
        binding.demoHotspot.setOnLongClickListener {
            if (restricted) return@setOnLongClickListener true
            demoMode = !demoMode
            binding.demoBar.visibility = if (demoMode) View.VISIBLE else View.GONE
            updateSdkStatus()
            true
        }
        binding.tabResultWin.setOnClickListener { demoResultWin = true; shownTrip = null; show(Screen.WIN) }
        binding.tabResultLow.setOnClickListener { demoResultWin = false; shownTrip = null; show(Screen.LOW) }
        binding.tabReplay.setOnClickListener { show(Screen.REPLAY) }
        binding.tabLeague.setOnClickListener { show(Screen.LEAGUE) }
        binding.tabReport.setOnClickListener { show(Screen.REPORT) }
        binding.tabDriving.setOnClickListener { show(Screen.DRIVING) }
        binding.btnSimWin.setOnClickListener { runDemo(true) }
        binding.btnSimLow.setOnClickListener { runDemo(false) }
    }

    private fun runDemo(win: Boolean) {
        val s = service ?: return
        demoResultWin = win
        show(Screen.DRIVING)
        s.runDemo(win)
    }

    private fun updateSdkStatus() {
        val problems = service?.sdkProblems.orEmpty()
        binding.tvSdkStatus.visibility = if (demoMode && problems.isNotEmpty()) View.VISIBLE else View.GONE
        binding.tvSdkStatus.text = "차량 신호 연결 문제\n" + problems.joinToString("\n")
    }

    private fun onTripResult(trip: CompletedTrip) {
        if (restricted) return // 주차(P)하면 제한이 풀리면서 applyRestriction이 결과를 연다
        if (demoResultWin != null) show(if (demoResultWin == true) Screen.WIN else Screen.LOW)
        else showResult(trip)
    }

    // ---------- 화면 전환 ----------

    private fun showResult(trip: CompletedTrip) {
        demoResultWin = null
        shownTrip = trip
        show(if (trip.trip.score >= 70.0) Screen.WIN else Screen.LOW)
    }

    private fun show(target: Screen) {
        if (restricted && target != Screen.DRIVING) return
        screen = target
        handler.removeCallbacks(drivingTick)
        binding.screenContainer.removeAllViews()
        when (target) {
            Screen.WIN -> showResultWin()
            Screen.LOW -> showResultLow()
            Screen.REPLAY -> showReplay()
            Screen.LEAGUE -> showLeague()
            Screen.REPORT -> showReport()
            Screen.DRIVING -> { showDriving(); handler.postDelayed(drivingTick, 5_000) }
        }
    }

    private val today get() = LocalDate.now().toString()
    private val realTrip get() = shownTrip.takeIf { demoResultWin == null }

    // ---------- 1. 결과 카드: 이긴 날 ----------

    private fun showResultWin() {
        val b = ViewResultWinBinding.inflate(layoutInflater, binding.screenContainer, true)
        val day = "2026-11-07"
        val t = realTrip
        val me = DemoData.ranks.getValue(day)
        val before = DemoData.ranks.getValue("2026-11-06")
        b.tvScore.text = (t?.trip?.score ?: DemoData.trips.first { it.date == day }.score).roundToInt().toString()
        b.tvKm.text = "${t?.trip?.km ?: DemoData.trips.first { it.date == day }.km}km"
        b.tvStreak.text = "${if (t != null) repository.getStreak(today) else DemoData.streakAt(day)}일"
        b.tvRankChange.text = "▲${before - me}"
        b.tvToPromotion.text = "승급 라인까지 ${me - LeagueCalculator.promotionRank(DemoData.league.total)}계단"
        t?.let { b.routeBg.routePoints = RouteNormalizer.normalize(it.route).ifEmpty { DemoData.route } }

        val rows = DemoData.neighbors(day)
        val above = rows.firstOrNull { it.rank == me - 1 }
        val myAvg = rows.first { it.name == "나" }.avg
        b.layoutTowerRows.removeAllViews()
        for (r in rows) {
            val isMe = r.name == "나"
            val passed = r.rank in (me + 1)..before
            b.layoutTowerRows.addView(towerRow(r, isMe, nameSuffix = if (isMe) " ▲${before - me}" else "",
                tag = if (passed) "추월" else null, dim = !isMe && !passed && kotlin.math.abs(r.rank - me) >= 2))
            if (r == above) b.layoutTowerRows.addView(note("${LeagueCalculator.round1(above.avg - myAvg)}점 차, 다음 주행에서 따라잡을 수 있어요"))
        }
        b.btnReplay.setOnClickListener { show(Screen.REPLAY) }
    }

    // ---------- 2. 결과 카드: 아쉬운 날 ----------

    private fun showResultLow() {
        val b = ViewResultLowBinding.inflate(layoutInflater, binding.screenContainer, true)
        val t = realTrip
        b.kerbTop.isMuted = true
        b.kerbBottom.isMuted = true
        b.tvScore.text = (t?.trip?.score?.roundToInt() ?: DemoData.lowDayScore).toString()
        b.tvKm.text = "${t?.trip?.km ?: DemoData.lowDayKm}km"
        val streakBefore = if (t != null) service?.streakBeforeToday ?: 0 else DemoData.lowDayStreakBefore
        b.tvStreakChange.text = "${streakBefore}일 → 0일"
        t?.let { b.routeBg.routePoints = RouteNormalizer.normalize(it.route).ifEmpty { DemoData.route } }

        // 피트 보드: 가장 많이 나온 사건 하나만
        val events = t?.events ?: DemoData.lowDayEvents
        val kind = listOf("lane", "brake", "accel").maxByOrNull { k -> events.count { it.kind == k } * 10 - listOf("lane", "brake", "accel").indexOf(k) }
        val count = events.count { it.kind == kind }
        val detail = t?.trip?.detailScore
        when {
            count == 0 -> { b.tvPitTip.text = "출발 전 모든 좌석 벨트 확인하기"; b.tvPitReason.text = "오늘 안전 기본기에서 놓친 점이 있어요"; b.tvPitGain.text = "" }
            kind == "lane" -> {
                b.tvPitTip.text = "차선 바꿀 때 깜빡이부터 켜기"
                b.tvPitReason.text = "오늘 깜빡이 없이 차선 변경 ${count}회"
                b.tvPitGain.text = "매너 +${detail?.let { (25 - it.manner).roundToInt() } ?: 9}점 가능"
            }
            kind == "brake" -> {
                b.tvPitTip.text = "멈출 때는 미리, 천천히 밟기"
                b.tvPitReason.text = "오늘 급제동 ${count}회"
                b.tvPitGain.text = "부드러움 +${detail?.let { (30 - it.smoothness).roundToInt() } ?: 8}점 가능"
            }
            else -> {
                b.tvPitTip.text = "출발은 부드럽게"
                b.tvPitReason.text = "오늘 급가속 ${count}회"
                b.tvPitGain.text = "부드러움 +${detail?.let { (30 - it.smoothness).roundToInt() } ?: 3}점 가능"
            }
        }

        val rows = DemoData.lowBoard
        val me = rows.first { it.name == "나" }
        val target = rows.first { it.rank == DemoData.lowDayRankBefore }
        b.layoutTowerRows.removeAllViews()
        for (r in rows) {
            val isMe = r == me
            val isTarget = r == target
            b.layoutTowerRows.addView(towerRow(r, isMe, nameSuffix = if (isMe) " ▼${me.rank - DemoData.lowDayRankBefore}" else "",
                tag = if (isTarget) "목표" else null, target = isTarget, dim = !isMe && !isTarget))
            if (isTarget) b.layoutTowerRows.addView(note("${LeagueCalculator.round1(target.avg - me.avg)}점 차, 한 번만 잘 달리면 되찾아요"))
        }
        b.btnReplay.setOnClickListener { show(Screen.REPLAY) }
    }

    // ---------- 3. 레이스 리플레이 ----------

    private fun showReplay() {
        val b = ViewReplayBinding.inflate(layoutInflater, binding.screenContainer, true)
        val t = realTrip
        val events: List<ReplayEvent> = t?.events ?: DemoData.lowDayEvents
        b.tvReplayMeta.text = if (t != null) "${t.startTime} ~ ${t.endTime} · ${t.trip.km}km"
        else "${DemoData.lowDayStart} ~ ${DemoData.lowDayEnd} · ${DemoData.lowDayKm}km"
        b.routeReplayView.isBackgroundMode = false
        b.routeReplayView.routePoints = t?.route?.let { RouteNormalizer.normalize(it) }?.ifEmpty { null } ?: DemoData.route
        b.routeReplayView.replayEvents = events

        val names = mapOf("accel" to "급가속", "brake" to "급제동", "lane" to "깜빡이 없이 차선 변경")
        b.layoutEventList.removeAllViews()
        for (e in events.sortedBy { it.minutes }) {
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundColor(color(R.color.cell))
                setPadding(dp(20), dp(10), dp(20), dp(10))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    .apply { setMargins(0, dp(4), 0, dp(4)) }
            }
            item.addView(text(e.at, R.color.grey, number = true).apply { minWidth = dp(90) })
            item.addView(text(names[e.kind] ?: e.kind, if (e.kind == "lane") R.color.yellow else R.color.white))
            b.layoutEventList.addView(item)
        }
        b.btnBack.setOnClickListener {
            if (realTrip != null) showResult(realTrip!!) else show(if (demoResultWin == true) Screen.WIN else Screen.LOW)
        }
    }

    // ---------- 4. 리그 순위 ----------

    private fun showLeague() {
        val b = ViewLeagueBinding.inflate(layoutInflater, binding.screenContainer, true)
        val day = "2026-11-09"
        val l = DemoData.league
        val me = DemoData.ranks.getValue(day)
        val line = LeagueCalculator.promotionRank(l.total)
        val rows = DemoData.neighbors(day)
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
            b.layoutLeagueRows.addView(towerRow(r, r.name == "나", top = r.rank <= 3))
            if (r.rank == line) b.layoutLeagueRows.addView(zoneLine("승급 라인 · 상위 30%", R.color.yellow))
            last = r.rank
        }
        b.layoutLeagueRows.addView(dots())
        b.layoutLeagueRows.addView(zoneLine("강등 라인 · 하위 30%", R.color.red))
    }

    // ---------- 5. 주간 리포트 ----------

    private fun showReport() {
        val b = ViewReportBinding.inflate(layoutInflater, binding.screenContainer, true)
        val from = LocalDate.parse("2026-11-03")
        val week = (0..6).map { from.plusDays(it.toLong()).toString() }
        val names = listOf("월", "화", "수", "목", "금", "토", "일")
        val scores = week.map { d -> DemoData.trips.firstOrNull { it.date == d }?.score?.roundToInt() }
        val avg = DemoData.average(week.first(), week.last())
        val last = DemoData.average("2026-10-27", "2026-11-02")
        val ranks = week.map { DemoData.ranks.getValue(it) }
        val line = LeagueCalculator.promotionRank(DemoData.league.total)
        val lineAvg = DemoData.neighbors(week.last()).first { it.rank == line }.avg

        b.tvReportTitle.text = "11월 2주차 리포트"
        b.tvReportRange.text = "11.03 ~ 11.09 · 주행 ${scores.count { it != null }}일"
        b.tvWeekAvg.text = String.format("%.1f", avg)
        b.tvWeekChange.text = "지난주보다 ${if (avg >= last) "+" else ""}${LeagueCalculator.round1(avg - last)}"
        b.tvLastWeek.text = "지난주 ${last}점"
        val move = ranks.first() - ranks.last()
        b.tvRankMove.text = "P${ranks.first()} → P${ranks.last()} ${if (move >= 0) "▲$move" else "▼${-move}"}"
        b.tvNextPlan.text = "깜빡이 누락만 줄이면 승급 라인까지 ${ranks.last() - line}계단, 평균 +${LeagueCalculator.round1(lineAvg - DemoData.seasonAverage(week.last()))}점이면 돼요"
        b.positionChart.promotionLine = line
        b.positionChart.ranks = ranks

        val best = scores.filterNotNull().maxOrNull()
        b.layoutScoreBars.removeAllViews()
        names.forEachIndexed { i, day ->
            val s = scores[i]
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply { setMargins(dp(4), 0, dp(4), 0) }
            }
            if (s != null) {
                col.addView(text(s.toString(), R.color.white, number = true).apply { gravity = Gravity.CENTER })
                col.addView(View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(((s - 60) / 40f * 170).roundToInt().coerceAtLeast(16)))
                    setBackgroundColor(if (s == best) color(R.color.yellow) else Color.parseColor("#4A4F57"))
                })
            }
            col.addView(text(if (s == null) "$day 휴식" else day, R.color.grey).apply { gravity = Gravity.CENTER })
            b.layoutScoreBars.addView(col)
        }
    }

    // ---------- 6. 주행 중 (정지 화면, 1km 단위 갱신) ----------

    private fun showDriving() {
        binding.screenContainer.removeAllViews()
        val b = ViewDrivingBinding.inflate(layoutInflater, binding.screenContainer, true)
        val s = service
        val real = s?.isTracking == true && demoResultWin == null
        val doneKm = if (real) s!!.currentTripKm else 11.2
        val destKm: Double? = if (real) null else DemoData.trips.first { it.date == "2026-11-07" }.km // 내비 목적지가 없으면 null
        drivingKmShown = doneKm.toInt()
        b.tvProgress.text = if (destKm != null) "${LeagueCalculator.round1(doneKm)} / ${destKm}km · 해운대까지" else "${LeagueCalculator.round1(doneKm)}km 기록됨"
        val streak = if (real) s!!.streakBeforeToday else DemoData.streakAt("2026-11-06")
        b.tvGoal.text = "70점 넘기면 연속 ${streak + 1}일째"

        val cells = destKm?.let { kotlin.math.ceil(it).toInt() } ?: maxOf(10, doneKm.toInt() + 1)
        b.layoutTrackBar.removeAllViews()
        for (i in 0 until cells) {
            b.layoutTrackBar.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(36), 1f).apply { setMargins(dp(3), 0, dp(3), 0) }
                setBackgroundColor(if (i < doneKm.toInt()) color(R.color.yellow) else Color.parseColor("#2B2F35"))
            })
        }
        if (destKm != null) b.layoutTrackBar.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(36)).apply { setMargins(dp(4), 0, 0, 0) }
            setBackgroundColor(color(R.color.white))
        })
    }

    // ---------- 헬퍼 ----------

    private fun dp(v: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).roundToInt()
    private fun dp(v: Float) = dp(v.roundToInt())
    private fun color(id: Int) = ContextCompat.getColor(this, id)
    private val numberFont: Typeface? by lazy { ResourcesCompat.getFont(this, R.font.saira_condensed) }

    private fun text(s: String, colorId: Int, sizeSp: Float = 24f, number: Boolean = false) = TextView(this).apply {
        text = s
        setTextColor(color(colorId))
        textSize = sizeSp
        if (number) setTypeface(numberFont, Typeface.ITALIC)
    }

    private fun note(s: String) = text(s, R.color.yellow).apply { setPadding(dp(12), dp(4), 0, dp(10)) }

    private fun dots() = text("⋯", R.color.grey).apply { gravity = Gravity.CENTER }

    private fun zoneLine(s: String, colorId: Int) = text("─── $s ───", colorId).apply {
        gravity = Gravity.CENTER
        setPadding(0, dp(6), 0, dp(6))
    }

    private fun towerRow(
        r: DriverRank, isMe: Boolean, nameSuffix: String = "", tag: String? = null,
        target: Boolean = false, dim: Boolean = false, top: Boolean = false
    ): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(if (isMe) 70 else 56))
                .apply { setMargins(0, dp(3), 0, dp(3)) }
            setPadding(dp(24), 0, dp(24), 0)
            setBackgroundResource(if (isMe) R.color.yellow else R.color.cell)
        }
        val fg = when {
            isMe -> R.color.asphalt
            dim -> R.color.grey
            else -> R.color.white
        }
        row.addView(text("P${r.rank}", if (isMe) R.color.asphalt else if (top) R.color.gold else R.color.grey, if (isMe) 32f else 28f, number = true)
            .apply { layoutParams = LinearLayout.LayoutParams(dp(120), LinearLayout.LayoutParams.WRAP_CONTENT) })
        row.addView(text(r.name + nameSuffix, fg, if (isMe) 26f else 24f).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        })
        if (tag != null) row.addView(text(tag, if (tag == "추월") R.color.white else R.color.asphalt).apply {
            setPadding(dp(12), dp(2), dp(12), dp(2))
            setBackgroundResource(if (tag == "추월") R.color.red else R.color.yellow)
        })
        row.addView(text(String.format("%.1f", r.avg), fg, if (isMe) 28f else 26f, number = true).apply { setPadding(dp(16), 0, dp(6), 0) })
        if (target) row.setBackgroundResource(R.drawable.shape_target_row)
        return row
    }
}
