package com.kiranoommen.wakesync.ui

import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer

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
