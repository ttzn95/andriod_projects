package com.example.smartcapture

import android.view.MotionEvent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

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

import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

import java.util.concurrent.Executors

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var statusText: TextView

    private val cameraExecutor =
        Executors.newSingleThreadExecutor()

    /*
     * ---------------------------------------------------------
     * SESSION / AUTHENTICATION
     * ---------------------------------------------------------
     */

    private var sessionToken: String? = null

    private var captureToken: String? = null

    private var staffName: String? = null


    /*
     * ---------------------------------------------------------
     * CAPTURE SELECTION
     * ---------------------------------------------------------
     *
     * Example:
     *
     * captureType = PHOTO
     * captureSubType = NRC
     * imageMode = COLOR
     *
     * OR
     *
     * captureType = DOCUMENT
     * captureSubType = A4
     * imageMode = BW
     */

    private var captureType: String? = null

    private var captureSubType: String? = null

    private var imageMode: String? = null


    /*
     * ---------------------------------------------------------
     * CAMERA PERMISSION
     * ---------------------------------------------------------
     */

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


    /*
     * ---------------------------------------------------------
     * ACTIVITY
     * ---------------------------------------------------------
     */

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        showMainScreen()
    }


    /*
     * =========================================================
     * MAIN SCREEN
     * =========================================================
     */

    private fun showMainScreen() {

        val layout = LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            setPadding(
                40,
                80,
                40,
                40
            )
        }


        val title = TextView(this).apply {

            text =
                "Smart Capture"

            textSize =
                28f
        }


        statusText = TextView(this).apply {

            text =
                "Ready"

            textSize =
                18f

            setPadding(
                0,
                40,
                0,
                40
            )
        }


        val scanButton = Button(this).apply {

            text =
                "Scan QR Code"

            setOnClickListener {

                requestCameraPermission()
            }
        }


        layout.addView(title)

        layout.addView(statusText)

        layout.addView(scanButton)


        setContentView(layout)
    }


    /*
     * =========================================================
     * CAMERA PERMISSION
     * =========================================================
     */

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


    /*
     * =========================================================
     * QR SCANNER
     * =========================================================
     */

    private fun startQrScanner() {

        val layout = LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL
        }


        previewView =
            PreviewView(this)


        val scannerStatus = TextView(this).apply {

            text =
                "Point the camera at the DMS QR code"

            textSize =
                18f

            setPadding(
                20,
                20,
                20,
                20
            )
        }


        layout.addView(
            previewView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )


        layout.addView(
            scannerStatus,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )


        setContentView(layout)


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


            val options =
                BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(
                        com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE
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

                                    /*
                                     * Stop scanner immediately.
                                     */

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


    /*
     * =========================================================
     * HANDLE QR CODE
     * =========================================================
     */

    private fun handleQrCode(
        qrValue: String
    ) {

        /*
         * For the current prototype the QR contains
         * the session token directly.
         */

        sessionToken =
            qrValue


        /*
         * Next step is staff login.
         */

        showLoginScreen()
    }


    /*
     * =========================================================
     * STAFF LOGIN
     * =========================================================
     */

    private fun showLoginScreen() {

        val layout =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    40,
                    80,
                    40,
                    40
                )
            }


        val title =
            TextView(this).apply {

                text =
                    "Staff Login"

                textSize =
                    28f
            }


        val information =
            TextView(this).apply {

                text =
                    "Enter your staff credentials"

                textSize =
                    17f

                setPadding(
                    0,
                    30,
                    0,
                    30
                )
            }


        val staffIdInput =
            EditText(this).apply {

                hint =
                    "Staff ID"

                inputType =
                    InputType.TYPE_CLASS_TEXT
            }


        val passwordInput =
            EditText(this).apply {

                hint =
                    "Password"

                inputType =
                    InputType.TYPE_CLASS_TEXT or
                            InputType.TYPE_TEXT_VARIATION_PASSWORD


                /*
                 * Password eye icon.
                 */

                setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    android.R.drawable.ic_menu_view,
                    0
                )


                setOnTouchListener { _, event ->

                    if (
                        event.action ==
                        MotionEvent.ACTION_UP &&
                        event.x >=
                        width -
                        compoundDrawablePadding -
                        80
                    ) {

                        val isPasswordVisible =
                            inputType ==
                                    (
                                            InputType.TYPE_CLASS_TEXT or
                                                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                                            )


                        if (isPasswordVisible) {

                            inputType =
                                InputType.TYPE_CLASS_TEXT or
                                        InputType.TYPE_TEXT_VARIATION_PASSWORD

                        } else {

                            inputType =
                                InputType.TYPE_CLASS_TEXT or
                                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        }


                        setSelection(
                            text.length
                        )


                        true

                    } else {

                        false
                    }
                }
            }


        val loginStatus =
            TextView(this).apply {

                textSize =
                    16f

                setPadding(
                    0,
                    30,
                    0,
                    30
                )
            }


        lateinit var loginButton: Button


        loginButton =
            Button(this).apply {

                text =
                    "Login"


                setOnClickListener {

                    val staffId =
                        staffIdInput
                            .text
                            .toString()
                            .trim()


                    val password =
                        passwordInput
                            .text
                            .toString()


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


                    loginButton.isEnabled =
                        false


                    loginStatus.text =
                        "Authenticating..."


                    login(
                        staffId = staffId,
                        password = password,
                        loginStatus = loginStatus,
                        loginButton = loginButton
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


    /*
     * =========================================================
     * BACKEND LOGIN
     * =========================================================
     */

    private fun login(
        staffId: String,
        password: String,
        loginStatus: TextView,
        loginButton: Button
    ) {

        val token =
            sessionToken


        if (token.isNullOrBlank()) {

            loginStatus.text =
                "Session is missing. Please scan the QR again."

            loginButton.isEnabled =
                true

            return
        }


        CoroutineScope(
            Dispatchers.IO
        ).launch {

            try {

                val response =
                    ApiClient.api.login(
                        LoginRequest(
                            session_token =
                                token,

                            staff_id =
                                staffId,

                            password =
                                password
                        )
                    )


                withContext(
                    Dispatchers.Main
                ) {

                    loginButton.isEnabled =
                        true


                    if (response.isSuccessful) {

                        val body =
                            response.body()


                        if (
                            body != null &&
                            body.success
                        ) {

                            /*
                             * Authentication successful.
                             */

                            captureToken =
                                body.capture_token


                            staffName =
                                body.staff_name


                            showReadyScreen(
                                body.staff_name
                            )

                        } else {

                            loginStatus.text =
                                "Authentication failed."
                        }

                    } else {

                        when (
                            response.code()
                        ) {

                            401 -> {

                                loginStatus.text =
                                    "Invalid Staff ID or password."
                            }


                            403 -> {

                                loginStatus.text =
                                    "Staff is not authorized."
                            }


                            else -> {

                                loginStatus.text =
                                    "Server error: ${response.code()}"
                            }
                        }
                    }
                }

            } catch (e: Exception) {

                withContext(
                    Dispatchers.Main
                ) {

                    loginButton.isEnabled =
                        true

                    loginStatus.text =
                        "Unable to connect to server."
                }
            }
        }
    }


    /*
     * =========================================================
     * READY / CAPTURE TYPE SCREEN
     * =========================================================
     */

    private fun showReadyScreen(
        currentStaffName: String
    ) {

        val layout =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    40,
                    80,
                    40,
                    40
                )
            }


        val title =
            TextView(this).apply {

                text =
                    "Smart Capture"

                textSize =
                    28f
            }


        val welcome =
            TextView(this).apply {

                text =
                    "Welcome, $currentStaffName"

                textSize =
                    18f

                setPadding(
                    0,
                    30,
                    0,
                    20
                )
            }


        val instruction =
            TextView(this).apply {

                text =
                    "What would you like to capture?"

                textSize =
                    18f

                setPadding(
                    0,
                    20,
                    0,
                    30
                )
            }


        val photosButton =
            Button(this).apply {

                text =
                    "Photos"


                setOnClickListener {

                    showPhotoTypes()
                }
            }


        val documentsButton =
            Button(this).apply {

                text =
                    "Documents"


                setOnClickListener {

                    showDocumentSizes()
                }
            }


        val logoutButton =
            Button(this).apply {

                text =
                    "Logout"


                setOnClickListener {

                    logout()
                }
            }


        layout.addView(title)

        layout.addView(welcome)

        layout.addView(instruction)

        layout.addView(photosButton)

        layout.addView(documentsButton)

        layout.addView(logoutButton)


        setContentView(layout)
    }


    /*
     * =========================================================
     * PHOTO TYPES
     * =========================================================
     */

    private fun showPhotoTypes() {

        val layout =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    40,
                    80,
                    40,
                    40
                )
            }


        val title =
            TextView(this).apply {

                text =
                    "Select Photo Type"

                textSize =
                    28f
            }


        layout.addView(title)


        val licenseButton =
            Button(this).apply {

                text =
                    "License"


                setOnClickListener {

                    selectCaptureType(
                        "PHOTO",
                        "LICENSE"
                    )
                }
            }


        val nrcButton =
            Button(this).apply {

                text =
                    "NRC"


                setOnClickListener {

                    selectCaptureType(
                        "PHOTO",
                        "NRC"
                    )
                }
            }


        val employmentButton =
            Button(this).apply {

                text =
                    "Employment Card"


                setOnClickListener {

                    selectCaptureType(
                        "PHOTO",
                        "EMPLOYMENT_CARD"
                    )
                }
            }


        val chequeButton =
            Button(this).apply {

                text =
                    "Cheque"


                setOnClickListener {

                    selectCaptureType(
                        "PHOTO",
                        "CHEQUE"
                    )
                }
            }


        val customButton =
            Button(this).apply {

                text =
                    "Custom Photo"


                setOnClickListener {

                    selectCaptureType(
                        "PHOTO",
                        "CUSTOM"
                    )
                }
            }


        val backButton =
            Button(this).apply {

                text =
                    "Back"


                setOnClickListener {

                    showReadyScreen(
                        getCurrentStaffName()
                    )
                }
            }


        layout.addView(licenseButton)

        layout.addView(nrcButton)

        layout.addView(employmentButton)

        layout.addView(chequeButton)

        layout.addView(customButton)

        layout.addView(backButton)


        setContentView(layout)
    }


    /*
     * =========================================================
     * DOCUMENT SIZES
     * =========================================================
     */

    private fun showDocumentSizes() {

        val layout =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    40,
                    80,
                    40,
                    40
                )
            }


        val title =
            TextView(this).apply {

                text =
                    "Select Document Size"

                textSize =
                    28f
            }


        layout.addView(title)


        val a4Button =
            Button(this).apply {

                text =
                    "A4"


                setOnClickListener {

                    selectCaptureType(
                        "DOCUMENT",
                        "A4"
                    )
                }
            }


        val legalButton =
            Button(this).apply {

                text =
                    "Legal"


                setOnClickListener {

                    selectCaptureType(
                        "DOCUMENT",
                        "LEGAL"
                    )
                }
            }


        val a5Button =
            Button(this).apply {

                text =
                    "A5"


                setOnClickListener {

                    selectCaptureType(
                        "DOCUMENT",
                        "A5"
                    )
                }
            }


        val b5Button =
            Button(this).apply {

                text =
                    "B5"


                setOnClickListener {

                    selectCaptureType(
                        "DOCUMENT",
                        "B5"
                    )
                }
            }


        val customButton =
            Button(this).apply {

                text =
                    "Custom Size"


                setOnClickListener {

                    selectCaptureType(
                        "DOCUMENT",
                        "CUSTOM"
                    )
                }
            }


        val backButton =
            Button(this).apply {

                text =
                    "Back"


                setOnClickListener {

                    showReadyScreen(
                        getCurrentStaffName()
                    )
                }
            }


        layout.addView(a4Button)

        layout.addView(legalButton)

        layout.addView(a5Button)

        layout.addView(b5Button)

        layout.addView(customButton)

        layout.addView(backButton)


        setContentView(layout)
    }


    /*
     * =========================================================
     * STORE CAPTURE TYPE
     * =========================================================
     */

    private fun selectCaptureType(
        type: String,
        subType: String
    ) {

        captureType =
            type

        captureSubType =
            subType

        /*
         * After selecting the document/photo type,
         * ask for Color or Black & White.
         */

        showImageModeSelection()
    }


    /*
     * =========================================================
     * IMAGE MODE
     * =========================================================
     */

    private fun showImageModeSelection() {

        val layout =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    40,
                    80,
                    40,
                    40
                )
            }


        val title =
            TextView(this).apply {

                text =
                    "Select Image Mode"

                textSize =
                    28f
            }


        val information =
            TextView(this).apply {

                text =
                    "Choose how the captured image should be processed."

                textSize =
                    17f

                setPadding(
                    0,
                    30,
                    0,
                    30
                )
            }


        val selectionInfo =
            TextView(this).apply {

                text =
                    "Type: ${captureType ?: "-"}\n" +
                            "Selection: ${captureSubType ?: "-"}"

                textSize =
                    16f

                setPadding(
                    0,
                    10,
                    0,
                    30
                )
            }


        val colorButton =
            Button(this).apply {

                text =
                    "Color"


                setOnClickListener {

                    imageMode =
                        "COLOR"

                    startCapture()
                }
            }


        val blackWhiteButton =
            Button(this).apply {

                text =
                    "Black & White"


                setOnClickListener {

                    imageMode =
                        "BW"

                    startCapture()
                }
            }


        val backButton =
            Button(this).apply {

                text =
                    "Back"


                setOnClickListener {

                    if (
                        captureType ==
                        "PHOTO"
                    ) {

                        showPhotoTypes()

                    } else {

                        showDocumentSizes()
                    }
                }
            }


        layout.addView(title)

        layout.addView(information)

        layout.addView(selectionInfo)

        layout.addView(colorButton)

        layout.addView(blackWhiteButton)

        layout.addView(backButton)


        setContentView(layout)
    }


    /*
     * =========================================================
     * CAPTURE SCREEN
     * =========================================================
     *
     * Temporary screen.
     *
     * The real document camera will be implemented next.
     */

    private fun startCapture() {

        val layout =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    40,
                    80,
                    40,
                    40
                )
            }


        val title =
            TextView(this).apply {

                text =
                    "Capture"

                textSize =
                    28f
            }


        val information =
            TextView(this).apply {

                text =
                    """
                    Capture Type: ${captureType ?: "-"}

                    Selection: ${captureSubType ?: "-"}

                    Image Mode: ${
                        if (imageMode == "BW")
                            "Black & White"
                        else
                            "Color"
                    }

                    Camera capture will be implemented next.
                    """.trimIndent()

                textSize =
                    18f

                setPadding(
                    0,
                    40,
                    0,
                    40
                )
            }


        val captureButton =
            Button(this).apply {

                text =
                    "Open Camera"


                setOnClickListener {

                    /*
                     * Real document/photo camera
                     * will be implemented in the next phase.
                     */

                    information.text =
                        "Camera module will be implemented next."
                }
            }


        val backButton =
            Button(this).apply {

                text =
                    "Back"


                setOnClickListener {

                    showImageModeSelection()
                }
            }


        layout.addView(title)

        layout.addView(information)

        layout.addView(captureButton)

        layout.addView(backButton)


        setContentView(layout)
    }


    /*
     * =========================================================
     * STAFF NAME
     * =========================================================
     */

    private fun getCurrentStaffName(): String {

        return staffName ?: "Staff"
    }


    /*
     * =========================================================
     * LOGOUT
     * =========================================================
     */

    private fun logout() {

        /*
         * Destroy all authentication information.
         */

        sessionToken =
            null

        captureToken =
            null

        staffName =
            null


        /*
         * Destroy capture selection too.
         */

        captureType =
            null

        captureSubType =
            null

        imageMode =
            null


        showMainScreen()
    }


    /*
     * =========================================================
     * CLEANUP
     * =========================================================
     */

    override fun onDestroy() {

        super.onDestroy()


        /*
         * Never keep authentication tokens
         * after the Activity is destroyed.
         */

        sessionToken =
            null

        captureToken =
            null

        staffName =
            null


        captureType =
            null

        captureSubType =
            null

        imageMode =
            null


        cameraExecutor.shutdown()
    }
}