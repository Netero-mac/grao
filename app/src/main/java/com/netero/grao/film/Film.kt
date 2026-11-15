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
    val splitShadow: Int? = null,     // cor puxada nas sombras (split toning)
    val splitHighlight: Int? = null,  // cor puxada nas luzes
    val splitAmount: Float = 0f,
    val glow: Float = 0f,             // brilho suave geral
    val dream: Float = 0f,            // imagem enevoada
    val duotone: Boolean = false,     // pinta a imagem com duas cores (sombra e luz)
    val gamma: Float = 1f,            // curva dos meios-tons (>1 escurece)
    val preGamma: Float = 1f,         // curva aplicada antes da matriz de cor
) {
    val category: FilmCategory get() = Films.categoryOf(id)
}

enum class FilmCategory(val label: String) {
    FILME("Filme"),
    PB("P&B"),
    CAMERAS("Câmeras"),
    RETRO("Retrô"),
    MATTE("Matte"),
    CINEMA("Cinema"),
    NEON("Neon"),
    ARTE("Arte"),
}

private object FilmCategories {
    val map = mapOf(
        "trix" to FilmCategory.PB, "hp5" to FilmCategory.PB, "noir" to FilmCategory.PB, "sepia" to FilmCategory.PB,
        "polaroid" to FilmCategory.CAMERAS, "descartavel" to FilmCategory.CAMERAS, "huji" to FilmCategory.CAMERAS,
        "cpm35" to FilmCategory.CAMERAS, "lomo" to FilmCategory.CAMERAS,
        "super8" to FilmCategory.RETRO, "vhs" to FilmCategory.RETRO, "anos70" to FilmCategory.RETRO, "indie" to FilmCategory.RETRO,
        "outono" to FilmCategory.MATTE, "cidade" to FilmCategory.MATTE, "sonho" to FilmCategory.MATTE,
        "matcha" to FilmCategory.MATTE, "gelo" to FilmCategory.MATTE, "rosa" to FilmCategory.MATTE,
        "teal" to FilmCategory.CINEMA, "sunset" to FilmCategory.CINEMA, "ouro" to FilmCategory.CINEMA, "cinestill" to FilmCategory.CINEMA,
        "miami" to FilmCategory.NEON, "vapor" to FilmCategory.NEON, "cyber" to FilmCategory.NEON,
        "y2k" to FilmCategory.NEON, "kidcore" to FilmCategory.NEON,
        "duo_rosa" to FilmCategory.ARTE, "duo_lima" to FilmCategory.ARTE,
    )
}

/**
 * "Outono": transformação de cor medida num vídeo de referência
 * (quadro sem filtro x quadro filtrado), com pretos levantados.
 */
internal fun outonoMatrix(): ColorMatrix {
    val k = 0.95f
    val m = ColorMatrix()
    m.setSaturation(1.15f)
    m.postConcat(
        ColorMatrix(
            floatArrayOf(
                0.625f * k, 0.042f * k, 0.128f * k, 0f, 48.7f,
                0.091f * k, 0.523f * k, 0.154f * k, 0f, 51.6f,
                -0.033f * k, 0.229f * k, 0.541f * k, 0f, 53.3f,
                0f, 0f, 0f, 1f, 0f,
            )
        )
    )
    return m
}

/** "Cidade": curvas medidas em outro vídeo de referência (duas cenas antes/depois). */
internal fun cidadeMatrix(): ColorMatrix {
    val m = ColorMatrix()
    m.setSaturation(0.92f)
    m.postConcat(
        ColorMatrix(
            floatArrayOf(
                0.87f, 0f, 0f, 0f, 32.6f,
                0f, 0.82f, 0f, 0f, 32.4f,
                0f, 0f, 0.734f, 0f, 34.2f,
                0f, 0f, 0f, 1f, 0f,
            )
        )
    )
    return m
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
