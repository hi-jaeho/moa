
package com.example.moa.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.moa.data.settings.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    error = Color(0xFFDC2626)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF93C5FD),
    onPrimary = Color(0xFF172554),
    primaryContainer = Color(0xFF1E3A8A),
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF1F5F9),
    error = Color(0xFFFCA5A5)
)

@Composable
fun MoaTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
