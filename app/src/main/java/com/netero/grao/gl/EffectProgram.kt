package com.netero.grao.gl

import android.graphics.Bitmap
import android.graphics.Color
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/** Programa GLSL com o shader de efeitos. Precisa de um contexto EGL atual. */
class EffectProgram(private val oes: Boolean) {

    private val program: Int
    private val locs = HashMap<String, Int>()
    private val aPos: Int
    private val aUv: Int

    private val quad: FloatBuffer = floatBuffer(
        -1f, -1f, 0f, 0f,
        1f, -1f, 1f, 0f,
        -1f, 1f, 0f, 1f,
        1f, 1f, 1f, 1f,
    )

    private val colorMat = FloatArray(16)
    private val colorOff = FloatArray(4)
    private val identity = FloatArray(16).also { Matrix.setIdentityM(it, 0) }

    init {
        val vs = compile(GLES20.GL_VERTEX_SHADER, Shaders.VERTEX)
        val fs = compile(GLES20.GL_FRAGMENT_SHADER, Shaders.fragment(oes))
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vs)
        GLES20.glAttachShader(program, fs)
        GLES20.glLinkProgram(program)
        val ok = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, ok, 0)
        if (ok[0] == 0) {
            val log = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            error("Falha ao linkar shader: $log")
        }
        GLES20.glDeleteShader(vs)
        GLES20.glDeleteShader(fs)
        aPos = GLES20.glGetAttribLocation(program, "aPos")
        aUv = GLES20.glGetAttribLocation(program, "aUv")
    }

    private fun loc(name: String): Int = locs.getOrPut(name) { GLES20.glGetUniformLocation(program, name) }

    /**
     * Desenha a textura de entrada com os efeitos no framebuffer atual.
     * @param yDown true quando a coordenada v cresce para baixo na imagem (texturas 2D de bitmap)
     */
    fun draw(
        tex: Int,
        texMatrix: FloatArray?,
        u: Uniforms,
        width: Int,
        height: Int,
        time: Float,
        seed: Float,
        yDown: Boolean,
        decalTex: Int = 0,
        depthOffsetX: Float = 0f,
        depthOffsetY: Float = 0f,
    ) {
        GLES20.glViewport(0, 0, width, height)
        GLES20.glUseProgram(program)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(if (oes) GLES11Ext.GL_TEXTURE_EXTERNAL_OES else GLES20.GL_TEXTURE_2D, tex)
        GLES20.glUniform1i(loc("uTex"), 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, decalTex)
        GLES20.glUniform1i(loc("uDecal"), 1)
        GLES20.glUniform1f(loc("uDecalOn"), if (decalTex != 0) 1f else 0f)

        GLES20.glUniformMatrix4fv(loc("uTexMatrix"), 1, false, texMatrix ?: identity, 0)

        // ColorMatrix do Android (4x5, linhas, offsets 0..255) -> mat4 em colunas + vec4
        val a = u.colorMatrix
        for (row in 0 until 4) {
            for (col in 0 until 4) colorMat[col * 4 + row] = a[row * 5 + col]
            colorOff[row] = a[row * 5 + 4] / 255f
        }
        GLES20.glUniformMatrix4fv(loc("uColorMat"), 1, false, colorMat, 0)
        GLES20.glUniform4fv(loc("uColorOff"), 1, colorOff, 0)

        f("uFilterMix", u.filterMix)
        f("uExposure", u.exposure)
        f("uContrast", u.contrast)
        f("uSaturation", u.saturation)
        f("uTemp", u.temperature)
        f("uTint", u.tint)
        f("uFade", u.fade)
        f("uSharpen", u.sharpen)
        f("uVignette", u.vignette)
        f("uGrain", u.grain)
        f("uChroma", u.chroma)
        f("uHalation", u.halation)
        color3("uHalationColor", u.halationColor)
        f("uGlow", u.glow)
        f("uSparkle", u.sparkle)
        f("uPrism", u.prism)
        f("uGlitch", u.glitch)
        f("uScanlines", u.scanlines)
        f("uLeak", u.leak)
        GLES20.glUniform2f(loc("uLeakPos"), u.leakX, u.leakY)
        color3("uLeakColor", u.leakColor)
        color3("uSplitS", u.splitShadow)
        color3("uSplitH", u.splitHighlight)
        f("uSplitAmt", u.splitAmount)
        f("uSoft", if (u.vhsSoften) 0.8f else 0f)
        f("uTime", time)
        f("uSeed", seed)
        GLES20.glUniform2f(loc("uRes"), width.toFloat(), height.toFloat())
        f("uYDown", if (yDown) 1f else 0f)
        GLES20.glUniform2f(loc("uDepthOffset"), depthOffsetX, depthOffsetY)
        f("uGlitter", u.glitter)
        f("uParty", u.party)
        f("uSideLight", u.sideLight)
        f("uRainbow", u.rainbow)
        f("uDream", u.dream)
        f("uPixel", u.pixel)
        f("uFisheye", u.fisheye)
        f("uMirror", u.mirror)
        f("uKaleido", u.kaleido)
        f("uComic", u.comic)
        f("uHalftone", u.halftone)
        f("uThermal", u.thermal)
        f("uDuotone", u.duotone)
        f("uGamma", u.gamma)
        f("uPreGamma", u.preGamma)
        f("uLightLine", u.lightLine)
        f("uDust", u.dust)

        quad.position(0)
        GLES20.glVertexAttribPointer(aPos, 2, GLES20.GL_FLOAT, false, 16, quad)
        GLES20.glEnableVertexAttribArray(aPos)
        quad.position(2)
        GLES20.glVertexAttribPointer(aUv, 2, GLES20.GL_FLOAT, false, 16, quad)
        GLES20.glEnableVertexAttribArray(aUv)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(aPos)
        GLES20.glDisableVertexAttribArray(aUv)
    }

    private fun f(name: String, v: Float) = GLES20.glUniform1f(loc(name), v)

    private fun color3(name: String, c: Int) =
        GLES20.glUniform3f(loc(name), Color.red(c) / 255f, Color.green(c) / 255f, Color.blue(c) / 255f)

    fun release() = GLES20.glDeleteProgram(program)

    companion object {
        fun compile(type: Int, src: String): Int {
            val s = GLES20.glCreateShader(type)
            GLES20.glShaderSource(s, src)
            GLES20.glCompileShader(s)
            val ok = IntArray(1)
            GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, ok, 0)
            if (ok[0] == 0) {
                val log = GLES20.glGetShaderInfoLog(s)
                GLES20.glDeleteShader(s)
                error("Falha ao compilar shader: $log")
            }
            return s
        }

        fun floatBuffer(vararg v: Float): FloatBuffer =
            ByteBuffer.allocateDirect(v.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(v)
                position(0)
            }

        fun createTexture2D(bitmap: Bitmap?): Int {
            val ids = IntArray(1)
            GLES20.glGenTextures(1, ids, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ids[0])
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            if (bitmap != null) GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
            return ids[0]
        }

        fun createOesTexture(): Int {
            val ids = IntArray(1)
            GLES20.glGenTextures(1, ids, 0)
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, ids[0])
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            return ids[0]
        }

        fun deleteTexture(id: Int) {
            if (id != 0) GLES20.glDeleteTextures(1, intArrayOf(id), 0)
        }
    }
}
