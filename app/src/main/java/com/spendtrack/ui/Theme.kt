package com.spendtrack.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Purple = Color(0xFF6C5CE7)
val PurpleLight = Color(0xFFA29BFE)
val Mint = Color(0xFF00B894)
val MintLight = Color(0xFF55EFC4)
val Coral = Color(0xFFFF7675)
val Amber = Color(0xFFF0A500)

private val LightColors = lightColorScheme(
    primary = Purple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E4FF),
    onPrimaryContainer = Color(0xFF2D2570),
    secondary = Mint,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5F5EC),
    onSecondaryContainer = Color(0xFF00513F),
    background = Color(0xFFF7F6FF),
    onBackground = Color(0xFF1E1B2E),
    surface = Color.White,
    onSurface = Color(0xFF1E1B2E),
    surfaceVariant = Color(0xFFEFEDFB),
    onSurfaceVariant = Color(0xFF6B6880),
    error = Color(0xFFE17055),
    outline = Color(0xFFD9D6EE)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9B8CFF),
    onPrimary = Color(0xFF1A1440),
    primaryContainer = Color(0xFF3A3170),
    onPrimaryContainer = Color(0xFFE8E4FF),
    secondary = MintLight,
    onSecondary = Color(0xFF00382B),
    secondaryContainer = Color(0xFF0B4A3B),
    onSecondaryContainer = Color(0xFFD5F5EC),
    background = Color(0xFF121218),
    onBackground = Color(0xFFEDEBFA),
    surface = Color(0xFF1D1C28),
    onSurface = Color(0xFFEDEBFA),
    surfaceVariant = Color(0xFF2A2938),
    onSurfaceVariant = Color(0xFFA9A6BF),
    error = Color(0xFFFF8A75),
    outline = Color(0xFF3A3950)
)

private val AppTypography = Typography(
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    bodySmall = TextStyle(fontSize = 12.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SpendTrackTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
