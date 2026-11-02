package ai.pleos.safetyleague.core.repository

import ai.pleos.safetyleague.core.calculator.LeagueCalculator
import ai.pleos.safetyleague.core.data.DemoData
import ai.pleos.safetyleague.core.model.CompletedTrip
import ai.pleos.safetyleague.core.model.DriverRank
import ai.pleos.safetyleague.core.model.Trip
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 트립 데이터 및 리그 상태 저장소
 */
class TripRepository {

    private val _trips = mutableListOf<Trip>().apply {
        // 기본 시연용 데모 데이터 로드
        addAll(DemoData.trips)
    }

    private val _latestCompletedTrip = MutableStateFlow<CompletedTrip?>(null)
    val latestCompletedTrip: StateFlow<CompletedTrip?> = _latestCompletedTrip.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    fun setTrackingState(tracking: Boolean) {
        _isTracking.value = tracking
    }

    /**
     * 완료된 트립 저장
     */
    fun saveTrip(completedTrip: CompletedTrip) {
        _latestCompletedTrip.value = completedTrip
        if (completedTrip.isEligible) {
            _trips.add(completedTrip.trip)
        }
    }

    /**
     * 전체 트립 목록 조회
     */
    fun getAllTrips(): List<Trip> = _trips.toList()

    /**
     * 특정 날짜 기준 시즌 누적 평균
     */
    fun getSeasonAverage(untilDate: String): Double {
        return LeagueCalculator.seasonAverage(_trips, DemoData.league.seasonStart, untilDate)
    }

    /**
     * 특정 날짜 기준 연속 안전 주행 일수 (70점 이상)
     */
    fun getStreak(untilDate: String): Int {
        return LeagueCalculator.streakAt(_trips, untilDate)
    }

    /**
     * 순위표 이웃 조회
     */
    fun getNeighbors(date: String): List<DriverRank> {
        val raw = DemoData.rawBoard[date] ?: emptyList()
        val myAvg = getSeasonAverage(date)
        return raw.map { (rank, name, avg) ->
            DriverRank(
                rank = rank,
                name = name,
                avg = if (name == "나") myAvg else avg
            )
        }
    }

    companion object {
        @Volatile
        private var instance: TripRepository? = null

        fun getInstance(): TripRepository {
            return instance ?: synchronized(this) {
                instance ?: TripRepository().also { instance = it }
            }
        }
    }
}
