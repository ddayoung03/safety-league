package io.github.ddayoung03.safetyleague.ui.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.random.Random

/**
 * 낮은 점수 시 점수 텍스트 위로 비가 내리는 모션 뷰
 */
class RainScoreView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val rainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private class RainDrop(
        var x: Float,
        var y: Float,
        var vy: Float,
        var length: Float,
        var strokeWidth: Float,
        var alpha: Int
    )

    private val drops = mutableListOf<RainDrop>()
    private val maxDrops = 50
    private var animator: ValueAnimator? = null

    init {
        startRainAnimation()
    }

    fun startRainAnimation() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                updateDrops()
                invalidate()
            }
            start()
        }
    }

    fun stopRainAnimation() {
        animator?.cancel()
        animator = null
    }

    private fun updateDrops() {
        if (width == 0 || height == 0) return

        // 새 빗방울 생성
        while (drops.size < maxDrops) {
            val rx = Random.nextFloat() * width
            val ry = -Random.nextFloat() * 50f
            val rvy = 12f + Random.nextFloat() * 14f
            val rLen = 14f + Random.nextFloat() * 26f
            val rWidth = 1.8f + Random.nextFloat() * 2.2f
            val rAlpha = 90 + Random.nextInt(120)

            drops.add(RainDrop(rx, ry, rvy, rLen, rWidth, rAlpha))
        }

        // 기존 빗방울 낙하
        val iterator = drops.iterator()
        while (iterator.hasNext()) {
            val d = iterator.next()
            d.y += d.vy
            d.x -= d.vy * 0.12f // 빗줄기가 약간 기울어져 내림 (바람 효과)

            if (d.y > height + 20f || d.x < -20f) {
                iterator.remove()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (d in drops) {
            rainPaint.color = Color.rgb(155, 161, 168) // #9BA1A8 (회색)
            rainPaint.alpha = d.alpha
            rainPaint.strokeWidth = d.strokeWidth
            val endX = d.x - d.length * 0.12f
            val endY = d.y + d.length
            canvas.drawLine(d.x, d.y, endX, endY, rainPaint)
        }
    }

    override fun onDetachedFromWindow() {
        stopRainAnimation()
        super.onDetachedFromWindow()
    }
}
