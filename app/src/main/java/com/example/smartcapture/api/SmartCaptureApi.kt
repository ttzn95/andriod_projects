package com.example.smartcapture.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface SmartCaptureApi {

    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>
}
