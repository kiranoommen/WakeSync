package com.kiranoommen.wakesync.ui.alarms

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kiranoommen.wakesync.model.AlarmMode
import com.kiranoommen.wakesync.model.AlarmSchedule
import com.kiranoommen.wakesync.ui.theme.Amber
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun AlarmEditorDialog(
    schedule: AlarmSchedule,
    isNew: Boolean,
    maxSmartWindowMinutes: Int,
    onDismiss: () -> Unit,
    onSave: (AlarmSchedule) -> Unit,
    onDelete: (AlarmSchedule) -> Unit
) {
    val context = LocalContext.current
    var draft by remember(
        schedule.id,
        maxSmartWindowMinutes
    ) {
        val cappedWindow =
            schedule.smartWindowMinutes
                .coerceAtMost(maxSmartWindowMinutes)
        mutableStateOf(
            schedule.copy(
                smartWindowMinutes = cappedWindow,
                smartOffsetMinutes =
                    schedule.smartOffsetMinutes
                        .coerceAtMost(cappedWindow)
            )
        )
    }

    var showMoreOptions by remember(schedule.id) {
        mutableStateOf(false)
    }
    var showSoundPicker by remember(schedule.id) {
        mutableStateOf(false)
    }
    var previewRingtone by remember {
        mutableStateOf<Ringtone?>(null)
    }
    var pendingSoundUri by remember(schedule.id) {
        mutableStateOf<String?>(null)
    }
    var pendingSoundName by remember(schedule.id) {
        mutableStateOf<String?>(null)
    }
    val alarmTones =
        remember(context) {
            loadAlarmTones(context)
        }

    val audioManager =
        remember(context) {
            context.getSystemService(AudioManager::class.java)
        }
    var alarmVolumePercent by remember {
        val max =
            audioManager
                .getStreamMaxVolume(
                    AudioManager.STREAM_ALARM
                )
                .coerceAtLeast(1)
        val current =
            audioManager
                .getStreamVolume(
                    AudioManager.STREAM_ALARM
                )
        mutableFloatStateOf(
            current.toFloat() /
                max.toFloat() *
                100f
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            previewRingtone?.stop()
            previewRingtone = null
        }
    }

    fun stopPreview() {
        previewRingtone?.stop()
        previewRingtone = null
    }

    fun previewSound(uri: Uri?) {
        stopPreview()
        val actualUri =
            uri ?: RingtoneManager
                .getActualDefaultRingtoneUri(
                    context,
                    RingtoneManager.TYPE_ALARM
                )

        if (actualUri != null) {
            previewRingtone =
                runCatching {
                    RingtoneManager
                        .getRingtone(context, actualUri)
                        ?.also { it.play() }
                }.getOrNull()
        }
    }

    val audioFileLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                runCatching {
                    context.contentResolver
                        .takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                }

                val displayName =
                    runCatching {
                        context.contentResolver.query(
                            uri,
                            arrayOf(OpenableColumns.DISPLAY_NAME),
                            null,
                            null,
                            null
                        )?.use { cursor ->
                            val index =
                                cursor.getColumnIndex(
                                    OpenableColumns.DISPLAY_NAME
                                )
                            if (
                                index >= 0 &&
                                cursor.moveToFirst()
                            ) {
                                cursor.getString(index)
                            } else {
                                null
                            }
                        }
                    }.getOrNull()

                draft = draft.copy(
                    soundEnabled = true,
                    soundUri = uri.toString(),
                    soundName =
                        displayName ?: "Custom audio"
                )
            }
        }

    val schedulePreset =
        when {
            draft.oneTimeDate != null ->
                "TOMORROW"
            draft.days == AlarmSchedule.WEEKDAYS ->
                "WEEKDAYS"
            draft.days.size == 7 ->
                "EVERY_DAY"
            else ->
                "CUSTOM"
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isNew) "New alarm" else "Edit alarm")
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
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
                            Amber.copy(alpha = 0.48f)
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
                                colors = CardDefaults.cardColors(
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
                                    text = "ALARM TIME",
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
                                            "Tap to change"
                                        } else {
                                            "Tap to change"
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
                }

                item {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = draft.label,
                        onValueChange = {
                            draft = draft.copy(
                                label = it.take(28)
                            )
                        },
                        label = { Text("Label (optional)") },
                        placeholder = { Text("Work, Weekend, Kids…") },
                        singleLine = true
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Smart Wake",
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                modifier = Modifier.padding(top = 2.dp),
                                text = "Wake during lighter sleep before the alarm time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked =
                                draft.mode == AlarmMode.SMART_WAKE,
                            onCheckedChange = { enabled ->
                                draft = draft.copy(
                                    mode =
                                        if (enabled) {
                                            AlarmMode.SMART_WAKE
                                        } else {
                                            AlarmMode.STANDARD
                                        },
                                    smartWindowMinutes =
                                        if (enabled) {
                                            draft.smartWindowMinutes
                                                .coerceAtLeast(10)
                                        } else {
                                            draft.smartWindowMinutes
                                        },
                                    smartOffsetMinutes =
                                        if (enabled) {
                                            draft.smartOffsetMinutes
                                        } else {
                                            0
                                        }
                                )
                            }
                        )
                    }
                }

                item {
                    Column {
                        Text(
                            text = "Repeat",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            modifier = Modifier.padding(top = 3.dp),
                            text = "Choose when this alarm repeats.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement =
                            Arrangement.spacedBy(7.dp)
                    ) {
                        FilterChip(
                            selected =
                                schedulePreset == "TOMORROW",
                            onClick = {
                                draft = draft.copy(
                                    oneTimeDate =
                                        LocalDate.now()
                                            .plusDays(1)
                                            .toString(),
                                    days = emptySet()
                                )
                            },
                            label = { Text("Tomorrow only") }
                        )

                        FilterChip(
                            selected =
                                schedulePreset == "WEEKDAYS",
                            onClick = {
                                draft = draft.copy(
                                    oneTimeDate = null,
                                    days = AlarmSchedule.WEEKDAYS
                                )
                            },
                            label = { Text("Weekdays") }
                        )

                        FilterChip(
                            selected =
                                schedulePreset == "EVERY_DAY",
                            onClick = {
                                draft = draft.copy(
                                    oneTimeDate = null,
                                    days = (1..7).toSet()
                                )
                            },
                            label = { Text("Every day") }
                        )

                        FilterChip(
                            selected =
                                schedulePreset == "CUSTOM",
                            onClick = {
                                draft = draft.copy(
                                    oneTimeDate = null,
                                    days =
                                        if (draft.days.isEmpty()) {
                                            AlarmSchedule.WEEKDAYS
                                        } else {
                                            draft.days
                                        }
                                )
                            },
                            label = { Text("Custom") }
                        )
                    }
                }

                if (schedulePreset == "CUSTOM") {
                    item {
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
                                                    draft.days.contains(value),
                                                onClick = {
                                                    val newDays =
                                                        draft.days
                                                            .toMutableSet()

                                                    if (
                                                        newDays.contains(value)
                                                    ) {
                                                        newDays.remove(value)
                                                    } else {
                                                        newDays.add(value)
                                                    }

                                                    draft = draft.copy(
                                                        oneTimeDate = null,
                                                        days = newDays
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
                }

                item {
                    Column {
                        Text(
                            text = "Wake window",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            modifier = Modifier.padding(top = 3.dp),
                            text = if (draft.mode == AlarmMode.SMART_WAKE) {
                                "Choose how early WakeSync may wake you."
                            } else {
                                "Turn on Smart Wake to use a wake window."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                    listOf(10, 15, 20, 30)
                        .filter { it <= maxSmartWindowMinutes }
                        .forEach { minutes ->
                        FilterChip(
                            selected =
                                draft.mode == AlarmMode.SMART_WAKE &&
                                    draft.smartWindowMinutes == minutes,
                            enabled = draft.mode == AlarmMode.SMART_WAKE,
                            onClick = {
                                draft = draft.copy(
                                    smartWindowMinutes = minutes,
                                    smartOffsetMinutes = draft.smartOffsetMinutes.coerceAtMost(minutes)
                                )
                            },
                            label = {
                                Text(minutes.toString() + "m")
                            }
                        )
                    }
                    }
                }

                item {
                    Text(
                        text =
                            if (draft.mode == AlarmMode.STANDARD) {
                                "Standard Alarm rings exactly at " +
                                    formatClock(draft.hour, draft.minute) + "."
                            } else {
                                when (draft.smartWindowMinutes) {
                                    10 -> "Tight: up to 10 minutes before the must-be-awake time."
                                    15 -> "Gentle: up to 15 minutes early."
                                    20 -> "Balanced: up to 20 minutes early."
                                    30 -> "Flexible: up to 30 minutes early."
                                    else -> "WakeSync stays inside your selected Smart Wake window."
                                }
                            },
                    style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item {
                    HorizontalDivider()
                }

                item {
                    Column {
                        Text(
                            text = "Backup alarms",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            modifier = Modifier.padding(top = 3.dp),
                            text =
                                if (draft.mode == AlarmMode.SMART_WAKE) {
                                    "After the Smart Wake attempt, the must-be-awake time stays armed. Add extra alarms after the deadline if you tend to dismiss the first alarm half-asleep."
                                } else {
                                    "Add extra alarms after the exact alarm time so dismissing one half-asleep does not end the whole sequence."
                                },
                            style = MaterialTheme.typography.bodySmall,
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0, 1, 2, 3).forEach { count ->
                            FilterChip(
                                selected =
                                    draft.backupRingCount == count,
                                onClick = {
                                    draft = draft.copy(
                                        backupRingCount = count
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
                }

                item {
                    Text(
                        text =
                            if (draft.backupRingCount == 0) {
                                "Off. One normal dismiss ends the active ring."
                            } else {
                                draft.backupRingCount.toString() +
                                    " extra alarm" +
                                    if (draft.backupRingCount == 1) {
                                        ""
                                    } else {
                                        "s"
                                    } +
                                    " · every 5 minutes. Dismissing one alarm keeps the rest armed; “I’m awake” stops them."
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item {
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            showMoreOptions = !showMoreOptions
                        }
                    ) {
                        Text(
                            text =
                                if (showMoreOptions) {
                                    "Hide more options"
                                } else {
                                    "More options"
                                },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (showMoreOptions) {
                    item {
                        HorizontalDivider()
                    }

                    item {
                        ToggleSettingRow(
                            title = "Alarm sound",
                            checked = draft.soundEnabled,
                            onCheckedChange = {
                                draft = draft.copy(
                                    soundEnabled = it
                                )
                            }
                        )
                    }

                    item {
                        ToggleSettingRow(
                            title = "Vibration",
                            checked = draft.vibrationEnabled,
                            onCheckedChange = {
                                draft = draft.copy(
                                    vibrationEnabled = it
                                )
                            }
                        )
                    }

                    if (draft.soundEnabled) {
                        item {
                            Card(
                                modifier =
                                    Modifier.fillMaxWidth(),
                                shape =
                                    RoundedCornerShape(18.dp),
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor =
                                            MaterialTheme.colorScheme
                                                .surfaceVariant
                                                .copy(alpha = 0.42f)
                                    )
                            ) {
                                Column(
                                    modifier =
                                        Modifier.padding(14.dp),
                                    verticalArrangement =
                                        Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "Alarm sound",
                                        style =
                                            MaterialTheme.typography.labelMedium,
                                        color =
                                            MaterialTheme.colorScheme
                                                .onSurfaceVariant
                                    )

                                    Text(
                                        text =
                                            draft.soundName
                                                ?: "System default",
                                        style =
                                            MaterialTheme.typography.titleMedium,
                                        fontWeight =
                                            FontWeight.ExtraBold
                                    )

                                    Row(
                                        modifier =
                                            Modifier.fillMaxWidth(),
                                        horizontalArrangement =
                                            Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            modifier =
                                                Modifier.weight(1f),
                                            onClick = {
                                                pendingSoundUri =
                                                    draft.soundUri
                                                pendingSoundName =
                                                    draft.soundName
                                                showSoundPicker = true
                                            }
                                        ) {
                                            Text("Ringtone")
                                        }

                                        OutlinedButton(
                                            modifier =
                                                Modifier.weight(1f),
                                            onClick = {
                                                audioFileLauncher.launch(
                                                    arrayOf("audio/*")
                                                )
                                            }
                                        ) {
                                            Text("Choose file")
                                        }
                                    }

                                    if (draft.soundUri != null) {
                                        TextButton(
                                            modifier =
                                                Modifier.fillMaxWidth(),
                                            onClick = {
                                                draft = draft.copy(
                                                    soundUri = null,
                                                    soundName = null
                                                )
                                            }
                                        ) {
                                            Text("Use system default")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    if (draft.mode == AlarmMode.SMART_WAKE) {
                        val start =
                            LocalTime.of(
                                draft.hour,
                                draft.minute
                            ).minusMinutes(
                                draft.smartWindowMinutes.toLong()
                            )

                        Column {
                            Text(
                                text =
                                    "Wake me between " +
                                        formatClock(
                                            start.hour,
                                            start.minute
                                        ) +
                                        "–" +
                                        formatClock(
                                            draft.hour,
                                            draft.minute
                                        ),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                modifier = Modifier.padding(top = 3.dp),
                                text =
                                    "WakeSync will try to wake you during lighter sleep. " +
                                        formatClock(
                                            draft.hour,
                                            draft.minute
                                        ) +
                                        " is guaranteed.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(draft) },
                enabled =
                    draft.oneTimeDate != null ||
                        draft.days.isNotEmpty()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (!isNew) {
                    TextButton(onClick = { onDelete(draft) }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )

    if (showSoundPicker) {
        AlertDialog(
            onDismissRequest = {
                stopPreview()
                showSoundPicker = false
            },
            title = {
                Text("Ringtone")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween,
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Alarm volume",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text =
                                alarmVolumePercent
                                    .toInt()
                                    .toString() +
                                    "%"
                        )
                    }

                    Slider(
                        value = alarmVolumePercent,
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

                            alarmVolumePercent =
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

                    LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    pendingSoundUri = null
                                    pendingSoundName = null
                                    previewSound(null)
                                }
                                .padding(
                                    horizontal = 6.dp,
                                    vertical = 14.dp
                                ),
                            horizontalArrangement =
                                Arrangement.SpaceBetween,
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            Text(
                                text = "System default",
                                fontWeight =
                                    if (pendingSoundUri == null) {
                                        FontWeight.ExtraBold
                                    } else {
                                        FontWeight.Normal
                                    }
                            )
                            if (pendingSoundUri == null) {
                                Text(
                                    text = "Selected",
                                    style =
                                        MaterialTheme.typography.labelMedium,
                                    color = Amber
                                )
                            }
                        }
                    }

                    items(
                        items = alarmTones,
                        key = { tone ->
                            tone.uri.toString()
                        }
                    ) { tone ->
                        val selected =
                            pendingSoundUri ==
                                tone.uri.toString()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    pendingSoundUri =
                                        tone.uri.toString()
                                    pendingSoundName =
                                        tone.name
                                    previewSound(tone.uri)
                                }
                                .padding(
                                    horizontal = 6.dp,
                                    vertical = 14.dp
                                ),
                            horizontalArrangement =
                                Arrangement.SpaceBetween,
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            Text(
                                modifier = Modifier.weight(1f),
                                text = tone.name,
                                fontWeight =
                                    if (selected) {
                                        FontWeight.ExtraBold
                                    } else {
                                        FontWeight.Normal
                                    }
                            )
                            if (selected) {
                                Text(
                                    text = "Playing",
                                    style =
                                        MaterialTheme.typography.labelMedium,
                                    color = Amber
                                )
                            }
                        }
                    }
                }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        stopPreview()
                        draft = draft.copy(
                            soundEnabled = true,
                            soundUri = pendingSoundUri,
                            soundName = pendingSoundName
                        )
                        showSoundPicker = false
                    }
                ) {
                    Text("Done")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        stopPreview()
                        showSoundPicker = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}


@Composable
private fun ToggleSettingRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontWeight = FontWeight.Medium)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}


private data class AlarmToneOption(
    val name: String,
    val uri: Uri
)

private fun loadAlarmTones(
    context: Context
): List<AlarmToneOption> {
    val manager =
        RingtoneManager(context).apply {
            setType(RingtoneManager.TYPE_ALARM)
        }
    val cursor = manager.cursor
    val tones = mutableListOf<AlarmToneOption>()

    try {
        for (index in 0 until cursor.count) {
            val uri =
                manager.getRingtoneUri(index)
                    ?: continue
            val name =
                runCatching {
                    RingtoneManager
                        .getRingtone(context, uri)
                        ?.getTitle(context)
                }.getOrNull()
                    ?.takeIf { it.isNotBlank() }
                    ?: "Alarm " + (index + 1)

            tones +=
                AlarmToneOption(
                    name = name,
                    uri = uri
                )
        }
    } finally {
        cursor.close()
    }

    return tones.distinctBy {
        it.uri.toString()
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
        if (hour % 12 == 0) 12 else hour % 12,
        minute,
        if (hour < 12) "AM" else "PM"
    )
