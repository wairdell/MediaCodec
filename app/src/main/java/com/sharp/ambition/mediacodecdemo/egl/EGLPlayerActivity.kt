package com.sharp.ambition.mediacodecdemo.egl

import android.os.Bundle
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import com.sharp.ambition.mediacodecdemo.R
import java.util.concurrent.Executors

/**
 *    author : fengqiao
 *    date   : 2022/11/17 16:50
 *    desc   :
 */
class EGLPlayerActivity: AppCompatActivity() {

    private val threadPool = Executors.newFixedThreadPool(10)
    private var renderer = CustomerGLRenderer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_egl_player)
        val surfaceView = findViewById<SurfaceView>(R.id.surface_view)
        renderer.setSurface(surfaceView)
    }

}