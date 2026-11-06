package io.github.ddayoung03.safetyleague.ui.view

import io.github.ddayoung03.safetyleague.R
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

internal const val WIN_BOUNCE = 6
internal const val CONTENT_W = 116

/**
 * 토끼 동작표. 프레임과 시간은 design/rabbit-forge/frames.json 값이다(똑같은 그림은 한 장을 다시 쓴다).
 * [bobHeight]·[bobPeriodMs]는 앱이 그림 전체에 더하는 들썩임으로, 프레임이 적은 동작도 살아 있게 한다.
 * [centerX]는 뷰 가운데에 올 그림의 x다. 팔과 깃발까지 넣은 가운데이고, WIN과 OVERTAKE는 바뀔 때 튀지 않게 같다.
 */
enum class Pose(
    val frames: List<Int>,
    val durationsMs: List<Long>,
    val loop: Boolean,
    val bobHeight: Int,
    val bobPeriodMs: Long,
    val centerX: Int
) {
    IDLE(listOf(R.drawable.rabbit_idle_00, R.drawable.rabbit_idle_00, R.drawable.rabbit_idle_00, R.drawable.rabbit_idle_03),
        listOf(500, 500, 500, 140), true, 1, 1640, 82),
    WIN(listOf(R.drawable.rabbit_win_00, R.drawable.rabbit_win_01, R.drawable.rabbit_win_01, R.drawable.rabbit_win_03),
        listOf(140, 220, 220, 180), true, WIN_BOUNCE, 760, 88),
    OVERTAKE(listOf(R.drawable.rabbit_overtake_00, R.drawable.rabbit_overtake_01, R.drawable.rabbit_overtake_02, R.drawable.rabbit_overtake_03),
        listOf(85, 85, 85, 85), true, 1, 170, 88),
    PIT(listOf(R.drawable.rabbit_pit_00),
        listOf(800), true, 1, 2000, 82),
    RADIO(listOf(R.drawable.rabbit_radio_00, R.drawable.rabbit_radio_01),
        listOf(220, 220), true, 1, 880, 79),
    LOAD(listOf(R.drawable.rabbit_load_00, R.drawable.rabbit_load_01, R.drawable.rabbit_load_02, R.drawable.rabbit_load_03, R.drawable.rabbit_load_04, R.drawable.rabbit_load_05, R.drawable.rabbit_load_06, R.drawable.rabbit_load_07),
        listOf(90, 90, 90, 90, 90, 90, 90, 90), true, 1, 360, 67),
    WELCOME(listOf(R.drawable.rabbit_welcome_00, R.drawable.rabbit_welcome_01, R.drawable.rabbit_welcome_02, R.drawable.rabbit_welcome_03, R.drawable.rabbit_welcome_02, R.drawable.rabbit_welcome_01),
        listOf(110, 110, 110, 110, 110, 110), true, 1, 660, 76),
    NOD(listOf(R.drawable.rabbit_nod_00, R.drawable.rabbit_nod_01),
        listOf(300, 300), true, 1, 1600, 97),
    POINT(listOf(R.drawable.rabbit_point_00),
        listOf(800), true, 1, 1200, 120)
}

/** [timeMs]에 보일 프레임. 반복하지 않는 동작은 마지막 프레임에서 멈춘다. */
fun frameIndex(pose: Pose, timeMs: Long, frameCount: Int, loop: Boolean = pose.loop): Int {
    var total = 0L
    for (i in 0 until frameCount) total += pose.durationsMs[i]
    if (!loop && timeMs >= total) return frameCount - 1
    var left = timeMs % total
    for (i in 0 until frameCount) {
        left -= pose.durationsMs[i]
        if (left < 0) return i
    }
    return frameCount - 1
}

/** 그림을 위로 올릴 칸 수. 칸은 스프라이트 픽셀이라 흐려지지 않는다. */
fun bobOffset(pose: Pose, timeMs: Long): Int {
    val phase = (timeMs % pose.bobPeriodMs).toFloat() / pose.bobPeriodMs
    return (pose.bobHeight * sin(PI * phase)).roundToInt()
}

/** 스프라이트 한 칸이 차지할 화면 픽셀 수. 모든 동작이 [contentW]x[contentH]칸 안에 든다. 뷰 크기에 [canvasScale]을 곱해야 화면 픽셀이다. */
internal fun spriteScale(widthPx: Int, heightPx: Int, canvasScale: Float, contentW: Int = CONTENT_W, contentH: Int = 144 + WIN_BOUNCE): Int =
    floor(min(widthPx * canvasScale / contentW, heightPx * canvasScale / contentH)).toInt().coerceAtLeast(1)
