package com.example.smartcapture.camera

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.smartcapture.capture.CaptureSettings
import com.example.smartcapture.capture.OrientationMode 
import android.widget.Button
import com.example.smartcapture.MainActivity
import java.io.File

class DocumentCamera(
    private val activity: MainActivity,
    private val captureSettings: CaptureSettings,
    private val onImageCaptured: (File) -> Unit,
    private val onCaptureError: (String) -> Unit,
    private val onCancelRequested: () -> Unit
) {

    lateinit var previewView: PreviewView
        private set

    var imageCapture: ImageCapture? = null
        private set

    fun start() {

        val rootLayout =
            LinearLayout(activity).apply {

                orientation =
                    LinearLayout.VERTICAL

                setBackgroundColor(
                    Color.BLACK
                )
            }

        val cameraArea =
            FrameLayout(activity).apply {

                setBackgroundColor(
                    Color.BLACK
                )
            }

        previewView =
            PreviewView(activity).apply {

                scaleType =
                    PreviewView.ScaleType.FILL_CENTER

                implementationMode =
                    PreviewView.ImplementationMode.COMPATIBLE
            }

        val frameWidthDp: Int
        val frameHeightDp: Int

        if (
            captureSettings.orientation ==
            OrientationMode.LANDSCAPE
        ) {

            frameWidthDp = 320
            frameHeightDp = 210

        } else {

            frameWidthDp = 240
            frameHeightDp = 320
        }

        val density =
            activity.resources.displayMetrics.density

        val frameWidth =
            (frameWidthDp * density).toInt()

        val frameHeight =
            (frameHeightDp * density).toInt()

        val captureFrame =
            FrameLayout(activity).apply {

                setBackgroundColor(
                    Color.BLACK
                )

                clipChildren = true
                clipToPadding = true
            }

        captureFrame.addView(
            previewView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val border =
            View(activity).apply {

                background =
                    android.graphics.drawable
                        .GradientDrawable().apply {

                        setColor(
                            Color.TRANSPARENT
                        )

                        setStroke(
                            4,
                            Color.WHITE
                        )
                    }

                isClickable = false
            }

        captureFrame.addView(
            border,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val frameParams =
            LinearLayout.LayoutParams(
                frameWidth,
                frameHeight
            ).apply {

                gravity =
                    Gravity.CENTER

                setMargins(
                    0,
                    20,
                    0,
                    20
                )
            }

        cameraArea.addView(
            captureFrame,
            frameParams
        )

        val status =
            TextView(activity).apply {

                text =
                    "Position the image inside the frame"

                textSize = 16f

                setTextColor(
                    Color.WHITE
                )

                gravity =
                    Gravity.CENTER

                setPadding(
                    20,
                    10,
                    20,
                    10
                )
            }

        cameraArea.addView(
            status,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {

                gravity =
                    Gravity.BOTTOM

                setMargins(
                    0,
                    0,
                    0,
                    10
                )
            }
        )

        rootLayout.addView(
            cameraArea,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val captureButton =
            activity.createCameraButton("Capture") {

                captureImage()
            }

        rootLayout.addView(
            captureButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {

                setMargins(
                    50,
                    10,
                    50,
                    5
                )
            }
        )

        val cancelButton =
            activity.createCameraButton("Cancel") {

                onCancelRequested()
            }

        rootLayout.addView(
            cancelButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {

                setMargins(
                    50,
                    5,
                    50,
                    15
                )
            }
        )

        activity.setContentView(rootLayout)

        startCamera(status)
    }

    private fun startCamera(
        status: TextView
    ) {

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(
                activity
            )

        cameraProviderFuture.addListener({

            val cameraProvider =
                cameraProviderFuture.get()

            val preview =
                Preview.Builder()
                    .setTargetRotation(
                        previewView.display.rotation
                    )
                    .build()
                    .also {

                        it.surfaceProvider =
                            previewView.surfaceProvider
                    }

            imageCapture =
                ImageCapture.Builder()
                    .setCaptureMode(
                        ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                    )
                    .setTargetRotation(
                        previewView.display.rotation
                    )
                    .build()

            try {

                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    activity,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )

            } catch (e: Exception) {

                status.text =
                    "Unable to start camera: ${e.message}"
            }

        }, ContextCompat.getMainExecutor(activity))
    }

    private fun captureImage() { 
        val capture =
            imageCapture
                ?: return

        val file =
            File(
                activity.cacheDir,
                "capture_${System.currentTimeMillis()}.jpg"
            )

        val outputOptions =
            ImageCapture.OutputFileOptions
                .Builder(file)
                .build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(activity),
            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(
                    outputFileResults:
                        ImageCapture.OutputFileResults
                ) {

                    onImageCaptured(file)
                }

                override fun onError(
                    exception: ImageCaptureException
                ) {

                    onCaptureError(
                        exception.message
                            ?: "Unable to capture image"
                    )
                }
            }
        )
    }
}