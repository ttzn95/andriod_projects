package com.example.smartcapture.camera

import android.graphics.BitmapFactory
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

        val layout =
            activity.createVerticalLayout()

        layout.addView(
            activity.createTitle("Captured Image")
        )

        val imageView =
            ImageView(activity).apply {

                setImageBitmap(
                    BitmapFactory.decodeFile(
                        file.absolutePath
                    )
                )

                adjustViewBounds = true

                scaleType =
                    ImageView.ScaleType.FIT_CENTER
            }

        layout.addView(
            imageView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
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
