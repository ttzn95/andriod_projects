package com.example.smartcapture.api

data class LoginRequest(
    val session_token: String,
    val staff_id: String,
    val password: String
)

data class LoginResponse(
    val success: Boolean,
    val staff_id: String,
    val staff_name: String,
    val capture_token: String,
    val expires_at: String
)

data class SessionResponse(
    val session_token: String,
    val expires_at: String,
    val status: String
)

data class UploadResponse(
    val success: Boolean,
    val message: String,
    val filename: String?,
    val staff_id: String?,
    val size: Int?
)

data class ApiError(
    val detail: String?
)