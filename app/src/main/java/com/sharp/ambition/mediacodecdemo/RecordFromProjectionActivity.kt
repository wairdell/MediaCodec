package com.sharp.ambition.mediacodecdemo

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjectionManager
import android.os.*
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

/**
 *    author : fengqiao
 *    date   : 2022/10/25 18:17
 *    desc   :
 */
class RecordFromProjectionActivity : AppCompatActivity() {

    companion object {
        val TAG = MainActivity::class.java.simpleName
    }

    private lateinit var permissionResultLauncher: ActivityResultLauncher<String>
    private lateinit var screenCaptureLauncher: ActivityResultLauncher<Void?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record_from_projection)
        permissionResultLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {

        }
        screenCaptureLauncher = registerForActivityResult(ScreenCaptureResultContract(this)) {

        }
        findViewById<Button>(R.id.btn_record_start).setOnClickListener {
            screenCaptureLauncher.launch(null)
        }
        findViewById<Button>(R.id.btn_record_stop).setOnClickListener {
            val serviceIntent = Intent(this, MediaCodecService::class.java)
            serviceIntent.putExtra("extra_type", "stop")
            startService(serviceIntent)
        }
    }

    class ScreenCaptureResultContract(val context: Context) :
        ActivityResultContract<Void?, Boolean>() {

        private val mediaProjectionManager =
            (context.getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager)

        override fun createIntent(context: Context, input: Void?): Intent {
            return mediaProjectionManager.createScreenCaptureIntent()
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
            if (resultCode == RESULT_OK && intent != null) {
                val serviceIntent = Intent(context, MediaCodecService::class.java)
                serviceIntent.putExtra("extra_type", "start")
                serviceIntent.putExtra("extra_result_code", resultCode)
                serviceIntent.putExtra("extra_data", intent)
                context.startService(serviceIntent)
            }
            return true
        }

    }

}