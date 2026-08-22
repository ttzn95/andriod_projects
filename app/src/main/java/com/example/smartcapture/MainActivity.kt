package com.example.smartcapture

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
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.smartcapture.api.ApiClient
import com.example.smartcapture.api.LoginRequest
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

    private lateinit var previewView: PreviewView
    private lateinit var statusText: TextView

    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private var sessionToken: String? = null
    private var captureToken: String? = null

    private var imageCapture: ImageCapture? = null

    private var selectedCaptureType: String? = null
    private var selectedPhotoType: String? = null
    private var selectedDocumentSize: String? = null
    private var selectedColorMode: String? = null
    private var selectedOrientation: String? = null

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

        showMainScreen()
    }

    // ---------------------------------------------------------
    // MAIN SCREEN
    // ---------------------------------------------------------

    private fun showMainScreen() {

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 80, 40, 40)
        }

        val title = TextView(this).apply {
            text = "Smart Capture"
            textSize = 28f
        }

        statusText = TextView(this).apply {
            text = "Ready"
            textSize = 18f
            setPadding(0, 40, 0, 40)
        }

        val scanButton = Button(this).apply {
            text = "Scan QR Code"

            setOnClickListener {
                requestCameraPermission()
            }
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

        val qrFrameSize = 700

        val qrFrame = View(this).apply {

            background = android.graphics.drawable.GradientDrawable().apply {

                setColor(
                    android.graphics.Color.TRANSPARENT
                )

                setStroke(
                    5,
                    android.graphics.Color.WHITE
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

                text =
                    "Place the QR code inside the square"

                textSize = 18f

                setTextColor(
                    android.graphics.Color.WHITE
                )

                setGravity(
                    android.view.Gravity.CENTER
                )

                setPadding(
                    20,
                    20,
                    20,
                    20
                )

                setBackgroundColor(
                    android.graphics.Color.argb(
                        150,
                        0,
                        0,
                        0
                    )
                )
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

        val cancelButton =
            Button(this).apply {

                text = "Cancel"

                isAllCaps = false

                setOnClickListener {

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

                                if (!value.isNullOrBlank()) {

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
            setPadding(40, 80, 40, 40)
        }

        val title = TextView(this).apply {
            text = "Staff Login"
            textSize = 28f
        }

        val information = TextView(this).apply {
            text = "Enter your staff credentials"
            textSize = 17f
            setPadding(0, 30, 0, 30)
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

        val loginStatus =
            TextView(this).apply {

                textSize = 16f
                setPadding(0, 30, 0, 30)
            }

        val loginButton =
            Button(this).apply {

                text = "Login"

                setOnClickListener {

                    val staffId =
                        staffIdInput.text.toString().trim()

                    val password =
                        passwordInput.text.toString()

                    if (staffId.isEmpty()) {

                        loginStatus.text =
                            "Please enter Staff ID."

                        return@setOnClickListener
                    }

                    if (password.isEmpty()) {

                        loginStatus.text =
                            "Please enter password."

                        return@setOnClickListener
                    }

                    isEnabled = false

                    loginStatus.text =
                        "Authenticating..."

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

            loginStatus.text =
                "Session is missing. Please scan the QR again."

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

                            loginStatus.text =
                                "Authentication failed."
                        }

                    } else {

                        when (response.code()) {

                            401 ->
                                loginStatus.text =
                                    "Invalid Staff ID or password."

                            403 ->
                                loginStatus.text =
                                    "Staff is not authorized."

                            else ->
                                loginStatus.text =
                                    "Server error: ${response.code()}"
                        }
                    }
                }

            } catch (e: Exception) {

                withContext(Dispatchers.Main) {

                    loginButton.isEnabled = true

                    loginStatus.text =
                        "Unable to connect to server."
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
            setPadding(40, 80, 40, 40)
        }

        val title = TextView(this).apply {
            text = "Smart Capture"
            textSize = 28f
        }

        val status = TextView(this).apply {
            text =
                "Authentication successful\n\nWelcome, $staffName"
            textSize = 18f
            setPadding(0, 40, 0, 40)
        }

        val captureButton =
            Button(this).apply {

                text = "Start Capture"

                setOnClickListener {

                    showCaptureTypeScreen()
                }
            }

        val logoutButton =
            Button(this).apply {

                text = "Logout"

                setOnClickListener {
                    logout()
                }
            }

        layout.addView(title)
        layout.addView(status)
        layout.addView(captureButton)
        layout.addView(logoutButton)

        setContentView(layout)
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

                selectedCaptureType =
                    "PHOTO"

                showPhotoTypeScreen()
            }

        val documentButton =
            createButton("Documents") {

                selectedCaptureType =
                    "DOCUMENT"

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

        val types =
            listOf(
                "License",
                "NRC",
                "Employment Card",
                "Cheque",
                "Custom Photo"
            )

        for (type in types) {

            layout.addView(
                createButton(type) {

                    selectedPhotoType =
                        type

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

        val layout =
            createVerticalLayout()

        layout.addView(
            createTitle("Document Size")
        )

        val sizes =
            listOf(
                "A4",
                "Legal",
                "A5",
                "B5",
                "Custom Size"
            )

        for (size in sizes) {

            layout.addView(
                createButton(size) {

                    selectedDocumentSize =
                        size

                    if (size == "Custom Size") {

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

        val continueButton =
            createButton("Continue") {

                if (
                    widthInput.text.isNullOrBlank() ||
                    heightInput.text.isNullOrBlank()
                ) {

                    return@createButton
                }

                selectedDocumentSize =
                    "${widthInput.text} x ${heightInput.text} mm"

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

    private fun showColorModeScreen() {

        if (selectedOrientation == null) {
            selectedOrientation = "PORTRAIT"
        }

        val layout = createVerticalLayout()

        layout.addView(
            createTitle("Capture Settings")
        )

        // -----------------------------------------------------
        // COLOR MODE
        // -----------------------------------------------------

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
                icon = "🌈",
                title = "Color",
                selected = selectedColorMode == "COLOR"
            ) {

                selectedColorMode = "COLOR"

                showColorModeScreen()
            }

        val blackWhiteButton =
            createSelectionButton(
                icon = "◐",
                title = "Black & White",
                selected = selectedColorMode == "BLACK_WHITE"
            ) {

                selectedColorMode = "BLACK_WHITE"

                showColorModeScreen()
            }

        colorLayout.addView(
            colorButton,
            LinearLayout.LayoutParams(
                0,
                180,
                1f
            ).apply {
                setMargins(0, 0, 10, 0)
            }
        )

        colorLayout.addView(
            blackWhiteButton,
            LinearLayout.LayoutParams(
                0,
                180,
                1f
            ).apply {
                setMargins(10, 0, 0, 0)
            }
        )

        layout.addView(colorLayout)

        // -----------------------------------------------------
        // ORIENTATION
        // -----------------------------------------------------

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
                icon = "▯",
                title = "Portrait",
                selected = selectedOrientation == "PORTRAIT"
            ) {

                selectedOrientation = "PORTRAIT"

                showColorModeScreen()
            }

        val landscapeButton =
            createSelectionButton(
                icon = "▭",
                title = "Landscape",
                selected = selectedOrientation == "LANDSCAPE"
            ) {

                selectedOrientation = "LANDSCAPE"

                showColorModeScreen()
            }

        orientationLayout.addView(
            portraitButton,
            LinearLayout.LayoutParams(
                0,
                180,
                1f
            ).apply {
                setMargins(0, 0, 10, 0)
            }
        )

        orientationLayout.addView(
            landscapeButton,
            LinearLayout.LayoutParams(
                0,
                180,
                1f
            ).apply {
                setMargins(10, 0, 0, 0)
            }
        )

        layout.addView(orientationLayout)

        // -----------------------------------------------------
        // CURRENT SELECTION
        // -----------------------------------------------------

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

        // -----------------------------------------------------
        // CONTINUE
        // -----------------------------------------------------

        layout.addView(
            createButton("Continue →") {

                if (selectedColorMode == null) {

                    selectedText.text =
                        "Please select Color Mode."

                    return@createButton
                }

                startDocumentCamera()
            }
        )

        // -----------------------------------------------------
        // BACK
        // -----------------------------------------------------

        layout.addView(
            createButton("Back") {

                if (selectedCaptureType == "PHOTO") {

                    showPhotoTypeScreen()

                } else {

                    showDocumentSizeScreen()
                }
            }
        )

        setContentView(layout)
    }

    private fun createSelectionButton(
        icon: String,
        title: String,
        selected: Boolean,
        onClick: () -> Unit
    ): Button {

        val button =
            Button(this).apply {

                text = "$icon\n$title"

                textSize = 16f

                isAllCaps = false

                setOnClickListener {
                    onClick()
                }
            }

        if (selected) {

            button.alpha = 1.0f

            button.setBackgroundColor(
                android.graphics.Color.rgb(
                    210,
                    230,
                    255
                )
            )

        } else {

            button.alpha = 0.65f
        }

        return button
    }

    private fun getColorModeDisplayName(): String {

        return when (selectedColorMode) {

            "COLOR" ->
                "Color"

            "BLACK_WHITE" ->
                "Black & White"

            else ->
                "Not selected"
        }
    }

    private fun getOrientationDisplayName(): String {

        return when (selectedOrientation) {

            "PORTRAIT" ->
                "Portrait"

            "LANDSCAPE" ->
                "Landscape"

            else ->
                "Not selected"
        }
    } 

    // ---------------------------------------------------------
    // DOCUMENT / PHOTO CAMERA
    // --------------------------------------------------------- 
    private fun startDocumentCamera() {

        // ---------------------------------------------------------
        // ROOT SCREEN
        // ---------------------------------------------------------

        val rootLayout =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setBackgroundColor(
                    android.graphics.Color.BLACK
                )
            }

        // ---------------------------------------------------------
        // CAMERA AREA
        // ---------------------------------------------------------

        val cameraArea =
            FrameLayout(this).apply {

                setBackgroundColor(
                    android.graphics.Color.BLACK
                )
            }

        // ---------------------------------------------------------
        // CAMERA PREVIEW
        // ---------------------------------------------------------

        previewView =
            PreviewView(this).apply {

                scaleType =
                    PreviewView.ScaleType.FILL_CENTER

                implementationMode =
                    PreviewView.ImplementationMode.COMPATIBLE
            }

        // ---------------------------------------------------------
        // CAPTURE FRAME SIZE
        //
        // The phone remains PORTRAIT.
        // Only the camera window changes shape.
        // ---------------------------------------------------------

        val frameWidthDp: Int
        val frameHeightDp: Int

        if (selectedOrientation == "LANDSCAPE") {

            // Horizontal document/photo
            frameWidthDp = 340
            frameHeightDp = 225

        } else {

            // Vertical document/photo
            frameWidthDp = 225
            frameHeightDp = 340
        }

        val density =
            resources.displayMetrics.density

        val frameWidth =
            (frameWidthDp * density).toInt()

        val frameHeight =
            (frameHeightDp * density).toInt()

        // ---------------------------------------------------------
        // CAMERA FRAME CONTAINER
        // ---------------------------------------------------------

        val captureFrame =
            FrameLayout(this).apply {

                setBackgroundColor(
                    android.graphics.Color.BLACK
                )

                clipChildren = true
                clipToPadding = true
            }

        // ---------------------------------------------------------
        // PREVIEW INSIDE FRAME
        // ---------------------------------------------------------

        captureFrame.addView(
            previewView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        // ---------------------------------------------------------
        // WHITE FRAME BORDER
        // ---------------------------------------------------------

        val border =
            View(this).apply {

                background =
                    android.graphics.drawable.GradientDrawable().apply {

                        setColor(
                            android.graphics.Color.TRANSPARENT
                        )

                        setStroke(
                            4,
                            android.graphics.Color.WHITE
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

        // ---------------------------------------------------------
        // CAMERA FRAME PARAMETERS
        // ---------------------------------------------------------

        val frameParams =
            LinearLayout.LayoutParams(
                frameWidth,
                frameHeight
            ).apply {

                gravity =
                    android.view.Gravity.CENTER

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

        // ---------------------------------------------------------
        // STATUS
        // ---------------------------------------------------------

        val status =
            TextView(this).apply {

                text =
                    "Position the ${getOrientationDisplayName().lowercase()} image inside the frame\n\n" +
                    "Color: ${getColorModeDisplayName()}\n" +
                    "Orientation: ${getOrientationDisplayName()}"

                textSize = 16f

                setTextColor(
                    android.graphics.Color.WHITE
                )

                setGravity(
                    android.view.Gravity.CENTER
                )

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
                    android.view.Gravity.BOTTOM

                setMargins(
                    0,
                    0,
                    0,
                    10
                )
            }
        )

        // ---------------------------------------------------------
        // CAMERA AREA WEIGHT
        // ---------------------------------------------------------

        rootLayout.addView(
            cameraArea,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        // ---------------------------------------------------------
        // CAPTURE BUTTON
        // ---------------------------------------------------------

        val captureButton =
            createButton("Capture") {

                takePicture()
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

        // ---------------------------------------------------------
        // CANCEL BUTTON
        // ---------------------------------------------------------

        val cancelButton =
            createButton("Cancel") {

                showColorModeScreen()
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

        // ---------------------------------------------------------
        // SHOW SCREEN
        // ---------------------------------------------------------

        setContentView(rootLayout)

        // ---------------------------------------------------------
        // CAMERA PROVIDER
        // ---------------------------------------------------------

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            val cameraProvider =
                cameraProviderFuture.get()

            // -----------------------------------------------------
            // PREVIEW
            // -----------------------------------------------------

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

            // -----------------------------------------------------
            // IMAGE CAPTURE
            // -----------------------------------------------------

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
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )

            } catch (e: Exception) {

                status.text =
                    "Unable to start camera: ${e.message}"
            }

        }, ContextCompat.getMainExecutor(this))
    }

    // ---------------------------------------------------------
    // TAKE PICTURE
    // ---------------------------------------------------------

    private fun takePicture() {

        val capture =
            imageCapture ?: return

        val file =
            File(
                cacheDir,
                "capture_${System.currentTimeMillis()}.jpg"
            )

        val outputOptions =
            ImageCapture.OutputFileOptions
                .Builder(file)
                .build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object :
                ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(
                    outputFileResults:
                    ImageCapture.OutputFileResults
                ) {

                    capturedImageFile =
                        file

                    showImagePreview(file)
                }

                override fun onError(
                    exception:
                    ImageCaptureException
                ) {

                    showCaptureError(
                        exception.message
                            ?: "Capture failed"
                    )
                }
            }
        )
    }

    // ---------------------------------------------------------
    // showImagePreview
    // ---------------------------------------------------------

    private fun showImagePreview(file: File) {

        val layout =
            createVerticalLayout()

        layout.addView(
            createTitle("Captured Image")
        )

        val imageView =
            ImageView(this).apply {

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
            createButton("Retake") {

                startDocumentCamera()
            }
        )

        // ---------------------------------------------------------
        // CONFIRM
        // ---------------------------------------------------------

        layout.addView(
            createButton("Confirm") {

                // For now, simply continue to the completion screen.
                // Later we will upload the image to the backend.

                showCaptureCompleteScreen()
            }
        )

        setContentView(layout)
    }

    // ---------------------------------------------------------
    // showCaptureError
    // ---------------------------------------------------------
    private fun showCaptureError(
        message: String
    ) {

        val layout =
            createVerticalLayout()

        layout.addView(
            createTitle("Capture Error")
        )

        val errorText =
            TextView(this).apply {

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
            createButton("Try Again") {

                startDocumentCamera()
            }
        )

        layout.addView(
            createButton("Back") {

                showColorModeScreen()
            }
        )

        setContentView(layout)
    }

    // ---------------------------------------------------------
    // UI HELPERS
    // ---------------------------------------------------------

    private fun createVerticalLayout(): LinearLayout {

        return LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            setPadding(
                40,
                40,
                40,
                40
            )
        }
    }

    // ---------------------------------------------------------
    // TITLE
    // ---------------------------------------------------------

    private fun createTitle(
        textValue: String
    ): TextView {

        return TextView(this).apply {

            text = textValue

            textSize = 28f

            setPadding(
                0,
                0,
                0,
                30
            )
        }
    }

    // ---------------------------------------------------------
    // BUTTON
    // ---------------------------------------------------------

    private fun createButton(
        textValue: String,
        onClick: () -> Unit
    ): Button {

        return Button(this).apply {

            text = textValue

            isAllCaps = false

            textSize = 16f

            setOnClickListener {

                onClick()
            }
        }
    }

    // ---------------------------------------------------------
    // LOGOUT
    // ---------------------------------------------------------

    private fun logout() {

        sessionToken = null

        captureToken = null

        selectedCaptureType = null
        selectedPhotoType = null
        selectedDocumentSize = null
        selectedColorMode = null
        selectedOrientation = null

        capturedImageFile = null

        imageCapture = null

        showMainScreen()
    }

     // ---------------------------------------------------------
    // showCaptureCompleteScreen
    // ---------------------------------------------------------
    private fun showCaptureCompleteScreen() {

        val layout =
            createVerticalLayout()

        layout.addView(
            createTitle("Capture Complete")
        )

        val information =
            TextView(this).apply {

                text =
                    "The image has been captured successfully.\n\n" +
                    "Type: ${selectedCaptureType ?: "Unknown"}\n" +
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