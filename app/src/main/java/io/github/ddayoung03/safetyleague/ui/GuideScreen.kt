package io.github.ddayoung03.safetyleague.ui

import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import io.github.ddayoung03.safetyleague.R
import io.github.ddayoung03.safetyleague.core.data.GuideCard
import io.github.ddayoung03.safetyleague.core.data.GuideCategory
import io.github.ddayoung03.safetyleague.core.data.SafetyGuide
import io.github.ddayoung03.safetyleague.databinding.ViewGuideBinding
import io.github.ddayoung03.safetyleague.ui.view.Pose

internal fun guideIcon(id: String) = when (id) {
    "quake" -> R.drawable.guide_quake
    "flood" -> R.drawable.guide_flood
    "tunnel" -> R.drawable.guide_tunnel
    "breakdown" -> R.drawable.guide_breakdown
    "rain" -> R.drawable.guide_rain
    "snow" -> R.drawable.guide_snow
    "fog" -> R.drawable.guide_fog
    "school" -> R.drawable.guide_school
    "right" -> R.drawable.guide_right
    "drowsy" -> R.drawable.guide_drowsy
    "signal" -> R.drawable.guide_signal
    "lane" -> R.drawable.guide_lane
    "distance" -> R.drawable.guide_distance
    "belt" -> R.drawable.guide_belt
    "crosswalk" -> R.drawable.guide_crosswalk
    else -> 0
}

internal fun MainActivity.showGuide() {
    val b = ViewGuideBinding.inflate(layoutInflater, binding.screenContainer, true)
    b.rabbit.pose = Pose.BOOK
    b.checkerStrip.background = checkerTile()
    b.btnGuideBack.background = skew(R.color.ink_glow)
    b.btnGuideBack.pressFeedback()
    b.btnGuideBack.setOnClickListener { show(Screen.HOME) }
    for (category in GuideCategory.entries) {
        val tab = TextView(this).apply {
            text = category.title
            textSize = 22f
            gravity = Gravity.CENTER
            setPadding(dp(28), 0, dp(28), 0)
            isClickable = true
            isFocusable = true
            pressFeedback()
            setOnClickListener {
                guideCategory = category
                guideCardId = null
                paintGuide(b)
            }
        }
        b.guideTabs.addView(tab, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(76)).apply { marginStart = dp(8) })
    }
    paintGuide(b)
}

private fun MainActivity.paintGuide(b: ViewGuideBinding) {
    GuideCategory.entries.forEachIndexed { i, category ->
        val tab = b.guideTabs.getChildAt(i) as TextView
        val on = category == guideCategory
        tab.background = skew(if (on) R.color.yellow else R.color.ink_glow)
        tab.setTextColor(color(if (on) R.color.ink else R.color.ink_muted))
    }
    val cards = SafetyGuide.of(guideCategory, currentWeather)
    val card = cards.firstOrNull { it.id == guideCardId } ?: cards.first()
    b.layoutCards.removeAllViews()
    cards.forEach { b.layoutCards.addView(cardRow(it, it == card, b)) }
    b.ivGuideIcon.setImageResource(guideIcon(card.id))
    b.tvGuideTitle.text = card.title
    b.tvGuideScore.text = card.score?.let { "지키면 $it 올라요" }.orEmpty()
    b.tvGuideScore.visibility = if (card.score == null) View.GONE else View.VISIBLE
    b.layoutSteps.removeAllViews()
    card.steps.forEachIndexed { i, step -> b.layoutSteps.addView(stepRow(i + 1, step)) }
    b.tvGuideSource.text = "출처: ${card.source}"
}

private fun MainActivity.cardRow(card: GuideCard, selected: Boolean, b: ViewGuideBinding): View {
    val row = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(20), dp(8), dp(16), dp(8))
        background = skew(if (selected) R.color.yellow else R.color.ink_glow)
        isClickable = true
        isFocusable = true
        contentDescription = card.title
        pressFeedback()
        setOnClickListener {
            guideCardId = card.id
            paintGuide(b)
        }
    }
    row.addView(ImageView(this).apply { setImageResource(guideIcon(card.id)) }, LinearLayout.LayoutParams(dp(60), dp(60)).apply { marginEnd = dp(16) })
    row.addView(text(card.title, if (selected) R.color.ink else R.color.ink_text, 24f))
    row.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(84)).apply { bottomMargin = dp(10) }
    return row
}

private fun MainActivity.stepRow(number: Int, step: String): View {
    val row = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(0, dp(9), 0, dp(9))
    }
    row.addView(text("$number", R.color.yellow, 30f, number = true), LinearLayout.LayoutParams(dp(44), LinearLayout.LayoutParams.WRAP_CONTENT))
    row.addView(text(step, R.color.ink_text, 26f), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
    return row
}
