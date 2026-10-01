package com.kiranoommen.wakesync.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import com.kiranoommen.wakesync.model.AlarmSchedule
import com.kiranoommen.wakesync.model.SleepNight
import com.kiranoommen.wakesync.model.SleepStageType
import com.kiranoommen.wakesync.ui.theme.Amber
import com.kiranoommen.wakesync.ui.theme.Indigo
import com.kiranoommen.wakesync.ui.theme.Lavender
import com.kiranoommen.wakesync.ui.theme.WakeSyncTheme
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

private enum class AppTab {
    HOME,
    ALARMS,
    SLEEP,
    SETTINGS
}

@Composable
fun WakeSyncScreen(
    sdkStatus: Int,
    hasPermission: Boolean,
    loading: Boolean,
    nights: List<SleepNight>,
    schedules: List<AlarmSchedule>,
    exactAlarmAccess: Boolean,
    errorMessage: String?,
    onConnect: () -> Unit,
    onRefresh: () -> Unit,
    onSaveSchedule: (AlarmSchedule) -> Unit,
    onDeleteSchedule: (AlarmSchedule) -> Unit,
    onToggleSchedule: (AlarmSchedule, Boolean) -> Unit,
    onSkipNext: (AlarmSchedule) -> Unit,
    onRequestExactAlarmAccess: () -> Unit
) {
    WakeSyncTheme {
        var tab by remember { mutableStateOf(AppTab.HOME) }
        var editingSchedule by remember { mutableStateOf<AlarmSchedule?>(null) }
        var creatingNew by remember { mutableStateOf(false) }

        val colors = MaterialTheme.colorScheme

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            colors.background,
                            colors.surfaceVariant.copy(alpha = 0.72f),
                            colors.background
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp)
            ) {
                AppHeader(tab)

                Box(modifier = Modifier.weight(1f)) {
                    when (tab) {
                    AppTab.HOME -> HomeTab(
                        sdkStatus = sdkStatus,
                        hasPermission = hasPermission,
                        loading = loading,
                        nights = nights,
                        schedules = schedules,
                        errorMessage = errorMessage,
                        onConnect = onConnect,
                        onRefresh = onRefresh,
                        onEditSchedule = { editingSchedule = it },
                        onSkipNext = onSkipNext
                    )

                    AppTab.ALARMS -> AlarmsTab(
                        schedules = schedules,
                        onAdd = {
                            creatingNew = true
                            editingSchedule = defaultSchedule()
                        },
                        onEdit = { editingSchedule = it },
                        onToggle = onToggleSchedule,
                        onSkipNext = onSkipNext
                    )

                    AppTab.SLEEP -> SleepTab(
                        nights = nights,
                        loading = loading,
                        onRefresh = onRefresh
                    )

                    AppTab.SETTINGS -> SettingsTab(
                        hasPermission = hasPermission,
                        exactAlarmAccess = exactAlarmAccess,
                        nights = nights,
                        onConnect = onConnect,
                        onRequestExactAlarmAccess = onRequestExactAlarmAccess
                    )
                    }
                }

                BottomNav(
                    selected = tab,
                    onSelected = { tab = it }
                )
            }
        }

        editingSchedule?.let { schedule ->
            AlarmEditorDialog(
                schedule = schedule,
                isNew = creatingNew,
                onDismiss = {
                    editingSchedule = null
                    creatingNew = false
                },
                onSave = {
                    onSaveSchedule(it)
                    editingSchedule = null
                    creatingNew = false
                },
                onDelete = {
                    onDeleteSchedule(it)
                    editingSchedule = null
                    creatingNew = false
                }
            )
        }
    }
}

@Composable
private fun AppHeader(tab: AppTab) {
    val colors = MaterialTheme.colorScheme

    Column(modifier = Modifier.padding(top = 12.dp, bottom = 14.dp)) {
        Text(
            text = "WakeSync",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
            color = colors.onBackground
        )
        Text(
            text = when (tab) {
                AppTab.HOME -> "Better mornings, in sync with you."
                AppTab.ALARMS -> "Your schedule. WakeSync handles the window."
                AppTab.SLEEP -> "Your sleep history from Health Connect."
                AppTab.SETTINGS -> "Privacy, permissions, and alarm reliability."
            },
            color = colors.onSurfaceVariant
        )
    }
}

