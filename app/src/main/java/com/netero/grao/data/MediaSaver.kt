package com.netero.grao.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.provider.MediaStore
import java.io.File
import kotlin.math.max

/** Salvar, abrir e compartilhar fotos e vídeos pela galeria do celular. */
object MediaSaver {

    fun saveImage(context: Context, bitmap: Bitmap, name: String = "grao_${System.currentTimeMillis()}.jpg"): Uri? = try {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Grao")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 94, it) }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        uri
    } catch (e: Exception) {
        null
    }

    fun saveVideoFile(context: Context, file: File, name: String = "grao_${System.currentTimeMillis()}.mp4"): Uri? = try {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Grao")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        uri
    } catch (e: Exception) {
        null
    }

    /** Abre uma imagem já com a rotação certa (EXIF), limitada a maxSide pixels. */
    fun load(context: Context, uri: Uri, maxSide: Int = 3000): Bitmap =
        decode(ImageDecoder.createSource(context.contentResolver, uri), maxSide)

    fun load(file: File, maxSide: Int = 3000): Bitmap = decode(ImageDecoder.createSource(file), maxSide)

    private fun decode(source: ImageDecoder.Source, maxSide: Int): Bitmap =
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = false
            val m = max(info.size.width, info.size.height)
            if (m > maxSide) {
                val s = maxSide.toFloat() / m
                decoder.setTargetSize((info.size.width * s).toInt().coerceAtLeast(1), (info.size.height * s).toInt().coerceAtLeast(1))
            }
        }.let { if (it.config == Bitmap.Config.ARGB_8888) it else it.copy(Bitmap.Config.ARGB_8888, false) }

    /** Pasta onde o Grão guarda os próprios vídeos (aparecem na aba Vídeos da galeria). */
    fun videoDir(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, "videos").apply { mkdirs() }

    fun listVideos(context: Context): List<File> =
        videoDir(context).listFiles { f -> f.extension == "mp4" && f.length() > 0 }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()

    /** Compartilha um arquivo do app (vídeo ou foto) via FileProvider. */
    fun shareFile(context: Context, file: File, mime: String) {
        val uri = androidx.core.content.FileProvider.getUriForFile(context, context.packageName + ".files", file)
        share(context, uri, mime)
    }

    fun share(context: Context, uri: Uri, mime: String = "image/jpeg") {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Compartilhar").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
