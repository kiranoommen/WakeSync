package com.kiranoommen.wakesync.ui

import android.app.TimePickerDialog
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import com.kiranoommen.wakesync.data.AppSettingsStore
import com.kiranoommen.wakesync.data.SleepExporter
import com.kiranoommen.wakesync.domain.SleepAnalytics
import com.kiranoommen.wakesync.model.AlarmSchedule
import com.kiranoommen.wakesync.model.SleepNight
import com.kiranoommen.wakesync.model.SleepStageType
import com.kiranoommen.wakesync.ui.theme.Amber
import com.kiranoommen.wakesync.ui.theme.Coral
import com.kiranoommen.wakesync.ui.theme.Cyan
import com.kiranoommen.wakesync.ui.theme.Indigo
import com.kiranoommen.wakesync.ui.theme.Lavender
import com.kiranoommen.wakesync.ui.theme.Mint
import com.kiranoommen.wakesync.ui.theme.PearlMuted
import com.kiranoommen.wakesync.ui.theme.Sunrise
import com.kiranoommen.wakesync.ui.theme.WakeSyncTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.roundToInt

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
    hasAnalyticsPermission: Boolean,
    hasHistoryPermission: Boolean,
    historyReadAvailable: Boolean,
    themeMode: String,
    sleepGoalMinutes: Int,
    dashboardWidgets: List<String>,
    errorMessage: String?,
    onConnect: () -> Unit,
    onRefresh: () -> Unit,
    onSaveSchedule: (AlarmSchedule) -> Unit,
    onDeleteSchedule: (AlarmSchedule) -> Unit,
    onToggleSchedule: (AlarmSchedule, Boolean) -> Unit,
    onSkipNext: (AlarmSchedule) -> Unit,
    onClearSkips: (AlarmSchedule) -> Unit,
    onRequestExactAlarmAccess: () -> Unit,
    onRequestAnalyticsAccess: () -> Unit,
    onRequestHistoryAccess: () -> Unit,
    onThemeModeChange: (String) -> Unit,
    onSleepGoalChange: (Int) -> Unit,
    onDashboardWidgetsChange: (List<String>) -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        AppSettingsStore.THEME_DARK -> true
        AppSettingsStore.THEME_LIGHT -> false
        else -> systemDark
    }

    WakeSyncTheme(darkTheme = darkTheme) {
        var tab by remember { mutableStateOf(AppTab.HOME) }
        var editingSchedule by remember { mutableStateOf<AlarmSchedule?>(null) }
        var creatingNew by remember { mutableStateOf(false) }

        val colors = MaterialTheme.colorScheme
        val context = LocalContext.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            colors.background,
                            if (darkTheme) Color(0xFF111A36) else Color(0xFFF2F0FF),
                            colors.background
                        )
                    )
                )
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (darkTheme) {
                    drawCircle(
                        color = Lavender.copy(alpha = 0.10f),
                        radius = size.minDimension * 0.52f,
                        center = Offset(size.width * 0.86f, size.height * 0.12f)
                    )
                    drawCircle(
                        color = Amber.copy(alpha = 0.075f),
                        radius = size.minDimension * 0.46f,
                        center = Offset(size.width * 0.08f, size.height * 0.50f)
                    )
                    drawCircle(
                        color = Indigo.copy(alpha = 0.10f),
                        radius = size.minDimension * 0.60f,
                        center = Offset(size.width * 0.78f, size.height * 0.86f)
                    )
                } else {
                    drawCircle(
                        color = Lavender.copy(alpha = 0.08f),
                        radius = size.minDimension * 0.48f,
                        center = Offset(size.width * 0.90f, size.height * 0.12f)
                    )
                    drawCircle(
                        color = Amber.copy(alpha = 0.06f),
                        radius = size.minDimension * 0.42f,
                        center = Offset(size.width * 0.10f, size.height * 0.52f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp)
            ) {
                AppHeader(tab, onSettings = { tab = AppTab.SETTINGS })

                Box(modifier = Modifier.weight(1f)) {
                    Crossfade(
                        targetState = tab,
                        label = "WakeSyncTab"
                    ) { activeTab ->
                        when (activeTab) {
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
                                onSkipNext = onSkipNext,
                                onClearSkips = onClearSkips,
                                onGoAlarms = { tab = AppTab.ALARMS },
                                sleepGoalMinutes = sleepGoalMinutes,
                                dashboardWidgets = dashboardWidgets,
                                onDashboardWidgetsChange = onDashboardWidgetsChange
                            )

                            AppTab.ALARMS -> AlarmsTab(
                                schedules = schedules,
                                onAdd = {
                                    creatingNew = true
                                    editingSchedule = defaultSchedule()
                                },
                                onEdit = { editingSchedule = it },
                                onToggle = onToggleSchedule,
                                onSkipNext = onSkipNext,
                                onClearSkips = onClearSkips
                            )

                            AppTab.SLEEP -> SleepTab(
                                nights = nights,
                                loading = loading,
                                sleepGoalMinutes = sleepGoalMinutes,
                                onRefresh = onRefresh,
                                onShareCsv = { SleepExporter.shareCsv(context, it) },
                                onSharePdf = { SleepExporter.sharePdf(context, it) }
                            )

                            AppTab.SETTINGS -> SettingsTab(
                                hasPermission = hasPermission,
                                exactAlarmAccess = exactAlarmAccess,
                                hasAnalyticsPermission = hasAnalyticsPermission,
                                hasHistoryPermission = hasHistoryPermission,
                                historyReadAvailable = historyReadAvailable,
                                themeMode = themeMode,
                                sleepGoalMinutes = sleepGoalMinutes,
                                nights = nights,
                                onConnect = onConnect,
                                onRequestExactAlarmAccess = onRequestExactAlarmAccess,
                                onRequestAnalyticsAccess = onRequestAnalyticsAccess,
                                onRequestHistoryAccess = onRequestHistoryAccess,
                                onThemeModeChange = onThemeModeChange,
                                onSleepGoalChange = onSleepGoalChange
                            )
                        }
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
private fun AppHeader(
    tab: AppTab,
    onSettings: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(
            modifier = Modifier
                .width(44.dp)
                .height(34.dp)
        ) {
            fun wave(y: Float): Path = Path().apply {
                moveTo(0f, y)
                cubicTo(
                    size.width * 0.20f,
                    y - size.height * 0.22f,
                    size.width * 0.37f,
                    y + size.height * 0.22f,
                    size.width * 0.57f,
                    y
                )
                cubicTo(
                    size.width * 0.74f,
                    y - size.height * 0.20f,
                    size.width * 0.86f,
                    y + size.height * 0.13f,
                    size.width,
                    y - size.height * 0.08f
                )
            }

            drawPath(
                path = wave(size.height * 0.36f),
                color = Lavender,
                style = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round)
            )
            drawPath(
                path = wave(size.height * 0.66f),
                color = Sunrise,
                style = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        ) {
            Text(
                text = "WakeSync",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = colors.onBackground
            )
            Text(
                text = when (tab) {
                    AppTab.HOME -> "Better mornings, in sync with you"
                    AppTab.ALARMS -> "Wake by your schedule — not ours"
                    AppTab.SLEEP -> "Understand your sleep pattern"
                    AppTab.SETTINGS -> "Privacy, preferences & reliability"
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }

        Card(
            onClick = onSettings,
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            border = BorderStroke(1.dp, colors.outlineVariant),
            colors = CardDefaults.cardColors(
                containerColor = colors.surface.copy(alpha = 0.78f),
                contentColor = colors.onSurface
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⚙",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface
                )
            }
        }
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
    onSkipNext: (AlarmSchedule) -> Unit,
    onClearSkips: (AlarmSchedule) -> Unit,
    onGoAlarms: () -> Unit,
    sleepGoalMinutes: Int,
    dashboardWidgets: List<String>,
    onDashboardWidgetsChange: (List<String>) -> Unit
) {
    val next = remember(schedules) { nextSchedule(schedules) }
    val nearestSkipped = remember(schedules) { nearestUpcomingSkipped(schedules) }
    var showCustomize by remember { mutableStateOf(false) }
    val dashboardAnalytics = remember(nights, sleepGoalMinutes) {
        SleepAnalytics.analyze(nights.take(14), sleepGoalMinutes)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            if (next != null) {
                NextWakeCard(
                    schedule = next.first,
                    deadline = next.second,
                    onEdit = { onEditSchedule(next.first) },
                    onSkip = { onSkipNext(next.first) }
                )
            } else {
                EmptyAlarmCard(onGoAlarms)
            }
        }

        if (nearestSkipped != null) {
            item {
                SkippedBanner(
                    schedule = nearestSkipped.first,
                    skippedDate = nearestSkipped.second,
                    onUndo = { onSkipNext(nearestSkipped.first) }
                )
            }
        }

        if (
            sdkStatus == HealthConnectClient.SDK_UNAVAILABLE ||
            sdkStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED
        ) {
            item {
                InfoCard(
                    title = "Health Connect needs attention",
                    body = "WakeSync can still manage alarms, but sleep insights need Health Connect available on this device."
                )
            }
        } else if (!hasPermission) {
            item { ConnectCard(onConnect) }
        }

        if (hasPermission) {
            item {
                SectionHeader(
                    title = "Your dashboard",
                    action = "Customize",
                    onAction = { showCustomize = true }
                )
            }

            dashboardWidgets.forEach { widget ->
                when (widget) {
                    AppSettingsStore.WIDGET_GOAL -> item {
                        GoalStreakCard(
                            analytics = dashboardAnalytics,
                            sleepGoalMinutes = sleepGoalMinutes
                        )
                    }

                    AppSettingsStore.WIDGET_SLEEP -> {
                        if (loading) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp),
                                    shape = RoundedCornerShape(28.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    colors = CardDefaults.cardColors(
                                        containerColor =
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.70f)
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = Cyan)
                                    }
                                }
                            }
                        } else if (nights.isNotEmpty()) {
                            item { SleepMetricRow(nights.first()) }
                        }
                    }

                    AppSettingsStore.WIDGET_INSIGHT -> item {
                        HomeInsightCard(
                            text = SleepAnalytics.insightFor(
                                dashboardAnalytics,
                                sleepGoalMinutes
                            )
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onRefresh) {
                        Text(
                            text = "Refresh sleep data",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item { PrivacyBanner() }

        errorMessage?.let { message ->
            item {
                InfoCard(
                    title = "Something needs attention",
                    body = message
                )
            }
        }

        item { Spacer(Modifier.height(10.dp)) }
    }

    if (showCustomize) {
        DashboardCustomizeDialog(
            current = dashboardWidgets,
            onDismiss = { showCustomize = false },
            onSave = {
                onDashboardWidgetsChange(it)
                showCustomize = false
            }
        )
    }
}

@Composable
private fun GoalStreakCard(
    analytics: com.kiranoommen.wakesync.domain.PeriodAnalytics,
    sleepGoalMinutes: Int
) {
    val recent = analytics.nights.sortedByDescending { it.date }
    val streak = recent.takeWhile { it.asleepMinutes >= sleepGoalMinutes }.size
    val latest = recent.firstOrNull()
    val progress = latest?.let {
        (it.asleepMinutes.toFloat() / sleepGoalMinutes.toFloat())
            .coerceIn(0f, 1f)
    } ?: 0f
    val progressPercent = (progress * 100f).roundToInt()
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        label = "sleepGoalProgress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(112.dp),
                contentAlignment = Alignment.Center
            ) {
                val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.09f)

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 10.dp.toPx()
                    val diameter = size.minDimension - stroke

                    drawArc(
                        color = track,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(stroke / 2f, stroke / 2f),
                        size = Size(diameter, diameter),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(Mint, Cyan, Mint)
                        ),
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        topLeft = Offset(stroke / 2f, stroke / 2f),
                        size = Size(diameter, diameter),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = progressPercent.toString() + "%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "goal",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 18.dp)
            ) {
                Text(
                    text = "Sleep goal",
                    color = Mint,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    modifier = Modifier.padding(top = 5.dp),
                    text = latest?.let { formatMinutes(it.asleepMinutes) } ?: "—",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatMinutes(sleepGoalMinutes.toLong()) + " target",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Card(
                    modifier = Modifier.padding(top = 12.dp),
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, Mint.copy(alpha = 0.28f)),
                    colors = CardDefaults.cardColors(
                        containerColor = Mint.copy(alpha = 0.12f),
                        contentColor = Mint
                    )
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        text = "🔥 " + streak + " Night Streak",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeInsightCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, Lavender.copy(alpha = 0.30f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    color = Lavender.copy(alpha = 0.13f),
                    radius = size.minDimension * 0.62f,
                    center = Offset(size.width * 0.92f, size.height * 0.05f)
                )
            }

            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "✨  WakeSync Insight",
                    fontWeight = FontWeight.ExtraBold,
                    color = Lavender
                )
                Text(
                    modifier = Modifier.padding(top = 9.dp),
                    text = text,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    modifier = Modifier.padding(top = 9.dp),
                    text = "Personalized from your on-device sleep trends",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DashboardCustomizeDialog(
    current: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    var working by remember(current) { mutableStateOf(current) }
    val all = listOf(
        AppSettingsStore.WIDGET_GOAL to "Sleep goal & streak",
        AppSettingsStore.WIDGET_SLEEP to "Last-night metrics",
        AppSettingsStore.WIDGET_INSIGHT to "Personal insight"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Customize dashboard") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Pin the cards you want and change their order.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                all.forEach { (id, label) ->
                    val visible = working.contains(id)
                    val index = working.indexOf(id)

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, fontWeight = FontWeight.Medium)
                                Switch(
                                    checked = visible,
                                    onCheckedChange = { checked ->
                                        working = if (checked) {
                                            working + id
                                        } else {
                                            working.filterNot { it == id }
                                        }
                                    }
                                )
                            }

                            if (visible) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    TextButton(
                                        enabled = index > 0,
                                        onClick = {
                                            val list = working.toMutableList()
                                            val item = list.removeAt(index)
                                            list.add(index - 1, item)
                                            working = list
                                        }
                                    ) {
                                        Text("Move up")
                                    }

                                    TextButton(
                                        enabled = index >= 0 && index < working.lastIndex,
                                        onClick = {
                                            val list = working.toMutableList()
                                            val item = list.removeAt(index)
                                            list.add(index + 1, item)
                                            working = list
                                        }
                                    ) {
                                        Text("Move down")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(working) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
    val editInteraction = remember { MutableInteractionSource() }
    val pressed by editInteraction.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        label = "editSchedulePress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
            contentColor = Color.White
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF4C1D95),
                            Color(0xFF6D28D9),
                            Color(0xFF7C3AED)
                        )
                    )
                )
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.10f),
                    radius = size.minDimension * 0.55f,
                    center = Offset(size.width * 0.87f, size.height * 0.16f)
                )
                drawCircle(
                    color = Sunrise.copy(alpha = 0.20f),
                    radius = size.minDimension * 0.38f,
                    center = Offset(size.width * 0.02f, size.height * 0.98f)
                )
            }

            Column(modifier = Modifier.padding(20.dp)) {
                Card(
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.11f),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                        text = "✨  SMART ALARM",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = if (schedule.smartWindowMinutes > 0) {
                        windowStart.format(timeFormat) + " – " + deadline.format(timeFormat)
                    } else {
                        deadline.format(timeFormat)
                    },
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = schedule.label.ifBlank { "Wake schedule" } +
                        " · " + deadline.format(dateFormat),
                    color = Color.White.copy(alpha = 0.80f)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.09f),
                        contentColor = Color.White
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (schedule.smartOffsetMinutes > 0) {
                                    "Predicted wake"
                                } else {
                                    "Protected deadline"
                                },
                                color = Color.White.copy(alpha = 0.72f),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                modifier = Modifier.padding(top = 2.dp),
                                text = if (schedule.smartOffsetMinutes > 0) {
                                    predicted.format(timeFormat)
                                } else {
                                    deadline.format(timeFormat)
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Card(
                            shape = RoundedCornerShape(999.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Mint.copy(alpha = 0.18f),
                                contentColor = Color(0xFFB7F7D8)
                            )
                        ) {
                            Text(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                text = "● Ready",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.padding(top = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        modifier = Modifier.scale(buttonScale),
                        onClick = onEdit,
                        interactionSource = editInteraction,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF0F172A)
                        )
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = 5.dp),
                            text = "Edit schedule",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onSkip,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            if (schedule.isNextOccurrenceSkipped()) {
                                "Undo skip"
                            } else {
                                "Skip once"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyAlarmCard(onGoAlarms: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        label = "setSchedulePress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
            contentColor = Color.White
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF4C1D95),
                            Color(0xFF6D28D9),
                            Color(0xFF7C3AED)
                        )
                    )
                )
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.10f),
                    radius = size.minDimension * 0.56f,
                    center = Offset(size.width * 0.88f, size.height * 0.12f)
                )
                drawCircle(
                    color = Sunrise.copy(alpha = 0.18f),
                    radius = size.minDimension * 0.42f,
                    center = Offset(size.width * 0.04f, size.height * 1.02f)
                )
            }

            Column(modifier = Modifier.padding(20.dp)) {
                Card(
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.11f),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                        text = "✨  SMART ALARM",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = "No wake schedule yet",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    modifier = Modifier.padding(top = 7.dp),
                    text = "Set your latest acceptable wake time. WakeSync handles the smart window inside it.",
                    color = Color.White.copy(alpha = 0.78f)
                )

                Button(
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .scale(buttonScale),
                    onClick = onGoAlarms,
                    interactionSource = interaction,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF0F172A)
                    )
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        text = "+ Set Schedule",
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun AlarmsTab(
    schedules: List<AlarmSchedule>,
    onAdd: () -> Unit,
    onEdit: (AlarmSchedule) -> Unit,
    onToggle: (AlarmSchedule, Boolean) -> Unit,
    onSkipNext: (AlarmSchedule) -> Unit,
    onClearSkips: (AlarmSchedule) -> Unit
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
                onSkip = { onSkipNext(schedule) },
                onClearSkips = { onClearSkips(schedule) }
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
    onSkip: () -> Unit,
    onClearSkips: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var showDisableChoice by remember { mutableStateOf(false) }
    val nextBase = schedule.nextBaseDeadline()
    val nextActual = schedule.nextDeadline()
    val nextSkipped = schedule.isNextOccurrenceSkipped()
    val futureSkips = schedule.futureSkippedDates()
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
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.55f)),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface.copy(alpha = 0.86f),
            contentColor = colors.onSurface
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
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
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
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.surfaceVariant.copy(alpha = 0.62f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        when {
                            nextSkipped && nextBase != null -> {
                                Text(
                                    text = "Next occurrence skipped",
                                    color = Amber,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    modifier = Modifier.padding(top = 3.dp),
                                    text = nextBase.format(
                                        DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a")
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant
                                )
                                if (nextActual != null) {
                                    Text(
                                        modifier = Modifier.padding(top = 3.dp),
                                        text = "Resumes " + nextActual.format(
                                            DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a")
                                        ),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            nextActual != null -> {
                                Text("Next alarm", fontWeight = FontWeight.Bold)
                                Text(
                                    modifier = Modifier.padding(top = 3.dp),
                                    text = nextActual.format(
                                        DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a")
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(onClick = onSkip) {
                        Text(if (nextSkipped) "Undo skip" else "Skip next")
                    }
                    if (futureSkips.size > 1) {
                        TextButton(onClick = onClearSkips) {
                            Text("Clear skips")
                        }
                    }
                }
            }
        }
    }

    if (showDisableChoice) {
        AlertDialog(
            onDismissRequest = { showDisableChoice = false },
            title = {
                Text(
                    if (nextSkipped) "Turn this schedule off?"
                    else "Skip once or turn schedule off?"
                )
            },
            text = {
                Text(
                    if (nextSkipped) {
                        "The next occurrence is already skipped. Turning this off disables the recurring schedule until you switch it back on."
                    } else {
                        "Skip once keeps this recurring schedule active and automatically resumes it on the next matching day."
                    }
                )
            },
            confirmButton = {
                if (!nextSkipped) {
                    Button(
                        onClick = {
                            onSkip()
                            showDisableChoice = false
                        }
                    ) {
                        Text("Skip once")
                    }
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
    sleepGoalMinutes: Int,
    onRefresh: () -> Unit,
    onShareCsv: (List<com.kiranoommen.wakesync.domain.NightAnalytics>) -> Unit,
    onSharePdf: (com.kiranoommen.wakesync.domain.PeriodAnalytics) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SleepAnalyticsScreen(
                nights = nights,
                loading = loading,
                targetSleepMinutes = sleepGoalMinutes,
                onRefresh = onRefresh,
                onShareCsv = onShareCsv,
                onSharePdf = onSharePdf
            )
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun SettingsTab(
    hasPermission: Boolean,
    exactAlarmAccess: Boolean,
    hasAnalyticsPermission: Boolean,
    hasHistoryPermission: Boolean,
    historyReadAvailable: Boolean,
    themeMode: String,
    sleepGoalMinutes: Int,
    nights: List<SleepNight>,
    onConnect: () -> Unit,
    onRequestExactAlarmAccess: () -> Unit,
    onRequestAnalyticsAccess: () -> Unit,
    onRequestHistoryAccess: () -> Unit,
    onThemeModeChange: (String) -> Unit,
    onSleepGoalChange: (Int) -> Unit
) {
    val source = nights.firstOrNull()?.sourcePackage ?: "No source detected yet"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            InfoCard(
                title = "Private by design",
                body = "WakeSync reads your permitted Health Connect data locally. It does not upload, sell, share, or modify health data. Export happens only when you choose it."
            )
        }

        item {
            SettingCard(
                title = "Health Connect",
                body = if (hasPermission) {
                    "Connected · read-only sleep access\nSource: " + sourceFriendlyName(source)
                } else {
                    "Sleep permission is not granted."
                },
                button = if (hasPermission) null else "Connect",
                onClick = onConnect
            )
        }

        item {
            SettingCard(
                title = "Recovery metrics",
                body = if (hasAnalyticsPermission) {
                    "HRV and resting-heart-rate access enabled."
                } else {
                    "Optional read-only access for HRV and resting heart rate. WakeSync works without it."
                },
                button = if (hasAnalyticsPermission) null else "Enable",
                onClick = onRequestAnalyticsAccess
            )
        }

        item {
            SettingCard(
                title = "Extended history",
                body = when {
                    !historyReadAvailable ->
                        "Historical Health Connect access is not available on this device."
                    hasHistoryPermission ->
                        "Enabled. WakeSync can analyze periods beyond the standard 30-day window."
                    else ->
                        "Optional permission required for 6-month and older comparisons."
                },
                button = if (historyReadAvailable && !hasHistoryPermission) "Enable" else null,
                onClick = onRequestHistoryAccess
            )
        }

        item {
            SettingCard(
                title = "Exact alarm access",
                body = if (exactAlarmAccess) {
                    "Enabled. Wake-by deadlines can use Android exact alarm scheduling."
                } else {
                    "Not enabled. Android may delay alarms. Grant exact alarm access for reliable wake deadlines."
                },
                button = if (exactAlarmAccess) null else "Allow",
                onClick = onRequestExactAlarmAccess
            )
        }

        item {
            PreferenceCard(title = "Appearance") {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    listOf(
                        AppSettingsStore.THEME_SYSTEM to "System",
                        AppSettingsStore.THEME_DARK to "Dark",
                        AppSettingsStore.THEME_LIGHT to "Light"
                    ).forEach { (value, label) ->
                        FilterChip(
                            selected = themeMode == value,
                            onClick = { onThemeModeChange(value) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }

        item {
            PreferenceCard(title = "Sleep goal") {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    listOf(420, 450, 480, 510, 540).forEach { minutes ->
                        FilterChip(
                            selected = sleepGoalMinutes == minutes,
                            onClick = { onSleepGoalChange(minutes) },
                            label = {
                                Text(
                                    when (minutes) {
                                        420 -> "7h"
                                        450 -> "7.5h"
                                        480 -> "8h"
                                        510 -> "8.5h"
                                        else -> "9h"
                                    }
                                )
                            }
                        )
                    }
                }
                Text(
                    modifier = Modifier.padding(top = 6.dp),
                    text = "Used for sleep-debt estimates and personal trend goals.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            InfoCard(
                title = "Smart wake guardrails",
                body = "You control the deadline and smart-window width. WakeSync never wakes you outside that window, and weak predictions fall back to the later deadline."
            )
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun PreferenceCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Box(modifier = Modifier.padding(top = 10.dp)) {
                content()
            }
        }
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
            LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = draft.label,
                    onValueChange = { draft = draft.copy(label = it.take(28)) },
                    label = { Text("Alarm name") },
                    singleLine = true
                    )
                }

                item {
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
                }

                item {
                    Text(
                        text = "Repeat",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    dayItems().chunked(4).forEach { rowDays ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowDays.forEach { (value, label) ->
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
                    }
                    }
                }

                item {
                    Text(
                        text = "Smart window",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                item {
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
                }

                item {
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
                }

                item {
                    HorizontalDivider()
                }

                item {
                    ToggleSettingRow(
                    title = "Sound",
                    checked = draft.soundEnabled,
                        onCheckedChange = { draft = draft.copy(soundEnabled = it) }
                    )
                }

                item {
                    ToggleSettingRow(
                    title = "Vibration",
                    checked = draft.vibrationEnabled,
                        onCheckedChange = { draft = draft.copy(vibrationEnabled = it) }
                    )
                }

                item {
                    Text(
                        text = "Snooze",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                    listOf(0, 5, 10).forEach { minutes ->
                        FilterChip(
                            selected = draft.snoozeMinutes == minutes,
                            onClick = { draft = draft.copy(snoozeMinutes = minutes) },
                            label = {
                                Text(if (minutes == 0) "Off" else minutes.toString() + "m")
                            }
                        )
                    }
                    }
                }

                item {
                    Text(
                        text = "If no favorable point is found, WakeSync alarms at " +
                            formatClock(draft.hour, draft.minute) + " anyway.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
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

@Composable
private fun BottomNav(
    selected: AppTab,
    onSelected: (AppTab) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 20.dp),
        shape = RoundedCornerShape(30.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface.copy(alpha = 0.90f),
            contentColor = colors.onSurface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 7.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                Triple(AppTab.HOME, "⌂", "Home"),
                Triple(AppTab.ALARMS, "◷", "Alarms"),
                Triple(AppTab.SLEEP, "☾", "Sleep"),
                Triple(AppTab.SETTINGS, "⚙", "Settings")
            )

            tabs.forEach { (tab, icon, label) ->
                val active = selected == tab
                val scale by animateFloatAsState(
                    targetValue = if (active) 1.04f else 1f,
                    label = "navScale" + tab.name
                )
                val contentColor by animateColorAsState(
                    targetValue = if (active) Color.White else colors.onSurfaceVariant,
                    label = "navColor" + tab.name
                )

                Card(
                    onClick = { onSelected(tab) },
                    modifier = Modifier.scale(scale),
                    shape = RoundedCornerShape(22.dp),
                    border = if (active) {
                        BorderStroke(1.dp, Lavender.copy(alpha = 0.28f))
                    } else {
                        null
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = if (active) {
                            Lavender.copy(alpha = 0.16f)
                        } else {
                            Color.Transparent
                        },
                        contentColor = contentColor
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = icon,
                            style = MaterialTheme.typography.titleMedium,
                            color = contentColor
                        )
                        Text(
                            modifier = Modifier.padding(top = 1.dp),
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            color = contentColor
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .size(if (active) 4.dp else 0.dp)
                                .background(
                                    color = if (active) Sunrise else Color.Transparent,
                                    shape = CircleShape
                                )
                        )
                    }
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
            containerColor = colors.surface.copy(alpha = 0.84f),
            contentColor = colors.onSurface
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

            SleepStageTimeline(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
                    .padding(top = 14.dp),
                night = night
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                StagePill("Deep", deep, Indigo, Modifier.weight(1f))
                StagePill("Light", light, Lavender, Modifier.weight(1f))
                StagePill("REM", rem, Amber, Modifier.weight(1f))
                StagePill("Awake", awake, colors.onSurfaceVariant, Modifier.weight(1f))
            }

            Text(
                modifier = Modifier.padding(top = 10.dp),
                text = "Source: " + sourceFriendlyName(night.sourcePackage),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SleepStageTimeline(
    modifier: Modifier,
    night: SleepNight
) {
    val deepColor = Indigo
    val lightColor = Lavender
    val remColor = Amber
    val awakeColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    Canvas(modifier = modifier) {
        if (night.stages.isEmpty()) return@Canvas

        val ordered = night.stages.sortedBy { it.start }
        val start = ordered.first().start
        val end = ordered.maxOf { it.end }
        val totalMillis = (end.toEpochMilli() - start.toEpochMilli()).coerceAtLeast(1L)
        val rowHeight = size.height / 4f
        val blockHeight = rowHeight * 0.42f

        repeat(4) { row ->
            val y = rowHeight * row + rowHeight / 2f
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        fun yFor(type: SleepStageType): Float =
            when (type) {
                SleepStageType.AWAKE -> rowHeight * 0.5f
                SleepStageType.REM -> rowHeight * 1.5f
                SleepStageType.LIGHT -> rowHeight * 2.5f
                SleepStageType.DEEP -> rowHeight * 3.5f
                SleepStageType.UNKNOWN -> rowHeight * 2.5f
            }

        fun colorFor(type: SleepStageType): Color =
            when (type) {
                SleepStageType.AWAKE -> awakeColor
                SleepStageType.REM -> remColor
                SleepStageType.LIGHT -> lightColor
                SleepStageType.DEEP -> deepColor
                SleepStageType.UNKNOWN -> lightColor.copy(alpha = 0.42f)
            }

        ordered.forEachIndexed { index, stage ->
            val xStart =
                ((stage.start.toEpochMilli() - start.toEpochMilli()).toFloat() / totalMillis) * size.width
            val xEnd =
                ((stage.end.toEpochMilli() - start.toEpochMilli()).toFloat() / totalMillis) * size.width
            val y = yFor(stage.type)

            drawRoundRect(
                color = colorFor(stage.type),
                topLeft = Offset(xStart, y - blockHeight / 2f),
                size = Size((xEnd - xStart).coerceAtLeast(2.dp.toPx()), blockHeight),
                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
            )

            if (index < ordered.lastIndex) {
                val next = ordered[index + 1]
                drawLine(
                    color = colorFor(next.type).copy(alpha = 0.66f),
                    start = Offset(xEnd, y),
                    end = Offset(xEnd, yFor(next.type)),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun SleepMetricRow(night: SleepNight) {
    val total = java.time.Duration.between(night.start, night.end).toMinutes()
    val deep = stageMinutes(night, SleepStageType.DEEP)
    val rem = stageMinutes(night, SleepStageType.REM)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetricCard(Modifier.weight(1f), "Total", formatMinutes(total), Amber)
        MetricCard(Modifier.weight(1f), "Deep", formatMinutes(deep), Indigo)
        MetricCard(Modifier.weight(1f), "REM", formatMinutes(rem), Lavender)
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier,
    label: String,
    value: String,
    accent: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = accent.copy(alpha = 0.10f)
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 11.dp, vertical = 12.dp)) {
            Text(value, fontWeight = FontWeight.Bold)
            Text(
                modifier = Modifier.padding(top = 2.dp),
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SkippedBanner(
    schedule: AlarmSchedule,
    skippedDate: LocalDate,
    onUndo: () -> Unit
) {
    val resume = schedule.nextDeadline(
        skippedDate
            .atTime(schedule.hour, schedule.minute)
            .atZone(ZoneId.systemDefault())
            .plusMinutes(1)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Amber.copy(alpha = 0.11f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Skipped " + skippedDate.format(
                        DateTimeFormatter.ofPattern("EEE, MMM d")
                    ),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    modifier = Modifier.padding(top = 2.dp),
                    text = if (resume != null) {
                        "Resumes " + resume.format(
                            DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a")
                        )
                    } else {
                        "Recurring schedule remains enabled."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onUndo) {
                Text("Undo")
            }
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
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
            contentColor = MaterialTheme.colorScheme.onSurface
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

private fun nearestUpcomingSkipped(
    schedules: List<AlarmSchedule>
): Pair<AlarmSchedule, LocalDate>? {
    val today = LocalDate.now()

    return schedules
        .filter { it.enabled }
        .flatMap { schedule ->
            schedule.futureSkippedDates(today).mapNotNull { value ->
                runCatching { schedule to LocalDate.parse(value) }.getOrNull()
            }
        }
        .minByOrNull { it.second }
}

private fun sourceFriendlyName(packageName: String): String =
    when {
        packageName.contains("fitbit", ignoreCase = true) -> "Fitbit via Health Connect"
        packageName.contains("samsung", ignoreCase = true) -> "Samsung Health via Health Connect"
        packageName.isBlank() -> "Health Connect"
        else -> packageName
    }

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
