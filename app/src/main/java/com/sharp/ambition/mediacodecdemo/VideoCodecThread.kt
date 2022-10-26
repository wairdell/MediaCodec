package com.sharp.ambition.mediacodecdemo

import android.media.MediaCodec
import android.media.MediaMuxer
import android.util.Log
import java.nio.ByteBuffer

/**
 *    author : fengqiao
 *    date   : 2022/10/24 14:35
 *    desc   :
 */
class VideoCodecThread(
    private val mediaMuxer: MediaMuxer,
    private val videoCodec: MediaCodec,
    private val bufferInfo: MediaCodec.BufferInfo,
    val status: MediaCodecDecorate.Status
) : Thread() {

    companion object {
        val TAG = VideoCodecThread::javaClass.name
    }

    private var videoTrackIndex = -1
    private var pts = 0L

    override fun run() {
        videoCodec.start()
        while (!isInterrupted) {
            if (status.isStop) {
                release()
                break
            }
            encodeVideoH264()
        }
    }

    private fun encodeVideoH264() {
        var outputBufferIndex: Int = videoCodec.dequeueOutputBuffer(bufferInfo, 0)
        if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            videoTrackIndex = mediaMuxer.addTrack(videoCodec.outputFormat)
            Log.e(TAG, "videoTrackIndex => $videoTrackIndex")
            if (status.isAudioReady.get()) {
                mediaMuxer.start()
                Log.e(TAG, "mediaMuxer.start");
            }
            status.isVideoReady.set(true)
        } else {
            while (outputBufferIndex >= 0) {
                val outputBuffer: ByteBuffer = videoCodec.getOutputBuffer(outputBufferIndex) ?: continue
                takeIf { status.isAudioReady.get() } ?: continue
                outputBuffer.position(bufferInfo.offset)
                outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                if (pts == 0L) {
                    pts = bufferInfo.presentationTimeUs
                }
                bufferInfo.presentationTimeUs = bufferInfo.presentationTimeUs - pts
                mediaMuxer.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo)
                Log.d(TAG, "视频秒数时间戳 = " + bufferInfo.presentationTimeUs / 1000000.0f)
                videoCodec.releaseOutputBuffer(outputBufferIndex, false)
                outputBufferIndex = videoCodec.dequeueOutputBuffer(bufferInfo, 0)
            }
        }
    }

    fun release() {
        videoCodec.signalEndOfInputStream()
        videoCodec.stop()
        videoCodec.release()
    }

}