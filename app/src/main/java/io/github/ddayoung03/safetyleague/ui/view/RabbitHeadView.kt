package io.github.ddayoung03.safetyleague.ui.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.withClip
import io.github.ddayoung03.safetyleague.R

/** 팀 라디오의 작은 토끼 얼굴. 머리를 가운데 두고 원으로 자른다. 정수 배율과 최근접 보간으로 그린다. */
class RabbitHeadView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val bitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.rabbit_idle_00)
    private val paint = Paint().apply { isFilterBitmap = false }
    private val clip = Path()
    private val dst = RectF()

    init {
        setBackgroundColor(context.getColor(R.color.ink_glow))
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val canvasScale = designScale()
        val cell = spriteScale(width, height, canvasScale, HEAD, HEAD) / canvasScale
        val x = width / 2f - HEAD_X * cell
        val y = height / 2f - HEAD_Y * cell
        dst.set(x, y, x + bitmap.width * cell, y + bitmap.height * cell)
        clip.reset()
        clip.addCircle(width / 2f, height / 2f, minOf(width, height) / 2f, Path.Direction.CW)
    }

    override fun draw(canvas: Canvas) {
        canvas.withClip(clip) { super.draw(canvas) }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawBitmap(bitmap, null, dst, paint)
    }

    private companion object {
        const val HEAD = 44
        const val HEAD_X = 82
        const val HEAD_Y = 84
    }
}
