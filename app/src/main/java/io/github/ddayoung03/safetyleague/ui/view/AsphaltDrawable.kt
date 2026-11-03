package io.github.ddayoung03.safetyleague.ui.view

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import io.github.ddayoung03.safetyleague.R
import kotlin.random.Random

/** Cached noise tile; the gradient changes only when bounds change. */
class AsphaltDrawable(context: Context, private val subdued: Boolean = false) : Drawable() {
    private val base = context.getColor(R.color.asphalt_deep)
    private val colors = intArrayOf(context.getColor(R.color.asphalt_light), context.getColor(R.color.asphalt), context.getColor(R.color.asphalt_shadow))
    private val paint = Paint()
    private val noise = Paint().apply { shader = BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT) }
    private var gradient: Shader? = null
    override fun onBoundsChange(bounds: Rect) {
        if (bounds.isEmpty) return
        gradient = RadialGradient(0f, 0f, 1f, colors, floatArrayOf(0f, .6f, 1f), Shader.TileMode.CLAMP).apply {
            setLocalMatrix(Matrix().apply {
                setScale(bounds.width() * .7f, bounds.height() * .9f)
                postTranslate(bounds.left + bounds.width() * .25f, bounds.top + bounds.height() * .45f)
            })
        }
    }
    override fun draw(canvas: Canvas) {
        paint.shader = null
        paint.color = base
        paint.alpha = 255
        canvas.drawRect(bounds, paint)
        paint.shader = gradient
        paint.alpha = if (subdued) 128 else 255
        canvas.drawRect(bounds, paint)
        noise.alpha = if (subdued) 128 else 255
        canvas.drawRect(bounds, noise)
    }
    override fun setAlpha(alpha: Int) { /* Opacity is fixed by the screen design. */ }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter; noise.colorFilter = colorFilter; invalidateSelf() }
    @Deprecated("Deprecated in Android")
    override fun getOpacity() = PixelFormat.OPAQUE
    companion object {
        private val tile: Bitmap by lazy {
            val random = Random(42)
            Bitmap.createBitmap(IntArray(240 * 240) { Color.argb(random.nextInt(19), 255, 255, 255) }, 240, 240, Bitmap.Config.ARGB_8888)
        }
    }
}
