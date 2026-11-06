package io.github.ddayoung03.safetyleague.ui

import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import io.github.ddayoung03.safetyleague.R
import io.github.ddayoung03.safetyleague.core.model.DriverRank
import io.github.ddayoung03.safetyleague.ui.view.SkewDrawable

/** 순위 줄 크기 (시안 CSS px = dp). 내 줄은 더 높고 왼쪽으로 튀어나온다. */
internal data class RowSize(val h: Int, val meH: Int, val p: Float, val meP: Float, val n: Float, val s: Float,
                           val meOut: Int, val meTop: Int, val meBottom: Int, val meBg: Int = R.color.yellow)
internal val rowWin = RowSize(64, 78, 36f, 46f, 26f, 34f, 28, 10, 16)
internal val rowLow = RowSize(60, 74, 36f, 44f, 25f, 32f, 28, 10, 10, R.color.white) // 아쉬운 날: 내 줄은 흰색 (시안)
internal val rowLeague = RowSize(50, 62, 32f, 36f, 24f, 28f, 24, 6, 6)

internal fun avgText(avg: Double) = String.format("%.1f", avg)

internal fun MainActivity.towerRow(
    r: DriverRank, isMe: Boolean, nameSuffix: String = "", tag: String? = null,
    target: Boolean = false, dim: Boolean = false, top: Boolean = false, size: RowSize = rowWin
): View {
    val row = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(if (isMe) size.meH else size.h))
            .apply { if (isMe) setMargins(-dp(size.meOut), dp(size.meTop), 0, dp(size.meBottom)) else setMargins(0, 0, 0, dp(6)) }
        setPadding(dp(20), 0, dp(20), 0)
        background = SkewDrawable(color(if (isMe) size.meBg else R.color.cell), density = resources.displayMetrics.density,
            gradientColors = (if (isMe && size.meBg == R.color.yellow) intArrayOf(R.color.yellow_light, R.color.yellow, R.color.yellow_dark)
            else if (isMe) intArrayOf(R.color.metal_top, R.color.white_bottom)
            else intArrayOf(R.color.glass_top, R.color.glass_bottom)).map(::color).toIntArray())
        elevation = dp(if (isMe) 8 else 3).toFloat()
    }
    val fg = when {
        isMe -> R.color.asphalt
        dim -> R.color.grey
        else -> R.color.white
    }
    row.addView(text("P${r.rank}", if (isMe) R.color.asphalt else if (top) R.color.gold else R.color.grey, if (isMe) size.meP else size.p, number = true)
        .apply { layoutParams = LinearLayout.LayoutParams(dp(112), LinearLayout.LayoutParams.WRAP_CONTENT) })
    row.addView(text(r.name + nameSuffix, fg, size.n).apply {
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        maxLines = 1
        ellipsize = android.text.TextUtils.TruncateAt.END
    })
    if (tag != null) row.addView(text(tag, if (tag == "추월") R.color.white else R.color.asphalt).apply {
        setPadding(dp(12), dp(2), dp(12), dp(2))
        setBackgroundResource(if (tag == "추월") R.color.red else R.color.yellow)
    })
    row.addView(text(avgText(r.avg), fg, size.s, number = true).apply { setPadding(dp(16), 0, paddingEnd, 0) })
    if (target) row.background = SkewDrawable(color(R.color.cell), color(R.color.yellow), resources.displayMetrics.density)
    return row
}

/** 타이밍 타워 줄 (시안 D). 연출이 순위 숫자와 평균을 바꿀 수 있게 글자 뷰를 함께 돌려준다. */
internal class TimingRow(val view: View, val rank: TextView, val avg: TextView, val tag: View?, val data: DriverRank)

internal fun MainActivity.timingRow(r: DriverRank, isMe: Boolean, nameSuffix: String = "", tag: String? = null, dim: Boolean = false): TimingRow {
    val density = resources.displayMetrics.density
    val row = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52)).apply { bottomMargin = dp(6) }
        setPadding(dp(18), 0, dp(18), 0)
        background = if (isMe) SkewDrawable(color(R.color.yellow), density = density, glow = ColorUtils.setAlphaComponent(color(R.color.yellow), 115))
            else SkewDrawable(color(R.color.ink_glow), density = density)
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
    val avg = text(avgText(r.avg), fg, 27f, number = true).apply {
        layoutParams = LinearLayout.LayoutParams(dp(76), LinearLayout.LayoutParams.WRAP_CONTENT)
        gravity = Gravity.END
        maxLines = 1
    }
    row.addView(avg)
    return TimingRow(row, rank, avg, tagView, r)
}
