package io.github.ddayoung03.safetyleague.core.collector

import io.github.ddayoung03.safetyleague.core.model.Point2D

/** 위치 출처 하나 (FusedLocation, NaviHelper, LocationManager). 점은 (경도, 위도). */
interface LocationSource {
    val name: String
    /** 시작할 수 없으면 false. 시작 후 동작 중 실패는 [onFailed]로 알린다. */
    fun start(onPoint: (Point2D) -> Unit, onFailed: (String) -> Unit): Boolean
    fun stop()
}

/**
 * 앞에서부터 쓸 수 있는 위치 출처 하나만 켠다. 켜진 출처가 동작 중 실패하면 끄고 다음 출처로 넘어간다.
 * Pleos 콘솔에서 FusedLocation이 "준비 중"이라 실제 차량에서 안 될 수 있어서 둔 대체 순서다.
 */
class LocationFallback(
    private val sources: List<LocationSource>,
    /** 실패 기록을 남길 목록. 데이터 소스의 problems를 넘기면 화면 상태에 바로 드러난다. */
    val problems: MutableList<String> = mutableListOf(),
    private val onPoint: (Point2D) -> Unit
) {
    private var index = -1
    val activeName: String? get() = sources.getOrNull(index)?.name

    fun start() = startFrom(0)

    private fun startFrom(from: Int) {
        for (i in from until sources.size) {
            val s = sources[i]
            index = i
            val ok = runCatching { s.start(onPoint) { reason -> failed(i, reason) } }.getOrElse { false }
            if (ok || index != i) return // index != i: 시작 도중 실패해서 이미 다음 출처로 넘어감
        }
        index = -1
        problems += "Location: 쓸 수 있는 위치 출처 없음"
    }

    private fun failed(i: Int, reason: String) {
        if (i != index) return // 이미 꺼진 출처의 늦은 실패는 무시
        problems += "${sources[i].name}: $reason"
        runCatching { sources[i].stop() }
        startFrom(i + 1)
    }

    fun stop() {
        sources.getOrNull(index)?.let { runCatching { it.stop() } }
        index = -1
    }
}
