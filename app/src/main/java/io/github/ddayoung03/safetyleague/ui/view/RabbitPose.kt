package io.github.ddayoung03.safetyleague.ui.view

import io.github.ddayoung03.safetyleague.R
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

internal const val WIN_BOUNCE = 6
internal const val CONTENT_W = 116
internal const val FEET_ROW = 168
internal const val TOP_ROW = 15

/**
 * 토끼 동작표. 프레임·시간·반복은 만들어진 RabbitFrames에서 오고, 여기에는 손으로 맞춘 값만 둔다.
 * [bobHeight]·[bobPeriodMs]는 앱이 그림 전체에 더하는 들썩임으로, 프레임이 적은 동작도 살아 있게 한다.
 * [centerX]는 뷰 가운데에 올 그림의 x다. 팔과 깃발까지 넣은 가운데이고, WIN과 OVERTAKE는 바뀔 때 튀지 않게 같다.
 */
enum class Pose(private val clip: RabbitClip, val bobHeight: Int, val bobPeriodMs: Long, val centerX: Int) {
    IDLE(RabbitFrames.IDLE, 1, 1640, 82),
    WIN(RabbitFrames.WIN, WIN_BOUNCE, RabbitFrames.WIN.durationsMs.sum(), 87), // 한 번 뛸 때 한 번 튄다
    OVERTAKE(RabbitFrames.OVERTAKE, 1, RabbitFrames.OVERTAKE.durationsMs.sum() / 2, 87),
    PIT(RabbitFrames.SIT, 1, 1800, 82), // 아쉬운 날: 타이어에 앉아 피트 보드를 든다
    RADIO(RabbitFrames.RADIO, 1, 880, 79),
    LOAD(RabbitFrames.LOAD, 1, 360, 67),
    WELCOME(RabbitFrames.WELCOME, 1, RabbitFrames.WELCOME.durationsMs.sum(), 76),
    NOD(RabbitFrames.NOD, 1, 1600, 79),
    POINT(RabbitFrames.POINT, 1, 1200, 120),
    CHEER(RabbitFrames.CHEER, 1, 1660, 72),   // 착지 후 두 발로 서서 만세
    COUPON(RabbitFrames.COUPON, 1, 1660, 82), // 쿠폰을 두 손으로 내민다
    BOOK(RabbitFrames.BOOK, 1, 1200, 85),     // 펼친 안내 책을 짚는다
    CLIP(RabbitFrames.CLIP, 1, 1200, 110);    // 클립보드에 기록을 체크한다

    val frames get() = clip.frames
    val durationsMs get() = clip.durationsMs
    val loop get() = clip.loop
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
internal fun spriteScale(widthPx: Int, heightPx: Int, canvasScale: Float, contentW: Int = CONTENT_W, contentH: Int = FEET_ROW - TOP_ROW + WIN_BOUNCE): Int =
    floor(min(widthPx * canvasScale / contentW, heightPx * canvasScale / contentH)).toInt().coerceAtLeast(1)
