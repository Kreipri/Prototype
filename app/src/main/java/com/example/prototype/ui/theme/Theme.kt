package com.example.prototype.ui.theme

// --- COMPOSE UI ---
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*

/**
 * Global UI Constants: Central source of truth for the app's design system.
 * Replaces old XML values from colors.xml and activity layouts.
 */
object AppTheme {
    // Colors (Refined from original XML hex values)
    val Background = Color(0xFFF0F7FF) // Light Blue
    val Surface = Color(0xFFFFFFFF)    // Pure White
    val Primary = Color(0xFF2196F3)    // Parent Blue
    val Success = Color(0xFF4CAF50)    // Child Green
    val Warning = Color(0xFFFFA000)    // Medium Severity
    val Error = Color(0xFFD32F2F)      // High Severity
    val Border = Color(0xFFD9D9D9)     // Default Stroke

    // Spacing & Sizing
    val PaddingDefault = 20.dp
    val ColumnGap = 16.dp
    val CardCorner = 24.dp
    val BadgeCorner = 8.dp
}