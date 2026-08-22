package com.example.smartcapture.camera

import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.smartcapture.MainActivity
import java.io.File

class CapturePreview(
    private val activity: MainActivity
) {

    fun showImagePreview(file: File) {

        val spacing =
            (12 * activity.resources.displayMetrics.density).toInt()

        val layout =
            activity.createVerticalLayout()

        layout.addView(
            activity.createTitle("Captured Image")
        )

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
            ImageView(activity).apply {

                setImageBitmap(
                    BitmapFactory.decodeFile(
                        file.absolutePath
                    )
                )

                adjustViewBounds = false

                scaleType =
                    ImageView.ScaleType.FIT_CENTER

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

        // ---------------------------------------------------------
        // RETAKE
        // ---------------------------------------------------------

        layout.addView(
            activity.createButton("Retake") {

                activity.startDocumentCamera()
            }
        )

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
