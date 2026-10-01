package com.kiranoommen.wakesync.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val DarkColors = darkColorScheme(
    primary = Sunrise,
    onPrimary = DayText,
    primaryContainer = Color(0xFF3C211D),
    onPrimaryContainer = Pearl,

    secondary = Lavender,
    onSecondary = Pearl,
    secondaryContainer = Color(0xFF251B45),
    onSecondaryContainer = Pearl,

    tertiary = Cyan,
    onTertiary = Midnight,
    tertiaryContainer = Color(0xFF0B3340),
    onTertiaryContainer = Pearl,

    background = Midnight,
    onBackground = Pearl,

    surface = MidnightSurface,
    onSurface = Pearl,
    surfaceVariant = MidnightElevated,
    onSurfaceVariant = PearlMuted,

    outline = Color(0xFF475569),
    outlineVariant = GlassBorder,

    inverseSurface = Pearl,
    inverseOnSurface = Midnight,
    inversePrimary = Indigo,

    error = Color(0xFFFF7B88),
    onError = Midnight,
    errorContainer = Color(0xFF4A2028),
    onErrorContainer = Color(0xFFFFE4E7)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFE45F42),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE7DE),
    onPrimaryContainer = Color(0xFF3A160F),

    secondary = Color(0xFF6D55D9),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF25145D),

    tertiary = Color(0xFF087C8F),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD8F5FA),
    onTertiaryContainer = Color(0xFF052D33),

    background = DayBackground,
    onBackground = DayText,

    surface = DaySurface,
    onSurface = DayText,
    surfaceVariant = DayElevated,
    onSurfaceVariant = DayMuted,

    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFDCE1EA),

    inverseSurface = Color(0xFF111827),
    inverseOnSurface = Color.White,
    inversePrimary = Sunrise,

    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private val WakeSyncTypography = Typography(
    headlineLarge = Typography().headlineLarge.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = Typography().headlineMedium.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    ),
    titleLarge = Typography().titleLarge.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    ),
    titleMedium = Typography().titleMedium.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp
    ),
    bodyLarge = Typography().bodyLarge.copy(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp
    ),
    bodyMedium = Typography().bodyMedium.copy(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp
    ),
    bodySmall = Typography().bodySmall.copy(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp
    ),
    labelLarge = Typography().labelLarge.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold
    )
)

@Composable
fun WakeSyncTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = WakeSyncTypography,
        content = content
    )
}
