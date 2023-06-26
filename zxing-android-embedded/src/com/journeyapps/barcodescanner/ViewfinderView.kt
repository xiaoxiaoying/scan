/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.journeyapps.barcodescanner

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.google.zxing.ResultPoint
import com.google.zxing.client.android.R
import com.journeyapps.barcodescanner.DecoratedBarcodeView.Companion.dipToPix
import java.util.*

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
open class ViewfinderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cornersPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mTickPath = Path()
    private var resultBitmap: Bitmap? = null

    var maskColor: Int = 0

    private val resultColor: Int

    private val laserColor: Int

    private val resultPointColor: Int

    var laserVisibility: Boolean = false

    private var strokeColor: Int = Color.WHITE

    private var strokeWidth: Int = context.dipToPix(2)

    private var scannerAlpha = 0

    private var possibleResultPoints = ArrayList<ResultPoint>(MAX_RESULT_POINTS)
    private var lastPossibleResultPoints = ArrayList<ResultPoint>(MAX_RESULT_POINTS)

    private val cameraListener = object : CameraPreview.StateListener {
        override fun previewSized() {
            refreshSizes()
            invalidate()
        }

        override fun previewStarted() {}
        override fun previewStopped() {}
        override fun cameraError(error: Exception) {}
        override fun cameraClosed() {}
    }

    var cameraPreview: CameraPreview? = null
        set(value) {
            field?.removeStateListener(cameraListener)
            field = value
            value?.addStateListener(cameraListener)
        }

    /**
     * 距离上边距
     */
    var offsetTop: Int = -1
        set(value) {
            field = value
            cameraListener.previewSized()
        }

    // Cache the framingRect and previewSize, so that we can still draw it after the preview
    // stopped.
    private var framingRect: Rect? = null
    private var previewSize: Size? = null

    private fun refreshSizes() {
        this.framingRect = cameraPreview?.framingRect
        if (framingRect != null && offsetTop != -1) {
            val newLeft = framingRect!!.left
            framingRect?.offsetTo(newLeft, offsetTop)
        }
        this.previewSize = cameraPreview?.previewSize
    }

    override fun onDraw(canvas: Canvas) {
        refreshSizes()
        framingRect ?: return
        previewSize ?: return

        val frame: Rect = framingRect!!

        // Draw the exterior (i.e. outside the framing rect) darkened
        paint.color = if (resultBitmap != null) resultColor else maskColor
        canvas.drawRect(0f, 0f, width.toFloat(), frame.top.toFloat(), paint)
        canvas.drawRect(
            0f,
            frame.top.toFloat(),
            frame.left.toFloat(),
            (frame.bottom + 1).toFloat(),
            paint
        )
        canvas.drawRect(
            (frame.right + 1).toFloat(),
            frame.top.toFloat(),
            width.toFloat(),
            (frame.bottom + 1).toFloat(),
            paint
        )
        canvas.drawRect(0f, (frame.bottom + 1).toFloat(), width.toFloat(), height.toFloat(), paint)
        if (resultBitmap != null) {
            // Draw the opaque result bitmap over the scanning rectangle
            paint.alpha = CURRENT_POINT_OPACITY
            canvas.drawBitmap(resultBitmap!!, null, frame, paint)
        } else {

            drawFourCorners(canvas, frame)

            // If wanted, draw a red "laser scanner" line through the middle to show decoding is active
            if (laserVisibility) {
                paint.color = laserColor
                paint.alpha = SCANNER_ALPHA[scannerAlpha]
                scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.size
                val middle = frame.height() / 2 + frame.top
                canvas.drawRect(
                    (frame.left + 2).toFloat(),
                    (middle - 1).toFloat(),
                    (frame.right - 1).toFloat(),
                    (middle + 2).toFloat(),
                    paint
                )
            }
            val scaleX = width / previewSize!!.width.toFloat()
            val scaleY = height / previewSize!!.height.toFloat()

            // draw the last possible result points
            if (lastPossibleResultPoints.isNotEmpty()) {
                paint.alpha = CURRENT_POINT_OPACITY / 2
                paint.color = resultPointColor
                val radius = POINT_SIZE / 2.0f

                lastPossibleResultPoints.forEach {
                    canvas.drawCircle(
                        it.x * scaleX,
                        it.y * scaleY,
                        radius, paint
                    )
                }
                lastPossibleResultPoints.clear()
            }

            // draw current possible result points
            if (possibleResultPoints.isNotEmpty()) {
                paint.alpha = CURRENT_POINT_OPACITY
                paint.color = resultPointColor

                possibleResultPoints.forEach {
                    canvas.drawCircle(
                        it.x * scaleX,
                        it.y * scaleY,
                        POINT_SIZE.toFloat(),
                        paint
                    )
                }


                // swap and clear buffers
                val temp = possibleResultPoints
                possibleResultPoints = lastPossibleResultPoints
                lastPossibleResultPoints = temp
                possibleResultPoints.clear()
            }

            // Request another update at the animation interval, but only repaint the laser line,
            // not the entire viewfinder mask.
            postInvalidateDelayed(
                ANIMATION_DELAY,
                frame.left - POINT_SIZE,
                frame.top - POINT_SIZE,
                frame.right + POINT_SIZE,
                frame.bottom + POINT_SIZE
            )
        }
    }

    /**
     * 绘制四个角
     */
    private fun drawFourCorners(canvas: Canvas, frame: Rect) {
        cornersPaint.style = Paint.Style.STROKE
        cornersPaint.strokeCap = Paint.Cap.ROUND
        cornersPaint.color = strokeColor
        cornersPaint.strokeWidth = strokeWidth.toFloat()

        mTickPath.reset()
        val frameWidth = frame.width()
        val frameHeight = frame.height()

        val cornersHLength = frameWidth / 4
        val cornersVLength = frameHeight / 4

        val leftTopX = frame.left + strokeWidth / 2f
        val leftTopY = frame.top + strokeWidth / 2f

        mTickPath.moveTo(leftTopX, leftTopY)
        mTickPath.lineTo(leftTopX, leftTopY + cornersVLength)
        canvas.drawPath(mTickPath, cornersPaint)

        mTickPath.reset()
        mTickPath.moveTo(leftTopX, leftTopY)
        mTickPath.lineTo(leftTopX + cornersHLength, leftTopY)
        canvas.drawPath(mTickPath, cornersPaint)

        val rightTopX = frame.right - strokeWidth / 2f
        val rightTopY = frame.top + strokeWidth / 2f
        mTickPath.reset()
        mTickPath.moveTo(rightTopX, rightTopY)
        mTickPath.lineTo(rightTopX, rightTopY + cornersVLength)
        canvas.drawPath(mTickPath, cornersPaint)

        mTickPath.reset()
        mTickPath.moveTo(rightTopX, leftTopY)
        mTickPath.lineTo(rightTopX - cornersHLength, rightTopY)
        canvas.drawPath(mTickPath, cornersPaint)

        val bottomLeftX = frame.left + strokeWidth / 2f
        val bottomLeftY = frame.bottom - strokeWidth / 2f

        mTickPath.reset()
        mTickPath.moveTo(bottomLeftX, bottomLeftY)
        mTickPath.lineTo(bottomLeftX, bottomLeftY - cornersVLength)
        canvas.drawPath(mTickPath, cornersPaint)

        mTickPath.reset()
        mTickPath.moveTo(bottomLeftX, bottomLeftY)
        mTickPath.lineTo(bottomLeftX + cornersHLength, bottomLeftY)
        canvas.drawPath(mTickPath, cornersPaint)

        val bottomRightX = frame.right - strokeWidth / 2f
        val bottomRightY = frame.bottom - strokeWidth / 2f

        mTickPath.reset()
        mTickPath.moveTo(bottomRightX, bottomRightY)
        mTickPath.lineTo(bottomRightX, bottomRightY - cornersVLength)
        canvas.drawPath(mTickPath, cornersPaint)

        mTickPath.reset()
        mTickPath.moveTo(bottomRightX, bottomRightY)
        mTickPath.lineTo(bottomRightX - cornersHLength, bottomRightY)
        canvas.drawPath(mTickPath, cornersPaint)

    }

    fun drawViewfinder() {
        val resultBitmap = resultBitmap
        this.resultBitmap = null
        resultBitmap?.recycle()
        invalidate()
    }

    /**
     * Draw a bitmap with the result points highlighted instead of the live scanning display.
     *
     * @param result An image of the result.
     */
    fun drawResultBitmap(result: Bitmap?) {
        resultBitmap = result
        invalidate()
    }

    /**
     * Only call from the UI thread.
     *
     * @param point a point to draw, relative to the preview frame
     */
    fun addPossibleResultPoint(point: ResultPoint) {
        if (possibleResultPoints.size < MAX_RESULT_POINTS) possibleResultPoints.add(point)
    }


    companion object {
        private const val TAG = "FINDER"
        private val SCANNER_ALPHA = intArrayOf(0, 64, 128, 192, 255, 192, 128, 64)
        private const val ANIMATION_DELAY = 80L
        private const val CURRENT_POINT_OPACITY = 0xA0
        private const val MAX_RESULT_POINTS = 20
        private const val POINT_SIZE = 6
    }

    // This constructor is used when the class is built from an XML resource.
    init {

        // Initialize these once for performance rather than calling them every time in onDraw().
        // Get setted attributes on view
        val attributes = context.obtainStyledAttributes(
            attrs,
            R.styleable.ViewfinderView,
            defStyleAttr,
            defStyleRes
        )
        maskColor = attributes.getColor(
            R.styleable.ViewfinderView_zxing_viewfinder_mask,
            ContextCompat.getColor(context, R.color.zxing_viewfinder_mask)
        )
        resultColor = attributes.getColor(
            R.styleable.ViewfinderView_zxing_result_view,
            ContextCompat.getColor(context, R.color.zxing_result_view)
        )
        laserColor = attributes.getColor(
            R.styleable.ViewfinderView_zxing_viewfinder_laser,
            ContextCompat.getColor(context, R.color.zxing_viewfinder_laser)
        )
        resultPointColor = attributes.getColor(
            R.styleable.ViewfinderView_zxing_possible_result_points,
            ContextCompat.getColor(context, R.color.zxing_possible_result_points)
        )
        laserVisibility = attributes.getBoolean(
            R.styleable.ViewfinderView_zxing_viewfinder_laser_visibility,
            true
        )
        attributes.recycle()

    }
}