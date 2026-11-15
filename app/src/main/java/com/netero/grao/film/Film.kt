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
    val fx: Map<com.netero.grao.gl.Fx, Float> = emptyMap(), // efeitos que já vêm com o filtro
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
    SOMBRIO("Sombrio"),
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
        "frio" to FilmCategory.MATTE, "esmeralda" to FilmCategory.CINEMA,
        "gotico" to FilmCategory.SOMBRIO, "etereo" to FilmCategory.SOMBRIO,
        "noturno" to FilmCategory.SOMBRIO, "lua" to FilmCategory.SOMBRIO, "vampiro" to FilmCategory.SOMBRIO,
        "floresta" to FilmCategory.SOMBRIO, "nevoa" to FilmCategory.SOMBRIO, "tempestade" to FilmCategory.SOMBRIO,
        "ruina" to FilmCategory.SOMBRIO, "assombrado" to FilmCategory.SOMBRIO, "crepusculo" to FilmCategory.SOMBRIO,
        "abismo" to FilmCategory.SOMBRIO,
        "teal" to FilmCategory.CINEMA, "nuvem" to FilmCategory.CINEMA, "sunset" to FilmCategory.CINEMA, "ouro" to FilmCategory.CINEMA, "cinestill" to FilmCategory.CINEMA,
        "miami" to FilmCategory.NEON, "vapor" to FilmCategory.NEON, "cyber" to FilmCategory.NEON,
        "y2k" to FilmCategory.NEON, "kidcore" to FilmCategory.NEON,
        "duo_rosa" to FilmCategory.ARTE, "cianotipia" to FilmCategory.ARTE, "riso" to FilmCategory.ARTE,
        "popart" to FilmCategory.ARTE, "quadrinho" to FilmCategory.ARTE, "jornal" to FilmCategory.ARTE,
        "infra" to FilmCategory.ARTE, "negativo" to FilmCategory.ARTE, "termica" to FilmCategory.ARTE,
        "pixel8" to FilmCategory.ARTE, "duo_sol" to FilmCategory.ARTE, "duo_menta" to FilmCategory.ARTE,
        "duo_noite" to FilmCategory.ARTE, "duo_lima" to FilmCategory.ARTE,
    )
}

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

        // --- Tons estéticos (estilo apps de efeitos) ---
        Film(
            id = "noir", name = "Noir", description = "Preto e branco duro, pretos profundos",
            matrix = look(saturation = 0f, contrast = 1.45f, warmth = 0f, fade = 0f),
            grain = 0.3f, vignette = 0.55f, leakChance = 0f, dateByDefault = false, polaroidFrame = false,
        ),
        Film(
            id = "teal", name = "Teal & Orange", description = "Cinema: sombras azul-petróleo e pele alaranjada",
            matrix = look(saturation = 1.1f, contrast = 1.1f, warmth = 0.04f, fade = 0.05f),
            grain = 0.15f, vignette = 0.3f, leakChance = 0f, dateByDefault = false, polaroidFrame = false,
            splitShadow = Color.rgb(0, 128, 140), splitHighlight = Color.rgb(255, 150, 70), splitAmount = 0.8f,
        ),
        Film(
            id = "miami", name = "Miami", description = "Rosa e ciano neon, clima anos 80",
            matrix = look(saturation = 1.25f, contrast = 1.05f, warmth = 0f, fade = 0.1f),
            grain = 0.15f, vignette = 0.25f, leakChance = 0.1f, dateByDefault = false, polaroidFrame = false,
            splitShadow = Color.rgb(40, 200, 230), splitHighlight = Color.rgb(255, 80, 180), splitAmount = 0.75f,
            glow = 0.2f,
        ),
        Film(
            id = "y2k", name = "Y2K Flash", description = "Flash estourado, frio e brilhante dos anos 2000",
            matrix = look(saturation = 1.05f, contrast = 1.2f, warmth = -0.08f, fade = 0f, b = 1.06f),
            grain = 0.2f, vignette = 0.45f, leakChance = 0f, dateByDefault = true, polaroidFrame = false,
            glow = 0.35f,
        ),
        Film(
            id = "rosa", name = "Rosé", description = "Tons rosados e suaves, sombras lilás",
            matrix = look(saturation = 0.9f, contrast = 0.95f, warmth = 0.03f, fade = 0.22f, r = 1.04f, g = 0.96f, b = 1.0f),
            grain = 0.15f, vignette = 0.2f, leakChance = 0.05f, dateByDefault = false, polaroidFrame = false,
            splitShadow = Color.rgb(150, 110, 200), splitHighlight = Color.rgb(255, 190, 200), splitAmount = 0.5f,
        ),
        Film(
            id = "kidcore", name = "Kidcore", description = "Cores primárias berrantes, clima de desenho infantil",
            matrix = look(saturation = 1.7f, contrast = 1.15f, warmth = 0.02f, fade = 0f),
            grain = 0.1f, vignette = 0.1f, leakChance = 0f, dateByDefault = false, polaroidFrame = false,
            glow = 0.15f,
        ),
        Film(
            id = "indie", name = "Indie", description = "Laranja queimado, verdes sujos e fade",
            matrix = look(saturation = 0.85f, contrast = 1.05f, warmth = 0.16f, fade = 0.3f, g = 1.04f, b = 0.85f),
            grain = 0.35f, vignette = 0.3f, leakChance = 0.15f, dateByDefault = false, polaroidFrame = false,
            splitShadow = Color.rgb(60, 110, 80), splitHighlight = Color.rgb(255, 170, 90), splitAmount = 0.45f,
        ),
        Film(
            id = "vapor", name = "Vapor", description = "Vaporwave: roxo, rosa e azul piscina",
            matrix = look(saturation = 1.2f, contrast = 1.0f, warmth = 0f, fade = 0.15f, r = 1.05f, g = 0.92f, b = 1.1f),
            grain = 0.15f, vignette = 0.25f, leakChance = 0f, dateByDefault = false, polaroidFrame = false,
            splitShadow = Color.rgb(90, 60, 200), splitHighlight = Color.rgb(255, 130, 210), splitAmount = 0.7f,
            glow = 0.25f,
        ),
        Film(
            id = "cyber", name = "Cyber", description = "Noite de neon: magenta e ciano com contraste alto",
            matrix = look(saturation = 1.3f, contrast = 1.3f, warmth = -0.05f, fade = 0f),
            grain = 0.2f, vignette = 0.5f, leakChance = 0f, dateByDefault = false, polaroidFrame = false,
            splitShadow = Color.rgb(0, 210, 230), splitHighlight = Color.rgb(255, 40, 170), splitAmount = 0.85f,
            glow = 0.3f, chroma = 0.25f,
        ),
        Film(
            id = "sunset", name = "Pôr do sol", description = "Hora dourada exagerada, tudo laranja e rosado",
            matrix = look(saturation = 1.2f, contrast = 1.05f, warmth = 0.22f, fade = 0.08f, b = 0.85f),
            grain = 0.15f, vignette = 0.3f, leakChance = 0.25f, dateByDefault = false, polaroidFrame = false,
            splitShadow = Color.rgb(180, 70, 120), splitHighlight = Color.rgb(255, 190, 90), splitAmount = 0.5f,
            glow = 0.2f,
        ),
        Film(
            id = "anos70", name = "Anos 70", description = "Marrom, mostarda e foto desbotada de álbum",
            matrix = look(saturation = 0.75f, contrast = 0.95f, warmth = 0.2f, fade = 0.35f, g = 0.98f, b = 0.78f),
            grain = 0.45f, vignette = 0.45f, leakChance = 0.15f, dateByDefault = false, polaroidFrame = false,
            dust = 0.3f,
        ),
        Film(
            id = "matcha", name = "Matcha", description = "Verde suave e calmo, sombras claras",
            matrix = look(saturation = 0.85f, contrast = 0.92f, warmth = -0.02f, fade = 0.25f, r = 0.95f, g = 1.05f, b = 0.95f),
            grain = 0.15f, vignette = 0.15f, leakChance = 0f, dateByDefault = false, polaroidFrame = false,
            splitShadow = Color.rgb(90, 140, 100), splitHighlight = Color.rgb(240, 240, 200), splitAmount = 0.4f,
        ),
        Film(
            id = "gelo", name = "Gelo", description = "Azul frio de inverno, brancos limpos",
            matrix = look(saturation = 0.8f, contrast = 1.05f, warmth = -0.18f, fade = 0.12f, b = 1.08f),
            grain = 0.1f, vignette = 0.2f, leakChance = 0f, dateByDefault = false, polaroidFrame = false,
            dream = 0.15f,
        ),
        Film(
            id = "sonho", name = "Sonho", description = "Pastel enevoado e brilhante",
            matrix = look(saturation = 0.9f, contrast = 0.85f, warmth = 0.04f, fade = 0.3f),
            grain = 0.1f, vignette = 0.1f, leakChance = 0.1f, dateByDefault = false, polaroidFrame = false,
            dream = 0.6f, glow = 0.3f,
        ),
        Film(
            id = "duo_rosa", name = "Duo Rosa", description = "Duas cores: azul-marinho e rosa",
            matrix = look(saturation = 1f, contrast = 1.1f, warmth = 0f, fade = 0f),
            grain = 0.15f, vignette = 0.2f, leakChance = 0f, dateByDefault = false, polaroidFrame = false,
            splitShadow = Color.rgb(25, 30, 90), splitHighlight = Color.rgb(255, 120, 170), duotone = true,
        ),
        Film(
            id = "duo_lima", name = "Duo Lima", description = "Duas cores: roxo e verde-limão",
            matrix = look(saturation = 1f, contrast = 1.1f, warmth = 0f, fade = 0f),
            grain = 0.15f, vignette = 0.2f, leakChance = 0f, dateByDefault = false, polaroidFrame = false,
            splitShadow = Color.rgb(60, 20, 110), splitHighlight = Color.rgb(200, 255, 80), duotone = true,
        ),
        Film(
            id = "outono", name = "Outono", description = "Matte e escuro: pretos lavados, brancos apagados, cores marrons",
            matrix = outonoMatrix(),
            grain = 0.15f, vignette = 0.25f, leakChance = 0f, dateByDefault = false, polaroidFrame = false,
            dream = 0.12f, gamma = 1.7f,
        ),
        Film(
            id = "cidade", name = "Cidade", description = "Matte urbano: pretos bem lavados, tom quente neutro",
            matrix = cidadeMatrix(),
            grain = 0.12f, vignette = 0.15f, leakChance = 0f, dateByDefault = false, polaroidFrame = false,
            preGamma = 1.55f,
        ),
        Film(
            id = "nuvem", name = "Nuvem Rosa", description = "Céu e luzes rosados, sombras azul-petróleo",
            matrix = nuvemMatrix(),
            grain = 0.1f, vignette = 0.15f, leakChance = 0f, dateByDefault = false, polaroidFrame = false,
        ),
        Film(
            id = "frio", name = "Frio", description = "Cores apagadas e frias, quase cinza azulado",
            matrix = frioMatrix(),
            grain = 0.15f, vignette = 0.2f, leakChance = 0f, dateByDefault = false, polaroidFrame = false,
            preGamma = 1.83f, gamma = 0.50f,
            splitShadow = Color.rgb(37, 46, 0), splitHighlight = Color.rgb(50, 205, 40), splitAmount = 1f,
        ),
        Film(
            id = "esmeralda", name = "Esmeralda", description = "Tudo verde-esmeralda, luzes creme",
            matrix = esmeraldaMatrix(),
            grain = 0.2f, vignette = 0.2f, leakChance = 0f, dateByDefault = false, polaroidFrame = false,
            preGamma = 0.76f, gamma = 1.33f,
            splitShadow = Color.rgb(164, 0, 160), splitHighlight = Color.rgb(128, 255, 255), splitAmount = 1f,
        ),
        Film(
            id = "gotico", name = "Gótico", description = "Sombras verde-petróleo escuras, clima de névoa",
            matrix = goticoMatrix(),
            grain = 0.2f, vignette = 0.3f, leakChance = 0f, dateByDefault = false, polaroidFrame = false,
            preGamma = 0.67f, gamma = 1.22f,
            splitShadow = Color.rgb(117, 158, 151), splitHighlight = Color.rgb(27, 255, 0), splitAmount = 1f,
        ),
        Film(
            id = "etereo", name = "Etéreo", description = "Contraste alto, brancos brilhantes e sombras frias",
            matrix = etereoMatrix(),
            grain = 0.15f, vignette = 0.25f, leakChance = 0f, dateByDefault = false, polaroidFrame = false,
            preGamma = 0.77f, gamma = 1.40f,
            splitShadow = Color.rgb(114, 161, 168), splitHighlight = Color.rgb(255, 255, 117), splitAmount = 1f,
        ),

        // --- Arte ---
        Film(
            id = "cianotipia", name = "Cianotipia", description = "Impressão azul antiga, como planta de arquiteto",
            matrix = look(saturation = 1f, contrast = 1.1f, warmth = 0f, fade = 0f),
            grain = 0.3f, vignette = 0.35f, leakChance = 0f, dateByDefault = false, polaroidFrame = false,
            splitShadow = Color.rgb(14, 38, 92), splitHighlight = Color.rgb(226, 236, 242), duotone = true, dust = 0.25f,
        ),
        Film(
            id = "riso", name = "Risografia", description = "Duas tintas, pontinhos e grão de gráfica",
            matrix = look(saturation = 1f, contrast = 1.15f, warmth = 0f, fade = 0f),
            grain = 0.5f, vignette = 0f, leakChance = 0f, dateByDefault = false, polaroidFrame = false,
            splitShadow = Color.rgb(0, 90, 170), splitHighlight = Color.rgb(255, 120, 170), duotone = true,
            fx = mapOf(com.netero.grao.gl.Fx.HALFTONE to 0.45f),
        ),
        Film(
            id = "popart", name = "Pop Art", description = "Cores berrantes com pontos de retícula",
            matrix = look(saturation = 1.8f, contrast = 1.3f, warmth = 0f, fade = 0f),
            grain = 0f, vignette = 0f, leakChance = 0f, dateByDefault = false, polaroidFrame = false,
            fx = mapOf(com.netero.grao.gl.Fx.HALFTONE to 0.6f, com.netero.grao.gl.Fx.COMIC to 0.35f),
        ),
        Film(
            id = "quadrinho", name = "Gibi", description = "Cores chapadas e contorno de desenho",
            matrix = look(saturation = 1.35f, contrast = 1.1f, warmth = 0.03f, fade = 0f),
            grain = 0f, vignette = 0f, leakChance = 0f, dateByDefault = false, polaroidFrame = false,
            fx = mapOf(com.netero.grao.gl.Fx.COMIC to 0.9f),
        ),
        Film(
            id = "jornal", name = "Jornal", description = "Preto e branco em pontinhos, papel de jornal",
            matrix = look(saturation = 0f, contrast = 1.25f, warmth = 0f, fade = 0.1f),
            grain = 0.2f, vignette = 0f, leakChance = 0f, dateByDefault = false, polaroidFrame = false,
            fx = mapOf(com.netero.grao.gl.Fx.HALFTONE to 0.85f),
        ),
        Film(
            id = "infra", name = "Infravermelho", description = "Folhas e plantas viram rosa e vermelho",
            matrix = ColorMatrix(
                floatArrayOf(
                    0.15f, 0.95f, -0.05f, 0f, 0f,
                    0.85f, 0.1f, 0.05f, 0f, 0f,
                    -0.05f, 0.1f, 0.95f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f,
                )
            ),
            grain = 0.3f, vignette = 0.3f, leakChance = 0f, dateByDefault = false, polaroidFrame = false,
        ),
        Film(
            id = "negativo", name = "Negativo", description = "Cores invertidas, como o filme antes de revelar",
            matrix = ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f,
                )
            ),
            grain = 0.3f, vignette = 0f, leakChance = 0f, dateByDefault = false, polaroidFrame = false,
        ),
        Film(
            id = "termica", name = "Térmica", description = "Visão de câmera de calor",
            matrix = look(saturation = 1f, contrast = 1.1f, warmth = 0f, fade = 0f),
            grain = 0.15f, vignette = 0.2f, leakChance = 0f, dateByDefault = false, polaroidFrame = false,
            fx = mapOf(com.netero.grao.gl.Fx.THERMAL to 1f),
        ),
        Film(
            id = "pixel8", name = "8 bits", description = "Pixels grandes e cores de videogame antigo",
            matrix = look(saturation = 1.4f, contrast = 1.2f, warmth = 0f, fade = 0f),
            grain = 0f, vignette = 0f, leakChance = 0f, dateByDefault = false, polaroidFrame = false,
            fx = mapOf(com.netero.grao.gl.Fx.PIXEL to 0.35f, com.netero.grao.gl.Fx.COMIC to 0.4f),
        ),
        Film(
            id = "duo_sol", name = "Duo Sol", description = "Duas cores: vinho e amarelo",
            matrix = look(saturation = 1f, contrast = 1.1f, warmth = 0f, fade = 0f),
            grain = 0.15f, vignette = 0.2f, leakChance = 0f, dateByDefault = false, polaroidFrame = false,
            splitShadow = Color.rgb(90, 10, 40), splitHighlight = Color.rgb(255, 210, 60), duotone = true,
        ),
        Film(
            id = "duo_menta", name = "Duo Menta", description = "Duas cores: verde-floresta e menta",
            matrix = look(saturation = 1f, contrast = 1.1f, warmth = 0f, fade = 0f),
            grain = 0.15f, vignette = 0.2f, leakChance = 0f, dateByDefault = false, polaroidFrame = false,
            splitShadow = Color.rgb(10, 50, 40), splitHighlight = Color.rgb(170, 255, 215), duotone = true,
        ),
        Film(
            id = "duo_noite", name = "Duo Noite", description = "Duas cores: preto e laranja neon",
            matrix = look(saturation = 1f, contrast = 1.2f, warmth = 0f, fade = 0f),
            grain = 0.2f, vignette = 0.25f, leakChance = 0f, dateByDefault = false, polaroidFrame = false,
            splitShadow = Color.rgb(12, 10, 20), splitHighlight = Color.rgb(255, 120, 40), duotone = true,
        ),

        // --- Sombrio ---
        Film(
            id = "noturno", name = "Noturno", description = "Noite azul profunda, pretos fechados",
            matrix = look(saturation = 0.7f, contrast = 1.15f, warmth = -0.2f, fade = 0f, b = 1.1f),
            grain = 0.35f, vignette = 0.6f, leakChance = 0f, dateByDefault = false, polaroidFrame = false,
            gamma = 1.35f, splitShadow = Color.rgb(20, 40, 110), splitHighlight = Color.rgb(170, 190, 230), splitAmount = 0.6f,
        ),
        Film(
            id = "lua", name = "Luar", description = "Luz prateada da lua, sombras escuras e brilho frio",
            matrix = look(saturation = 0.45f, contrast = 1.2f, warmth = -0.12f, fade = 0f, b = 1.05f),
            grain = 0.25f, vignette = 0.55f, leakChance = 0f, dateByDefault = false, polaroidFrame = false,
            gamma = 1.25f, glow = 0.35f, splitShadow = Color.rgb(30, 45, 80), splitHighlight = Color.rgb(220, 230, 255), splitAmount = 0.5f,
        ),
        Film(
            id = "vampiro", name = "Vampiro", description = "Quase sem cor, só o vermelho escuro sobrevive",
            matrix = vampiroMatrix(),
            grain = 0.35f, vignette = 0.6f, leakChance = 0f, dateByDefault = false, polaroidFrame = false,
            gamma = 1.2f, splitShadow = Color.rgb(70, 0, 15), splitHighlight = Color.rgb(200, 190, 190), splitAmount = 0.5f,
        ),
        Film(
            id = "floresta", name = "Floresta Sombria", description = "Verde escuro e úmido de mata fechada",
            matrix = look(saturation = 0.75f, contrast = 1.15f, warmth = -0.05f, fade = 0.05f, r = 0.9f, g = 1.05f, b = 0.92f),
            grain = 0.35f, vignette = 0.55f, leakChance = 0f, dateByDefault = false, polaroidFrame = false,
            gamma = 1.3f, splitShadow = Color.rgb(15, 50, 35), splitHighlight = Color.rgb(200, 215, 180), splitAmount = 0.6f,
            dream = 0.15f,
        ),
        Film(
            id = "nevoa", name = "Névoa", description = "Neblina cinza, tudo lavado e misterioso",
            matrix = look(saturation = 0.5f, contrast = 0.85f, warmth = -0.05f, fade = 0.45f),
            grain = 0.3f, vignette = 0.35f, leakChance = 0f, dateByDefault = false, polaroidFrame = false,
            dream = 0.55f, splitShadow = Color.rgb(70, 80, 90), splitHighlight = Color.rgb(215, 220, 225), splitAmount = 0.4f,
        ),
        Film(
            id = "tempestade", name = "Tempestade", description = "Azul-aço, contraste duro de céu carregado",
            matrix = look(saturation = 0.55f, contrast = 1.35f, warmth = -0.15f, fade = 0f),
            grain = 0.4f, vignette = 0.5f, leakChance = 0f, dateByDefault = false, polaroidFrame = false,
            gamma = 1.15f, splitShadow = Color.rgb(25, 40, 60), splitHighlight = Color.rgb(190, 205, 215), splitAmount = 0.5f,
        ),
        Film(
            id = "ruina", name = "Ruína", description = "Bleach bypass: sem cor, metálico e duro",
            matrix = look(saturation = 0.35f, contrast = 1.45f, warmth = 0f, fade = 0f, g = 1.02f),
            grain = 0.45f, vignette = 0.45f, leakChance = 0f, dateByDefault = false, polaroidFrame = false,
            gamma = 1.1f,
        ),
        Film(
            id = "assombrado", name = "Assombrado", description = "Foto antiga escura, sépia sujo com poeira",
            matrix = look(saturation = 0f, contrast = 1.15f, warmth = 0f, fade = 0.1f, r = 1.1f, g = 0.95f, b = 0.75f),
            grain = 0.6f, vignette = 0.85f, leakChance = 0f, dateByDefault = false, polaroidFrame = false,
            gamma = 1.35f, dust = 0.9f,
        ),
        Film(
            id = "crepusculo", name = "Crepúsculo", description = "Fim de tarde roxo, sombras violeta",
            matrix = look(saturation = 0.85f, contrast = 1.1f, warmth = 0.05f, fade = 0.05f, r = 1.02f, g = 0.9f, b = 1.08f),
            grain = 0.25f, vignette = 0.5f, leakChance = 0.1f, dateByDefault = false, polaroidFrame = false,
            gamma = 1.25f, splitShadow = Color.rgb(55, 20, 90), splitHighlight = Color.rgb(255, 170, 150), splitAmount = 0.65f,
        ),
        Film(
            id = "abismo", name = "Abismo", description = "Quase tudo preto, só as luzes aparecem em verde-azulado",
            matrix = look(saturation = 0.6f, contrast = 1.3f, warmth = -0.1f, fade = 0f),
            grain = 0.4f, vignette = 0.8f, leakChance = 0f, dateByDefault = false, polaroidFrame = false,
            gamma = 1.6f, halation = 0.3f, halationColor = Color.rgb(60, 200, 190),
            splitShadow = Color.rgb(5, 25, 30), splitHighlight = Color.rgb(160, 220, 210), splitAmount = 0.6f,
        ),
        Film(
            id = "ouro", name = "Ouro", description = "Dourado brilhante com reflexos de luz",
            matrix = look(saturation = 1.05f, contrast = 1.15f, warmth = 0.18f, fade = 0f, b = 0.8f),
            grain = 0.1f, vignette = 0.35f, leakChance = 0f, dateByDefault = false, polaroidFrame = false,
            glow = 0.35f,
        ),
    )

    fun byId(id: String?): Film = all.firstOrNull { it.id == id } ?: all.first()

    fun categoryOf(id: String): FilmCategory = FilmCategories.map[id] ?: FilmCategory.FILME
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

