package com.netero.grao.film

import android.graphics.Color
import android.graphics.ColorMatrix

/**
 * Um "filme" define o visual da foto: cor (matriz de cor), grão,
 * vinheta, vazamento de luz, data, moldura e efeitos especiais.
 */
class Film(
    val id: String,
    val name: String,
    val description: String,
    val matrix: ColorMatrix,
    val grain: Float,                 // 0..1
    val vignette: Float,              // 0..1
    val leakChance: Float,            // 0..1, chance por foto
    val dateByDefault: Boolean,
    val polaroidFrame: Boolean,
    val halation: Float = 0f,         // brilho em volta das luzes fortes (CineStill, bloom)
    val halationColor: Int = Color.rgb(255, 70, 40),
    val dust: Float = 0f,             // poeira, fios e arranhões
    val chroma: Float = 0f,           // aberração cromática (cores separadas)
    val vhs: Boolean = false,         // linhas de scan, imagem macia e texto PLAY
)

object Films {
    val all: List<Film> = listOf(
        // --- Filmes coloridos clássicos ---
        Film(
            id = "portra", name = "Portra 400", description = "Tons quentes, pele bonita e grão fino",
            matrix = look(saturation = 0.95f, contrast = 1.05f, warmth = 0.08f, fade = 0.15f, b = 0.94f),
            grain = 0.25f, vignette = 0.35f, leakChance = 0.15f, dateByDefault = false, polaroidFrame = false,
        ),
        Film(
            id = "gold", name = "Gold 200", description = "Dourado, nostálgico, cara de férias nos anos 90",
            matrix = look(saturation = 1.1f, contrast = 1.05f, warmth = 0.12f, fade = 0.1f, b = 0.9f),
            grain = 0.3f, vignette = 0.3f, leakChance = 0.15f, dateByDefault = false, polaroidFrame = false,
        ),
        Film(
            id = "ektar", name = "Ektar 100", description = "Cores vivas e saturadas, grão quase invisível",
            matrix = look(saturation = 1.35f, contrast = 1.15f, warmth = 0.05f, fade = 0.02f),
            grain = 0.12f, vignette = 0.2f, leakChance = 0.05f, dateByDefault = false, polaroidFrame = false,
        ),
        Film(
            id = "superia", name = "Superia", description = "Verdes frios de filme japonês",
            matrix = look(saturation = 1.05f, contrast = 1.05f, warmth = -0.03f, fade = 0.1f, r = 0.96f, g = 1.03f, b = 0.98f),
            grain = 0.3f, vignette = 0.3f, leakChance = 0.1f, dateByDefault = false, polaroidFrame = false,
        ),
        Film(
            id = "pro400h", name = "Pro 400H", description = "Pastel, claro e arejado, sombras em ciano",
            matrix = look(saturation = 0.8f, contrast = 0.92f, warmth = -0.05f, fade = 0.28f, r = 0.97f, g = 1.03f, b = 1.02f),
            grain = 0.2f, vignette = 0.15f, leakChance = 0.08f, dateByDefault = false, polaroidFrame = false,
        ),
        Film(
            id = "velvia", name = "Velvia 50", description = "Slide de paisagem: céu azul e verde intensos",
            matrix = look(saturation = 1.5f, contrast = 1.25f, warmth = 0f, fade = 0f, g = 1.05f, b = 1.02f),
            grain = 0.1f, vignette = 0.35f, leakChance = 0.03f, dateByDefault = false, polaroidFrame = false,
        ),
        Film(
            id = "ektachrome", name = "Ektachrome", description = "Slide limpo e frio, azuis bonitos",
            matrix = look(saturation = 1.15f, contrast = 1.12f, warmth = -0.06f, fade = 0.05f, b = 1.04f),
            grain = 0.15f, vignette = 0.25f, leakChance = 0.05f, dateByDefault = false, polaroidFrame = false,
        ),
        Film(
            id = "cinestill", name = "CineStill 800T", description = "Filme de cinema: tons frios e brilho vermelho nas luzes",
            matrix = look(saturation = 1.05f, contrast = 1.1f, warmth = -0.12f, fade = 0.12f),
            grain = 0.45f, vignette = 0.3f, leakChance = 0.1f, dateByDefault = false, polaroidFrame = false,
            halation = 0.85f, halationColor = Color.rgb(255, 60, 35),
        ),

        // --- Preto e branco ---
        Film(
            id = "trix", name = "Tri-X P&B", description = "Preto e branco contrastado, de rua",
            matrix = look(saturation = 0f, contrast = 1.25f, warmth = 0f, fade = 0.05f),
            grain = 0.55f, vignette = 0.45f, leakChance = 0.05f, dateByDefault = false, polaroidFrame = false,
        ),
        Film(
            id = "hp5", name = "HP5 P&B", description = "Preto e branco suave, cinzas ricos e muito grão",
            matrix = look(saturation = 0f, contrast = 1.1f, warmth = 0f, fade = 0.15f),
            grain = 0.6f, vignette = 0.35f, leakChance = 0.05f, dateByDefault = false, polaroidFrame = false,
        ),
        Film(
            id = "sepia", name = "Sépia 1900", description = "Foto de época: sépia, vinheta forte e poeira",
            matrix = look(saturation = 0f, contrast = 1.05f, warmth = 0f, fade = 0.3f, r = 1.12f, g = 0.95f, b = 0.72f),
            grain = 0.55f, vignette = 0.7f, leakChance = 0f, dateByDefault = false, polaroidFrame = false,
            dust = 0.8f,
        ),

        // --- Câmeras e estilos de app ---
        Film(
            id = "polaroid", name = "Polaroid", description = "Cores lavadas e moldura branca",
            matrix = look(saturation = 0.8f, contrast = 0.9f, warmth = 0.04f, fade = 0.35f, g = 1.02f, b = 0.95f),
            grain = 0.2f, vignette = 0.25f, leakChance = 0.1f, dateByDefault = false, polaroidFrame = true,
        ),
        Film(
            id = "descartavel", name = "Descartável", description = "Saturada, vinheta forte e data",
            matrix = look(saturation = 1.25f, contrast = 1.15f, warmth = 0.1f, fade = 0.05f),
            grain = 0.4f, vignette = 0.6f, leakChance = 0.35f, dateByDefault = true, polaroidFrame = false,
        ),
        Film(
            id = "huji", name = "Huji 98", description = "Câmera de 1998: quente, data e light leak quase sempre",
            matrix = look(saturation = 1.1f, contrast = 1.05f, warmth = 0.15f, fade = 0.18f),
            grain = 0.35f, vignette = 0.4f, leakChance = 0.85f, dateByDefault = true, polaroidFrame = false,
        ),
        Film(
            id = "cpm35", name = "CPM35", description = "Quente, suave e com brilho difuso",
            matrix = look(saturation = 0.9f, contrast = 0.95f, warmth = 0.14f, fade = 0.25f, b = 0.88f),
            grain = 0.35f, vignette = 0.3f, leakChance = 0.2f, dateByDefault = false, polaroidFrame = false,
            halation = 0.35f, halationColor = Color.rgb(255, 210, 160),
        ),
        Film(
            id = "lomo", name = "Lomo Cross", description = "Revelação cruzada: cores loucas e vinheta pesada",
            matrix = look(saturation = 1.4f, contrast = 1.3f, warmth = 0f, fade = 0f, r = 1.05f, g = 1.1f, b = 0.8f, bOff = 25f),
            grain = 0.35f, vignette = 0.75f, leakChance = 0.25f, dateByDefault = false, polaroidFrame = false,
            chroma = 0.3f,
        ),
        Film(
            id = "super8", name = "Super 8", description = "Filme caseiro dos anos 70: quente, granulado e arranhado",
            matrix = look(saturation = 0.9f, contrast = 1.1f, warmth = 0.12f, fade = 0.2f),
            grain = 0.75f, vignette = 0.65f, leakChance = 0.3f, dateByDefault = false, polaroidFrame = false,
            dust = 0.6f,
        ),
        Film(
            id = "vhs", name = "VHS", description = "Fita de vídeo: linhas, cores borradas e PLAY na tela",
            matrix = look(saturation = 1.2f, contrast = 0.95f, warmth = 0.02f, fade = 0.15f, b = 1.05f),
            grain = 0.2f, vignette = 0.2f, leakChance = 0f, dateByDefault = false, polaroidFrame = false,
            chroma = 0.8f, vhs = true,
        ),
    )

