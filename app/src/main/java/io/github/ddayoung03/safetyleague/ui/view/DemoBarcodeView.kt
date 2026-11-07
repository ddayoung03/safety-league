package io.github.ddayoung03.safetyleague.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import io.github.ddayoung03.safetyleague.R
import java.util.Random

/** 쿠폰 id마다 같은 막대 폭. 진짜 코드가 아니라 그림이다. */
internal fun barcodeBars(id: String, count: Int = 40): List<Int> {
    val random = Random(id.hashCode().toLong())
    return List(count) { 1 + random.nextInt(4) }
}

/** 쓸 수 있는 코드로 보이지 않게 막대 위에 시연용 표시를 겹친 가짜 바코드. */
class DemoBarcodeView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    var couponId = ""
        set(value) { field = value; invalidate() }

    private val bar = Paint().apply { color = context.getColor(R.color.ink_text) }
    private val plate = Paint().apply { color = context.getColor(R.color.yellow) }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.ink)
        textAlign = Paint.Align.CENTER
        typeface = ResourcesCompat.getFont(context, R.font.sl_sans_bold)
    }

    override fun onDraw(canvas: Canvas) {
        val bars = barcodeBars(couponId)
        val unit = width / (bars.sum() * 2f)
        var x = 0f
        for (w in bars) {
            canvas.drawRect(x, 0f, x + w * unit, height.toFloat(), bar)
            x += w * unit * 2
        }
        label.textSize = height * .34f
        val half = label.measureText(DEMO) / 2 + height * .16f
        val cy = height / 2f
        canvas.drawRect(width / 2f - half, cy - height * .27f, width / 2f + half, cy + height * .27f, plate)
        canvas.drawText(DEMO, width / 2f, cy - (label.ascent() + label.descent()) / 2, label)
    }

    private companion object {
        const val DEMO = "시연용"
    }
}