@Composable
private fun HomeTab(
    sdkStatus: Int,
    hasPermission: Boolean,
    loading: Boolean,
    nights: List<SleepNight>,
    schedules: List<AlarmSchedule>,
    errorMessage: String?,
    onConnect: () -> Unit,
    onRefresh: () -> Unit,
    onEditSchedule: (AlarmSchedule) -> Unit,
    onSkipNext: (AlarmSchedule) -> Unit
) {
    val next = remember(schedules) { nextSchedule(schedules) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            when {
                sdkStatus == HealthConnectClient.SDK_UNAVAILABLE -> {
                    InfoCard(
                        title = "Health Connect unavailable",
                        body = "This device does not support the Health Connect connection WakeSync needs."
                    )
                }

                sdkStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                    InfoCard(
                        title = "Health Connect needs attention",
                        body = "Install or update Health Connect, then return to WakeSync."
                    )
                }

                !hasPermission -> ConnectCard(onConnect)
                next != null -> NextWakeCard(
                    schedule = next.first,
                    deadline = next.second,
                    onEdit = { onEditSchedule(next.first) },
                    onSkip = { onSkipNext(next.first) }
                )
                else -> EmptyAlarmCard()
            }
        }

        item {
            PrivacyBanner()
        }

        if (hasPermission) {
            item {
                SectionHeader(
                    title = "Last night's sleep",
                    action = "Refresh",
                    onAction = onRefresh
                )
            }

            if (loading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Amber)
                    }
                }
            } else if (nights.isNotEmpty()) {
                item {
                    SleepNightCard(nights.first())
                }
            } else {
                item {
                    InfoCard(
                        title = "Waiting for sleep data",
                        body = "WakeSync is connected, but no sleep session was returned yet."
                    )
                }
            }
        }

        errorMessage?.let { message ->
            item {
                Text(
                    text = "Error: $message",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        item { Spacer(Modifier.height(6.dp)) }
    }
}

@Composable
private fun NextWakeCard(
    schedule: AlarmSchedule,
    deadline: ZonedDateTime,
    onEdit: () -> Unit,
    onSkip: () -> Unit
) {
    val timeFormat = DateTimeFormatter.ofPattern("h:mm a")
    val dateFormat = DateTimeFormatter.ofPattern("EEE, MMM d")
    val windowStart = deadline.minusMinutes(schedule.smartWindowMinutes.toLong())
    val predicted = deadline.minusMinutes(schedule.smartOffsetMinutes.toLong())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Indigo.copy(alpha = 0.92f),
                            Lavender.copy(alpha = 0.78f),
                            Amber.copy(alpha = 0.80f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "Next wake",
                    color = Color.White.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = deadline.format(timeFormat),
                    color = Color.White,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = deadline.format(dateFormat) + " · " +
                        schedule.label.ifBlank { "Alarm" },
                    color = Color.White.copy(alpha = 0.85f)
                )

                Spacer(Modifier.height(16.dp))

                if (schedule.smartWindowMinutes > 0) {
                    Text(
                        text = "Smart window  " +
                            windowStart.format(timeFormat) + " – " + deadline.format(timeFormat),
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        modifier = Modifier.padding(top = 4.dp),
                        text = if (schedule.smartOffsetMinutes > 0) {
                            "Current prediction: " + predicted.format(timeFormat) +
                                ". Deadline is always protected."
                        } else {
                            "Current prediction: use the deadline. WakeSync is preserving sleep."
                        },
                        color = Color.White.copy(alpha = 0.76f),
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        text = "Exact-time alarm",
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onEdit,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.18f),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Edit")
                    }

                    OutlinedButton(
                        onClick = onSkip,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text("Skip once")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyAlarmCard() {
    InfoCard(
        title = "No wake schedule yet",
        body = "Add an alarm from the Alarms tab. Each schedule can have its own days, wake-by time, and smart-window length."
    )
}

@Composable
private fun AlarmsTab(
    schedules: List<AlarmSchedule>,
    onAdd: () -> Unit,
    onEdit: (AlarmSchedule) -> Unit,
    onToggle: (AlarmSchedule, Boolean) -> Unit,
    onSkipNext: (AlarmSchedule) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onAdd,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Amber,
                    contentColor = Color(0xFF15192A)
                )
            ) {
                Text(
                    modifier = Modifier.padding(vertical = 5.dp),
                    text = "+ Add wake schedule",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (schedules.isEmpty()) {
            item {
                InfoCard(
                    title = "Keep it simple",
                    body = "Example: Mon/Tue/Thu/Fri at 7:00 AM, Wednesday at 6:00 AM, weekends off."
                )
            }
        }

        items(schedules, key = { it.id }) { schedule ->
            AlarmScheduleCard(
                schedule = schedule,
                onEdit = { onEdit(schedule) },
                onToggle = { onToggle(schedule, it) },
                onSkip = { onSkipNext(schedule) }
            )
        }

        item { Spacer(Modifier.height(6.dp)) }
    }
}

@Composable
private fun AlarmScheduleCard(
    schedule: AlarmSchedule,
    onEdit: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onSkip: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var showDisableChoice by remember { mutableStateOf(false) }
    val time = String.format(
        "%d:%02d %s",
        if (schedule.hour % 12 == 0) 12 else schedule.hour % 12,
        schedule.minute,
        if (schedule.hour < 12) "AM" else "PM"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface.copy(alpha = 0.86f)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = time,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (schedule.enabled) colors.onSurface else colors.onSurfaceVariant
                    )
                    Text(
                        text = schedule.label.ifBlank { "Alarm" },
                        color = colors.onSurfaceVariant
                    )
                }

                Switch(
                    checked = schedule.enabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            onToggle(true)
                        } else {
                            showDisableChoice = true
                        }
                    }
                )
            }

            Text(
                modifier = Modifier.padding(top = 14.dp),
                text = daysLabel(schedule.days),
                color = colors.onSurface
            )

            Text(
                modifier = Modifier.padding(top = 5.dp),
                text = if (schedule.smartWindowMinutes == 0) {
                    "Exact time"
                } else {
                    "Smart window: " + schedule.smartWindowMinutes + " min · " +
                        if (schedule.smartOffsetMinutes > 0) {
                            "prediction " + schedule.smartOffsetMinutes + " min early"
                        } else {
                            "prediction at deadline"
                        }
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )

            if (schedule.enabled) {
                TextButton(
                    modifier = Modifier.padding(top = 4.dp),
                    onClick = onSkip
                ) {
                    Text("Skip next occurrence")
                }
            }
        }
    }

    if (showDisableChoice) {
        AlertDialog(
            onDismissRequest = { showDisableChoice = false },
            title = { Text("Skip once or turn schedule off?") },
            text = {
                Text(
                    "Skip once keeps this recurring schedule active and automatically resumes it on the next matching day."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSkip()
                        showDisableChoice = false
                    }
                ) {
                    Text("Skip once")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            onToggle(false)
                            showDisableChoice = false
                        }
                    ) {
                        Text("Turn off")
                    }
                    TextButton(onClick = { showDisableChoice = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}

@Composable
private fun SleepTab(
    nights: List<SleepNight>,
    loading: Boolean,
    onRefresh: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Recent nights",
                action = "Refresh",
                onAction = onRefresh
            )
        }

        if (loading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Amber)
                }
            }
        }

        items(nights.take(30)) { night ->
            SleepNightCard(night)
        }

        if (!loading && nights.isEmpty()) {
            item {
                InfoCard(
                    title = "No sleep data",
                    body = "Once Health Connect returns sleep sessions, they will appear here."
                )
            }
        }

        item { Spacer(Modifier.height(6.dp)) }
    }
}

