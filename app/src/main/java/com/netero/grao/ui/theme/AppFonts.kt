package com.netero.grao.ui.theme

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.netero.grao.R

/**
 * Fontes embutidas para desenhar com Canvas (data na foto, PLAY do VHS, texto do editor).
 * Carregadas uma vez no início; se falhar, cai nas fontes do sistema.
 */
object AppFonts {
    @Volatile
    var mono: Typeface = Typeface.MONOSPACE
        private set

    @Volatile
    var title: Typeface = Typeface.SERIF
        private set

    fun load(context: Context) {
        runCatching { ResourcesCompat.getFont(context, R.font.share_tech_mono) }.getOrNull()?.let { mono = it }
        runCatching { ResourcesCompat.getFont(context, R.font.fraunces_semibold) }.getOrNull()?.let { title = it }
    }
}
