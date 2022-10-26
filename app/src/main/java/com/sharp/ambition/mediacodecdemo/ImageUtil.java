package com.sharp.ambition.mediacodecdemo;

import android.media.Image;

/**
 * author : fengqiao
 * date   : 2022/10/24 18:53
 * desc   :
 */
public class ImageUtil {
    //    nv21ToNV12
    public static void yuvToNv21(byte[] y, byte[] u, byte[] v, byte[] nv21, int stride, int height) {
        System.arraycopy(y, 0, nv21, 0, y.length);
        // 注意，若length值为 y.length * 3 / 2 会有数组越界的风险，需使用真实数据长度计算
        int length = y.length + u.length / 2 + v.length / 2;
        int uIndex = 0, vIndex = 0;
        for (int i = stride * height; i < length; i += 2) {
            nv21[i] = v[vIndex];
            nv21[i + 1] = u[uIndex];
            vIndex += 2;
            uIndex += 2;
        }
    }

    public static byte[] nv21_rotate_to_90(byte[] nv21_data, byte[] nv21_rotated, int width, int height) {
        int y_size = width * height;
        int buffser_size = y_size * 3 / 2;

        // Rotate the Y luma
        int i = 0;
        int startPos = (height - 1) * width;
        for (int x = 0; x < width; x++) {
            int offset = startPos;
            for (int y = height - 1; y >= 0; y--) {
                nv21_rotated[i] = nv21_data[offset + x];
                i++;
                offset -= width;
            }
        }
        // Rotate the U and V color components
        i = buffser_size - 1;
        for (int x = width - 1; x > 0; x = x - 2) {
            int offset = y_size;
            for (int y = 0; y < height / 2; y++) {
                nv21_rotated[i] = nv21_data[offset + x];
                i--;
                nv21_rotated[i] = nv21_data[offset + (x - 1)];
                i--;
                offset += width;
            }
        }
        return nv21_rotated;
    }

    //3/2    2   1
    public static byte[] nv21toNV12(byte[] nv21) {
        int size = nv21.length;
        byte[] nv12 = new byte[size];
        int len = size * 2 / 3;
        System.arraycopy(nv21, 0, nv12, 0, len);

        int i = len;
        while (i < size - 1) {
            nv12[i] = nv21[i + 1];
            nv12[i + 1] = nv21[i];
            i += 2;
        }
        return nv12;
    }

    private static byte[] y;
    private static byte[] u;
    private static byte[] v;
    private static byte[] nv21;//width  height
    static byte[] nv21_rotated;

    public static byte[] format(Image image, int height) {
        Image.Plane[] planes = image.getPlanes();
        // 重复使用同一批byte数组，减少gc频率
        if (y == null) {
            y = new byte[planes[0].getBuffer().limit() - planes[0].getBuffer().position()];
            u = new byte[planes[1].getBuffer().limit() - planes[1].getBuffer().position()];
            v = new byte[planes[2].getBuffer().limit() - planes[2].getBuffer().position()];
        }
        if (image.getPlanes()[0].getBuffer().remaining() == y.length) {
            planes[0].getBuffer().get(y);
            planes[1].getBuffer().get(u);
            planes[2].getBuffer().get(v);
        }
        if (nv21 == null) {
//            实例化一次
            nv21 = new byte[planes[0].getRowStride() * height * 3 / 2];
            nv21_rotated = new byte[planes[0].getRowStride() * height * 3 / 2];
        }
        ImageUtil.yuvToNv21(y, u, v, nv21, planes[0].getRowStride(), height);
        ImageUtil.nv21_rotate_to_90(nv21, nv21_rotated, planes[0].getRowStride(), height);
        return ImageUtil.nv21toNV12(nv21_rotated);
    }

    public static void YUVToNV21_NV12(Image image, byte[] nv21, int w, int h, String type) {
        Image.Plane[] planes = image.getPlanes();
        int remaining0 = planes[0].getBuffer().remaining();
        int remaining1 = planes[1].getBuffer().remaining();
        int remaining2 = planes[2].getBuffer().remaining();
        //分别准备三个数组接收YUV分量。
        byte[] yRawSrcBytes = new byte[remaining0];
        byte[] uRawSrcBytes = new byte[remaining1];
        byte[] vRawSrcBytes = new byte[remaining2];
        planes[0].getBuffer().get(yRawSrcBytes);
        planes[1].getBuffer().get(uRawSrcBytes);
        planes[2].getBuffer().get(vRawSrcBytes);
        int j = 0, k = 0;
        boolean flag = type.equals("NV21");
        for (int i = 0; i < nv21.length; i++) {
            if (i < w * h) {
                //首先填充w*h个Y分量
                nv21[i] = yRawSrcBytes[i];
            } else {
                if (flag) {
                    //若NV21类型 则Y分量分配完后第一个将是V分量
                    nv21[i] = vRawSrcBytes[j];
                    //PixelStride有用数据步长 = 1紧凑按顺序填充，=2每间隔一个填充数据
                    j += planes[1].getPixelStride();
                } else {
                    //若NV12类型 则Y分量分配完后第一个将是U分量
                    nv21[i] = uRawSrcBytes[k];
                    //PixelStride有用数据步长 = 1紧凑按顺序填充，=2每间隔一个填充数据
                    k += planes[2].getPixelStride();
                }
                //紧接着可以交错UV或者VU排列不停的改变flag标志即可交错排列
                flag = !flag;
            }
        }
    }
}

