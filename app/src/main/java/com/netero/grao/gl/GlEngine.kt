package com.netero.grao.gl

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.Matrix
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Renderizador fora da tela: aplica o shader de efeitos numa foto (editor e foto final)
 * e gera o vídeo do efeito 3D. Tudo roda numa thread própria com o seu contexto EGL.
 */
object GlEngine {

    private val dispatcher = Executors.newSingleThreadExecutor { r -> Thread(r, "grao-gl") }.asCoroutineDispatcher()

    private var egl: EglCore? = null
    private var pbuffer: EGLSurface? = null
    private var program: EffectProgram? = null
    private var maxTexture = 2048

    private val flipY = FloatArray(16).also {
        Matrix.setIdentityM(it, 0)
        Matrix.translateM(it, 0, 0f, 1f, 0f)
        Matrix.scaleM(it, 0, 1f, -1f, 1f)
    }

    private fun ensure() {
        if (egl != null) return
        val e = EglCore()
        val pb = e.createPbuffer()
        e.makeCurrent(pb)
        program = EffectProgram(oes = false)
        val v = IntArray(1)
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, v, 0)
        maxTexture = if (v[0] > 0) min(v[0], 4096) else 2048
        egl = e
        pbuffer = pb
    }

    /** Cria o contexto e compila o shader antecipadamente. */
    suspend fun warmUp() = withContext(dispatcher) { ensure() }

    /** Aplica filtro + efeitos + decalque e (opcionalmente) a moldura. */
    suspend fun render(
        src: Bitmap,
        params: EffectParams,
        maxSide: Int = 2400,
        time: Float = 0f,
        withFrame: Boolean = true,
    ): Bitmap = withContext(dispatcher) {
        ensure()
        val e = egl!!
        e.makeCurrent(pbuffer!!)
        val input = fit(software(src), min(maxSide, maxTexture))
        val w = input.width
        val h = input.height

        val inTex = EffectProgram.createTexture2D(input)
        val outTex = EffectProgram.createTexture2D(null)
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, w, h, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)
        val fbo = IntArray(1)
        GLES20.glGenFramebuffers(1, fbo, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo[0])
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, outTex, 0)

        val key = params.decalKey()
        val decalBmp = if (Decal.isEmpty(key)) null else Decal.draw(key, w, h)
        val decalTex = decalBmp?.let { EffectProgram.createTexture2D(it) } ?: 0

        try {
            check(GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) == GLES20.GL_FRAMEBUFFER_COMPLETE) {
                "Framebuffer incompleto"
            }
            program!!.draw(
                tex = inTex, texMatrix = null, u = params.resolve(), width = w, height = h,
                time = time, seed = (params.seed % 997).toFloat(), yDown = true, decalTex = decalTex,
            )
            val buf = ByteBuffer.allocateDirect(w * h * 4).order(ByteOrder.nativeOrder())
            GLES20.glReadPixels(0, 0, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf)
            buf.rewind()
            val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            out.copyPixelsFromBuffer(buf)
            if (withFrame) Decal.applyFrame(out, params.effectiveFrame) else out
        } finally {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
            GLES20.glDeleteFramebuffers(1, fbo, 0)
            EffectProgram.deleteTexture(inTex)
            EffectProgram.deleteTexture(outTex)
            EffectProgram.deleteTexture(decalTex)
            decalBmp?.recycle()
            if (input !== src) input.recycle()
        }
    }

    /**
     * Efeito 3D: gera um vídeo MP4 curto com a foto "balançando" em profundidade.
     */
    suspend fun renderMotion(
        src: Bitmap,
        params: EffectParams,
        outFile: File,
        amount: Float,
        seconds: Int = 4,
        fps: Int = 30,
    ) = withContext(dispatcher) {
        ensure()
        val e = egl!!
        e.makeCurrent(pbuffer!!)

        val fitted = fit(software(src), 1280)
        val w = max(16, fitted.width / 16 * 16)
        val h = max(16, fitted.height / 16 * 16)
        val input = if (fitted.width == w && fitted.height == h) fitted else Bitmap.createScaledBitmap(fitted, w, h, true)

        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, w, h).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, 8_000_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val inputSurface = codec.createInputSurface()
        codec.start()
        val muxer = MediaMuxer(outFile.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val win = e.createWindowSurface(inputSurface)
        e.makeCurrent(win)

        val inTex = EffectProgram.createTexture2D(input)
        val key = params.decalKey()
        val decalBmp = if (Decal.isEmpty(key)) null else Decal.draw(key, w, h)
        val decalTex = decalBmp?.let { EffectProgram.createTexture2D(it) } ?: 0

        val info = MediaCodec.BufferInfo()
        var track = -1
        var muxing = false

        fun drain(end: Boolean) {
            if (end) codec.signalEndOfInputStream()
            var guard = 0
            while (true) {
                val idx = codec.dequeueOutputBuffer(info, 10_000)
                when {
                    idx == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        if (!end || ++guard > 300) return
                    }
                    idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        track = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        muxing = true
                    }
                    idx >= 0 -> {
                        val buf = codec.getOutputBuffer(idx)
                        if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                        if (buf != null && info.size > 0 && muxing) {
                            buf.position(info.offset)
                            buf.limit(info.offset + info.size)
                            muxer.writeSampleData(track, buf, info)
                        }
                        codec.releaseOutputBuffer(idx, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                    }
                }
            }
        }

        try {
            val u = params.resolve()
            val frames = seconds * fps
            for (i in 0 until frames) {
                drain(false)
                val a = (i.toFloat() / frames) * 2f * PI.toFloat()
                program!!.draw(
                    tex = inTex, texMatrix = flipY, u = u, width = w, height = h,
                    time = i / fps.toFloat(), seed = (params.seed % 997).toFloat(), yDown = false,
                    decalTex = decalTex,
                    depthOffsetX = cos(a) * 0.03f * amount,
                    depthOffsetY = sin(a) * 0.02f * amount,
                )
                e.setPresentationTime(win, i * 1_000_000_000L / fps)
                e.swap(win)
            }
            drain(true)
        } finally {
            e.makeCurrent(pbuffer!!)
            e.releaseSurface(win)
            EffectProgram.deleteTexture(inTex)
            EffectProgram.deleteTexture(decalTex)
            decalBmp?.recycle()
            runCatching { codec.stop() }
            codec.release()
            inputSurface.release()
            if (muxing) runCatching { muxer.stop() }
            muxer.release()
            if (input !== src && input !== fitted) input.recycle()
            if (fitted !== src) fitted.recycle()
        }
    }

    private fun software(b: Bitmap): Bitmap =
        if (b.config == Bitmap.Config.ARGB_8888) b else b.copy(Bitmap.Config.ARGB_8888, false)

    fun fit(b: Bitmap, maxSide: Int): Bitmap {
        val m = max(b.width, b.height)
        if (m <= maxSide) return b
        val s = maxSide.toFloat() / m
        return Bitmap.createScaledBitmap(b, max(1, (b.width * s).toInt()), max(1, (b.height * s).toInt()), true)
    }
}
