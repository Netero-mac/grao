package com.netero.grao.gl

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.netero.grao.film.FilmProcessor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/** Desenha (com Canvas) tudo o que fica por cima da imagem: poeira, data, texto e o PLAY do VHS. */
object Decal {

    fun isEmpty(k: EffectParams.DecalKey) = k.dust <= 0f && !k.dateOn && !k.vhsText && k.text.text.isBlank()

    fun draw(k: EffectParams.DecalKey, w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val rnd = Random(k.seed)
        if (k.dust > 0f) drawDust(c, w, h, k.dust, rnd)
        if (k.vhsText) drawVhs(c, w, h) else if (k.dateOn) drawDate(c, w, h)
        if (k.text.text.isNotBlank()) drawText(c, w, h, k.text)
        return bmp
    }

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

    private fun drawDate(c: Canvas, w: Int, h: Int) {
        val base = min(w, h).toFloat()
        val size = base * 0.055f
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 165, 48)
            textSize = size
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            setShadowLayer(size * 0.35f, 0f, 0f, Color.argb(200, 255, 120, 20))
        }
        c.drawText(FilmProcessor.dateText(), w - base * 0.06f, h - base * 0.06f, p)
    }

    private fun drawVhs(c: Canvas, w: Int, h: Int) {
        val base = min(w, h).toFloat()
        val size = base * 0.06f
        val margin = base * 0.07f
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = size
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            setShadowLayer(size * 0.15f, size * 0.08f, size * 0.08f, Color.argb(180, 0, 0, 0))
        }
        c.drawText("PLAY", margin, margin + size, p)
        val tx = margin + p.measureText("PLAY ") + size * 0.1f
        val ty = margin + size * 0.15f
        c.drawPath(Path().apply {
            moveTo(tx, ty)
            lineTo(tx + size * 0.75f, ty + size * 0.42f)
            lineTo(tx, ty + size * 0.84f)
            close()
        }, p)
        val now = Date()
        c.drawText(SimpleDateFormat("a hh:mm", Locale.US).format(now), margin, h - margin - size * 1.25f, p)
        c.drawText(SimpleDateFormat("MMM. dd yyyy", Locale.US).format(now).uppercase(Locale.US), margin, h - margin, p)
    }

    private fun drawText(c: Canvas, w: Int, h: Int, t: TextLayer) {
        val base = min(w, h).toFloat()
        val size = base * when (t.style) {
            TextStyleKind.MONO -> 0.06f
            TextStyleKind.SERIF -> 0.08f
            TextStyleKind.BOLD -> 0.09f
        }
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = t.color
            textSize = size
            textAlign = Paint.Align.CENTER
            typeface = when (t.style) {
                TextStyleKind.MONO -> Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                TextStyleKind.SERIF -> Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
                TextStyleKind.BOLD -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            setShadowLayer(size * 0.12f, 0f, size * 0.05f, Color.argb(160, 0, 0, 0))
        }
        val lines = t.text.trim().lines().take(4)
        val lineH = size * 1.2f
        val block = lineH * lines.size
        val top = when (t.pos) {
            TextPos.TOP -> base * 0.08f + size
            TextPos.CENTER -> (h - block) / 2f + size
            TextPos.BOTTOM -> h - base * 0.08f - block + size
        }
        lines.forEachIndexed { i, line -> c.drawText(line, w / 2f, top + i * lineH, p) }
    }

    /** Molduras: mudam o tamanho final, por isso são aplicadas depois do shader. */
    fun applyFrame(img: Bitmap, frame: Frame): Bitmap = when (frame) {
        Frame.NONE -> img
        Frame.POLAROID -> {
            val sq = cropSquare(img)
            val side = (sq.width * 0.06f).toInt()
            val bottom = (sq.width * 0.24f).toInt()
            val out = Bitmap.createBitmap(sq.width + side * 2, sq.height + side + bottom, Bitmap.Config.ARGB_8888)
            Canvas(out).apply {
                drawColor(Color.rgb(255, 253, 248))
                drawBitmap(sq, side.toFloat(), side.toFloat(), null)
            }
            out
        }
        Frame.WHITE -> {
            val b = (max(img.width, img.height) * 0.04f).toInt()
            val out = Bitmap.createBitmap(img.width + b * 2, img.height + b * 2, Bitmap.Config.ARGB_8888)
            Canvas(out).apply {
                drawColor(Color.rgb(250, 248, 242))
                drawBitmap(img, b.toFloat(), b.toFloat(), null)
            }
            out
        }
        Frame.FILM35 -> {
            val band = (min(img.width, img.height) * 0.11f).toInt()
            val landscape = img.width >= img.height
            val out = if (landscape) Bitmap.createBitmap(img.width, img.height + band * 2, Bitmap.Config.ARGB_8888)
            else Bitmap.createBitmap(img.width + band * 2, img.height, Bitmap.Config.ARGB_8888)
            val c = Canvas(out)
            c.drawColor(Color.rgb(18, 14, 12))
            if (landscape) c.drawBitmap(img, 0f, band.toFloat(), null) else c.drawBitmap(img, band.toFloat(), 0f, null)
            // furos da película
            val hole = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(235, 225, 205) }
            val hw = band * 0.32f
            val hh = band * 0.42f
            val len = if (landscape) out.width else out.height
            var pos = hw
            while (pos < len - hw) {
                if (landscape) {
                    c.drawRoundRect(RectF(pos, band * 0.29f, pos + hw, band * 0.29f + hh), hw * 0.2f, hw * 0.2f, hole)
                    c.drawRoundRect(RectF(pos, out.height - band * 0.29f - hh, pos + hw, out.height - band * 0.29f), hw * 0.2f, hw * 0.2f, hole)
                } else {
                    c.drawRoundRect(RectF(band * 0.29f, pos, band * 0.29f + hh, pos + hw), hw * 0.2f, hw * 0.2f, hole)
                    c.drawRoundRect(RectF(out.width - band * 0.29f - hh, pos, out.width - band * 0.29f, pos + hw), hw * 0.2f, hw * 0.2f, hole)
                }
                pos += hw * 2.2f
            }
            val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(244, 167, 44)
                textSize = band * 0.2f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }
            if (landscape) c.drawText("GRAO 400   12A", band * 0.4f, band * 0.22f, label)
            out
        }
        Frame.CINEMA -> {
            // faixas pretas 2.35:1
            val targetH = (img.width / 2.35f).toInt().coerceAtMost(img.height)
            val bar = (img.height - targetH) / 2
            val out = img.copy(Bitmap.Config.ARGB_8888, true)
            val c = Canvas(out)
            val p = Paint().apply { color = Color.BLACK }
            c.drawRect(0f, 0f, img.width.toFloat(), bar.toFloat(), p)
            c.drawRect(0f, (img.height - bar).toFloat(), img.width.toFloat(), img.height.toFloat(), p)
            out
        }
    }

    private fun cropSquare(b: Bitmap): Bitmap {
        val s = min(b.width, b.height)
        return Bitmap.createBitmap(b, (b.width - s) / 2, (b.height - s) / 2, s, s)
    }
}
