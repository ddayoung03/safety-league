package io.github.ddayoung03.safetyleague.ui

import android.car.Car
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import io.github.ddayoung03.safetyleague.ui.view.Pose
import io.github.ddayoung03.safetyleague.ui.view.RabbitView
import io.github.ddayoung03.safetyleague.ui.view.canAnimateCrew
import io.github.ddayoung03.safetyleague.ui.motion.MotionScope
import io.github.ddayoung03.safetyleague.ui.motion.SoundFx
import io.github.ddayoung03.safetyleague.ui.motion.canPlaySound
import io.github.ddayoung03.safetyleague.ui.motion.ScreenTransition
import io.github.ddayoung03.safetyleague.ui.motion.enter
import io.github.ddayoung03.safetyleague.ui.motion.revealFrom
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.View
import android.view.inputmethod.InputMethodManager
import io.github.ddayoung03.safetyleague.core.collector.VehicleProfileReader
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import io.github.ddayoung03.safetyleague.R
import io.github.ddayoung03.safetyleague.core.calculator.Wallet
import io.github.ddayoung03.safetyleague.core.data.DemoData
import io.github.ddayoung03.safetyleague.core.data.GuideCategory
import io.github.ddayoung03.safetyleague.core.model.CompletedTrip
import io.github.ddayoung03.safetyleague.core.collector.LocationPolicy
import io.github.ddayoung03.safetyleague.core.repository.LocationConsentStore
import io.github.ddayoung03.safetyleague.core.repository.PointWalletStore
import io.github.ddayoung03.safetyleague.core.repository.TripRepository
import io.github.ddayoung03.safetyleague.databinding.ActivityMainBinding
import io.github.ddayoung03.safetyleague.databinding.ViewDrivingBinding
import io.github.ddayoung03.safetyleague.service.TripTrackingService
import io.github.ddayoung03.safetyleague.weather.*

internal enum class Screen { HOME, WIN, LOW, REPLAY, LEAGUE, REPORT, DRIVING, VEHICLE, PLACEMENT, CONSENT, SETTLEMENT, SHOP, GUIDE }
internal enum class LeagueTab { MODEL, REGION, FRIENDS }

/**
 * 화면 6종을 하나의 액티비티에서 바꿔 끼운다.
 * - 주행 중(CarUxRestrictions가 최적화 요구)에는 주행 중 화면만 보이고 다른 화면으로 갈 수 없다.
 * - P단에서 트립이 끝나면 서비스가 결과를 넘기고, 결과 카드(점수 70 이상 이긴 날 / 미만 아쉬운 날)를 연다.
 * - 시연 모드(왼쪽 위 모서리 길게 누르기)에서만 화면 전환 바와 각본 주행 버튼이 보인다.
 */
class MainActivity : AppCompatActivity() {

    internal lateinit var binding: ActivityMainBinding
    internal lateinit var repository: TripRepository
    internal var service: TripTrackingService? = null
    private var car: Car? = null
    private var uxManager: CarUxRestrictionsManager? = null

