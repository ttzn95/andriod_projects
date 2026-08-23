package com.example.smartcapture.camera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class CropOverlayView(context: Context) : View(context) {

    enum class Mode { FREEFORM, SQUARE }

    private val shadePaint = Paint().apply { color = Color.argb(150, 0, 0, 0) }
    private val linePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val gridPaint = Paint().apply {
        color = Color.argb(190, 255, 255, 255)
        strokeWidth = 1f
    }
    private val handlePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private var selection = RectF()
    private var mode = Mode.FREEFORM
    private var downX = 0f
    private var downY = 0f
    private var moving = false
    private var resizingHandle = 0

    fun beginCrop() {
        if (width == 0 || height == 0) {
            post { beginCrop() }
            return
        }
        selection.set(width * 0.1f, height * 0.1f, width * 0.9f, height * 0.9f)
        constrainSelection()
        visibility = VISIBLE
        invalidate()
    }

    fun setMode(newMode: Mode) {
        mode = newMode
        if (mode == Mode.SQUARE) {
            val size = min(selection.width(), selection.height())
            selection.right = selection.left + size
            selection.bottom = selection.top + size
            constrainSelection()
        }
        invalidate()
    }

    fun selectedRect(): RectF = RectF(selection)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (selection.isEmpty) return
        canvas.drawRect(0f, 0f, width.toFloat(), selection.top, shadePaint)
        canvas.drawRect(0f, selection.top, selection.left, selection.bottom, shadePaint)
        canvas.drawRect(selection.right, selection.top, width.toFloat(), selection.bottom, shadePaint)
        canvas.drawRect(0f, selection.bottom, width.toFloat(), height.toFloat(), shadePaint)

        val thirdWidth = selection.width() / 3f
        val thirdHeight = selection.height() / 3f
        canvas.drawLine(selection.left + thirdWidth, selection.top, selection.left + thirdWidth, selection.bottom, gridPaint)
        canvas.drawLine(selection.left + thirdWidth * 2, selection.top, selection.left + thirdWidth * 2, selection.bottom, gridPaint)
        canvas.drawLine(selection.left, selection.top + thirdHeight, selection.right, selection.top + thirdHeight, gridPaint)
        canvas.drawLine(selection.left, selection.top + thirdHeight * 2, selection.right, selection.top + thirdHeight * 2, gridPaint)
        canvas.drawRect(selection, linePaint)
        val handleRadius = 12f
        canvas.drawCircle(selection.left, selection.top, handleRadius, handlePaint)
        canvas.drawCircle(selection.right, selection.top, handleRadius, handlePaint)
        canvas.drawCircle(selection.left, selection.bottom, handleRadius, handlePaint)
        canvas.drawCircle(selection.right, selection.bottom, handleRadius, handlePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                resizingHandle = findHandle(event.x, event.y)
                moving = resizingHandle == 0 && selection.contains(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> if (resizingHandle != 0) {
                resizeSelection(event.x, event.y)
                invalidate()
            } else if (moving) {
                val dx = event.x - downX
                val dy = event.y - downY
                selection.offset(dx, dy)
                constrainSelection()
                downX = event.x
                downY = event.y
                invalidate()
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                moving = false
                resizingHandle = 0
            }
        }
        return true
    }

    private fun constrainSelection() {
        if (selection.isEmpty) return
        if (mode == Mode.SQUARE) {
            val size = min(selection.width(), selection.height())
            selection.right = selection.left + size
            selection.bottom = selection.top + size
        }
        val dx = when {
            selection.left < 0 -> -selection.left
            selection.right > width -> width - selection.right
            else -> 0f
        }
        val dy = when {
            selection.top < 0 -> -selection.top
            selection.bottom > height -> height - selection.bottom
            else -> 0f
        }
        selection.offset(dx, dy)
    }

    private fun findHandle(x: Float, y: Float): Int {
        val threshold = 48f
        return when {
            abs(x - selection.left) < threshold && abs(y - selection.top) < threshold -> 1
            abs(x - selection.right) < threshold && abs(y - selection.top) < threshold -> 2
            abs(x - selection.left) < threshold && abs(y - selection.bottom) < threshold -> 3
            abs(x - selection.right) < threshold && abs(y - selection.bottom) < threshold -> 4
            else -> 0
        }
    }

    private fun resizeSelection(x: Float, y: Float) {
        val minimumSize = 80f
        when (resizingHandle) {
            1 -> {
                selection.left = min(x, selection.right - minimumSize)
                selection.top = min(y, selection.bottom - minimumSize)
            }
            2 -> {
                selection.right = max(x, selection.left + minimumSize)
                selection.top = min(y, selection.bottom - minimumSize)
            }
            3 -> {
                selection.left = min(x, selection.right - minimumSize)
                selection.bottom = max(y, selection.top + minimumSize)
            }
            4 -> {
                selection.right = max(x, selection.left + minimumSize)
                selection.bottom = max(y, selection.top + minimumSize)
            }
        }
        if (mode == Mode.SQUARE) {
            val size = min(selection.width(), selection.height())
            when (resizingHandle) {
                1 -> {
                    selection.left = selection.right - size
                    selection.top = selection.bottom - size
                }
                2 -> selection.top = selection.bottom - size
                3 -> selection.left = selection.right - size
                4 -> {
                    selection.right = selection.left + size
                    selection.bottom = selection.top + size
                }
            }
        }
        constrainSelection()
    }
}
