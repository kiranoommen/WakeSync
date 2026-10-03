package com.kiranoommen.wakesync.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kiranoommen.wakesync.model.AlarmMode
import com.kiranoommen.wakesync.model.AlarmSchedule
import com.kiranoommen.wakesync.model.SmartWakeReadiness
import com.kiranoommen.wakesync.ui.theme.Amber
import com.kiranoommen.wakesync.ui.theme.Mint
import com.kiranoommen.wakesync.ui.theme.Sunrise

@Composable
fun SmartWakeStatusCard(
    readiness: SmartWakeReadiness,
    nextSchedule: AlarmSchedule?
) {
    val smart =
        nextSchedule?.mode == AlarmMode.SMART_WAKE

    val status =
        when {
            !smart ->
                "Standard alarm scheduled"
            readiness.liveSmartWakeReady ->
                "Smart Wake armed"
            readiness.alarmReliabilityReady &&
                readiness.historyReady ->
                "Smart Wake fallback ready"
            else ->
                "Smart Wake needs attention"
        }

    val accent =
        when {
            !smart -> Amber
            readiness.liveSmartWakeReady -> Mint
            readiness.alarmReliabilityReady -> Amber
            else -> Sunrise
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(
            1.dp,
            accent.copy(alpha = 0.35f)
        ),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surface
                    .copy(alpha = 0.72f),
            contentColor =
                MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(17.dp),
            verticalArrangement =
                Arrangement.spacedBy(9.dp)
        ) {
            Text(
                text = status,
                style =
                    MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accent
            )

            if (smart) {
                StatusRow(
                    label = "Live sleep",
                    value =
                        when {
                            !readiness.sleepPermission ->
                                "Health Connect not connected"
                            !readiness.backgroundSleepPermission ->
                                "Background access needed"
                            readiness.latestStageAgeMinutes != null &&
                                readiness.latestStageAgeMinutes <= 5 ->
                                "Fresh · " +
                                    sourceName(
                                        readiness.latestStageSource
                                    )
                            else ->
                                "Will check overnight"
                        }
                )

                StatusRow(
                    label = "Wake history",
                    value =
                        readiness.historyStrength +
                            " · " +
                            readiness.usableHistoryNights +
                            " nights"
                )

                StatusRow(
                    label = "Alarm reliability",
                    value =
                        if (
                            readiness.alarmReliabilityReady
                        ) {
                            "All checks passed"
                        } else {
                            "Needs attention"
                        }
                )

                Text(
                    text =
                        "WakeSync checks live sleep during your wake window. A fitness tracker is much more useful here than phone-only sleep data because the phone usually cannot provide live sleep stages.",
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text =
                        "This alarm will ring at its exact scheduled time. Smart Wake is available on alarms where you want live sleep timing.",
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style =
                MaterialTheme.typography.bodyMedium,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style =
                MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun sourceName(packageName: String?): String =
    when {
        packageName.isNullOrBlank() ->
            "Health Connect"
        packageName.contains(
            "fitbit",
            ignoreCase = true
        ) -> "Fitbit"
        packageName.contains(
            "samsung",
            ignoreCase = true
        ) -> "Samsung Health"
        packageName.contains(
            "oura",
            ignoreCase = true
        ) -> "Oura"
        else -> "Health Connect"
    }
