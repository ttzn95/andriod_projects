package com.example.smartcapture

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
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
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var statusText: TextView

    private val cameraExecutor = Executors.newSingleThreadExecutor()

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

    private fun startQrScanner() {

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        previewView = PreviewView(this)

        val scannerStatus = TextView(this).apply {
            text = "Point the camera at a QR code"
            textSize = 18f
            setPadding(20, 20, 20, 20)
        }

        layout.addView(
            previewView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        layout.addView(
            scannerStatus,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
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
                    it.surfaceProvider = previewView.surfaceProvider
                }

            val options =
                BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(
                        com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE
                    )
                    .build()

            val scanner = BarcodeScanning.getClient(options)

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

                                val value = barcode.rawValue

                                if (!value.isNullOrEmpty()) {

                                    runOnUiThread {

                                        scannerStatus.text =
                                            "QR detected:\n$value"

                                    }

                                    imageAnalysis.clearAnalyzer()

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

    override fun onDestroy() {

        super.onDestroy()

        cameraExecutor.shutdown()
    }
}
