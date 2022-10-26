package com.sharp.ambition.mediacodecdemo

import android.annotation.SuppressLint
import android.content.Context
import android.media.*
import android.os.Build
import android.os.Environment
import android.view.Surface
import java.util.concurrent.atomic.AtomicBoolean


/**
 * author : fengqiao
 * date   : 2022/10/24 18:18
 * desc   :
 */
class MediaCodecDecorate(private val context: Context) {
    lateinit var mediaCodec: MediaCodec
    lateinit var mediaMuxer: MediaMuxer
    lateinit var videoBuffer: MediaCodec.BufferInfo
    lateinit var videoCodecThread: VideoCodecThread
    lateinit var surface: Surface
        private set
    private var status: Status = Status()

    val width = 1920
    val height = 1080

    private fun initMediaMuxer() {
        val filePath = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.absolutePath + "/" + System.currentTimeMillis().toString() + ".mp4"
        mediaMuxer = MediaMuxer(filePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    }

    private fun initMediaCodec(outSurface: Surface?) {
        mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
//            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
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
        videoBuffer = MediaCodec.BufferInfo()
        surface = mediaCodec.createInputSurface()
        /*mediaCodec.setCallback(object : MediaCodec.Callback() {
            override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {

            }
            override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
            }

            override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
            }

            override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
            }

        })*/
        videoCodecThread = VideoCodecThread(mediaMuxer, mediaCodec, videoBuffer, status)
    }

    private lateinit var audioCodec: MediaCodec
    private lateinit var audioRecord: AudioRecord
    private lateinit var audioCodecThread: AudioCodecThread

    @SuppressLint("MissingPermission")
    private fun initAudioCodec() {
        val sampleRateInHz = 44100
        val channelConfig = AudioFormat.CHANNEL_IN_STEREO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat)

        audioCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        val mediaFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRateInHz, 2).apply {
            val bitRate = 96000
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            val maxInoutSize = 8192
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, maxInoutSize)
        }
        audioCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRateInHz, channelConfig, audioFormat, bufferSize)
        audioCodecThread = AudioCodecThread(mediaMuxer, audioCodec, audioRecord, bufferSize, status)
    }

    fun init(surface: Surface? = null) {
        initMediaMuxer()
        initMediaCodec(surface)
        initAudioCodec()
    }

    private var isStart = false

    fun start() {
        if (!isStart) {
            videoCodecThread.start()
            audioRecord.startRecording()
            audioCodecThread.start()
            isStart = true
        }
    }

    fun release() {
        status.isStop = true
        audioRecord.stop()
        audioCodec.release()
        mediaMuxer.stop()
        mediaMuxer.release()
    }

    data class Status(var isStop: Boolean = false,
                      var isVideoReady: AtomicBoolean = AtomicBoolean(false),
                      var isAudioReady: AtomicBoolean = AtomicBoolean(false))

}