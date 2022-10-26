package com.sharp.ambition.mediacodecdemo

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.*
import android.os.*
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

/**
 *    author : fengqiao
 *    date   : 2022/10/25 18:12
 *    desc   :
 */
class RecordFromCameraSurfaceActivity : AppCompatActivity() {

    companion object {
        val TAG = MainActivity::class.java.simpleName
    }

    private lateinit var permissionResultLauncher: ActivityResultLauncher<String>
    private lateinit var mediaCodecDecorate: MediaCodecDecorate
    private lateinit var surfaceView: TextureView
    private lateinit var surface: Surface
    private lateinit var cameraManager: CameraManager
    private var cameraCharacteristics: CameraCharacteristics? = null
    private var cameraId: String = ""
    private var cameraHandler: Handler = Handler(Looper.getMainLooper())
    private var cameraCaptureSession: CameraCaptureSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record_from_camera)
        permissionResultLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        }
        initCamera()
        findViewById<Button>(R.id.btn_camera_start).setOnClickListener {
            openCamera()
        }
        findViewById<Button>(R.id.btn_camera_stop).setOnClickListener {
            cameraCaptureSession?.abortCaptures()
            cameraCaptureSession?.close()
            mediaCodecDecorate.release()
        }
        surfaceView = findViewById<TextureView>(R.id.texture_view)
        mediaCodecDecorate = MediaCodecDecorate(this@RecordFromCameraSurfaceActivity)
    }

    private fun initCamera() {
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        for (cameraId in cameraManager.cameraIdList) {
            val cameraCharacteristics: CameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
            val facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                continue
            }
            this.cameraId = cameraId
            this.cameraCharacteristics = cameraCharacteristics
        }

        val backgroundThread = HandlerThread(TAG, Process.THREAD_PRIORITY_BACKGROUND)
        backgroundThread.start()
        cameraHandler = Handler(backgroundThread.looper)
    }

    private fun openCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                Log.d(TAG, "onOpened() called with: camera = $camera")
                surface = Surface(surfaceView.surfaceTexture)
                mediaCodecDecorate.init()
                camera.createCaptureSession(arrayListOf(mediaCodecDecorate.surface, surface), object : CameraCaptureSession.StateCallback() {

                    override fun onConfigured(session: CameraCaptureSession) {
                        Log.d(TAG, "onConfigured() called with: session = $session")
                        val captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                        captureRequestBuilder.addTarget(mediaCodecDecorate.surface)
                        captureRequestBuilder.addTarget(surface)
                        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
                        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                        session.setRepeatingRequest(captureRequestBuilder.build(), null, cameraHandler)
                        this@RecordFromCameraSurfaceActivity.cameraCaptureSession = session
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.d(TAG, "onConfigureFailed() called with: session = ${session}")
                    }

                }, null)
            }

            override fun onDisconnected(camera: CameraDevice) {
                Log.d(TAG, "onDisconnected() called with: camera = $camera")
            }

            override fun onError(camera: CameraDevice, error: Int) {
                Log.d(TAG, "onError() called with: camera = $camera, error = $error")
            }

        }, cameraHandler)
    }


}