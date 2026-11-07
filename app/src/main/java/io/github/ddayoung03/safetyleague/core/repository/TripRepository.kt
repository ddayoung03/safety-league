package io.github.ddayoung03.safetyleague.core.repository

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import io.github.ddayoung03.safetyleague.core.model.CompletedTrip
import io.github.ddayoung03.safetyleague.core.model.DetailScore
import io.github.ddayoung03.safetyleague.core.model.Point2D
import io.github.ddayoung03.safetyleague.core.model.ReplayEvent
import io.github.ddayoung03.safetyleague.core.model.Trip
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 트립 저장소. 실제 주행 트립은 앱 내부 파일(trips.json)에 저장해 재시작 후에도 남는다.
 * 경로는 이 파일에만 있고 밖으로 보내지 않는다. 리그(다른 운전자)는 시연용 가상 데이터.
 */
class TripRepository private constructor(private val file: File) {

    private val realTrips = mutableListOf<CompletedTrip>()

    private val _latestCompletedTrip = MutableStateFlow<CompletedTrip?>(null)
    val latestCompletedTrip: StateFlow<CompletedTrip?> = _latestCompletedTrip.asStateFlow()

    init {
        load()
        _latestCompletedTrip.value = realTrips.lastOrNull()
    }

    @Synchronized fun saveTrip(completed: CompletedTrip) {
        realTrips += completed
        _latestCompletedTrip.value = completed
        persist()
    }

    /** 차종을 정하기 전에 달린 트립을 그 차종 리그에 넣는다. */
    @Synchronized fun assignUnassignedTrips(leagueKey: String) {
        realTrips.replaceAll { TripQueries.withLeague(it, leagueKey) }
        _latestCompletedTrip.value = realTrips.lastOrNull()
        persist()
    }

    /** 점수 집계에는 실제 유효 트립만 사용한다. */
    @Synchronized fun getAllTrips(leagueKey: String? = null): List<Trip> = TripQueries.eligible(realTrips, leagueKey)

    @Synchronized fun getStreak(untilDate: String): Int = TripQueries.streak(realTrips, untilDate)

    /** 쓰는 중에 시동이 꺼져도 이전 기록이 남도록 임시 파일에 쓴 뒤 바꿔치기한다. 저장 실패로 앱이 죽지 않게 한다. */
    private fun persist() {
        val arr = JSONArray()
        realTrips.forEach { arr.put(toJson(it)) }
        val store = AtomicFile(file)
        val out = runCatching { store.startWrite() }.getOrElse { Log.w(TAG, "트립 저장 실패", it); return }
        runCatching { out.write(arr.toString().toByteArray()); store.finishWrite(out) }
            .onFailure { store.failWrite(out); Log.w(TAG, "트립 저장 실패", it) }
    }

    private fun load() {
        val store = AtomicFile(file)
        runCatching {
            val arr = JSONArray(String(store.readFully()))
            for (i in 0 until arr.length()) realTrips += fromJson(arr.getJSONObject(i))
        }.onFailure {
            if (it is java.io.FileNotFoundException) return
            // 깨진 파일을 다음 저장이 덮어쓰기 전에 남겨 둔다
            Log.w(TAG, "트립 파일을 읽지 못함", it)
            runCatching { file.copyTo(File(file.parent, "trips.broken.json"), overwrite = true) }
        }
    }

    private fun toJson(c: CompletedTrip) = JSONObject().apply {
        put("date", c.trip.date); put("score", c.trip.score); put("km", c.trip.km)
        c.trip.detailScore?.let { d ->
            put("detail", JSONArray(listOf(d.smoothness, d.manner, d.eco, d.safety, d.consistency)))
        }
        put("events", JSONArray(c.events.map { JSONObject().put("at", it.at).put("kind", it.kind).put("t", it.t) }))
        put("route", JSONArray(c.route.map { JSONArray(listOf(it.x, it.y)) }))
        put("start", c.startTime); put("end", c.endTime); put("eligible", c.isEligible)
        c.leagueKey?.let { put("leagueKey", it) }
    }

    private fun fromJson(o: JSONObject): CompletedTrip {
        val d = o.optJSONArray("detail")?.let { a -> DetailScore(a.getDouble(0), a.getDouble(1), a.getDouble(2), a.getDouble(3), a.getDouble(4)) }
        val ev = o.getJSONArray("events").let { a -> (0 until a.length()).map { a.getJSONObject(it) } }
            .map { ReplayEvent(it.getString("at"), it.getString("kind"), it.getDouble("t")) }
        val route = o.getJSONArray("route").let { a -> (0 until a.length()).map { a.getJSONArray(it) } }
            .map { Point2D(it.getDouble(0), it.getDouble(1)) }
        return CompletedTrip(
            Trip(o.getString("date"), o.getDouble("score"), o.getDouble("km"), d),
            ev, route, o.getString("start"), o.getString("end"), o.getBoolean("eligible"),
            if (o.has("leagueKey")) o.getString("leagueKey") else null
        )
    }

    companion object {
        private const val TAG = "TripRepository"

        @Volatile
        private var instance: TripRepository? = null

        fun getInstance(context: Context): TripRepository =
            instance ?: synchronized(this) {
                instance ?: TripRepository(File(context.applicationContext.filesDir, "trips.json")).also { instance = it }
            }
    }
}
