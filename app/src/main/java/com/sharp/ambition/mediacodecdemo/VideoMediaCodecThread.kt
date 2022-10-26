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
class VideoMediaCodecThread(val mediaMuxer: MediaMuxer, val videoCodec: MediaCodec, val bufferInfo: MediaCodec.BufferInfo) : Thread() {

    companion object {
        val TAG = VideoMediaCodecThread::javaClass.name
    }

    private var videoTrackIndex = -1
    private var pts = 0L
    private var isStop = false

    fun cancel() {
        isStop = true
    }

    override fun run() {
        videoCodec.start()
        while (!isInterrupted) {
            if (isStop) {
                release()
                break
            }
            var outputBufferIndex: Int = videoCodec.dequeueOutputBuffer(bufferInfo, 0)
            if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                videoTrackIndex = mediaMuxer.addTrack(videoCodec.outputFormat)
                mediaMuxer.start()
            } else {
                while (outputBufferIndex >= 0) {
                    val outputBuffer: ByteBuffer = videoCodec.getOutputBuffer(outputBufferIndex) ?: continue
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
    }

    fun release() {
        videoCodec.stop()
        videoCodec.release()
        mediaMuxer.stop()
        mediaMuxer.release()
    }

}