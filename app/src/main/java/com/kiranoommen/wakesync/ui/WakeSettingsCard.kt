package com.kiranoommen.wakesync.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kiranoommen.wakesync.model.WakePreferences
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun WakeSettingsCard(
    preferences: WakePreferences,
    sleepPermissionGranted: Boolean,
    backgroundReadAvailable: Boolean,
    backgroundReadGranted: Boolean,
    exactAlarmAccess: Boolean,
    notificationsAllowed: Boolean,
    fullScreenAlarmAccess: Boolean,
    onPreferencesChanged: (WakePreferences) -> Unit,
    onRequestHealthPermissions: () -> Unit,
    onRequestExactAlarmAccess: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestFullScreenAlarmAccess: () -> Unit,
    onAlarmEnabledChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val rangeValid = preferences.hasValidRange()
    val setupReady = sleepPermissionGranted &&
        backgroundReadAvailable &&
        backgroundReadGranted &&
        exactAlarmAccess &&
        notificationsAllowed &&
        fullScreenAlarmAccess &&
        rangeValid

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface.copy(alpha = 0.86f)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Smart wake",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                modifier = Modifier.padding(top = 5.dp),
                text = "WakeSync starts checking live sleep 45 minutes before your range, wakes you at a favorable point inside it, and always alarms by the end time.",
                color = colors.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                onPreferencesChanged(
                                    preferences.copy(
                                        earliestHour = hour,
                                        earliestMinute = minute
                                    )
                                )
                            },
                            preferences.earliestHour,
                            preferences.earliestMinute,
                            false
                        ).show()
                    }
                ) {
                    Text("From ${formatTime(preferences.earliest)}")
                }

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                onPreferencesChanged(
                                    preferences.copy(
                                        latestHour = hour,
                                        latestMinute = minute
                                    )
                                )
                            },
                            preferences.latestHour,
                            preferences.latestMinute,
                            false
                        ).show()
                    }
                ) {
                    Text("By ${formatTime(preferences.latest)}")
                }
            }

            if (!rangeValid) {
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = "The end of the wake range must be later than the start.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            RequirementRow(
                label = "Sleep + background Health Connect access",
                ready = sleepPermissionGranted &&
                    backgroundReadAvailable &&
                    backgroundReadGranted,
                unavailable = !backgroundReadAvailable,
                actionLabel = "Allow",
                onAction = onRequestHealthPermissions
            )

            RequirementRow(
                label = "Exact alarms",
                ready = exactAlarmAccess,
                actionLabel = "Allow",
                onAction = onRequestExactAlarmAccess
            )

            RequirementRow(
                label = "Alarm notifications",
                ready = notificationsAllowed,
                actionLabel = "Allow",
                onAction = onRequestNotifications
            )

            RequirementRow(
                label = "Full-screen alarm",
                ready = fullScreenAlarmAccess,
                actionLabel = "Allow",
                onAction = onRequestFullScreenAlarmAccess
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (preferences.enabled) {
                            "Smart alarm on"
                        } else {
                            "Smart alarm off"
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        modifier = Modifier.padding(top = 2.dp),
                        text = if (setupReady) {
                            "Ready for live overnight monitoring."
                        } else {
                            "Complete the setup items above to turn it on."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }

                Switch(
                    checked = preferences.enabled,
                    enabled = setupReady || preferences.enabled,
                    onCheckedChange = onAlarmEnabledChanged
                )
            }
        }
    }
}

@Composable
private fun RequirementRow(
    label: String,
    ready: Boolean,
    actionLabel: String,
    unavailable: Boolean = false,
    onAction: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                modifier = Modifier.padding(top = 1.dp),
                text = when {
                    unavailable -> "Not supported by this Health Connect version"
                    ready -> "Ready"
                    else -> "Required"
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }

        if (!ready && !unavailable) {
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

private val wakeTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

private fun formatTime(time: LocalTime): String = time.format(wakeTimeFormatter)
