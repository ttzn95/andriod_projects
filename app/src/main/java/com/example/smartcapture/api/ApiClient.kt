package com.example.smartcapture.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private const val BASE_URL =
        "https://improved-doodle-976pxr49xqq5fwpr-8000.app.github.dev/"

    val api: SmartCaptureApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(SmartCaptureApi::class.java)
    }
}
