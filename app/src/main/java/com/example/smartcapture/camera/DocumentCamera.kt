package com.example.smartcapture.camera

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
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
import com.example.smartcapture.capture.PhotoSide
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
    private var captureInProgress = false

    fun start() {

        isActive = true

        val rootLayout =
            LinearLayout(activity).apply {

                orientation =
                    LinearLayout.VERTICAL

                setBackgroundColor(
                    Color.BLACK
                )
                setPadding(
                    0,
                    (16 * activity.resources.displayMetrics.density).toInt(),
                    0,
                    0
                )
            }

        val density = activity.resources.displayMetrics.density
        val margin = (16 * density).toInt()
        val gap = (8 * density).toInt()
        val controlHeight = (56 * density).toInt()

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
                background = GradientDrawable().apply {
                    cornerRadius = 12 * density
                    setColor(Color.BLACK)
                }
                clipToOutline = true
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
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
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
            ).apply {
                marginStart = margin
                marginEnd = margin
            }
        )

        if (captureSettings.captureType == CaptureType.PHOTO) {
            if (captureSettings.photoSide == null) {
                captureSettings.photoSide = PhotoSide.FRONT
            }
            val sideLayout = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            val frontButton = activity.createCameraSelectionButton(
                android.R.drawable.ic_menu_agenda,
                "Front",
                captureSettings.photoSide == PhotoSide.FRONT
            ) {
                captureSettings.photoSide = PhotoSide.FRONT
                stopCamera()
                start()
            }
            val backButton = activity.createCameraSelectionButton(
                android.R.drawable.ic_menu_agenda,
                "Back",
                captureSettings.photoSide == PhotoSide.BACK
            ) {
                captureSettings.photoSide = PhotoSide.BACK
                stopCamera()
                start()
            }
            sideLayout.addView(
                frontButton,
                LinearLayout.LayoutParams(0, controlHeight, 1f).apply {
                    marginStart = margin
                    marginEnd = gap / 2
                }
            )
            sideLayout.addView(
                backButton,
                LinearLayout.LayoutParams(0, controlHeight, 1f).apply {
                    marginStart = gap / 2
                    marginEnd = margin
                }
            )
            rootLayout.addView(sideLayout, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                controlHeight
            ).apply {
                topMargin = gap
            })
        }

        val captureButton =
            activity.createCameraButton("Capture") {

                captureImage()
            }
        captureButton.setTextColor(Color.WHITE)
        captureButton.compoundDrawables.forEach { drawable -> drawable?.setTint(Color.WHITE) }

        rootLayout.addView(
            captureButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                controlHeight
            ).apply {
                marginStart = margin
                topMargin = gap
                marginEnd = margin
            }
        )

        val galleryButton =
            activity.createOutlinedButton("Add from Gallery") {
                stopCamera()
                activity.openGalleryPicker(fromCamera = true)
            }

        rootLayout.addView(
            galleryButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                controlHeight
            ).apply {
                marginStart = margin
                topMargin = gap
                marginEnd = margin
            }
        )

        val cancelButton =
            activity.createOutlinedButton("Cancel") {

                stopCamera()
                onCancelRequested()
            }

        rootLayout.addView(
            cancelButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                controlHeight
            ).apply {
                marginStart = margin
                topMargin = gap
                marginEnd = margin
                bottomMargin = margin
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
        val width: Float
        val height: Float

        if (captureSettings.captureType == CaptureType.PHOTO) {
            width = 85.60f
            height = 53.98f
        } else {
            width = captureSettings.documentSize?.widthMm?.toFloat() ?: 210f
            height = captureSettings.documentSize?.heightMm?.toFloat() ?: 297f
        }

        val customWidth = captureSettings.customDocumentWidthMm
        val customHeight = captureSettings.customDocumentHeightMm
        val selectedWidth = if (captureSettings.documentSize == com.example.smartcapture.capture.DocumentSize.CUSTOM) {
            customWidth ?: width
        } else {
            width
        }
        val selectedHeight = if (captureSettings.documentSize == com.example.smartcapture.capture.DocumentSize.CUSTOM) {
            customHeight ?: height
        } else {
            height
        }

        val ratio = if (selectedWidth > 0f && selectedHeight > 0f) {
            selectedWidth / selectedHeight
        } else {
            210f / 297f
        }
        return if (captureSettings.orientation == OrientationMode.LANDSCAPE) {
            ratio
        } else {
            1f / ratio
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
        if (captureInProgress) return

        val capture =
            imageCapture ?: run {
                onCaptureError("Camera is not ready. Please try again.")
                return
            }

        captureInProgress = true

        val file =
            File(
                activity.cacheDir,
                "capture_${System.currentTimeMillis()}.jpg"
            )

        val outputOptions =
            ImageCapture.OutputFileOptions
                .Builder(file)
                .build()

        try {
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
                                val converted = try {
                                    ImageProcessor.convertToBlackAndWhite(file, processedFile)
                                } catch (_: Exception) {
                                    false
                                }
                                withContext(Dispatchers.Main) {
                                    captureInProgress = false
                                    if (converted) {
                                        file.delete()
                                        deliverCapturedImage(processedFile)
                                    } else {
                                        processedFile.delete()
                                        file.delete()
                                        onCaptureError("Unable to process captured image")
                                    }
                                }
                            }
                        } else {
                            captureInProgress = false
                            deliverCapturedImage(file)
                        }
                    }

                    override fun onError(
                        exception: ImageCaptureException
                    ) {

                        captureInProgress = false
                        file.delete()
                        onCaptureError(
                            exception.message
                                ?: "Unable to capture image"
                        )
                    }
                }
            )
        } catch (exception: Exception) {
            captureInProgress = false
            file.delete()
            onCaptureError(exception.message ?: "Unable to capture image")
        }
    }

    private fun deliverCapturedImage(file: File) {
        try {
            onImageCaptured(file)
        } catch (exception: Exception) {
            file.delete()
            onCaptureError(exception.message ?: "Unable to open captured image")
        }
    }

    fun stopCamera() {
        isActive = false
        cameraProvider?.unbindAll()
        cameraProvider = null
        imageCapture = null
    }
}