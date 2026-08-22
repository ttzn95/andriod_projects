package com.example.smartcapture.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.ImageView
import kotlin.math.max
import kotlin.math.min

class ZoomPanImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ImageView(context, attrs) {

    private val imageMatrixState = Matrix()
    private val baseMatrix = Matrix()
    private var minimumScale = 1f
    private var currentScale = 1f
    private var lastX = 0f
    private var lastY = 0f
    private var moving = false

    private val scaleDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                setScale(currentScale * detector.scaleFactor, detector.focusX, detector.focusY)
                return true
            }
        })

    init {
        scaleType = ScaleType.MATRIX
        setBackgroundColor(android.graphics.Color.rgb(27, 37, 44))
    }

    override fun setImageBitmap(bitmap: Bitmap?) {
        super.setImageBitmap(bitmap)
        post { resetZoom() }
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        resetZoom()
    }

    fun resetZoom() {
        val bitmap = drawable ?: return
        val sourceWidth = bitmap.intrinsicWidth.toFloat()
        val sourceHeight = bitmap.intrinsicHeight.toFloat()
        val scale = min(width / sourceWidth, height / sourceHeight)
        minimumScale = scale
        currentScale = scale
        baseMatrix.reset()
        baseMatrix.setScale(scale, scale)
        baseMatrix.postTranslate(
            (width - sourceWidth * scale) / 2f,
            (height - sourceHeight * scale) / 2f
        )
        imageMatrixState.set(baseMatrix)
        imageMatrix = imageMatrixState
    }

    fun zoomIn() = setScale(currentScale * 1.25f, width / 2f, height / 2f)
    fun zoomOut() = setScale(currentScale / 1.25f, width / 2f, height / 2f)

    private fun setScale(target: Float, focusX: Float, focusY: Float) {
        val bounded = target.coerceIn(minimumScale, minimumScale * 4f)
        val factor = bounded / currentScale
        imageMatrixState.postScale(factor, factor, focusX, focusY)
        currentScale = bounded
        constrainMatrix()
        imageMatrix = imageMatrixState
    }

    private fun constrainMatrix() {
        val bounds = getDisplayedBounds()
        var dx = 0f
        var dy = 0f
        if (bounds.width() <= width) dx = width / 2f - bounds.centerX()
        else if (bounds.left > 0) dx = -bounds.left
        else if (bounds.right < width) dx = width - bounds.right
        if (bounds.height() <= height) dy = height / 2f - bounds.centerY()
        else if (bounds.top > 0) dy = -bounds.top
        else if (bounds.bottom < height) dy = height - bounds.bottom
        imageMatrixState.postTranslate(dx, dy)
    }

    private fun getDisplayedBounds(): RectF {
        val drawable = drawable ?: return RectF()
        val bounds = RectF(0f, 0f, drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())
        imageMatrixState.mapRect(bounds)
        return bounds
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                moving = true
            }
            MotionEvent.ACTION_MOVE -> if (!scaleDetector.isInProgress && moving) {
                imageMatrixState.postTranslate(event.x - lastX, event.y - lastY)
                constrainMatrix()
                imageMatrix = imageMatrixState
                lastX = event.x
                lastY = event.y
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> moving = false
        }
        return true
    }

    fun createVisibleCrop(): Bitmap? {
        val source = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap ?: return null
        val inverse = Matrix()
        if (!imageMatrixState.invert(inverse)) return null
        val crop = RectF(0f, 0f, width.toFloat(), height.toFloat())
        inverse.mapRect(crop)
        val left = max(0, crop.left.toInt())
        val top = max(0, crop.top.toInt())
        val right = min(source.width, crop.right.toInt())
        val bottom = min(source.height, crop.bottom.toInt())
        if (right <= left || bottom <= top) return null
        return Bitmap.createBitmap(source, left, top, right - left, bottom - top)
    }
}
