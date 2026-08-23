package com.example.smartcapture.camera

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.app.AlertDialog
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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

        val currentFiles = files.toMutableList()
        val originalFiles = files.toMutableList()
        var currentIndex = currentFiles.lastIndex
        var currentFile = currentFiles[currentIndex]

        val spacing =
            (12 * activity.resources.displayMetrics.density).toInt()

        val layout =
            activity.createVerticalLayout()

        layout.addView(
            activity.createTitle("Captured Image")
        )

        layout.addView(TextView(activity).apply {
            text = "PAGE IMAGES: ${currentFiles.size}"
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
                    ImageProcessor.decodeForPreview(currentFile)
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

        val controls = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER_HORIZONTAL
        }

        fun iconButton(label: String, description: String, onClick: () -> Unit): Button {
            return activity.createButton(label, onClick).apply {
                text = ""
                contentDescription = description
            }
        }

        fun addThreeColumnRow(first: Button, second: Button, third: Button) {
            val row = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER
            }
            listOf(first, second, third).forEachIndexed { index, button ->
                row.addView(button, LinearLayout.LayoutParams(0, spacing * 3, 1f).apply {
                    setMargins(
                        if (index == 0) 0 else spacing / 3,
                        0,
                        if (index == 2) 0 else spacing / 3,
                        spacing / 3
                    )
                })
            }
            controls.addView(row)
        }

        fun updateImage(file: File) {
            currentFile = file
            imageView.setImageBitmap(ImageProcessor.decodeForPreview(file))
            imageView.resetZoom()
        }

        val previousButton = iconButton("Previous", "Previous image") {
            if (currentIndex > 0) {
                currentIndex--
                updateImage(currentFiles[currentIndex])
            }
        }
        val nextButton = iconButton("Next", "Next image") {
            if (currentIndex < currentFiles.lastIndex) {
                currentIndex++
                updateImage(currentFiles[currentIndex])
            }
        }
        val removeButton = iconButton("Remove", "Remove image") {
            if (currentFiles.size <= 1) {
                AlertDialog.Builder(activity)
                    .setTitle("Cannot remove image")
                    .setMessage("At least one image is required.")
                    .setPositiveButton("OK", null)
                    .show()
            } else {
                val removed = currentFiles.removeAt(currentIndex)
                originalFiles.removeAt(currentIndex)
                activity.removePageImage(removed)
                currentIndex = currentIndex.coerceAtMost(currentFiles.lastIndex)
                updateImage(currentFiles[currentIndex])
                showImagePreview(currentFiles)
            }
        }
        addThreeColumnRow(previousButton, nextButton, removeButton)

        val zoomButton = iconButton("Zoom", "Zoom out") { imageView.zoomOut() }
        val zoomInButton = iconButton("Zoom In", "Zoom in") { imageView.zoomIn() }
        val resetButton = iconButton("Reset", "Restore original image") {
            val original = originalFiles[currentIndex]
            if (currentFile != original) {
                activity.replaceCapturedImage(original, currentFile)
                currentFiles[currentIndex] = original
                updateImage(original)
            } else {
                imageView.resetZoom()
            }
        }
        addThreeColumnRow(zoomButton, zoomInButton, resetButton)

        val cropButton = iconButton("Crop", "Crop image") {
            val cropped = imageView.createVisibleCrop()
            if (cropped == null) {
                showCaptureError("Unable to crop this image.")
                return@iconButton
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
            currentFiles[currentIndex] = croppedFile
            updateImage(croppedFile)
        }
        val addCameraButton = iconButton("+", "Add image from camera") {
            activity.startDocumentCamera()
        }
        val backButton = iconButton("Back", "Back to settings") {
            activity.discardPageImages()
            activity.showColorModeScreen()
        }
        addThreeColumnRow(cropButton, addCameraButton, backButton)

        val homeButton = iconButton("Home", "Return home") {
            activity.discardPageImages()
            activity.showAuthenticatedHome()
        }
        val spacer = iconButton("", "") {}.apply {
            visibility = View.INVISIBLE
            isEnabled = false
        }
        val secondSpacer = iconButton("", "") {}.apply {
            visibility = View.INVISIBLE
            isEnabled = false
        }
        addThreeColumnRow(homeButton, spacer, secondSpacer)

        // ---------------------------------------------------------
        // CONFIRM
        // ---------------------------------------------------------

        controls.addView(
            activity.createButton("Confirm And Upload") {
                activity.uploadCapturedImage()
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, spacing * 4)
        )

        layout.addView(
            controls,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
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
