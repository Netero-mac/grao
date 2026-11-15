package com.netero.grao.film

import android.graphics.ColorMatrix

/**
 * Um "filme" define o visual da foto: cor (matriz de cor), grão,
 * vinheta, chance de vazamento de luz, data no canto e moldura.
 */
class Film(
    val id: String,
    val name: String,
    val description: String,
    val matrix: ColorMatrix,
    val grain: Float,        // 0..1
    val vignette: Float,     // 0..1
    val leakChance: Float,   // 0..1, chance por foto
    val dateByDefault: Boolean,
    val polaroidFrame: Boolean,
)

object Films {
    val all: List<Film> = listOf(
        Film(
            id = "portra",
            name = "Portra 400",
            description = "Tons quentes e pele bonita",
            matrix = look(saturation = 0.95f, contrast = 1.05f, warmth = 0.08f, fade = 0.15f, b = 0.94f),
            grain = 0.25f, vignette = 0.35f, leakChance = 0.15f,
            dateByDefault = false, polaroidFrame = false,
        ),
        Film(
            id = "trix",
            name = "Tri-X P&B",
            description = "Preto e branco contrastado",
            matrix = look(saturation = 0f, contrast = 1.25f, warmth = 0f, fade = 0.05f),
            grain = 0.55f, vignette = 0.45f, leakChance = 0.05f,
            dateByDefault = false, polaroidFrame = false,
        ),
        Film(
            id = "polaroid",
            name = "Polaroid",
            description = "Cores lavadas e moldura branca",
            matrix = look(saturation = 0.8f, contrast = 0.9f, warmth = 0.04f, fade = 0.35f, g = 1.02f, b = 0.95f),
            grain = 0.2f, vignette = 0.25f, leakChance = 0.1f,
            dateByDefault = false, polaroidFrame = true,
        ),
        Film(
            id = "descartavel",
            name = "Descartável",
            description = "Saturada, vinheta forte e data",
            matrix = look(saturation = 1.25f, contrast = 1.15f, warmth = 0.1f, fade = 0.05f),
            grain = 0.4f, vignette = 0.6f, leakChance = 0.35f,
            dateByDefault = true, polaroidFrame = false,
        ),
        Film(
            id = "superia",
            name = "Superia",
            description = "Verdes frios de filme japonês",
            matrix = look(saturation = 1.05f, contrast = 1.05f, warmth = -0.03f, fade = 0.1f, r = 0.96f, g = 1.03f, b = 0.98f),
            grain = 0.3f, vignette = 0.3f, leakChance = 0.1f,
            dateByDefault = false, polaroidFrame = false,
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
                (1f + warmth) * r, 0f, 0f, 0f, 0f,
                0f, g, 0f, 0f, 0f,
                0f, 0f, (1f - warmth) * b, 0f, 0f,
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
