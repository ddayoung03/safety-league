package io.github.ddayoung03.safetyleague.ui.view

import androidx.core.graphics.withTranslation

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import io.github.ddayoung03.safetyleague.R

/** Static kilometre steps only. No animator or per-frame callbacks. */
class DrivingTrackView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    var state = DrivingTrackState.from(0.0, null)
        set(value) { if (field != value) { field = value; invalidate() } }
    private val d = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val yellow = context.getColor(R.color.yellow)
    private val empty = context.getColor(R.color.track_empty)
    private val grey = context.getColor(R.color.grey)
    private val line = context.getColor(R.color.track_qualifying)
    private val car = ContextCompat.getDrawable(context, R.drawable.ic_track_car)!!
    override fun onDraw(canvas: Canvas) {
        val h = 44 * d
        val skew = h * .249328f
        val finish = if (state.hasFinish) 66 * d else 0f
        val gap = state.cellGap(width - skew - finish, 6 * d)
        val cell = (width - skew - finish - gap * (state.cells - 1)) / state.cells
        val step = cell + gap
        paint.color = line
        canvas.drawRect(0f, 0f, skew + 3 * step - gap, 3 * d, paint)
        canvas.withTranslation(skew, 36 * d) {
        canvas.skew(-.249328f, 0f)
        for (i in 0 until state.cells) {
            paint.color = if (i < state.filled) yellow else empty
            paint.alpha = if (i < state.filled) 204 else 255
            canvas.drawRect(i * step, 0f, i * step + cell, h, paint)
        }
        paint.alpha = 255
        if (state.hasFinish) {
            canvas.withTranslation(state.cells * step, 0f) {
            canvas.clipRect(0f, 0f, 60 * d, h)
            // CSS 22px repeating-conic tile contains four 11px squares.
            val tile = 11 * d
            for (y in 0..3) for (x in 0..5) {
                paint.color = if ((x + y) % 2 == 0) grey else empty
                canvas.drawRect(x * tile, y * tile, (x + 1) * tile, (y + 1) * tile, paint)
            }
            }
        }
        }
        val end = if (state.filled == 0) skew else skew + state.filled * step - gap
        val left = (end - 40 * d).coerceIn(0f, (width - 64 * d).coerceAtLeast(0f)).toInt()
        car.setBounds(left, (4 * d).toInt(), left + (64 * d).toInt(), (34 * d).toInt())
        car.draw(canvas)
    }
}
