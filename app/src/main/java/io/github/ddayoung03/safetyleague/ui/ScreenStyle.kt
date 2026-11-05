package io.github.ddayoung03.safetyleague.ui

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import io.github.ddayoung03.safetyleague.R
import io.github.ddayoung03.safetyleague.ui.view.GlassDrawable
import io.github.ddayoung03.safetyleague.ui.view.SkewDrawable
import kotlin.math.roundToInt

internal fun MainActivity.dp(v: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).roundToInt()
internal fun MainActivity.dp(v: Float) = dp(v.roundToInt())
internal fun MainActivity.color(id: Int) = ContextCompat.getColor(this, id)

internal fun MainActivity.text(s: String, colorId: Int, sizeSp: Float = 24f, number: Boolean = false) = TextView(this).apply {
    text = s
    setTextColor(color(colorId))
    textSize = sizeSp
    if (number) {
        setTypeface(numberFont, Typeface.ITALIC)
        // 기울임(글꼴을 비틀어 그림)은 오른쪽 끝이 글자 칸 밖으로 나온다(7의 윗획이 가장 많이).
        // TextView는 패딩 안쪽으로 잘라 그리므로 여백만으로는 안 되고, 투명 그림자 반경만큼 그리는 범위를 넓힌다.
        val overhang = dp(maxOf(4f, sizeSp * 0.2f))
        setPadding(0, 0, overhang, 0)
        setShadowLayer(overhang.toFloat(), 0f, 0f, Color.TRANSPARENT)
    }
}

internal fun MainActivity.note(s: String) = text(s, R.color.yellow).apply { setPadding(dp(12), dp(4), 0, dp(10)) }

internal fun MainActivity.dots() = text("⋯", R.color.grey).apply { gravity = Gravity.CENTER }

internal fun MainActivity.zoneLine(s: String, colorId: Int) = text("─── $s ───", colorId).apply {
    gravity = Gravity.CENTER
    setPadding(0, dp(6), 0, dp(6))
}

internal fun MainActivity.applySkew(view: View) {
    (view.background as? ColorDrawable)?.let { original ->
        val shade = original.color
        if (view.tag == "skew") {
            val gradient = when (shade) {
                color(R.color.yellow) -> intArrayOf(color(R.color.yellow_light), shade, color(R.color.yellow_dark))
                color(R.color.white) -> intArrayOf(color(R.color.metal_top), color(R.color.white_bottom))
                color(R.color.cell) -> intArrayOf(color(R.color.glass_top), color(R.color.glass_bottom))
                else -> null
            }
            view.background = SkewDrawable(shade, density = resources.displayMetrics.density, gradientColors = gradient)
        } else if (shade == color(R.color.cell)) {
            view.background = GlassDrawable(this)
            view.elevation = dp(3).toFloat()
        }
    }
    if (view is ViewGroup) for (i in 0 until view.childCount) applySkew(view.getChildAt(i))
}
