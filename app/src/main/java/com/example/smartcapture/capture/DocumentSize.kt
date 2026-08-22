package com.example.smartcapture.capture

enum class DocumentSize(
    val displayName: String,
    val widthMm: Int?,
    val heightMm: Int?
) {

    A4(
        displayName = "A4",
        widthMm = 210,
        heightMm = 297
    ),

    LEGAL(
        displayName = "Legal",
        widthMm = 216,
        heightMm = 356
    ),

    A5(
        displayName = "A5",
        widthMm = 148,
        heightMm = 210
    ),

    B5(
        displayName = "B5",
        widthMm = 176,
        heightMm = 250
    ),

    CUSTOM(
        displayName = "Custom",
        widthMm = null,
        heightMm = null
    )
}