package com.sharp.ambition.mediacodecdemo

import android.opengl.GLSurfaceView
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 *    author : fengqiao
 *    date   : 2022/11/8 13:50
 *    desc   :
 */
class OpenGLActivity : AppCompatActivity() {

    private lateinit var gLSurfaceView: GLSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_opengl)
//        gLSurfaceView = findViewById<GLSurfaceView>(R.id.gl_surface_view)
//        gLSurfaceView.setRenderer(OpenGLRenderer())

        var displayShapeView = DisplayShapeView(this)
        displayShapeView.paramRenderer = ImageRenderer(this)
        setContentView(displayShapeView)
    }


    class OpenGLRenderer : GLSurfaceView.Renderer {

        private val trianVertices = floatArrayOf(
            0.0f, 2.0f, 0.0f, // 上顶点
            -2.0f, -2.0f, 0.0f, // 坐下点
            2.0f, -2.0f, 0.0f, // 右下点
        )
        private val trianBuffer: FloatBuffer

        private val quaterVertices = floatArrayOf(
            1.0f, 1.0f, 0.0f, -1.0f, 1.0f, 0.0f, 1.0f, -1.0f, 0.0f, -1.0f, -1.0f, 0.0f
        )
        private val quaterBuffer: FloatBuffer

        private val colorVertices = floatArrayOf(
            1.0f, 0.0f, 0.0f, 1.0f,
            0.0f, 1.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f, 1.0f,
        )
        private val colorBuffer: FloatBuffer

        init {
            val tbb = ByteBuffer.allocateDirect(trianVertices.size * 4)
            tbb.order(ByteOrder.nativeOrder())
            trianBuffer = tbb.asFloatBuffer()
            trianBuffer.put(trianVertices)
            trianBuffer.position(0)

            val qbb = ByteBuffer.allocateDirect(quaterVertices.size * 4)
            qbb.order(ByteOrder.nativeOrder())
            quaterBuffer = qbb.asFloatBuffer()
            quaterBuffer.put(quaterVertices)
            quaterBuffer.position(0)

            val cbb = ByteBuffer.allocateDirect(colorVertices.size * 4)
            cbb.order(ByteOrder.nativeOrder())
            colorBuffer = cbb.asFloatBuffer()
            colorBuffer.put(colorVertices)
            colorBuffer.position(0)
        }

        override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
            gl.glClearColor(0.0F, 0.0F, 0.0F, 0.5F)
            gl.glShadeModel(GL10.GL_SMOOTH)
            gl.glClearDepthf(1.0F)
            gl.glEnable(GL10.GL_DEPTH_TEST)
            gl.glDepthFunc(GL10.GL_LEQUAL)
            gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST)
        }

        override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
            val ratio = width / height.toFloat()
            gl.glViewport(0, 0, width, height)
            gl.glMatrixMode(GL10.GL_PROJECTION)
            gl.glLoadIdentity()
            gl.glFrustumf(-ratio, ratio, -1F, 1F, 1F, 10F)
            gl.glMatrixMode(GL10.GL_MODELVIEW)
            gl.glLoadIdentity()
        }

        override fun onDrawFrame(gl: GL10) {
            // 清除屏幕和深度缓存
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT or GL10.GL_DEPTH_BUFFER_BIT)
            // 重置当前的模型观察矩阵
            gl.glLoadIdentity()
            // 左移 1.5 单位，并移入屏幕 6.0
            gl.glTranslatef(-2.0F, -0.0F, -6.0F)
            gl.glColor4f(0.5F, 0.5F, 1.0F, 1.0F)
            // 启用定点数组
            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY)
            // 设置三角形顶点
            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, trianBuffer)
            // 绘制三角形
            gl.glDrawArrays(GL10.GL_TRIANGLES, 0, 3)

            /* 渲染正方形 */
            // 重置当前的模型观察矩阵
            gl.glLoadIdentity()
            // 左移 1.5 单位，并移入屏幕 6.0
            gl.glTranslatef(2.0F, 0.0F, -6.0F)
            gl.glRotatef(60F, 0F, 0F, -6.0F)
            //设置颜色
            //gl.glColor4f(1.0f, 0.0f, 0.0f, 1.0f)

            //设置颜色数组
            gl.glEnableClientState(GL10.GL_COLOR_ARRAY)
            gl.glColorPointer(4, GL10.GL_FLOAT, 0, colorBuffer)

            // 设置和绘制正方形
            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, quaterBuffer)
            gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4)
            //关闭颜色数组
            gl.glDisableClientState(GL10.GL_COLOR_ARRAY)
            // 取消顶点数组
            gl.glDisableClientState(GL10.GL_VERTEX_ARRAY)
        }

    }

}