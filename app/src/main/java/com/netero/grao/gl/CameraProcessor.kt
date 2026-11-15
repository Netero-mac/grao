package com.netero.grao.gl

import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.opengl.EGLSurface
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import androidx.camera.core.CameraEffect
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceProcessor
import androidx.camera.core.SurfaceRequest
import androidx.core.util.Consumer
import java.util.concurrent.Executor

/**
 * Recebe os quadros da câmera, aplica o shader de efeitos na GPU e entrega
 * para o visor e para a gravação de vídeo. Assim o que se vê é o que se grava.
 */
class CameraProcessor : SurfaceProcessor {

    private val thread = HandlerThread("grao-camera-gl").apply { start() }
    private val handler = Handler(thread.looper)
    val executor: Executor = Executor { r -> handler.post(r) }

    private var egl: EglCore? = null
    private var pbuffer: EGLSurface? = null
    private var program: EffectProgram? = null

    private var oesTex = 0
    private var surfaceTexture: SurfaceTexture? = null
    private val outputs = LinkedHashMap<SurfaceOutput, EGLSurface>()
    private val texMatrix = FloatArray(16)
    private val outMatrix = FloatArray(16)

    private var decalTex = 0
    private var decalKey: Any? = null
    private var released = false

    @Volatile
    var params: EffectParams = EffectParams()

    private var cachedFor: EffectParams? = null
    private var cachedUniforms: Uniforms? = null

    init {
        handler.post {
            try {
                val e = EglCore()
                val pb = e.createPbuffer()
                e.makeCurrent(pb)
                program = EffectProgram(oes = true)
                egl = e
                pbuffer = pb
            } catch (t: Throwable) {
                Log.e(TAG, "Falha ao iniciar GL da câmera", t)
            }
        }
    }

    override fun onInputSurface(request: SurfaceRequest) {
        val e = egl
        if (released || e == null) {
            request.willNotProvideSurface()
            return
        }
        e.makeCurrent(pbuffer!!)
        val tex = EffectProgram.createOesTexture()
        val st = SurfaceTexture(tex)
        st.setDefaultBufferSize(request.resolution.width, request.resolution.height)
        val surface = Surface(st)
        request.provideSurface(surface, executor) {
            st.setOnFrameAvailableListener(null)
            if (surfaceTexture === st) surfaceTexture = null
            st.release()
            surface.release()
            egl?.let { core -> pbuffer?.let { core.makeCurrent(it) } }
            EffectProgram.deleteTexture(tex)
            if (oesTex == tex) oesTex = 0
        }
        st.setOnFrameAvailableListener({ onFrame(it) }, handler)
        surfaceTexture = st
        oesTex = tex
    }

    override fun onOutputSurface(output: SurfaceOutput) {
        val e = egl
        if (released || e == null) {
            output.close()
            return
        }
        val surface = output.getSurface(executor) {
            outputs.remove(output)?.let { s -> egl?.releaseSurface(s) }
            output.close()
        }
        try {
            outputs[output] = e.createWindowSurface(surface)
        } catch (t: Throwable) {
            Log.e(TAG, "Falha ao criar saída", t)
            output.close()
        }
    }

    private fun onFrame(st: SurfaceTexture) {
        val e = egl ?: return
        val prog = program ?: return
        if (released || st !== surfaceTexture) return
        try {
            e.makeCurrent(pbuffer!!)
            st.updateTexImage()
            st.getTransformMatrix(texMatrix)

            val p = params
            val u = if (p === cachedFor) cachedUniforms!! else p.resolve().also {
                cachedFor = p
                cachedUniforms = it
            }
            val first = outputs.keys.firstOrNull()
            if (first != null) ensureDecal(p, first.size.width, first.size.height)

            val time = (st.timestamp / 1_000_000_000.0).toFloat() % 1000f
            for ((out, surf) in outputs) {
                e.makeCurrent(surf)
                out.updateTransformMatrix(outMatrix, texMatrix)
                prog.draw(
                    tex = oesTex, texMatrix = outMatrix, u = u,
                    width = out.size.width, height = out.size.height,
                    time = time, seed = (p.seed % 997).toFloat(), yDown = false, decalTex = decalTex,
                )
                e.setPresentationTime(surf, st.timestamp)
                e.swap(surf)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Erro ao desenhar quadro", t)
        }
    }

    private fun ensureDecal(p: EffectParams, w: Int, h: Int) {
        val k = p.decalKey()
        val full = Pair(k, w * 10_000 + h)
        if (full == decalKey) return
        decalKey = full
        EffectProgram.deleteTexture(decalTex)
        decalTex = 0
        if (!Decal.isEmpty(k)) {
            // Decalque em resolução menor no visor: mais leve
            val scale = 720f / maxOf(w, h)
            val dw = (w * minOf(1f, scale)).toInt().coerceAtLeast(1)
            val dh = (h * minOf(1f, scale)).toInt().coerceAtLeast(1)
            val bmp: Bitmap = Decal.draw(k, dw, dh)
            decalTex = EffectProgram.createTexture2D(bmp)
            bmp.recycle()
        }
    }

    fun release() {
        handler.post {
            released = true
            val e = egl
            if (e != null) {
                pbuffer?.let { e.makeCurrent(it) }
                outputs.values.forEach { e.releaseSurface(it) }
                outputs.keys.forEach { it.close() }
                outputs.clear()
                surfaceTexture?.release()
                EffectProgram.deleteTexture(oesTex)
                EffectProgram.deleteTexture(decalTex)
                program?.release()
                pbuffer?.let { e.releaseSurface(it) }
                e.release()
            }
            egl = null
            thread.quitSafely()
        }
    }

    companion object {
        private const val TAG = "GraoCamGL"
    }
}

/** Liga o processador ao visor e ao vídeo do CameraX. */
class GraoCameraEffect(val processor: CameraProcessor) : CameraEffect(
    CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE,
    processor.executor,
    processor,
    Consumer<Throwable> { Log.e("GraoCamGL", "Erro no efeito da câmera", it) },
)
