package com.netero.grao.film

import android.graphics.Color
import android.graphics.ColorMatrix
import com.netero.grao.gl.Fx

/**
 * Pacote grande de filtros, organizado por categoria.
 * Cada linha usa os mesmos "botões" do shader: matriz de cor (look), curvas,
 * split toning, grão, vinheta, brilho e efeitos embutidos.
 */
internal object FilmsExtra {

    val categories = HashMap<String, FilmCategory>()

    private fun mk(
        cat: FilmCategory,
        id: String,
        name: String,
        desc: String,
        sat: Float = 1f,
        con: Float = 1f,
        warm: Float = 0f,
        fade: Float = 0f,
        r: Float = 1f,
        g: Float = 1f,
        b: Float = 1f,
        bOff: Float = 0f,
        rOff: Float = 0f,
        matrix: ColorMatrix? = null,
        grain: Float = 0.2f,
        vig: Float = 0.3f,
        leak: Float = 0f,
        date: Boolean = false,
        polaroid: Boolean = false,
        shadow: Int? = null,
        high: Int? = null,
        split: Float = 0f,
        glow: Float = 0f,
        dream: Float = 0f,
        halation: Float = 0f,
        halationColor: Int = Color.rgb(255, 70, 40),
        dust: Float = 0f,
        chroma: Float = 0f,
        vhs: Boolean = false,
        duo: Boolean = false,
        gamma: Float = 1f,
        pre: Float = 1f,
        fx: Map<Fx, Float> = emptyMap(),
    ): Film {
        categories[id] = cat
        return Film(
            id = id, name = name, description = desc,
            matrix = matrix ?: look(sat, con, warm, fade, r, g, b, rOff = rOff, bOff = bOff),
            grain = grain, vignette = vig, leakChance = leak, dateByDefault = date, polaroidFrame = polaroid,
            halation = halation, halationColor = halationColor, dust = dust, chroma = chroma, vhs = vhs,
            splitShadow = shadow, splitHighlight = high, splitAmount = split,
            glow = glow, dream = dream, duotone = duo, gamma = gamma, preGamma = pre, fx = fx,
        )
    }

    private fun rgb(r: Int, g: Int, b: Int) = Color.rgb(r, g, b)

    /** Gira as cores (efeito "ácido"). */
    private fun hueShift(deg: Float, sat: Float): ColorMatrix {
        val m = ColorMatrix()
        m.setSaturation(sat)
        val rot = ColorMatrix()
        val a = Math.toRadians(deg.toDouble())
        val c = Math.cos(a).toFloat()
        val s = Math.sin(a).toFloat()
        // rotação de matiz em torno do eixo cinza
        rot.set(
            floatArrayOf(
                0.213f + c * 0.787f - s * 0.213f, 0.715f - c * 0.715f - s * 0.715f, 0.072f - c * 0.072f + s * 0.928f, 0f, 0f,
                0.213f - c * 0.213f + s * 0.143f, 0.715f + c * 0.285f + s * 0.140f, 0.072f - c * 0.072f - s * 0.283f, 0f, 0f,
                0.213f - c * 0.213f - s * 0.787f, 0.715f - c * 0.715f + s * 0.715f, 0.072f + c * 0.928f + s * 0.072f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            )
        )
        m.postConcat(rot)
        return m
    }

    private val F = FilmCategory.FILME
    private val PB = FilmCategory.PB
    private val CAM = FilmCategory.CAMERAS
    private val RET = FilmCategory.RETRO
    private val MAT = FilmCategory.MATTE
    private val CIN = FilmCategory.CINEMA
    private val SOM = FilmCategory.SOMBRIO
    private val NEO = FilmCategory.NEON
    private val ART = FilmCategory.ARTE

