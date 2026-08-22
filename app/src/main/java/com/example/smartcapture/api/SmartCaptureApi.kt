package com.example.smartcapture.api
 
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface SmartCaptureApi {

    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @Multipart
    @POST("api/capture/upload")
    suspend fun uploadCapture(
        @Part("capture_token") captureToken: RequestBody,
        @Part image: MultipartBody.Part
    ): Response<UploadResponse>
}