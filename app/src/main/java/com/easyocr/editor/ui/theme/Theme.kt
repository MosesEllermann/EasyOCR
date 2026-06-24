package com.easyocr.editor.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    secondary = Color(0xFFB45309),
    surface = Color(0xFFFFFBFE),
    surfaceContainer = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFFE2E8F0),
    background = Color(0xFFF8FAFC),
    outline = Color(0xFF64748B),
)

@Composable
fun EasyOcrTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}
