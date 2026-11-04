package io.github.ddayoung03.safetyleague.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import io.github.ddayoung03.safetyleague.R
import io.github.ddayoung03.safetyleague.weather.Weather
import io.github.ddayoung03.safetyleague.weather.shouldAnimateWeather
import kotlin.random.Random

/** Fixed seeded drawing while moving; only parking or a bounded transition schedules frames. */
class WeatherAmbientView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val moon = Path().apply {
        addCircle(1100f, 100f, 34f, Path.Direction.CW)
        op(Path().apply { addCircle(1116f, 88f, 30f, Path.Direction.CW) }, Path.Op.DIFFERENCE)
    }
    private val random = Random(42)
    private val dots = List(140) { floatArrayOf(random.nextFloat(), random.nextFloat(), random.nextFloat()) }
    private var weather = Weather.SUN
    private var still = false
    private var phase = 0f
    private var transitionStart: Long? = null
    private var owner: LifecycleOwner? = null
    private var tint: Shader? = null
    private val observer = object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) = schedule()
        override fun onPause(owner: LifecycleOwner) = freeze()
    }
    private val frame = object : Runnable {
        override fun run() {
            if (!canAnimate()) return
            if (still) phase += .65f
            if (transitionStart?.let { SystemClock.uptimeMillis() - it >= 1000 } == true) transitionStart = null
            invalidate()
            if (canAnimate()) postDelayed(this, 34)
        }
    }

    fun setState(value: Weather, allowMotion: Boolean, transition: Boolean) {
        val changed = value != weather
        if (still && !allowMotion) freeze()
        still = allowMotion
        if (changed) {
            weather = value
            transitionStart = if (transition && isShown && owner?.lifecycle?.currentState == Lifecycle.State.RESUMED)
                SystemClock.uptimeMillis() else null
            updateTint()
        }
        invalidate()
        schedule()
    }

    private fun canAnimate() = shouldAnimateWeather(
        isShown && isAttachedToWindow, owner?.lifecycle?.currentState == Lifecycle.State.RESUMED,
        still, transitionStart != null, weather)

    private fun schedule() {
        removeCallbacks(frame)
        if (canAnimate()) postDelayed(frame, 34)
    }

    private fun freeze() {
        removeCallbacks(frame)
        transitionStart = null
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        owner = findViewTreeLifecycleOwner()
        owner?.lifecycle?.addObserver(observer)
        schedule()
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        if (isVisible) schedule() else freeze()
    }

    override fun onDetachedFromWindow() {
        freeze()
        owner?.lifecycle?.removeObserver(observer)
        owner = null
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) = updateTint()

    private fun updateTint() {
        if (height == 0) return
        val color = context.getColor(when (weather) {
            Weather.SUN -> R.color.weather_sun
            Weather.RAIN -> R.color.weather_rain
            Weather.SNOW -> R.color.weather_snow
            Weather.NIGHT -> R.color.weather_night
        })
        tint = LinearGradient(0f, 0f, 0f, height * .65f, color, context.getColor(R.color.clear), Shader.TileMode.CLAMP)
    }

    override fun onDraw(canvas: Canvas) {
        val d = resources.displayMetrics.density
        val fade = transitionStart?.let { ((SystemClock.uptimeMillis() - it) / 1000f).coerceIn(0f, 1f) } ?: 1f
        paint.shader = tint
        paint.alpha = ((if (weather == Weather.NIGHT) 120 else 35) * fade).toInt()
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null
        val saved = canvas.save()
        // Match actual XML title, recording label and bottom track, including taller AAOS windows.
        canvas.clipOutRect(50 * d, 40 * d, 650 * d, 120 * d)
        canvas.clipOutRect(50 * d, 150 * d, 1120 * d, 350 * d)
        canvas.clipOutRect(50 * d, height - 300 * d, width - 50 * d, height - 40 * d)
        paint.color = context.getColor(if (weather == Weather.RAIN) R.color.weather_rain else R.color.white)
        paint.strokeWidth = 2 * d
        for ((i, dot) in dots.withIndex()) {
            val x = dot[0] * width
            val y = (dot[1] * height + phase * d * if (weather == Weather.NIGHT) .1f else 1f) % height.coerceAtLeast(1)
            paint.alpha = ((35 + dot[2] * 60) * fade).toInt()
            when (weather) {
                Weather.RAIN -> canvas.drawLine(x, y, x - 8 * d, y + (18 + dot[2] * 26) * d, paint)
                Weather.SNOW -> if (i < 110) canvas.drawCircle(x, y, (1.5f + dot[2] * 3.5f) * d, paint)
                Weather.NIGHT -> if (i < 40 && x > width * .5f && y < height * .35f) canvas.drawCircle(x, y, (1 + dot[2]) * d, paint)
                Weather.SUN -> Unit
            }
        }
        if (weather == Weather.NIGHT || weather == Weather.SUN) {
            paint.color = context.getColor(if (weather == Weather.SUN) R.color.yellow else R.color.white)
            paint.alpha = ((if (weather == Weather.SUN) 25 else 140) * fade).toInt()
            if (weather == Weather.SUN) {
                canvas.drawCircle(width * .88f, -10 * d, 70 * d, paint)
            } else {
                canvas.scale(width / 1280f, height / 720f)
                canvas.drawPath(moon, paint)
            }
        }
        canvas.restoreToCount(saved)
    }
}
