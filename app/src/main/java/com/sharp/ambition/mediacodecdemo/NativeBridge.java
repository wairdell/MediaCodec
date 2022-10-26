package com.sharp.ambition.mediacodecdemo;

import android.media.Image;

import java.nio.ByteBuffer;

/**
 * author : fengqiao
 * date   : 2022/10/26 11:38
 * desc   :
 */
public class NativeBridge {

    static {
        System.loadLibrary("bridge-lib");
    }

    public byte[] yuvToBuffer(Image image) {
        //val buffer: ByteArray = ImageUtil.format(image, height)
        Image.Plane[] planes = image.getPlanes();
        Image.Plane yPlane = planes[0];
        Image.Plane uPlane = planes[1];
        Image.Plane vPlane = planes[2];
        byte[] buffer = new NativeBridge().yuvToBuffer(yPlane.getBuffer(),
            uPlane.getBuffer(),
            vPlane.getBuffer(),
            yPlane.getPixelStride(),
            yPlane.getRowStride(),
            uPlane.getPixelStride(),
            uPlane.getRowStride(),
            vPlane.getPixelStride(),
            vPlane.getRowStride(),
            image.getWidth(),
            image.getHeight());
        return buffer;
    }

    public native byte[] yuvToBuffer(ByteBuffer y, ByteBuffer u, ByteBuffer v, int yPixelStride, int yRowStride,
                                     int uPixelStride, int uRowStride, int vPixelStride, int vRowStride, int imgWidth, int imgHeight);

}
