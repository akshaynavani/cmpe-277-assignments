package com.example.android.interviewassistant.ui.theme

import androidx.compose.ui.graphics.Color

// Material3 color scheme — seed: Deep Indigo #5C6BC0
val Primary = Color(0xFF5C6BC0)
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFFE0E2FF)
val OnPrimaryContainer = Color(0xFF0A0C5E)
val Secondary = Color(0xFF5D5B71)
val OnSecondary = Color(0xFFFFFFFF)
val SecondaryContainer = Color(0xFFE3DFF8)
val OnSecondaryContainer = Color(0xFF1A1829)
val Tertiary = Color(0xFF79537A)
val OnTertiary = Color(0xFFFFFFFF)
val TertiaryContainer = Color(0xFFFFD6FA)
val OnTertiaryContainer = Color(0xFF300D33)
val Error = Color(0xFFB3261E)
val OnError = Color(0xFFFFFFFF)
val ErrorContainer = Color(0xFFF9DEDC)
val OnErrorContainer = Color(0xFF410E0B)
val Background = Color(0xFFFFFBFF)
val OnBackground = Color(0xFF1C1B1F)
val Surface = Color(0xFFFFFBFF)
val OnSurface = Color(0xFF1C1B1F)
val SurfaceVariant = Color(0xFFE5E0EC)
val OnSurfaceVariant = Color(0xFF48454F)
val Outline = Color(0xFF79757F)

// App-specific semantic colors
val ScoreHigh = Color(0xFF4CAF50)
val ScoreMid = Color(0xFFFF9800)
val ScoreLow = Color(0xFFF44336)

fun scoreColor(score: Float): Color = when {
    score >= 80f -> ScoreHigh
    score >= 60f -> ScoreMid
    else -> ScoreLow
}
