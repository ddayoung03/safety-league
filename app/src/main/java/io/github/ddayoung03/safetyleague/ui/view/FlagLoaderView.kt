package io.github.ddayoung03.safetyleague.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import io.github.ddayoung03.safetyleague.R
import io.github.ddayoung03.safetyleague.ui.motion.animationsEnabled
import kotlin.math.cos
import kotlin.math.sin

/** 체커기 세로 흔들림(논리 px). 깃대(x=0)는 0이고 자유단일수록 커지며, 절댓값은 14 이하다. */
internal fun flagWave(x: Float, width: Float, timeSec: Float): Float {
    val k = x / width
    return (k * 11f * sin(x * 0.055f - timeSec * 6.2f) + k * 3f * sin(x * 0.12f - timeSec * 9f)).coerceIn(-14f, 14f)
}

/** 12x8칸 체커기. start()~stop() 사이, 화면에 보일 때만 그린다. 애니메이션이 꺼져 있으면 멈춘 깃발. */
class FlagLoaderView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val pole = context.getColor(R.color.ink_text)
    private var running = false
    private val t0 = SystemClock.uptimeMillis()

    fun start() { running = true; invalidate() }
    fun stop() { running = false }

    override fun onDraw(canvas: Canvas) {
        val sc = minOf(width / W, height / H)
        canvas.scale(sc, sc)
        val t = if (animationsEnabled()) (SystemClock.uptimeMillis() - t0) / 1000f else 0f
        paint.color = pole
        canvas.drawRect(4f, 4f, 9f, H - 2f, paint)
        val cw = (W - 16f) / COLS
        val ch = (H - 34f) / ROWS
        for (i in 0 until COLS) {
            val x0 = i * cw
            val x1 = x0 + cw
            val y0 = flagWave(x0, W - 16f, t)
            val y1 = flagWave(x1, W - 16f, t)
            val sh = 0.72f + 0.28f * cos(x0 * 0.055f - t * 6.2f)
            for (j in 0 until ROWS) {
                val v = (if ((i + j) % 2 == 0) 255f * sh else 34f * (2f - sh)).toInt()
                paint.setARGB(255, v, v, (v * 0.97f).toInt())
                path.rewind()
                path.moveTo(9 + x0, 12 + y0 + j * ch)
                path.lineTo(9 + x1 + 0.6f, 12 + y1 + j * ch)
                path.lineTo(9 + x1 + 0.6f, 12 + y1 + (j + 1) * ch + 0.6f)
                path.lineTo(9 + x0, 12 + y0 + (j + 1) * ch + 0.6f)
                canvas.drawPath(path, paint)
            }
        }
        if (running && animationsEnabled() && isShown && isAttachedToWindow) postInvalidateOnAnimation()
    }

    private companion object {
        const val W = 118f
        const val H = 92f
        const val COLS = 12
        const val ROWS = 8
    }
}
