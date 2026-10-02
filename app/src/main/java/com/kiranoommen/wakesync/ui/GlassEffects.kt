package com.kiranoommen.wakesync.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance

fun Modifier.wakeGlassBlur(
    radiusPx: Float = 25f
): Modifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        graphicsLayer {
            renderEffect =
                BlurEffect(
                    radiusX = radiusPx,
                    radiusY = radiusPx,
                    edgeTreatment =
                        TileMode.Mirror
                )
        }
    } else {
        this
    }

@Composable
fun WakeGlassBackdrop(
    modifier: Modifier = Modifier
) {
    val isDark =
        MaterialTheme.colorScheme.background
            .luminance() < 0.5f
    val base =
        if (isDark) {
            Color(0x33161B26)
        } else {
            Color.White.copy(alpha = 0.60f)
        }

    Box(
        modifier = modifier
            .wakeGlassBlur()
            .background(
                Brush.linearGradient(
                    listOf(
                        base,
                        Color.White.copy(
                            alpha =
                                if (isDark) {
                                    0.035f
                                } else {
                                    0.20f
                                }
                        ),
                        base.copy(
                            alpha =
                                if (isDark) {
                                    0.12f
                                } else {
                                    0.48f
                                }
                        )
                    )
                )
            )
    )
}
