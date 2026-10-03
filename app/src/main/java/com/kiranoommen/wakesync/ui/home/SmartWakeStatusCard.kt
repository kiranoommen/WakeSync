package com.kiranoommen.wakesync.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kiranoommen.wakesync.model.AlarmMode
import com.kiranoommen.wakesync.model.AlarmSchedule
import com.kiranoommen.wakesync.model.LiveSleepSnapshot
import com.kiranoommen.wakesync.ui.theme.Amber
import com.kiranoommen.wakesync.ui.theme.Cyan
import com.kiranoommen.wakesync.ui.theme.Lavender
import com.kiranoommen.wakesync.ui.theme.Mint
import java.time.Duration
import java.time.Instant

@Composable
fun SmartWakeStatusCard(
    nextSchedule: AlarmSchedule?,
    sleepPermission: Boolean,
    backgroundReadAvailable: Boolean,
    backgroundReadPermission: Boolean,
    exactAlarmAccess: Boolean,
    notificationsAllowed: Boolean,
    fullScreenAllowed: Boolean,
    alarmVolumePercent: Int,
    usableHistoryNights: Int,
    latestSnapshot: LiveSleepSnapshot?
) {
    val smart =
        nextSchedule?.mode == AlarmMode.SMART_WAKE

    val headline =
        when {
            nextSchedule == null ->
                "No alarm scheduled"
            !smart ->
                "Standard alarm scheduled"
            !exactAlarmAccess ||
                !notificationsAllowed ||
                !fullScreenAllowed ||
                alarmVolumePercent < 30 ->
                "Alarm reliability needs attention"
            !sleepPermission ->
                "Smart Wake needs Health Connect"
            backgroundReadAvailable &&
                !backgroundReadPermission ->
                "Background sleep access needed"
            else ->
                "Smart Wake armed"
        }

    val accent =
        when {
            nextSchedule == null ->
                MaterialTheme.colorScheme.onSurfaceVariant
            !exactAlarmAccess ||
                !notificationsAllowed ||
                !fullScreenAllowed ||
                alarmVolumePercent < 30 ||
                (smart && !sleepPermission) ||
                (smart &&
                    backgroundReadAvailable &&
                    !backgroundReadPermission) ->
                Amber
            smart -> Mint
            else -> Cyan
        }

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
                    .copy(alpha = 0.72f),
            contentColor =
                MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(17.dp),
            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "TONIGHT",
                style =
                    MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accent
            )

            Text(
                text = headline,
                style =
                    MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )

            if (smart) {
                StatusLine(
                    label = "Live sleep",
                    value =
                        liveStatus(
                            latestSnapshot,
                            sleepPermission
                        ),
                    accent =
                        if (
                            latestSnapshot?.isFresh() == true
                        ) {
                            Mint
                        } else {
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                        }
                )

                StatusLine(
                    label = "Wake history",
                    value =
                        historyStatus(
                            usableHistoryNights
                        ),
                    accent =
                        when {
                            usableHistoryNights >= 14 ->
                                Mint
                            usableHistoryNights >= 3 ->
                                Cyan
                            else ->
                                Lavender
                        }
                )

                StatusLine(
                    label = "Background checks",
                    value =
                        when {
                            !backgroundReadAvailable ->
                                "Not supported · fallback still protected"
                            backgroundReadPermission ->
                                "Allowed"
                            else ->
                                "Permission needed"
                        },
                    accent =
                        if (
                            !backgroundReadAvailable ||
                            backgroundReadPermission
                        ) {
                            Cyan
                        } else {
                            Amber
                        }
                )
            }

            StatusLine(
                label = "Exact alarm",
                value =
                    if (exactAlarmAccess) {
                        "Ready"
                    } else {
                        "Permission needed"
                    },
                accent =
                    if (exactAlarmAccess) {
                        Mint
                    } else {
                        Amber
                    }
            )

            StatusLine(
                label = "Alarm path",
                value =
                    when {
                        !notificationsAllowed ->
                            "Notifications need attention"
                        !fullScreenAllowed ->
                            "Full-screen access needed"
                        alarmVolumePercent < 30 ->
                            "Volume low · $alarmVolumePercent%"
                        else ->
                            "Ready · volume $alarmVolumePercent%"
                    },
                accent =
                    if (
                        notificationsAllowed &&
                        fullScreenAllowed &&
                        alarmVolumePercent >= 30
                    ) {
                        Mint
                    } else {
                        Amber
                    }
            )

            if (smart) {
                Text(
                    text =
                        "Live stage availability is checked overnight. WakeSync will not claim live Smart Wake unless Health Connect has fresh stage data.",
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusLine(
    label: String,
    value: String,
    accent: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = accent
        )
    }
}

private fun historyStatus(
    usableNights: Int
): String =
    when {
        usableNights >= 14 ->
            "Strong · $usableNights nights"
        usableNights >= 3 ->
            "Ready · $usableNights nights"
        usableNights > 0 ->
            "Learning · $usableNights nights"
        else ->
            "Learning"
    }

private fun liveStatus(
    snapshot: LiveSleepSnapshot?,
    sleepPermission: Boolean
): String {
    if (!sleepPermission) {
        return "Not connected"
    }

    if (snapshot == null) {
        return "Waiting for tonight"
    }

    if (snapshot.isFresh()) {
        return snapshot.stage.name
            .lowercase()
            .replaceFirstChar { it.uppercase() } +
            " · fresh"
    }

    val now = Instant.now()
    val ageMinutes =
        if (snapshot.stageEnd.isAfter(now)) {
            0L
        } else {
            Duration.between(
                snapshot.stageEnd,
                now
            ).toMinutes().coerceAtLeast(0L)
        }

    return if (ageMinutes < 120L) {
        "Delayed · $ageMinutes min old"
    } else {
        "Waiting for tonight"
    }
}
