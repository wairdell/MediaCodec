package com.sharp.ambition.mediacodecdemo

import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaMuxer
import android.util.Log
import java.nio.ByteBuffer

/**
 *    author : fengqiao
 *    date   : 2022/10/26 15:17
 *    desc   :
 */
class AudioCodecThread(
    private val mediaMuxer: MediaMuxer,
    private val mediaCodec: MediaCodec,
    private val audioRecord: AudioRecord,
    private val bufferSize: Int,
    private val status: MediaCodecDecorate.Status
): Thread() {

    companion object {
        val TAG: String = AudioCodecThread::class.java.simpleName
    }

    val audioBuffer = MediaCodec.BufferInfo()
    private var audioTrackIndex = -1
    private val nonaTime = System.nanoTime()

    override fun run() {
        mediaCodec.start()
        val buffer = ByteArray(bufferSize)
        var len: Int
        while (!isInterrupted) {
            if (status.isStop) {
                release()
                return
            }
            len = audioRecord.read(buffer, 0, bufferSize)
            Log.d(TAG, "audioRecord len => $len")
            if (len > 0) {
                encodeAudioToAAC(buffer, len)
            }
        }
    }

    private fun encodeAudioToAAC(data: ByteArray, len: Int) {
        val inputBufferIndex : Int = mediaCodec.dequeueInputBuffer(100000);
        if (inputBufferIndex >= 0) {
            val inputBuffer: ByteBuffer = mediaCodec.getInputBuffer(inputBufferIndex)  ?: return
            inputBuffer.clear()
            inputBuffer.put(data)
            mediaCodec.queueInputBuffer(inputBufferIndex, 0, data.size, (System.nanoTime() - this.nonaTime) / 1000, 0)
        }
        getEncodeData()
    }

    private fun getEncodeData() {
        var outputBufferIndex: Int = mediaCodec.dequeueOutputBuffer(audioBuffer, 0)
        if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            audioTrackIndex = mediaMuxer.addTrack(mediaCodec.outputFormat)
            Log.e(TAG, "audioTrackIndex => $audioTrackIndex")
            if (status.isVideoReady.get()) {
                mediaMuxer.start()
                Log.e(TAG, "mediaMuxer.start");
            }
            status.isAudioReady.set(true)
        } else {
            while (outputBufferIndex >= 0) {
                val outputBuffer: ByteBuffer = mediaCodec.getOutputBuffer(outputBufferIndex) ?: continue
                outputBuffer.position(audioBuffer.offset)
                outputBuffer.limit(audioBuffer.offset + audioBuffer.size)
                mediaMuxer.writeSampleData(audioTrackIndex, outputBuffer, audioBuffer)
                mediaCodec.releaseOutputBuffer(outputBufferIndex, false)
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(audioBuffer, 0)
            }
        }
    }

    private fun release() {
        mediaCodec.signalEndOfInputStream()
        mediaCodec.stop()
        mediaCodec.release()
    }

}