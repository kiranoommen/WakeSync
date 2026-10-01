package com.kiranoommen.wakesync.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Amber,
    onPrimary = Midnight,
    primaryContainer = Color(0xFF3A2C20),
    onPrimaryContainer = AmberBright,

    secondary = Lavender,
    onSecondary = Midnight,
    secondaryContainer = Color(0xFF272447),
    onSecondaryContainer = Pearl,

    tertiary = Indigo,
    onTertiary = Pearl,
    tertiaryContainer = Color(0xFF20264C),
    onTertiaryContainer = Pearl,

    background = Midnight,
    onBackground = Pearl,

    surface = MidnightSurface,
    onSurface = Pearl,
    surfaceVariant = MidnightElevated,
    onSurfaceVariant = PearlMuted,

    outline = Color(0xFF59627F),
    outlineVariant = Color(0xFF303852),

    inverseSurface = Pearl,
    inverseOnSurface = Midnight,
    inversePrimary = Indigo,

    error = Color(0xFFFF7D8B),
    onError = Midnight,
    errorContainer = Color(0xFF4A2028),
    onErrorContainer = Color(0xFFFFD9DE)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFB66A00),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE2B8),
    onPrimaryContainer = Color(0xFF3E2600),

    secondary = Indigo,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9E5FF),
    onSecondaryContainer = Color(0xFF251D64),

    tertiary = Color(0xFF6B56D9),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFECE6FF),
    onTertiaryContainer = Color(0xFF211354),

    background = DayBackground,
    onBackground = DayText,

    surface = DaySurface,
    onSurface = DayText,
    surfaceVariant = DayElevated,
    onSurfaceVariant = DayMuted,

    outline = Color(0xFF838AA0),
    outlineVariant = Color(0xFFD7DBE8),

    inverseSurface = Color(0xFF23283A),
    inverseOnSurface = Pearl,
    inversePrimary = AmberBright,

    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
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
