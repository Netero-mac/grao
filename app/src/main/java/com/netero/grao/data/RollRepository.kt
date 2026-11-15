package com.netero.grao.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.provider.MediaStore
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class Roll(
    val id: String,
    val number: Int,
    val filmId: String,
    val capacity: Int,
    val shots: Int,
    val createdAt: Long,
    val fullAt: Long? = null,
    val readyAt: Long? = null,
    val developed: Boolean = false,
) {
    val inDevelopment: Boolean get() = fullAt != null && !developed
    val isFull: Boolean get() = shots >= capacity
}

/**
 * Guarda os rolos em files/rolls/<id>/ (roll.json + 001.jpg, 002.jpg...).
 * As fotos só vão para a galeria do celular depois de "reveladas".
 */
class RollRepository(private val context: Context) {

    private val root = File(context.filesDir, "rolls").apply { mkdirs() }
    private val prefs = context.getSharedPreferences("grao", Context.MODE_PRIVATE)

    fun currentRoll(): Roll? = prefs.getString(KEY_CURRENT, null)?.let { load(it) }

    fun rolls(): List<Roll> =
        root.listFiles()?.mapNotNull { load(it.name) }?.sortedByDescending { it.createdAt } ?: emptyList()

    fun photos(roll: Roll): List<File> =
        File(root, roll.id).listFiles { f -> f.extension == "jpg" }?.sortedBy { it.name } ?: emptyList()

    fun addShot(filmId: String, capacity: Int, photo: Bitmap): Roll {
        val roll = currentRoll() ?: startRoll(filmId, capacity)
        val n = roll.shots + 1
        File(dir(roll), "%03d.jpg".format(n)).outputStream().use {
            photo.compress(Bitmap.CompressFormat.JPEG, 92, it)
        }
        val updated = roll.copy(shots = n)
        return if (updated.isFull) sendToDevelop(updated) else updated.also { save(it) }
    }

    /** "Rebobinar": manda o rolo atual para revelar mesmo sem estar cheio. */
    fun rewindCurrent(): Roll? {
        val r = currentRoll() ?: return null
        if (r.shots == 0) return null
        return sendToDevelop(r)
    }

    /** Agitar o rolo adianta a revelação. */
    fun shake(roll: Roll, minutes: Int = SHAKE_MINUTES) {
        val fresh = load(roll.id) ?: return
        val ready = fresh.readyAt ?: return
        val now = System.currentTimeMillis()
        save(fresh.copy(readyAt = maxOf(now, ready - minutes * 60_000L)))
    }

    /** Revela os rolos cujo tempo acabou e exporta as fotos para a galeria. */
    fun developReady(): Int {
        val now = System.currentTimeMillis()
        var count = 0
        rolls().filter { it.inDevelopment && (it.readyAt ?: 0L) <= now }.forEach { r ->
            photos(r).forEach { exportToGallery(it, "grao_rolo%02d_%s".format(r.number, it.name)) }
            save(r.copy(developed = true))
            count++
        }
        return count
    }

    private fun startRoll(filmId: String, capacity: Int): Roll {
        val r = Roll(
            id = UUID.randomUUID().toString(),
            number = (rolls().maxOfOrNull { it.number } ?: 0) + 1,
            filmId = filmId,
            capacity = capacity,
            shots = 0,
            createdAt = System.currentTimeMillis(),
        )
        dir(r).mkdirs()
        save(r)
        prefs.edit().putString(KEY_CURRENT, r.id).apply()
        return r
    }

    private fun sendToDevelop(r: Roll): Roll {
        val now = System.currentTimeMillis()
        val updated = r.copy(fullAt = now, readyAt = now + DEVELOP_MINUTES * 60_000L)
        save(updated)
        prefs.edit().remove(KEY_CURRENT).apply()
        return updated
    }

    private fun dir(r: Roll) = File(root, r.id)

    private fun save(r: Roll) {
        val j = JSONObject()
            .put("id", r.id)
            .put("number", r.number)
            .put("filmId", r.filmId)
            .put("capacity", r.capacity)
            .put("shots", r.shots)
            .put("createdAt", r.createdAt)
            .put("developed", r.developed)
        r.fullAt?.let { j.put("fullAt", it) }
        r.readyAt?.let { j.put("readyAt", it) }
        File(dir(r), "roll.json").writeText(j.toString())
    }

    private fun load(id: String): Roll? {
        val f = File(File(root, id), "roll.json")
        if (!f.exists()) return null
        return try {
            val j = JSONObject(f.readText())
            Roll(
                id = j.getString("id"),
                number = j.getInt("number"),
                filmId = j.getString("filmId"),
                capacity = j.getInt("capacity"),
                shots = j.getInt("shots"),
                createdAt = j.getLong("createdAt"),
                fullAt = if (j.has("fullAt")) j.getLong("fullAt") else null,
                readyAt = if (j.has("readyAt")) j.getLong("readyAt") else null,
                developed = j.optBoolean("developed", false),
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun exportToGallery(file: File, name: String) {
        try {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Grao")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return
            resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (e: Exception) {
            // Se falhar, a foto continua disponível dentro do app.
        }
    }

    companion object {
        const val DEVELOP_MINUTES = 60
        const val SHAKE_MINUTES = 5
        private const val KEY_CURRENT = "current_roll"
    }
}