@Composable
private fun SettingsTab(
    hasPermission: Boolean,
    exactAlarmAccess: Boolean,
    nights: List<SleepNight>,
    onConnect: () -> Unit,
    onRequestExactAlarmAccess: () -> Unit
) {
    val source = nights.firstOrNull()?.sourcePackage ?: "No source detected yet"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            InfoCard(
                title = "Private by design",
                body = "WakeSync reads sleep data locally through Health Connect. It does not upload, sell, share, or modify your health data."
            )
        }

        item {
            SettingCard(
                title = "Health Connect",
                body = if (hasPermission) {
                    "Connected · read-only sleep access\nSource: $source"
                } else {
                    "Sleep permission is not granted."
                },
                button = if (hasPermission) null else "Connect",
                onClick = onConnect
            )
        }

        item {
            SettingCard(
                title = "Exact alarm access",
                body = if (exactAlarmAccess) {
                    "Enabled. Wake-by deadlines can use Android's exact alarm scheduling."
                } else {
                    "Not enabled. Android may delay alarms. Grant exact alarm access for reliable wake deadlines."
                },
                button = if (exactAlarmAccess) null else "Allow",
                onClick = onRequestExactAlarmAccess
            )
        }

        item {
            InfoCard(
                title = "How smart wake works",
                body = "You choose the latest acceptable wake time and how wide the smart window may be. WakeSync never moves the alarm outside that window, and the deadline always wins."
            )
        }

        item { Spacer(Modifier.height(6.dp)) }
    }
}

