package com.example.androidprojects.data

import com.example.androidprojects.model.*

object SampleData {

    val snippetsList = listOf(
        CodeSnippet(
            id = "s1",
            title = "Modern StateFlow in ViewModel",
            category = "Architecture",
            description = "Recommended pattern for exposing UI State using StateFlow with WhileSubscribed.",
            code = """
// ViewModel state exposition
private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
val uiState: StateFlow<UiState> = _uiState
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState.Loading
    )
            """.trimIndent()
        ),
        CodeSnippet(
            id = "s2",
            title = "Custom Canvas Circular Progress Indicator",
            category = "Jetpack Compose",
            description = "Draw dynamic arc with gradient stroke on Canvas with animated sweep angle.",
            code = """
@Composable
fun GradientCircularProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(Color(0xFF6366F1), Color(0xFF14B8A6))
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "progress"
    )
    Canvas(modifier = modifier.size(120.dp)) {
        val stroke = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
        drawArc(
            brush = Brush.sweepGradient(colors),
            startAngle = -90f,
            sweepAngle = 360f * animatedProgress,
            useCenter = false,
            style = stroke
        )
    }
}
            """.trimIndent()
        ),
        CodeSnippet(
            id = "s3",
            title = "Type-Safe Navigation Compose",
            category = "Navigation",
            description = "Type-safe navigation routing with Kotlinx Serialization and NavHost.",
            code = """
@Serializable
object HomeRoute

@Serializable
data class DetailRoute(val projectId: String)

// In NavHost setup
NavHost(navController = navController, startDestination = HomeRoute) {
    composable<HomeRoute> {
        HomeScreen(onNavigateToDetail = { id -> 
            navController.navigate(DetailRoute(projectId = id))
        })
    }
    composable<DetailRoute> { backStackEntry ->
        val route: DetailRoute = backStackEntry.toRoute()
        DetailScreen(projectId = route.projectId)
    }
}
            """.trimIndent()
        ),
        CodeSnippet(
            id = "s4",
            title = "Room Database Flow Dao Pattern",
            category = "Database",
            description = "Reactive room querying using Flow with Coroutines.",
            code = """
@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)
}
            """.trimIndent()
        )
    )

