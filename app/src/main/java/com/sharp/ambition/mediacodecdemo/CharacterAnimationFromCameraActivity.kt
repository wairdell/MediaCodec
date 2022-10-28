package com.sharp.ambition.mediacodecdemo

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
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
 *    date   : 2022/10/27 17:57
 *    desc   :
 */
class CharacterAnimationFromCameraActivity: AppCompatActivity() {

    companion object {
        val TAG = MainActivity::class.java.simpleName
    }

    private lateinit var permissionResultLauncher: ActivityResultLauncher<String>
    private lateinit var preViewSurface: Surface
    private lateinit var cameraManager: CameraManager
    private var cameraCharacteristics: CameraCharacteristics? = null
    private lateinit var characterView: CharacterView
    private var cameraId: String = ""
    private var cameraHandler: Handler = Handler(Looper.getMainLooper())
    private var cameraCaptureSession: CameraCaptureSession? = null
    private lateinit var imageReader: ImageReader
    private lateinit var textureView: TextureView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record_from_camera)
        permissionResultLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        }
        initImageReader()
        initCamera()
        characterView = findViewById<CharacterView>(R.id.character_view);
        findViewById<Button>(R.id.btn_camera_start).setOnClickListener {
            openCamera()
        }
        findViewById<Button>(R.id.btn_camera_stop).setOnClickListener {
            cameraCaptureSession?.abortCaptures()
            cameraCaptureSession?.close()
        }
        textureView = findViewById<TextureView>(R.id.texture_view)
    }

    val width = (360 * 1.5).toInt()
    val height = (640 * 1.5).toInt()

    private fun initImageReader() {
        imageReader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 2)
        imageReader.setOnImageAvailableListener({
            onImageAvailable(it)
        }, cameraHandler)
    }

    fun onImageAvailable(reader: ImageReader) {
        val image = reader.acquireNextImage() ?: return
        characterView.drawCharacter(image, width, height)
        image.close()
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
                preViewSurface = Surface(textureView.surfaceTexture)
                camera.createCaptureSession(arrayListOf(imageReader.surface), object : CameraCaptureSession.StateCallback() {

                    override fun onConfigured(session: CameraCaptureSession) {
                        Log.d(TAG, "onConfigured() called with: session = $session")
                        val captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                        captureRequestBuilder.addTarget(imageReader.surface)
//                        captureRequestBuilder.addTarget(preViewSurface)
                        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
                        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                        session.setRepeatingRequest(captureRequestBuilder.build(), object : CameraCaptureSession.CaptureCallback() {

                            override fun onCaptureStarted(session: CameraCaptureSession, request: CaptureRequest, timestamp: Long, frameNumber: Long) {
                                super.onCaptureStarted(session, request, timestamp, frameNumber)
                                Log.d(TAG, "onCaptureStarted() called with: session = $session, request = $request, timestamp = $timestamp, frameNumber = $frameNumber")
                            }

                        }, cameraHandler)
                        this@CharacterAnimationFromCameraActivity.cameraCaptureSession = session
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