package io.github.ddayoung03.safetyleague.ui.view

import android.content.Context
import android.graphics.drawable.GradientDrawable
import io.github.ddayoung03.safetyleague.R

class GlassDrawable(context: Context) : GradientDrawable(Orientation.TOP_BOTTOM,
    intArrayOf(context.getColor(R.color.glass_top), context.getColor(R.color.glass_bottom))) {
    init {
        cornerRadius = 6 * context.resources.displayMetrics.density
        setStroke(context.resources.displayMetrics.density.toInt().coerceAtLeast(1), context.getColor(R.color.line))
    }
}
