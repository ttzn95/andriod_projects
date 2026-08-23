package com.example.smartcapture.api

import com.example.smartcapture.capture.CaptureSettings
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File

class CaptureUploadParts private constructor(
    val captureToken: RequestBody,
    val captureType: RequestBody,
    val photoType: RequestBody?,
    val photoSide: RequestBody?,
    val documentSize: RequestBody?,
    val customDocumentWidthMm: RequestBody?,
    val customDocumentHeightMm: RequestBody?,
    val colorMode: RequestBody,
    val orientation: RequestBody,
    val images: List<MultipartBody.Part>
) {

    companion object {
        private val textMediaType = MediaType.parse("text/plain")

        fun create(
            token: String,
            captureType: String,
            settings: CaptureSettings,
            files: List<File>
        ): CaptureUploadParts {
            return CaptureUploadParts(
                captureToken = text(token),
                captureType = text(captureType),
                photoType = settings.photoType?.name?.let(::text),
                photoSide = settings.photoSide?.name?.let(::text),
                documentSize = settings.documentSize?.name?.let(::text),
                customDocumentWidthMm = settings.customDocumentWidthMm?.toString()?.let(::text),
                customDocumentHeightMm = settings.customDocumentHeightMm?.toString()?.let(::text),
                colorMode = text(settings.colorMode.name),
                orientation = text(settings.orientation.name),
                images = files.map { imagePart(it) }
            )
        }

        private fun text(value: String): RequestBody =
            RequestBody.create(textMediaType, value)

        private fun imagePart(file: File): MultipartBody.Part {
            val mimeType = when (file.extension.lowercase()) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "svg" -> "image/svg+xml"
                else -> "application/octet-stream"
            }
            return MultipartBody.Part.createFormData(
                "images",
                file.name,
                RequestBody.create(MediaType.parse(mimeType), file)
            )
        }
    }
}
