package ai.pleos.safetyleague.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

/**
 * 레이싱 서킷 연석 띠 뷰 (빨강-흰색 줄무늬 또는 아쉬운 날 회색 줄무늬)
 */
class KerbView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var isMuted: Boolean = false // 아쉬운 날: 회색조 연석
        set(value) {
            field = value
            invalidate()
        }

    private val paint1 = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paint2 = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        if (isMuted) {
            paint1.color = Color.parseColor("#4A4F57")
            paint2.color = Color.parseColor("#9BA1A8")
        } else {
            paint1.color = Color.parseColor("#C8102E") // 레이스 레드
            paint2.color = Color.parseColor("#F2F1EC") // 트랙 화이트
        }

        val stripeWidth = 56f
        val skewOffset = height * 0.58f // -30도 기울기 효과
        var curX = -skewOffset * 2f
        var toggle = false

        while (curX < width + skewOffset * 2f) {
            path.reset()
            path.moveTo(curX + skewOffset, 0f)
            path.lineTo(curX + stripeWidth + skewOffset, 0f)
            path.lineTo(curX + stripeWidth, height.toFloat())
            path.lineTo(curX, height.toFloat())
            path.close()

            canvas.drawPath(path, if (toggle) paint1 else paint2)
            curX += stripeWidth
            toggle = !toggle
        }
    }
}
