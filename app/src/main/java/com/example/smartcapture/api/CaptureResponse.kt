package com.example.smartcapture.api

data class CaptureResponse(
    val success: Boolean,
    val capture: Capture
)

data class Capture(
    val capture_id: String,
    val staff_id: String,
    val capture_type: String?,
    val photo_type: String?,
    val photo_side: String?,
    val document_size: String?,
    val custom_document_width_mm: Float?,
    val custom_document_height_mm: Float?,
    val color_mode: String?,
    val orientation: String?,
    val filename: String?,
    val content_type: String?,
    val stored: String?,
    val created_at: String?,
    val status: String?
)