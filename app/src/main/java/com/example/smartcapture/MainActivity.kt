package com.example.smartcapture

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

    private val cameraExecutor = Executors.newSingleThreadExecutor()

    /*
     * IMPORTANT:
     *
     * Keep the session token only in memory.
     *
     * Do NOT save it to SharedPreferences,
     * files, gallery, or database.
     */
    private var sessionToken: String? = null

    /*
     * Capture token returned by backend after successful login.
     *
     * Also kept only in memory for now.
     */
    private var captureToken: String? = null

    private val cameraPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                startQrScanner()
            } else {
                statusText.text = "Camera permission is required."
            }
        }

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

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        previewView = PreviewView(this)

        val scannerStatus = TextView(this).apply {
            text = "Point the camera at the DMS QR code"
            textSize = 18f
            setPadding(20, 20, 20, 20)
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

            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.surfaceProvider =
                        previewView.surfaceProvider
                }

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

                val mediaImage = imageProxy.image

                if (mediaImage != null) {

                    val image = InputImage.fromMediaImage(
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
                                     * Stop scanning immediately.
                                     */
                                    imageAnalysis.clearAnalyzer()

                                    runOnUiThread {

                                        /*
                                         * DO NOT DISPLAY THE TOKEN.
                                         */
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

        /*
         * For our first prototype, the QR contains
         * the session token directly.
         *
         * Later we can make the QR a signed payload such as:
         *
         * {
         *   "session_id": "...",
         *   "nonce": "...",
         *   "expires_at": "..."
         * }
         */

        sessionToken = qrValue

        showLoginScreen()
    }

    // ---------------------------------------------------------
    // STAFF LOGIN SCREEN
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

        val staffIdInput = EditText(this).apply {
            hint = "Staff ID"
            inputType = InputType.TYPE_CLASS_TEXT
        }

        val passwordInput = EditText(this).apply {
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
            event.action == android.view.MotionEvent.ACTION_UP &&
            event.x >= (width - compoundDrawablePadding - 80)
        ) {

            val isPasswordVisible =
                inputType ==
                    (InputType.TYPE_CLASS_TEXT or
                     InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)

            if (isPasswordVisible) {

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

        val loginStatus = TextView(this).apply {
            textSize = 16f
            setPadding(0, 30, 0, 30)
        }

        /*
         * Declare the variable first.
         *
         * We cannot reference loginButton while the Button
         * itself is still being initialized.
         */
        lateinit var loginButton: Button

        loginButton = Button(this)

        loginButton.text = "Login"

        loginButton.setOnClickListener {

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

            loginButton.isEnabled = false

            loginStatus.text =
                "Authenticating..."

            login(
                staffId = staffId,
                password = password,
                loginStatus = loginStatus,
                loginButton = loginButton
            )
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

                        val body = response.body()

                        if (body != null && body.success) {

                            /*
                             * Authentication succeeded.
                             *
                             * Keep capture token in memory only.
                             */
                            captureToken = body.capture_token

                            showReadyScreen(body.staff_name)

                        } else {

                            loginStatus.text =
                                "Authentication failed."
                        }

                    } else {

                        when (response.code()) {

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

        val captureButton = Button(this).apply {
            text = "Capture Document"

            setOnClickListener {

                /*
                 * Next phase:
                 *
                 * Open camera
                 * Capture document
                 * Crop
                 * Perspective correction
                 * Image quality check
                 * Upload to DMS
                 */
                status.text =
                    "Document capture will be implemented next."
            }
        }

        val logoutButton = Button(this).apply {
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
    // LOGOUT
    // ---------------------------------------------------------

    private fun logout() {

        /*
         * Important:
         * Destroy both tokens from memory.
         */
        sessionToken = null
        captureToken = null

        showMainScreen()
    }

    // ---------------------------------------------------------
    // ACTIVITY CLEANUP
    // ---------------------------------------------------------

    override fun onDestroy() {

        super.onDestroy()

        sessionToken = null
        captureToken = null

        cameraExecutor.shutdown()
    }
}
