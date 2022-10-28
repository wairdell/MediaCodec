package com.sharp.ambition.mediacodecdemo

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.media.Image
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import java.util.concurrent.Executors
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

/**
 *    author : fengqiao
 *    date   : 2022/10/27 18:03
 *    desc   :
 */
class CharacterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    SurfaceView(context, attrs, defStyleAttr) {

    private var paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bl = 8
    private var pixelsWidth = 400
    private var pixelsHeight = 400
    private var pixels: IntArray = IntArray(pixelsWidth * pixelsHeight)
    private var isPrepare = false
    private var bitmapQueue = PriorityBlockingQueue<Task>(11) { o1, o2 -> (o1.realtime - o2.realtime).toInt() }

    init {
        paint.style = Paint.Style.FILL
        paint.textSize = bl* 1.5F
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        holder.addCallback(object : SurfaceHolder.Callback {

            private var lastDrawTime = 0L

            override fun surfaceCreated(holder: SurfaceHolder) {
                isPrepare = true
                drawService.execute {
                    while (isPrepare) {
                        val task = bitmapQueue.take() ?: continue
                        val bitmap = task.bitmap
                        Log.e("TAG", "take => " + task.realtime)
                        holder.lockCanvas().let {
                            it.drawBitmap(bitmap, 0F, 0F, null)
                            holder.unlockCanvasAndPost(it)
                        }
                        val realtime = SystemClock.elapsedRealtime()
                        if (realtime - lastDrawTime < 30) {
                            SystemClock.sleep(realtime - lastDrawTime)
                        }
                        lastDrawTime = SystemClock.elapsedRealtime()
                    }
                }
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                Log.e("TAG", "surfaceChanged")
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                isPrepare = false
            }

        })
    }

    private val calculateService = Executors.newFixedThreadPool(1, object : ThreadFactory {

        private val count: AtomicInteger = AtomicInteger(0)

        override fun newThread(r: Runnable): Thread {
            return Thread(r).apply {
                name = "calculate ${count.getAndIncrement()}"
            }
        }

    })

    private val drawService = Executors.newFixedThreadPool(4)
    private var lastTime = 0L

    @SuppressLint("NewApi")
    fun drawCharacter(image: Image, width: Int, height: Int) {
        val realtime = SystemClock.elapsedRealtime()
        if (realtime - lastTime < 60) {
            return
        }
        val bitmap = image.toBitmap()
        calculateService.execute {
            if (!isPrepare) {
                return@execute
            }
            val startRealtime = SystemClock.elapsedRealtime()
            Log.e("TAG", "drawCharacter() called start ${Thread.currentThread().name} $startRealtime")
            val rotateBitmap = bitmap.rotate(90F)
            //it.drawBitmap(image.toBitmap().rotate(90F), 0F, 0F, null)
            Log.e("TAG", "drawCharacter() convertBitmap ${SystemClock.elapsedRealtime() - startRealtime}")
            pixelsWidth = rotateBitmap.width
            pixelsHeight = rotateBitmap.height
            if (pixels.size != rotateBitmap.byteCount / 4) {
                pixels = IntArray(rotateBitmap.byteCount / 4)
            }
            rotateBitmap.getPixels(pixels, 0, pixelsWidth, 0, 0, pixelsWidth, pixelsHeight)
            val drawBitmap = Bitmap.createBitmap(pixelsWidth, pixelsHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(drawBitmap)
            for (i in pixels.indices) {
                val x = i % pixelsWidth
                val y = i / pixelsWidth
                if (x % bl == 0 && y % bl == 0) {
                    val pixel = pixels[i]
                    val g = ((Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 1.5).toInt()
                    paint.color = Color.argb(Color.alpha(pixel), g, g, g)
                    canvas.drawText("6", x.toFloat(), y.toFloat(), paint)
                }
            }
            Log.e("TAG", "drawCharacter() called end size=${pixels.size} width=${rotateBitmap.width} height=${rotateBitmap.height} ${SystemClock.elapsedRealtime() - startRealtime}")
            drawBitmap(drawBitmap, realtime)
        }
        lastTime = SystemClock.elapsedRealtime()
    }

    private fun drawBitmap(bitmap: Bitmap, realtime: Long) {
        bitmapQueue.offer(Task(bitmap, realtime))
    }

    data class Task(val bitmap: Bitmap, val realtime: Long)

}