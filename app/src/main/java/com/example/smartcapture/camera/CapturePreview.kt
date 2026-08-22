package com.example.smartcapture.camera

import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.smartcapture.MainActivity
import java.io.File
import java.io.FileOutputStream

class CapturePreview(
    private val activity: MainActivity
) {

    fun showImagePreview(files: List<File>) {

        if (files.isEmpty()) return

        var currentIndex = files.lastIndex
        var currentFile = files[currentIndex]

        val spacing =
            (12 * activity.resources.displayMetrics.density).toInt()

        val layout =
            activity.createVerticalLayout()

        layout.addView(
            activity.createTitle("Captured Image")
        )

        layout.addView(TextView(activity).apply {
            text = "PAGE IMAGES: ${files.size}"
            textSize = 13f
            setTextColor(Color.rgb(168, 184, 180))
            setPadding(0, 0, 0, 10)
        })

        layout.addView(
            TextView(activity).apply {
                text = "REVIEW BEFORE UPLOAD"
                textSize = 13f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                setTextColor(Color.rgb(168, 184, 180))
                setPadding(0, 0, 0, 16)
            }
        )

        val imageView =
            ZoomPanImageView(activity).apply {

                setImageBitmap(
                    BitmapFactory.decodeFile(
                        currentFile.absolutePath
                    )
                )

                minimumHeight = 0

                setPadding(spacing, spacing, spacing, spacing)
                background = GradientDrawable().apply {
                    cornerRadius = spacing.toFloat() * 1.5f
                    setColor(Color.rgb(27, 37, 44))
                    setStroke(1, Color.rgb(67, 84, 87))
                }
            }

        layout.addView(
            imageView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                bottomMargin = spacing * 2
            }
        )

        val zoomControls = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
        }

        zoomControls.addView(activity.createButton("-") {
            imageView.zoomOut()
        })
        zoomControls.addView(activity.createButton("Reset") {
            imageView.resetZoom()
        })
        zoomControls.addView(activity.createButton("+") {
            imageView.zoomIn()
        })
        layout.addView(zoomControls)

        val pageControls = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
        }
        val previousButton = activity.createButton("Previous") {
            if (currentIndex > 0) {
                currentIndex--
                currentFile = files[currentIndex]
                imageView.setImageBitmap(BitmapFactory.decodeFile(currentFile.absolutePath))
            }
        }
        val nextButton = activity.createButton("Next") {
            if (currentIndex < files.lastIndex) {
                currentIndex++
                currentFile = files[currentIndex]
                imageView.setImageBitmap(BitmapFactory.decodeFile(currentFile.absolutePath))
            }
        }
        pageControls.addView(previousButton)
        pageControls.addView(nextButton)
        layout.addView(pageControls)

        layout.addView(activity.createButton("Add from Gallery") {
            activity.openGalleryPicker()
        })
        layout.addView(activity.createButton("Add from Camera") {
            activity.startDocumentCamera()
        })

        layout.addView(activity.createButton("Crop") {
            val cropped = imageView.createVisibleCrop()
            if (cropped == null) {
                activity.showCaptureError("Unable to crop this image.")
                return@createButton
            }
            val croppedFile = File(
                activity.cacheDir,
                "capture_crop_${System.currentTimeMillis()}.jpg"
            )
            FileOutputStream(croppedFile).use { output ->
                cropped.compress(Bitmap.CompressFormat.JPEG, 95, output)
            }
            cropped.recycle()
            activity.replaceCapturedImage(croppedFile, currentFile)
            currentFile = croppedFile
            imageView.setImageBitmap(BitmapFactory.decodeFile(currentFile.absolutePath))
        })

        // ---------------------------------------------------------
        // CONFIRM
        // ---------------------------------------------------------

        layout.addView(
            activity.createButton("Confirm") {
                activity.uploadCapturedImage()
            }
        )

        activity.setContentView(layout)
    }

    fun showCaptureError(
        message: String
    ) {

        val layout =
            activity.createVerticalLayout()

        layout.addView(
            activity.createTitle("Capture Error")
        )

        val errorText =
            TextView(activity).apply {

                text = message

                textSize = 17f

                setPadding(
                    0,
                    20,
                    0,
                    30
                )
            }

        layout.addView(errorText)

        layout.addView(
            activity.createButton("Try Again") {

                activity.startDocumentCamera()
            }
        )

        layout.addView(
            activity.createButton("Back") {

                activity.showColorModeScreen()
            }
        )

        activity.setContentView(layout)
    }
}
