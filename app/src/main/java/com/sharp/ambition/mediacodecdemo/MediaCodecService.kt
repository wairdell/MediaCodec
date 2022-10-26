package com.sharp.ambition.mediacodecdemo

import android.app.*
import android.content.Intent
import android.os.IBinder
import android.graphics.BitmapFactory
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.util.Log

/**
 * author : fengqiao
 * date   : 2022/10/24 15:01
 * desc   :
 */
class MediaCodecService : Service() {

    companion object {
        val TAG: String = MediaCodecService::class.java.simpleName
    }

    private lateinit var virtualDisplay: VirtualDisplay
    private lateinit var mediaCodecDecorate: MediaCodecDecorate


    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        mediaCodecDecorate = MediaCodecDecorate(this)
    }

    private fun createVirtualDisplay(mediaProjection: MediaProjection) {
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "ScreenSharingDemo",
            mediaCodecDecorate.width,
            mediaCodecDecorate.height,
            resources.displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaCodecDecorate.surface,
            object: VirtualDisplay.Callback() {

                override fun onResumed() {
                    super.onResumed()
                    mediaCodecDecorate.start()
                }
            },
            null
        )
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val type = intent.getStringExtra("extra_type")
        Log.e(TAG, "type => $type")
        if (type == "start") {
            mediaCodecDecorate.init()
            val mediaProjectionManager = (getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager)
            val mediaProjection = mediaProjectionManager.getMediaProjection(
                intent.getIntExtra("extra_result_code", 0),
                intent.getParcelableExtra<Intent>("extra_data")!!
            )
            mediaProjection.registerCallback(object : MediaProjection.Callback() {

            }, null)
            createVirtualDisplay(mediaProjection)
        } else if (type == "stop") {
//            mediaCodec.flush()
            virtualDisplay.release()
            mediaCodecDecorate.release()
//            videoMediaCodecThread.interrupt()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun createNotificationChannel() {
        //获取一个Notification构造器
        val builder = Notification.Builder(this.applicationContext)
        //点击后跳转的界面，可以设置跳转数据
        val nfIntent = Intent(this, MainActivity::class.java)
        builder.setContentIntent(PendingIntent.getActivity(this, 0, nfIntent, PendingIntent.FLAG_IMMUTABLE))
            .setLargeIcon(BitmapFactory.decodeResource(this.resources, R.mipmap.ic_launcher)) //.setContentTitle("SMI InstantView")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentText("is running......")
            .setWhen(System.currentTimeMillis())

        /*以下是对Android 8.0的适配*/
        //普通notification适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId("notification_id")
        }
        //前台服务notification适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel("notification_id", "notification_name", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }
        // 获取构建好的Notification
        val notification = builder.build()
        //设置为默认的声音
        notification.defaults = Notification.DEFAULT_SOUND
        startForeground(110, notification)
    }
}