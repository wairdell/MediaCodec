package com.sharp.ambition.mediacodecdemo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.android.rtmpvideo.RtmpActivity
import com.sharp.ambition.mediacodecdemo.egl.EGLPlayerActivity


class MainActivity : AppCompatActivity() {

    companion object {
        val TAG = MainActivity::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.btn_record_from_projection).setOnClickListener {
            startActivity(Intent(this, RecordFromProjectionActivity::class.java))
        }
        findViewById<Button>(R.id.btn_record_from_camera_surface).setOnClickListener {
            startActivity(Intent(this, RecordFromCamera2SurfaceActivity::class.java))
        }
        findViewById<Button>(R.id.btn_record_from_camera_yuv).setOnClickListener {
            startActivity(Intent(this, RecordFromCamera2YUVActivity::class.java))
        }
        findViewById<Button>(R.id.btn_character_animation_from_camera).setOnClickListener {
            startActivity(Intent(this, RtmpActivity::class.java))
        }
        findViewById<Button>(R.id.btn_opengl).setOnClickListener {
            startActivity(Intent(this, OpenGLActivity::class.java))
        }
        findViewById<Button>(R.id.btn_egl).setOnClickListener {
            startActivity(Intent(this, EGLPlayerActivity::class.java))
        }
    }
}