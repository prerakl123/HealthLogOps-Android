package com.example.healthlogops.ui.theme

import androidx.compose.ui.graphics.Color

// Primary theme colors (Teal/Cyan)
val TealPrimary = Color(0xFF009688)
val TealLight = Color(0xFF4DB6AC)
val TealDark = Color(0xFF00796B)

// Accent colors (Amber)
val AmberAccent = Color(0xFFFFC107)

// Category colors - matching Kivy app
val CategoryColors = mapOf(
    "Strength Training" to Color(0xFFE91E63), // Pink
    "Cardio" to Color(0xFFFF5722), // Deep Orange
    "Meal" to Color(0xFF4CAF50), // Green
    "Sleep" to Color(0xFF3F51B5), // Indigo
    "Water Intake" to Color(0xFF2196F3), // Blue
    "Weight Log" to Color(0xFF9C27B0), // Purple
    "Daily Steps" to Color(0xFFFF9800), // Orange
    "Bowel Movement" to Color(0xFF795548), // Brown
    "default" to Color(0xFF607D8B) // Blue Grey
)

// Light background colors for pills
val CategoryColorsLight = mapOf(
    "Strength Training" to Color(0xFFFCE4EC), // Pink light
    "Cardio" to Color(0xFFFFE0B2), // Deep Orange light
    "Meal" to Color(0xFFE8F5E9), // Green light
    "Sleep" to Color(0xFFE8EAF6), // Indigo light
    "Water Intake" to Color(0xFFE3F2FD), // Blue light
    "Weight Log" to Color(0xFFF3E5F5), // Purple light
    "Daily Steps" to Color(0xFFFFF3E0), // Orange light
    "Bowel Movement" to Color(0xFFEFEBE9), // Brown light
    "default" to Color(0xFFECEFF1) // Blue Grey light
)

// Background colors
val BackgroundLight = Color(0xFFF6F7F8)
val BackgroundDark = Color(0xFF1E1E20)

// Card colors
val CardLight = Color(0xFFFFFFFF)
val CardDark = Color(0xFF2E2E32)

// Text colors
val TextPrimaryLight = Color(0xFF262626)
val TextPrimaryDark = Color(0xFFE6E6EB)
val TextSecondaryLight = Color(0xFF8C8C99)
val TextSecondaryDark = Color(0xFFA6A6B3)