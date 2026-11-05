package io.github.ddayoung03.safetyleague.core.calculator

import io.github.ddayoung03.safetyleague.core.model.DetailScore
import io.github.ddayoung03.safetyleague.core.model.ReplayEvent
import kotlin.math.roundToInt

/**
 * 피트 보드: 이번 트립에서 가장 많이 잃은 점수 하나와 그 원인만 보여 준다.
 * 꾸준함(연속 기록·거리)은 이번 트립의 운전 습관으로 고칠 수 있는 것이 아니라 고르지 않는다.
 */
object PitBoard {

    data class Advice(val word: String, val tip: String, val reason: String, val gain: String)

    fun pick(detail: DetailScore, events: List<ReplayEvent>): Advice {
        val accels = events.count { it.kind == "accel" }
        val brakes = events.count { it.kind == "brake" }
        val lanes = events.count { it.kind == "lane" }

        // 부드러움 감점을 급가속·급제동 몫으로 나눈다 (10km당 계수 비율, 0점 아래로 잘린 경우도 같은 비율)
        val smoothLost = 30.0 - detail.smoothness
        val accelRaw = accels * ScoreCalculator.ACCEL_PENALTY_PER_10KM
        val brakeRaw = brakes * ScoreCalculator.BRAKE_PENALTY_PER_10KM
        val rawSum = accelRaw + brakeRaw
        val accelLost = if (rawSum > 0) smoothLost * accelRaw / rawSum else 0.0
        val brakeLost = if (rawSum > 0) smoothLost * brakeRaw / rawSum else 0.0

        // 같은 감점이면 앞의 것을 고른다
        val candidates = listOf(
            "lane" to 25.0 - detail.manner,
            "brake" to brakeLost,
            "accel" to accelLost,
            "safety" to 15.0 - detail.safety,
            "eco" to 20.0 - detail.eco
        )
        var (kind, lost) = candidates.first()
        for ((k, v) in candidates) if (v > lost) { kind = k; lost = v }

        val points = lost.roundToInt()
        if (points <= 0) return Advice("좋아요!", "지금처럼 차분하게 달리기", "오늘은 큰 실수 없이 달렸어요", "")
        return when (kind) {
            "lane" -> Advice("깜빡이!", "차선 바꿀 때 깜빡이부터 켜기", "오늘 깜빡이 없이 차선 변경 ${lanes}회", "매너 +${points}점 가능")
            "brake" -> Advice("천천히!", "멈출 때는 미리, 천천히 밟기", "오늘 급제동 ${brakes}회", "부드러움 +${points}점 가능")
            "accel" -> Advice("부드럽게!", "출발은 부드럽게", "오늘 급가속 ${accels}회", "부드러움 +${points}점 가능")
            "safety" -> Advice("기본기!", "출발 전 벨트, 주행 중 경고등 바로 확인하기", "오늘 안전 기본기 ${detail.safety}점 (15점 만점)", "안전 기본기 +${points}점 가능")
            else -> Advice("에코!", "일정한 속도로 부드럽게 달리기", "오늘 에코 ${detail.eco}점 (20점 만점)", "에코 +${points}점 가능")
        }
    }
}
