package io.github.ddayoung03.safetyleague.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.ddayoung03.safetyleague.core.collector.MockVehicleDataSource
import io.github.ddayoung03.safetyleague.core.collector.PleosVehicleDataSource
import io.github.ddayoung03.safetyleague.core.collector.TripCollector
import io.github.ddayoung03.safetyleague.core.data.DemoData
import io.github.ddayoung03.safetyleague.core.model.CompletedTrip
import io.github.ddayoung03.safetyleague.core.repository.TripRepository
import io.github.ddayoung03.safetyleague.ui.MainActivity
import java.time.LocalDate

/**
 * 주행 중 백그라운드에서 차량 신호를 모아 트립을 기록한다. 화면은 띄우지 않는다.
 * P단에서 트립이 끝나면 저장하고, 앱이 앞에 있으면 [onResult]로 바로, 뒤에 있으면 전체 화면 알림으로 결과 카드를 연다.
 */
class TripTrackingService : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): TripTrackingService = this@TripTrackingService
    }

    private val binder = LocalBinder()
    private lateinit var repository: TripRepository
    private lateinit var collector: TripCollector
    private lateinit var pleos: PleosVehicleDataSource
    private val mock = MockVehicleDataSource()

    /** 앱 화면이 앞에 있을 때 MainActivity가 등록한다. */
    var onResult: ((CompletedTrip) -> Unit)? = null

    /** 차량 SDK 연결 문제 (시연 모드 화면에 표시) */
    val sdkProblems: List<String> get() = pleos.problems.toList()

    /** 시연 모드: 진행 중인 트립의 주행 거리 등을 보여 주는 데 쓴다. */
    val isTracking: Boolean get() = collector.isTracking
    val currentTripKm: Double get() = collector.currentTripKm
    val streakBeforeToday: Int get() = collector.streakDays

    override fun onCreate() {
        super.onCreate()
        repository = TripRepository.getInstance(this)
        createChannels()
        collector = TripCollector(
            date = LocalDate.now().toString(),
            streakDays = repository.getStreak(LocalDate.now().toString()),
            leagueAvgEfficiency = DemoData.leagueAvgEfficiency,
            onTripCompleted = ::onTripFinished
        )
        pleos = PleosVehicleDataSource(this)
        pleos.startListening(collector)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 위치 권한이 있을 때만 location 타입으로 (없이 location 타입이면 API 34에서 SecurityException)
        val canLocate = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (canLocate) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else 0
        ServiceCompat.startForeground(this, TRACKING_ID, trackingNotification(), type)
        return START_STICKY
    }

    /** 시연 모드 전용: 실제 신호를 잠시 끊고 각본대로 달린 뒤 다시 연결한다. */
    fun runDemo(win: Boolean) {
        pleos.stopListening()
        mock.startListening(collector)
        if (win) mock.simulateWinDay(speedMultiplier = 15L) else mock.simulateLowDay(speedMultiplier = 15L)
    }

    private fun onTripFinished(trip: CompletedTrip) {
        repository.saveTrip(trip)
        collector.streakDays = repository.getStreak(LocalDate.now().toString())
        if (mockRunning) {
            mock.stopListening()
            pleos.startListening(collector)
        }
        val shown = onResult
        if (shown != null) shown(trip) else showResultNotification()
    }

    private val mockRunning get() = mock.isRunning

    private fun showResultNotification() {
        val open = PendingIntent.getActivity(
            this, 1,
            Intent(this, MainActivity::class.java)
                .putExtra(EXTRA_SHOW_RESULT_CARD, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(this, RESULT_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("오늘의 레이스 종료")
            .setContentText("결과를 확인해 보세요")
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setFullScreenIntent(open, true)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(RESULT_ID, n)
    }

    override fun onDestroy() {
        pleos.stopListening()
        mock.stopListening()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private fun trackingNotification(): Notification =
        NotificationCompat.Builder(this, TRACKING_CHANNEL)
            .setContentTitle("주행 기록 중")
            .setContentText("결과는 주차하면 공개돼요")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun createChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(TRACKING_CHANNEL, "주행 기록", NotificationManager.IMPORTANCE_LOW))
        nm.createNotificationChannel(NotificationChannel(RESULT_CHANNEL, "주행 결과", NotificationManager.IMPORTANCE_HIGH))
    }

    companion object {
        const val TRACKING_CHANNEL = "safety_league_tracking"
        const val RESULT_CHANNEL = "safety_league_result"
        const val TRACKING_ID = 1001
        const val RESULT_ID = 1002
        const val EXTRA_SHOW_RESULT_CARD = "extra_show_result_card"
    }
}
