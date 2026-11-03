package io.github.ddayoung03.safetyleague.ui.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import kotlin.math.min

/**
 * 화면은 시안과 같은 1280×720dp 캔버스 기준으로 그린다. 실제 창이 더 크면 캔버스를 통째로 키워
 * 시안 비율을 지킨다. 창이 시안보다 세로로 길면(Pleos 앱 영역 약 1690×1210dp) 캔버스 높이를 늘려
 * 남는 높이는 각 화면의 늘어나는 여백(weight)이 채운다. 잘리거나 여백 띠가 생기지 않는다.
 */
class DesignCanvas @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    private val designW = 1280 * resources.displayMetrics.density
    private val designH = 720 * resources.displayMetrics.density
    private var scale = 1f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        scale = min(w / designW, h / designH)
        val cw = MeasureSpec.makeMeasureSpec((w / scale).toInt(), MeasureSpec.EXACTLY)
        val ch = MeasureSpec.makeMeasureSpec((h / scale).toInt(), MeasureSpec.EXACTLY)
        for (i in 0 until childCount) getChildAt(i).measure(cw, ch)
        setMeasuredDimension(w, h)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        for (i in 0 until childCount) {
            val c = getChildAt(i)
            c.layout(0, 0, c.measuredWidth, c.measuredHeight)
            c.pivotX = 0f; c.pivotY = 0f
            c.scaleX = scale; c.scaleY = scale
        }
    }
}
