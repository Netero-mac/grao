package com.netero.grao.film

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/** Aplica o visual do filme na foto final (depois de tirar). */
object FilmProcessor {

    private const val MAX_SIDE = 2400

    private val leakColors = intArrayOf(
        Color.rgb(255, 106, 42),
        Color.rgb(232, 64, 42),
        Color.rgb(255, 179, 71),
    )

    fun process(source: Bitmap, film: Film, dateOn: Boolean, seed: Long = System.nanoTime()): Bitmap {
        val rnd = Random(seed)
        var img = downscale(source)
        if (film.polaroidFrame) img = cropSquare(img)

        val w = img.width
        val h = img.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        val colorPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(film.matrix)
        }
        canvas.drawBitmap(img, 0f, 0f, colorPaint)

        if (rnd.nextFloat() < film.leakChance) drawLightLeak(canvas, w, h, rnd)
        drawVignette(canvas, w, h, film.vignette)
        drawGrain(canvas, w, h, film.grain, rnd)
        if (dateOn) drawDate(canvas, w, h)

        return if (film.polaroidFrame) addPolaroidFrame(out) else out
    }

    fun rotate(b: Bitmap, degrees: Int, mirror: Boolean): Bitmap {
        if (degrees == 0 && !mirror) return b
        val m = Matrix().apply {
            postRotate(degrees.toFloat())
            if (mirror) postScale(-1f, 1f)
        }
        return Bitmap.createBitmap(b, 0, 0, b.width, b.height, m, true)
    }

    /** Textura de ruído cinza usada como grão (também no visor ao vivo). */
    fun noiseTile(size: Int = 256, seed: Long = 7L): Bitmap {
        val rnd = Random(seed)
        val px = IntArray(size * size) {
            val v = rnd.nextInt(256)
            Color.rgb(v, v, v)
        }
        return Bitmap.createBitmap(px, size, size, Bitmap.Config.ARGB_8888)
    }

    fun dateText(time: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("''yy MM dd", Locale.US).format(Date(time))

    private fun drawLightLeak(c: Canvas, w: Int, h: Int, rnd: Random) {
        val color = leakColors[rnd.nextInt(leakColors.size)]
        val cx = if (rnd.nextBoolean()) -0.1f * w else 1.1f * w
        val cy = h * rnd.nextFloat()
        val radius = 0.75f * max(w, h)
        val mid = Color.argb(110, Color.red(color), Color.green(color), Color.blue(color))
        val transparent = Color.argb(0, Color.red(color), Color.green(color), Color.blue(color))
        val paint = Paint().apply {
            shader = RadialGradient(
                cx, cy, radius,
                intArrayOf(color, mid, transparent),
                floatArrayOf(0f, 0.35f, 1f),
                Shader.TileMode.CLAMP,
            )
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            alpha = 200
        }
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
    }

    private fun drawVignette(c: Canvas, w: Int, h: Int, strength: Float) {
        if (strength <= 0f) return
        val r = hypot(w.toFloat(), h.toFloat()) / 2f
        val edge = Color.argb((strength * 230).toInt().coerceIn(0, 255), 0, 0, 0)
        val paint = Paint().apply {
            shader = RadialGradient(
                w / 2f, h / 2f, r,
                intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, edge),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
    }

    private fun drawGrain(c: Canvas, w: Int, h: Int, amount: Float, rnd: Random) {
        if (amount <= 0f) return
        val tile = noiseTile(256, rnd.nextLong())
        val shader = BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        val scale = max(1f, min(w, h) / 1200f)
        shader.setLocalMatrix(Matrix().apply { setScale(scale, scale) })
        val paint = Paint().apply {
            this.shader = shader
            xfermode = PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
            alpha = (amount * 200).toInt().coerceIn(0, 255)
        }
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        tile.recycle()
    }

    private fun drawDate(c: Canvas, w: Int, h: Int) {
        val base = min(w, h).toFloat()
        val size = base * 0.055f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 165, 48)
            textSize = size
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            setShadowLayer(size * 0.35f, 0f, 0f, Color.argb(200, 255, 120, 20))
        }
        c.drawText(dateText(), w - base * 0.06f, h - base * 0.06f, paint)
    }

    private fun cropSquare(b: Bitmap): Bitmap {
        val s = min(b.width, b.height)
        return Bitmap.createBitmap(b, (b.width - s) / 2, (b.height - s) / 2, s, s)
    }

    private fun addPolaroidFrame(img: Bitmap): Bitmap {
        val side = (img.width * 0.06f).toInt()
        val bottom = (img.width * 0.24f).toInt()
        val out = Bitmap.createBitmap(img.width + side * 2, img.height + side + bottom, Bitmap.Config.ARGB_8888)
        Canvas(out).apply {
            drawColor(Color.rgb(255, 253, 248))
            drawBitmap(img, side.toFloat(), side.toFloat(), null)
        }
        return out
    }

    private fun downscale(b: Bitmap): Bitmap {
        val m = max(b.width, b.height)
        if (m <= MAX_SIDE) return b
        val s = MAX_SIDE.toFloat() / m
        return Bitmap.createScaledBitmap(b, (b.width * s).toInt(), (b.height * s).toInt(), true)
    }
}
