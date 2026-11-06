package io.github.ddayoung03.safetyleague.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import io.github.ddayoung03.safetyleague.R

/** 첫 점부터 마지막 점까지 [progress]만큼 그렸을 때 선 끝의 칸 위치. 출발 전에는 -1이라 첫 점도 보이지 않는다. */
internal fun chartHead(first: Int, last: Int, progress: Float): Float =
    if (progress <= 0f) -1f else first + (last - first) * progress

/**
 * 주간 리포트용 F1식 포지션 순위 변동 차트 뷰
 */
class PositionChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val inkColor = context.getColor(R.color.ink)
    private val textColor = context.getColor(R.color.ink_text)
    private val yellowColor = context.getColor(R.color.yellow)

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = textColor
        strokeCap = Paint.Cap.ROUND
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = context.getColor(R.color.ink_glow)
    }

    private val promoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = yellowColor
        pathEffect = DashPathEffect(floatArrayOf(12f, 10f), 0f)
    }

    // 순위 숫자는 앱 숫자 글꼴(Saira Condensed 기울임), 요일은 앱 한글 글꼴
    private val numberFont: Typeface? = if (isInEditMode) null else ResourcesCompat.getFont(context, R.font.saira_condensed)
    private val rankPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 24f * resources.displayMetrics.scaledDensity
        color = context.getColor(R.color.ink_muted)
        typeface = Typeface.create(numberFont, Typeface.ITALIC)
        textAlign = Paint.Align.RIGHT
    }
    private val textPaint = Paint(rankPaint).apply {
        typeface = if (isInEditMode) null else ResourcesCompat.getFont(context, R.font.pretendard_bold)
        textAlign = Paint.Align.CENTER
    }

    private val highlightTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 26f * resources.displayMetrics.scaledDensity
        color = yellowColor
        if (!isInEditMode) typeface = ResourcesCompat.getFont(context, R.font.pretendard_bold)
        textAlign = Paint.Align.CENTER
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val pointStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = textColor
    }

    /** 가로축 요일. 시연 리포트는 화~월이라 막대와 같은 목록을 넘긴다. */
    var days = listOf("월", "화", "수", "목", "금", "토", "일")
        set(v) { field = v; layoutChart(); invalidate() }
    /** 요일별 순위([days] 순서)와 승급 라인. 순위가 없는 날은 null(실제 모드: 시즌 첫 트립 전, 오늘 이후). */
    var ranks: List<Int?> = emptyList()
        set(v) { field = v; layoutChart(); invalidate() }
    var promotionLine: Int = 119
        set(v) { field = v; layoutChart(); invalidate() }
    /** 순위 선을 왼쪽부터 그린 정도. 1이면 전부. 격자와 요일은 처음부터 보인다. */
    var progress: Float = 1f
        set(v) { field = v; invalidate() }

    // 그리기 값은 데이터나 크기가 바뀔 때만 계산한다. onDraw는 매 프레임 불린다.
    private var ready = false
    private var xs = FloatArray(0)
    private var ys = FloatArray(0)
    private var gridYs = FloatArray(0)
    private var gridLabels = emptyList<String>()
    private var promoY = 0f
    private var firstPoint = 0
    private var lastPoint = 0
    private var jumpIndex = -1
    private var jumpLabel = ""

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        layoutChart()
    }

    private fun layoutChart() {
        val known = ranks.filterNotNull()
        ready = width > 0 && height > 0 && ranks.size == 7 && known.isNotEmpty()
        if (!ready) return

        val minRank = minOf(promotionLine, known.minOrNull() ?: promotionLine)
        val maxRank = maxOf(promotionLine, known.maxOrNull() ?: promotionLine) + 3
        // 칸 간격. 시연 범위(15 이하)는 3, 실제 순위 폭이 크면 줄이 6개 남짓이 되게 넓힌다.
        val step = maxOf(3, ((maxRank - minRank) + 4) / 5)

        val paddingTop = -highlightTextPaint.fontMetrics.ascent + 20f
        val chartW = width - PADDING_LEFT - PADDING_RIGHT
        val chartH = height - paddingTop - PADDING_BOTTOM
        fun getY(rank: Int) = paddingTop + (rank - minRank).toFloat() / (maxRank - minRank).toFloat() * chartH

        xs = FloatArray(days.size) { PADDING_LEFT + it.toFloat() / (days.size - 1).toFloat() * chartW }
        ys = FloatArray(ranks.size) { i -> ranks[i]?.let { getY(it) } ?: 0f }
        // 가로 그리드선 및 순위 라벨 (P122, P125, P128, P131, P134). 승급 라인은 따로 그린다.
        val gridRanks = (minRank..maxRank step step).filter { it != promotionLine }
        gridYs = FloatArray(gridRanks.size) { getY(gridRanks[it]) }
        gridLabels = gridRanks.map { "P$it" }
        promoY = getY(promotionLine)
        firstPoint = ranks.indexOfFirst { it != null }
        lastPoint = ranks.indexOfLast { it != null }

        // 전날보다 가장 많이 오른 날 강조
        fun jump(i: Int) = ranks[i - 1]?.let { a -> ranks[i]?.let { b -> a - b } }
        val best = (1 until ranks.size).filter { jump(it) != null }.maxByOrNull { jump(it)!! }
        val bestJump = best?.let { jump(it) } ?: 0
        jumpIndex = if (bestJump > 0) best!! else -1
        jumpLabel = "+$bestJump"
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!ready) return

        val right = width - PADDING_RIGHT
        for (i in gridYs.indices) {
            canvas.drawLine(PADDING_LEFT, gridYs[i], right, gridYs[i], gridPaint)
            canvas.drawText(gridLabels[i], PADDING_LEFT - 16f, gridYs[i] + 8f, rankPaint)
        }
        // 승급 라인 (노란 점선)
        canvas.drawLine(PADDING_LEFT, promoY, right, promoY, promoPaint)
        highlightTextPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("승급 라인", right, promoY - 16f, highlightTextPaint)
        highlightTextPaint.textAlign = Paint.Align.CENTER

        // 순위 변화 연결선. 순위가 없는 날은 끊긴다.
        val head = chartHead(firstPoint, lastPoint, progress)
        for (i in firstPoint until lastPoint) {
            if (i >= head) break
            if (ranks[i] == null || ranks[i + 1] == null) continue
            val part = minOf(1f, head - i)
            canvas.drawLine(xs[i], ys[i], xs[i] + (xs[i + 1] - xs[i]) * part, ys[i] + (ys[i + 1] - ys[i]) * part, linePaint)
        }

        // 요일별 라벨과 선이 지나간 점
        for (i in days.indices) {
            val px = xs[i]
            canvas.drawText(days[i], px, height - 16f, textPaint)
            if (ranks[i] == null || i > head) continue
            val py = ys[i]
            if (i == jumpIndex) {
                // 최고 상승 강조 (노란 점 + 상승 폭)
                pointPaint.color = yellowColor
                canvas.drawCircle(px, py, 11f, pointPaint)
                canvas.drawText(jumpLabel, px, py - 20f, highlightTextPaint)
            } else {
                pointPaint.color = inkColor
                canvas.drawCircle(px, py, 8f, pointPaint)
                canvas.drawCircle(px, py, 8f, pointStrokePaint)
            }
        }
    }

    private companion object {
        const val PADDING_LEFT = 80f
        const val PADDING_RIGHT = 40f
        const val PADDING_BOTTOM = 60f
    }
}
