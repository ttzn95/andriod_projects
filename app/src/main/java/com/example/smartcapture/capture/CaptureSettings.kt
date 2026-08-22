package com.example.smartcapture.capture

data class CaptureSettings(

    var captureType: CaptureType? = null,

    var photoType: PhotoType? = null,

    var photoSide: PhotoSide? = null,

    var documentSize: DocumentSize? = null,

    var customDocumentWidthMm: Float? = null,

    var customDocumentHeightMm: Float? = null,

    var colorMode: ColorMode = ColorMode.COLOR,

    var orientation: OrientationMode =
        OrientationMode.PORTRAIT
)