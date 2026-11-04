package io.github.ddayoung03.safetyleague.ui.view

import androidx.core.graphics.withClip

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import io.github.ddayoung03.safetyleague.R

class CrewFaceView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : AppCompatImageView(context, attrs) {
    private val clip = Path()
    init {
        setImageResource(R.drawable.crew_face)
        setBackgroundColor(context.getColor(R.color.cell))
        scaleType = ScaleType.FIT_CENTER
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        clip.reset()
        clip.addCircle(w / 2f, h / 2f, minOf(w, h) / 2f, Path.Direction.CW)
    }
    override fun draw(canvas: Canvas) {
        canvas.withClip(clip) {
            super.draw(canvas)
        }
    }
}
