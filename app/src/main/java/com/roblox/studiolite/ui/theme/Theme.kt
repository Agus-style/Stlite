package com.roblox.studiolite.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val StudioDarkColorScheme = darkColorScheme(
    primary         = StudioAccentBlue,
    onPrimary       = StudioText,
    secondary       = StudioAccentRed,
    onSecondary     = StudioText,
    tertiary        = StudioAccentGreen,
    background      = StudioBg,
    onBackground    = StudioText,
    surface         = StudioSurface,
    onSurface       = StudioText,
    surfaceVariant  = StudioSurface2,
    onSurfaceVariant = StudioTextDim,
    outline         = StudioDivider,
    error           = StudioAccentRed,
)

@Composable
fun StudioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StudioDarkColorScheme,
        typography  = StudioTypography,
        content     = content
    )
}
