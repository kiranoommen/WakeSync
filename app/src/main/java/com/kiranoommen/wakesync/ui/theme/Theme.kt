package com.kiranoommen.wakesync.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = Amber,
    secondary = Lavender,
    background = Midnight,
    surface = MidnightSurface,
    surfaceVariant = MidnightElevated,
    onPrimary = Midnight,
    onSecondary = Pearl,
    onBackground = Pearl,
    onSurface = Pearl,
    onSurfaceVariant = PearlMuted
)

private val LightColors = lightColorScheme(
    primary = Amber,
    secondary = Indigo,
    background = DayBackground,
    surface = DaySurface,
    surfaceVariant = DayElevated,
    onPrimary = DayText,
    onSecondary = Pearl,
    onBackground = DayText,
    onSurface = DayText,
    onSurfaceVariant = DayMuted
)

@Composable
fun WakeSyncTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
