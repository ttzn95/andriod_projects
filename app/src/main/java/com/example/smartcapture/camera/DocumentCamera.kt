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
import com.example.smartcapture.capture.CaptureType
import com.example.smartcapture.capture.OrientationMode 
import android.widget.Button
import com.example.smartcapture.MainActivity
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    private var cameraProvider: ProcessCameraProvider? = null
    private var isActive = false

    fun start() {

        isActive = true

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
                    PreviewView.ScaleType.FIT_CENTER

                implementationMode =
                    PreviewView.ImplementationMode.COMPATIBLE
            }

        val aspectRatio = getCaptureAspectRatio()

        val captureFrame =
            AspectRatioFrameLayout(activity, aspectRatio).apply {

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
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {

                gravity =
                    Gravity.CENTER

                setMargins(
                    0,
                    0,
                    0,
                    0
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

                stopCamera()
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

            val provider =
                cameraProviderFuture.get()

            if (!isActive) {
                return@addListener
            }

            cameraProvider = provider

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

                provider.unbindAll()

                provider.bindToLifecycle(
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

    private fun getCaptureAspectRatio(): Float {
        val landscape = captureSettings.orientation == OrientationMode.LANDSCAPE

        return if (captureSettings.captureType == CaptureType.PHOTO) {
            if (landscape) 4f / 3f else 3f / 4f
        } else {
            if (landscape) 16f / 9f else 9f / 16f
        }
    }

    private class AspectRatioFrameLayout(
        context: android.content.Context,
        private val aspectRatio: Float
    ) : FrameLayout(context) {

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val maxWidth = MeasureSpec.getSize(widthMeasureSpec)
            val maxHeight = MeasureSpec.getSize(heightMeasureSpec)
            var measuredWidth = maxWidth
            var measuredHeight = (measuredWidth / aspectRatio).toInt()

            if (maxHeight > 0 && measuredHeight > maxHeight) {
                measuredHeight = maxHeight
                measuredWidth = (measuredHeight * aspectRatio).toInt()
            }

            setMeasuredDimension(measuredWidth, measuredHeight)
            val childWidthSpec = MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.EXACTLY)
            val childHeightSpec = MeasureSpec.makeMeasureSpec(measuredHeight, MeasureSpec.EXACTLY)
            for (index in 0 until childCount) {
                getChildAt(index).measure(childWidthSpec, childHeightSpec)
            }
        }
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

                    stopCamera()
                    if (captureSettings.colorMode == com.example.smartcapture.capture.ColorMode.BLACK_WHITE) {
                        val processedFile = File(
                            activity.cacheDir,
                            "capture_bw_${System.currentTimeMillis()}.jpg"
                        )
                        CoroutineScope(Dispatchers.Default).launch {
                            val converted = ImageProcessor.convertToBlackAndWhite(file, processedFile)
                            withContext(Dispatchers.Main) {
                                if (converted) {
                                    file.delete()
                                    onImageCaptured(processedFile)
                                } else {
                                    processedFile.delete()
                                    onImageCaptured(file)
                                }
                            }
                        }
                    } else {
                        onImageCaptured(file)
                    }
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

    fun stopCamera() {
        isActive = false
        cameraProvider?.unbindAll()
        cameraProvider = null
        imageCapture = null
    }
}