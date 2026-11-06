package io.github.ddayoung03.safetyleague.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import io.github.ddayoung03.safetyleague.R
import kotlin.random.Random

/** 왼쪽으로 흐르며 잦아드는 속도선. 시간이 다 되거나, 멈추거나, 창에서 떨어지거나, 보이지 않으면 프레임을 돌리지 않는다. */
class SpeedLinesView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val d = resources.displayMetrics.density
    private val paint = Paint().apply { color = context.getColor(R.color.ink_text) }
    private val x = FloatArray(LINES)
    private val y = FloatArray(LINES)
    private val length = FloatArray(LINES)
    private val speed = FloatArray(LINES)
    private val alphas = IntArray(LINES)
    private var startedAt = 0L
    private var lastFrameAt = 0L
    private var durationMs = 1L
    private var running = false

    fun start(ms: Long) {
        for (i in 0 until LINES) {
            x[i] = Random.nextFloat() * width
            y[i] = height * (0.08f + Random.nextFloat() * 0.78f)
            length[i] = (80 + Random.nextFloat() * 260) * d
            speed[i] = (26 + Random.nextFloat() * 30) * d
            alphas[i] = 13 + Random.nextInt(41)
        }
        durationMs = ms
        startedAt = SystemClock.uptimeMillis()
        lastFrameAt = startedAt
        running = true
        invalidate()
    }

    fun stop() { running = false; invalidate() }

    override fun onDetachedFromWindow() { running = false; super.onDetachedFromWindow() }

    override fun onDraw(canvas: Canvas) {
        if (!running) return
        val now = SystemClock.uptimeMillis()
        val k = 1f - (now - startedAt).toFloat() / durationMs
        if (k <= 0f) { running = false; return }
        val frames = (now - lastFrameAt) / FRAME_MS
        lastFrameAt = now
        for (i in 0 until LINES) {
            x[i] -= speed[i] * k * frames
            if (x[i] + length[i] < 0f) x[i] = width.toFloat()
            paint.alpha = (alphas[i] * k).toInt()
            canvas.drawRect(x[i], y[i], x[i] + length[i] * k, y[i] + 2 * d, paint)
        }
        postInvalidateOnAnimation()
    }

    private companion object {
        const val LINES = 46
        const val FRAME_MS = 16.67f
    }
}
