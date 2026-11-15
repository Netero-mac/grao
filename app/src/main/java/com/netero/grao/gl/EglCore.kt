package com.netero.grao.gl

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface

/** Contexto OpenGL ES 2 próprio (sem tela), usado pela câmera e pelo editor. */
class EglCore {

    val display: EGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
    private val config: EGLConfig
    val context: EGLContext

    init {
        check(display != EGL14.EGL_NO_DISPLAY) { "Sem display EGL" }
        val version = IntArray(2)
        check(EGL14.eglInitialize(display, version, 0, version, 1)) { "eglInitialize falhou" }
        config = chooseConfig()
        context = EGL14.eglCreateContext(
            display, config, EGL14.EGL_NO_CONTEXT,
            intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0,
        )
        check(context != EGL14.EGL_NO_CONTEXT) { "eglCreateContext falhou" }
    }

    private fun chooseConfig(): EGLConfig {
        // Tenta do mais completo (gravável, janela + pbuffer) para o mais simples
        val surfaceTypes = intArrayOf(EGL14.EGL_WINDOW_BIT or EGL14.EGL_PBUFFER_BIT, EGL14.EGL_PBUFFER_BIT)
        for (recordable in listOf(true, false)) {
            for (type in surfaceTypes) {
                val attribs = mutableListOf(
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_ALPHA_SIZE, 8,
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_SURFACE_TYPE, type,
                )
                if (recordable) {
                    attribs += EGLExt.EGL_RECORDABLE_ANDROID
                    attribs += 1
                }
                attribs += EGL14.EGL_NONE
                val configs = arrayOfNulls<EGLConfig>(1)
                val num = IntArray(1)
                if (EGL14.eglChooseConfig(display, attribs.toIntArray(), 0, configs, 0, 1, num, 0) && num[0] > 0) {
                    configs[0]?.let { return it }
                }
            }
        }
        error("Nenhuma configuração EGL compatível")
    }

    fun createWindowSurface(surface: Any): EGLSurface {
        val s = EGL14.eglCreateWindowSurface(display, config, surface, intArrayOf(EGL14.EGL_NONE), 0)
        check(s != EGL14.EGL_NO_SURFACE) { "eglCreateWindowSurface falhou" }
        return s
    }

    fun createPbuffer(w: Int = 1, h: Int = 1): EGLSurface {
        val s = EGL14.eglCreatePbufferSurface(
            display, config,
            intArrayOf(EGL14.EGL_WIDTH, w, EGL14.EGL_HEIGHT, h, EGL14.EGL_NONE), 0,
        )
        check(s != EGL14.EGL_NO_SURFACE) { "eglCreatePbufferSurface falhou" }
        return s
    }

    fun makeCurrent(s: EGLSurface) {
        check(EGL14.eglMakeCurrent(display, s, s, context)) { "eglMakeCurrent falhou" }
    }

    fun swap(s: EGLSurface) = EGL14.eglSwapBuffers(display, s)

    fun setPresentationTime(s: EGLSurface, nanos: Long) {
        EGLExt.eglPresentationTimeANDROID(display, s, nanos)
    }

    fun releaseSurface(s: EGLSurface) {
        EGL14.eglDestroySurface(display, s)
    }

    fun release() {
        EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
        EGL14.eglDestroyContext(display, context)
    }
}
