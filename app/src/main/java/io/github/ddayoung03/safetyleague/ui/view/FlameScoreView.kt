package io.github.ddayoung03.safetyleague.ui.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.random.Random

/**
 * 고득점 시 큰 점수 텍스트 주변에 불꽃이 이글거리는 모션 뷰
 */
class FlameScoreView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val flamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private class FlameParticle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var radius: Float,
        var alpha: Int,
        var color: Int,
        var life: Float,
        var maxLife: Float
    )

    private val particles = mutableListOf<FlameParticle>()
    private val maxParticles = 65
    private var animator: ValueAnimator? = null
    private var glowPulse = 0f

    init {
        startFlameAnimation()
    }

    fun startFlameAnimation() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                glowPulse = it.animatedValue as Float
                updateParticles()
                invalidate()
            }
            start()
        }
    }

    fun stopFlameAnimation() {
        animator?.cancel()
        animator = null
    }

    private fun updateParticles() {
        if (width == 0 || height == 0) return

        // 새 파티클 보충
        val spawnXMin = width * 0.15f
        val spawnXMax = width * 0.85f
        val spawnY = height * 0.88f

        while (particles.size < maxParticles) {
            val px = spawnXMin + Random.nextFloat() * (spawnXMax - spawnXMin)
            val py = spawnY + Random.nextFloat() * (height * 0.1f)
            val pLife = 30f + Random.nextFloat() * 40f
            val pColor = when (Random.nextInt(3)) {
                0 -> Color.argb(220, 255, 197, 46) // 차선 노랑 (#FFC52E)
                1 -> Color.argb(200, 200, 16, 46)  // 레이스 레드 (#C8102E)
                else -> Color.argb(240, 255, 120, 0) // 주황 화염
            }

            particles.add(
                FlameParticle(
                    x = px,
                    y = py,
                    vx = (Random.nextFloat() - 0.5f) * 2.2f,
                    vy = -3.5f - Random.nextFloat() * 5.5f,
                    radius = 8f + Random.nextFloat() * 18f,
                    alpha = 220,
                    color = pColor,
                    life = pLife,
                    maxLife = pLife
                )
            )
        }

        // 기존 파티클 이동 및 소멸
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.x += p.vx
            p.y += p.vy
            p.life -= 1f
            val lifeRatio = p.life / p.maxLife
            p.alpha = (lifeRatio * 220).toInt().coerceIn(0, 255)
            p.radius *= 0.97f

            if (p.life <= 0f || p.y < 0) {
                iterator.remove()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width * 0.5f
        val cy = height * 0.6f
        val baseRadius = (width.coerceAtMost(height) * 0.5f)

        // 1. 하단/배경 화염 글로우 펄스
        val pulseScale = 1.0f + 0.08f * kotlin.math.sin(glowPulse * Math.PI.toFloat() * 2f)
        val glowRadius = baseRadius * pulseScale
        glowPaint.shader = RadialGradient(
            cx, cy, glowRadius,
            intArrayOf(
                Color.argb(70, 255, 160, 20),
                Color.argb(30, 200, 16, 46),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, glowRadius, glowPaint)

        // 2. 불꽃 파티클 렌더링
        for (p in particles) {
            flamePaint.color = p.color
            flamePaint.alpha = p.alpha
            canvas.drawCircle(p.x, p.y, p.radius, flamePaint)
        }
    }

    override fun onDetachedFromWindow() {
        stopFlameAnimation()
        super.onDetachedFromWindow()
    }
}
