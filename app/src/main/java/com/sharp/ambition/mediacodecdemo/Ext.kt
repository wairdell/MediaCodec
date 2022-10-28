package com.sharp.ambition.mediacodecdemo

import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.Image
import com.google.android.renderscript.Toolkit
import com.google.android.renderscript.YuvFormat

/**
 *    author : fengqiao
 *    date   : 2022/10/28 13:43
 *    desc   :
 */

fun Image.toBitmap(): Bitmap {
    val y: Image.Plane = planes[0]
    val u: Image.Plane = planes[1]
    val v: Image.Plane = planes[2]
    val yb = y.buffer.remaining()
    val ub = u.buffer.remaining()
    val vb = v.buffer.remaining()
    val data = ByteArray(yb + ub + vb)
    y.buffer[data, 0, yb]
    v.buffer[data, yb, vb]
    u.buffer[data, yb + vb, ub]
    return Toolkit.yuvToRgbBitmap(data, width, height, YuvFormat.NV21)
}

fun Bitmap.rotate(degrees: Float): Bitmap {
    val matrix = Matrix()
    matrix.postScale(1f, 1f)
    matrix.postRotate(degrees)
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}