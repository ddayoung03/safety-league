package io.github.ddayoung03.safetyleague.ui.view

import androidx.core.graphics.withTranslation

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import io.github.ddayoung03.safetyleague.R

/** Full-height finish ribbon, clipped at the screen edge like the mockup. */
class CheckerFlagView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val paint = Paint()
    private val white = context.getColor(R.color.white)
    private val asphalt = context.getColor(R.color.asphalt)
    override fun onDraw(canvas: Canvas) {
        val d = resources.displayMetrics.density
        val tile = 40 * d
        canvas.withTranslation(width - 60 * d, 0f) {
        canvas.skew(-0.249328f, 0f)
        canvas.translate(height * 0.249328f / 2, 0f)
        canvas.clipRect(0f, 0f, 150 * d, height.toFloat())
        for (y in 0..(height / tile).toInt()) for (x in 0..3) {
            paint.color = if ((x + y) % 2 == 0) white else asphalt
            canvas.drawRect(x * tile, y * tile, (x + 1) * tile, (y + 1) * tile, paint)
        }
        }
    }
}
