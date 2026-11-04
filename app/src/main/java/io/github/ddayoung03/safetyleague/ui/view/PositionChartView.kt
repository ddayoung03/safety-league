package io.github.ddayoung03.safetyleague.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import io.github.ddayoung03.safetyleague.R

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

    // 순위 숫자는 앱 숫자 글꼴(Saira Condensed 기울임), 요일은 앱 한글 글꼴
    private val numberFont: Typeface? = if (isInEditMode) null else ResourcesCompat.getFont(context, R.font.saira_condensed)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 24f * resources.displayMetrics.scaledDensity
        color = Color.parseColor("#9BA1A8")
    }
    private val rankPaint = Paint(textPaint).apply { typeface = Typeface.create(numberFont, Typeface.ITALIC) }
    private val dayFont: Typeface? = if (isInEditMode) null else ResourcesCompat.getFont(context, R.font.pretendard_bold)

    private val highlightTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 26f * resources.displayMetrics.scaledDensity
        color = Color.parseColor("#FFC52E")
        if (!isInEditMode) typeface = ResourcesCompat.getFont(context, R.font.pretendard_bold)
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
    /** 요일별 순위(월~일)와 승급 라인. MainActivity가 DemoData에서 채운다. */
    var ranks: List<Int> = emptyList()
        set(v) { field = v; invalidate() }
    var promotionLine: Int = 119
        set(v) { field = v; invalidate() }
    private val minRank get() = promotionLine
    private val maxRank get() = (ranks.maxOrNull() ?: promotionLine) + 3

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0 || ranks.size != 7) return

        val paddingLeft = 80f
        val paddingRight = 40f
        val paddingTop = -highlightTextPaint.fontMetrics.ascent + 20f
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
                canvas.drawText("승급 라인", width - paddingRight, gy - 16f, highlightTextPaint)
            } else {
                canvas.drawLine(paddingLeft, gy, width - paddingRight, gy, gridPaint)
                rankPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("P$r", paddingLeft - 16f, gy + 8f, rankPaint)
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
        // 전날보다 가장 많이 오른 날 강조
        val bestJumpIndex = (1 until ranks.size).maxByOrNull { ranks[it - 1] - ranks[it] } ?: 0
        val bestJump = if (bestJumpIndex > 0) ranks[bestJumpIndex - 1] - ranks[bestJumpIndex] else 0
        for (i in days.indices) {
            val px = getX(i)
            val py = getY(ranks[i])

            if (i == bestJumpIndex) {
                // 금요일 최고 상승 강조 (노란 점 + ▲3 텍스트)
                pointPaint.color = Color.parseColor("#FFC52E")
                canvas.drawCircle(px, py, 11f, pointPaint)
                highlightTextPaint.textAlign = Paint.Align.CENTER
                canvas.drawText("+$bestJump", px, py - 20f, highlightTextPaint)
            } else {
                pointPaint.color = Color.parseColor("#24272C")
                canvas.drawCircle(px, py, 8f, pointPaint)
                canvas.drawCircle(px, py, 8f, pointStrokePaint)
            }

            // 하단 요일 라벨
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.typeface = dayFont
            canvas.drawText(days[i], px, height - 16f, textPaint)
        }
    }
}
