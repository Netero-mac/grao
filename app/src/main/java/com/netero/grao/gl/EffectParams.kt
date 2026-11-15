package com.netero.grao.gl

import android.graphics.Color
import com.netero.grao.film.Film
import com.netero.grao.film.Films

/** Efeitos que podem ser empilhados por cima do filme. */
enum class Fx(val label: String, val description: String) {
    GRAIN("Grão", "Grão de filme extra"),
    LEAK("Light leak", "Vazamento de luz laranja"),
    GLOW("Brilho", "Glow suave nas luzes"),
    SPARKLE("Diamante", "Estrelinhas nos pontos de luz"),
    HALATION("Halação", "Brilho vermelho de filme de cinema"),
    PRISM("Prisma", "Arco-íris nas bordas"),
    RGB("RGB Split", "Canais de cor separados"),
    GLITCH("Glitch", "Faixas deslocadas, imagem com defeito"),
    VHS("VHS", "Linhas de fita e PLAY na tela"),
    DUST("Poeira", "Poeira, fios e arranhões"),
    MOTION3D("3D", "Foto em movimento 3D (vira vídeo)"),
}

/** Ajustes manuais, de -1 a 1 (alguns só de 0 a 1). */
enum class Adj(val label: String, val bipolar: Boolean) {
    EXPOSURE("Exposição", true),
    CONTRAST("Contraste", true),
    SATURATION("Saturação", true),
    TEMPERATURE("Temperatura", true),
    TINT("Matiz", true),
    FADE("Fade", false),
    SHARPEN("Nitidez", false),
    VIGNETTE("Vinheta", false),
}

enum class Frame(val label: String) {
    NONE("Nenhuma"),
    POLAROID("Polaroid"),
    WHITE("Borda branca"),
    FILM35("Filme 35mm"),
    CINEMA("Cinema"),
}

enum class TextPos(val label: String) { TOP("Topo"), CENTER("Centro"), BOTTOM("Base") }

enum class TextStyleKind(val label: String) { MONO("Máquina"), SERIF("Serifa"), BOLD("Forte") }

data class TextLayer(
    val text: String = "",
    val pos: TextPos = TextPos.BOTTOM,
    val style: TextStyleKind = TextStyleKind.MONO,
    val color: Int = Color.rgb(242, 232, 213),
)

/** Tudo que define o visual de uma imagem. Imutável: cada mudança gera uma cópia. */
data class EffectParams(
    val filmId: String = Films.all.first().id,
    val filterMix: Float = 1f,
    val effects: Map<Fx, Float> = emptyMap(),
    val adjust: Map<Adj, Float> = emptyMap(),
    val dateOn: Boolean = false,
    val text: TextLayer = TextLayer(),
    val frame: Frame = Frame.NONE,
    /** Sorteado por foto: light leak automático do filme, posição e cor. */
    val seed: Long = 1L,
    val autoLeak: Boolean = false,
) {
    val film: Film get() = Films.byId(filmId)

    fun fx(f: Fx) = effects[f] ?: 0f
    fun adj(a: Adj) = adjust[a] ?: 0f

    fun withFx(f: Fx, v: Float) = copy(effects = effects + (f to v.coerceIn(0f, 1f)))
    fun withAdj(a: Adj, v: Float) = copy(adjust = adjust + (a to v.coerceIn(-1f, 1f)))

    /** Moldura efetiva: o filme Polaroid já vem com a sua. */
    val effectiveFrame: Frame get() = if (frame == Frame.NONE && film.polaroidFrame) Frame.POLAROID else frame

    fun resolve(): Uniforms {
        val f = film
        val m = filterMix
        val rnd = kotlin.random.Random(seed)
        val leakColors = intArrayOf(Color.rgb(255, 106, 42), Color.rgb(232, 64, 42), Color.rgb(255, 179, 71))
        val leakColor = leakColors[rnd.nextInt(leakColors.size)]
        val leakX = if (rnd.nextBoolean()) -0.1f else 1.1f
        val leakY = rnd.nextFloat()
        val auto = if (autoLeak && rnd.nextFloat() < f.leakChance) 0.8f * m else 0f

        val shadow = f.splitShadow ?: Color.GRAY
        val high = f.splitHighlight ?: Color.GRAY

        return Uniforms(
            colorMatrix = f.matrix.array.copyOf(),
            filterMix = m,
            exposure = adj(Adj.EXPOSURE),
            contrast = adj(Adj.CONTRAST) * 0.6f,
            saturation = adj(Adj.SATURATION),
            temperature = adj(Adj.TEMPERATURE),
            tint = adj(Adj.TINT),
            fade = adj(Adj.FADE).coerceAtLeast(0f),
            sharpen = adj(Adj.SHARPEN).coerceAtLeast(0f),
            vignette = (f.vignette * m + adj(Adj.VIGNETTE).coerceAtLeast(0f)).coerceIn(0f, 1.2f),
            grain = (f.grain * m + fx(Fx.GRAIN)).coerceIn(0f, 1.2f),
            chroma = (f.chroma * m + fx(Fx.RGB)).coerceIn(0f, 1.5f),
            halation = (f.halation * m + fx(Fx.HALATION)).coerceIn(0f, 1.5f),
            halationColor = if (f.halation > 0f && fx(Fx.HALATION) == 0f) f.halationColor else Color.rgb(255, 60, 35),
            glow = (f.glow * m + fx(Fx.GLOW)).coerceIn(0f, 1.5f),
            sparkle = fx(Fx.SPARKLE),
            prism = fx(Fx.PRISM),
            glitch = fx(Fx.GLITCH),
            scanlines = ((if (f.vhs) 1f else 0f) * m + fx(Fx.VHS)).coerceIn(0f, 1f),
            leak = (auto + fx(Fx.LEAK)).coerceIn(0f, 1f),
            leakX = leakX,
            leakY = leakY,
            leakColor = leakColor,
            splitShadow = shadow,
            splitHighlight = high,
            splitAmount = f.splitAmount * m,
            vhsSoften = f.vhs && m > 0.5f || fx(Fx.VHS) > 0.5f,
        )
    }

    /** O que precisa ser desenhado por cima (decalque): muda pouco, por isso tem chave própria. */
    data class DecalKey(
        val dust: Float,
        val dateOn: Boolean,
        val vhsText: Boolean,
        val text: TextLayer,
        val seed: Long,
    )

    fun decalKey(): DecalKey = DecalKey(
        dust = (film.dust * filterMix + fx(Fx.DUST)).coerceIn(0f, 1.2f),
        dateOn = dateOn,
        vhsText = (film.vhs && filterMix > 0.5f) || fx(Fx.VHS) > 0.5f,
        text = text,
        seed = seed,
    )
}

/** Valores já calculados que vão direto para o shader. */
data class Uniforms(
    val colorMatrix: FloatArray,
    val filterMix: Float,
    val exposure: Float,
    val contrast: Float,
    val saturation: Float,
    val temperature: Float,
    val tint: Float,
    val fade: Float,
    val sharpen: Float,
    val vignette: Float,
    val grain: Float,
    val chroma: Float,
    val halation: Float,
    val halationColor: Int,
    val glow: Float,
    val sparkle: Float,
    val prism: Float,
    val glitch: Float,
    val scanlines: Float,
    val leak: Float,
    val leakX: Float,
    val leakY: Float,
    val leakColor: Int,
    val splitShadow: Int,
    val splitHighlight: Int,
    val splitAmount: Float,
    val vhsSoften: Boolean,
) {
    override fun equals(other: Any?) = this === other
    override fun hashCode() = System.identityHashCode(this)
}