/** "Nuvem Rosa": transformação medida no terceiro vídeo de referência. */
internal fun nuvemMatrix(): ColorMatrix = ColorMatrix(
    floatArrayOf(
        0.943f, 0.559f, -0.233f, 0f, -27.5f,
        -0.189f, 1.211f, 0.015f, 0f, -0.5f,
        -0.018f, 0.21f, 0.828f, 0f, -1.3f,
        0f, 0f, 0f, 1f, 0f,
    )
)

/** Filtros medidos em vídeos de referência (antes/depois). */
internal fun frioMatrix(): ColorMatrix = ColorMatrix(
    floatArrayOf(
        0.986f, -0.384f, 0.670f, 0f, 6.5f,
        0.157f, 0.281f, 0.314f, 0f, 0.3f,
        0.071f, 0.346f, 0.935f, 0f, 10.7f,
        0f, 0f, 0f, 1f, 0f,
    )
)

internal fun esmeraldaMatrix(): ColorMatrix = ColorMatrix(
    floatArrayOf(
        1.408f, -0.774f, 0.494f, 0f, -28.7f,
        0.104f, 0.270f, 0.172f, 0f, 80.8f,
        0.388f, -0.082f, 0.604f, 0f, -38.4f,
        0f, 0f, 0f, 1f, 0f,
    )
)

internal fun goticoMatrix(): ColorMatrix = ColorMatrix(
    floatArrayOf(
        1.639f, 0.061f, 0.006f, 0f, -102.0f,
        0.011f, 1.057f, 0.004f, 0f, -47.3f,
        0.017f, 0.056f, 1.498f, 0f, -67.9f,
        0f, 0f, 0f, 1f, 0f,
    )
)

internal fun etereoMatrix(): ColorMatrix = ColorMatrix(
    floatArrayOf(
        1.325f, 0.287f, 0.029f, 0f, -102.0f,
        0.084f, 1.500f, 0.028f, 0f, -102.0f,
        0.085f, 0.284f, 1.258f, 0f, -78.0f,
        0f, 0f, 0f, 1f, 0f,
    )
)


/** "Vampiro": tira a cor de tudo, menos de uma parte do vermelho. */
internal fun vampiroMatrix(): ColorMatrix {
    val m = ColorMatrix()
    m.setSaturation(0.25f)
    m.postConcat(
        ColorMatrix(
            floatArrayOf(
                1.35f, -0.2f, -0.15f, 0f, 0f,
                0f, 0.92f, 0f, 0f, 0f,
                0f, 0f, 0.92f, 0f, 0f,
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
