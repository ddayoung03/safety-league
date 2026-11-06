package io.github.ddayoung03.safetyleague.ui.view

import androidx.core.graphics.ColorUtils
import androidx.core.graphics.withTranslation

import android.content.Context
import io.github.ddayoung03.safetyleague.R
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.util.AttributeSet
import android.view.View
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.FloatValueHolder
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import io.github.ddayoung03.safetyleague.core.calculator.LeagueCalculator
import io.github.ddayoung03.safetyleague.core.data.DemoData
import io.github.ddayoung03.safetyleague.core.model.Point2D
import io.github.ddayoung03.safetyleague.core.model.ReplayEvent
import io.github.ddayoung03.safetyleague.ui.motion.SPRING_DAMPING
import io.github.ddayoung03.safetyleague.ui.motion.SPRING_STIFFNESS
import kotlin.math.min

/** 경로를 [progress]까지 그렸을 때 지나간 사건 수. 출발 전에는 0이다. */
internal fun visibleEvents(eventPositions: List<Float>, progress: Float): Int =
    if (progress <= 0f) 0 else eventPositions.count { it <= progress }

/**
 * 레이스 주행 경로 서킷 및 사건 지점을 렌더링하는 커스텀 뷰
 */
class CircuitRouteView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var routePoints: List<Point2D> = DemoData.route
        set(value) {
            field = value
            rebuild()
        }

    /** 경로를 따라 지나는 순서로 넣는다. 핀은 앞에서부터 [visibleCount]개가 보인다. */
    var replayEvents: List<ReplayEvent> = emptyList()
        set(value) {
            field = value
            pinScale = FloatArray(value.size) { 1f }
            rebuild()
        }

    /** 그려진 길이의 비율(0~1).  */
    var progress: Float = 1f
        set(value) {
            field = value.coerceIn(0f, 1f)
            trim()
        }

    /** 차가 지나간 사건 수. */
    var visibleCount = 0
        private set

    private val route = Path()
    private val drawn = Path()
    private val measure = PathMeasure()
    private val carPos = FloatArray(2)
    private val ends = FloatArray(4) // 출발점과 도착점의 화면 좌표
    private var scale = 1f
    private var eventPositions = emptyList<Float>()
    private var pins = emptyList<Point2D>() // 화면 좌표
    private var pinScale = FloatArray(0)
    private val pops = mutableListOf<SpringAnimation>()

    private val shadowColor = context.getColor(R.color.shadow)
    private val edgeColor = ColorUtils.setAlphaComponent(context.getColor(R.color.ink_text), 40)
    private val asphaltColor = context.getColor(R.color.ink_glow)
    private val inkColor = context.getColor(R.color.ink)
    private val textColor = context.getColor(R.color.ink_text)
    private val yellowColor = context.getColor(R.color.yellow)

    private val roadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val centerLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 3f
        color = ColorUtils.setAlphaComponent(yellowColor, 120) // 차선 노랑
        pathEffect = DashPathEffect(floatArrayOf(12f, 10f), 0f)
    }

    private val eventPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val eventBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = textColor
        strokeWidth = 2.5f
    }

    private val eventIcons = listOf("lane", "brake", "accel").associateWith {
        androidx.appcompat.content.res.AppCompatResources.getDrawable(context, replayEventIcon(it))!!
    }

    /** 사건 [index]의 핀을 스프링으로 튀어나오게 한다. */
    fun popPin(index: Int) {
        pinScale[index] = 0f
        pops += SpringAnimation(FloatValueHolder(0f)).apply {
            spring = SpringForce(1f).setStiffness(SPRING_STIFFNESS).setDampingRatio(SPRING_DAMPING)
            minimumVisibleChange = DynamicAnimation.MIN_VISIBLE_CHANGE_SCALE
            addUpdateListener { _, value, _ -> pinScale[index] = value; invalidate() }
            start()
        }
    }

    /** 경로를 끝까지 그리고 핀을 모두 제 크기로 맞춘다. */
    fun finishReplay() {
        pops.forEach { it.cancel() }
        pops.clear()
        pinScale.fill(1f)
        progress = 1f
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuild()
    }

    /** 경로와 핀 위치를 다시 계산한다. onDraw에서 만들지 않으려고 값이 바뀔 때만 부른다. */
    private fun rebuild() {
        route.reset()
        eventPositions = replayEvents.map { it.t.toFloat() }
        pins = emptyList()
        if (routePoints.size >= 2 && width > 0 && height > 0) {
            val padding = 40f
            val availableW = width - (padding * 2f)
            val availableH = height - (padding * 2f)

            // 100x60 가상 좌표 스케일링
            scale = min(availableW / 100f, availableH / 60f)
            val offsetX = padding + (availableW - (100f * scale)) * 0.5f
            val offsetY = padding + (availableH - (60f * scale)) * 0.5f

            fun toScreen(p: Point2D) = Point2D(offsetX + p.x * scale, offsetY + p.y * scale)

            val screen = routePoints.map(::toScreen)
            route.moveTo(screen.first().x.toFloat(), screen.first().y.toFloat())
            for (i in 1 until screen.size) route.lineTo(screen[i].x.toFloat(), screen[i].y.toFloat())
            ends[0] = screen.first().x.toFloat()
            ends[1] = screen.first().y.toFloat()
            ends[2] = screen.last().x.toFloat()
            ends[3] = screen.last().y.toFloat()
            pins = replayEvents.map { toScreen(LeagueCalculator.pointAt(routePoints, it.t)) }
        }
        measure.setPath(route, false)
        trim()
    }

    /** 그릴 부분을 잘라 둔다. 그 끝이 차의 자리다. */
    private fun trim() {
        visibleCount = visibleEvents(eventPositions, progress)
        val end = measure.length * progress
        drawn.reset()
        measure.getSegment(0f, end, drawn, true)
        measure.getPosTan(end, carPos, null)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (routePoints.size < 2 || width == 0 || height == 0) return
        val density = resources.displayMetrics.density

        // Shadow below the road, never above event labels.
        roadPaint.strokeWidth = 44f
        roadPaint.color = shadowColor
        canvas.withTranslation(0f, 8 * density) {
            drawPath(drawn, roadPaint)
        }
        // 1. 도로 아스팔트 베이스
        roadPaint.strokeWidth = 44f
        roadPaint.color = edgeColor
        canvas.drawPath(drawn, roadPaint)

        roadPaint.strokeWidth = 36f
        roadPaint.color = asphaltColor
        canvas.drawPath(drawn, roadPaint)

        // 중앙선
        canvas.drawPath(drawn, centerLinePaint)

        // 2. 출발점 (그린 서클) & 도착점 (체커기 마커). 도착점은 차가 닿으면 나온다.
        eventPaint.color = textColor // 출발: 트랙 화이트
        canvas.drawCircle(ends[0], ends[1], 9f, eventPaint)
        canvas.drawCircle(ends[0], ends[1], 9f, eventBorderPaint)

        if (progress >= 1f) {
            eventPaint.color = textColor
            canvas.drawCircle(ends[2], ends[3], 10f, eventPaint)
            eventPaint.color = inkColor
            canvas.drawCircle(ends[2], ends[3], 5f, eventPaint)
            canvas.drawCircle(ends[2], ends[3], 10f, eventBorderPaint)
        }

        // 3. 리플레이 이벤트 마커 렌더링
        for (i in 0 until visibleCount) {
            val icon = eventIcons[replayEvents[i].kind] ?: continue
            val radius = 24 * density * pinScale[i]
            val x = pins[i].x.toFloat()
            val y = pins[i].y.toFloat()
            icon.setBounds((x - radius).toInt(), (y - radius).toInt(), (x + radius).toInt(), (y + radius).toInt())
            icon.draw(canvas)
        }

        // 4. 달리는 차
        if (progress > 0f && progress < 1f) {
            eventPaint.color = yellowColor
            canvas.drawCircle(carPos[0], carPos[1], 10 * density, eventPaint)
            canvas.drawCircle(carPos[0], carPos[1], 10 * density, eventBorderPaint)
        }
    }
}
