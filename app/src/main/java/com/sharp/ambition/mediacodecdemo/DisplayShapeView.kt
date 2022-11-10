package com.sharp.ambition.mediacodecdemo

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet

/**
 *    author : fengqiao
 *    date   : 2022/11/8 14:45
 *    desc   :
 */
class DisplayShapeView @JvmOverloads constructor(pContext: Context, attrs: AttributeSet? = null) : GLSurfaceView(pContext, attrs) {

    private val renderer: GLSurfaceView.Renderer

    init {
        // 设置版本号
        setEGLContextClientVersion(3)
        // 创建 Renderer
//        renderer = DisplayShapeRenderer()
        renderer = ImageRenderer(pContext)
        setRenderer(renderer)
        // 渲染模式设置为 仅在调用 requestRender() 时才渲染，减少不必要的绘制
        renderMode = RENDERMODE_WHEN_DIRTY
    }

}