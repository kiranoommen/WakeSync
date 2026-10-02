package com.kiranoommen.wakesync.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

private val AmbientMidnight = Color(0xFF0B0E14)
private val AmbientViolet = Color(0xFF1A102F)
private val AmbientOrange = Color(0xFFFF7A00)
private val AmbientCyan = Color(0xFF00E5FF)
private val AmbientLight = Color(0xFFF6F7FB)

@Composable
fun AmbientGradientBackground(
    pagePosition: Float,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawAmbientMesh(
            pagePosition = pagePosition,
            darkTheme = darkTheme
        )
    }
}

private fun DrawScope.drawAmbientMesh(
    pagePosition: Float,
    darkTheme: Boolean
) {
    val width = size.width
    val height = size.height
    val maxDimension = maxOf(width, height)
    val pageShift = (pagePosition - 1.5f) * width * 0.09f

    drawRect(
        color =
            if (darkTheme) {
                AmbientMidnight
            } else {
                AmbientLight
            }
    )

    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                AmbientViolet.copy(
                    alpha =
                        if (darkTheme) {
                            0.92f
                        } else {
                            0.13f
                        }
                ),
                Color.Transparent
            ),
            center = Offset(
                x = width * 0.12f + pageShift,
                y = height * 0.08f
            ),
            radius = maxDimension * 0.72f
        )
    )

    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                AmbientOrange.copy(
                    alpha =
                        if (darkTheme) {
                            0.14f
                        } else {
                            0.10f
                        }
                ),
                Color.Transparent
            ),
            center = Offset(
                x = width * 0.92f - pageShift * 0.72f,
                y = height * 0.24f
            ),
            radius = maxDimension * 0.44f
        )
    )

    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                AmbientCyan.copy(
                    alpha =
                        if (darkTheme) {
                            0.12f
                        } else {
                            0.08f
                        }
                ),
                Color.Transparent
            ),
            center = Offset(
                x = width * 0.16f - pageShift * 0.48f,
                y = height * 0.86f
            ),
            radius = maxDimension * 0.54f
        )
    )

    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF4F46E5).copy(
                    alpha =
                        if (darkTheme) {
                            0.10f
                        } else {
                            0.055f
                        }
                ),
                Color.Transparent
            ),
            center = Offset(
                x = width * 0.70f + pageShift * 0.34f,
                y = height * 0.72f
            ),
            radius = maxDimension * 0.50f
        )
    )
}