    internal val motion = MotionScope()
    internal lateinit var sound: SoundFx
    internal var restricted = false
    /** CarUxRestrictions에 연결하지 못했으면 기어·속도로 주행 중인지 판단한다(모르면 주행 중). */
    private var uxFallback = false
    /** 주행 중(제한 중)에 끝난 트립. 제한이 풀리면 이 트립의 결과 카드를 연다. */
    private var pendingResult: CompletedTrip? = null
    private var screen = Screen.HOME
    internal var demoMode = false
    /** 시연 모드에서 각본 주행을 돌렸으면 결과 카드는 각본 화면(92점/64점)으로 보여 준다. */
    internal var demoResultWin: Boolean? = null
    /** 트립이나 시연 각본이 방금 끝났다. 큰 연출은 이때만 돈다. show()가 화면을 만든 뒤 거짓으로 돌린다. */
    internal var resultJustFinished = false
    /** 결과 카드와 리플레이가 보여 줄 실제 트립 (없으면 시연 데이터) */
    private var shownTrip: CompletedTrip? = null
    internal var drivingKmShown = -1
    internal var drivingBinding: ViewDrivingBinding? = null
    internal var weatherSignals = WeatherSignals()
    internal val weatherGate = WeatherTransitionGate()
    internal var displayedWeather: Weather? = null
    internal var demoWeather = Weather.SUN
    private var rabbit: RabbitView? = null
    private val weatherSource by lazy {
        VehicleWeatherSource(this) { signals ->
            runOnUiThread {
                weatherSignals = signals
                if (uxFallback) applyRestriction(isDrivingByMotion(signals))
                syncRabbits()
                updateWeather()
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val drivingTick = object : Runnable {
        override fun run() {
            if (screen == Screen.DRIVING) {
                val km = service?.currentTripKm?.toInt() ?: 0
                if (km != drivingKmShown) updateDrivingProgress() // keep weather phase and transition gate
                updateWeather()
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
            syncWithTrip()
        }
        override fun onServiceDisconnected(name: ComponentName?) { service = null }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!intent.getBooleanExtra(PermissionActivity.EXTRA_ASKED, false) && missingPermissions(this).isNotEmpty()) {
            startActivity(Intent(this, PermissionActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            finishAndRemoveTask()
            return
        }
        sound = SoundFx(this) // 불러오는 데 시간이 걸려 첫 연출 전에 미리 만든다
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(this, backHandler)
        binding.flagLoaderRabbit.pose = Pose.LOAD
        binding.root.post { showFlagLoader(text = R.string.loader_start) }
        repository = TripRepository.getInstance(this)
        setupDemoControls()
        startTracking()
        seasonStore.settleIfNeeded(repository)
        if (profileStore.load() == null) {
            val detected = VehicleProfileReader.read(this)
            if (detected != null) { profileStore.save(detected); show(Screen.PLACEMENT) }
            else show(Screen.VEHICLE)
            return
        }
        // 주행 중인지는 onStart에서야 안다. 안 본 결과는 서비스에 붙은 뒤 syncWithTrip이 연다.
        intent.removeExtra(TripTrackingService.EXTRA_SHOW_RESULT_CARD)
        if (LocationPolicy.shouldAsk(consentStore.load(), restricted, demoMode)) { show(Screen.CONSENT); return }
        show(Screen.HOME)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (!intent.getBooleanExtra(TripTrackingService.EXTRA_SHOW_RESULT_CARD, false)) return
        intent.removeExtra(TripTrackingService.EXTRA_SHOW_RESULT_CARD)
        val latest = repository.latestCompletedTrip.value?.takeIf(::isUnseen) ?: return
        if (restricted) pendingResult = latest else showResult(latest)
    }

    override fun onStart() {
        super.onStart()
        bindService(Intent(this, TripTrackingService::class.java), connection, Context.BIND_AUTO_CREATE)
        connectUxRestrictions()
        weatherSource.start()
        // 앱을 켜 둔 채 달이 바뀔 수 있다. 시상대는 서비스에 붙은 뒤 syncWithTrip이 띄운다.
        if (::repository.isInitialized) seasonStore.settleIfNeeded(repository)
    }

    override fun onResume() {
        super.onResume()
        binding.root.post { syncRabbits() } // 수명 주기는 onResume이 끝나야 RESUMED가 된다
        if (screen == Screen.DRIVING) {
            handler.removeCallbacks(drivingTick)
            handler.postDelayed(drivingTick, 5_000)
        }
    }

    override fun onPause() {
        motion.finishAll()
        syncRabbits(allowed = false)
        handler.removeCallbacks(drivingTick)
        super.onPause()
    }

    override fun onDestroy() {
        if (::sound.isInitialized) sound.release()
        super.onDestroy()
    }

    override fun onStop() {
        weatherSource.stop()
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

    internal fun startTracking() {
        ContextCompat.startForegroundService(this, Intent(this, TripTrackingService::class.java))
    }

    // ---------- 주행 중 제한 ----------

    private fun connectUxRestrictions() {
        uxFallback = false
        runCatching {
            car = Car.createCar(this)
            uxManager = (car?.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as? CarUxRestrictionsManager)?.also { m ->
                applyRestriction(m.currentCarUxRestrictions?.isRequiresDistractionOptimization == true)
                m.registerListener { r -> runOnUiThread { applyRestriction(r.isRequiresDistractionOptimization) } }
            }
        }.onFailure { android.util.Log.w("VehicleSignals", "Driving restriction connection failed", it) }
        if (uxManager == null) {
            // 연결 실패는 제한 없음이 아니라 주행 중으로 본다. 이후 기어 P·정차 신호가 오면 풀린다.
            uxFallback = true
            applyRestriction(isDrivingByMotion(weatherSignals))
        }
    }

    private fun applyRestriction(on: Boolean) {
        if (on == restricted) return
        restricted = on
        syncRabbits()
        binding.demoBar.visibility = if (!on && demoMode) View.VISIBLE else View.GONE
        if (on) { hideKeyboard(); show(Screen.DRIVING) }
        else if (screen == Screen.DRIVING) {
            if (profileStore.load() == null) { show(Screen.VEHICLE); return }
            // 이번 주행이 P로 끝났으면 그 결과. 트립이 아직 진행 중이면(신호 대기 등) 주행 중 화면 유지.
            // 이전 트립(latestCompletedTrip)은 열지 않는다. 결과가 나중에 오면 onTripResult가 연다.
            val pending = pendingResult
            pendingResult = null
            if (pending != null) onTripResult(pending) else syncWithTrip()
        }
    }

    // ---------- 시연 모드 ----------

    private fun setupDemoControls() {
        binding.demoHotspot.setOnLongClickListener {
            if (restricted || service?.demoRunning == true) return@setOnLongClickListener true
            demoMode = !demoMode
            if (demoMode) demoWallet = DemoData.startWallet()
            if (!demoMode) { demoResultWin = null; show(if (profileStore.load() == null) Screen.VEHICLE else Screen.HOME) }
            binding.demoBar.visibility = if (demoMode) View.VISIBLE else View.GONE
            updateSdkStatus()
            true
        }
        binding.tabResultWin.setOnClickListener { demoResultWin = true; shownTrip = null; show(Screen.WIN) }
        binding.tabResultLow.setOnClickListener { demoResultWin = false; shownTrip = null; show(Screen.LOW) }
        binding.tabReplay.setOnClickListener { show(Screen.REPLAY) }
        binding.tabLeague.setOnClickListener { show(Screen.LEAGUE) }
        binding.tabPlacement.setOnClickListener { show(Screen.PLACEMENT) }
        binding.tabReport.setOnClickListener { show(Screen.REPORT) }
        binding.tabDriving.setOnClickListener { show(Screen.DRIVING) }
        binding.btnWeather.setOnClickListener {
            if (restricted) return@setOnClickListener
            demoWeather = Weather.entries[(demoWeather.ordinal + 1) % Weather.entries.size]
            binding.btnWeather.text = getString(R.string.weather_button, demoWeather.label)
        }
        binding.btnSettlement.setOnClickListener { demoWallet = DemoData.startWallet(); show(Screen.SETTLEMENT) }
        binding.btnSimWin.setOnClickListener { runDemo(true) }
        binding.btnSimLow.setOnClickListener { runDemo(false) }
    }

    private fun runDemo(win: Boolean) {
        val s = service ?: return
        if (s.isTracking) return // 진행 중인 실제 트립을 각본이 끊지 않게 한다
        demoResultWin = win
        show(Screen.DRIVING)
        s.runDemo(win)
    }

    private fun updateSdkStatus() {
        val problems = service?.sdkProblems.orEmpty() + weatherSource.problems
        binding.tvSdkStatus.visibility = View.GONE
        if (problems.isNotEmpty()) android.util.Log.w("VehicleSignals", problems.joinToString("\n"))
    }

    private fun onTripResult(trip: CompletedTrip) {
        if (restricted) { pendingResult = trip; return } // 주차(P)하면 제한이 풀리면서 applyRestriction이 연다
        resultJustFinished = true // 시연 각본의 끝도 여기로 온다
        if (service?.lastResultWasDemo == true) show(if (demoResultWin == true) Screen.WIN else Screen.LOW)
        else showResult(trip)
    }

    // ---------- 복귀 ----------

    private val seenPrefs by lazy { getSharedPreferences("result_seen", Context.MODE_PRIVATE) }
    private fun tripKey(t: CompletedTrip) = "${t.trip.date} ${t.startTime} ${t.endTime}"
    /** 지난달 트립은 정산으로 끝난 일이라 결과 카드를 다시 열지 않는다. */
    private fun isUnseen(t: CompletedTrip) = seenPrefs.getString("trip", null) != tripKey(t) &&
        t.trip.date.startsWith(java.time.YearMonth.now().toString())
    private fun markSeen(t: CompletedTrip) {
        seenPrefs.edit().putString("trip", tripKey(t)).apply()
        getSystemService(android.app.NotificationManager::class.java).cancel(TripTrackingService.RESULT_ID)
    }

    /** 서비스에 다시 붙었을 때 화면을 트립 상태에 맞춘다. 첫 설정 화면과 시상대는 건드리지 않는다. */
    private fun syncWithTrip() {
        val s = service ?: return // 붙으면 onServiceConnected가 다시 부른다
        if (demoMode) return
        if (!s.isTracking && screen in listOf(Screen.VEHICLE, Screen.PLACEMENT, Screen.CONSENT, Screen.SETTLEMENT)) return
        val latest = repository.latestCompletedTrip.value
        val unseen = latest != null && seasonStore.card() == null && isUnseen(latest)
        when (resumeAction(screen, restricted, s.isTracking, unseen)) {
            Resume.DRIVING -> show(Screen.DRIVING)
            Resume.RESULT -> showResult(latest!!)
            Resume.HOME -> show(Screen.HOME)
            Resume.STAY -> if ((screen == Screen.LEAGUE || screen == Screen.HOME) && !restricted && seasonStore.card() != null) show(screen)
        }
    }

    private val backHandler = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            val panel = binding.screenContainer.findViewById<View>(R.id.confirm_panel)
            val target = backTarget(screen, profileStore.load() != null)
            when {
                panel?.visibility == View.VISIBLE -> panel.visibility = View.GONE
                screen == Screen.REPLAY -> realTrip?.let { showResult(it) } ?: show(if (demoResultWin == true) Screen.WIN else Screen.LOW)
                target != null -> show(target)
                else -> {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        }
    }

    // ---------- 화면 전환 ----------

    internal fun showResult(trip: CompletedTrip) {
        if (profileStore.load() == null) { show(Screen.VEHICLE); return }
        demoResultWin = null
        shownTrip = trip
        markSeen(trip)
        show(if (trip.trip.score >= 70.0) Screen.WIN else Screen.LOW)
    }

    /** [origin]을 주면 그 버튼에서 원이 퍼지며 열린다. 없으면(앱 시작, 뒤로) 보통 전환이다. */
    internal fun show(requested: Screen, origin: View? = null) {
        if (restricted && requested != Screen.DRIVING) return
        val target = if (redirectToSettlement(requested, seasonStore.card() != null, demoMode)) Screen.SETTLEMENT else requested
        if (target != Screen.LEAGUE) leagueTab = LeagueTab.MODEL
        screen = target
        drivingBinding = null
        displayedWeather = null
        binding.demoBar.visibility = if (target != Screen.DRIVING && demoMode) View.VISIBLE else View.GONE
        binding.btnWeather.visibility = if (target == Screen.DRIVING) View.GONE else View.VISIBLE
        handler.removeCallbacks(drivingTick)
        motion.finishAll()
        val reveal = origin?.let { ScreenTransition.revealFrom(it, binding.screenContainer, motion) }
        binding.screenContainer.removeAllViews()
        when (target) {
            Screen.WIN -> showResultWin()
            Screen.LOW -> showResultLow()
            Screen.REPLAY -> showReplay()
            Screen.HOME -> showHome()
            Screen.LEAGUE -> showLeague()
            Screen.REPORT -> showReport()
            Screen.VEHICLE -> showVehicleSelection()
            Screen.PLACEMENT -> showPlacement()
            Screen.CONSENT -> showLocationConsent()
            Screen.SETTLEMENT -> showSettlement()
            Screen.SHOP -> showShop()
            Screen.GUIDE -> showGuide()
            Screen.DRIVING -> { showDriving(); handler.postDelayed(drivingTick, 5_000) }
        }
        resultJustFinished = false
        rabbit = binding.screenContainer.findViewById(R.id.rabbit)
        syncRabbits()
        applySkew(binding.screenContainer)
        if (reveal != null) reveal()
        else if (!restricted && target != Screen.WIN && target != Screen.LOW) ScreenTransition.enter(binding.screenContainer, motion) // 결과 화면은 playResultWin·playResultLow가 맡는다
    }

    private fun crewMotionAllowed() = canAnimateCrew(weatherSignals.gear, weatherSignals.speedMps,
        lifecycle.currentState == androidx.lifecycle.Lifecycle.State.RESUMED, restricted)

    /** 연출 시작에 한 번 불러 소리를 내도 되는지 답한다. 포커스는 낼 수 있는 조건일 때만 얻는다. */
    internal fun requestSound() = canPlaySound(weatherSignals.gear, weatherSignals.speedMps,
        lifecycle.currentState == androidx.lifecycle.Lifecycle.State.RESUMED, restricted, focus = true) && sound.requestFocus()

    /** 움직여도 되는 동안만 토끼가 돈다. 화면에 없는 토끼는 뷰가 알아서 쉰다. */
    private fun syncRabbits(allowed: Boolean = crewMotionAllowed()) {
        for (r in listOf(rabbit, binding.flagLoaderRabbit)) if (allowed) r?.start() else r?.stop()
    }

    internal val realTrip get() = shownTrip.takeIf { demoResultWin == null }

    internal fun hideKeyboard() {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(binding.root.windowToken, 0)
        currentFocus?.clearFocus()
    }

    internal val consentStore by lazy { LocationConsentStore(this) }

    internal val profileStore by lazy { io.github.ddayoung03.safetyleague.core.repository.VehicleProfileStore(this) }
    internal val seasonStore by lazy { io.github.ddayoung03.safetyleague.core.repository.SeasonSettlementStore(this) }
    internal val walletStore by lazy { PointWalletStore(this) }
    /** 시연 포인트와 쿠폰은 메모리에만 둔다. 다시 켜면 처음으로 돌아가 리허설을 반복할 수 있다. */
    internal var demoWallet = DemoData.startWallet()
    internal val wallet: Wallet get() = if (demoMode) demoWallet else walletStore.load()
    internal fun saveWallet(w: Wallet): Boolean {
        if (!demoMode) return walletStore.save(w)
        demoWallet = w
        return true
    }
    internal val myTier get() = profileStore.load()?.tier ?: "브론즈"
    internal val myModel get() = profileStore.load()?.model ?: "내 차종"
    internal val myRegion = "부산"
    internal var leagueTab = LeagueTab.MODEL
    internal var shopTab = ShopTab.SHOP
    internal var guideCategory = GuideCategory.DISASTER
    internal var guideCardId: String? = null
    /** 차량 신호로 판정한 지금 날씨. 시연은 시연 바의 날씨를 쓴다. */
    internal val currentWeather get() = if (demoMode) demoWeather else inferWeather(weatherSignals)
    internal val numberFont: Typeface? by lazy { ResourcesCompat.getFont(this, R.font.saira_condensed) }
}
