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

    fun convertToBlackAndWhite(source: File, destination: File): Boolean {
        val sourceBitmap = BitmapFactory.decodeFile(source.absolutePath) ?: return false
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
            result.compress(Bitmap.CompressFormat.JPEG, 95, output)
        }
        sourceBitmap.recycle()
        result.recycle()
        return true
    }
}