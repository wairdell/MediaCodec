package com.sharp.ambition.mediacodecdemo.egl

import android.opengl.EGLContext
import android.opengl.EGLSurface

/**
 *    author : fengqiao
 *    date   : 2022/11/17 16:19
 *    desc   :
 */
class EGLSurfaceHolder {

    companion object {
        private val TAG = EGLSurfaceHolder::class.java.simpleName
    }

    private lateinit var eglCore: EGLCore

    private var eglSurface: EGLSurface? = null

    fun init(shareContext: EGLContext? = null, flags: Int) {
        eglCore = EGLCore()
        eglCore.init(shareContext, flags)
    }

    fun createEGLSurface(surface: Any?, width: Int = -1, height: Int = -1) {
        eglSurface = if (surface != null) eglCore.createWindowSurface(surface) else eglCore.createOffscreenSurface(width, height)
    }

    fun makeCurrent() {
        eglSurface?.let { eglCore.makeCurrent(it) }
    }

    fun swapBuffers() {
        eglSurface?.let { eglCore.swapBuffers(it) }
    }

    fun destroyEGLSurface() {
        eglSurface?.let {
            eglCore.destroySurface(it)
            eglSurface = null
        }
    }

    fun release() {
        eglCore.release()
    }

}