package com.journeyapps.barcodescanner.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.journeyapps.barcodescanner.DecoratedBarcodeView.Companion.getScreenWidth
import java.util.*
import kotlin.collections.HashMap


/**
 * @author xiaoxiaoying
 * @date 3/3/21
 */
object CodeCreator {
    var imageHALFWIDTH = 150

    /**
     * 生成二维码，默认大小为500*500
     *
     * @param text 需要生成二维码的文字、网址等
     * @return bitmap
     */
    @JvmStatic
    fun createQRCode(text: String?, size: Int = 150, margin: Int = 1): Bitmap? {

        text ?: return null

        return try {

            val bitMatrix = QRCodeWriter().encode(
                text,
                BarcodeFormat.QR_CODE, size, size, getHints(margin)
            )
            val pixels = IntArray(size * size)
            for (y in 0 until size) {
                for (x in 0 until size) {
                    if (bitMatrix[x, y]) {
                        pixels[y * size + x] = -0x1000000
                    } else {
                        pixels[y * size + x] = -0x1
                    }
                }
            }
            val bitmap = Bitmap.createBitmap(
                size, size,
                Bitmap.Config.ARGB_8888
            )
            bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
            bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
            null
        }
    }


    /**
     * 生成带logo的二维码，默认二维码的大小为500，logo为二维码的1/5
     *
     * @param text    需要生成二维码的文字、网址等
     * @param mBitmap logo文件
     * @return bitmap
     */
    fun createQRCodeWithLogo(text: String?, mBitmap: Bitmap): Bitmap? {
        return createQRCodeWithLogo(text, 500, mBitmap)
    }


