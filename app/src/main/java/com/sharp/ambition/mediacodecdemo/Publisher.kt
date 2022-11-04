package com.sharp.ambition.mediacodecdemo

import android.content.Context
import android.os.Environment
import com.android.rtmpvideo.RtmpPublisher
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue

/**
 *    author : fengqiao
 *    date   : 2022/11/4 16:01
 *    desc   :
 */
class Publisher(val context: Context) {

    private val rtmpPublisher: RtmpPublisher = RtmpPublisher.newInstance()
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val runnableQueue = LinkedBlockingQueue<Runnable>()
    private var isStarted = false

    init {

    }

    fun stop() {
        isStarted = false;
        runnableQueue.clear()
        rtmpPublisher.stopRtmpPublish()
    }

    fun start() {
        isStarted = true
        executor.execute {
            while (!Thread.interrupted() && isStarted) {
                val runnable = runnableQueue.take()
                runnable.run()
            }
        }
        runnableQueue.add {
            val logPath = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)!!.absolutePath + "/" + "rtmp.log";
            rtmpPublisher.init("rtmp://10.10.30.136/live/livestream", logPath)
//            rtmpPublisher.init("rtmp://sendtc3a.douyu.com/live/11209824rI4fVKKv?wsSecret=cb5eaeefa8e6dcb1a80d3a038696ba03&wsTime=6364d21c&wsSeek=off&wm=0&tw=0&roirecognition=0&record=flv&origin=tct&txHost=sendtc3a.douyu.com", logPath)
        }
    }

    fun outputVideoSpsPps(sps: ByteArray, spslen: Int, pps: ByteArray, ppslen: Int, nTimeStamp: Int) {
        runnableQueue.add {
            rtmpPublisher.sendSpsAndPps(sps, spslen, pps, ppslen, nTimeStamp)
        }
    }

    fun outputVideoFrame(nalu: ByteArray, len: Int, nTimeStamp: Int) {
        runnableQueue.add {
            rtmpPublisher.sendAVCFrame(nalu, len, nTimeStamp)
        }
    }

}