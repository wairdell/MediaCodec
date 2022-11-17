package com.sharp.ambition.mediacodecdemo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.*
import android.os.*
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.WindowManager
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.android.rtmpvideo.YuvEngineWrap
import com.android.rtmpvideo.YuvOperateJni.Nv12ClockWiseRotate90
import java.nio.ByteBuffer
import java.nio.ReadOnlyBufferException
import kotlin.experimental.inv
import kotlin.math.abs

/**
 * author : fengqiao
 * date   : 2022/10/25 18:28
 * desc   :
 */
class RecordFromCamera2YUVActivity : AppCompatActivity() {

    companion object {
        val TAG = MainActivity::class.java.simpleName
    }

    private lateinit var permissionResultLauncher: ActivityResultLauncher<String>
    private lateinit var imageReader: ImageReader
    private lateinit var textureView: TextureView
    private lateinit var preViewSurface: Surface

    private lateinit var cameraManager: CameraManager
    private var cameraCharacteristics: CameraCharacteristics? = null
    private var cameraId: String = ""
    private var cameraHandler: Handler = Handler(Looper.getMainLooper())
    private var cameraCaptureSession: CameraCaptureSession? = null

    private lateinit var mediaCodec: MediaCodec
    private lateinit var mediaMuxer: MediaMuxer
    private lateinit var bufferInfo: MediaCodec.BufferInfo
    private var glSurfaceView: DisplayShapeView? = null
    var width = 1280
    var height = 720
    private var videoTrackIndex = -1
    private var pts = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record_from_camera)
        textureView = findViewById<TextureView>(R.id.texture_view)
        permissionResultLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {

        }
        initCamera()
        findViewById<Button>(R.id.btn_camera_start).setOnClickListener {
            val previewSize = getBestPreviewSize()
            width = previewSize.width
            height = previewSize.height
            Log.e(TAG, "previewSize => $previewSize");
            openCamera()
            publisher.start()
        }
        findViewById<Button>(R.id.btn_camera_stop).setOnClickListener {
            release()
        }
        glSurfaceView = findViewById<DisplayShapeView>(R.id.gl_surface_view).apply {
            paramRenderer = VideoRenderer(this@RecordFromCamera2YUVActivity)
        }
        publisher = Publisher(this)
    }

    fun release() {
        if (cameraCaptureSession == null) {
            return
        }
        publisher.stop()
        imageReader.close()
        cameraCaptureSession?.abortCaptures()
        cameraCaptureSession?.close()
        cameraCaptureSession = null
        mediaCodec.stop()
        mediaCodec.release()
        mediaMuxer.stop()
        mediaMuxer.release()
    }


    private fun initCamera() {
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        for (cameraId in cameraManager.cameraIdList) {
            val cameraCharacteristics: CameraCharacteristics =
                cameraManager.getCameraCharacteristics(cameraId)
            val facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)
            this.cameraId = cameraId
            this.cameraCharacteristics = cameraCharacteristics
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                break
            }
        }
        val backgroundThread = HandlerThread(TAG, Process.THREAD_PRIORITY_BACKGROUND)
        backgroundThread.start()
        cameraHandler = Handler(backgroundThread.looper)
    }

    private fun getBestPreviewSize(): Size {
        cameraCharacteristics?.let {
            val map = it.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: return Size(width, height)
            val sizes = map.getOutputSizes(SurfaceTexture::class.java)
            return findSuitablePreviewSize(sizes, textureView.height, textureView.width)
        }
        return Size(width, height)
    }

    private fun getCameraOrientation(): Int {
        return cameraCharacteristics?.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
    }

    fun displayRotation(context: Context): Int {
        return when((context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation) {
            Surface.ROTATION_0   -> 0
            Surface.ROTATION_90  -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
    }

    private fun findSuitablePreviewSize(supportedPreviewSizes: Array<Size>, previewWidth: Int, previewHeight: Int): Size {
        Log.e("TAG", "previewSize => ${previewWidth}x${previewHeight}")
        val originalAspectRatio = previewWidth / previewHeight.toDouble()
        var previewSize: Size? = null
        var lastFit = Double.MAX_VALUE
        var currentFit: Double = 0.0
        for (size in supportedPreviewSizes) {
            if (size.width == previewWidth && size.height == previewHeight) {
                previewSize = size
                break
            } else if (previewSize == null) {
                lastFit = abs((size.width / size.height.toDouble()) - originalAspectRatio)
                previewSize = size
            } else {
                currentFit = abs((size.width / size.height.toDouble()) - originalAspectRatio)
                if (currentFit < lastFit && abs(previewWidth - size.width) <= abs(previewWidth - previewSize.width)) {
                    previewSize = size
                    lastFit = currentFit
                }
            }
            Log.e("TAG", "size => ${size}")
        }
        return previewSize ?: Size(previewWidth, previewHeight)
    }

    private fun openCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        initImageReader()
        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                Log.d(TAG, "onOpened() called with: camera = $camera")
                preViewSurface = glSurfaceView?.let {
                    val surfaceTexture = it.getSurfaceTexture()
                    surfaceTexture?.setDefaultBufferSize(it.width, it.height)
                    it.initDefMatrix(width, height)
                    Log.e(TAG, "surfaceTexture => $surfaceTexture")
                    Surface(surfaceTexture)
                } ?:  Surface(textureView.surfaceTexture)
                initMediaCodec()
                initMediaMuxer()
                mediaCodec.start()
                camera.createCaptureSession(arrayListOf(imageReader.surface, preViewSurface), object : CameraCaptureSession.StateCallback() {

                    override fun onConfigured(session: CameraCaptureSession) {
                        Log.d(TAG, "onConfigured() called with: session = $session")
                        val captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                        val angle = (getCameraOrientation() - displayRotation(this@RecordFromCamera2YUVActivity) + 360) % 360
                        Log.e(TAG, "angle => ${angle}")
//                        captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, angle)
                        captureRequestBuilder.addTarget(imageReader.surface)
                        captureRequestBuilder.addTarget(preViewSurface)
                        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
                        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                        session.setRepeatingRequest(captureRequestBuilder.build(), null, cameraHandler)
                        this@RecordFromCamera2YUVActivity.cameraCaptureSession = session
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

    private fun initImageReader() {
        imageReader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 2)
        imageReader.setOnImageAvailableListener({
            onImageAvailable(it)
        }, cameraHandler)
    }

    private fun initMediaMuxer() {
        val filePath = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.absolutePath + "/" + System.currentTimeMillis().toString() + ".mp4"
        mediaMuxer = MediaMuxer(filePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
//        mediaMuxer.setOrientationHint(90)
    }

    private fun initMediaCodec() {
        mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            //width*height*N  N标识码率低、中、高，类似可设置成1，3，5，码率越高视频越大，也越清晰
            setInteger(MediaFormat.KEY_BIT_RATE, width * height * 4)
            //每秒关键帧数
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
//            setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel31)
            }
        }
        mediaCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        bufferInfo = MediaCodec.BufferInfo()
    }

    /*private val outWidth = IntArray(1)
    private val outHeight = IntArray(1)
    private val yuvBuffer = ByteArray(this.width * this.height * 3 / 2)
    private val rotateYuvBuffer = ByteArray(this.width * this.height * 3 / 2)*/

    fun onImageAvailable(reader: ImageReader) {
//        Log.d(TAG, "onImageAvailable() called with: reader = $reader")
        findViewById<DisplayShapeView>(R.id.gl_surface_view).requestRender()
        val image: Image = reader.acquireNextImage() ?: return
        val I420size: Int = image.width * image.height * 3 / 2;
        val nv21 = ByteArray(I420size)
        //提取YUV填充nv21数据
        ImageUtil.YUVToNV21_NV12(image, nv21, image.width, image.height,"NV21")
        val buffer: ByteArray = nv21
//        val buffer: ByteArray = ImageUtil.rotateNV21_working(nv21, image.width, image.height, 90)
//        val buffer: ByteArray = ImageUtil.YUV_420_888toNV21(image)
        /*YuvEngineWrap.newInstance().Nv21ToNv12(buffer, yuvBuffer, width, height);
        YuvEngineWrap.newInstance().Nv12ClockWiseRotate90(yuvBuffer, width, height, rotateYuvBuffer, outWidth, outHeight);*/
        /*val nv21_rotated = ByteArray(buffer.size)
        ImageUtil.nv21_rotate_to_90(buffer, nv21_rotated, height, width)*/
        encodeFrame(buffer)
        image.close()
    }

    ///摄像头调用
    fun encodeFrame(input: ByteArray): Int {
        Log.e("TAG", "input => ${input.size}")
        val inputBufferIndex : Int = mediaCodec.dequeueInputBuffer(100000);
        if (inputBufferIndex >= 0) {
            val inputBuffer: ByteBuffer = mediaCodec.getInputBuffer(inputBufferIndex)  ?: return -1
            inputBuffer.clear()
            inputBuffer.put(input)
            val presentationTimeUs : Long = computePresentationTime(frameIndex);
            mediaCodec.queueInputBuffer(inputBufferIndex, 0, input.size, presentationTimeUs, 0)
            frameIndex++
        }
        var outputBufferIndex: Int = mediaCodec.dequeueOutputBuffer(bufferInfo, 100000)
        if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            videoTrackIndex = mediaMuxer.addTrack(mediaCodec.outputFormat)
            mediaMuxer.start()
        } else {
            while (outputBufferIndex >= 0) {
                val outputBuffer: ByteBuffer = mediaCodec.getOutputBuffer(outputBufferIndex) ?: continue
                outputBuffer.position(bufferInfo.offset)
                outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                if (pts == 0L) {
                    pts = bufferInfo.presentationTimeUs
                }
                bufferInfo.presentationTimeUs = bufferInfo.presentationTimeUs - pts
                mediaMuxer.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo)
                Log.d(VideoCodecThread.TAG, "视频秒数时间戳 = " + bufferInfo.presentationTimeUs / 1000000.0f)
                publisher(outputBuffer)
                mediaCodec.releaseOutputBuffer(outputBufferIndex, false)
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0)
            }
        }
        return 0
    }

    var frameIndex = 0L


    private fun computePresentationTime(frameIndex: Long): Long {
        //第一帧添加个缓存时间
        return 132 + frameIndex * 1000000 / 15
    }

    private var mSpsNalu: ByteArray? = null
    private var mPpsNalu: ByteArray? = null
    private lateinit var publisher: Publisher

    private fun publisher(outputBuffer: ByteBuffer) {
        var outData: ByteArray = ByteArray(bufferInfo.size)
        outputBuffer.get(outData)
        if (mSpsNalu != null && mPpsNalu != null) {
            val naluType: Int = 0x1f and outData[4].toInt()
            /*val rotateData: ByteArray = ByteArray(bufferInfo.size)
            YuvEngineWrap.newInstance().Nv21ClockWiseRotate90(outData, width, height, rotateData, IntArray(1), IntArray(1))*/
            publisher.outputVideoFrame(outData, outData.size, (bufferInfo.presentationTimeUs / 1000).toInt())
        } else {
            val spsPpsBuffer = ByteBuffer.wrap(outData)
            if (spsPpsBuffer.int == 0x00000001 && spsPpsBuffer[4] == 0x67.toByte()) {
                mSpsNalu = ByteArray(outData.size - 4 - 8)
                mPpsNalu = ByteArray(4)
                spsPpsBuffer.get(mSpsNalu!!, 0, mSpsNalu!!.size)
                spsPpsBuffer.int
                spsPpsBuffer.get(mPpsNalu!!, 0, mPpsNalu!!.size)
                publisher.outputVideoSpsPps(mSpsNalu!!, mSpsNalu!!.size, mPpsNalu!!, mPpsNalu!!.size, (bufferInfo.presentationTimeUs / 1000).toInt())
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        release()
    }

}