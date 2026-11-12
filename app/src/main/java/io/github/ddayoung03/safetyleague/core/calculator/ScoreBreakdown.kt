package io.github.ddayoung03.safetyleague.core.calculator

import io.github.ddayoung03.safetyleague.core.model.DetailScore
import io.github.ddayoung03.safetyleague.core.model.ReplayEvent
import io.github.ddayoung03.safetyleague.core.model.TripStats
import kotlin.math.roundToInt

/**
 * 점수 상세: 다섯 항목의 점수와, 그 점수가 나온 이유 한 줄.
 * 이유는 트립 집계값([TripStats])으로 쓴다. 집계값이 없는 옛 트립·시연 카드는 사건 목록으로 대신한다.
 */
object ScoreBreakdown {

    data class Row(val label: String, val got: Double, val max: Double, val note: String)

    fun rows(d: DetailScore, events: List<ReplayEvent>, km: Double, s: TripStats?): List<Row> {
        val accels = s?.hardAccels ?: events.count { it.kind == "accel" }
        val brakes = s?.hardBrakes ?: events.count { it.kind == "brake" }
        val unindicated = s?.unindicatedLaneChanges ?: events.count { it.kind == "lane" }

        val smooth = if (accels + brakes == 0) "급가속·급제동 없음" else
            listOfNotNull("급가속 ${accels}회".takeIf { accels > 0 }, "급제동 ${brakes}회".takeIf { brakes > 0 }).joinToString(" · ") +
                " · ${km}km 주행이라 10km당 ${LeagueCalculator.round1((accels + brakes) * 10 / km)}회꼴로 환산"

        val manner = when {
            unindicated == 0 -> "깜빡이 없는 차선 변경 없음"
            s != null -> "차선 변경 ${s.laneChanges}회 중 ${unindicated}회가 깜빡이 없이"
            else -> "깜빡이 없이 차선 변경 ${unindicated}회"
        }

        val measured = s?.efficiency?.takeIf { it.isFinite() && it > 0 && s.leagueAvgEfficiency > 0 }
        val eco = when {
            s == null -> "같은 차종 평균과 비교한 전비·연비"
            measured == null -> "전비·연비 신호가 없어 평균(10점)으로 계산"
            else -> "같은 차종 평균의 ${(measured / s.leagueAvgEfficiency * 100).roundToInt()}%"
        }

        val safety = when {
            s == null -> if (d.safety >= 15.0) SAFE else "출발 전 벨트 · 경고등 방치 · 주행 중 문 열림에서 감점"
            else -> listOfNotNull(
                "출발할 때 벨트 미착용".takeIf { !s.allBeltsFastened },
                "경고등 ${s.unresolvedWarningSeconds}초 방치".takeIf { s.unresolvedWarningSeconds >= 30 },
                "주행 중 문 열림 ${s.doorOpenWhileMoving}회".takeIf { s.doorOpenWhileMoving > 0 }
            ).joinToString(" · ").ifEmpty { SAFE }
        }

        val consistency = if (s == null) "연속 안전 주행일과 주행거리로 계산"
        else "이전까지 연속 안전 주행 ${s.streakDays}일(10일이면 7점) · 주행 ${km}km(10km면 3점)"

        return listOf(
            Row("부드러움", d.smoothness, 30.0, smooth),
            Row("매너", d.manner, 25.0, manner),
            Row("에코", d.eco, 20.0, eco),
            Row("안전 기본기", d.safety, 15.0, safety),
            Row("꾸준함", d.consistency, 10.0, consistency)
        )
    }

    private const val SAFE = "벨트·경고등·문 모두 이상 없음"
}
