package com.kiranoommen.wakesync.ui

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.kiranoommen.wakesync.ui.theme.Cyan
import com.kiranoommen.wakesync.ui.theme.Indigo
import com.kiranoommen.wakesync.ui.theme.Sunrise

@Composable
fun WakeSyncBrandMark(
    modifier: Modifier = Modifier
) {
    val horizon =
        MaterialTheme.colorScheme.onSurface
            .copy(alpha = 0.92f)

    Canvas(modifier = modifier) {
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF14142F),
                    Color(0xFF0B101E)
                )
            ),
            cornerRadius =
                CornerRadius(
                    x = size.minDimension * 0.24f,
                    y = size.minDimension * 0.24f
                )
        )
        drawRoundRect(
            color =
                Color.White.copy(
                    alpha = 0.16f
                ),
            cornerRadius =
                CornerRadius(
                    x = size.minDimension * 0.24f,
                    y = size.minDimension * 0.24f
                ),
            style = Stroke(
                width =
                    1.25.dp.toPx()
            )
        )

        val horizonY =
            size.height * 0.53f
        val sunRadius =
            size.minDimension * 0.30f

        drawArc(
            color = Sunrise,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(
                x = size.width * 0.50f -
                    sunRadius,
                y = horizonY -
                    sunRadius
            ),
            size = Size(
                sunRadius * 2f,
                sunRadius * 2f
            ),
            style = Stroke(
                width =
                    4.dp.toPx(),
                cap =
                    StrokeCap.Round
            )
        )

        drawLine(
            color = horizon,
            start = Offset(
                x = size.width * 0.08f,
                y = horizonY
            ),
            end = Offset(
                x = size.width * 0.92f,
                y = horizonY
            ),
            strokeWidth =
                3.dp.toPx(),
            cap =
                StrokeCap.Round
        )

        val wave =
            Path().apply {
                moveTo(
                    size.width * 0.05f,
                    size.height * 0.68f
                )
                cubicTo(
                    size.width * 0.22f,
                    size.height * 0.72f,
                    size.width * 0.30f,
                    size.height * 0.48f,
                    size.width * 0.43f,
                    size.height * 0.47f
                )
                cubicTo(
                    size.width * 0.57f,
                    size.height * 0.46f,
                    size.width * 0.62f,
                    size.height * 0.77f,
                    size.width * 0.78f,
                    size.height * 0.76f
                )
                cubicTo(
                    size.width * 0.86f,
                    size.height * 0.76f,
                    size.width * 0.91f,
                    size.height * 0.68f,
                    size.width * 0.97f,
                    size.height * 0.64f
                )
                lineTo(
                    size.width * 0.97f,
                    size.height * 0.92f
                )
                lineTo(
                    size.width * 0.05f,
                    size.height * 0.92f
                )
                close()
            }

        drawPath(
            path = wave,
            brush = Brush.linearGradient(
                colors =
                    listOf(
                        Indigo,
                        Cyan
                    ),
                start = Offset(
                    0f,
                    size.height * 0.70f
                ),
                end = Offset(
                    size.width,
                    size.height * 0.70f
                )
            )
        )
    }
}
