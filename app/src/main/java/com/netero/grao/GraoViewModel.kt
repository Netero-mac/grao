package com.netero.grao

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.netero.grao.data.Roll
import com.netero.grao.data.RollRepository
import com.netero.grao.film.Film
import com.netero.grao.film.FilmProcessor
import com.netero.grao.film.Films
import com.netero.grao.gl.EffectParams
import com.netero.grao.gl.Fx
import com.netero.grao.gl.GlEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/** O que abrir no editor: uma foto do celular ou uma foto do próprio Grão. */
sealed class EditSource {
    data class FromUri(val uri: Uri) : EditSource()
    data class FromFile(val file: File) : EditSource()
}

class GraoViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = RollRepository(app)
    private val mutex = Mutex()
    private val prefs = app.getSharedPreferences("grao_ui", 0)

    var currentRoll by mutableStateOf<Roll?>(null)
        private set
    var rolls by mutableStateOf<List<Roll>>(emptyList())
        private set
    var lastPhoto by mutableStateOf<File?>(null)
        private set
    var videos by mutableStateOf<List<File>>(emptyList())
        private set

    fun refreshVideos() {
        viewModelScope.launch {
            videos = withContext(Dispatchers.IO) { com.netero.grao.data.MediaSaver.listVideos(getApplication()) }
        }
    }

    var selectedFilmId by mutableStateOf(Films.all.first().id)
    var capacity by mutableIntStateOf(36)
    var dateOn by mutableStateOf(Films.all.first().dateByDefault)
    var flashOn by mutableStateOf(false)

    /** Efeitos ligados no visor da câmera. */
    var cameraEffects by mutableStateOf<Map<Fx, Float>>(emptyMap())

    /** Instantâneo: a foto aparece na hora. Rolo: espera revelar. */
    var instantMode by mutableStateOf(prefs.getBoolean("instant", true))
        private set

    var favorites by mutableStateOf(prefs.getStringSet("favs", emptySet())!!.toSet())
        private set

    var editSource by mutableStateOf<EditSource?>(null)

    var processing by mutableIntStateOf(0)
        private set

    /** Prévia pequena mostrada na hora do clique, enquanto a foto é processada. */
    var quickThumb by mutableStateOf<Bitmap?>(null)
        private set

    var justFilled by mutableStateOf(false)

    val activeFilm: Film
        get() = if (instantMode) Films.byId(selectedFilmId) else Films.byId(currentRoll?.filmId ?: selectedFilmId)
    val shotsTaken: Int get() = (currentRoll?.shots ?: 0) + if (instantMode) 0 else processing
    val rollLocked: Boolean get() = !instantMode && shotsTaken > 0

    /** Parâmetros do visor ao vivo. */
    val cameraParams: EffectParams
        get() = EffectParams(filmId = activeFilm.id, effects = cameraEffects, dateOn = dateOn, seed = 7L)

    init {
        refresh()
        refreshVideos()
        // Liga a GPU antes da primeira foto para ela não demorar mais
        viewModelScope.launch {
            try {
                GlEngine.warmUp()
            } catch (t: Throwable) {
                Log.w("Grao", "Aquecimento da GPU falhou", t)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val data = withContext(Dispatchers.IO) {
                mutex.withLock {
                    repo.developReady()
                    val all = repo.rolls()
                    val last = all.firstOrNull { it.developed }?.let { repo.photos(it).lastOrNull() }
                    Triple(repo.currentRoll(), all, last)
                }
            }
            currentRoll = data.first
            rolls = data.second
            lastPhoto = data.third
            if (!instantMode) data.first?.let {
                selectedFilmId = it.filmId
                capacity = it.capacity
            }
        }
    }

    fun toggleInstant() {
        instantMode = !instantMode
        prefs.edit().putBoolean("instant", instantMode).apply()
        refresh()
    }

    fun toggleFavorite(id: String) {
        favorites = if (id in favorites) favorites - id else favorites + id
        prefs.edit().putStringSet("favs", favorites).apply()
    }

    fun selectFilm(film: Film) {
        if (rollLocked) return
        selectedFilmId = film.id
        dateOn = film.dateByDefault
    }

    fun setCameraFx(fx: Fx, value: Float) {
        cameraEffects = if (value <= 0f) cameraEffects - fx else cameraEffects + (fx to value)
    }

    fun toggleCapacity() {
        if (rollLocked) return
        capacity = if (capacity == 36) 24 else 36
    }

    fun onCaptured(bitmap: Bitmap, rotation: Int, mirror: Boolean) {
        val params = cameraParams.copy(seed = System.nanoTime(), autoLeak = true)
        val cap = capacity
        val instant = instantMode
        processing++
        viewModelScope.launch {
            try {
                quickThumb = withContext(Dispatchers.Default) {
                    FilmProcessor.rotate(GlEngine.fit(bitmap, 240), rotation, mirror)
                }
                val rotated = withContext(Dispatchers.Default) { FilmProcessor.rotate(bitmap, rotation, mirror) }
                val processed = try {
                    GlEngine.render(rotated, params)
                } catch (t: Throwable) {
                    Log.e("Grao", "GPU falhou, usando processamento antigo", t)
                    withContext(Dispatchers.Default) { FilmProcessor.process(rotated, params.film, params.dateOn) }
                }
                val roll = withContext(Dispatchers.IO) {
                    mutex.withLock {
                        if (instant) repo.addInstant(params.filmId, processed)
                        else repo.addShot(params.filmId, cap, processed)
                    }
                }
                if (!instant && roll.fullAt != null) justFilled = true
            } catch (t: Throwable) {
                Log.e("Grao", "Erro ao salvar foto", t)
            } finally {
                processing--
                refresh()
            }
        }
    }

    fun rewind() {
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) { mutex.withLock { repo.rewindCurrent() } }
            if (r != null) justFilled = true
            refresh()
        }
    }

    /** Rebobina o rolo atual (vai para revelar) e já deixa o novo filme escolhido. */
    fun switchFilm(film: Film) {
        if (processing > 0) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { mutex.withLock { repo.rewindCurrent() } }
            currentRoll = null
            selectedFilmId = film.id
            dateOn = film.dateByDefault
            refresh()
        }
    }

    fun shake(roll: Roll) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { mutex.withLock { repo.shake(roll) } }
            refresh()
        }
    }

    suspend fun photosOf(roll: Roll): List<File> = withContext(Dispatchers.IO) { repo.photos(roll) }
}
