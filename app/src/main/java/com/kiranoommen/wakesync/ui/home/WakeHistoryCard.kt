package com.kiranoommen.wakesync.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kiranoommen.wakesync.data.WakeEventStore
import com.kiranoommen.wakesync.model.WakeEvent
import com.kiranoommen.wakesync.ui.theme.Amber
import com.kiranoommen.wakesync.ui.theme.Cyan
import com.kiranoommen.wakesync.ui.theme.Lavender
import com.kiranoommen.wakesync.ui.theme.Mint
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun WakeHistoryCard(
    events: List<WakeEvent>,
    onFeedback: (
        eventId: String,
        feedback: String
    ) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
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
                Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Wake History",
                style =
                    MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text =
                    "Why WakeSync actually rang — stored only on this device.",
                style =
                    MaterialTheme.typography.bodySmall,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )

            if (events.isEmpty()) {
                Text(
                    text =
                        "Your next wake will appear here with the trigger, timing, and live-data freshness when available.",
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            } else {
                events.take(5).forEachIndexed {
                        index,
                        event ->
                    WakeEventRow(
                        event = event,
                        askFeedback = index == 0 &&
                            !event.isBackup,
                        onFeedback = onFeedback
                    )
                }
            }
        }
    }
}

@Composable
private fun WakeEventRow(
    event: WakeEvent,
    askFeedback: Boolean,
    onFeedback: (
        eventId: String,
        feedback: String
    ) -> Unit
) {
    Column(
        verticalArrangement =
            Arrangement.spacedBy(5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = event.label,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = firedTime(event),
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }

            Text(
                text = kindLabel(event),
                style =
                    MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = kindColor(event)
            )
        }

        Text(
            text = detail(event),
            style = MaterialTheme.typography.bodySmall,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
        )

        if (askFeedback) {
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = "How did that wake feel?",
                style =
                    MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {
                FeedbackChip(
                    label = "Too early",
                    value =
                        WakeEventStore.FEEDBACK_TOO_EARLY,
                    event = event,
                    onFeedback = onFeedback
                )
                FeedbackChip(
                    label = "Good",
                    value =
                        WakeEventStore.FEEDBACK_GOOD,
                    event = event,
                    onFeedback = onFeedback
                )
                FeedbackChip(
                    label = "Too late",
                    value =
                        WakeEventStore.FEEDBACK_TOO_LATE,
                    event = event,
                    onFeedback = onFeedback
                )
            }
        }
    }
}

@Composable
private fun FeedbackChip(
    label: String,
    value: String,
    event: WakeEvent,
    onFeedback: (
        eventId: String,
        feedback: String
    ) -> Unit
) {
    FilterChip(
        selected = event.feedback == value,
        onClick = {
            onFeedback(event.id, value)
        },
        label = { Text(label) }
    )
}

private fun firedTime(
    event: WakeEvent
): String {
    val value =
        Instant.ofEpochMilli(event.firedAtMillis)
            .atZone(ZoneId.systemDefault())

    return value.format(
        DateTimeFormatter.ofPattern(
            "EEE, MMM d · h:mm a"
        )
    )
}

private fun kindLabel(
    event: WakeEvent
): String =
    when {
        event.kind == "live" ->
            "LIVE SMART WAKE"
        event.kind == "historical" ->
            "HISTORY FALLBACK"
        event.kind == "deadline" &&
            event.mode.name == "STANDARD" ->
            "STANDARD"
        event.kind == "deadline" ->
            "MUST-BE-AWAKE"
        event.isBackup ->
            "BACKUP ALARM"
        else ->
            "ALARM"
    }

@Composable
private fun kindColor(
    event: WakeEvent
) =
    when {
        event.kind == "live" -> Mint
        event.kind == "historical" -> Cyan
        event.isBackup -> Amber
        else -> Lavender
    }

private fun detail(
    event: WakeEvent
): String {
    val pieces = mutableListOf<String>()

    if (
        event.sleepStage != null &&
        event.kind == "live"
    ) {
        pieces +=
            event.sleepStage.lowercase()
                .replaceFirstChar { it.uppercase() } +
                " sleep"
    }

    event.dataAgeSeconds?.let { seconds ->
        pieces +=
            if (seconds < 60) {
                "Health Connect data <1 min old"
            } else {
                "Health Connect data " +
                    (seconds / 60L) +
                    " min old"
            }
    }

    if (
        event.deadlineMillis > 0L &&
        event.firedAtMillis <
            event.deadlineMillis
    ) {
        pieces +=
            event.minutesBeforeDeadline
                .toString() +
                " min before must-be-awake time"
    }

    if (pieces.isEmpty()) {
        pieces +=
            when {
                event.isBackup ->
                    "Safety alarm after your must-be-awake time"
                event.kind == "deadline" ->
                    "Reached your must-be-awake time"
                else ->
                    event.reason
            }
    }

    return pieces.joinToString(" · ")
}
