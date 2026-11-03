package io.github.ddayoung03.safetyleague.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

/**
 * 주간 리포트용 F1식 포지션 순위 변동 차트 뷰
 */
class PositionChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.parseColor("#F2F1EC")
        strokeJoin = Paint.Join.ROUND
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor("#3A3E45")
    }

    private val promoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.parseColor("#FFC52E")
        pathEffect = DashPathEffect(floatArrayOf(12f, 10f), 0f)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 24f
        color = Color.parseColor("#9BA1A8")
    }

    private val highlightTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 26f
        color = Color.parseColor("#FFC52E")
        textAlign = Paint.Align.CENTER
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val pointStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.parseColor("#F2F1EC")
    }

    // 11.03 ~ 11.09 요일별 데이터
    private val days = listOf("월", "화", "수", "목", "금", "토", "일")
    private val ranks = listOf(131, 130, 130, 128, 125, 126, 125)
    private val promotionLine = 119
    private val minRank = 119
    private val maxRank = 134

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        val paddingLeft = 80f
        val paddingRight = 40f
        val paddingTop = 40f
        val paddingBottom = 60f

        val chartW = width - paddingLeft - paddingRight
        val chartH = height - paddingTop - paddingBottom

        fun getY(rank: Int): Float {
            val ratio = (rank - minRank).toFloat() / (maxRank - minRank).toFloat()
            return paddingTop + (ratio * chartH)
        }

        fun getX(index: Int): Float {
            return paddingLeft + (index.toFloat() / (days.size - 1).toFloat()) * chartW
        }

        // 1. 가로 그리드선 및 순위 라벨 (P119, P122, P125, P128, P131, P134)
        for (r in minRank..maxRank step 3) {
            val gy = getY(r)
            if (r == promotionLine) {
                // 승급 라인 (노란 점선)
                canvas.drawLine(paddingLeft, gy, width - paddingRight, gy, promoPaint)
                highlightTextPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("승급 라인 P119", width - paddingRight, gy - 8f, highlightTextPaint)
            } else {
                canvas.drawLine(paddingLeft, gy, width - paddingRight, gy, gridPaint)
                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("P$r", paddingLeft - 16f, gy + 8f, textPaint)
            }
        }

        // 2. 순위 변화 연결선 (Polyline)
        val path = Path()
        path.moveTo(getX(0), getY(ranks[0]))
        for (i in 1 until ranks.size) {
            path.lineTo(getX(i), getY(ranks[i]))
        }
        canvas.drawPath(path, linePaint)

        // 3. 요일별 점 및 텍스트 렌더링
        val bestJumpIndex = 4 // 금요일 (P128 -> P125 ▲3)
        for (i in days.indices) {
            val px = getX(i)
            val py = getY(ranks[i])

            if (i == bestJumpIndex) {
                // 금요일 최고 상승 강조 (노란 점 + ▲3 텍스트)
                pointPaint.color = Color.parseColor("#FFC52E")
                canvas.drawCircle(px, py, 11f, pointPaint)
                highlightTextPaint.textAlign = Paint.Align.CENTER
                canvas.drawText("+3", px, py - 20f, highlightTextPaint)
            } else {
                pointPaint.color = Color.parseColor("#24272C")
                canvas.drawCircle(px, py, 8f, pointPaint)
                canvas.drawCircle(px, py, 8f, pointStrokePaint)
            }

            // 하단 요일 라벨
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(days[i], px, height - 16f, textPaint)
        }
    }
}
