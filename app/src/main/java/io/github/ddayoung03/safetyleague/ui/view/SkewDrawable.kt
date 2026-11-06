package io.github.ddayoung03.safetyleague.ui.view

import androidx.core.graphics.withClip

import android.graphics.*
import android.graphics.drawable.Drawable

/** Background only: labels and hit areas stay upright. */
class SkewDrawable(color: Int, private val outline: Int? = null, private val density: Float = 1f, glow: Int? = null) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        if (glow != null) setShadowLayer(15 * density, 0f, 0f, glow)
    }
    private val path = Path()
    override fun onBoundsChange(bounds: Rect) {
        val inset = bounds.height() * 0.176327f // tan(10 degrees)
        path.reset()
        path.moveTo(bounds.left + inset, bounds.top.toFloat())
        path.lineTo(bounds.right.toFloat(), bounds.top.toFloat())
        path.lineTo(bounds.right - inset, bounds.bottom.toFloat())
        path.lineTo(bounds.left.toFloat(), bounds.bottom.toFloat())
        path.close()
    }
    override fun draw(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        canvas.drawPath(path, paint)
        outline?.let {
            val fill = paint.color
            paint.color = it
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3 * density
            paint.pathEffect = DashPathEffect(floatArrayOf(10 * density, 8 * density), 0f)
            canvas.withClip(path) {
                drawPath(path, paint)
            }
            paint.pathEffect = null
            paint.color = fill
        }
    }
    override fun setAlpha(alpha: Int) { paint.alpha = alpha; invalidateSelf() }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter; invalidateSelf() }
    @Deprecated("Deprecated in Android")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}
