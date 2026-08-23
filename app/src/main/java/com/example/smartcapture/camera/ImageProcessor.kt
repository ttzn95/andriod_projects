package com.example.smartcapture.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import java.io.File
import java.io.FileOutputStream

object ImageProcessor {

    private const val UPLOAD_JPEG_QUALITY = 88
    private const val DEFAULT_UPLOAD_DIMENSION = 3000
    private const val MERGE_GAP_PX = 10

    fun convertToBlackAndWhite(source: File, destination: File): Boolean {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.absolutePath, bounds)
        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, DEFAULT_UPLOAD_DIMENSION)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val sourceBitmap = BitmapFactory.decodeFile(source.absolutePath, options) ?: return false
        val result = Bitmap.createBitmap(
            sourceBitmap.width,
            sourceBitmap.height,
            Bitmap.Config.ARGB_8888
        )

        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        }
        Canvas(result).drawBitmap(sourceBitmap, 0f, 0f, paint)

        FileOutputStream(destination).use { output ->
            result.compress(Bitmap.CompressFormat.JPEG, UPLOAD_JPEG_QUALITY, output)
        }
        sourceBitmap.recycle()
        result.recycle()
        return true
    }

    fun decodeForPreview(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, 2048)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeFile(file.absolutePath, options)
    }

    fun mergeVertically(files: List<File>, destination: File, maximumDimension: Int): Boolean {
        val bitmaps = files.mapNotNull { decodeForUpload(it, maximumDimension) }
        if (bitmaps.size != files.size) {
            bitmaps.forEach(Bitmap::recycle)
            return false
        }

        val outputWidth = bitmaps.maxOf { it.width }
        val outputHeight = bitmaps.sumOf { bitmap ->
            (bitmap.height * outputWidth.toFloat() / bitmap.width).toInt()
        } + MERGE_GAP_PX * (bitmaps.size - 1)
        if (outputWidth <= 0 || outputHeight <= 0) {
            bitmaps.forEach(Bitmap::recycle)
            return false
        }

        val merged = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(merged)
        canvas.drawColor(android.graphics.Color.WHITE)
        var top = 0f
        bitmaps.forEachIndexed { index, bitmap ->
            val scaledHeight = bitmap.height * outputWidth.toFloat() / bitmap.width
            val target = android.graphics.RectF(0f, top, outputWidth.toFloat(), top + scaledHeight)
            canvas.drawBitmap(bitmap, null, target, null)
            top += scaledHeight + if (index < bitmaps.lastIndex) MERGE_GAP_PX else 0
            bitmap.recycle()
        }

        FileOutputStream(destination).use { output ->
            merged.compress(Bitmap.CompressFormat.JPEG, UPLOAD_JPEG_QUALITY, output)
        }
        merged.recycle()
        return true
    }

    fun compressForUpload(source: File, destination: File, maximumDimension: Int): Boolean {
        val bitmap = decodeForUpload(source, maximumDimension) ?: return false
        return try {
            FileOutputStream(destination).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, UPLOAD_JPEG_QUALITY, output)
            }
            true
        } finally {
            bitmap.recycle()
        }
    }

    private fun decodeForUpload(file: File, maximumDimension: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(
                bounds.outWidth,
                bounds.outHeight,
                maximumDimension
            )
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeFile(file.absolutePath, options)
    }

    private fun calculateSampleSize(width: Int, height: Int, maximumDimension: Int): Int {
        var sampleSize = 1
        while (width / sampleSize > maximumDimension || height / sampleSize > maximumDimension) {
            sampleSize *= 2
        }
        return sampleSize
    }
}