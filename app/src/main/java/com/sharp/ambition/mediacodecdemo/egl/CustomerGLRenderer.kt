package com.sharp.ambition.mediacodecdemo.egl

import android.opengl.GLES20
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import java.lang.ref.WeakReference

/**
 *    author : fengqiao
 *    date   : 2022/11/17 16:36
 *    desc   :
 */
class CustomerGLRenderer : SurfaceHolder.Callback {

    private val thread: RenderThread = RenderThread()

    private var surfaceView: WeakReference<SurfaceView>? = null

    private var drawer: IDrawer = TriangleDrawer()

    init {
        thread.start()
    }

    fun setSurface(surface: SurfaceView) {
        surfaceView = WeakReference(surface)
        surface.holder.addCallback(this)
        surface.addOnAttachStateChangeListener(object: View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
            }

            override fun onViewDetachedFromWindow(v: View) {
                thread.onSurfaceStop()
            }

        })
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        thread.onSurfaceCreate()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        thread.onSurfaceChange(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        thread.onSurfaceDestroy()
    }


    inner class RenderThread: Thread() {
        // 渲染状态
        private var state = RenderState.NO_SURFACE

        private var eglSurface: EGLSurfaceHolder? = null
        // 是否绑定了EGLSurface
        private var haveBindEGLContext = false
        //是否已经新建过EGL上下文，用于判断是否需要生产新的纹理ID
        private var neverCreateEglContext = true

        private var width = 0
        private var height = 0

        private val waitLock = Object()

        private fun holdOn() {
            synchronized(waitLock) {
                waitLock.wait()
            }
        }

        private fun notifyGo() {
            synchronized(waitLock) {
                waitLock.notify()
            }
        }

        fun onSurfaceCreate() {
            Log.e("TAG", "onSurfaceCreate")
            state = RenderState.FRESH_SURFACE
            notifyGo()
        }

        fun onSurfaceChange(width: Int, height: Int) {
            Log.e("TAG", "onSurfaceChange")
            this.width = width
            this.height = height
            state = RenderState.SURFACE_CHANGE
            notifyGo()
        }

        fun onSurfaceDestroy() {
            state = RenderState.SURFACE_DESTROY
            notifyGo()
        }

        fun onSurfaceStop() {
            state = RenderState.STOP
            notifyGo()
        }

        override fun run() {
            initEGL()
            while (true) {
                Log.e("TAG", "$state")
                when (state) {
                    RenderState.FRESH_SURFACE -> {
                        //【2】使用surface初始化EGLSurface，并绑定上下文
                        createEGLSurfaceFirst()
                        holdOn()
                    }
                    RenderState.SURFACE_CHANGE -> {
                        createEGLSurfaceFirst()
                        //【3】初始化OpenGL世界坐标系宽高
                        GLES20.glViewport(0, 0, width, height)
                        configWordSize()
                        state = RenderState.RENDERING
                    }
                    RenderState.RENDERING -> {
                        render()
                        //【4】进入循环渲染
                        sleep(200)
                    }
                    RenderState.SURFACE_DESTROY -> {
                        //【5】销毁EGLSurface，并解绑上下文
                        destroyEGLSurface()
                        state = RenderState.NO_SURFACE
                    }
                    RenderState.STOP -> {
                        //【6】释放所有资源
                        releaseEGL()
                        return
                    }
                    else -> {
                        holdOn()
                    }
                }
            }
        }

        private fun initEGL() {
            eglSurface = EGLSurfaceHolder().apply {
                init(null, EGL_RECORDABLE_ANDROID)
            }
        }

        private fun createEGLSurfaceFirst() {
            if (!haveBindEGLContext) {
                haveBindEGLContext = true
                createEGLSurface()
                if (neverCreateEglContext) {
                    neverCreateEglContext = false
                    generateTextureID()
                }
            }
        }

        private fun createEGLSurface() {
            eglSurface?.let {
                val surface = surfaceView?.get()?.holder?.surface
                Log.e("TAG", "surface => $surface")
                it.createEGLSurface(surface)
                it.makeCurrent()
            }
        }

        private fun destroyEGLSurface() {
            eglSurface?.destroyEGLSurface()
            haveBindEGLContext = false
        }

        private fun releaseEGL() {
            eglSurface?.release()
        }

        private fun generateTextureID() {
            val texture = IntArray(1)
            GLES20.glGenTextures(1, texture, 0) //生成纹理
            //drawer.setTextureID(texture[0])
        }

        private fun configWordSize() {
            drawer.setWorldSize(width, height)
        }

        private fun render() {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
            drawer.draw()
            eglSurface?.swapBuffers()
        }

    }

}