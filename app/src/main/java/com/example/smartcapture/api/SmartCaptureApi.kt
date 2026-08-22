package com.example.smartcapture.api
 
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path

interface SmartCaptureApi {

    @GET("api/capture/{captureId}")
        suspend fun getCapture(
            @Path("captureId") captureId: String
    ): Response<CaptureResponse>

    @GET("api/capture/{captureId}/image")
        suspend fun getCaptureImage(
            @Path("captureId") captureId: String
    ): Response<ResponseBody>

    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @Multipart
    @POST("api/capture/upload")
    suspend fun uploadCapture(
        @Part("capture_token") captureToken: RequestBody,
        @Part("capture_type") captureType: RequestBody,
        @Part("photo_type") photoType: RequestBody?,
        @Part("photo_side") photoSide: RequestBody?,
        @Part("document_size") documentSize: RequestBody?,
        @Part("custom_document_width_mm") customDocumentWidthMm: RequestBody?,
        @Part("custom_document_height_mm") customDocumentHeightMm: RequestBody?,
        @Part("color_mode") colorMode: RequestBody,
        @Part("orientation") orientation: RequestBody,
        @Part images: List<MultipartBody.Part>
    ): Response<UploadResponse>
}