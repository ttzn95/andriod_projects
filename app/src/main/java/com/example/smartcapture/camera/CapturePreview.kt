package com.example.smartcapture.camera

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.app.AlertDialog
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.smartcapture.MainActivity
import com.example.smartcapture.R
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
            activity.createVerticalLayout().apply {
                setPadding(spacing + spacing / 3, spacing, spacing + spacing / 3, 0)
            }

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

                background = GradientDrawable().apply {
                    cornerRadius = spacing.toFloat() * 1.5f
                    setColor(Color.rgb(27, 37, 44))
                    setStroke(1, Color.rgb(67, 84, 87))
                }
            }

        val imageContainer = FrameLayout(activity).apply {
            addView(imageView, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ))
        }
        val cropOverlay = CropOverlayView(activity).apply {
            visibility = View.GONE
        }
        imageContainer.addView(cropOverlay, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))
        layout.addView(imageContainer, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f
        ).apply {
            bottomMargin = spacing * 2
        })
        val bottomControls = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, spacing / 2, 0, spacing)
            setBackgroundColor(Color.rgb(245, 247, 246))
            elevation = spacing.toFloat()
        }
        val reviewControlsLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
        }
        val cropControlsLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }

        fun toolbarButton(label: String, description: String, onClick: () -> Unit): Button {
            return activity.createButton(label, onClick).apply {
                text = ""
                contentDescription = description
                minHeight = spacing * 4
                minimumHeight = spacing * 4
                setPadding(0, 0, 0, 0)
                setTextColor(Color.rgb(27, 37, 44))
                background = null
                elevation = 0f
            }
        }

        fun addToolbarButton(toolbar: LinearLayout, button: Button) {
            toolbar.addView(button, LinearLayout.LayoutParams(spacing * 4, spacing * 4).apply {
                marginStart = spacing / 3
                marginEnd = spacing / 3
            })
        }

        fun secondaryButton(label: String, description: String, onClick: () -> Unit): Button {
            return activity.createOutlinedButton(label, onClick).apply {
                contentDescription = description
            }
        }

        fun updateImage(file: File) {
            currentFile = file
            imageView.setImageBitmap(ImageProcessor.decodeForPreview(file))
            imageView.resetZoom()
        }

        val previousButton = toolbarButton("Previous", "Previous image") {
            if (currentIndex > 0) {
                currentIndex--
                updateImage(currentFiles[currentIndex])
            }
        }
        val nextButton = toolbarButton("Next", "Next image") {
            if (currentIndex < currentFiles.lastIndex) {
                currentIndex++
                updateImage(currentFiles[currentIndex])
            }
        }
        val removeButton = toolbarButton("Remove", "Remove image") {
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
        val zoomButton = toolbarButton("Zoom", "Zoom out") { imageView.zoomOut() }
        val zoomInButton = toolbarButton("Zoom In", "Zoom in") { imageView.zoomIn() }
        val resetButton = toolbarButton("Reset", "Restore original image") {
            val original = originalFiles[currentIndex]
            if (currentFile != original) {
                activity.replaceCapturedImage(original, currentFile)
                currentFiles[currentIndex] = original
                updateImage(original)
            } else {
                imageView.resetZoom()
            }
        }
        val rotateButton = toolbarButton("Rotate", "Rotate image clockwise") {
            imageView.rotateClockwise()
        }

        fun saveCroppedImage(cropped: Bitmap?) {
            if (cropped == null) {
                showCaptureError("Unable to crop this image.")
                return
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
            cropOverlay.visibility = View.GONE
            cropControlsLayout.visibility = View.GONE
            reviewControlsLayout.visibility = View.VISIBLE
        }

        val autoCropButton = toolbarButton("Auto Crop", "Auto crop the visible image area") {
            saveCroppedImage(imageView.createVisibleCrop())
        }
        val manualCropButton = toolbarButton("Manual Crop", "Enter manual crop mode") {
            cropOverlay.visibility = View.VISIBLE
            cropOverlay.beginCrop()
            reviewControlsLayout.visibility = View.GONE
            cropControlsLayout.visibility = View.VISIBLE
        }
        val addCameraButton = toolbarButton("Add", "Add image from camera") {
            activity.startDocumentCamera()
        }
        val toolbar = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        listOf(
            previousButton,
            nextButton,
            removeButton,
            zoomButton,
            zoomInButton,
            rotateButton,
            resetButton,
            autoCropButton,
            manualCropButton,
            addCameraButton
        ).forEach { addToolbarButton(toolbar, it) }
        reviewControlsLayout.addView(HorizontalScrollView(activity).apply {
            isHorizontalScrollBarEnabled = false
            addView(toolbar, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        })

        val freeformButton = secondaryButton("Freeform", "Freeform crop") {
            cropOverlay.setMode(CropOverlayView.Mode.FREEFORM)
        }.apply {
            setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        }
        val squareButton = secondaryButton("Square", "Square crop") {
            cropOverlay.setMode(CropOverlayView.Mode.SQUARE)
        }.apply {
            setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        }
        val cropButtonHeight = (56 * activity.resources.displayMetrics.density).toInt()
        fun cropButtonParams() = LinearLayout.LayoutParams(0, cropButtonHeight, 1f)
        val cropModeRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(freeformButton, cropButtonParams().apply {
                marginEnd = spacing / 3
            })
            addView(squareButton, cropButtonParams())
        }
        cropControlsLayout.addView(cropModeRow, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = activity.resources.displayMetrics.density.times(12).toInt()
        })
        val applyCropButton = activity.createButton("Apply") {
            saveCroppedImage(imageView.createCrop(cropOverlay.selectedRect()))
        }.apply {
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check, 0, 0, 0)
            setTextColor(Color.WHITE)
            compoundDrawables.forEach { drawable -> drawable?.setTint(Color.WHITE) }
        }
        val cancelCropButton = secondaryButton("Cancel", "Cancel crop") {
            cropOverlay.visibility = View.GONE
            cropControlsLayout.visibility = View.GONE
            reviewControlsLayout.visibility = View.VISIBLE
        }.apply {
            setTextColor(Color.rgb(27, 37, 44))
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_close, 0, 0, 0)
            compoundDrawables.forEach { drawable -> drawable?.setTint(Color.rgb(27, 37, 44)) }
        }
        val cropActionRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(cancelCropButton, cropButtonParams().apply {
                marginEnd = spacing / 3
            })
            addView(applyCropButton, cropButtonParams())
        }
        cropControlsLayout.addView(cropActionRow)
        val backButton = secondaryButton("Back", "Back to settings") {
            activity.discardPageImages()
            activity.showColorModeScreen()
        }
        val homeButton = secondaryButton("Home", "Return home") {
            activity.discardPageImages()
            activity.showAuthenticatedHome()
        }
        val navigationRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        navigationRow.addView(backButton, LinearLayout.LayoutParams(0, spacing * 4, 1f).apply {
            marginEnd = spacing / 3
        })
        navigationRow.addView(homeButton, LinearLayout.LayoutParams(0, spacing * 4, 1f))
        reviewControlsLayout.addView(navigationRow, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = spacing / 2
        })

        // ---------------------------------------------------------
        // CONFIRM
        // ---------------------------------------------------------

        reviewControlsLayout.addView(
            activity.createButton("Confirm And Upload") {
                activity.uploadCapturedImage()
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, spacing * 4).apply {
                topMargin = spacing * 2
                marginStart = spacing + spacing / 3
                marginEnd = spacing + spacing / 3
            }
        )
        bottomControls.addView(reviewControlsLayout)
        bottomControls.addView(cropControlsLayout)
        layout.addView(bottomControls, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))

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
