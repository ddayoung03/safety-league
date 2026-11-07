package io.github.ddayoung03.safetyleague.ui.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.withTranslation
import io.github.ddayoung03.safetyleague.R
import io.github.ddayoung03.safetyleague.ui.motion.animationsEnabled

/**
 * 픽셀 토끼. 발끝(그림의 168번째 줄)을 뷰 아래에, 동작의 가운데를 뷰 가운데에 맞추고, 화면 픽셀 기준 정수 배율과 최근접 보간으로 그린다.
 * start()는 움직여도 된다는 허락이고, 화면에 보이는 동안만 돈다. stop()이나 애니메이션 꺼짐이면 첫 프레임에 멈춘다.
 */
class RabbitView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    var pose = Pose.IDLE
        set(value) {
            if (field == value) return
            field = value
            startMs = SystemClock.uptimeMillis()
            update()
        }

    /** 피트 보드의 빈 판에 쓸 말. PIT 동작일 때만 그린다. */
    var word: String? = null
        set(value) {
            field = value
            // 글꼴이 2.6MB라 말판이 필요한 화면에서만 불러온다
            if (value != null && wordPaint.typeface == null) wordPaint.typeface = ResourcesCompat.getFont(context, R.font.sl_sans_black)
            fitWord()
            invalidate()
        }

    private val bitmaps = HashMap<Int, Bitmap>()
    private val paint = Paint().apply { isFilterBitmap = false }
    private val wordPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.yellow)
        textAlign = Paint.Align.CENTER
    }
    private val dst = RectF()
    private var cell = 1f
    private var frame: Bitmap? = null
    private var lift = 0
    private var allowed = false
    private var startMs = 0L
    private val tick = object : Runnable {
        override fun run() {
            update()
            postDelayed(this, TICK_MS)
        }
    }

    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

    fun start() {
        if (allowed) return
        allowed = true
        startMs = SystemClock.uptimeMillis()
        syncLoop()
    }

    fun stop() {
        if (!allowed) return
        allowed = false
        syncLoop()
        update()
    }

    private fun moving() = allowed && animationsEnabled()

    private fun syncLoop() {
        removeCallbacks(tick)
        if (moving() && isAttachedToWindow && isShown) post(tick)
    }

    private fun update() {
        val t = if (moving()) SystemClock.uptimeMillis() - startMs else 0L
        val id = pose.frames[frameIndex(pose, t, pose.frames.size)]
        val next = bitmaps.getOrPut(id) { BitmapFactory.decodeResource(resources, id) }
        val nextLift = bobOffset(pose, t)
        if (next === frame && nextLift == lift) return
        frame = next
        lift = nextLift
        invalidate()
    }

    private fun fitWord() {
        val text = word ?: return
        wordPaint.textSize = BOARD_H * 0.6f * cell
        val room = BOARD_W * 0.88f * cell
        val width = wordPaint.measureText(text)
        if (width > room) wordPaint.textSize *= room / width
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val canvasScale = designScale()
        cell = spriteScale(width, height, canvasScale) / canvasScale
        val y = height - FEET_ROW * cell
        dst.set(0f, y, FRAME_W * cell, y + FRAME_H * cell)
        fitWord()
        update()
    }

    override fun onDraw(canvas: Canvas) {
        val bitmap = frame ?: return
        canvas.withTranslation(width / 2f - pose.centerX * cell, -lift * cell) {
            drawBitmap(bitmap, null, dst, paint)
            val text = word
            if (text != null && pose == Pose.PIT) {
                val baseline = dst.top + BOARD_CENTER_Y * cell - (wordPaint.ascent() + wordPaint.descent()) / 2
                drawText(text, BOARD_CENTER_X * cell, baseline, wordPaint)
            }
        }
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); syncLoop() }
    override fun onDetachedFromWindow() { removeCallbacks(tick); super.onDetachedFromWindow() }
    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        syncLoop()
    }

    private companion object {
        const val TICK_MS = 33L
        const val FRAME_W = 192
        const val FRAME_H = 176
        // rabbit_pit_00의 빈 판: x 65~98, y 114~129
        const val BOARD_W = 34
        const val BOARD_H = 16
        const val BOARD_CENTER_X = 82f
        const val BOARD_CENTER_Y = 122f
    }
}
