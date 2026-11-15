package com.netero.grao

import android.app.Application
import android.graphics.Bitmap
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class GraoViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = RollRepository(app)
    private val mutex = Mutex()

    var currentRoll by mutableStateOf<Roll?>(null)
        private set
    var rolls by mutableStateOf<List<Roll>>(emptyList())
        private set
    var lastPhoto by mutableStateOf<File?>(null)
        private set

    var selectedFilmId by mutableStateOf(Films.all.first().id)
    var capacity by mutableIntStateOf(36)
    var dateOn by mutableStateOf(Films.all.first().dateByDefault)
    var flashOn by mutableStateOf(false)

    /** Fotos tiradas que ainda estão sendo processadas. */
    var processing by mutableIntStateOf(0)
        private set

    /** Vira true quando um rolo enche; a tela de revelação abre sozinha. */
    var justFilled by mutableStateOf(false)

    val activeFilm: Film get() = Films.byId(currentRoll?.filmId ?: selectedFilmId)
    val shotsTaken: Int get() = (currentRoll?.shots ?: 0) + processing
    val rollLocked: Boolean get() = shotsTaken > 0

    init {
        refresh()
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
            data.first?.let {
                selectedFilmId = it.filmId
                capacity = it.capacity
            }
        }
    }

    fun selectFilm(film: Film) {
        if (rollLocked) return
        selectedFilmId = film.id
        dateOn = film.dateByDefault
    }

    fun toggleCapacity() {
        if (rollLocked) return
        capacity = if (capacity == 36) 24 else 36
    }

    fun onCaptured(bitmap: Bitmap, rotation: Int, mirror: Boolean) {
        val film = activeFilm
        val date = dateOn
        val cap = capacity
        processing++
        viewModelScope.launch {
            try {
                val processed = withContext(Dispatchers.Default) {
                    FilmProcessor.process(FilmProcessor.rotate(bitmap, rotation, mirror), film, date)
                }
                val roll = withContext(Dispatchers.IO) {
                    mutex.withLock { repo.addShot(film.id, cap, processed) }
                }
                if (roll.fullAt != null) justFilled = true
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

    fun shake(roll: Roll) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { mutex.withLock { repo.shake(roll) } }
            refresh()
        }
    }

    suspend fun photosOf(roll: Roll): List<File> = withContext(Dispatchers.IO) { repo.photos(roll) }
}