    /**
     * 生成带logo的二维码，logo默认为二维码的1/5
     *
     * @param text    需要生成二维码的文字、网址等
     * @param size    需要生成二维码的大小（）
     * @param bitmap logo文件
     * @return bitmap
     */
    @JvmStatic
    fun createQRCodeWithLogo(
        text: String?,
        size: Int,
        bitmap: Bitmap
    ): Bitmap? {
        var mBitmap = bitmap
        return try {
            val imageHalfwidth = size / 10
            val hints = Hashtable<EncodeHintType, Any?>()
            hints[EncodeHintType.CHARACTER_SET] = "utf-8"
            /*
             * 设置容错级别，默认为ErrorCorrectionLevel.L
             * 因为中间加入logo所以建议你把容错级别调至H,否则可能会出现识别不了
             */hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
            //设置空白边距的宽度
            hints[EncodeHintType.MARGIN] = 1 //default is 4
            // 图像数据转换，使用了矩阵转换
            val bitMatrix = QRCodeWriter().encode(
                text,
                BarcodeFormat.QR_CODE, size, size, hints
            )
            val width = bitMatrix.width //矩阵高度
            val height = bitMatrix.height //矩阵宽度
            val halfW = width / 2
            val halfH = height / 2
            val m = Matrix()
            val sx = 2.toFloat() * imageHalfwidth / mBitmap.width
            val sy = (2.toFloat() * imageHalfwidth
                    / mBitmap.height)
            m.setScale(sx, sy)
            //设置缩放信息
            //将logo图片按martix设置的信息缩放
            mBitmap = Bitmap.createBitmap(
                mBitmap, 0, 0,
                mBitmap.width, mBitmap.height, m, false
            )
            val pixels = IntArray(size * size)
            for (y in 0 until size) {
                for (x in 0 until size) {
                    if (x > halfW - imageHalfwidth && x < halfW + imageHalfwidth &&
                        y > halfH - imageHalfwidth && y < halfH + imageHalfwidth
                    ) {
                        //该位置用于存放图片信息
                        //记录图片每个像素信息
                        pixels[y * width + x] = mBitmap.getPixel(
                            x - halfW
                                    + imageHalfwidth, y - halfH + imageHalfwidth
                        )
                    } else {
                        if (bitMatrix[x, y]) {
                            pixels[y * size + x] = -0x1000000
                        } else {
                            pixels[y * size + x] = -0x1
                        }
                    }
                }
            }
            val newBitmap = Bitmap.createBitmap(
                size, size,
                Bitmap.Config.ARGB_8888
            )
            newBitmap.setPixels(pixels, 0, size, 0, 0, size, size)
            newBitmap
        } catch (e: WriterException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 生成二维码Bitmap  此方法与上面的createQRCodeWithLogo方法效果一样（设置Bitmap两种写方法）
     *
     * @param context
     * @param data     文本内容
     * @param logoBm   二维码中心的Logo图标（可以为null）
     * @return 合成后的bitmap
     */
    fun createQRImage(context: Context, data: String?, logoBm: Bitmap?, margin: Int = 1): Bitmap? {
        try {
            if (data == null || "" == data) {
                return null
            }
            var widthPix = context.getScreenWidth()
            widthPix = widthPix / 5 * 3
            val heightPix = widthPix


            // 图像数据转换，使用了矩阵转换
            val bitMatrix =
                QRCodeWriter().encode(
                    data,
                    BarcodeFormat.QR_CODE,
                    widthPix,
                    heightPix,
                    getHints(margin)
                )
            val pixels = IntArray(widthPix * heightPix)
            // 下面这里按照二维码的算法，逐个生成二维码的图片，
            // 两个for循环是图片横列扫描的结果

            (0 until heightPix).forEach { y ->
                (0 until widthPix).forEach { x ->
                    if (bitMatrix[x, y]) {
                        pixels[y * widthPix + x] = -0x1000000
                    } else {
                        pixels[y * widthPix + x] = -0x1
                    }
                }
            }

            // 生成二维码图片的格式，使用ARGB_8888
            var bitmap = Bitmap.createBitmap(widthPix, heightPix, Bitmap.Config.ARGB_8888)
            bitmap!!.setPixels(pixels, 0, widthPix, 0, 0, widthPix, heightPix)
            if (logoBm != null) {
                bitmap = addLogo(bitmap, logoBm)
            }
            return bitmap
            //必须使用compress方法将bitmap保存到文件中再进行读取。直接返回的bitmap是没有任何压缩的，内存消耗巨大！
            //return bitmap != null && bitmap.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(filePath));
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * 在二维码中间添加Logo图案
     * @param logo 可以看作是APP的logo 或者个人头像等等
     * @return bitmap
     */
    private fun addLogo(src: Bitmap?, logo: Bitmap?): Bitmap? {
        src ?: return null
        logo ?: return src

        //获取图片的宽高
        val srcWidth = src.width
        val srcHeight = src.height
        val logoWidth = logo.width
        val logoHeight = logo.height
        if (srcWidth == 0 || srcHeight == 0) {
            return null
        }
        if (logoWidth == 0 || logoHeight == 0) {
            return src
        }
        //logo大小为二维码整体大小的1/5
        val scaleFactor = srcWidth * 1.0f / 5 / logoWidth
        var bitmap = Bitmap.createBitmap(srcWidth, srcHeight, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap!!)
            canvas.drawBitmap(src, 0f, 0f, null)
            canvas.scale(
                scaleFactor,
                scaleFactor,
                (srcWidth / 2).toFloat(),
                (srcHeight / 2).toFloat()
            )
            canvas.drawBitmap(
                logo,
                ((srcWidth - logoWidth) / 2).toFloat(),
                ((srcHeight - logoHeight) / 2).toFloat(),
                null
            )
            canvas.save()
            canvas.restore()
        } catch (e: Exception) {
            bitmap = null
            e.stackTrace
        }
        return bitmap
    }

    @JvmStatic
    fun getHints(margin: Int = 1): HashMap<EncodeHintType, Any?> {
        //配置参数
        val hints = HashMap<EncodeHintType, Any?>()
        hints[EncodeHintType.CHARACTER_SET] = "utf-8"
        //容错级别
        hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
        //设置空白边距的宽度
        hints[EncodeHintType.MARGIN] = margin //default is 4
        return hints
    }
}