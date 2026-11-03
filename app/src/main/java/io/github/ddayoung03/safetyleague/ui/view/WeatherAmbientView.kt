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

enum class WeatherType {
    SUNNY,
    RAINY,
    CLOUDY,
    SNOWY
}

/**
 * 주행 중 화면용 실시간 날씨 앰비언트 애니메이션 뷰
 * - CarUxRestrictions 준수: 운전자 시야 방해를 막기 위해 15~20% 은은한 알파로 렌더링
 */
class WeatherAmbientView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var weatherType: WeatherType = WeatherType.SUNNY
        set(value) {
            field = value
            particles.clear()
            invalidate()
        }

    private val weatherPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private class WeatherParticle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var size: Float,
        var alpha: Int
    )

    private val particles = mutableListOf<WeatherParticle>()
    private var animator: ValueAnimator? = null
    private var animProgress = 0f

    init {
        startWeatherAnimation()
    }

    fun startWeatherAnimation() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                animProgress = it.animatedValue as Float
                updateWeather()
                invalidate()
            }
            start()
        }
    }

    private fun updateWeather() {
        if (width == 0 || height == 0) return

        when (weatherType) {
            WeatherType.RAINY -> {
                while (particles.size < 40) {
                    particles.add(
                        WeatherParticle(
                            x = Random.nextFloat() * (width + 100f),
                            y = -Random.nextFloat() * 40f,
                            vx = -2.5f,
                            vy = 12f + Random.nextFloat() * 10f,
                            size = 18f + Random.nextFloat() * 20f,
                            alpha = 45 + Random.nextInt(60) // 차분한 투명도
                        )
                    )
                }
                val it = particles.iterator()
                while (it.hasNext()) {
                    val p = it.next()
                    p.x += p.vx
                    p.y += p.vy
                    if (p.y > height + 30f || p.x < -30f) it.remove()
                }
            }
            WeatherType.SNOWY -> {
                while (particles.size < 35) {
                    particles.add(
                        WeatherParticle(
                            x = Random.nextFloat() * width,
                            y = -Random.nextFloat() * 30f,
                            vx = (Random.nextFloat() - 0.5f) * 1.5f,
                            vy = 1.5f + Random.nextFloat() * 2.5f,
                            size = 3.5f + Random.nextFloat() * 5.5f,
                            alpha = 50 + Random.nextInt(70)
                        )
                    )
                }
                val it = particles.iterator()
                while (it.hasNext()) {
                    val p = it.next()
                    p.x += p.vx + (kotlin.math.sin(p.y * 0.05f) * 0.8f)
                    p.y += p.vy
                    if (p.y > height + 20f) it.remove()
                }
            }
            WeatherType.SUNNY, WeatherType.CLOUDY -> {
                particles.clear()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        when (weatherType) {
            WeatherType.SUNNY -> {
                // 우상단 은은한 햇살 앰비언트 글로우
                val sunX = width * 0.85f
                val sunY = height * 0.15f
                val pulse = 1.0f + 0.05f * kotlin.math.sin(animProgress * Math.PI.toFloat() * 2f)
                val radius = (width * 0.45f) * pulse

                sunPaint.shader = RadialGradient(
                    sunX, sunY, radius,
                    intArrayOf(
                        Color.argb(45, 255, 210, 80),
                        Color.argb(15, 255, 197, 46),
                        Color.TRANSPARENT
                    ),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawCircle(sunX, sunY, radius, sunPaint)
            }
            WeatherType.RAINY -> {
                weatherPaint.style = Paint.Style.STROKE
                weatherPaint.strokeCap = Paint.Cap.ROUND
                weatherPaint.strokeWidth = 2.0f
                weatherPaint.color = Color.rgb(180, 195, 210)
                for (p in particles) {
                    weatherPaint.alpha = p.alpha
                    canvas.drawLine(p.x, p.y, p.x + p.vx, p.y + p.size, weatherPaint)
                }
            }
            WeatherType.SNOWY -> {
                weatherPaint.style = Paint.Style.FILL
                weatherPaint.color = Color.WHITE
                for (p in particles) {
                    weatherPaint.alpha = p.alpha
                    canvas.drawCircle(p.x, p.y, p.size, weatherPaint)
                }
            }
            WeatherType.CLOUDY -> {
                // 은은하고 차분한 구름 틴트
                canvas.drawColor(Color.argb(15, 120, 130, 140))
            }
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }
}
