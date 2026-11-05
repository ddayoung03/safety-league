package io.github.ddayoung03.safetyleague.ui

import io.github.ddayoung03.safetyleague.core.model.ReplayEvent
import kotlin.math.roundToInt

internal data class Pit(val tip: String, val reason: String, val gain: String, val word: String? = null)

internal fun pitBoard(events: List<ReplayEvent>, detail: io.github.ddayoung03.safetyleague.core.model.DetailScore?, demo: Boolean): Pit {
    if (!demo && detail != null) return io.github.ddayoung03.safetyleague.core.calculator.PitBoard.pick(detail, events).let { Pit(it.tip, it.reason, it.gain, it.word) }
    val order = listOf("lane", "brake", "accel")
    val kind = order.maxByOrNull { k -> events.count { it.kind == k } * 10 - order.indexOf(k) }
    val n = events.count { it.kind == kind }
    // 세부 점수가 없으면(옛 트립) 숫자 없이 둔다
    fun gain(label: String, max: Double, got: Double?) = got?.let { "$label +${(max - it).roundToInt()}점 가능" } ?: ""
    return when {
        n == 0 -> Pit("출발 전 모든 좌석 벨트 확인하기", "오늘은 큰 실수 없이 달렸어요", "")
        kind == "lane" -> Pit("차선 바꿀 때 깜빡이부터 켜기", "오늘 깜빡이 없이 차선 변경 ${n}회", gain("매너", 25.0, detail?.manner))
        kind == "brake" -> Pit("멈출 때는 미리, 천천히 밟기", "오늘 급제동 ${n}회", gain("부드러움", 30.0, detail?.smoothness))
        else -> Pit("출발은 부드럽게", "오늘 급가속 ${n}회", gain("부드러움", 30.0, detail?.smoothness))
    }
}
