package io.github.ddayoung03.safetyleague.ui.view

import android.content.Context
import android.graphics.LinearGradient
import android.graphics.Shader
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import io.github.ddayoung03.safetyleague.R

/** Gradient follows the glyph height, including the sharp middle reflection. */
class MetalTextView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    AppCompatTextView(context, attrs) {
    var dull = false
        set(value) { field = value; updateMetal(); invalidate() }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateMetal()
    }

    private fun updateMetal() {
        if (height == 0) return
        val ids = if (dull) intArrayOf(R.color.dull_top, R.color.dull_upper, R.color.dull_edge, R.color.dull_bottom)
            else intArrayOf(R.color.metal_top, R.color.metal_upper, R.color.metal_edge, R.color.metal_bottom)
        val top = baseline + paint.fontMetrics.ascent
        paint.shader = LinearGradient(0f, top, 0f, baseline.toFloat(), ids.map(context::getColor).toIntArray(),
            floatArrayOf(0f, .46f, .52f, 1f), Shader.TileMode.CLAMP)
        setShadowLayer(maxOf(12 * resources.displayMetrics.density, textSize * .2f), 0f, 8 * resources.displayMetrics.density, context.getColor(R.color.shadow))
    }
}
