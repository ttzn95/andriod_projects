package com.example.smartcapture

import okhttp3.MultipartBody
import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.Retrofit
import com.example.smartcapture.camera.CapturePreview
import com.example.smartcapture.camera.DocumentCamera
import com.example.smartcapture.capture.CaptureSettings
import com.example.smartcapture.capture.CaptureType
import com.example.smartcapture.capture.ColorMode
import com.example.smartcapture.capture.DocumentSize
import com.example.smartcapture.capture.OrientationMode
import com.example.smartcapture.capture.PhotoType
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Typeface
import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.text.InputType
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.drawable.GradientDrawable
import android.app.AlertDialog
import com.google.gson.Gson
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis 
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.smartcapture.api.ApiClient
import com.example.smartcapture.api.LoginRequest
import com.example.smartcapture.api.ApiError
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private val ink get() = getColor(R.color.sc_ink)
    private val panel get() = getColor(R.color.sc_panel)
    private val paper get() = getColor(R.color.sc_paper)
    private val muted get() = getColor(R.color.sc_muted)
    private val signal get() = getColor(R.color.sc_signal)
    private val outline get() = getColor(R.color.sc_outline)
    private val errorColor get() = getColor(R.color.sc_error)

    private var uploadInProgress = false

    private lateinit var previewView: PreviewView
    private lateinit var statusText: TextView
    private lateinit var capturePreview: CapturePreview

    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private var sessionToken: String? = null
    private var captureToken: String? = null 
    private var captureSettings = CaptureSettings()
    private var capturedImageFile: File? = null

    // ---------------------------------------------------------
    // CAMERA PERMISSION
    // ---------------------------------------------------------

    private val cameraPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                startQrScanner()
            } else {
                statusText.text =
                    "Camera permission is required."
            }
        }

    // ---------------------------------------------------------
    // ACTIVITY
    // ---------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        capturePreview = CapturePreview(this)
        showMainScreen()
    }

    // ---------------------------------------------------------
    // MAIN SCREEN
    // ---------------------------------------------------------

    private fun showMainScreen() {

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(28), dp(64), dp(28), dp(28))
            setBackgroundColor(ink)
            gravity = android.view.Gravity.CENTER
        }

        val title = TextView(this).apply {
            text = "Smart Capture"
            textSize = 34f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(paper)
            gravity = android.view.Gravity.CENTER
        }

        statusText = TextView(this).apply {
            text = "FIELD OPERATIONS  /  01\nREADY TO CAPTURE\nSecure document intake for your team"
            textSize = 16f
            setTextColor(muted)
            gravity = android.view.Gravity.CENTER
            setPadding(0, dp(16), 0, dp(40))
        }

        val scanButton = createButton("Scan QR Code") {
            requestCameraPermission()
        }

        layout.addView(title)
        layout.addView(statusText)
        layout.addView(scanButton)

        setContentView(layout)
    }

    // ---------------------------------------------------------
    // CAMERA PERMISSION
    // ---------------------------------------------------------

    private fun requestCameraPermission() {

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            startQrScanner()

        } else {

            cameraPermissionLauncher.launch(
                Manifest.permission.CAMERA
            )
        }
    }

    // ---------------------------------------------------------
    // QR SCANNER
    // ---------------------------------------------------------

    private fun startQrScanner() {

        val rootLayout = FrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
        }

        previewView = PreviewView(this).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }

        rootLayout.addView(
            previewView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        // ---------------------------------------------------------
        // SQUARE QR FRAME
        // ---------------------------------------------------------

        val qrFrameSize = dp(280)

        val scanOverlay = ScanOverlay(this, qrFrameSize)
        rootLayout.addView(
            scanOverlay,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val qrFrame = View(this).apply {

            background = GradientDrawable().apply {

                setColor(Color.TRANSPARENT)

                setStroke(
                    dp(3),
                    signal
                )
            }
        }

        val qrFrameParams =
            FrameLayout.LayoutParams(
                qrFrameSize,
                qrFrameSize
            ).apply {

                gravity =
                    android.view.Gravity.CENTER
            }

        rootLayout.addView(
            qrFrame,
            qrFrameParams
        )

        // ---------------------------------------------------------
        // INSTRUCTION
        // ---------------------------------------------------------

        val scannerStatus =
            TextView(this).apply {

                text = "ALIGN QR CODE WITHIN THE FRAME"

                textSize = 18f

                setTextColor(Color.WHITE)

                setGravity(
                    android.view.Gravity.CENTER
                )

                setPadding(
                    20,
                    20,
                    20,
                    20
                )

                setBackgroundColor(Color.argb(190, 15, 22, 28))
            }

        val statusParams =
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {

                gravity =
                    android.view.Gravity.BOTTOM

                setMargins(
                    20,
                    0,
                    20,
                    40
                )
            }

        rootLayout.addView(
            scannerStatus,
            statusParams
        )

        // ---------------------------------------------------------
        // CANCEL BUTTON
        // ---------------------------------------------------------

        val cancelButton = createCameraButton("Close scanner") {

            val cameraProviderFuture =
                ProcessCameraProvider.getInstance(
                    this@MainActivity
                )

            cameraProviderFuture.addListener({

                try {

                    cameraProviderFuture.get()
                        .unbindAll()

                } catch (_: Exception) {
                }

            }, ContextCompat.getMainExecutor(this@MainActivity))

            showMainScreen()
        }

        val cancelParams =
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {

                gravity =
                    android.view.Gravity.TOP or
                    android.view.Gravity.START

                setMargins(
                    20,
                    40,
                    0,
                    0
                )
            }

        rootLayout.addView(
            cancelButton,
            cancelParams
        )

        setContentView(rootLayout)

        // ---------------------------------------------------------
        // CAMERA
        // ---------------------------------------------------------

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            val cameraProvider =
                cameraProviderFuture.get()

            val preview =
                Preview.Builder()
                    .build()
                    .also {

                        it.surfaceProvider =
                            previewView.surfaceProvider
                    }

            // -----------------------------------------------------
            // QR SCANNER
            // -----------------------------------------------------

            val options =
                BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(
                        Barcode.FORMAT_QR_CODE
                    )
                    .build()

            val scanner =
                BarcodeScanning.getClient(options)

            val imageAnalysis =
                ImageAnalysis.Builder()
                    .setBackpressureStrategy(
                        ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                    )
                    .build()

                    var qrDetectionHandled = false

            imageAnalysis.setAnalyzer(
                cameraExecutor
            ) { imageProxy ->

                val mediaImage =
                    imageProxy.image

                if (mediaImage != null) {

                    val image =
                        InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )

                    scanner.process(image)
                        .addOnSuccessListener { barcodes ->

                            for (barcode in barcodes) {

                                val value =
                                    barcode.rawValue

                                val bounds = barcode.boundingBox
                                val rotatedWidth =
                                    if (imageProxy.imageInfo.rotationDegrees % 180 == 0) {
                                        imageProxy.width
                                    } else {
                                        imageProxy.height
                                    }
                                val rotatedHeight =
                                    if (imageProxy.imageInfo.rotationDegrees % 180 == 0) {
                                        imageProxy.height
                                    } else {
                                        imageProxy.width
                                    }
                                val frameLeft =
                                    (rotatedWidth - qrFrameSize * rotatedWidth / previewView.width) / 2f
                                val frameTop =
                                    (rotatedHeight - qrFrameSize * rotatedHeight / previewView.height) / 2f
                                val frameRight = rotatedWidth - frameLeft
                                val frameBottom = rotatedHeight - frameTop

                                if (!qrDetectionHandled &&
                                    !value.isNullOrBlank() &&
                                    bounds != null &&
                                    bounds.left >= frameLeft &&
                                    bounds.top >= frameTop &&
                                    bounds.right <= frameRight &&
                                    bounds.bottom <= frameBottom
                                ) {

                                    qrDetectionHandled = true

                                    imageAnalysis.clearAnalyzer()

                                    runOnUiThread {

                                        scannerStatus.text =
                                            "QR detected. Validating session..."

                                        handleQrCode(value)
                                    }

                                    break
                                }
                            }
                        }
                        .addOnCompleteListener {

                            imageProxy.close()
                        }

                } else {

                    imageProxy.close()
                }
            }

            try {

                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )

            } catch (e: Exception) {

                scannerStatus.text =
                    "Unable to start camera: ${e.message}"
            }

        }, ContextCompat.getMainExecutor(this))
    }

    // ---------------------------------------------------------
    // HANDLE QR
    // ---------------------------------------------------------

    private fun handleQrCode(qrValue: String) {

        sessionToken = qrValue

        showLoginScreen()
    }

    // ---------------------------------------------------------
    // STAFF LOGIN
    // ---------------------------------------------------------

    private fun showLoginScreen() {

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(28), dp(56), dp(28), dp(28))
            setBackgroundColor(ink)
            gravity = android.view.Gravity.CENTER
        }

        val title = TextView(this).apply {
            text = "Staff Login"
            textSize = 32f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(paper)
            gravity = android.view.Gravity.CENTER
        }

        val information = TextView(this).apply {
            text = "AUTHENTICATE TO CONTINUE\nUse the credentials assigned to your capture team."
            textSize = 17f
            setTextColor(muted)
            gravity = android.view.Gravity.CENTER
            setPadding(0, dp(16), 0, dp(30))
        }

        val staffIdInput =
            EditText(this).apply {

                hint = "Staff ID"

                inputType =
                    InputType.TYPE_CLASS_TEXT
            }

        val passwordInput =
            EditText(this).apply {

                hint = "Password"

                inputType =
                    InputType.TYPE_CLASS_TEXT or
                            InputType.TYPE_TEXT_VARIATION_PASSWORD

                setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    android.R.drawable.ic_menu_view,
                    0
                )

                setOnTouchListener { _, event ->

                    if (
                        event.action == MotionEvent.ACTION_UP &&
                        event.x >=
                        width - compoundDrawablePadding - 100
                    ) {

                        val visible =
                            inputType ==
                                    (
                                            InputType.TYPE_CLASS_TEXT or
                                                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                                            )

                        if (visible) {

                            inputType =
                                InputType.TYPE_CLASS_TEXT or
                                        InputType.TYPE_TEXT_VARIATION_PASSWORD

                        } else {

                            inputType =
                                InputType.TYPE_CLASS_TEXT or
                                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        }

                        setSelection(text.length)

                        true

                    } else {

                        false
                    }
                }
            }

            styleInput(staffIdInput)
            styleInput(passwordInput)

        val loginStatus =
            TextView(this).apply {

                textSize = 16f
                setTextColor(muted)
                setPadding(0, dp(20), 0, dp(20))
            }

        val loginButton =
            Button(this).apply {

                text = "Login"
                minHeight = dp(48)
                minimumHeight = dp(48)
                setCompoundDrawablesWithIntrinsicBounds(
                    android.R.drawable.ic_menu_send,
                    0,
                    0,
                    0
                )
                setCompoundDrawablePadding(dp(10))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(8)
                }

                setOnClickListener {

                    val staffId =
                        staffIdInput.text.toString().trim()

                    val password =
                        passwordInput.text.toString()

                    if (staffId.isEmpty()) {

                        showLoginError(loginStatus, "Please enter Staff ID.")

                        return@setOnClickListener
                    }

                    if (password.isEmpty()) {

                        showLoginError(loginStatus, "Please enter password.")

                        return@setOnClickListener
                    }

                    isEnabled = false

                    loginStatus.text =
                        "Authenticating..."
                    loginStatus.setTextColor(muted)

                    login(
                        staffId,
                        password,
                        loginStatus,
                        this
                    )
                }
            }

        layout.addView(title)
        layout.addView(information)
        layout.addView(staffIdInput)
        layout.addView(passwordInput)
        layout.addView(loginStatus)
        layout.addView(loginButton)

        setContentView(layout)
    }

    // ---------------------------------------------------------
    // BACKEND LOGIN
    // ---------------------------------------------------------

    private fun login(
        staffId: String,
        password: String,
        loginStatus: TextView,
        loginButton: Button
    ) {

        val token = sessionToken

        if (token.isNullOrBlank()) {

            showLoginError(loginStatus, "Session is missing. Please scan the QR again.")

            loginButton.isEnabled = true

            return
        }

        CoroutineScope(Dispatchers.IO).launch {

            try {

                val response =
                    ApiClient.api.login(
                        LoginRequest(
                            session_token = token,
                            staff_id = staffId,
                            password = password
                        )
                    )

                withContext(Dispatchers.Main) {

                    loginButton.isEnabled = true

                    if (response.isSuccessful) {

                        val body =
                            response.body()

                        if (
                            body != null &&
                            body.success
                        ) {

                            captureToken =
                                body.capture_token

                            showReadyScreen(
                                body.staff_name
                            )

                        } else {

                            showLoginError(loginStatus, "Authentication failed. Please try again.")
                        }

                    } else {

                        when (response.code()) {

                            401 ->
                                showLoginError(
                                    loginStatus,
                                    getApiErrorMessage(response.errorBody()?.string())
                                )

                            403 ->
                                showLoginError(loginStatus, "Staff is not authorized.")

                            else ->
                                showLoginError(loginStatus, "Server error: ${response.code()}")
                        }
                    }
                }

            } catch (e: Exception) {

                withContext(Dispatchers.Main) {

                    loginButton.isEnabled = true

                    showLoginError(loginStatus, "Unable to connect to server.")
                }
            }
        }
    }

    // ---------------------------------------------------------
    // READY SCREEN
    // ---------------------------------------------------------

    private fun showReadyScreen(
        staffName: String
    ) {

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(28), dp(56), dp(28), dp(28))
            setBackgroundColor(ink)
            gravity = android.view.Gravity.CENTER
        }

        val title = TextView(this).apply {
            text = "Smart Capture"
            textSize = 32f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(paper)
            gravity = android.view.Gravity.CENTER
        }

        val status = TextView(this).apply {
            text =
                "Authentication successful\n\nWelcome, $staffName"
            textSize = 18f
            setTextColor(muted)
            gravity = android.view.Gravity.CENTER
            setPadding(0, dp(32), 0, dp(40))
        }

        val captureButton = createButton("Start Capture") {
            showCaptureTypeScreen()
        }

        val logoutButton = createButton("Logout") {
            logout()
        }

        layout.addView(title)
        layout.addView(status)
        layout.addView(captureButton)
        layout.addView(logoutButton)

        setContentView(layout)
    }   

    fun showCaptureError(message: String) {
        capturePreview.showCaptureError(message)
    }

    // ---------------------------------------------------------
    // UPLOAD CapturedImage
    // ---------------------------------------------------------
    fun uploadCapturedImage() {

        if (uploadInProgress) {
            return
        }

        val file = capturedImageFile

        if (file == null) {
            showCaptureError("No captured image available.")
            return
        }

        val token = captureToken

        if (token.isNullOrBlank()) {
            showCaptureError("Capture session is not authenticated.")
            return
        }

        uploadInProgress = true

        CoroutineScope(Dispatchers.IO).launch {

            try {

                val tokenBody =
                    RequestBody.create(
                        MediaType.parse("text/plain"),
                        token
                    )

                val mimeType =
                    when (file.extension.lowercase()) {
                        "jpg", "jpeg" -> "image/jpeg"
                        "png" -> "image/png"
                        "svg" -> "image/svg+xml"
                        else -> "application/octet-stream"
                    }

                val imageBody =
                    RequestBody.create(
                        MediaType.parse(mimeType),
                        file
                    )

                val imagePart =
                    MultipartBody.Part.createFormData(
                        "image",
                        file.name,
                        imageBody
                    )

                val response =
                    ApiClient.api.uploadCapture(
                        tokenBody,
                        imagePart
                    )

                withContext(Dispatchers.Main) {

                    if (response.isSuccessful) {

                        val result = response.body()

                        if (result?.success == true) {

                            showUploadCompleteDialog()

                        } else {

                            uploadInProgress = false
                            showCaptureError(
                                result?.message
                                    ?: "Upload failed"
                            )
                        }

                    } else {

                        uploadInProgress = false
                        showCaptureError(
                            getApiErrorMessage(
                                response.errorBody()?.string()
                            )
                        )
                    }
                }

            } catch (e: Exception) {

                withContext(Dispatchers.Main) {

                    uploadInProgress = false
                    showCaptureError(
                        e.message
                            ?: "Unable to upload image"
                    )
                }
            }
        }
    }

    private fun showLoginError(status: TextView, message: String) {
        status.setTextColor(errorColor)
        status.text = message
    }

    private fun getApiErrorMessage(errorBody: String?): String {
        val detail = try {
            errorBody?.let { Gson().fromJson(it, ApiError::class.java).detail }
        } catch (_: Exception) {
            null
        }

        return detail ?: "Request failed. Please try again."
    }

    private fun showUploadCompleteDialog() {
        AlertDialog.Builder(this)
            .setTitle("Upload complete")
            .setMessage("Your captured image was uploaded successfully.")
            .setPositiveButton("Continue") { _, _ ->
                showCaptureCompleteScreen()
            }
            .setCancelable(false)
            .show()
    }

    // ---------------------------------------------------------
    // CAPTURE TYPE
    // ---------------------------------------------------------
    private fun showCaptureTypeScreen() {

        val layout = createVerticalLayout()

        val title =
            createTitle("Select Capture Type")

        layout.addView(title)

        val photoButton =
            createButton("Photos") {

                captureSettings.captureType =    CaptureType.PHOTO

                showPhotoTypeScreen()
            }

        val documentButton =
            createButton("Documents") {

                captureSettings.captureType =    CaptureType.DOCUMENT

                showDocumentSizeScreen()
            }

        val backButton =
            createButton("Back") {

                showReadyScreen("Staff")
            }

        layout.addView(photoButton)
        layout.addView(documentButton)
        layout.addView(backButton)

        setContentView(layout)
    }



    // ---------------------------------------------------------
    // PHOTO TYPE
    // ---------------------------------------------------------

    private fun showPhotoTypeScreen() {

        val layout = createVerticalLayout()

        layout.addView(
            createTitle("Photo Type")
        )

        val types = listOf(
            "License" to PhotoType.LICENSE,
            "NRC" to PhotoType.NRC,
            "Employment Card" to PhotoType.EMPLOYMENT_CARD,
            "Cheque" to PhotoType.CHEQUE,
            "Custom Photo" to PhotoType.CUSTOM
        )

        for ((displayName, photoType) in types) {

            layout.addView(
                createButton(displayName) {

                    captureSettings.photoType = photoType

                    showColorModeScreen()
                }
            )
        }

        layout.addView(
            createButton("Back") {

                showCaptureTypeScreen()
            }
        )

        setContentView(layout)
    }

    // ---------------------------------------------------------
    // DOCUMENT SIZE
    // ---------------------------------------------------------
    private fun showDocumentSizeScreen() {

        val layout = createVerticalLayout()

        layout.addView(
            createTitle("Document Size")
        )

        val sizes = listOf(
            "A4" to DocumentSize.A4,
            "Legal" to DocumentSize.LEGAL,
            "A5" to DocumentSize.A5,
            "B5" to DocumentSize.B5,
            "Custom Size" to DocumentSize.CUSTOM
        )

        for ((displayName, documentSize) in sizes) {

            layout.addView(
                createButton(displayName) {

                    captureSettings.documentSize = documentSize

                    if (documentSize == DocumentSize.CUSTOM) {

                        showCustomDocumentSizeScreen()

                    } else {

                        showColorModeScreen()
                    }
                }
            )
        }

        layout.addView(
            createButton("Back") {

                showCaptureTypeScreen()
            }
        )

        setContentView(layout)
    }


    // ---------------------------------------------------------
    // CUSTOM DOCUMENT SIZE
    // ---------------------------------------------------------
    private fun showCustomDocumentSizeScreen() {

        val layout =
            createVerticalLayout()

        layout.addView(
            createTitle("Custom Document Size")
        )

        val widthInput =
            EditText(this).apply {

                hint = "Width (mm)"

                inputType =
                    InputType.TYPE_CLASS_NUMBER or
                            InputType.TYPE_NUMBER_FLAG_DECIMAL
            }

        val heightInput =
            EditText(this).apply {

                hint = "Height (mm)"

                inputType =
                    InputType.TYPE_CLASS_NUMBER or
                            InputType.TYPE_NUMBER_FLAG_DECIMAL
            }

                    styleInput(widthInput)
                    styleInput(heightInput)

        val continueButton =
        createButton("Continue") {

            if (
                widthInput.text.isNullOrBlank() ||
                heightInput.text.isNullOrBlank()
            ) {

                return@createButton
            }

            captureSettings.documentSize =
                DocumentSize.CUSTOM

            captureSettings.customDocumentWidthMm =
                widthInput.text.toString().toFloat()

            captureSettings.customDocumentHeightMm =
                heightInput.text.toString().toFloat()

            showColorModeScreen()
        }

        layout.addView(widthInput)
        layout.addView(heightInput)
        layout.addView(continueButton)

        layout.addView(
            createButton("Back") {

                showDocumentSizeScreen()
            }
        )

        setContentView(layout)
    }

    // ---------------------------------------------------------
    // COLOR MODE
    // ---------------------------------------------------------
    fun showColorModeScreen() {

        val layout = createVerticalLayout()

        layout.addView(
            createTitle("Capture Settings")
        )

        // ---------------------------------------------------------
        // COLOR MODE
        // ---------------------------------------------------------

        layout.addView(
            createTitle("Color Mode")
        )

        val colorLayout =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 20, 0, 20)
            }

        val colorButton =
            createSelectionButton(
                iconRes = android.R.drawable.ic_menu_gallery,
                title = "Color",
                selected =
                    captureSettings.colorMode ==
                            ColorMode.COLOR
            ) {

                captureSettings.colorMode =
                    ColorMode.COLOR

                showColorModeScreen()
            }

        val blackWhiteButton =
            createSelectionButton(
                iconRes = android.R.drawable.ic_menu_view,
                title = "Black & White",
                selected =
                    captureSettings.colorMode ==
                            ColorMode.BLACK_WHITE
            ) {

                captureSettings.colorMode =
                    ColorMode.BLACK_WHITE

                showColorModeScreen()
            }

        colorLayout.addView(
            colorButton,
            LinearLayout.LayoutParams(
                0,
                dp(132),
                1f
            ).apply {
                setMargins(0, 0, dp(6), dp(12))
            }
        )

        colorLayout.addView(
            blackWhiteButton,
            LinearLayout.LayoutParams(
                0,
                dp(132),
                1f
            ).apply {
                setMargins(dp(6), 0, 0, dp(12))
            }
        )

        layout.addView(colorLayout)

        // ---------------------------------------------------------
        // ORIENTATION
        // ---------------------------------------------------------

        layout.addView(
            createTitle("Orientation")
        )

        val orientationLayout =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 20, 0, 20)
            }

        val portraitButton =
            createSelectionButton(
                iconRes = android.R.drawable.ic_menu_crop,
                title = "Portrait",
                selected =
                    captureSettings.orientation ==
                            OrientationMode.PORTRAIT
            ) {

                captureSettings.orientation =
                    OrientationMode.PORTRAIT

                showColorModeScreen()
            }

        val landscapeButton =
            createSelectionButton(
                iconRes = android.R.drawable.ic_menu_rotate,
                title = "Landscape",
                selected =
                    captureSettings.orientation ==
                            OrientationMode.LANDSCAPE
            ) {

                captureSettings.orientation =
                    OrientationMode.LANDSCAPE

                showColorModeScreen()
            }

        orientationLayout.addView(
            portraitButton,
            LinearLayout.LayoutParams(
                0,
                dp(132),
                1f
            ).apply {
                setMargins(0, 0, dp(6), dp(12))
            }
        )

        orientationLayout.addView(
            landscapeButton,
            LinearLayout.LayoutParams(
                0,
                dp(132),
                1f
            ).apply {
                setMargins(dp(6), 0, 0, dp(12))
            }
        )

        layout.addView(orientationLayout)

        // ---------------------------------------------------------
        // CURRENT SELECTION
        // ---------------------------------------------------------

        val selectedText =
            TextView(this).apply {

                text =
                    "Color: ${getColorModeDisplayName()}\n" +
                    "Orientation: ${getOrientationDisplayName()}"

                textSize = 16f

                setPadding(
                    0,
                    20,
                    0,
                    20
                )
            }

        layout.addView(selectedText)

        // ---------------------------------------------------------
        // CONTINUE
        // ---------------------------------------------------------

        layout.addView(
            createButton("Continue →") {

                startDocumentCamera()
            }
        )

        // ---------------------------------------------------------
        // BACK
        // ---------------------------------------------------------

        layout.addView(
            createButton("Back") {

                if (
                    captureSettings.captureType ==
                    CaptureType.PHOTO
                ) {

                    showPhotoTypeScreen()

                } else {

                    showDocumentSizeScreen()
                }
            }
        )

        setContentView(layout)
    }

    private fun createSelectionButton(
        iconRes: Int,
        title: String,
        selected: Boolean,
        onClick: () -> Unit
        ): Button {

        val button =
            Button(this).apply {

                text = title

                textSize = 16f

                isAllCaps = false
                gravity = android.view.Gravity.CENTER
                setCompoundDrawablePadding(dp(8))
                val icon = getDrawable(iconRes)?.mutate()
                icon?.setTint(if (selected) ink else paper)
                setCompoundDrawablesWithIntrinsicBounds(null, icon, null, null)

                setOnClickListener {
                    onClick()
                }
            }

        if (selected) {

            button.alpha = 1.0f

            button.setTextColor(ink)
            button.background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(signal)
            }

        } else {

            button.alpha = 0.65f
            button.setTextColor(paper)
            button.background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(panel)
                setStroke(dp(1), outline)
            }
        }

        button.compoundDrawables.forEach { drawable ->
            drawable?.setTint(if (selected) ink else paper)
        }

        button.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN ->
                    view.animate().scaleX(0.97f).scaleY(0.97f).alpha(0.88f)
                        .setDuration(190).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    view.animate().scaleX(1f).scaleY(1f).alpha(1f)
                        .setDuration(280).start()
            }
            false
        }

        return button
    }
 
    private fun getColorModeDisplayName(): String {

        return when (captureSettings.colorMode) {

        ColorMode.COLOR ->
            "Color"

        ColorMode.BLACK_WHITE ->
            "Black & White"
        }
    } 

    private fun getOrientationDisplayName(): String {

        return when (captureSettings.orientation) {

            OrientationMode.PORTRAIT ->
                "Portrait"

            OrientationMode.LANDSCAPE ->
                "Landscape"

            else ->
                "Not selected"
        }
    } 

    // ---------------------------------------------------------
    // DOCUMENT / PHOTO CAMERA
    // --------------------------------------------------------- 
    fun startDocumentCamera() { 
        val documentCamera =
            DocumentCamera(
                activity = this,
                captureSettings = captureSettings,

                onImageCaptured = { file ->
                    capturedImageFile = file
                    capturePreview.showImagePreview(file)
                },

                onCaptureError = { message ->
                    capturePreview.showCaptureError(message)
                },

                onCancelRequested = {

                    showColorModeScreen()
                }
            )

        documentCamera.start()
    } 

    // ---------------------------------------------------------
    // UI HELPERS
    // ---------------------------------------------------------

    fun createVerticalLayout(): LinearLayout {

        return LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            setPadding(
                dp(24),
                dp(44),
                dp(24),
                dp(24)
            )
            setBackgroundColor(ink)
            gravity = android.view.Gravity.CENTER_HORIZONTAL
        }
    }

    // ---------------------------------------------------------
    // TITLE
    // ---------------------------------------------------------

    fun createTitle(
        textValue: String
    ): TextView {

        return TextView(this).apply {

            text = textValue

            textSize = 30f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(paper)
            gravity = android.view.Gravity.CENTER

            setPadding(
                0,
                0,
                0,
                dp(28)
            )
        }
    }

    // ---------------------------------------------------------
    // BUTTON
    // ---------------------------------------------------------

    fun createButton(
        textValue: String,
        onClick: () -> Unit
    ): Button {

        return Button(this).apply {

            text = textValue

            isAllCaps = false

            textSize = 16f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(if (textValue.startsWith("Back") || textValue == "Retake") muted else ink)
            minHeight = dp(48)
            minimumHeight = dp(48)
            setPadding(dp(16), dp(4), dp(16), dp(4))
            stateListAnimator = null
            elevation = dp(2).toFloat()
            gravity = android.view.Gravity.CENTER
            setCompoundDrawablePadding(dp(10))
            setCompoundDrawablesWithIntrinsicBounds(
                iconForButton(textValue),
                0,
                0,
                0
            )
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(14)
            }

            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(if (textValue.startsWith("Back") || textValue == "Retake") panel else signal)
                if (textValue.startsWith("Back") || textValue == "Retake") {
                    setStroke(dp(1), outline)
                }
            }

            setOnClickListener {

                onClick()
            }

            setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        view.animate().scaleX(0.97f).scaleY(0.97f).alpha(0.88f)
                            .setDuration(190).start()
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        view.animate().scaleX(1f).scaleY(1f).alpha(1f)
                            .setDuration(280).start()
                    }
                }
                false
            }
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun iconForButton(textValue: String): Int {
        return when {
            textValue.contains("Scan") -> android.R.drawable.ic_menu_camera
            textValue.contains("Login") -> android.R.drawable.ic_menu_send
            textValue.contains("Capture") -> android.R.drawable.ic_menu_camera
            textValue.contains("Confirm") -> android.R.drawable.ic_menu_upload
            textValue.contains("Continue") -> android.R.drawable.ic_media_next
            textValue.contains("Retake") -> android.R.drawable.ic_menu_rotate
            textValue.contains("Back") -> android.R.drawable.ic_media_previous
            textValue.contains("Close") || textValue.contains("Cancel") ->
                android.R.drawable.ic_menu_close_clear_cancel
            textValue.contains("Logout") -> android.R.drawable.ic_lock_power_off
            textValue.contains("Try Again") -> android.R.drawable.ic_popup_sync
            textValue.contains("Photos") -> android.R.drawable.ic_menu_gallery
            textValue.contains("Documents") -> android.R.drawable.ic_menu_agenda
            else -> android.R.drawable.ic_menu_manage
        }
    }

    private fun styleInput(input: EditText) {
        input.setTextColor(paper)
        input.setHintTextColor(muted)
        input.setPadding(dp(16), dp(14), dp(16), dp(14))
        input.background = GradientDrawable().apply {
            cornerRadius = dp(14).toFloat()
            setColor(panel)
            setStroke(dp(1), outline)
        }
    }

    private class ScanOverlay(
        context: android.content.Context,
        private val scanSize: Int
    ) : View(context) {

        private val maskPaint = android.graphics.Paint().apply {
            color = Color.argb(190, 0, 0, 0)
        }

        private val guidePaint = android.graphics.Paint().apply {
            color = Color.rgb(185, 235, 86)
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 6f
            strokeCap = android.graphics.Paint.Cap.SQUARE
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val left = (width - scanSize) / 2f
            val top = (height - scanSize) / 2f
            val right = left + scanSize
            val bottom = top + scanSize

            canvas.drawRect(0f, 0f, width.toFloat(), top, maskPaint)
            canvas.drawRect(0f, top, left, bottom, maskPaint)
            canvas.drawRect(right, top, width.toFloat(), bottom, maskPaint)
            canvas.drawRect(0f, bottom, width.toFloat(), height.toFloat(), maskPaint)

            val corner = scanSize * 0.14f
            canvas.drawLine(left, top, left + corner, top, guidePaint)
            canvas.drawLine(left, top, left, top + corner, guidePaint)
            canvas.drawLine(right, top, right - corner, top, guidePaint)
            canvas.drawLine(right, top, right, top + corner, guidePaint)
            canvas.drawLine(left, bottom, left + corner, bottom, guidePaint)
            canvas.drawLine(left, bottom, left, bottom - corner, guidePaint)
            canvas.drawLine(right, bottom, right - corner, bottom, guidePaint)
            canvas.drawLine(right, bottom, right, bottom - corner, guidePaint)
        }
    }

    internal fun createCameraButton(
        text: String,
        onClick: () -> Unit
        ): Button {
        return createButton(text, onClick)
    }

    // ---------------------------------------------------------
    // LOGOUT
    // ---------------------------------------------------------

    private fun logout() {

        sessionToken = null

        captureToken = null

        captureSettings =
            CaptureSettings()

        capturedImageFile = null 

        showMainScreen()
    }

     // ---------------------------------------------------------
    // showCaptureCompleteScreen
    // ---------------------------------------------------------
    fun showCaptureCompleteScreen() {

        val layout =
            createVerticalLayout()

        layout.addView(
            createTitle("Capture Complete")
        )

        val information =
            TextView(this).apply {

                text =
                    "The image has been captured successfully.\n\n" +
                    "Type: ${captureSettings.captureType?.name ?: "Unknown"}\n" +
                    "Color: ${getColorModeDisplayName()}\n" +
                    "Orientation: ${getOrientationDisplayName()}"

                textSize = 17f

                setPadding(
                    0,
                    20,
                    0,
                    30
                )
            }

        layout.addView(information)

        // ---------------------------------------------------------
        // CAPTURE ANOTHER
        // ---------------------------------------------------------

        layout.addView(
            createButton("Capture Another") {

                showCaptureTypeScreen()
            }
        )

        // ---------------------------------------------------------
        // BACK TO MAIN
        // ---------------------------------------------------------

        layout.addView(
            createButton("Back to Main") {

                logout()
            }
        )

        setContentView(layout)
    }
}