package com.sharp.ambition.mediacodecdemo

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.os.Environment
import android.view.Surface

/**
 * author : fengqiao
 * date   : 2022/10/24 18:18
 * desc   :
 */
class MediaCodecDecorate(val context: Context) {
    lateinit var mediaCodec: MediaCodec
    lateinit var mediaMuxer: MediaMuxer
    lateinit var videoBuffer: MediaCodec.BufferInfo
    lateinit var videoMediaCodecThread: VideoMediaCodecThread
    lateinit var surface: Surface
        private set

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
    }

    fun init(surface: Surface? = null) {
        initMediaMuxer()
        initMediaCodec(surface)
        videoMediaCodecThread = VideoMediaCodecThread(mediaMuxer, mediaCodec, videoBuffer)
        videoMediaCodecThread.start()
//        mediaCodec.start()
    }

    fun release() {
        videoMediaCodecThread.cancel()
    }

}