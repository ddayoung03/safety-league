package io.github.ddayoung03.safetyleague.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import io.github.ddayoung03.safetyleague.R

/** 출발 신호등 5개. 340x76dp로 놓는다. [lit]개만큼 왼쪽부터 켠다. */
class StartLightsView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val d = resources.displayMetrics.density
    private val red = context.getColor(R.color.signal_red)
    private val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.getColor(R.color.ink_glow) }
    private val offPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.getColor(R.color.ink_muted); alpha = 56 }
    private val onPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = red; setShadowLayer(14 * d, 0f, 0f, red) }
    private val panel = RectF()

    var lit = 0
        set(value) { field = value; invalidate() }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) = panel.set(0f, 0f, w.toFloat(), h.toFloat())

    override fun onDraw(canvas: Canvas) {
        canvas.drawRoundRect(panel, 14 * d, 14 * d, panelPaint)
        for (i in 0 until LIGHTS)
            canvas.drawCircle((18 + 24 + i * 64) * d, height / 2f, 24 * d, if (i < lit) onPaint else offPaint)
    }

    private companion object { const val LIGHTS = 5 }
}
