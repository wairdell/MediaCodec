package com.sharp.ambition.mediacodecdemo

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.*
import android.os.*
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.nio.ByteBuffer
import java.nio.ReadOnlyBufferException
import kotlin.experimental.inv

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
    val width = 1920
    val height = 1080
    private var videoTrackIndex = -1
    private var pts = 0L

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
            release()
        }
        textureView = findViewById<TextureView>(R.id.texture_view)

        initImageReader()
    }

    fun release() {
        cameraCaptureSession?.abortCaptures()
        cameraCaptureSession?.close()
        imageReader.close()
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
                initMediaCodec()
                initMediaMuxer()
                mediaCodec.start()
                camera.createCaptureSession(arrayListOf(imageReader.surface, preViewSurface), object : CameraCaptureSession.StateCallback() {

                    override fun onConfigured(session: CameraCaptureSession) {
                        Log.d(TAG, "onConfigured() called with: session = $session")
                        val captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
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
        mediaMuxer.setOrientationHint(90)
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
            setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel31)
            }
        }
        mediaCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        bufferInfo = MediaCodec.BufferInfo()
    }

    fun onImageAvailable(reader: ImageReader) {
        Log.d(TAG, "onImageAvailable() called with: reader = $reader")
        val image: Image = reader.acquireNextImage() ?: return
        /*val I420size: Int = image.width * image.height * 3 / 2;
        val nv21 = ByteArray(I420size)
        //提取YUV填充nv21数据
        ImageUtil.YUVToNV21_NV12(image, nv21, image.width, image.height,"NV21")
        val buffer: ByteArray = nv21*/
//        val buffer: ByteArray = ImageUtil.rotateNV21_working(nv21, image.width, image.height, 90)
        val buffer: ByteArray = YUV_420_888toNV21(image)
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

    private fun YUV_420_888toNV21(image: Image): ByteArray {
        //https://stackoverflow.com/questions/52726002/camera2-captured-picture-conversion-from-yuv-420-888-to-nv21/52740776#52740776
        val width = image.width
        val height = image.height
        val ySize = width * height
        val uvSize = width * height / 4
        val nv21 = ByteArray(ySize + uvSize * 2)
        val yBuffer = image.planes[0].buffer // Y
        val uBuffer = image.planes[1].buffer // U
        val vBuffer = image.planes[2].buffer // V
        var rowStride = image.planes[0].rowStride
        assert(image.planes[0].pixelStride === 1)
        var pos = 0
        if (rowStride == width) { // likely
            yBuffer[nv21, 0, ySize]
            pos += ySize
        } else {
            var yBufferPos = -rowStride.toLong() // not an actual position
            while (pos < ySize) {
                yBufferPos += rowStride.toLong()
                yBuffer.position(yBufferPos.toInt())
                yBuffer[nv21, pos, width]
                pos += width
            }
        }
        rowStride = image.planes[2].rowStride
        val pixelStride = image.planes[2].pixelStride
        assert(rowStride == image.planes[1].rowStride)
        assert(pixelStride == image.planes[1].pixelStride)
        if (pixelStride == 2 && rowStride == width && uBuffer[0] == vBuffer[1]) {
            // maybe V an U planes overlap as per NV21, which means vBuffer[1] is alias of uBuffer[0]
            val savePixel = vBuffer[1]
            try {
                vBuffer.put(1, savePixel.inv())
                if (uBuffer[0] == savePixel.inv()) {
                    vBuffer.put(1, savePixel)
                    vBuffer.position(0)
                    uBuffer.position(0)
                    vBuffer[nv21, ySize, 1]
                    uBuffer[nv21, ySize + 1, uBuffer.remaining()]
                    return nv21 // shortcut
                }
            } catch (ex: ReadOnlyBufferException) {
                // unfortunately, we cannot check if vBuffer and uBuffer overlap
            }

            // unfortunately, the check failed. We must save U and V pixel by pixel
            vBuffer.put(1, savePixel)
        }

        // other optimizations could check if (pixelStride == 1) or (pixelStride == 2),
        // but performance gain would be less significant
        for (row in 0 until height / 2) {
            for (col in 0 until width / 2) {
                val vuPos = col * pixelStride + row * rowStride
                nv21[pos++] = vBuffer[vuPos]
                nv21[pos++] = uBuffer[vuPos]
            }
        }
        return nv21
    }

}