@Composable
private fun AlarmEditorDialog(
    schedule: AlarmSchedule,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (AlarmSchedule) -> Unit,
    onDelete: (AlarmSchedule) -> Unit
) {
    val context = LocalContext.current
    var draft by remember(schedule.id) { mutableStateOf(schedule) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isNew) "New wake schedule" else "Edit wake schedule")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                draft = draft.copy(hour = hour, minute = minute)
                            },
                            draft.hour,
                            draft.minute,
                            false
                        ).show()
                    }
                ) {
                    Text(
                        text = "Wake by  " + formatClock(draft.hour, draft.minute),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Text(
                    text = "Repeat",
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    dayItems().forEach { (value, label) ->
                        FilterChip(
                            selected = draft.days.contains(value),
                            onClick = {
                                val newDays = draft.days.toMutableSet()
                                if (newDays.contains(value)) newDays.remove(value) else newDays.add(value)
                                draft = draft.copy(days = newDays)
                            },
                            label = { Text(label) }
                        )
                    }
                }

                Text(
                    text = "Smart window",
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0, 10, 20, 30, 45).forEach { minutes ->
                        FilterChip(
                            selected = draft.smartWindowMinutes == minutes,
                            onClick = {
                                draft = draft.copy(
                                    smartWindowMinutes = minutes,
                                    smartOffsetMinutes = draft.smartOffsetMinutes.coerceAtMost(minutes)
                                )
                            },
                            label = {
                                Text(if (minutes == 0) "Off" else minutes.toString())
                            }
                        )
                    }
                }

                Text(
                    text = when (draft.smartWindowMinutes) {
                        0 -> "Alarm exactly at your wake-by time."
                        10 -> "Tight: WakeSync can move up to 10 minutes earlier."
                        20 -> "Balanced: up to 20 minutes earlier."
                        30 -> "Flexible: up to 30 minutes earlier."
                        else -> "Wide: up to 45 minutes earlier. Only use this if you are comfortable waking substantially early."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()

                Text(
                    text = "If no favorable point is found, WakeSync alarms at " +
                        formatClock(draft.hour, draft.minute) + " anyway.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(draft) },
                enabled = draft.days.isNotEmpty()
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
}

@Composable
private fun BottomNav(
    selected: AppTab,
    onSelected: (AppTab) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp, top = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface.copy(alpha = 0.94f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(
                AppTab.HOME to "Home",
                AppTab.ALARMS to "Alarms",
                AppTab.SLEEP to "Sleep",
                AppTab.SETTINGS to "Settings"
            ).forEach { (tab, label) ->
                TextButton(
                    onClick = { onSelected(tab) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (selected == tab) Amber else colors.onSurfaceVariant
                    )
                ) {
                    Text(label, fontWeight = if (selected == tab) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable
private fun ConnectCard(onConnect: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Connect your sleep data",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                modifier = Modifier.padding(top = 6.dp),
                text = "WakeSync uses Health Connect with read-only sleep access.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                onClick = onConnect,
                colors = ButtonDefaults.buttonColors(containerColor = Amber)
            ) {
                Text("Connect sleep data", color = Color(0xFF15192A))
            }
        }
    }
}

@Composable
private fun PrivacyBanner() {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface.copy(alpha = 0.76f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Processed on your device",
                fontWeight = FontWeight.SemiBold
            )
            Text(
                modifier = Modifier.padding(top = 3.dp),
                text = "Read-only access. Your sleep data stays on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SleepNightCard(night: SleepNight) {
    val colors = MaterialTheme.colorScheme
    val zone = ZoneId.systemDefault()
    val start = night.start.atZone(zone)
    val end = night.end.atZone(zone)
    val totalMinutes = java.time.Duration.between(night.start, night.end).toMinutes()

    val deep = stageMinutes(night, SleepStageType.DEEP)
    val light = stageMinutes(night, SleepStageType.LIGHT)
    val rem = stageMinutes(night, SleepStageType.REM)
    val awake = stageMinutes(night, SleepStageType.AWAKE)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface.copy(alpha = 0.84f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = end.format(DateTimeFormatter.ofPattern("EEE, MMM d")),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = start.format(DateTimeFormatter.ofPattern("h:mm a")) +
                            " – " + end.format(DateTimeFormatter.ofPattern("h:mm a")),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }

                Text(
                    text = formatMinutes(totalMinutes),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                StagePill("Deep", deep, Indigo, Modifier.weight(1f))
                StagePill("Light", light, Lavender, Modifier.weight(1f))
                StagePill("REM", rem, Amber, Modifier.weight(1f))
                StagePill("Awake", awake, colors.onSurfaceVariant, Modifier.weight(1f))
            }

            Text(
                modifier = Modifier.padding(top = 10.dp),
                text = "Source: " + night.sourcePackage,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StagePill(
    label: String,
    minutes: Long,
    color: Color,
    modifier: Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.12f)
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 10.dp)) {
            Text(
                text = label,
                color = color,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                modifier = Modifier.padding(top = 3.dp),
                text = formatMinutes(minutes),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                modifier = Modifier.padding(top = 6.dp),
                text = body,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingCard(
    title: String,
    body: String,
    button: String?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                modifier = Modifier.padding(top = 6.dp),
                text = body,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (button != null) {
                Button(
                    modifier = Modifier.padding(top = 12.dp),
                    onClick = onClick
                ) {
                    Text(button)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    action: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        TextButton(onClick = onAction) {
            Text(action)
        }
    }
}

private fun nextSchedule(schedules: List<AlarmSchedule>): Pair<AlarmSchedule, ZonedDateTime>? =
    schedules
        .filter { it.enabled }
        .mapNotNull { schedule ->
            schedule.nextDeadline()?.let { schedule to it }
        }
        .minByOrNull { it.second }

private fun defaultSchedule(): AlarmSchedule =
    AlarmSchedule(
        id = UUID.randomUUID().toString(),
        label = "Workday",
        hour = 7,
        minute = 0,
        days = AlarmSchedule.WEEKDAYS,
        smartWindowMinutes = 20,
        enabled = true
    )

private fun dayItems(): List<Pair<Int, String>> = listOf(
    DayOfWeek.SUNDAY.value to "S",
    DayOfWeek.MONDAY.value to "M",
    DayOfWeek.TUESDAY.value to "T",
    DayOfWeek.WEDNESDAY.value to "W",
    DayOfWeek.THURSDAY.value to "T",
    DayOfWeek.FRIDAY.value to "F",
    DayOfWeek.SATURDAY.value to "S"
)

private fun daysLabel(days: Set<Int>): String {
    if (days == AlarmSchedule.WEEKDAYS) return "Mon, Tue, Wed, Thu, Fri"
    if (days.size == 7) return "Every day"

    return dayItems()
        .filter { days.contains(it.first) }
        .joinToString(", ") { (value, _) ->
            DayOfWeek.of(value).name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
        }
}

private fun formatClock(hour: Int, minute: Int): String =
    String.format(
        "%d:%02d %s",
        if (hour % 12 == 0) 12 else hour % 12,
        minute,
        if (hour < 12) "AM" else "PM"
    )

private fun stageMinutes(night: SleepNight, type: SleepStageType): Long =
    night.stages
        .asSequence()
        .filter { it.type == type }
        .sumOf { java.time.Duration.between(it.start, it.end).toMinutes() }

private fun formatMinutes(minutes: Long): String {
    if (minutes <= 0) return "—"
    val hours = minutes / 60
    val remainder = minutes % 60
    return if (hours > 0) {
        hours.toString() + "h " + remainder + "m"
    } else {
        remainder.toString() + "m"
    }
}
