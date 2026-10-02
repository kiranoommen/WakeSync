package com.kiranoommen.wakesync.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

private val AmbientMidnight = Color(0xFF0B101E)
private val AmbientViolet = Color(0xFF14142F)
private val AmbientOrange = Color(0xFFFF7A00)
private val AmbientCyan = Color(0xFF00E5FF)
private val AmbientLight = Color(0xFFF8F8FA)

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
        drawBrandWallpaperMotif(
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
    val pageShift =
        (pagePosition - 1.5f) *
            width *
            0.09f

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
                            0.96f
                        } else {
                            0.14f
                        }
                ),
                Color.Transparent
            ),
            center = Offset(
                x =
                    width *
                        0.12f +
                        pageShift,
                y =
                    height *
                        0.08f
            ),
            radius =
                maxDimension *
                    0.72f
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
                            0.09f
                        }
                ),
                Color.Transparent
            ),
            center = Offset(
                x =
                    width *
                        0.92f -
                        pageShift *
                        0.72f,
                y =
                    height *
                        0.24f
            ),
            radius =
                maxDimension *
                    0.44f
        )
    )

    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                AmbientCyan.copy(
                    alpha =
                        if (darkTheme) {
                            0.13f
                        } else {
                            0.07f
                        }
                ),
                Color.Transparent
            ),
            center = Offset(
                x =
                    width *
                        0.16f -
                        pageShift *
                        0.48f,
                y =
                    height *
                        0.86f
            ),
            radius =
                maxDimension *
                    0.54f
        )
    )
}

private fun DrawScope.drawBrandWallpaperMotif(
    pagePosition: Float,
    darkTheme: Boolean
) {
    val width = size.width
    val height = size.height
    val shift =
        (pagePosition - 1.5f) *
            width *
            0.035f
    val motifAlpha =
        if (darkTheme) {
            0.055f
        } else {
            0.040f
        }
    val horizonY =
        height *
            0.72f
    val sunRadius =
        width *
            0.24f

    drawArc(
        color =
            AmbientOrange.copy(
                alpha =
                    motifAlpha *
                        1.15f
            ),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(
            x =
                width *
                    0.60f +
                    shift -
                    sunRadius,
            y =
                horizonY -
                    sunRadius
        ),
        size = Size(
            sunRadius *
                2f,
            sunRadius *
                2f
        ),
        style = Stroke(
            width = 3.5f,
            cap = StrokeCap.Round
        )
    )

    drawLine(
        color =
            (
                if (darkTheme) {
                    Color.White
                } else {
                    Color(0xFF1C1C1E)
                }
                )
                .copy(
                    alpha =
                        motifAlpha *
                            0.85f
                ),
        start = Offset(
            x =
                width *
                    0.10f,
            y = horizonY
        ),
        end = Offset(
            x =
                width *
                    0.92f,
            y = horizonY
        ),
        strokeWidth = 2.25f,
        cap = StrokeCap.Round
    )

    val wave =
        Path().apply {
            moveTo(
                width *
                    0.02f,
                height *
                    0.82f
            )
            cubicTo(
                width *
                    0.20f,
                height *
                    0.84f,
                width *
                    0.27f,
                height *
                    0.73f,
                width *
                    0.42f +
                    shift,
                height *
                    0.74f
            )
            cubicTo(
                width *
                    0.56f +
                    shift,
                height *
                    0.74f,
                width *
                    0.61f,
                height *
                    0.87f,
                width *
                    0.77f,
                height *
                    0.86f
            )
            cubicTo(
                width *
                    0.87f,
                height *
                    0.86f,
                width *
                    0.93f,
                height *
                    0.81f,
                width *
                    1.02f,
                height *
                    0.80f
            )
        }

    drawPath(
        path = wave,
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF6366F1).copy(
                    alpha =
                        motifAlpha
                ),
                AmbientCyan.copy(
                    alpha =
                        motifAlpha *
                            1.35f
                )
            ),
            start = Offset(
                0f,
                height *
                    0.80f
            ),
            end = Offset(
                width,
                height *
                    0.80f
            )
        ),
        style = Stroke(
            width = 10f,
            cap = StrokeCap.Round
        )
    )
}
