package com.journeyapps.barcodescanner.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.zxing.*
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import java.util.*
import kotlin.collections.ArrayList


/**
 * @author xiaoxiaoying
 * @date 3/3/21
 */
object QRCodeDecoder {
    private val HINTS = HashMap<DecodeHintType, Any?>()

    init {
        val allFormats = ArrayList<BarcodeFormat>()
        allFormats.add(BarcodeFormat.AZTEC)
        allFormats.add(BarcodeFormat.CODABAR)
        allFormats.add(BarcodeFormat.CODE_39)
        allFormats.add(BarcodeFormat.CODE_93)
        allFormats.add(BarcodeFormat.CODE_128)
        allFormats.add(BarcodeFormat.DATA_MATRIX)
        allFormats.add(BarcodeFormat.EAN_8)
        allFormats.add(BarcodeFormat.EAN_13)
        allFormats.add(BarcodeFormat.ITF)
        allFormats.add(BarcodeFormat.MAXICODE)
        allFormats.add(BarcodeFormat.PDF_417)
        allFormats.add(BarcodeFormat.QR_CODE)
        allFormats.add(BarcodeFormat.RSS_14)
        allFormats.add(BarcodeFormat.RSS_EXPANDED)
        allFormats.add(BarcodeFormat.UPC_A)
        allFormats.add(BarcodeFormat.UPC_E)
        allFormats.add(BarcodeFormat.UPC_EAN_EXTENSION)
        //容错级别
//        HINTS[DecodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
        HINTS[DecodeHintType.TRY_HARDER] = BarcodeFormat.QR_CODE
        HINTS[DecodeHintType.POSSIBLE_FORMATS] = allFormats
        HINTS[DecodeHintType.CHARACTER_SET] = "utf-8"
    }

    fun String.decoderQRCode(): String? {
        val bitmap = getDecoderBitmap()
        val content = bitmap?.decoderQRCode()
        if (bitmap != null && bitmap.isRecycled)
            bitmap.recycle()
        return content
    }

    fun Bitmap.decoderQRCode(): String? {
        var source: RGBLuminanceSource? = null
        return try {
            val pixels = IntArray(width * height)
            getPixels(pixels, 0, width, 0, 0, width, height)
            source = RGBLuminanceSource(width, height, pixels)
            val hybridBinarizer = HybridBinarizer(source)
            val binaryBitmap = BinaryBitmap(hybridBinarizer)
            val result = MultiFormatReader().decodeWithState(binaryBitmap)

            result.text
        } catch (e: Exception) {
            if (source != null) {
                try {
                    val hybridBinarizer = GlobalHistogramBinarizer(source)
                    val binaryBitmap = BinaryBitmap(hybridBinarizer)
                    val result = MultiFormatReader().decodeWithState(binaryBitmap)
                    return result.text
                } catch (e: Exception) {
                }
            }
            null
        }
    }

    /**
     * 根据路径获取 bitmap
     */
    fun String.getDecoderBitmap(): Bitmap? {
        return try {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = false
            BitmapFactory.decodeFile(this, options)
        } catch (e: Exception) {
            null
        }
    }
}