package com.kiranoommen.wakesync.ui.alarms

import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.kiranoommen.wakesync.model.AlarmMode
import com.kiranoommen.wakesync.model.AlarmSchedule
import com.kiranoommen.wakesync.ui.theme.Amber
import java.time.DayOfWeek
import java.time.LocalDate

private enum class SchedulePreset {
    TOMORROW,
    EVERY_DAY,
    WEEKDAYS,
    CUSTOM
}

@Composable
internal fun AlarmEditorDialog(
    schedule: AlarmSchedule,
    isNew: Boolean,
    maxSmartWindowMinutes: Int,
    onDismiss: () -> Unit,
    onSave: (AlarmSchedule) -> Unit,
    onDelete: (AlarmSchedule) -> Unit
) {
    val context = LocalContext.current
    var showMore by remember(schedule.id) {
        mutableStateOf(false)
    }

    var draft by remember(
        schedule.id,
        maxSmartWindowMinutes
    ) {
        val cappedWindow =
            schedule.smartWindowMinutes
                .coerceIn(10, maxSmartWindowMinutes)

        mutableStateOf(
            schedule.copy(
                smartWindowMinutes = cappedWindow,
                smartOffsetMinutes =
                    schedule.smartOffsetMinutes
                        .coerceAtMost(cappedWindow)
            )
        )
    }

    val preset = when {
        draft.oneTimeDate != null ->
            SchedulePreset.TOMORROW

        draft.days == AlarmSchedule.EVERY_DAY ->
            SchedulePreset.EVERY_DAY

        draft.days == AlarmSchedule.WEEKDAYS ->
            SchedulePreset.WEEKDAYS

        else ->
            SchedulePreset.CUSTOM
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isNew) {
                    "New wake schedule"
                } else {
                    "Edit wake schedule"
                }
            )
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(14.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                draft = draft.copy(
                                    hour = hour,
                                    minute = minute
                                )
                            },
                            draft.hour,
                            draft.minute,
                            false
                        ).show()
                    },
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(
                        1.dp,
                        Amber.copy(alpha = 0.50f)
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor =
                            Amber.copy(alpha = 0.10f),
                        contentColor =
                            MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 16.dp,
                                vertical = 15.dp
                            ),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Card(
                            shape = CircleShape,
                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        Amber.copy(alpha = 0.18f)
                                )
                        ) {
                            Icon(
                                modifier = Modifier
                                    .padding(10.dp)
                                    .size(24.dp),
                                imageVector =
                                    Icons.Default.AccessAlarm,
                                contentDescription = null,
                                tint = Amber
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 13.dp)
                        ) {
                            Text(
                                text = "MUST BE AWAKE BY",
                                style =
                                    MaterialTheme.typography.labelSmall,
                                fontWeight =
                                    FontWeight.ExtraBold,
                                color = Amber
                            )
                            Text(
                                modifier =
                                    Modifier.padding(top = 2.dp),
                                text =
                                    formatClock(
                                        draft.hour,
                                        draft.minute
                                    ),
                                style =
                                    MaterialTheme.typography.headlineMedium,
                                fontWeight =
                                    FontWeight.ExtraBold
                            )
                            Text(
                                modifier =
                                    Modifier.padding(top = 2.dp),
                                text =
                                    if (
                                        draft.mode ==
                                        AlarmMode.SMART_WAKE
                                    ) {
                                        "Latest possible alarm · Tap to change"
                                    } else {
                                        "Exact alarm time · Tap to change"
                                    },
                                style =
                                    MaterialTheme.typography.bodySmall,
                                color =
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "CHANGE",
                            style =
                                MaterialTheme.typography.labelSmall,
                            fontWeight =
                                FontWeight.ExtraBold,
                            color = Amber
                        )
                    }
                }

                Text(
                    text = "Wake style",
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        modifier = Modifier.weight(1f),
                        selected =
                            draft.mode == AlarmMode.SMART_WAKE,
                        onClick = {
                            draft = draft.copy(
                                mode = AlarmMode.SMART_WAKE
                            )
                        },
                        label = {
                            Text("Smart Wake")
                        }
                    )
                    FilterChip(
                        modifier = Modifier.weight(1f),
                        selected =
                            draft.mode == AlarmMode.STANDARD,
                        onClick = {
                            draft = draft.copy(
                                mode = AlarmMode.STANDARD,
                                smartOffsetMinutes = 0
                            )
                        },
                        label = {
                            Text("Standard")
                        }
                    )
                }

                Text(
                    text =
                        if (draft.mode == AlarmMode.SMART_WAKE) {
                            "WakeSync can wake you at a better moment before your latest wake time. Live Health Connect sleep data works best with a fitness tracker."
                        } else {
                            "A normal exact alarm at the selected time."
                        },
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Schedule",
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        SchedulePreset.TOMORROW to "Tomorrow",
                        SchedulePreset.EVERY_DAY to "Daily",
                        SchedulePreset.WEEKDAYS to "Weekdays"
                    ).forEach { (value, label) ->
                        FilterChip(
                            selected = preset == value,
                            onClick = {
                                draft =
                                    when (value) {
                                        SchedulePreset.TOMORROW ->
                                            draft.copy(
                                                oneTimeDate =
                                                    LocalDate.now()
                                                        .plusDays(1)
                                                        .toString(),
                                                days = emptySet(),
                                                skippedDates =
                                                    emptySet()
                                            )

                                        SchedulePreset.EVERY_DAY ->
                                            draft.copy(
                                                oneTimeDate = null,
                                                days =
                                                    AlarmSchedule.EVERY_DAY,
                                                skippedDates =
                                                    emptySet()
                                            )

                                        SchedulePreset.WEEKDAYS ->
                                            draft.copy(
                                                oneTimeDate = null,
                                                days =
                                                    AlarmSchedule.WEEKDAYS,
                                                skippedDates =
                                                    emptySet()
                                            )

                                        SchedulePreset.CUSTOM ->
                                            draft
                                    }
                            },
                            label = {
                                Text(label)
                            }
                        )
                    }

                    FilterChip(
                        selected =
                            preset == SchedulePreset.CUSTOM,
                        onClick = {
                            draft = draft.copy(
                                oneTimeDate = null,
                                days =
                                    if (
                                        draft.days.isEmpty()
                                    ) {
                                        AlarmSchedule.WEEKDAYS
                                    } else {
                                        draft.days
                                    },
                                skippedDates = emptySet()
                            )
                        },
                        label = {
                            Text("Custom")
                        }
                    )
                }

                AnimatedVisibility(
                    visible =
                        preset == SchedulePreset.CUSTOM
                ) {
                    Column(
                        verticalArrangement =
                            Arrangement.spacedBy(6.dp)
                    ) {
                        dayItems()
                            .chunked(4)
                            .forEach { rowDays ->
                                Row(
                                    modifier =
                                        Modifier.fillMaxWidth(),
                                    horizontalArrangement =
                                        Arrangement.spacedBy(6.dp)
                                ) {
                                    rowDays.forEach {
                                            (value, label) ->
                                        FilterChip(
                                            selected =
                                                draft.days
                                                    .contains(value),
                                            onClick = {
                                                val days =
                                                    draft.days
                                                        .toMutableSet()
                                                if (
                                                    days.contains(value)
                                                ) {
                                                    days.remove(value)
                                                } else {
                                                    days.add(value)
                                                }
                                                draft = draft.copy(
                                                    days = days,
                                                    oneTimeDate = null
                                                )
                                            },
                                            label = {
                                                Text(label)
                                            }
                                        )
                                    }
                                }
                            }
                    }
                }

                if (
                    draft.mode ==
                    AlarmMode.SMART_WAKE
                ) {
                    Text(
                        text = "Smart Wake window",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text =
                            "How early WakeSync may wake you before “Must be awake by.”",
                        style =
                            MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(10, 15, 20, 30)
                            .filter {
                                it <= maxSmartWindowMinutes
                            }
                            .forEach { minutes ->
                                FilterChip(
                                    selected =
                                        draft.smartWindowMinutes ==
                                            minutes,
                                    onClick = {
                                        draft = draft.copy(
                                            smartWindowMinutes =
                                                minutes,
                                            smartOffsetMinutes =
                                                draft.smartOffsetMinutes
                                                    .coerceAtMost(
                                                        minutes
                                                    )
                                        )
                                    },
                                    label = {
                                        Text(
                                            minutes.toString() +
                                                " min"
                                        )
                                    }
                                )
                            }
                    }
                }

                HorizontalDivider()

                Text(
                    text = "Backup alarms",
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text =
                        "If you dismiss an alarm half-asleep, WakeSync can ring again every 5 minutes. “I’m awake” stops the remaining alarms.",
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0, 1, 2, 3)
                        .forEach { count ->
                            FilterChip(
                                selected =
                                    draft.backupRingCount ==
                                        count,
                                onClick = {
                                    draft = draft.copy(
                                        backupRingCount =
                                            count
                                    )
                                },
                                label = {
                                    Text(
                                        if (count == 0) {
                                            "Off"
                                        } else {
                                            count.toString()
                                        }
                                    )
                                }
                            )
                        }
                }

                TextButton(
                    onClick = {
                        showMore = !showMore
                    }
                ) {
                    Text(
                        if (showMore) {
                            "Hide more options"
                        } else {
                            "More options"
                        }
                    )
                }

                AnimatedVisibility(
                    visible = showMore
                ) {
                    Column(
                        verticalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            modifier =
                                Modifier.fillMaxWidth(),
                            value = draft.label,
                            onValueChange = {
                                draft = draft.copy(
                                    label = it.take(28)
                                )
                            },
                            label = {
                                Text("Alarm name")
                            },
                            placeholder = {
                                Text("Optional")
                            },
                            singleLine = true
                        )

                        ToggleRow(
                            title = "Sound",
                            checked =
                                draft.soundEnabled,
                            onCheckedChange = {
                                draft = draft.copy(
                                    soundEnabled = it
                                )
                            }
                        )

                        ToggleRow(
                            title = "Vibration",
                            checked =
                                draft.vibrationEnabled,
                            onCheckedChange = {
                                draft = draft.copy(
                                    vibrationEnabled = it
                                )
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(draft)
                },
                enabled =
                    draft.oneTimeDate != null ||
                        draft.days.isNotEmpty()
            ) {
                Text("Done")
            }
        },
        dismissButton = {
            Row {
                if (!isNew) {
                    TextButton(
                        onClick = {
                            onDelete(draft)
                        }
                    ) {
                        Text(
                            "Delete",
                            color =
                                MaterialTheme.colorScheme.error
                        )
                    }
                }
                TextButton(
                    onClick = onDismiss
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween,
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Text(
            title,
            fontWeight = FontWeight.Medium
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

private fun dayItems(): List<Pair<Int, String>> =
    listOf(
        DayOfWeek.SUNDAY.value to "S",
        DayOfWeek.MONDAY.value to "M",
        DayOfWeek.TUESDAY.value to "T",
        DayOfWeek.WEDNESDAY.value to "W",
        DayOfWeek.THURSDAY.value to "T",
        DayOfWeek.FRIDAY.value to "F",
        DayOfWeek.SATURDAY.value to "S"
    )

private fun formatClock(
    hour: Int,
    minute: Int
): String =
    String.format(
        "%d:%02d %s",
        if (hour % 12 == 0) {
            12
        } else {
            hour % 12
        },
        minute,
        if (hour < 12) "AM" else "PM"
    )
