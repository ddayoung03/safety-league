package ai.pleos.safetyleague.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import ai.pleos.safetyleague.R
import ai.pleos.safetyleague.core.collector.MockVehicleDataSource
import ai.pleos.safetyleague.core.collector.PleosVehicleDataSource
import ai.pleos.safetyleague.core.collector.TripCollector
import ai.pleos.safetyleague.core.collector.VehicleDataSource
import ai.pleos.safetyleague.core.model.CompletedTrip
import ai.pleos.safetyleague.core.repository.TripRepository
import ai.pleos.safetyleague.ui.MainActivity

/**
 * 주행 중 백그라운드에서 차량 신호를 수집하고 트립을 기록하는 서비스
 * - CarUxRestrictions 준수: 주행 중 점수 화면 노출 금지, 백그라운드 수집만 진행
 * - GEAR_SELECTION = P 변경 시 트립 자동 완료 및 결과 카드 트리거
 */
class TripTrackingService : Service() {

    private val binder = LocalBinder()
    private val repository = TripRepository.getInstance()

    private lateinit var collector: TripCollector
    private lateinit var dataSource: VehicleDataSource
    val mockDataSource = MockVehicleDataSource()

    inner class LocalBinder : Binder() {
        fun getService(): TripTrackingService = this@TripTrackingService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        collector = TripCollector(
            streakDays = repository.getStreak("2026-11-07"),
            efficiencyRatio = 1.0,
            onTripCompleted = { completedTrip ->
                onTripFinished(completedTrip)
            }
        )

        // 기본 데이터 소스로 PleosVehicleDataSource 연결
        dataSource = PleosVehicleDataSource(this)
        dataSource.startListening(collector)
        mockDataSource.startListening(collector) // 시연/에뮬레이터용 모의 소스도 병행 리스닝
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildTrackingNotification()
        startForeground(NOTIFICATION_ID, notification)
        repository.setTrackingState(true)
        return START_STICKY
    }

    private fun onTripFinished(completedTrip: CompletedTrip) {
        repository.saveTrip(completedTrip)
        repository.setTrackingState(false)

        // P단 주차 시 결과 카드 표시를 위한 인텐트 전송
        val resultIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_SHOW_RESULT_CARD, true)
        }
        startActivity(resultIntent)
    }

    override fun onDestroy() {
        dataSource.stopListening()
        mockDataSource.stopListening()
        repository.setTrackingState(false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private fun buildTrackingNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("오늘의 레이스 진행 중")
            .setContentText("결과는 주차하면 공개돼요")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "세이프티 리그 주행 기록",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "주행 중 안전운전 데이터 백그라운드 수집 알림"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "safety_league_tracking_channel"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_SHOW_RESULT_CARD = "extra_show_result_card"
    }
}