    fun byId(id: String?): Film = all.firstOrNull { it.id == id } ?: all.first()
}

/** Monta uma matriz de cor a partir de ajustes simples. */
internal fun look(
    saturation: Float,
    contrast: Float,
    warmth: Float,
    fade: Float,
    r: Float = 1f,
    g: Float = 1f,
    b: Float = 1f,
    rOff: Float = 0f,
    gOff: Float = 0f,
    bOff: Float = 0f,
): ColorMatrix {
    val m = ColorMatrix()
    m.setSaturation(saturation)

    val t = 128f * (1f - contrast)
    m.postConcat(
        ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, t,
                0f, contrast, 0f, 0f, t,
                0f, 0f, contrast, 0f, t,
                0f, 0f, 0f, 1f, 0f,
            )
        )
    )

    m.postConcat(
        ColorMatrix(
            floatArrayOf(
                (1f + warmth) * r, 0f, 0f, 0f, rOff,
                0f, g, 0f, 0f, gOff,
                0f, 0f, (1f - warmth) * b, 0f, bOff,
                0f, 0f, 0f, 1f, 0f,
            )
        )
    )

    // "Fade": levanta os pretos, como filme velho
    val lift = fade * 60f
    val s = 1f - lift / 255f
    m.postConcat(
        ColorMatrix(
            floatArrayOf(
                s, 0f, 0f, 0f, lift,
                0f, s, 0f, 0f, lift,
                0f, 0f, s, 0f, lift,
                0f, 0f, 0f, 1f, 0f,
            )
        )
    )
    return m
}
