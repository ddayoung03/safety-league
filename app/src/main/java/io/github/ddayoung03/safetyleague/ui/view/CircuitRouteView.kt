package io.github.ddayoung03.safetyleague.ui.view

import android.content.Context
import io.github.ddayoung03.safetyleague.R
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import io.github.ddayoung03.safetyleague.core.calculator.LeagueCalculator
import io.github.ddayoung03.safetyleague.core.data.DemoData
import io.github.ddayoung03.safetyleague.core.model.Point2D
import io.github.ddayoung03.safetyleague.core.model.ReplayEvent
import kotlin.math.min

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
            invalidate()
        }

    var replayEvents: List<ReplayEvent> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    var isBackgroundMode: Boolean = false // 결과 카드 배경용 흐린 모드
        set(value) {
            field = value
            invalidate()
        }

    private val roadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val centerLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val eventPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val eventBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = 2.5f
    }

    private val eventIcons = listOf("lane", "brake", "accel").associateWith {
        context.getDrawable(replayEventIcon(it))!!
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (routePoints.size < 2 || width == 0 || height == 0) return

        val padding = 40f
        val availableW = width - (padding * 2f)
        val availableH = height - (padding * 2f)

        // 100x60 가상 좌표 스케일링
        val scaleX = availableW / 100f
        val scaleY = availableH / 60f
        val scale = min(scaleX, scaleY)

        val offsetX = padding + (availableW - (100f * scale)) * 0.5f
        val offsetY = padding + (availableH - (60f * scale)) * 0.5f

        fun toScreen(p: Point2D): Pair<Float, Float> {
            return Pair(offsetX + (p.x.toFloat() * scale), offsetY + (p.y.toFloat() * scale))
        }

        val path = Path()
        val first = toScreen(routePoints.first())
        path.moveTo(first.first, first.second)

        for (i in 1 until routePoints.size) {
            val pt = toScreen(routePoints[i])
            path.lineTo(pt.first, pt.second)
        }

        // Shadow below the road, never above event labels.
        roadPaint.strokeWidth = if (isBackgroundMode) 36f * (scale / 6f).coerceAtLeast(1f) else 44f
        roadPaint.color = context.getColor(R.color.shadow)
        canvas.save()
        canvas.translate(0f, 8 * resources.displayMetrics.density)
        canvas.drawPath(path, roadPaint)
        canvas.restore()
        // 1. 도로 아스팔트 베이스
        if (isBackgroundMode) {
            roadPaint.strokeWidth = 36f * (scale / 6f).coerceAtLeast(1f)
            roadPaint.color = Color.argb(30, 242, 241, 236) // 흐린 서킷
            canvas.drawPath(path, roadPaint)
        } else {
            roadPaint.strokeWidth = 44f
            roadPaint.color = Color.argb(40, 255, 255, 255)
            canvas.drawPath(path, roadPaint)

            roadPaint.strokeWidth = 36f
            roadPaint.color = Color.parseColor("#2E3238")
            canvas.drawPath(path, roadPaint)

            // 중앙선
            centerLinePaint.strokeWidth = 3f
            centerLinePaint.color = Color.argb(120, 255, 197, 46) // 차선 노랑
            centerLinePaint.pathEffect = DashPathEffect(floatArrayOf(12f, 10f), 0f)
            canvas.drawPath(path, centerLinePaint)
        }

        // 2. 출발점 (그린 서클) & 도착점 (체커기 마커)
        val startPt = toScreen(routePoints.first())
        val endPt = toScreen(routePoints.last())

        eventPaint.color = Color.parseColor("#F2F1EC") // 출발: 트랙 화이트
        canvas.drawCircle(startPt.first, startPt.second, 9f, eventPaint)
        canvas.drawCircle(startPt.first, startPt.second, 9f, eventBorderPaint)

        eventPaint.color = Color.WHITE
        canvas.drawCircle(endPt.first, endPt.second, 10f, eventPaint)
        eventPaint.color = Color.parseColor("#24272C")
        canvas.drawCircle(endPt.first, endPt.second, 5f, eventPaint)
        canvas.drawCircle(endPt.first, endPt.second, 10f, eventBorderPaint)

        // 3. 리플레이 이벤트 마커 렌더링
        if (!isBackgroundMode && replayEvents.isNotEmpty()) {
            for (ev in replayEvents) {
                val pt = LeagueCalculator.pointAt(routePoints, ev.t)
                val scr = toScreen(pt)

                val icon = eventIcons[ev.kind] ?: continue
                val radius = 24 * resources.displayMetrics.density
                icon.setBounds((scr.first - radius).toInt(), (scr.second - radius).toInt(),
                    (scr.first + radius).toInt(), (scr.second + radius).toInt())
                icon.draw(canvas)
            }
        }
    }
}
