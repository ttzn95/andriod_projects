package com.example.androidprojects.model

import androidx.compose.ui.graphics.Color
import com.example.androidprojects.ui.theme.*

enum class ProjectCategory(val displayName: String, val color: Color) {
    ALL("All", PrimaryIndigo),
    ECOMMERCE("E-Commerce", CatEcommerce),
    HEALTH("Health & Fitness", CatHealth),
    IOT("Smart Home / IoT", CatIoT),
    CRYPTO("FinTech & Crypto", CatCrypto),
    AI("AI & LLM", CatAI),
    WEATHER("Weather & Radar", CatWeather),
    PRODUCTIVITY("Productivity", CatSocial)
}

enum class ProjectStatus(val label: String) {
    COMPLETED("Completed"),
    IN_PROGRESS("In Progress"),
    PLANNING("Planning"),
    ARCH_REVIEW("Review")
}

enum class InteractiveDemoType {
    ANIMATED_COUNTER,
    BIOMETRIC_CARD,
    SENSOR_TELEMETRY,
    CHAT_BUBBLES,
    CHART_VISUALIZER,
    THEME_PALETTE
}

data class ProjectFeature(
    val title: String,
    val description: String,
    val isImplemented: Boolean = true
)

data class CodeSnippet(
    val id: String,
    val title: String,
    val category: String,
    val language: String = "Kotlin",
    val description: String,
    val code: String
)

data class Project(
    val id: String,
    val title: String,
    val tagLine: String,
    val description: String,
    val category: ProjectCategory,
    val status: ProjectStatus,
    val progress: Float, // 0.0f to 1.0f
    val techStack: List<String>,
    val architecture: String,
    val isFavorite: Boolean = false,
    val starCount: Int = 120,
    val features: List<ProjectFeature>,
    val snippets: List<CodeSnippet>,
    val interactiveDemoType: InteractiveDemoType = InteractiveDemoType.ANIMATED_COUNTER
)
