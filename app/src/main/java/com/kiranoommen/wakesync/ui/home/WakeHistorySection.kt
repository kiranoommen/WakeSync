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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kiranoommen.wakesync.model.WakeEvent
import com.kiranoommen.wakesync.model.WakeFeedback
import com.kiranoommen.wakesync.ui.theme.Lavender
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun WakeHistorySection(
    events: List<WakeEvent>,
    onFeedback: (String, WakeFeedback) -> Unit,
    onClear: () -> Unit
) {
    val completed =
        events.filter {
            it.completedAtMillis != null
        }

    if (completed.isEmpty()) return

    Column(
        verticalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {
            Text(
                text = "Wake history",
                style =
                    MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )

            TextButton(onClick = onClear) {
                Text("Clear")
            }
        }

        completed.take(3).forEach { event ->
            WakeHistoryCard(
                event = event,
                onFeedback = onFeedback
            )
        }
    }
}

@Composable
private fun WakeHistoryCard(
    event: WakeEvent,
    onFeedback: (String, WakeFeedback) -> Unit
) {
    val wokeAt =
        Instant.ofEpochMilli(
            event.completedAtMillis
                ?: event.firedAtMillis
        ).atZone(ZoneId.systemDefault())

    val fired =
        Instant.ofEpochMilli(
            event.firedAtMillis
        ).atZone(ZoneId.systemDefault())
    val deadline =
        if (event.deadlineMillis > 0L) {
            Instant.ofEpochMilli(
                event.deadlineMillis
            ).atZone(ZoneId.systemDefault())
        } else {
            null
        }
    val minutesEarly =
        deadline?.let {
            java.time.Duration
                .between(fired, it)
                .toMinutes()
        }?.takeIf { it > 0 }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.dp,
            Lavender.copy(alpha = 0.20f)
        ),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surface
                    .copy(alpha = 0.68f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(7.dp)
        ) {
            Text(
                text =
                    wokeAt.format(
                        DateTimeFormatter.ofPattern(
                            "EEE · h:mm a"
                        )
                    ),
                style =
                    MaterialTheme.typography.labelLarge,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = reasonTitle(event),
                style =
                    MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = reasonDetail(event),
                style =
                    MaterialTheme.typography.bodySmall,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (deadline != null) {
                Text(
                    text =
                        "Must be awake by: " +
                            deadline.format(
                                DateTimeFormatter.ofPattern(
                                    "h:mm a"
                                )
                            ),
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (minutesEarly != null) {
                Text(
                    text =
                        minutesEarly.toString() +
                            " min before must-be-awake time",
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                modifier =
                    Modifier.padding(top = 4.dp),
                text = "How did this wake feel?",
                style =
                    MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {
                FeedbackChip(
                    modifier = Modifier.weight(1f),
                    label = "Too early",
                    selected =
                        event.feedback ==
                            WakeFeedback.TOO_EARLY,
                    onClick = {
                        onFeedback(
                            event.id,
                            WakeFeedback.TOO_EARLY
                        )
                    }
                )
                FeedbackChip(
                    modifier = Modifier.weight(1f),
                    label = "Good",
                    selected =
                        event.feedback ==
                            WakeFeedback.GOOD,
                    onClick = {
                        onFeedback(
                            event.id,
                            WakeFeedback.GOOD
                        )
                    }
                )
                FeedbackChip(
                    modifier = Modifier.weight(1f),
                    label = "Too late",
                    selected =
                        event.feedback ==
                            WakeFeedback.TOO_LATE,
                    onClick = {
                        onFeedback(
                            event.id,
                            WakeFeedback.TOO_LATE
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun FeedbackChip(
    modifier: Modifier,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        modifier = modifier,
        selected = selected,
        onClick = onClick,
        label = {
            Text(label)
        }
    )
}

private fun reasonTitle(event: WakeEvent): String =
    when {
        event.kind == "live" ->
            "Live Smart Wake"
        event.kind == "historical" ->
            "History fallback"
        event.kind == "deadline" ->
            if (
                event.reason == "Standard alarm"
            ) {
                "Standard alarm"
            } else {
                "Must-be-awake alarm"
            }
        event.kind.startsWith("backup_") ->
            "Backup alarm"
        else ->
            "Wake alarm"
    }

private fun reasonDetail(event: WakeEvent): String =
    when {
        event.kind == "live" -> {
            val stage =
                event.reason
                    .substringAfter(":")
                    .trim()
                    .replaceFirstChar {
                        it.uppercase()
                    }
            val freshness =
                event.dataAgeMinutes?.let {
                    " · data " +
                        it +
                        " min old"
                }.orEmpty()
            val source =
                sourceName(
                    event.sourcePackage
                )
            "$stage sleep detected · $source$freshness"
        }

        event.kind == "historical" ->
            "No favorable fresh live stage fired first, so WakeSync used your recent wake pattern."

        event.kind == "deadline" &&
            event.reason != "Standard alarm" ->
            "Smart Wake did not end the sequence earlier, so the protected latest wake time rang."

        event.kind.startsWith("backup_") ->
            "A backup alarm rang because the earlier alarm was dismissed without ending the wake sequence."

        else ->
            event.reason
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
