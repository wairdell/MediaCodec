package com.sharp.ambition.mediacodecdemo

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 *    author : fengqiao
 *    date   : 2022/11/8 14:47
 *    desc   :
 */
class DisplayShapeRenderer: GLSurfaceView.Renderer {

    private var triangle: Triangle? = null

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        GLES30.glClearColor(0F, 0F, 0F, 0F)
        triangle = Triangle()
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        triangle?.draw()
    }
}