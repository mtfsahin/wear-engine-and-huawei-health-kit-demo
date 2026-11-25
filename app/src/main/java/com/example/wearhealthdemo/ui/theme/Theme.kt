package com.example.wearhealthdemo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LoginFreeColors = darkColorScheme(
    primary = Ink,
    onPrimary = Surface0,
    secondary = Brand,
    secondaryContainer = LineStrong,
    onSecondaryContainer = Ink,
    background = Surface0,
    onBackground = Ink,
    surface = Surface0,
    onSurface = Ink,
    surfaceContainer = Surface1,
    surfaceContainerHigh = Surface1,
    surfaceContainerHighest = Surface1,
    surfaceVariant = Surface1,
    onSurfaceVariant = Ink2,
    outline = LineStrong,
    outlineVariant = Line,
    error = ErrorRed,
)

@Composable
fun LoginFreeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LoginFreeColors,
        typography = Typography,
        content = content
    )
}