    val list: List<Film> = listOf(
        // ---------- Filme ----------
        mk(F, "portra160", "Portra 160", "Pastel suave, pele delicada e luz macia",
            sat = 0.85f, con = 0.95f, warm = 0.06f, fade = 0.2f, b = 0.95f, grain = 0.15f, vig = 0.2f, leak = 0.1f),
        mk(F, "portra800", "Portra 800", "Quente e granulado para pouca luz",
            sat = 1.0f, con = 1.05f, warm = 0.11f, fade = 0.15f, b = 0.9f, grain = 0.45f, vig = 0.35f, leak = 0.15f),
        mk(F, "ultramax", "UltraMax 400", "Cores fortes e quentes de filme barato",
            sat = 1.2f, con = 1.1f, warm = 0.1f, fade = 0.08f, b = 0.9f, grain = 0.35f, vig = 0.3f, leak = 0.2f),
        mk(F, "colorplus", "ColorPlus 200", "Amarelado, nostálgico, grão visível",
            sat = 1.05f, con = 1.0f, warm = 0.14f, fade = 0.15f, g = 1.02f, b = 0.82f, grain = 0.4f, vig = 0.35f, leak = 0.2f),
        mk(F, "c200", "Fuji 200", "Verdes frescos e tons frios do dia a dia",
            sat = 1.05f, con = 1.05f, warm = -0.04f, fade = 0.12f, r = 0.96f, g = 1.04f, grain = 0.3f, vig = 0.25f, leak = 0.1f),
        mk(F, "lomo800", "Lomo 800", "Saturado, vermelho forte e contraste",
            sat = 1.35f, con = 1.2f, warm = 0.08f, fade = 0.05f, r = 1.05f, grain = 0.5f, vig = 0.45f, leak = 0.25f),
        mk(F, "agfa", "Vista 200", "Vermelhos quentes e céu ciano",
            sat = 1.25f, con = 1.1f, warm = 0.06f, fade = 0.1f, r = 1.06f, b = 1.0f, grain = 0.3f, vig = 0.3f, leak = 0.15f,
            shadow = rgb(30, 90, 110), high = rgb(255, 200, 160), split = 0.3f),
        mk(F, "natura", "Natura 1600", "Filme de noite: grão grosso e tons frios",
            sat = 0.95f, con = 1.05f, warm = -0.06f, fade = 0.15f, grain = 0.7f, vig = 0.4f),

        // ---------- P&B ----------
        mk(PB, "delta3200", "Delta 3200", "Preto e branco com grão enorme",
            sat = 0f, con = 1.15f, fade = 0.1f, grain = 0.95f, vig = 0.4f),
        mk(PB, "panf", "Pan F 50", "Preto e branco liso e contrastado",
            sat = 0f, con = 1.35f, grain = 0.08f, vig = 0.3f),
        mk(PB, "acros", "Acros", "Pretos profundos e cinzas limpos",
            sat = 0f, con = 1.2f, gamma = 1.1f, grain = 0.15f, vig = 0.25f),
        mk(PB, "selenio", "Selênio", "P&B virado: sombras frias arroxeadas",
            sat = 0f, con = 1.2f, grain = 0.25f, vig = 0.35f,
            shadow = rgb(70, 60, 110), high = rgb(240, 235, 225), split = 0.5f),
        mk(PB, "prata", "Prata", "Clarinho, cinzas suaves e brilho",
            sat = 0f, con = 0.9f, fade = 0.25f, grain = 0.2f, vig = 0.15f, glow = 0.2f),
        mk(PB, "carvao", "Carvão", "Escuro e pesado, quase só sombras",
            sat = 0f, con = 1.3f, gamma = 1.45f, grain = 0.45f, vig = 0.6f),
        mk(PB, "ir_pb", "IR P&B", "Infravermelho: folhas brancas e céu negro",
            matrix = ColorMatrix(
                floatArrayOf(
                    0.3f, 1.1f, -0.4f, 0f, 0f,
                    0.3f, 1.1f, -0.4f, 0f, 0f,
                    0.3f, 1.1f, -0.4f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f,
                )
            ),
            grain = 0.4f, vig = 0.4f, glow = 0.3f),
        mk(PB, "lith", "Lith", "Revelação lith: sombras duras e luzes cor de chá",
            sat = 0f, con = 1.35f, grain = 0.55f, vig = 0.45f,
            shadow = rgb(30, 20, 15), high = rgb(240, 200, 160), split = 0.7f),

        // ---------- Câmeras ----------
        mk(CAM, "instax", "Instax", "Clarinha, fria e com moldura branca",
            sat = 0.95f, con = 0.95f, warm = -0.05f, fade = 0.2f, grain = 0.15f, vig = 0.2f, polaroid = true, glow = 0.15f),
        mk(CAM, "polaroid_vencida", "Polaroid Vencida", "Filme instantâneo vencido: cores tortas e lavadas",
            sat = 0.7f, con = 0.85f, fade = 0.4f, grain = 0.35f, vig = 0.35f, polaroid = true, leak = 0.3f,
            shadow = rgb(40, 110, 120), high = rgb(255, 230, 160), split = 0.6f),
        mk(CAM, "holga", "Holga", "Câmera de plástico: vinheta escura e foco macio",
            sat = 1.15f, con = 1.15f, warm = 0.05f, grain = 0.4f, vig = 0.9f, leak = 0.35f, dream = 0.3f),
        mk(CAM, "diana", "Diana", "Pastel sonhador com vazamentos de luz",
            sat = 0.9f, con = 0.95f, warm = 0.06f, fade = 0.25f, grain = 0.35f, vig = 0.6f, leak = 0.5f, dream = 0.25f),
        mk(CAM, "pinhole", "Pinhole", "Câmera de furo: tudo macio e vinheta enorme",
            sat = 0.85f, con = 1.05f, warm = 0.08f, fade = 0.1f, grain = 0.5f, vig = 1f, dream = 0.5f),
        mk(CAM, "celular2005", "Celular 2005", "Câmera de celular antigo: estourada e com ruído",
            sat = 1.3f, con = 1.2f, warm = 0.05f, grain = 0.55f, vig = 0.2f, chroma = 0.35f, date = true,
            fx = mapOf(Fx.PIXEL to 0.05f)),
        mk(CAM, "webcam", "Webcam", "Webcam dos anos 2000: fria, lavada e ruidosa",
            sat = 0.8f, con = 0.9f, warm = -0.1f, fade = 0.2f, grain = 0.6f, vig = 0.3f, chroma = 0.25f),
        mk(CAM, "flash_festa", "Flash de Festa", "Flash estourado no rosto, fundo escuro",
            sat = 1.1f, con = 1.3f, warm = 0.06f, gamma = 1.2f, grain = 0.3f, vig = 0.7f, date = true, glow = 0.25f),

        // ---------- Retrô ----------
        mk(RET, "anos50", "Anos 50", "Cores ricas de slide antigo",
            sat = 1.3f, con = 1.15f, warm = 0.05f, fade = 0.1f, r = 1.05f, grain = 0.3f, vig = 0.4f),
        mk(RET, "anos60", "Anos 60", "Technicolor pastel: azul-turquesa e laranja",
            sat = 1.1f, con = 1.0f, warm = 0.05f, fade = 0.25f, grain = 0.3f, vig = 0.4f,
            shadow = rgb(40, 140, 150), high = rgb(255, 210, 150), split = 0.5f),
        mk(RET, "anos80", "Anos 80", "Magenta no ar, brilho e grão",
            sat = 1.15f, con = 1.05f, fade = 0.15f, r = 1.04f, g = 0.95f, b = 1.04f, grain = 0.35f, vig = 0.3f, glow = 0.2f, leak = 0.2f),
        mk(RET, "anos90", "Anos 90", "Verde de minilab, data laranja e grão",
            sat = 1.05f, con = 1.05f, warm = 0.04f, fade = 0.15f, g = 1.05f, grain = 0.4f, vig = 0.35f, date = true, leak = 0.3f),
        mk(RET, "technicolor", "Technicolor", "Primárias fortíssimas de filme antigo",
            sat = 1.55f, con = 1.2f, warm = 0.03f, grain = 0.2f, vig = 0.3f),
        mk(RET, "kodachrome", "Kodachrome", "Vermelho profundo, azul denso e contraste",
            sat = 1.3f, con = 1.25f, warm = 0.04f, r = 1.05f, b = 1.02f, gamma = 1.1f, grain = 0.2f, vig = 0.3f),
        mk(RET, "postal", "Cartão Postal", "Impressão desbotada: ciano e amarelo",
            sat = 0.85f, con = 0.95f, fade = 0.35f, grain = 0.3f, vig = 0.3f,
            shadow = rgb(60, 140, 160), high = rgb(255, 235, 170), split = 0.55f),
        mk(RET, "album_vo", "Álbum da Vó", "Foto de álbum antigo: laranja desbotado e poeira",
            sat = 0.7f, con = 0.9f, warm = 0.2f, fade = 0.4f, b = 0.8f, grain = 0.45f, vig = 0.5f, dust = 0.45f),

        // ---------- Matte ----------
        mk(MAT, "lavanda", "Lavanda", "Matte lilás e suave",
            sat = 0.8f, con = 0.92f, fade = 0.3f, r = 1.02f, g = 0.95f, b = 1.05f, grain = 0.15f, vig = 0.2f,
            shadow = rgb(120, 100, 170), high = rgb(240, 225, 245), split = 0.4f),
        mk(MAT, "cafe", "Café", "Marrom aconchegante, pretos lavados",
            sat = 0.75f, con = 0.95f, warm = 0.15f, fade = 0.3f, b = 0.85f, grain = 0.25f, vig = 0.3f,
            shadow = rgb(70, 45, 30), high = rgb(240, 215, 180), split = 0.45f),
        mk(MAT, "areia", "Areia", "Bege quente e desbotado",
            sat = 0.7f, con = 0.9f, warm = 0.12f, fade = 0.35f, b = 0.88f, grain = 0.15f, vig = 0.15f),
        mk(MAT, "nordico", "Nórdico", "Branco frio e limpo, minimalista",
            sat = 0.65f, con = 0.95f, warm = -0.1f, fade = 0.25f, grain = 0.1f, vig = 0.1f),
        mk(MAT, "pessego", "Pêssego", "Rosado alaranjado e macio",
            sat = 0.9f, con = 0.92f, warm = 0.1f, fade = 0.28f, r = 1.04f, g = 0.97f, grain = 0.12f, vig = 0.15f,
            shadow = rgb(150, 100, 100), high = rgb(255, 210, 180), split = 0.4f),
        mk(MAT, "oliva", "Oliva", "Verde-oliva e marrom terroso",
            sat = 0.75f, con = 0.95f, warm = 0.08f, fade = 0.3f, g = 1.04f, b = 0.85f, grain = 0.25f, vig = 0.25f,
            shadow = rgb(70, 80, 50), high = rgb(230, 220, 180), split = 0.4f),
        mk(MAT, "cinza_suave", "Cinza Suave", "Quase sem cor, tudo baixo e calmo",
            sat = 0.4f, con = 0.9f, fade = 0.3f, grain = 0.2f, vig = 0.2f),
        mk(MAT, "bruma", "Bruma", "Leve neblina clara e pastel",
            sat = 0.8f, con = 0.85f, warm = 0.02f, fade = 0.35f, grain = 0.1f, vig = 0.1f, dream = 0.45f),

        // ---------- Cinema ----------
        mk(CIN, "blockbuster", "Blockbuster", "Azul-petróleo e laranja fortes, contraste de cinema",
            sat = 1.15f, con = 1.2f, warm = 0.04f, grain = 0.15f, vig = 0.4f,
            shadow = rgb(0, 110, 130), high = rgb(255, 160, 80), split = 1f),
        mk(CIN, "western", "Western", "Deserto quente e duro, poeira no ar",
            sat = 0.8f, con = 1.25f, warm = 0.22f, fade = 0.05f, b = 0.8f, grain = 0.35f, vig = 0.5f, dust = 0.2f),
        mk(CIN, "matrix", "Código Verde", "Tudo esverdeado e frio, clima de ficção",
            sat = 0.7f, con = 1.2f, warm = -0.05f, r = 0.85f, g = 1.12f, b = 0.85f, grain = 0.2f, vig = 0.45f),
        mk(CIN, "deserto", "Duna", "Laranja e areia, céu pálido",
            sat = 0.9f, con = 1.1f, warm = 0.2f, fade = 0.12f, b = 0.82f, grain = 0.2f, vig = 0.35f,
            shadow = rgb(110, 60, 30), high = rgb(255, 220, 170), split = 0.5f),
        mk(CIN, "bleach_cor", "Bleach", "Cores lavadas e metálicas, contraste alto",
            sat = 0.55f, con = 1.4f, grain = 0.3f, vig = 0.4f),
        mk(CIN, "scifi", "Sci-Fi", "Ciano gelado e metálico",
            sat = 0.75f, con = 1.15f, warm = -0.15f, b = 1.06f, grain = 0.15f, vig = 0.4f, glow = 0.15f,
            shadow = rgb(10, 60, 80), high = rgb(200, 240, 255), split = 0.6f),
        mk(CIN, "romance", "Romance", "Brilho dourado e macio de filme romântico",
            sat = 1.0f, con = 0.95f, warm = 0.12f, fade = 0.15f, grain = 0.12f, vig = 0.3f, glow = 0.35f, dream = 0.2f),
        mk(CIN, "pastel_retro", "Pastel Simétrico", "Rosa, amarelo e menta em tons de bala",
            sat = 1.0f, con = 0.95f, warm = 0.05f, fade = 0.2f, grain = 0.12f, vig = 0.15f,
            shadow = rgb(120, 170, 160), high = rgb(255, 215, 200), split = 0.6f),

        // ---------- Sombrio ----------
        mk(SOM, "sangue", "Sangue", "Duas cores: preto e vermelho escuro",
            grain = 0.3f, vig = 0.5f, shadow = rgb(8, 2, 4), high = rgb(200, 30, 40), duo = true),
        mk(SOM, "cripta", "Cripta", "Verde-cinza de pedra úmida",
            sat = 0.4f, con = 1.15f, g = 1.04f, gamma = 1.3f, grain = 0.4f, vig = 0.65f,
            shadow = rgb(25, 40, 30), high = rgb(190, 200, 180), split = 0.5f),
        mk(SOM, "sombra", "Sombra", "Luz baixa: só os destaques aparecem",
            sat = 0.8f, con = 1.25f, gamma = 1.7f, grain = 0.35f, vig = 0.7f),
        mk(SOM, "inverno_escuro", "Inverno Escuro", "Azul-acinzentado gelado e pesado",
            sat = 0.5f, con = 1.1f, warm = -0.15f, gamma = 1.25f, grain = 0.3f, vig = 0.5f, dream = 0.15f),
        mk(SOM, "pesadelo", "Pesadelo", "Sem cor, contorno trêmulo e cores separadas",
            sat = 0.3f, con = 1.3f, gamma = 1.2f, grain = 0.5f, vig = 0.75f, chroma = 0.5f,
            fx = mapOf(Fx.GLITCH to 0.15f)),

        // ---------- Neon ----------
        mk(NEO, "synthwave", "Synthwave", "Roxo e rosa com brilho de neon",
            sat = 1.3f, con = 1.1f, grain = 0.15f, vig = 0.4f, glow = 0.35f,
            shadow = rgb(60, 20, 140), high = rgb(255, 80, 200), split = 0.85f),
        mk(NEO, "tokyo", "Tóquio", "Noite de cidade: magenta, ciano e halação",
            sat = 1.2f, con = 1.2f, warm = -0.05f, grain = 0.25f, vig = 0.45f, halation = 0.4f,
            halationColor = rgb(255, 60, 160), shadow = rgb(0, 160, 200), high = rgb(255, 120, 200), split = 0.7f),
        mk(NEO, "laser", "Laser", "Verde e magenta de show de luz",
            sat = 1.35f, con = 1.15f, grain = 0.15f, vig = 0.4f, glow = 0.3f,
            shadow = rgb(0, 200, 120), high = rgb(255, 60, 220), split = 0.8f),
        mk(NEO, "acido", "Ácido", "Cores giradas e saturadas, psicodélico",
            matrix = hueShift(120f, 1.5f), grain = 0.2f, vig = 0.3f, glow = 0.2f),
        mk(NEO, "holo", "Holográfico", "Pastel iridescente com arco-íris nas bordas",
            sat = 0.9f, con = 0.95f, fade = 0.2f, grain = 0.1f, vig = 0.1f, glow = 0.3f,
            shadow = rgb(150, 200, 255), high = rgb(255, 200, 240), split = 0.5f, fx = mapOf(Fx.PRISM to 0.35f)),
        mk(NEO, "neon_azul", "Neon Azul", "Azul elétrico brilhando",
            sat = 1.2f, con = 1.15f, warm = -0.2f, b = 1.1f, grain = 0.2f, vig = 0.45f, glow = 0.4f,
            shadow = rgb(0, 30, 120), high = rgb(120, 220, 255), split = 0.7f),
        mk(NEO, "rave", "Rave", "Luzes coloridas de festa e cores fortes",
            sat = 1.4f, con = 1.15f, grain = 0.2f, vig = 0.4f, glow = 0.25f, fx = mapOf(Fx.PARTY to 0.6f)),
        mk(NEO, "luz_negra", "Luz Negra", "Ultravioleta: roxo e brancos brilhando",
            sat = 1.1f, con = 1.2f, warm = -0.1f, r = 0.9f, g = 0.75f, b = 1.15f, grain = 0.25f, vig = 0.5f, glow = 0.45f,
            shadow = rgb(40, 0, 90), high = rgb(220, 200, 255), split = 0.7f),

        // ---------- Arte ----------
        mk(ART, "esboco", "Esboço", "Desenho a lápis em papel",
            sat = 0f, con = 1.4f, fade = 0.3f, grain = 0.3f, vig = 0f, fx = mapOf(Fx.COMIC to 1f)),
        mk(ART, "aquarela", "Aquarela", "Cores chapadas e suaves de pintura",
            sat = 1.1f, con = 0.95f, fade = 0.2f, grain = 0.1f, vig = 0.1f, dream = 0.4f, fx = mapOf(Fx.COMIC to 0.4f)),
        mk(ART, "serigrafia", "Serigrafia", "Duas tintas e retícula grossa",
            grain = 0.2f, vig = 0f, shadow = rgb(20, 40, 120), high = rgb(255, 210, 40), duo = true,
            fx = mapOf(Fx.HALFTONE to 0.6f)),
        mk(ART, "gravura", "Gravura Sépia", "Impressão antiga em pontos sépia",
            sat = 0f, con = 1.2f, r = 1.1f, g = 0.95f, b = 0.75f, grain = 0.3f, vig = 0.3f, fx = mapOf(Fx.HALFTONE to 0.7f)),
        mk(ART, "duo_oceano", "Duo Oceano", "Duas cores: azul-marinho e água",
            grain = 0.15f, vig = 0.2f, shadow = rgb(10, 25, 70), high = rgb(120, 240, 230), duo = true),
        mk(ART, "duo_uva", "Duo Uva", "Duas cores: roxo e lilás claro",
            grain = 0.15f, vig = 0.2f, shadow = rgb(45, 10, 70), high = rgb(230, 190, 255), duo = true),
        mk(ART, "vitral", "Vitral", "Imagem em gomos com cores fortes",
            sat = 1.4f, con = 1.15f, grain = 0f, vig = 0.2f, fx = mapOf(Fx.KALEIDO to 0.4f, Fx.COMIC to 0.3f)),
    )
}
