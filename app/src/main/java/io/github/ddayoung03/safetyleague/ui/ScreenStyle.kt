package io.github.ddayoung03.safetyleague.ui

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.StateListAnimator
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import io.github.ddayoung03.safetyleague.R
import io.github.ddayoung03.safetyleague.ui.motion.MotionTokens
import io.github.ddayoung03.safetyleague.ui.view.SkewDrawable
import kotlin.math.roundToInt

/** 티어를 나타내는 안전벨트 그림. 모르는 티어는 브론즈로 본다. */
internal fun tierBelt(tier: String) = when (tier) {
    "실버" -> R.drawable.tier_belt_silver
    "골드" -> R.drawable.tier_belt_gold
    "플래티넘" -> R.drawable.tier_belt_platinum
    "다이아" -> R.drawable.tier_belt_diamond
    else -> R.drawable.tier_belt_bronze
}

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

internal fun MainActivity.skew(colorId: Int) = SkewDrawable(color(colorId), density = resources.displayMetrics.density)

internal fun MainActivity.note(s: String) = text(s, R.color.yellow).apply { setPadding(dp(12), dp(4), 0, dp(10)) }

internal fun MainActivity.dots() = text("⋯", R.color.ink_muted, 19f).apply { gravity = Gravity.CENTER }

internal fun MainActivity.zoneLine(s: String, colorId: Int) = text("─── $s ───", colorId, 19f).apply {
    gravity = Gravity.CENTER
    setPadding(0, dp(2), 0, dp(6))
}

internal fun MainActivity.applySkew(view: View) {
    val shade = (view.background as? ColorDrawable)?.color
    if (view.tag == "skew" && shade != null) view.background = SkewDrawable(shade, density = resources.displayMetrics.density)
    if (view is ViewGroup) for (i in 0 until view.childCount) applySkew(view.getChildAt(i))
}

/** 기울어진 바탕과 눌림. 머티리얼 버튼은 스타일의 틴트가 새 바탕을 덮으므로 틴트를 지운다. */
internal fun Button.skewButton(colorId: Int) {
    background = SkewDrawable(context.getColor(colorId), density = resources.displayMetrics.density)
    backgroundTintList = null
    pressFeedback(.96f)
}

/** 누르는 동안 살짝 작아진다. 자리는 움직이지 않는다. */
internal fun View.pressFeedback(to: Float = .97f) {
    stateListAnimator = StateListAnimator().apply {
        addState(intArrayOf(android.R.attr.state_pressed), pressScale(to))
        addState(intArrayOf(), pressScale(1f))
    }
}

private fun View.pressScale(to: Float) = ObjectAnimator.ofPropertyValuesHolder(this,
    PropertyValuesHolder.ofFloat(View.SCALE_X, to), PropertyValuesHolder.ofFloat(View.SCALE_Y, to)).setDuration(MotionTokens.FAST)
