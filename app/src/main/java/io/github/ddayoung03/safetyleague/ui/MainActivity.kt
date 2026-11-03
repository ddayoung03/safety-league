package io.github.ddayoung03.safetyleague.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.github.ddayoung03.safetyleague.R
import io.github.ddayoung03.safetyleague.core.data.DemoData
import io.github.ddayoung03.safetyleague.core.calculator.LeagueCalculator
import io.github.ddayoung03.safetyleague.core.model.CompletedTrip
import io.github.ddayoung03.safetyleague.core.model.DriverRank
import io.github.ddayoung03.safetyleague.core.model.ReplayEvent
import io.github.ddayoung03.safetyleague.core.model.Trip
import io.github.ddayoung03.safetyleague.core.repository.TripRepository
import io.github.ddayoung03.safetyleague.databinding.ActivityMainBinding
import io.github.ddayoung03.safetyleague.databinding.ViewDrivingBinding
import io.github.ddayoung03.safetyleague.databinding.ViewLeagueBinding
import io.github.ddayoung03.safetyleague.databinding.ViewReplayBinding
import io.github.ddayoung03.safetyleague.databinding.ViewReportBinding
import io.github.ddayoung03.safetyleague.databinding.ViewResultLowBinding
import io.github.ddayoung03.safetyleague.databinding.ViewResultWinBinding
import io.github.ddayoung03.safetyleague.service.TripTrackingService
import io.github.ddayoung03.safetyleague.ui.view.WeatherType

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val repository = TripRepository.getInstance()

    private var trackingService: TripTrackingService? = null
    private var isServiceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TripTrackingService.LocalBinder
            trackingService = binder.getService()
            isServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            trackingService = null
            isServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bindTrackingService()
        setupNavigationTabs()
        setupSimulationButtons()

        // 시작 기본 화면: 결과 카드 (이긴 날)
        showResultWin()
    }

    private fun bindTrackingService() {
        val intent = Intent(this, TripTrackingService::class.java)
        startService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
        super.onDestroy()
    }

    private fun setupNavigationTabs() {
        val tabs = listOf(
            binding.tabResultWin to { showResultWin() },
            binding.tabResultLow to { showResultLow() },
            binding.tabReplay to { showReplay() },
            binding.tabLeague to { showLeague() },
            binding.tabReport to { showReport() },
            binding.tabDriving to { showDriving() }
        )

        tabs.forEach { (button, action) ->
            button.setOnClickListener {
                resetTabColors()
                button.setTextColor(ContextCompat.getColor(this, R.color.yellow))
                action()
            }
        }
    }

    private fun resetTabColors() {
        val grey = ContextCompat.getColor(this, R.color.grey)
        binding.tabResultWin.setTextColor(grey)
        binding.tabResultLow.setTextColor(grey)
        binding.tabReplay.setTextColor(grey)
        binding.tabLeague.setTextColor(grey)
        binding.tabReport.setTextColor(grey)
        binding.tabDriving.setTextColor(grey)
    }

    private fun setupSimulationButtons() {
        // 이긴 날 모의 주행 실행
        binding.btnSimWin.setOnClickListener {
            resetTabColors()
            binding.tabDriving.setTextColor(ContextCompat.getColor(this, R.color.yellow))
            showDriving()
            trackingService?.mockDataSource?.simulateWinDay(speedMultiplier = 15L)

            // 주차 시점에 결과 화면 자동 전환
            window.decorView.postDelayed({
                resetTabColors()
                binding.tabResultWin.setTextColor(ContextCompat.getColor(this, R.color.yellow))
                showResultWin()
            }, 6000L)
        }

        // 아쉬운 날 모의 주행 실행
        binding.btnSimLow.setOnClickListener {
            resetTabColors()
            binding.tabDriving.setTextColor(ContextCompat.getColor(this, R.color.yellow))
            showDriving()
            trackingService?.mockDataSource?.simulateLowDay(speedMultiplier = 15L)

            // 주차 시점에 결과 화면 자동 전환
            window.decorView.postDelayed({
                resetTabColors()
                binding.tabResultLow.setTextColor(ContextCompat.getColor(this, R.color.yellow))
                showResultLow()
            }, 7500L)
        }
    }

    // ==========================================
    // 1. 주행 결과 카드: 이긴 날 (92점 + 불타는 모션)
    // ==========================================
    private fun showResultWin(trip: Trip? = null) {
        val winBinding = ViewResultWinBinding.inflate(layoutInflater, binding.screenContainer, false)
        binding.screenContainer.removeAllViews()
        binding.screenContainer.addView(winBinding.root)

        val score = trip?.score?.toInt() ?: 92
        val km = trip?.km ?: 18.4
        val streak = DemoData.streakAt("2026-11-07")

        winBinding.tvScore.text = score.toString()
        winBinding.tvKm.text = "${km}km"
        winBinding.tvStreak.text = "${streak}일"

        // 고득점 시 불타는 모션 활성화
        winBinding.flameScoreView.startFlameAnimation()

        // 순위 타워 렌더링
        val rows = DemoData.neighbors("2026-11-07")
        winBinding.layoutTowerRows.removeAllViews()

        for (r in rows) {
            val isMe = r.name == "나"
            val rowView = createTowerRowView(
                rank = r.rank,
                name = if (isMe) "나 ▲3" else r.name,
                avg = r.avg,
                isMe = isMe,
                tagText = if (r.rank == 126) "추월" else null,
                isDim = kotlin.math.abs(r.rank - 125) >= 2 && r.rank != 126
            )
            winBinding.layoutTowerRows.addView(rowView)

            if (r.rank == 124) {
                val gapView = TextView(this).apply {
                    text = "0.3점 차, 다음 주행에서 따라잡을 수 있어요"
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.yellow))
                    textSize = 18f
                    setPadding(12, 4, 0, 12)
                }
                winBinding.layoutTowerRows.addView(gapView)
            }
        }

        // 리플레이 보기 버튼
        winBinding.btnReplay.setOnClickListener {
            resetTabColors()
            binding.tabReplay.setTextColor(ContextCompat.getColor(this, R.color.yellow))
            showReplay()
        }
    }

    // ==========================================
    // 2. 주행 결과 카드: 아쉬운 날 (64점 + 비 내리는 모션)
    // ==========================================
    private fun showResultLow(trip: Trip? = null) {
        val lowBinding = ViewResultLowBinding.inflate(layoutInflater, binding.screenContainer, false)
        binding.screenContainer.removeAllViews()
        binding.screenContainer.addView(lowBinding.root)

        val score = trip?.score?.toInt() ?: 64
        lowBinding.tvScore.text = score.toString()

        // 낮은 점수 시 비 내리는 모션 활성화
        lowBinding.rainScoreView.startRainAnimation()
        lowBinding.kerbTop.isMuted = true
        lowBinding.kerbBottom.isMuted = true

        // 순위 타워 (목표 강조)
        lowBinding.layoutTowerRows.removeAllViews()
        val lowRows = listOf(
            DriverRank(124, "깜빡이장인", 89.1),
            DriverRank(125, "해운대드라이버", 88.7),
            DriverRank(126, "나 ▼1", 88.3),
            DriverRank(127, "조용한퇴근", 88.2)
        )

        for (r in lowRows) {
            val isMe = r.name.startsWith("나")
            val isTarget = r.rank == 125
            val rowView = createTowerRowView(
                rank = r.rank,
                name = r.name,
                avg = r.avg,
                isMe = isMe,
                tagText = if (isTarget) "목표" else null,
                isTarget = isTarget,
                isDim = r.rank == 124 || r.rank == 127
            )
            lowBinding.layoutTowerRows.addView(rowView)

            if (r.rank == 125) {
                val gapView = TextView(this).apply {
                    text = "0.4점 차, 한 번만 잘 달리면 되찾아요"
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.yellow))
                    textSize = 18f
                    setPadding(12, 4, 0, 10)
                }
                lowBinding.layoutTowerRows.addView(gapView)
            }
        }

        // 리플레이 보기 버튼
        lowBinding.btnReplay.setOnClickListener {
            resetTabColors()
            binding.tabReplay.setTextColor(ContextCompat.getColor(this, R.color.yellow))
            showReplay()
        }
    }

    // ==========================================
    // 3. 레이스 리플레이 (경로 서킷 + 7개 사건)
    // ==========================================
    private fun showReplay() {
        val replayBinding = ViewReplayBinding.inflate(layoutInflater, binding.screenContainer, false)
        binding.screenContainer.removeAllViews()
        binding.screenContainer.addView(replayBinding.root)

        val day = DemoData.lowDayEvents
        replayBinding.routeReplayView.isBackgroundMode = false
        replayBinding.routeReplayView.replayEvents = day

        // 사건 목록 바인딩
        replayBinding.layoutEventList.removeAllViews()
        val eventNames = mapOf(
            "accel" to "급가속",
            "brake" to "급제동",
            "lane" to "깜빡이 없이 차선 변경"
        )

        for (e in day) {
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 4, 0, 8) }
                setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.cell))
                setPadding(20, 12, 20, 12)
            }

            val timeTv = TextView(this).apply {
                text = e.at
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.grey))
                textSize = 20f
                layoutParams = LinearLayout.LayoutParams(90, LinearLayout.LayoutParams.WRAP_CONTENT)
            }

            val nameTv = TextView(this).apply {
                text = eventNames[e.kind] ?: e.kind
                val color = if (e.kind == "lane") R.color.yellow else if (e.kind == "brake") R.color.red else R.color.white
                setTextColor(ContextCompat.getColor(this@MainActivity, color))
                textSize = 20f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            item.addView(timeTv)
            item.addView(nameTv)
            replayBinding.layoutEventList.addView(item)
        }

        // 뒤로가기
        replayBinding.btnBack.setOnClickListener {
            resetTabColors()
            binding.tabResultLow.setTextColor(ContextCompat.getColor(this, R.color.yellow))
            showResultLow()
        }
    }

    // ==========================================
    // 4. 리그 순위 타워
    // ==========================================
    private fun showLeague() {
        val leagueBinding = ViewLeagueBinding.inflate(layoutInflater, binding.screenContainer, false)
        binding.screenContainer.removeAllViews()
        binding.screenContainer.addView(leagueBinding.root)

        val rows = DemoData.neighbors("2026-11-09")
        val line = LeagueCalculator.promotionRank(397) // P119

        leagueBinding.layoutLeagueRows.removeAllViews()
        var lastRank = 0

        for (r in rows) {
            if (lastRank > 0 && r.rank > lastRank + 1) {
                val dots = TextView(this).apply {
                    text = "⋯"
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.grey))
                    textSize = 22f
                    gravity = android.view.Gravity.CENTER
                    setPadding(0, 4, 0, 4)
                }
                leagueBinding.layoutLeagueRows.addView(dots)
            }

            val isMe = r.name == "나"
            val rowView = createTowerRowView(
                rank = r.rank,
                name = r.name,
                avg = r.avg,
                isMe = isMe,
                isTop3 = r.rank <= 2
            )
            leagueBinding.layoutLeagueRows.addView(rowView)

            if (r.rank == line) {
                // 승급 라인 표시선
                val promoLine = TextView(this).apply {
                    text = "--- 승급 라인 · 상위 30% (P119) ---"
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.yellow))
                    textSize = 18f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    gravity = android.view.Gravity.CENTER
                    setPadding(0, 8, 0, 8)
                }
                leagueBinding.layoutLeagueRows.addView(promoLine)
            }
            lastRank = r.rank
        }

        // 강등 라인 표시선
        val demoDots = TextView(this).apply {
            text = "⋯"
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.grey))
            textSize = 22f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 4, 0, 4)
        }
        val demoLine = TextView(this).apply {
            text = "--- 강등 라인 · 하위 30% (P279) ---"
            setTextColor(Color.parseColor("#F07A80"))
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
            setPadding(0, 8, 0, 8)
        }
        leagueBinding.layoutLeagueRows.addView(demoDots)
        leagueBinding.layoutLeagueRows.addView(demoLine)
    }

    // ==========================================
    // 5. 주간 리포트
    // ==========================================
    private fun showReport() {
        val reportBinding = ViewReportBinding.inflate(layoutInflater, binding.screenContainer, false)
        binding.screenContainer.removeAllViews()
        binding.screenContainer.addView(reportBinding.root)

        // 요일별 점수 막대 그래프 생성
        val days = listOf("월" to 84, "화" to 88, "수" to null, "목" to 90, "금" to 92, "토" to 86, "일" to 93)
        reportBinding.layoutScoreBars.removeAllViews()

        for ((day, score) in days) {
            val barItem = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                    .apply { setMargins(4, 0, 4, 0) }
            }

            if (score != null) {
                val scoreTv = TextView(this).apply {
                    text = score.toString()
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                    textSize = 18f
                    gravity = android.view.Gravity.CENTER
                }

                val barView = View(this).apply {
                    val barH = ((score - 60).toFloat() / 40f * 260f).toInt().coerceAtLeast(30)
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, barH)
                    setBackgroundColor(
                        if (score == 93) ContextCompat.getColor(this@MainActivity, R.color.yellow)
                        else Color.parseColor("#4A4F57")
                    )
                }
                barItem.addView(scoreTv)
                barItem.addView(barView)
            } else {
                val restTv = TextView(this).apply {
                    text = "휴식"
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.grey))
                    textSize = 16f
                    gravity = android.view.Gravity.CENTER
                }
                barItem.addView(restTv)
            }

            val dayTv = TextView(this).apply {
                text = day
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.grey))
                textSize = 18f
                gravity = android.view.Gravity.CENTER
                setPadding(0, 8, 0, 0)
            }
            barItem.addView(dayTv)
            reportBinding.layoutScoreBars.addView(barItem)
        }
    }

    // ==========================================
    // 6. 주행 중 화면 (날씨 애니메이션 + 1km 트랙)
    // ==========================================
    private fun showDriving() {
        val drivingBinding = ViewDrivingBinding.inflate(layoutInflater, binding.screenContainer, false)
        binding.screenContainer.removeAllViews()
        binding.screenContainer.addView(drivingBinding.root)

        // 날씨 앰비언트 모션 버튼 연결
        drivingBinding.btnWeatherSunny.setOnClickListener {
            drivingBinding.weatherAmbientView.weatherType = WeatherType.SUNNY
            drivingBinding.btnWeatherSunny.setTextColor(ContextCompat.getColor(this, R.color.yellow))
            drivingBinding.btnWeatherRainy.setTextColor(ContextCompat.getColor(this, R.color.grey))
            drivingBinding.btnWeatherSnowy.setTextColor(ContextCompat.getColor(this, R.color.grey))
        }

        drivingBinding.btnWeatherRainy.setOnClickListener {
            drivingBinding.weatherAmbientView.weatherType = WeatherType.RAINY
            drivingBinding.btnWeatherRainy.setTextColor(ContextCompat.getColor(this, R.color.yellow))
            drivingBinding.btnWeatherSunny.setTextColor(ContextCompat.getColor(this, R.color.grey))
            drivingBinding.btnWeatherSnowy.setTextColor(ContextCompat.getColor(this, R.color.grey))
        }

        drivingBinding.btnWeatherSnowy.setOnClickListener {
            drivingBinding.weatherAmbientView.weatherType = WeatherType.SNOWY
            drivingBinding.btnWeatherSnowy.setTextColor(ContextCompat.getColor(this, R.color.yellow))
            drivingBinding.btnWeatherSunny.setTextColor(ContextCompat.getColor(this, R.color.grey))
            drivingBinding.btnWeatherRainy.setTextColor(ContextCompat.getColor(this, R.color.grey))
        }

        // 1km 트랙 칸 채우기 (총 18칸 중 11칸 채움 + 체커기)
        val totalKm = 18
        val doneKm = 11
        drivingBinding.layoutTrackBar.removeAllViews()

        for (i in 0 until totalKm) {
            val cell = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, 36, 1f).apply {
                    setMargins(3, 0, 3, 0)
                }
                setBackgroundColor(
                    if (i < doneKm) ContextCompat.getColor(this@MainActivity, R.color.yellow)
                    else Color.parseColor("#2B2F35")
                )
            }
            drivingBinding.layoutTrackBar.addView(cell)
        }

        // 끝 체커기 깃발 칸
        val flagCell = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(48, 36).apply {
                setMargins(4, 0, 0, 0)
            }
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.white))
        }
        drivingBinding.layoutTrackBar.addView(flagCell)
    }

    // ==========================================
    // 헬퍼: 순위 타워 줄 생성
    // ==========================================
    private fun createTowerRowView(
        rank: Int,
        name: String,
        avg: Double,
        isMe: Boolean = false,
        tagText: String? = null,
        isTarget: Boolean = false,
        isDim: Boolean = false,
        isTop3: Boolean = false
    ): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                if (isMe) 70 else 54
            ).apply { setMargins(0, 3, 0, 3) }

            setPadding(24, 0, 24, 0)

            if (isMe) {
                setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.yellow))
            } else if (isTarget) {
                setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.cell))
            } else {
                setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.cell))
            }
        }

        val rankTv = TextView(this).apply {
            text = "P$rank"
            textSize = if (isMe) 28f else 22f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(
                if (isMe) ContextCompat.getColor(this@MainActivity, R.color.asphalt)
                else if (isTop3) ContextCompat.getColor(this@MainActivity, R.color.gold)
                else ContextCompat.getColor(this@MainActivity, R.color.grey)
            )
            layoutParams = LinearLayout.LayoutParams(100, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val nameTv = TextView(this).apply {
            text = name
            textSize = if (isMe) 24f else 20f
            setTypeface(null, if (isMe) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            setTextColor(
                if (isMe) ContextCompat.getColor(this@MainActivity, R.color.asphalt)
                else if (isDim) ContextCompat.getColor(this@MainActivity, R.color.grey)
                else ContextCompat.getColor(this@MainActivity, R.color.white)
            )
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        row.addView(rankTv)
        row.addView(nameTv)

        if (tagText != null) {
            val tagTv = TextView(this).apply {
                text = tagText
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(14, 4, 14, 4)
                if (tagText == "추월") {
                    setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.red))
                    setTextColor(Color.WHITE)
                } else {
                    setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.yellow))
                    setTextColor(ContextCompat.getColor(this@MainActivity, R.color.asphalt))
                }
            }
            row.addView(tagTv)
        }

        val avgTv = TextView(this).apply {
            text = String.format("%.1f", avg)
            textSize = if (isMe) 26f else 22f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(
                if (isMe) ContextCompat.getColor(this@MainActivity, R.color.asphalt)
                else ContextCompat.getColor(this@MainActivity, R.color.white)
            )
            setPadding(16, 0, 0, 0)
        }
        row.addView(avgTv)

        return row
    }
}
