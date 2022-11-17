package com.sharp.ambition.mediacodecdemo.egl

import android.graphics.SurfaceTexture

/**
 *    author : fengqiao
 *    date   : 2022/11/17 17:04
 *    desc   :
 */
interface IDrawer {
    fun setVideoSize(videoW: Int, videoH: Int)
    fun setWorldSize(worldW: Int, worldH: Int)
    fun draw()
    fun release()
}