package com.sharp.ambition.mediacodecdemo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.opengl.*
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 *    author : fengqiao
 *    date   : 2022/11/10 15:05
 *    desc   :
 */
class VideoRenderer(val context: Context): GLSurfaceView.Renderer {

    private var programId = -1
    private var textureId: Int = 0
    lateinit var surfaceTexture: SurfaceTexture

    init {

    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {

        GLES30.glClearColor(0.0F, 0.0F, 0.0F, 0.0F)

        val texture = IntArray(1)
        GLES30.glGenTextures(1, texture, 0)
        textureId = texture[0]
        surfaceTexture = SurfaceTexture(textureId)

        val vertexShaderId = loadShader(GLES30.GL_VERTEX_SHADER, getVertexShader())
        val fragmentShaderId = loadShader(GLES30.GL_FRAGMENT_SHADER, getFragmentShader())
        programId = GLES30.glCreateProgram()
        GLES30.glAttachShader(programId, vertexShaderId)
        GLES30.glAttachShader(programId, fragmentShaderId)
        GLES30.glLinkProgram(programId)
        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, linkStatus, 0)
        /*if (linkStatus[0] == 0) {
            GLES30.glDeleteProgram(programId)
        }*/
    }

    private var viewPortWidth = 0
    private var viewPortHeight = 0

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        viewPortWidth = width
        viewPortHeight = height
        GLES30.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10) {
        Log.d("TAG", "onDrawFrame() called with: gl = $gl")
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

        GLES30.glUseProgram(programId)
        val vertexPosHandler = GLES30.glGetAttribLocation(programId, "aPosition")
        val texturePosHandler = GLES30.glGetAttribLocation(programId, "aCoordinate")
        val textureHandler = GLES30.glGetUniformLocation(programId, "uTexture")
        val vertexMatrixHandler = GLES30.glGetUniformLocation(programId, "uMatrix")


        GLES30.glActiveTexture(GLES30.GL_TEXTURE)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES30.glUniform1i(textureHandler, 0)
        GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR.toFloat())
        GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR.toFloat())
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT)

        surfaceTexture.updateTexImage()

        GLES30.glEnableVertexAttribArray(vertexPosHandler)
        GLES30.glEnableVertexAttribArray(texturePosHandler)

        GLES30.glUniformMatrix4fv(vertexMatrixHandler, 1, false, mMatrix, 0)

        //设置着色器参数， 第二个参数表示一个顶点包含的数据数量，这里为xy，所以为2
        GLES30.glVertexAttribPointer(vertexPosHandler, 2, GLES30.GL_FLOAT, false, 0, vertexCoors.asFloatBuffer())
        GLES30.glVertexAttribPointer(texturePosHandler, 2, GLES30.GL_FLOAT, false, 0, textureCoors.asFloatBuffer())
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
    }

    private fun FloatArray.asFloatBuffer(): FloatBuffer {
        val cc = ByteBuffer.allocateDirect(size * Float.SIZE_BYTES)
        cc.order(ByteOrder.nativeOrder())
        return cc.asFloatBuffer().apply {
            put(this@asFloatBuffer)
            position(0)
        }
    }

    private val vertexCoors = floatArrayOf(
        -1f, -1f,
        1f, -1f,
        -1f, 1f,
        1f, 1f
    )

    private val textureCoors = floatArrayOf(
        0f, 1f,
        1f, 1f,
        0f, 0f,
        1f, 0f
    )

    private fun getVertexShader(): String {
        return "attribute vec4 aPosition;" +
                "uniform mat4 uMatrix;" +
                "attribute vec2 aCoordinate;" +
                "varying vec2 vCoordinate;" +
                "void main() {" +
                "  gl_Position = aPosition*uMatrix;;" +
                "  vCoordinate = aCoordinate;" +
                "}"
    }

    private fun getFragmentShader(): String {
        return "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float;" +
                "varying vec2 vCoordinate;" +
                "uniform samplerExternalOES uTexture;" +
                "void main() {" +
                "  vec4 color = texture2D(uTexture, vCoordinate);" +
                "  float gray = (color.r + color.g + color.b)/3.0;" +
                "  gl_FragColor = vec4(gray, gray, gray, 1.0);" +
                "}"
    }


    private fun loadShader(type: Int, code: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, code)
        GLES30.glCompileShader(shader)
        val compileStatus = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
        /*if (compileStatus[0] == 0) {
            GLES30.glDeleteShader(shader)
        }*/
        return shader
    }

    private val mMatrix: FloatArray = FloatArray(16)

    fun initDefMatrix(videoWidth: Int, videoHeight: Int) {
//        var prjMatrix = FloatArray(16)
        var prjMatrix = mMatrix
        val originRatio = videoWidth / videoHeight.toFloat()
        val worldRatio = viewPortWidth / viewPortHeight.toFloat()
        if (viewPortWidth > viewPortHeight) {
            if (originRatio > worldRatio) {
                val actualRatio = originRatio / worldRatio
                Matrix.orthoM(
                    prjMatrix, 0,
                    -1f, 1f,
                    -actualRatio, actualRatio,
                    -1f, 3f
                )
            } else {// 原始比例小于窗口比例，缩放高度度会导致高度超出，因此，高度以窗口为准，缩放宽度
                val actualRatio = worldRatio / originRatio
                Matrix.orthoM(
                    prjMatrix, 0,
                    -actualRatio, actualRatio,
                    -1f, 1f,
                    -1f, 3f
                )
            }
        } else {
            if (originRatio > worldRatio) {
                val actualRatio = originRatio / worldRatio
                Matrix.orthoM(
                    prjMatrix, 0,
                    -1f, 1f,
                    -actualRatio, actualRatio,
                    -1f, 3f
                )
            } else {// 原始比例小于窗口比例，缩放高度会导致高度超出，因此，高度以窗口为准，缩放宽度
                val actualRatio = worldRatio / originRatio
                Matrix.orthoM(
                    prjMatrix, 0,
                    -actualRatio, actualRatio,
                    -1f, 1f,
                    -1f, 3f
                )
            }
        }
    }


}