package io.github.ddayoung03.safetyleague.ui

import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import io.github.ddayoung03.safetyleague.R
import io.github.ddayoung03.safetyleague.core.model.DriverRank
import io.github.ddayoung03.safetyleague.ui.view.SkewDrawable

internal fun avgText(avg: Double) = String.format("%.1f", avg)

/** 타이밍 타워 줄 (시안 D). 연출이 순위 숫자를 바꿀 수 있게 글자 뷰를 함께 돌려준다. */
internal class TimingRow(val view: View, val rank: TextView, val tag: View?, val data: DriverRank)

/** [quiet]는 아쉬운 날: 내 줄이 빛 없이 흰색이다. [target]은 되찾을 순위의 점선 테두리. */
internal fun MainActivity.timingRow(
    r: DriverRank, isMe: Boolean, nameSuffix: String = "", tag: String? = null, dim: Boolean = false,
    target: Boolean = false, quiet: Boolean = false
): TimingRow {
    val density = resources.displayMetrics.density
    val row = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52)).apply { bottomMargin = dp(6) }
        setPadding(dp(18), 0, dp(18), 0)
        background = when {
            target -> SkewDrawable(color(R.color.ink_glow), color(R.color.yellow), density)
            isMe && quiet -> SkewDrawable(color(R.color.ink_text), density = density)
            isMe -> SkewDrawable(color(R.color.yellow), density = density, glow = ColorUtils.setAlphaComponent(color(R.color.yellow), 115))
            else -> SkewDrawable(color(R.color.ink_glow), density = density)
        }
        if (isMe) elevation = dp(2).toFloat() // 추월할 때 다른 줄 위로 지나간다
    }
    val fg = when { isMe -> R.color.ink; dim -> R.color.ink_muted; else -> R.color.ink_text }
    val rank = text("P${r.rank}", if (isMe) R.color.ink else R.color.ink_muted, 30f, number = true).apply {
        layoutParams = LinearLayout.LayoutParams(dp(96), LinearLayout.LayoutParams.WRAP_CONTENT)
        maxLines = 1
    }
    row.addView(rank)
    row.addView(text(r.name + nameSuffix, fg, 22f).apply {
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        maxLines = 1
        ellipsize = android.text.TextUtils.TruncateAt.END
    })
    val tagView = tag?.let { label ->
        text(label, R.color.ink, 18f).apply {
            setPadding(dp(10), dp(2), dp(10), dp(2))
            setBackgroundResource(R.color.yellow)
        }.also(row::addView)
    }
    row.addView(text(avgText(r.avg), fg, 27f, number = true).apply {
        layoutParams = LinearLayout.LayoutParams(dp(76), LinearLayout.LayoutParams.WRAP_CONTENT)
        gravity = Gravity.END
        maxLines = 1
    })
    return TimingRow(row, rank, tagView, r)
}
