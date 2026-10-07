package com.kiranoommen.wakesync.ui.settings

import android.media.AudioManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kiranoommen.wakesync.model.SmartWakeReadiness
import com.kiranoommen.wakesync.ui.theme.Amber
import com.kiranoommen.wakesync.ui.theme.Mint
import com.kiranoommen.wakesync.ui.theme.Sunrise

@Composable
fun AlarmReliabilityCard(
    readiness: SmartWakeReadiness,
    alarmVolumePercent: Int,
    onGrantExactAlarmAccess: () -> Unit,
    onRequestBackgroundSmartWakeAccess: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenFullScreenSettings: () -> Unit
) {
    val context = LocalContext.current
    val audioManager =
        remember(context) {
            context.getSystemService(AudioManager::class.java)
        }
    var volumePercent by remember(alarmVolumePercent) {
        mutableFloatStateOf(alarmVolumePercent.toFloat())
    }
    val ready =
        readiness.alarmReliabilityReady &&
            volumePercent > 0f
    val accent =
        if (ready) Mint else Sunrise

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(
            1.dp,
            accent.copy(alpha = 0.34f)
        ),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surface
                    .copy(alpha = 0.70f)
        )
    ) {
        Column(
            modifier = Modifier.padding(17.dp),
            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Alarm reliability",
                style =
                    MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )

            ReliabilityRow(
                label = "Exact alarms",
                ready = readiness.exactAlarmAccess
            )
            ReliabilityRow(
                label = "Notifications",
                ready = readiness.notificationsAllowed
            )
            ReliabilityRow(
                label = "Full-screen alarms",
                ready = readiness.fullScreenAllowed
            )
            if (readiness.backgroundFeatureAvailable) {
                ReliabilityRow(
                    label = "Background Smart Wake",
                    ready =
                        readiness.backgroundSleepPermission
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Background Smart Wake",
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Not supported",
                        fontWeight = FontWeight.Bold,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Alarm volume",
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = volumePercent.toInt().toString() + "%",
                    fontWeight = FontWeight.Bold,
                    color =
                        if (volumePercent > 0f) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            Sunrise
                        }
                )
            }

            Slider(
                modifier = Modifier.fillMaxWidth(),
                value = volumePercent,
                onValueChange = { requested ->
                    val max =
                        audioManager
                            .getStreamMaxVolume(
                                AudioManager.STREAM_ALARM
                            )
                            .coerceAtLeast(1)
                    val level =
                        ((requested / 100f) * max)
                            .toInt()
                            .coerceIn(0, max)

                    audioManager.setStreamVolume(
                        AudioManager.STREAM_ALARM,
                        level,
                        0
                    )

                    volumePercent =
                        (
                            audioManager
                                .getStreamVolume(
                                    AudioManager.STREAM_ALARM
                                )
                                .toFloat() /
                                max.toFloat() *
                                100f
                            ).coerceIn(0f, 100f)
                },
                valueRange = 0f..100f
            )

            if (!readiness.exactAlarmAccess) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onGrantExactAlarmAccess,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Amber,
                        contentColor =
                            MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = "Fix exact alarm access",
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            if (!readiness.notificationsAllowed) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenNotificationSettings,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text("Fix notification access")
                }
            }

            if (!readiness.fullScreenAllowed) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenFullScreenSettings,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text("Fix full-screen alarm access")
                }
            }

            if (
                readiness.sleepPermission &&
                readiness.backgroundFeatureAvailable &&
                !readiness.backgroundSleepPermission
            ) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick =
                        onRequestBackgroundSmartWakeAccess,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text("Fix background Smart Wake access")
                }
            }

            Text(
                text = "Alarm volume is controlled here in WakeSync and uses Android’s alarm audio stream.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReliabilityRow(
    label: String,
    ready: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text =
                if (ready) {
                    "✓ Ready"
                } else {
                    "Needs attention"
                },
            fontWeight = FontWeight.Bold,
            color =
                if (ready) Mint else Sunrise
        )
    }
}