    val sampleProjects = listOf(
        Project(
            id = "proj-1",
            title = "Aetheria Commerce",
            tagLine = "Full-featured modern Jetpack Compose E-commerce store",
            description = "A responsive, modern online shopping application with animated product cards, checkout flows, category carousels, and persistent shopping cart using MVI architecture.",
            category = ProjectCategory.ECOMMERCE,
            status = ProjectStatus.COMPLETED,
            progress = 1.0f,
            techStack = listOf("Jetpack Compose", "MVI Architecture", "Room", "Coil", "Coroutines"),
            architecture = "Clean Architecture + MVI Orbit",
            isFavorite = true,
            starCount = 284,
            features = listOf(
                ProjectFeature("Animated Shopping Cart", "Seamless badge counters with slide-in cart drawer"),
                ProjectFeature("Product Carousel", "Horizontal pager with zoom previews and color swatch selector"),
                ProjectFeature("Instant Checkout", "Multi-step payment sheet with validation"),
                ProjectFeature("Offline Wishlist", "Room-backed offline caching with instant sync")
            ),
            snippets = listOf(
                CodeSnippet(
                    id = "p1-s1",
                    title = "Cart Badge Animation",
                    category = "UI Animation",
                    description = "Bouncing badge counter when adding items",
                    code = """
@Composable
fun CartBadge(count: Int) {
    AnimatedContent(
        targetState = count,
        transitionSpec = {
            slideInVertically { it } + fadeIn() togetherWith
            slideOutVertically { -it } + fadeOut()
        },
        label = "cart_badge"
    ) { targetCount ->
        Text(
            text = "${'$'}targetCount",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}
                    """.trimIndent()
                )
            ),
            interactiveDemoType = InteractiveDemoType.ANIMATED_COUNTER
        ),
        Project(
            id = "proj-2",
            title = "VitalPulse Health & Fitness",
            tagLine = "Biometric health dashboard & workout telemetry",
            description = "Health tracking companion featuring heart rate monitoring simulations, step goal circular rings, customizable workout plans, and weekly calorie expenditure telemetry.",
            category = ProjectCategory.HEALTH,
            status = ProjectStatus.COMPLETED,
            progress = 0.95f,
            techStack = listOf("Jetpack Compose", "Canvas 2D", "Health Connect API", "ViewModel", "Flow"),
            architecture = "MVVM + Repository Pattern",
            isFavorite = true,
            starCount = 412,
            features = listOf(
                ProjectFeature("Live Heart Rate Telemetry", "Real-time pulse graph rendered on Compose Canvas"),
                ProjectFeature("Ring Progress Tracker", "Triple-ring activity visualizer for Move, Exercise, Stand"),
                ProjectFeature("Workout Session Logger", "Interval timer with audio alerts and zone tracking"),
                ProjectFeature("Sleep Cycle Analysis", "Deep/REM/Light sleep stage breakdown bar chart")
            ),
            snippets = listOf(
                CodeSnippet(
                    id = "p2-s1",
                    title = "Heart Rate Waveform Draw",
                    category = "Canvas",
                    description = "Rendering real-time ECG waveform path",
                    code = """
fun DrawScope.drawEcgWave(points: List<Float>, color: Color) {
    val path = Path()
    points.forEachIndexed { index, y ->
        val x = (size.width / points.size) * index
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path, color = color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
}
                    """.trimIndent()
                )
            ),
            interactiveDemoType = InteractiveDemoType.BIOMETRIC_CARD
        ),
        Project(
            id = "proj-3",
            title = "Nexus Smart IoT Hub",
            tagLine = "Connected home automation and device manager",
            description = "Control lights, thermostat, security sensors, and smart plugs with haptic feedback, custom rotary sliders, scene automations, and live device telemetry.",
            category = ProjectCategory.IOT,
            status = ProjectStatus.IN_PROGRESS,
            progress = 0.78f,
            techStack = listOf("Jetpack Compose", "MQTT / WebSockets", "Custom Gestures", "Ktor"),
            architecture = "Clean Architecture + Domain Use Cases",
            isFavorite = false,
            starCount = 189,
            features = listOf(
                ProjectFeature("Rotary Thermostat Dial", "Custom interactive circular drag slider with temperature readout"),
                ProjectFeature("Scene Automation Engine", "Rules builder for sunrise/sunset device triggers"),
                ProjectFeature("Live Camera Grid", "Low latency stream viewports with motion detection overlays"),
                ProjectFeature("Energy Metering", "Hourly kWh breakdown with cost estimators")
            ),
            snippets = listOf(
                CodeSnippet(
                    id = "p3-s1",
                    title = "Interactive Rotary Dial Gestures",
                    category = "Gestures",
                    description = "Calculating touch angle for rotary dial control",
                    code = """
Modifier.pointerInput(Unit) {
    detectDragGestures { change, _ ->
        val center = Offset(size.width / 2f, size.height / 2f)
        val touch = change.position
        val angle = Math.toDegrees(atan2((touch.y - center.y).toDouble(), (touch.x - center.x).toDouble())).toFloat()
        onAngleChanged((angle + 360) % 360)
    }
}
                    """.trimIndent()
                )
            ),
            interactiveDemoType = InteractiveDemoType.SENSOR_TELEMETRY
        ),
        Project(
            id = "proj-4",
            title = "Nova AI Assistant",
            tagLine = "Conversational AI assistant with Gemini Pro integration",
            description = "Intelligent mobile assistant with streaming markdown chat bubbles, multimodal photo queries, voice transcription waveform, and code snippet formatting.",
            category = ProjectCategory.AI,
            status = ProjectStatus.COMPLETED,
            progress = 1.0f,
            techStack = listOf("Gemini API", "Jetpack Compose", "Markdown Parser", "Coroutines Flow"),
            architecture = "MVI + Sealed UI State",
            isFavorite = true,
            starCount = 530,
            features = listOf(
                ProjectFeature("Streaming Response Bubbles", "Real-time token-by-token typewriter chat bubbles"),
                ProjectFeature("Multimodal Image Prompts", "Camera snap with visual reasoning capabilities"),
                ProjectFeature("Code Highlight Rendering", "Monospace formatted syntax highlighted snippets"),
                ProjectFeature("Contextual Memory", "Local conversation thread retention and export")
            ),
            snippets = listOf(
                CodeSnippet(
                    id = "p4-s1",
                    title = "Streaming Gemini Response Flow",
                    category = "AI / Network",
                    description = "Collecting chunks and updating state",
                    code = """
suspend fun streamPrompt(prompt: String) {
    generativeModel.generateContentStream(prompt).collect { chunk ->
        _uiState.update { current ->
            current.copy(streamedText = current.streamedText + (chunk.text ?: ""))
        }
    }
}
                    """.trimIndent()
                )
            ),
            interactiveDemoType = InteractiveDemoType.CHAT_BUBBLES
        ),
        Project(
            id = "proj-5",
            title = "Apex Crypto & FinTech",
            tagLine = "Real-time cryptocurrency portfolio & market tracker",
            description = "Track Bitcoin, Ethereum, and DeFi assets with live candlestick charts, portfolio allocation breakdown, price alert notifications, and mock swap simulator.",
            category = ProjectCategory.CRYPTO,
            status = ProjectStatus.IN_PROGRESS,
            progress = 0.65f,
            techStack = listOf("Jetpack Compose", "WebSocket", "Vico Charts", "StateFlow"),
            architecture = "MVVM + Clean Data Layer",
            isFavorite = false,
            starCount = 195,
            features = listOf(
                ProjectFeature("Interactive Sparkline Charts", "Touch-scrubbable price charts with tooltip timestamps"),
                ProjectFeature("Asset Distribution Donut", "Canvas donut chart displaying portfolio weights"),
                ProjectFeature("Instant Token Swap", "Slippage calculation and gas estimator UI"),
                ProjectFeature("Realtime Ticker Tape", "Animated marquee banner showing top gainers/losers")
            ),
            snippets = listOf(),
            interactiveDemoType = InteractiveDemoType.CHART_VISUALIZER
        ),
        Project(
            id = "proj-6",
            title = "Stratus Weather & Radar",
            tagLine = "Hyper-local weather forecasts with animated sky conditions",
            description = "Dynamic meteorological forecast application featuring animated rain particle systems, UV index gauges, hourly precipitation bar charts, and radar maps.",
            category = ProjectCategory.WEATHER,
            status = ProjectStatus.COMPLETED,
            progress = 1.0f,
            techStack = listOf("Jetpack Compose", "Custom Shaders", "Location API", "Ktor"),
            architecture = "MVVM + Offline Cache",
            isFavorite = false,
            starCount = 310,
            features = listOf(
                ProjectFeature("Particle Rain & Snow Effect", "Custom canvas particle physics simulation"),
                ProjectFeature("Hourly Temperature Wave", "Smooth cubic bezier curve temperature graph"),
                ProjectFeature("Air Quality & UV Gauges", "Segmented meter gauges with health advisory tips"),
                ProjectFeature("Severe Storm Warnings", "Animated emergency banner with alert levels")
            ),
            snippets = listOf(),
            interactiveDemoType = InteractiveDemoType.THEME_PALETTE
        )
    )
}
