package com.netero.grao.film

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
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
        if (film.vhs) img = soften(img, 720)

        val w = img.width
        val h = img.height
        var out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        var canvas = Canvas(out)

        val colorPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(film.matrix)
        }
        canvas.drawBitmap(img, 0f, 0f, colorPaint)

        if (film.halation > 0f) drawHalation(canvas, out, film.halation, film.halationColor)
        if (film.chroma > 0f) {
            out = chromaShift(out, film.chroma)
            canvas = Canvas(out)
        }
        if (rnd.nextFloat() < film.leakChance) drawLightLeak(canvas, w, h, rnd)
        drawVignette(canvas, w, h, film.vignette)
        drawGrain(canvas, w, h, film.grain, rnd)
        if (film.dust > 0f) drawDust(canvas, w, h, film.dust, rnd)

        if (film.vhs) {
            drawScanlines(canvas, w, h)
            drawVhsOverlay(canvas, w, h)
        } else if (dateOn) {
            drawDate(canvas, w, h)
        }

        return if (film.polaroidFrame) addPolaroidFrame(out) else out
    }

    /** Brilho em volta das partes mais claras (halação do CineStill, bloom). */
    private fun drawHalation(c: Canvas, src: Bitmap, strength: Float, color: Int) {
        val w = src.width
        val h = src.height
        val sw = max(1, w / 16)
        val sh = max(1, h / 16)
        val small = Bitmap.createScaledBitmap(src, sw, sh, true)
        val px = IntArray(sw * sh)
        small.getPixels(px, 0, sw, 0, 0, sw, sh)
        for (i in px.indices) {
            val p = px[i]
            val lum = 0.299f * Color.red(p) + 0.587f * Color.green(p) + 0.114f * Color.blue(p)
            val a = ((lum - 185f) / 70f).coerceIn(0f, 1f)
            px[i] = Color.argb((a * 255f * strength).toInt().coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
        }
        val mask = Bitmap.createBitmap(px, sw, sh, Bitmap.Config.ARGB_8888)
        val tiny = Bitmap.createScaledBitmap(mask, max(1, sw / 2), max(1, sh / 2), true)
        val glow = Bitmap.createScaledBitmap(tiny, w, h, true)
        c.drawBitmap(glow, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        })
        if (small !== src) small.recycle()
        if (tiny !== mask) tiny.recycle()
        mask.recycle()
        glow.recycle()
    }

    /** Separa os canais de cor, como lente barata ou fita VHS. */
    private fun chromaShift(src: Bitmap, amount: Float): Bitmap {
        val w = src.width
        val h = src.height
        val dx = max(1f, w * 0.004f * amount)
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.drawColor(Color.BLACK)
        fun channel(r: Float, g: Float, b: Float) = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(
                ColorMatrix(
                    floatArrayOf(
                        r, 0f, 0f, 0f, 0f,
                        0f, g, 0f, 0f, 0f,
                        0f, 0f, b, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f,
                    )
                )
            )
            xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
        }
        c.drawBitmap(src, dx, 0f, channel(1f, 0f, 0f))
        c.drawBitmap(src, 0f, 0f, channel(0f, 1f, 0f))
        c.drawBitmap(src, -dx, 0f, channel(0f, 0f, 1f))
        return out
    }

    /** Poeira, fios de cabelo e arranhões de filme velho. */
    private fun drawDust(c: Canvas, w: Int, h: Int, amount: Float, rnd: Random) {
        val base = min(w, h).toFloat()
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        repeat((amount * 120).toInt()) {
            val light = rnd.nextBoolean()
            p.color = if (light) Color.argb(150 + rnd.nextInt(80), 255, 250, 235)
            else Color.argb(120 + rnd.nextInt(100), 20, 15, 10)
            c.drawCircle(rnd.nextFloat() * w, rnd.nextFloat() * h, base * (0.0008f + rnd.nextFloat() * 0.0025f), p)
        }

        p.style = Paint.Style.STROKE
        p.strokeWidth = base * 0.0015f
        repeat((amount * 4).toInt()) {
            p.color = Color.argb(140, 25, 20, 15)
            val x = rnd.nextFloat() * w
            val y = rnd.nextFloat() * h
            val len = base * (0.03f + rnd.nextFloat() * 0.06f)
            val path = Path().apply {
                moveTo(x, y)
                quadTo(x + len * (rnd.nextFloat() - 0.5f), y + len * 0.5f, x + len * (rnd.nextFloat() - 0.5f), y + len)
            }
            c.drawPath(path, p)
        }

        p.strokeWidth = base * 0.0012f
        repeat((amount * 3).toInt() + 1) {
            p.color = Color.argb(70, 255, 250, 235)
            val x = rnd.nextFloat() * w
            c.drawLine(x, 0f, x + rnd.nextFloat() * base * 0.02f, h.toFloat(), p)
        }
    }

    private fun drawScanlines(c: Canvas, w: Int, h: Int) {
        val p = Paint().apply { color = Color.argb(45, 0, 0, 0) }
        val step = max(3, h / 360)
        val line = max(1, step / 2)
        var y = 0
        while (y < h) {
            c.drawRect(0f, y.toFloat(), w.toFloat(), (y + line).toFloat(), p)
            y += step
        }
    }

    /** Texto de câmera de vídeo: PLAY ▶ em cima, hora e data embaixo. */
    private fun drawVhsOverlay(c: Canvas, w: Int, h: Int) {
        val base = min(w, h).toFloat()
        val size = base * 0.06f
        val margin = base * 0.07f
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = size
            typeface = com.netero.grao.ui.theme.AppFonts.mono
            setShadowLayer(size * 0.15f, size * 0.08f, size * 0.08f, Color.argb(180, 0, 0, 0))
        }
        c.drawText("PLAY", margin, margin + size, p)
        val tx = margin + p.measureText("PLAY ") + size * 0.1f
        val ty = margin + size * 0.15f
        val tri = Path().apply {
            moveTo(tx, ty)
            lineTo(tx + size * 0.75f, ty + size * 0.42f)
            lineTo(tx, ty + size * 0.84f)
            close()
        }
        c.drawPath(tri, p)

        val now = Date()
        val hour = SimpleDateFormat("a hh:mm", Locale.US).format(now)
        val day = SimpleDateFormat("MMM. dd yyyy", Locale.US).format(now).uppercase(Locale.US)
        c.drawText(hour, margin, h - margin - size * 1.25f, p)
        c.drawText(day, margin, h - margin, p)
    }

    /** Deixa a imagem macia, como resolução de fita. */
    private fun soften(b: Bitmap, targetWidth: Int): Bitmap {
        if (b.width <= targetWidth) return b
        val th = max(1, b.height * targetWidth / b.width)
        val small = Bitmap.createScaledBitmap(b, targetWidth, th, true)
        val back = Bitmap.createScaledBitmap(small, b.width, b.height, true)
        small.recycle()
        return back
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
            typeface = com.netero.grao.ui.theme.AppFonts.mono
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
