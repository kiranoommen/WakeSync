package com.kiranoommen.wakesync.ui

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Settings
import androidx.health.connect.client.HealthConnectClient
import com.kiranoommen.wakesync.data.AppSettingsStore
import com.kiranoommen.wakesync.domain.SleepAnalytics
import com.kiranoommen.wakesync.model.AlarmMode
import com.kiranoommen.wakesync.model.AlarmSchedule
import com.kiranoommen.wakesync.model.SleepNight
import com.kiranoommen.wakesync.model.SleepStageType
import com.kiranoommen.wakesync.model.SmartWakeReadiness
import com.kiranoommen.wakesync.model.WakeEvent
import com.kiranoommen.wakesync.model.WakeFeedback
import com.kiranoommen.wakesync.ui.alarms.AlarmEditorDialog
import com.kiranoommen.wakesync.ui.home.SmartWakeStatusCard
import com.kiranoommen.wakesync.ui.home.WakeHistorySection
import com.kiranoommen.wakesync.ui.settings.AlarmReliabilityCard
import com.kiranoommen.wakesync.ui.theme.Amber
import com.kiranoommen.wakesync.ui.theme.Coral
import com.kiranoommen.wakesync.ui.theme.Cyan
import com.kiranoommen.wakesync.ui.theme.Indigo
import com.kiranoommen.wakesync.ui.theme.IndigoGlow
import com.kiranoommen.wakesync.ui.theme.Lavender
import com.kiranoommen.wakesync.ui.theme.Mint
import com.kiranoommen.wakesync.ui.theme.PearlMuted
import com.kiranoommen.wakesync.ui.theme.Sunrise
import com.kiranoommen.wakesync.ui.theme.WakeSyncTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private enum class AppTab {
    HOME,
    ALARMS,
    SLEEP,
    SETTINGS
}

private const val PAYPAL_DONATION_URL =
    "https://www.paypal.com/donate/?business=MNCUN6HWWXAEY&no_recurring=0&currency_code=USD"

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun WakeSyncScreen(
    sdkStatus: Int,
    hasPermission: Boolean,
    loading: Boolean,
    nights: List<SleepNight>,
    schedules: List<AlarmSchedule>,
    exactAlarmAccess: Boolean,
    smartWakeReadiness: SmartWakeReadiness,
    alarmVolumePercent: Int,
    wakeEvents: List<WakeEvent>,
    hasHistoryPermission: Boolean,
    historyReadAvailable: Boolean,
    themeMode: String,
    sleepGoalMinutes: Int,
    goalsEnabled: Boolean,
    errorMessage: String?,
    onConnect: () -> Unit,
    onRefresh: () -> Unit,
    onSaveSchedule: (AlarmSchedule) -> Unit,
    onDeleteSchedule: (AlarmSchedule) -> Unit,
    onToggleSchedule: (AlarmSchedule, Boolean) -> Unit,
    onSkipNext: (AlarmSchedule) -> Unit,
    onClearSkips: (AlarmSchedule) -> Unit,
    onRequestHistoryAccess: () -> Unit,
    onWakeFeedback: (String, WakeFeedback) -> Unit,
    onClearWakeHistory: () -> Unit,
    onRequestBackgroundSmartWakeAccess: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenFullScreenSettings: () -> Unit,
    onTestAlarm: () -> Unit,
    onOpenAlarmVolumeSettings: () -> Unit,
    onRequestExactAlarmAccess: () -> Unit,
    onThemeModeChange: (String) -> Unit,
    onSleepGoalChange: (Int) -> Unit,
    onGoalsEnabledChange: (Boolean) -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        AppSettingsStore.THEME_DARK -> true
        AppSettingsStore.THEME_LIGHT -> false
        else -> systemDark
    }

    WakeSyncTheme(darkTheme = darkTheme) {
        var editingSchedule by remember {
            mutableStateOf<AlarmSchedule?>(null)
        }
        var creatingNew by remember {
            mutableStateOf(false)
        }

        val context =
            LocalContext.current
        val tabs =
            remember {
                listOf(
                    AppTab.ALARMS,
                    AppTab.SETTINGS
                )
            }
        val pagerState =
            rememberPagerState(
                initialPage = 0,
                pageCount = {
                    tabs.size
                }
            )
        val pagerScope =
            rememberCoroutineScope()
        val pagerPosition =
            pagerState.currentPage.toFloat() +
                pagerState.currentPageOffsetFraction
        val activeTab =
            tabs[
                pagerState.currentPage
                    .coerceIn(
                        0,
                        tabs.lastIndex
                    )
            ]

        Box(
            modifier =
                Modifier.fillMaxSize()
        ) {
            AmbientGradientBackground(
                pagePosition =
                    pagerPosition,
                darkTheme =
                    darkTheme,
                modifier =
                    Modifier.fillMaxSize()
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(
                        horizontal = 18.dp
                    )
            ) {
                AppHeader(
                    activeTab
                )

                HorizontalPager(
                    state = pagerState,
                    modifier =
                        Modifier.weight(1f),
                    beyondViewportPageCount = 1
                ) { page ->
                    when (
                        tabs[
                            page.coerceIn(
                                0,
                                tabs.lastIndex
                            )
                        ]
                    ) {
                        AppTab.HOME ->
                            HomeTab(
                                sdkStatus =
                                    sdkStatus,
                                hasPermission =
                                    hasPermission,
                                loading =
                                    loading,
                                nights =
                                    nights,
                                schedules =
                                    schedules,
                                errorMessage =
                                    errorMessage,
                                onConnect =
                                    onConnect,
                                onRefresh =
                                    onRefresh,
                                onEditSchedule = {
                                    editingSchedule =
                                        it
                                },
                                onSkipNext =
                                    onSkipNext,
                                onClearSkips =
                                    onClearSkips,
                                onGoAlarms = {
                                    pagerScope.launch {
                                        pagerState
                                            .animateScrollToPage(
                                                tabs.indexOf(AppTab.ALARMS)
                                            )
                                    }
                                },
                                sleepGoalMinutes =
                                    sleepGoalMinutes,
                                goalsEnabled =
                                    goalsEnabled,
                                smartWakeReadiness =
                                    smartWakeReadiness,
                                wakeEvents =
                                    wakeEvents,
                                onWakeFeedback =
                                    onWakeFeedback,
                                onClearWakeHistory =
                                    onClearWakeHistory,
                            )

                        AppTab.ALARMS ->
                            AlarmsTab(
                                schedules =
                                    schedules,
                                onAdd = {
                                    creatingNew =
                                        true
                                    editingSchedule =
                                        defaultSchedule()
                                },
                                onEdit = {
                                    editingSchedule =
                                        it
                                },
                                onDuplicate = { source ->
                                    creatingNew = true
                                    editingSchedule = source.copy(
                                        id =
                                            UUID.randomUUID()
                                                .toString(),
                                        label = "",
                                        skippedDates = emptySet()
                                    )
                                },
                                onToggle =
                                    onToggleSchedule,
                                onSkipNext =
                                    onSkipNext,
                                onClearSkips =
                                    onClearSkips
                            )

                        AppTab.SLEEP ->
                            SleepTab(
                                nights = nights,
                                loading = loading,
                                sleepGoalMinutes =
                                    sleepGoalMinutes,
                                goalsEnabled = goalsEnabled,
                                onRefresh = onRefresh
                            )

                        AppTab.SETTINGS ->
                            SettingsTab(
                                hasPermission =
                                    hasPermission,
                                exactAlarmAccess =
                                    exactAlarmAccess,
                                smartWakeReadiness =
                                    smartWakeReadiness,
                                alarmVolumePercent =
                                    alarmVolumePercent,
                                hasHistoryPermission =
                                    hasHistoryPermission,
                                historyReadAvailable =
                                    historyReadAvailable,
                                themeMode =
                                    themeMode,
                                sleepGoalMinutes =
                                    sleepGoalMinutes,
                                goalsEnabled =
                                    goalsEnabled,
                                nights =
                                    nights,
                                onConnect =
                                    onConnect,
                                onRequestHistoryAccess =
                                    onRequestHistoryAccess,
                                onRequestExactAlarmAccess =
                                    onRequestExactAlarmAccess,
                                onRequestBackgroundSmartWakeAccess =
                                    onRequestBackgroundSmartWakeAccess,
                                onOpenNotificationSettings =
                                    onOpenNotificationSettings,
                                onOpenFullScreenSettings =
                                    onOpenFullScreenSettings,
                                onTestAlarm =
                                    onTestAlarm,
                                onOpenAlarmVolumeSettings =
                                    onOpenAlarmVolumeSettings,
                                onThemeModeChange =
                                    onThemeModeChange,
                                onSleepGoalChange =
                                    onSleepGoalChange,
                                onGoalsEnabledChange =
                                    onGoalsEnabledChange,
                            )
                    }
                }

                BottomNav(
                    pagerPosition =
                        pagerPosition,
                    onSelected = {
                            tab ->
                        pagerScope.launch {
                            pagerState
                                .animateScrollToPage(
                                    tabs.indexOf(tab)
                                        .coerceAtLeast(0)
                                )
                        }
                    }
                )
            }
        }

        editingSchedule?.let { schedule ->
            AlarmEditorDialog(
                schedule = schedule,
                isNew = creatingNew,
                maxSmartWindowMinutes = 30,
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
    tab: AppTab
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WakeSyncBrandMark(
            modifier =
                Modifier.size(42.dp)
        )

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
                    AppTab.ALARMS -> "Simple alarms with Smart Wake when you want it"
                    AppTab.SLEEP -> "Understand your sleep pattern"
                    AppTab.SETTINGS -> "Alarm reliability & preferences"
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
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
    goalsEnabled: Boolean,
    smartWakeReadiness: SmartWakeReadiness,
    wakeEvents: List<WakeEvent>,
    onWakeFeedback: (String, WakeFeedback) -> Unit,
    onClearWakeHistory: () -> Unit
) {
    val next = remember(schedules) { nextSchedule(schedules) }
    val nearestSkipped = remember(schedules) { nearestUpcomingSkipped(schedules) }
    var infoSheet by remember { mutableStateOf<MetricInfo?>(null) }
    val dashboardNights = remember(nights) {
        val cutoff =
            LocalDate.now().minusDays(13)
        nights
            .filter {
                !it.end
                    .atZone(
                        ZoneId.systemDefault()
                    )
                    .toLocalDate()
                    .isBefore(cutoff)
            }
            .sortedByDescending { it.end }
    }
    val dashboardAnalytics = remember(
        dashboardNights,
        sleepGoalMinutes
    ) {
        SleepAnalytics.analyze(
            dashboardNights,
            sleepGoalMinutes
        )
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
                    onSkip = { onSkipNext(next.first) },
                    onInfo = {
                        infoSheet = MetricInfo(
                            title = "Smart wake window",
                            meaning = "The range WakeSync is allowed to wake you in, ending at your protected wake-by deadline.",
                            measurement = "Your schedule sets the deadline and smart-window width. WakeSync may choose an earlier point only inside that range.",
                            importance = "A narrow, user-controlled window avoids the frustrating early wake-ups common in overly aggressive smart alarms."
                        )
                    }
                )
            } else {
                EmptyAlarmCard(
                    onGoAlarms = onGoAlarms,
                    onInfo = {
                        infoSheet = MetricInfo(
                            title = "Smart alarm schedule",
                            meaning = "Your wake-by schedule defines the latest acceptable wake time for each selected day.",
                            measurement = "WakeSync combines the recurring schedule with your chosen smart-window width and preserves the deadline.",
                            importance = "A protected deadline keeps the smart feature from making you late while still allowing a better wake point when appropriate."
                        )
                    }
                )
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

        item {
            SmartWakeStatusCard(
                readiness = smartWakeReadiness,
                nextSchedule = next?.first
            )
        }

        if (wakeEvents.isNotEmpty()) {
            item {
                WakeHistorySection(
                    events = wakeEvents,
                    onFeedback = onWakeFeedback,
                    onClear = onClearWakeHistory
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
                    title = "Last night",
                    action = "Refresh",
                    onAction = onRefresh
                )
            }

            item {
                LastNightDashboardTile(
                    modifier = Modifier.fillMaxWidth(),
                    night = nights.maxByOrNull { it.end },
                    loading = loading,
                    onInfo = {
                        infoSheet = MetricInfo(
                            title = "Last night",
                            meaning = "A compact summary of the most recent Health Connect sleep session.",
                            measurement = "WakeSync keeps sleep context secondary to the alarm and shows only the latest useful summary here.",
                            importance = "The full Sleep tab remains available for trends without turning Home into a sleep-tracking dashboard."
                        )
                    }
                )
            }
        }

        item {
            PrivacyBanner(
                onInfo = {
                    infoSheet = MetricInfo(
                        title = "On-device privacy",
                        meaning = "WakeSync reads permitted Health Connect data locally and does not operate a health-data cloud database.",
                        measurement = "Sleep context and wake recommendations are calculated on the device.",
                        importance = "Keeping raw health data local reduces unnecessary exposure and keeps the app usable without a paid server API."
                    )
                }
            )
        }

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

    MetricInfoBottomSheet(
        info = infoSheet,
        onDismiss = { infoSheet = null }
    )

}

@Composable
private fun DashboardBentoGrid(
    widgets: List<String>,
    analytics: com.kiranoommen.wakesync.domain.PeriodAnalytics,
    nights: List<SleepNight>,
    schedules: List<AlarmSchedule>,
    loading: Boolean,
    goalsEnabled: Boolean,
    sleepGoalMinutes: Int,
    onEditSchedule: (AlarmSchedule) -> Unit,
    onSkipNext: (AlarmSchedule) -> Unit,
    onGoAlarms: () -> Unit,
    onInfo: (MetricInfo) -> Unit
) {
    val visible = widgets
        .distinct()
        .filterNot {
            !goalsEnabled &&
                (
                    it == AppSettingsStore.WIDGET_GOAL ||
                        it == AppSettingsStore.WIDGET_DEBT
                    )
        }

    Column(
        verticalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {
        var index = 0

        while (index < visible.size) {
            val id = visible[index]

            if (dashboardSpan(id) == 2) {
                DashboardWidgetTile(
                    modifier =
                        Modifier.fillMaxWidth(),
                    id = id,
                    analytics = analytics,
                    nights = nights,
                    schedules = schedules,
                    loading = loading,
                    goalsEnabled = goalsEnabled,
                    sleepGoalMinutes =
                        sleepGoalMinutes,
                    onEditSchedule =
                        onEditSchedule,
                    onSkipNext =
                        onSkipNext,
                    onGoAlarms = onGoAlarms,
                    onInfo = onInfo
                )
                index++
            } else {
                val next =
                    visible.getOrNull(
                        index + 1
                    )
                val pair =
                    next != null &&
                        dashboardSpan(next) == 1

                if (pair) {
                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {
                        DashboardWidgetTile(
                            modifier =
                                Modifier.weight(1f),
                            id = id,
                            analytics = analytics,
                            nights = nights,
                            schedules = schedules,
                            loading = loading,
                            goalsEnabled =
                                goalsEnabled,
                            sleepGoalMinutes =
                                sleepGoalMinutes,
                            onEditSchedule =
                                onEditSchedule,
                            onSkipNext =
                                onSkipNext,
                            onGoAlarms =
                                onGoAlarms,
                            onInfo = onInfo
                        )

                        DashboardWidgetTile(
                            modifier =
                                Modifier.weight(1f),
                            id = next!!,
                            analytics =
                                analytics,
                            nights = nights,
                            schedules =
                                schedules,
                            loading = loading,
                            goalsEnabled =
                                goalsEnabled,
                            sleepGoalMinutes =
                                sleepGoalMinutes,
                            onEditSchedule =
                                onEditSchedule,
                            onSkipNext =
                                onSkipNext,
                            onGoAlarms =
                                onGoAlarms,
                            onInfo = onInfo
                        )
                    }
                } else {
                    DashboardWidgetTile(
                        modifier =
                            Modifier.fillMaxWidth(),
                        id = id,
                        analytics = analytics,
                        nights = nights,
                        schedules = schedules,
                        loading = loading,
                        goalsEnabled =
                            goalsEnabled,
                        sleepGoalMinutes =
                            sleepGoalMinutes,
                        onEditSchedule =
                            onEditSchedule,
                        onSkipNext =
                            onSkipNext,
                        onGoAlarms =
                            onGoAlarms,
                        onInfo = onInfo
                    )
                }

                index +=
                    if (pair) {
                        2
                    } else {
                        1
                    }
            }
        }
    }
}

private fun dashboardSpan(
    id: String
): Int =
    when (id) {
        AppSettingsStore.WIDGET_SLEEP,
        AppSettingsStore.WIDGET_INSIGHT,
        AppSettingsStore.WIDGET_HYPNOGRAM -> 2

        else -> 1
    }

@Composable
private fun DashboardWidgetTile(
    modifier: Modifier,
    id: String,
    analytics: com.kiranoommen.wakesync.domain.PeriodAnalytics,
    nights: List<SleepNight>,
    schedules: List<AlarmSchedule>,
    loading: Boolean,
    goalsEnabled: Boolean,
    sleepGoalMinutes: Int,
    onEditSchedule: (AlarmSchedule) -> Unit,
    onSkipNext: (AlarmSchedule) -> Unit,
    onGoAlarms: () -> Unit,
    onInfo: (MetricInfo) -> Unit
) {
    when (id) {
        AppSettingsStore.WIDGET_GOAL ->
            GoalDashboardTile(
                modifier = modifier,
                analytics = analytics,
                targetMinutes =
                    sleepGoalMinutes,
                onInfo = {
                    onInfo(
                        MetricInfo(
                            title =
                                "Sleep Goal & Streak",
                            meaning =
                                "Progress toward your optional nightly sleep target and consecutive tracked nights that met it.",
                            measurement =
                                "Latest sleep-stage minutes are divided by your selected target. The streak counts recent nights at or above that target.",
                            importance =
                                "Targets are optional and can be disabled in Settings."
                        )
                    )
                }
            )

        AppSettingsStore.WIDGET_SLEEP ->
            LastNightDashboardTile(
                modifier = modifier,
                night =
                    nights.maxByOrNull {
                        it.end
                    },
                loading = loading,
                onInfo = {
                    onInfo(
                        MetricInfo(
                            title =
                                "Last-Night Metrics",
                            meaning =
                                "A quick look at actual sleep duration, efficiency and stage mix from the latest Health Connect sleep session.",
                            measurement =
                                "Sleep Efficiency measures the percentage of time spent asleep while in bed: Time Asleep ÷ Total Time in Bed. It measures sleep continuity, not total hours.",
                            importance =
                                "A short night can still have high efficiency. For example, 5h 32m of nearly unbroken sleep can still produce about 97% efficiency even though total sleep was short."
                        )
                    )
                }
            )

        AppSettingsStore.WIDGET_INSIGHT ->
            InsightDashboardTile(
                modifier = modifier,
                text =
                    if (goalsEnabled) {
                        SleepAnalytics.insightFor(
                            analytics,
                            sleepGoalMinutes
                        )
                    } else {
                        "You averaged " +
                            formatMinutes(
                                analytics.averageSleepMinutes
                            ) +
                            " of sleep with " +
                            (
                                analytics.averageEfficiencyPercent
                                    ?.let {
                                        it.toString() +
                                            "%"
                                    }
                                    ?: "unavailable"
                                ) +
                            " estimated efficiency recently."
                    },
                onInfo = {
                    onInfo(
                        MetricInfo(
                            title =
                                "Personal Insights",
                            meaning =
                                "A plain-language observation generated from your recent local sleep trend.",
                            measurement =
                                "WakeSync compares recent duration, efficiency, regularity and timing on-device.",
                            importance =
                                "The goal is to highlight repeatable patterns without turning one night into a diagnosis."
                        )
                    )
                }
            )

        AppSettingsStore.WIDGET_DEBT ->
            SleepDebtDashboardTile(
                modifier = modifier,
                analytics = analytics,
                targetMinutes =
                    sleepGoalMinutes,
                onInfo = {
                    onInfo(
                        sleepDebtInfo()
                    )
                }
            )

        AppSettingsStore.WIDGET_HYPNOGRAM ->
            WeeklyHypnogramDashboardTile(
                modifier = modifier,
                nights = nights,
                onInfo = {
                    onInfo(
                        MetricInfo(
                            title =
                                "Weekly Hypnogram Trend",
                            meaning =
                                "A compact seven-night view of how Deep, Light, REM and Awake time were distributed.",
                            measurement =
                                "Each bar is built from Health Connect sleep-stage intervals for that night.",
                            importance =
                                "This is useful for spotting broad changes in fragmentation and stage mix. Wearable stage labels remain estimates."
                        )
                    )
                }
            )

        AppSettingsStore.WIDGET_ALARM ->
            SmartAlarmDashboardTile(
                modifier = modifier,
                schedules = schedules,
                onEdit = onEditSchedule,
                onSkip = onSkipNext,
                onGoAlarms = onGoAlarms,
                onInfo = {
                    onInfo(
                        MetricInfo(
                            title =
                                "Smart Alarm Status",
                            meaning =
                                "Must be awake by is the latest time the alarm will sound. Smart Wake Window (Early Window) is the optional earlier interval WakeSync can use.",
                            measurement =
                                "WakeSync schedules the must-be-awake time with Android and may choose an earlier wake point only inside the configured window.",
                            importance =
                                "The deadline prevents the smart feature from making you late."
                        )
                    )
                }
            )
    }
}

@Composable
private fun DashboardGlassCard(
    modifier: Modifier,
    title: String,
    onInfo: () -> Unit,
    content: @Composable () -> Unit
) {
    val isDark =
        MaterialTheme.colorScheme.background
            .luminance() < 0.5f

    Card(
        modifier = modifier,
        shape =
            RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.dp,
            wakeGlassBorderBrush()
        ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.Transparent,
                contentColor =
                    MaterialTheme.colorScheme.onSurface
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    if (isDark) {
                        8.dp
                    } else {
                        2.dp
                    }
            )
    ) {
        Box(
            modifier =
                Modifier.fillMaxWidth()
        ) {
            WakeGlassBackdrop(
                modifier =
                    Modifier.matchParentSize()
            )

            Column(
                modifier =
                    Modifier.padding(15.dp)
            ) {
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Text(
                        modifier =
                            Modifier.weight(1f),
                        text = title,
                        style =
                            MaterialTheme.typography.titleMedium,
                        fontWeight =
                            FontWeight.ExtraBold,
                        color =
                            MaterialTheme.colorScheme.onSurface
                    )
                    InfoTrigger(
                        onClick = onInfo
                    )
                }

                content()
            }
        }
    }
}

@Composable
private fun GoalDashboardTile(
    modifier: Modifier,
    analytics: com.kiranoommen.wakesync.domain.PeriodAnalytics,
    targetMinutes: Int,
    onInfo: () -> Unit
) {
    val latest =
        analytics.nights.firstOrNull()
    val streak =
        analytics.nights
            .sortedByDescending {
                it.date
            }
            .takeWhile {
                it.asleepMinutes >=
                    targetMinutes
            }
            .size

    DashboardGlassCard(
        modifier = modifier
            .height(178.dp),
        title = "Goal",
        onInfo = onInfo
    ) {
        Text(
            modifier =
                Modifier.padding(top = 16.dp),
            text =
                latest?.let {
                    formatMinutes(
                        it.asleepMinutes
                    )
                } ?: "—",
            style =
                MaterialTheme.typography.headlineMedium,
            fontWeight =
                FontWeight.ExtraBold,
            color =
                MaterialTheme.colorScheme.onSurface
        )
        Text(
            text =
                formatMinutes(
                    targetMinutes.toLong()
                ) +
                    " target",
            style =
                MaterialTheme.typography.bodySmall,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            modifier =
                Modifier.padding(top = 14.dp),
            text =
                "🔥 " +
                    streak +
                    " night streak",
            style =
                MaterialTheme.typography.labelMedium,
            fontWeight =
                FontWeight.Bold,
            color = Mint
        )
    }
}

@Composable
private fun LastNightDashboardTile(
    modifier: Modifier,
    night: SleepNight?,
    loading: Boolean,
    onInfo: () -> Unit
) {
    DashboardGlassCard(
        modifier = modifier,
        title = "Last-Night Metrics",
        onInfo = onInfo
    ) {
        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp),
                contentAlignment =
                    Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Cyan
                )
            }
        } else if (night == null) {
            Text(
                modifier =
                    Modifier.padding(top = 14.dp),
                text =
                    "No recent sleep session yet.",
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val deep =
                stageMinutes(
                    night,
                    SleepStageType.DEEP
                )
            val light =
                stageMinutes(
                    night,
                    SleepStageType.LIGHT
                )
            val rem =
                stageMinutes(
                    night,
                    SleepStageType.REM
                )
            val unknown =
                stageMinutes(
                    night,
                    SleepStageType.UNKNOWN
                )
            val asleep =
                deep +
                    light +
                    rem +
                    unknown
            val total =
                java.time.Duration
                    .between(
                        night.start,
                        night.end
                    )
                    .toMinutes()
                    .coerceAtLeast(1L)
            val efficiency =
                (
                    asleep.toDouble() /
                        total.toDouble() *
                        100.0
                    )
                    .roundToInt()
                    .coerceIn(
                        0,
                        100
                    )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement =
                    Arrangement.SpaceEvenly
            ) {
                DashboardMetric(
                    label = "Sleep",
                    value =
                        formatMinutes(
                            asleep
                        ),
                    accent = Cyan
                )
                DashboardMetric(
                    label = "Efficiency",
                    value =
                        efficiency.toString() +
                            "%",
                    accent = Mint
                )
                DashboardMetric(
                    label = "REM",
                    value =
                        formatMinutes(
                            rem
                        ),
                    accent = Lavender
                )
            }
        }
    }
}

@Composable
private fun DashboardMetric(
    label: String,
    value: String,
    accent: Color
) {
    Column(
        modifier =
            Modifier.padding(horizontal = 6.dp),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(
                    accent,
                    CircleShape
                )
        )
        Text(
            modifier =
                Modifier.padding(top = 7.dp),
            text = value,
            fontWeight =
                FontWeight.ExtraBold,
            color =
                MaterialTheme.colorScheme.onSurface
        )
        Text(
            modifier =
                Modifier.padding(top = 2.dp),
            text = label,
            style =
                MaterialTheme.typography.labelSmall,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InsightDashboardTile(
    modifier: Modifier,
    text: String,
    onInfo: () -> Unit
) {
    DashboardGlassCard(
        modifier = modifier,
        title = "✨ Personal Insight",
        onInfo = onInfo
    ) {
        Text(
            modifier =
                Modifier.padding(top = 12.dp),
            text = text,
            style =
                MaterialTheme.typography.bodyLarge,
            color =
                MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SleepDebtDashboardTile(
    modifier: Modifier,
    analytics: com.kiranoommen.wakesync.domain.PeriodAnalytics,
    targetMinutes: Int,
    onInfo: () -> Unit
) {
    val debt =
        analytics.sleepDebtMinutes
    val avgGap =
        if (
            analytics.nights.isNotEmpty()
        ) {
            debt /
                analytics.nights.size
        } else {
            0L
        }

    DashboardGlassCard(
        modifier = modifier
            .height(178.dp),
        title = "Sleep Debt",
        onInfo = onInfo
    ) {
        Text(
            modifier =
                Modifier.padding(top = 16.dp),
            text =
                if (debt > 0) {
                    formatMinutes(debt)
                } else {
                    "0m"
                },
            style =
                MaterialTheme.typography.headlineMedium,
            fontWeight =
                FontWeight.ExtraBold,
            color =
                if (debt > 0) {
                    Sunrise
                } else {
                    Mint
                }
        )
        Text(
            text =
                if (debt > 0) {
                    "below target"
                } else {
                    "on target"
                },
            style =
                MaterialTheme.typography.bodySmall,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            modifier =
                Modifier.padding(top = 12.dp),
            text =
                "Avg gap " +
                    formatMinutes(
                        avgGap
                    ),
            style =
                MaterialTheme.typography.labelMedium,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SmartAlarmDashboardTile(
    modifier: Modifier,
    schedules: List<AlarmSchedule>,
    onEdit: (AlarmSchedule) -> Unit,
    onSkip: (AlarmSchedule) -> Unit,
    onGoAlarms: () -> Unit,
    onInfo: () -> Unit
) {
    val next =
        remember(schedules) {
            nextSchedule(schedules)
        }

    DashboardGlassCard(
        modifier = modifier
            .height(178.dp),
        title = "Smart Alarm",
        onInfo = onInfo
    ) {
        if (next == null) {
            Text(
                modifier =
                    Modifier.padding(top = 16.dp),
                text =
                    "No schedule",
                fontWeight =
                    FontWeight.ExtraBold
            )
            TextButton(
                onClick = onGoAlarms
            ) {
                Text("Set one")
            }
        } else {
            val schedule = next.first
            val deadline = next.second

            Text(
                modifier =
                    Modifier.padding(top = 13.dp),
                text =
                    deadline.format(
                        DateTimeFormatter.ofPattern(
                            "h:mm a"
                        )
                    ),
                style =
                    MaterialTheme.typography.headlineMedium,
                fontWeight =
                    FontWeight.ExtraBold
            )
            Text(
                text = "Must be awake by",
                style =
                    MaterialTheme.typography.bodySmall,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier =
                    Modifier.padding(top = 6.dp),
                horizontalArrangement =
                    Arrangement.spacedBy(2.dp)
            ) {
                TextButton(
                    onClick = {
                        onEdit(schedule)
                    }
                ) {
                    Text("Edit")
                }
                TextButton(
                    onClick = {
                        onSkip(schedule)
                    }
                ) {
                    Text(
                        if (
                            schedule.isNextOccurrenceSkipped()
                        ) {
                            "Undo"
                        } else {
                            "Skip"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyHypnogramDashboardTile(
    modifier: Modifier,
    nights: List<SleepNight>,
    onInfo: () -> Unit
) {
    val recent =
        nights
            .sortedBy { it.end }
            .takeLast(7)

    DashboardGlassCard(
        modifier = modifier,
        title = "Weekly Hypnogram Trend",
        onInfo = onInfo
    ) {
        if (recent.isEmpty()) {
            Text(
                modifier =
                    Modifier.padding(top = 14.dp),
                text =
                    "No tracked nights yet.",
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(105.dp)
                    .padding(top = 14.dp)
            ) {
                val widthPer =
                    size.width /
                        recent.size
                val barWidth =
                    widthPer * 0.48f

                recent.forEachIndexed {
                        index,
                        night ->
                    val stages = listOf(
                        SleepStageType.DEEP to Lavender,
                        SleepStageType.LIGHT to Cyan,
                        SleepStageType.REM to Indigo,
                        SleepStageType.AWAKE to Sunrise
                    )
                    val values =
                        stages.map {
                            stageMinutes(
                                night,
                                it.first
                            )
                        }
                    val total =
                        values.sum()
                            .coerceAtLeast(1L)

                    var bottom =
                        size.height

                    stages.zip(values)
                        .forEach {
                                pair ->
                            val value =
                                pair.second
                            val height =
                                size.height *
                                    value.toFloat() /
                                    total.toFloat()

                            drawRoundRect(
                                color =
                                    pair.first.second
                                        .copy(
                                            alpha = 0.88f
                                        ),
                                topLeft =
                                    Offset(
                                        widthPer *
                                            index +
                                            (
                                                widthPer -
                                                    barWidth
                                                ) /
                                                2f,
                                        bottom -
                                            height
                                    ),
                                size =
                                    Size(
                                        barWidth,
                                        height
                                    ),
                                cornerRadius =
                                    CornerRadius(
                                        4.dp.toPx(),
                                        4.dp.toPx()
                                    )
                            )
                            bottom -=
                                height
                        }
                }
            }

            Text(
                modifier =
                    Modifier.padding(top = 7.dp),
                text =
                    "Deep · Light · REM · Awake",
                style =
                    MaterialTheme.typography.bodySmall,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun wakeGlassBorderBrush(): Brush =
    if (
        MaterialTheme.colorScheme.background
            .luminance() < 0.5f
    ) {
        Brush.linearGradient(
            listOf(
                Color.White.copy(
                    alpha = 0.25f
                ),
                Cyan.copy(
                    alpha = 0.10f
                ),
                Color.White.copy(
                    alpha = 0.03f
                )
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color.White.copy(
                    alpha = 0.82f
                ),
                Cyan.copy(
                    alpha = 0.08f
                ),
                MaterialTheme.colorScheme.outlineVariant
                    .copy(
                        alpha = 0.22f
                    )
            )
        )
    }

private fun sleepDebtInfo(): MetricInfo =
    MetricInfo(
        title = "Sleep Debt vs Daily Score",
        meaning =
            "Sleep Debt tracks your cumulative sleep deficit over 7–14 days against your goal, whereas your Daily Score evaluates last night's individual sleep quality.",
        measurement =
            "WakeSync sums each tracked night's shortfall versus your selected sleep goal across the recent analysis period. The Daily Score is calculated separately from sleep-quality pillars.",
        importance =
            "You can have a decent individual night and still carry sleep debt from several shorter nights before it."
    )

@Composable
private fun HomeScoreGauge(
    modifier: Modifier,
    score: Int,
    color: Color,
    onClick: () -> Unit
) {
    val progress by animateFloatAsState(
        targetValue = score.coerceIn(0, 100) / 100f,
        animationSpec = spring(
            stiffness = 300f,
            dampingRatio = 0.78f
        ),
        label = "homeScoreGauge"
    )
    val track =
        MaterialTheme.colorScheme.onSurface
            .copy(alpha = 0.08f)

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val stroke = 8.dp.toPx()
            val diameter =
                size.minDimension - stroke

            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(
                    stroke / 2f,
                    stroke / 2f
                ),
                size = Size(
                    diameter,
                    diameter
                ),
                style = Stroke(
                    width = stroke,
                    cap = StrokeCap.Round
                )
            )

            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(
                    stroke / 2f,
                    stroke / 2f
                ),
                size = Size(
                    diameter,
                    diameter
                ),
                style = Stroke(
                    width = stroke,
                    cap = StrokeCap.Round
                )
            )
        }

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Text(
                text = score.toString(),
                style =
                    MaterialTheme.typography.titleLarge,
                fontWeight =
                    FontWeight.ExtraBold
            )
            Text(
                text = "score",
                style =
                    MaterialTheme.typography.labelSmall,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        }
    }
}

@Composable
private fun GoalStreakCard(
    analytics: com.kiranoommen.wakesync.domain.PeriodAnalytics,
    sleepGoalMinutes: Int,
    onInfo: () -> Unit
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
        shape = RoundedCornerShape(20.dp),
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

                Canvas(modifier = Modifier.matchParentSize()) {
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sleep goal",
                        color = Mint,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    InfoTrigger(onClick = onInfo)
                }
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
private fun HomeInsightCard(
    text: String,
    onInfo: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✨  WakeSync Insight",
                        fontWeight = FontWeight.ExtraBold,
                        color = Lavender
                    )
                    InfoTrigger(onClick = onInfo)
                }
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
    goalsEnabled: Boolean,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    var working by remember(current) {
        mutableStateOf(current)
    }

    val all = buildList {
        if (goalsEnabled) {
            add(
                AppSettingsStore.WIDGET_GOAL to
                    "Sleep Goal & Streak"
            )
        }

        add(
            AppSettingsStore.WIDGET_SLEEP to
                "Last-Night Metrics"
        )
        add(
            AppSettingsStore.WIDGET_INSIGHT to
                "Personal Insights"
        )

        if (goalsEnabled) {
            add(
                AppSettingsStore.WIDGET_DEBT to
                    "Sleep Debt & Deficit"
            )
        }

        add(
            AppSettingsStore.WIDGET_HYPNOGRAM to
                "Weekly Hypnogram Trend"
        )
        add(
            AppSettingsStore.WIDGET_ALARM to
                "Smart Alarm Status & Quick Controls"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Customize Your Dashboard")
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 500.dp)
                    .verticalScroll(
                        rememberScrollState()
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text =
                        "Toggle cards on or off. Long-press and drag the reorder handle to change the dashboard order.",
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )

                val labels =
                    all.toMap()
                val availableIds =
                    all.map { it.first }
                val visibleIds =
                    working.filter {
                        availableIds.contains(it)
                    }
                val hiddenIds =
                    availableIds.filterNot {
                        visibleIds.contains(it)
                    }
                val displayIds =
                    visibleIds + hiddenIds

                displayIds.forEach { id ->
                    val label =
                        labels[id] ?: id
                    val visible =
                        working.contains(id)
                    val index =
                        working.indexOf(id)

                    DashboardCustomizeRow(
                        label = label,
                        visible = visible,
                        orderIndex = index,
                        canDrag = visible,
                        onVisibleChange = {
                                checked ->
                            working =
                                if (checked) {
                                    working + id
                                } else {
                                    working.filterNot {
                                        it == id
                                    }
                                }
                        },
                        onMove = { direction ->
                            if (
                                index >= 0 &&
                                working.isNotEmpty()
                            ) {
                                val target =
                                    (index + direction)
                                        .coerceIn(
                                            0,
                                            working.lastIndex
                                        )

                                if (target != index) {
                                    val list =
                                        working.toMutableList()
                                    val item =
                                        list.removeAt(index)
                                    list.add(
                                        target,
                                        item
                                    )
                                    working = list
                                }
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(working)
                }
            ) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DashboardCustomizeRow(
    label: String,
    visible: Boolean,
    orderIndex: Int,
    canDrag: Boolean,
    onVisibleChange: (Boolean) -> Unit,
    onMove: (Int) -> Unit
) {
    val density = LocalDensity.current
    val threshold = with(density) {
        42.dp.toPx()
    }
    var dragOffset by remember {
        mutableStateOf(0f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = dragOffset
            }
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        border =
            if (
                MaterialTheme.colorScheme.background
                    .luminance() < 0.5f
            ) {
                BorderStroke(
                    1.dp,
                    Color.White.copy(
                        alpha = 0.08f
                    )
                )
            } else {
                null
            },
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surfaceVariant
                        .copy(alpha = 0.42f),
                contentColor =
                    MaterialTheme.colorScheme.onSurface
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 10.dp,
                    vertical = 8.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            if (canDrag) {
                Icon(
                    imageVector =
                        Icons.Default.Reorder,
                    contentDescription =
                        "Reorder $label",
                    modifier = Modifier
                        .size(28.dp)
                        .pointerInput(
                            label,
                            visible,
                            orderIndex
                        ) {
                            detectDragGesturesAfterLongPress(
                                onDragEnd = {
                                    dragOffset = 0f
                                },
                                onDragCancel = {
                                    dragOffset = 0f
                                },
                                onDrag = {
                                        _,
                                        dragAmount ->
                                    dragOffset +=
                                        dragAmount.y

                                    if (
                                        dragOffset >=
                                        threshold
                                    ) {
                                        onMove(1)
                                        dragOffset = 0f
                                    } else if (
                                        dragOffset <=
                                        -threshold
                                    ) {
                                        onMove(-1)
                                        dragOffset = 0f
                                    }
                                }
                            )
                        },
                    tint =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(
                    modifier =
                        Modifier.width(28.dp)
                )
            }

            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
                text = label,
                fontWeight =
                    FontWeight.Medium,
                color =
                    MaterialTheme.colorScheme.onSurface
            )

            GlassSwitch(
                checked = visible,
                enabled = true,
                onCheckedChange =
                    onVisibleChange
            )
        }
    }
}

@Composable
private fun NextWakeCard(
    schedule: AlarmSchedule,
    deadline: ZonedDateTime,
    onEdit: () -> Unit,
    onSkip: () -> Unit,
    onInfo: () -> Unit
) {
    val timeFormat = DateTimeFormatter.ofPattern("h:mm a")
    val dateFormat = DateTimeFormatter.ofPattern("EEE, MMM d")
    val windowStart =
        if (schedule.mode == AlarmMode.SMART_WAKE) {
            deadline.minusMinutes(schedule.smartWindowMinutes.toLong())
        } else {
            deadline
        }
    val predicted = deadline.minusMinutes(schedule.smartOffsetMinutes.toLong())
    val editInteraction = remember { MutableInteractionSource() }
    val pressed by editInteraction.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(
            stiffness = 300f,
            dampingRatio = 0.78f
        ),
        label = "editSchedulePress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                            text =
                                if (schedule.mode == AlarmMode.SMART_WAKE) {
                                    "✨  SMART WAKE"
                                } else {
                                    "⏰  STANDARD ALARM"
                                },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    InfoTrigger(onClick = onInfo)
                }

                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text =
                        if (schedule.mode == AlarmMode.SMART_WAKE) {
                            windowStart.format(timeFormat) +
                                " – " +
                                deadline.format(timeFormat)
                        } else {
                            deadline.format(timeFormat)
                        },
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = deadline.format(dateFormat),
                    color = Color.White.copy(alpha = 0.80f)
                )

                if (schedule.backupRingCount > 0) {
                    Text(
                        modifier = Modifier.padding(top = 4.dp),
                        text =
                            schedule.backupRingCount.toString() +
                                " backup alarm" +
                                if (schedule.backupRingCount == 1) {
                                    ""
                                } else {
                                    "s"
                                } +
                                " · every 5 min after " +
                                deadline.format(timeFormat),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.68f)
                    )
                }

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
                                text = when {
                                    schedule.mode == AlarmMode.STANDARD ->
                                        "Standard alarm"
                                    schedule.smartOffsetMinutes > 0 ->
                                        "Historical fallback forecast"
                                    else ->
                                        "Must be awake by"
                                },
                                color = Color.White.copy(alpha = 0.72f),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                modifier = Modifier.padding(top = 2.dp),
                                text =
                                    if (
                                        schedule.mode == AlarmMode.SMART_WAKE &&
                                        schedule.smartOffsetMinutes > 0
                                    ) {
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
                                text = "● Scheduled",
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

                    if (schedule.oneTimeDate == null) {
                        OutlinedButton(
                            onClick = onSkip,
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(
                                1.dp,
                                Color.White.copy(alpha = 0.28f)
                            ),
                            colors =
                                ButtonDefaults.outlinedButtonColors(
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
}

@Composable
private fun EmptyAlarmCard(
    onGoAlarms: () -> Unit,
    onInfo: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(
            stiffness = 300f,
            dampingRatio = 0.78f
        ),
        label = "setSchedulePress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                    InfoTrigger(onClick = onInfo)
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
                    text = "Set your Must be awake by — the latest time the alarm will sound. WakeSync can wake you gently inside the Smart Wake Window before it.",
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
    onDuplicate: (AlarmSchedule) -> Unit,
    onToggle: (AlarmSchedule, Boolean) -> Unit,
    onSkipNext: (AlarmSchedule) -> Unit,
    onClearSkips: (AlarmSchedule) -> Unit
) {
    var infoSheet by remember {
        mutableStateOf<MetricInfo?>(null)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onAdd,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF0F172A)
                )
            ) {
                Text(
                    modifier = Modifier.padding(vertical = 5.dp),
                    text = "+ Add alarm",
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        if (schedules.isEmpty()) {
            item {
                InfoCard(
                    title = "No alarms yet",
                    body = "Tap + Add alarm to create a Standard Alarm or turn on Smart Wake."
                )
            }
        }

        items(schedules, key = { it.id }) { schedule ->
            SwipeableAlarmScheduleCard(
                schedule = schedule,
                onEdit = { onEdit(schedule) },
                onDuplicate = { onDuplicate(schedule) },
                onToggle = { onToggle(schedule, it) },
                onSkip = { onSkipNext(schedule) },
                onClearSkips = { onClearSkips(schedule) },
                onInfo = {
                    infoSheet = MetricInfo(
                        title = "Wake schedule",
                        meaning = "A wake schedule can be tomorrow-only or recurring, with optional Smart Wake flexibility.",
                        measurement = "WakeSync stores the selected schedule, must-be-awake time and allowed early-wake window locally, then schedules the protected alarm with Android.",
                        importance = "Swipe right to quickly turn an alarm on or off. Swipe left to duplicate it and edit the copy."
                    )
                }
            )
        }

        item { Spacer(Modifier.height(10.dp)) }
    }

    MetricInfoBottomSheet(
        info = infoSheet,
        onDismiss = {
            infoSheet = null
        }
    )
}

@Composable
private fun WeeklyAlarmOverview(
    schedules: List<AlarmSchedule>,
    onInfo: () -> Unit
) {
    val today = LocalDate.now()
    val dates = remember(today) {
        (0L..6L).map {
            today.plusDays(it)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.dp,
            wakeGlassBorderBrush()
        ),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
            contentColor =
                MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            WakeGlassBackdrop(
                modifier = Modifier.matchParentSize()
            )

            Column(
                modifier = Modifier.padding(16.dp)
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Your week",
                        style =
                            MaterialTheme.typography.titleMedium,
                        fontWeight =
                            FontWeight.ExtraBold,
                        color =
                            MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        modifier =
                            Modifier.padding(top = 2.dp),
                        text =
                            "Today through " +
                                dates.last().format(
                                    DateTimeFormatter.ofPattern(
                                        "EEE, MMM d"
                                    )
                                ),
                        style =
                            MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                InfoTrigger(onClick = onInfo)
            }

            Row(
                modifier = Modifier
                    .horizontalScroll(
                        rememberScrollState()
                    )
                    .padding(top = 14.dp),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                dates.forEachIndexed {
                        index,
                        date ->
                    val schedule =
                        schedules
                            .asSequence()
                            .filter {
                                it.enabled &&
                                    it.isBaseScheduledOn(date)
                            }
                            .minByOrNull {
                                it.hour * 60 +
                                    it.minute
                            }

                    Card(
                        modifier =
                            Modifier.width(84.dp),
                        shape =
                            RoundedCornerShape(16.dp),
                        border =
                            if (
                                MaterialTheme.colorScheme.background
                                    .luminance() < 0.5f
                            ) {
                                BorderStroke(
                                    1.dp,
                                    if (schedule != null) {
                                        Lavender.copy(
                                            alpha = 0.28f
                                        )
                                    } else {
                                        Color.White.copy(
                                            alpha = 0.08f
                                        )
                                    }
                                )
                            } else {
                                null
                            },
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    if (
                                        schedule != null
                                    ) {
                                        Lavender.copy(
                                            alpha =
                                                if (
                                                    MaterialTheme.colorScheme.background
                                                        .luminance() < 0.5f
                                                ) {
                                                    0.10f
                                                } else {
                                                    0.07f
                                                }
                                        )
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                            .copy(
                                                alpha = 0.28f
                                            )
                                    },
                                contentColor =
                                    MaterialTheme.colorScheme.onSurface
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 8.dp,
                                    vertical = 11.dp
                                ),
                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {
                            Text(
                                text =
                                    if (index == 0) {
                                        "Today"
                                    } else {
                                        date.format(
                                            DateTimeFormatter.ofPattern(
                                                "EEE"
                                            )
                                        )
                                    },
                                style =
                                    MaterialTheme.typography.labelMedium,
                                fontWeight =
                                    FontWeight.Bold
                            )
                            Text(
                                modifier =
                                    Modifier.padding(top = 2.dp),
                                text =
                                    date.format(
                                        DateTimeFormatter.ofPattern(
                                            "MMM d"
                                        )
                                    ),
                                style =
                                    MaterialTheme.typography.labelSmall,
                                color =
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                modifier =
                                    Modifier.padding(top = 7.dp),
                                text =
                                    if (
                                        schedule != null
                                    ) {
                                        formatClock(
                                            schedule.hour,
                                            schedule.minute
                                        )
                                    } else {
                                        "Off"
                                    },
                                style =
                                    MaterialTheme.typography.labelSmall,
                                color =
                                    if (
                                        schedule != null
                                    ) {
                                        Lavender
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                fontWeight =
                                    FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

}
@Composable
private fun SwipeableAlarmScheduleCard(
    schedule: AlarmSchedule,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onSkip: () -> Unit,
    onClearSkips: () -> Unit,
    onInfo: () -> Unit
) {
    val haptics = LocalHapticFeedback.current

    val swipeState =
        rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                when (value) {
                    SwipeToDismissBoxValue.StartToEnd -> {
                        haptics.performHapticFeedback(
                            HapticFeedbackType.LongPress
                        )
                        onToggle(!schedule.enabled)
                        false
                    }

                    SwipeToDismissBoxValue.EndToStart -> {
                        haptics.performHapticFeedback(
                            HapticFeedbackType.LongPress
                        )
                        onDuplicate()
                        false
                    }

                    SwipeToDismissBoxValue.Settled ->
                        false
                }
            }
        )

    SwipeToDismissBox(
        state = swipeState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val towardStart =
                swipeState.targetValue ==
                    SwipeToDismissBoxValue.EndToStart

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 22.dp),
                horizontalArrangement =
                    if (towardStart) {
                        Arrangement.End
                    } else {
                        Arrangement.Start
                    },
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Text(
                    text =
                        if (towardStart) {
                            "Duplicate"
                        } else if (schedule.enabled) {
                            "Turn off"
                        } else {
                            "Turn on"
                        },
                    fontWeight = FontWeight.ExtraBold,
                    color =
                        if (towardStart) {
                            Lavender
                        } else if (schedule.enabled) {
                            Sunrise
                        } else {
                            Mint
                        }
                )
            }
        }
    ) {
        AlarmScheduleCard(
            schedule = schedule,
            onEdit = onEdit,
            onDuplicate = onDuplicate,
            onToggle = onToggle,
            onSkip = onSkip,
            onClearSkips = onClearSkips,
            onInfo = onInfo
        )
    }
}

@Composable
private fun AlarmScheduleCard(
    schedule: AlarmSchedule,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onSkip: () -> Unit,
    onClearSkips: () -> Unit,
    onInfo: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var showDisableChoice by remember { mutableStateOf(false) }
    val nextBase = schedule.nextBaseDeadline()
    val nextActual = schedule.nextDeadline()
    val nextSkipped = schedule.isNextOccurrenceSkipped()
    val futureSkips = schedule.futureSkippedDates()
    val cardScale by animateFloatAsState(
        targetValue = if (schedule.enabled) 1f else 0.985f,
        animationSpec = spring(
            stiffness = 300f,
            dampingRatio = 0.78f
        ),
        label = "alarmEnabledScale" + schedule.id
    )
    val cardContent by animateColorAsState(
        targetValue = if (schedule.enabled) colors.onSurface else colors.onSurfaceVariant,
        label = "alarmContentColor" + schedule.id
    )
    val time = String.format(
        "%d:%02d %s",
        if (schedule.hour % 12 == 0) 12 else schedule.hour % 12,
        schedule.minute,
        if (schedule.hour < 12) "AM" else "PM"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(cardScale)
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.dp,
            wakeGlassBorderBrush()
        ),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
            contentColor = cardContent
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            WakeGlassBackdrop(
                modifier = Modifier.matchParentSize()
            )

            if (schedule.enabled) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawCircle(
                        color = Lavender.copy(alpha = 0.08f),
                        radius = size.minDimension * 0.70f,
                        center = Offset(size.width * 0.96f, size.height * 0.05f)
                    )
                }
            }

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
                            fontWeight = FontWeight.ExtraBold,
                            color = cardContent
                        )
                        Text(
                            modifier = Modifier.padding(top = 2.dp),
                            text =
                                if (schedule.mode == AlarmMode.SMART_WAKE) {
                                    "Smart Wake"
                                } else {
                                    "Standard"
                                },
                            color = colors.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InfoTrigger(onClick = onInfo)
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
                }

                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(999.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Lavender.copy(alpha = 0.13f),
                            contentColor = Lavender
                        )
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            text = if (schedule.smartWindowMinutes == 0) {
                                "Exact time"
                            } else {
                                schedule.smartWindowMinutes.toString() + "m smart window"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Card(
                        shape = RoundedCornerShape(999.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = colors.surfaceVariant.copy(alpha = 0.60f),
                            contentColor = colors.onSurfaceVariant
                        )
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            text = scheduleDaysLabel(schedule),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (schedule.enabled) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, colors.outlineVariant),
                        colors = CardDefaults.cardColors(
                            containerColor = colors.surfaceVariant.copy(alpha = 0.46f),
                            contentColor = colors.onSurface
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .background(
                                        color = if (nextSkipped) Sunrise else Mint,
                                        shape = CircleShape
                                    )
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 10.dp)
                            ) {
                                when {
                                    nextSkipped && nextBase != null -> {
                                        Text(
                                            text = "Next occurrence skipped",
                                            fontWeight = FontWeight.Bold,
                                            color = Sunrise
                                        )
                                        Text(
                                            modifier = Modifier.padding(top = 2.dp),
                                            text = nextBase.format(
                                                DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a")
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.onSurfaceVariant
                                        )
                                        if (nextActual != null) {
                                            Text(
                                                modifier = Modifier.padding(top = 2.dp),
                                                text = "Resumes " + nextActual.format(
                                                    DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a")
                                                ),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.onSurface
                                            )
                                        }
                                    }

                                    nextActual != null -> {
                                        Text(
                                            text = "Next alarm",
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            modifier = Modifier.padding(top = 2.dp),
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
                    }

                    Row(
                        modifier = Modifier.padding(top = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (schedule.oneTimeDate == null) {
                            TextButton(onClick = onSkip) {
                            Text(
                                if (nextSkipped) "Undo skip" else "Skip next",
                                color = Lavender
                            )
                            }
                        }

                        if (
                            schedule.oneTimeDate == null &&
                            futureSkips.size > 1
                        ) {
                            TextButton(onClick = onClearSkips) {
                                Text("Clear skips", color = colors.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDisableChoice) {
        AlertDialog(
            onDismissRequest = { showDisableChoice = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = colors.surface,
            titleContentColor = colors.onSurface,
            textContentColor = colors.onSurfaceVariant,
            title = {
                Text(
                    when {
                        schedule.oneTimeDate != null ->
                            "Turn this alarm off?"
                        nextSkipped ->
                            "Turn this schedule off?"
                        else ->
                            "Skip once or turn schedule off?"
                    }
                )
            },
            text = {
                Text(
                    when {
                        schedule.oneTimeDate != null ->
                            "This tomorrow-only alarm will stay available but will not ring until you turn it back on or edit its schedule."
                        nextSkipped ->
                            "The next occurrence is already skipped. Turning this off disables the recurring schedule until you switch it back on."
                        else ->
                            "Skip once keeps this recurring schedule active and automatically resumes it on the next matching day."
                    }
                )
            },
            confirmButton = {
                if (
                    schedule.oneTimeDate == null &&
                    !nextSkipped
                ) {
                    Button(
                        onClick = {
                            onSkip()
                            showDisableChoice = false
                        },
                        shape = RoundedCornerShape(999.dp)
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
    goalsEnabled: Boolean,
    onRefresh: () -> Unit
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
                goalsEnabled = goalsEnabled,
                onRefresh = onRefresh
            )
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun SettingsTab(
    hasPermission: Boolean,
    exactAlarmAccess: Boolean,
    smartWakeReadiness: SmartWakeReadiness,
    alarmVolumePercent: Int,
    hasHistoryPermission: Boolean,
    historyReadAvailable: Boolean,
    themeMode: String,
    sleepGoalMinutes: Int,
    goalsEnabled: Boolean,
    nights: List<SleepNight>,
    onConnect: () -> Unit,
    onRequestHistoryAccess: () -> Unit,
    onRequestExactAlarmAccess: () -> Unit,
    onRequestBackgroundSmartWakeAccess: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenFullScreenSettings: () -> Unit,
    onTestAlarm: () -> Unit,
    onOpenAlarmVolumeSettings: () -> Unit,
    onThemeModeChange: (String) -> Unit,
    onSleepGoalChange: (Int) -> Unit,
    onGoalsEnabledChange: (Boolean) -> Unit
) {
    var infoSheet by remember {
        mutableStateOf<MetricInfo?>(null)
    }
    var showSleepTargetPicker by remember {
        mutableStateOf(false)
    }
    var sleepTargetDraft by remember(
        sleepGoalMinutes
    ) {
        mutableStateOf(
            sleepGoalMinutes
        )
    }
    val settingsContext =
        LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {
        item {
            AlarmReliabilityCard(
                readiness = smartWakeReadiness,
                alarmVolumePercent = alarmVolumePercent,
                onGrantExactAlarmAccess =
                    onRequestExactAlarmAccess,
                onRequestBackgroundSmartWakeAccess =
                    onRequestBackgroundSmartWakeAccess,
                onOpenNotificationSettings =
                    onOpenNotificationSettings,
                onOpenFullScreenSettings =
                    onOpenFullScreenSettings,
                onOpenAlarmVolumeSettings =
                    onOpenAlarmVolumeSettings,
                onTestAlarm = onTestAlarm
            )
        }

        item {
            SettingsBentoCard(
                title = "Targets & Goals",
                onInfo = {
                    infoSheet = MetricInfo(
                        title = "Sleep targets & goals",
                        meaning = "Optional personal targets for nightly sleep duration, goal progress and sleep-debt estimates.",
                        measurement = "When enabled, WakeSync stores your 4–12 hour target as total minutes in 15-minute increments and uses that exact value for goal progress and sleep-debt calculations. When disabled, target and debt indicators are hidden.",
                        importance = "Some people find targets motivating; others prefer neutral duration and efficiency trends. WakeSync supports both."
                    )
                }
            ) {
                SettingsToggleRow(
                    title = "Enable Sleep Targets & Goals",
                    subtitle =
                        if (goalsEnabled) {
                            "Target progress and sleep-debt indicators are shown"
                        } else {
                            "WakeSync shows duration and efficiency without target/debt labels"
                        },
                    checked = goalsEnabled,
                    enabled = true,
                    onCheckedChange = onGoalsEnabledChange
                )

                if (goalsEnabled) {
                    Text(
                        text = "Sleep target",
                        style =
                            MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Card(
                        onClick = {
                            sleepTargetDraft =
                                sleepGoalMinutes
                            showSleepTargetPicker =
                                true
                        },
                        modifier =
                            Modifier.fillMaxWidth(),
                        shape =
                            RoundedCornerShape(18.dp),
                        border =
                            if (
                                MaterialTheme.colorScheme.background
                                    .luminance() <
                                    0.5f
                            ) {
                                BorderStroke(
                                    1.dp,
                                    Color.White.copy(
                                        alpha = 0.10f
                                    )
                                )
                            } else {
                                null
                            },
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme.colorScheme.surfaceVariant
                                        .copy(
                                            alpha =
                                                if (
                                                    MaterialTheme.colorScheme.background
                                                        .luminance() <
                                                        0.5f
                                                ) {
                                                    0.34f
                                                } else {
                                                    0.24f
                                                }
                                        ),
                                contentColor =
                                    MaterialTheme.colorScheme.onSurface
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 16.dp,
                                    vertical = 14.dp
                                ),
                            horizontalArrangement =
                                Arrangement.SpaceBetween,
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text =
                                        "Nightly target",
                                    style =
                                        MaterialTheme.typography.labelMedium,
                                    color =
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    modifier =
                                        Modifier.padding(
                                            top = 2.dp
                                        ),
                                    text =
                                        formatSleepTarget(
                                            sleepGoalMinutes
                                        ),
                                    style =
                                        MaterialTheme.typography.titleLarge,
                                    fontWeight =
                                        FontWeight.ExtraBold
                                )
                            }

                            Text(
                                text = "Tap to adjust",
                                style =
                                    MaterialTheme.typography.labelSmall,
                                color = Lavender,
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text =
                            "Tap the target field to choose any duration from 4h 00m to 12h 00m in 15-minute steps. Changes apply only after you confirm.",
                        style =
                            MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text =
                            "Targets are off. Sleep views now emphasize actual sleep duration, efficiency, stages and consistency.",
                        style =
                            MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            SettingsBentoCard(
                title = "Data Source Integration",
                onInfo = {
                    infoSheet = MetricInfo(
                        title = "Health Connect data source",
                        meaning = "WakeSync reads sleep records through Android Health Connect rather than talking directly to every wearable vendor.",
                        measurement = "The source package attached to the latest sleep session identifies which connected app wrote the record.",
                        importance = "One Health Connect integration keeps WakeSync wearable-agnostic across supported Android health ecosystems."
                    )
                }
            ) {
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Column(
                        modifier =
                            Modifier.weight(1f)
                    ) {
                        Text(
                            text =
                                if (hasPermission) {
                                    "Health Connect Integration"
                                } else {
                                    "Health Connect Integration"
                                },
                            style =
                                MaterialTheme.typography.titleMedium,
                            fontWeight =
                                FontWeight.ExtraBold,
                            color =
                                MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            modifier =
                                Modifier.padding(
                                    top = 3.dp
                                ),
                            text =
                                if (hasPermission) {
                                    "Read-only sleep connection"
                                } else {
                                    "Sleep permission not granted"
                                },
                            style =
                                MaterialTheme.typography.bodySmall,
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            modifier =
                                Modifier.padding(
                                    top = 7.dp
                                ),
                            text =
                                "Compatible with Pixel Watch, Galaxy Watch, Garmin, Oura, Fitbit, and all Health Connect wearables.",
                            style =
                                MaterialTheme.typography.bodySmall,
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    ConnectionBadge(
                        connected =
                            hasPermission
                    )
                }

                if (!hasPermission) {
                    Button(
                        modifier =
                            Modifier.padding(top = 12.dp),
                        onClick = onConnect,
                        shape =
                            RoundedCornerShape(999.dp)
                    ) {
                        Text("Connect")
                    }
                }

                if (historyReadAvailable) {
                    SettingsToggleRow(
                        title = "Extended history",
                        subtitle =
                            "Allows longer historical ranges",
                        checked =
                            hasHistoryPermission,
                        enabled =
                            !hasHistoryPermission,
                        onCheckedChange = {
                            if (
                                it &&
                                !hasHistoryPermission
                            ) {
                                onRequestHistoryAccess()
                            }
                        }
                    )
                }
            }
        }

        item {
            SettingsBentoCard(
                title = "Privacy",
                onInfo = {
                    infoSheet = MetricInfo(
                        title = "On-device privacy",
                        meaning = "WakeSync reads permitted Health Connect data locally and does not require an account or health-data cloud.",
                        measurement = "Alarm decisions, sleep context, and wake history remain on this device.",
                        importance = "The app only asks for data needed to support Smart Wake and its local sleep context."
                    )
                }
            ) {
                Text(
                    text = "WakeSync processes Smart Wake and sleep context on this device. Raw Health Connect data is not copied into a WakeSync cloud database.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            SettingsBentoCard(
                title = "Appearance",
                onInfo = {
                    infoSheet = MetricInfo(
                        title = "Appearance",
                        meaning = "Choose whether WakeSync follows Android's appearance or forces its dark/light theme.",
                        measurement = "The selected mode changes Compose color roles only; it does not affect sleep calculations.",
                        importance = "High-contrast color roles preserve readability in both themes."
                    )
                }
            ) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(
                            rememberScrollState()
                        ),
                    horizontalArrangement =
                        Arrangement.spacedBy(7.dp)
                ) {
                    listOf(
                        AppSettingsStore.THEME_SYSTEM
                            to "System",
                        AppSettingsStore.THEME_DARK
                            to "Dark",
                        AppSettingsStore.THEME_LIGHT
                            to "Light"
                    ).forEach {
                            (value, label) ->
                        FilterChip(
                            selected =
                                themeMode == value,
                            onClick = {
                                onThemeModeChange(
                                    value
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

        item {
            Text(
                text = "About & Support",
                style =
                    MaterialTheme.typography.titleLarge,
                fontWeight =
                    FontWeight.ExtraBold,
                color =
                    MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            SettingsBentoCard(
                title = "Support WakeSync",
                borderColor =
                    Lavender.copy(alpha = 0.30f),
                onInfo = {
                    infoSheet = MetricInfo(
                        title = "Support WakeSync",
                        meaning = "WakeSync is built privacy-first with no ads. Donations are optional and do not unlock features.",
                        measurement = "The PayPal button opens the official WakeSync donation page in your browser.",
                        importance = "Optional support can help fund future development while keeping the app free of advertising."
                    )
                }
            ) {
                Text(
                    text =
                        "WakeSync is built privacy-first with no ads. If you find it helpful, consider supporting future development!",
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    modifier =
                        Modifier.fillMaxWidth(),
                    onClick = {
                        val intent =
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(
                                    PAYPAL_DONATION_URL
                                )
                            )
                        settingsContext.startActivity(
                            intent
                        )
                    },
                    shape =
                        RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = "Donate via PayPal",
                        fontWeight =
                            FontWeight.ExtraBold
                    )
                }

            }
        }

        item {
            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )
        }
    }

    if (showSleepTargetPicker) {
        AlertDialog(
            onDismissRequest = {
                showSleepTargetPicker =
                    false
                sleepTargetDraft =
                    sleepGoalMinutes
            },
            title = {
                Text(
                    text =
                        "Choose sleep target",
                    fontWeight =
                        FontWeight.ExtraBold
                )
            },
            text = {
                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(
                            10.dp
                        )
                ) {
                    Text(
                        text =
                            "Scroll to your preferred nightly sleep target.",
                        style =
                            MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    SleepTargetWheel(
                        selectedMinutes =
                            sleepTargetDraft,
                        onSelected = {
                            sleepTargetDraft =
                                it
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSleepGoalChange(
                            sleepTargetDraft
                        )
                        showSleepTargetPicker =
                            false
                    },
                    shape =
                        RoundedCornerShape(
                            999.dp
                        )
                ) {
                    Text(
                        text = "✓ Confirm",
                        fontWeight =
                            FontWeight.ExtraBold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        sleepTargetDraft =
                            sleepGoalMinutes
                        showSleepTargetPicker =
                            false
                    }
                ) {
                    Text(
                        text = "✕ Cancel",
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        )
    }

    MetricInfoBottomSheet(
        info = infoSheet,
        onDismiss = {
            infoSheet = null
        }
    )
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun SleepTargetWheel(
    selectedMinutes: Int,
    onSelected: (Int) -> Unit
) {
    val options =
        remember {
            (240..720 step 15).toList()
        }
    val selectedIndex =
        options.indexOf(
            selectedMinutes
                .coerceIn(240, 720)
        )
            .coerceAtLeast(0)
    val listState =
        rememberLazyListState(
            initialFirstVisibleItemIndex =
                selectedIndex
        )
    val fling =
        rememberSnapFlingBehavior(
            lazyListState =
                listState
        )

    LaunchedEffect(
        listState.isScrollInProgress,
        options
    ) {
        if (!listState.isScrollInProgress) {
            val layout =
                listState.layoutInfo
            val center =
                (
                    layout.viewportStartOffset +
                        layout.viewportEndOffset
                    ) / 2
            val item =
                layout.visibleItemsInfo
                    .minByOrNull {
                        abs(
                            (
                                it.offset +
                                    it.size / 2
                                ) -
                                center
                        )
                    }
            item?.index
                ?.takeIf {
                    it in
                        options.indices
                }
                ?.let { index ->
                    val minutes =
                        options[index]
                    if (
                        minutes !=
                        selectedMinutes
                    ) {
                        onSelected(
                            minutes
                        )
                    }
                }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(168.dp),
        contentAlignment =
            Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape =
                RoundedCornerShape(18.dp),
            border = BorderStroke(
                1.dp,
                Lavender.copy(
                    alpha = 0.38f
                )
            ),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        Lavender.copy(
                            alpha = 0.12f
                        )
                )
        ) {
            Box(
                modifier =
                    Modifier.fillMaxSize()
            )
        }

        LazyColumn(
            modifier =
                Modifier.fillMaxSize(),
            state = listState,
            flingBehavior = fling,
            contentPadding =
                PaddingValues(
                    vertical = 58.dp
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            itemsIndexed(
                options,
                key = {
                        _,
                        minutes ->
                    minutes
                }
            ) {
                    _,
                    minutes ->
                val distance =
                    abs(
                        minutes -
                            selectedMinutes
                    ) / 15
                val alpha =
                    when (distance) {
                        0 -> 1f
                        1 -> 0.62f
                        else -> 0.34f
                    }
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(top = 13.dp),
                    text =
                        formatSleepTarget(
                            minutes
                        ),
                    style =
                        MaterialTheme.typography.titleLarge,
                    fontWeight =
                        if (
                            minutes ==
                            selectedMinutes
                        ) {
                            FontWeight.ExtraBold
                        } else {
                            FontWeight.Medium
                        },
                    color =
                        MaterialTheme.colorScheme.onSurface
                            .copy(alpha = alpha),
                    textAlign =
                        androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(42.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface
                                .copy(alpha = 0.88f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(42.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface
                                .copy(alpha = 0.88f)
                        )
                    )
                )
        )
    }
}

private fun formatSleepTarget(
    minutes: Int
): String {
    val hours =
        minutes / 60
    val remainder =
        minutes % 60
    return hours.toString() +
        "h " +
        remainder.toString()
            .padStart(
                2,
                '0'
            ) +
        "m"
}

@Composable
private fun SettingsBentoCard(
    title: String,
    borderColor: Color =
        MaterialTheme.colorScheme.outlineVariant,
    onInfo: () -> Unit,
    content: @Composable () -> Unit
) {
    val isDark =
        MaterialTheme.colorScheme.background
            .luminance() < 0.5f
    val accentBrush =
        if (
            borderColor !=
            MaterialTheme.colorScheme.outlineVariant
        ) {
            Brush.linearGradient(
                listOf(
                    borderColor.copy(
                        alpha = 0.28f
                    ),
                    Color.White.copy(
                        alpha =
                            if (isDark) {
                                0.16f
                            } else {
                                0.70f
                            }
                    ),
                    borderColor.copy(
                        alpha = 0.04f
                    )
                )
            )
        } else {
            wakeGlassBorderBrush()
        }

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.dp,
            accentBrush
        ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.Transparent,
                contentColor =
                    MaterialTheme.colorScheme.onSurface
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    if (isDark) {
                        8.dp
                    } else {
                        2.dp
                    }
            )
    ) {
        Box(
            modifier =
                Modifier.fillMaxWidth()
        ) {
            WakeGlassBackdrop(
                modifier =
                    Modifier.matchParentSize()
            )

            Column(
                modifier =
                    Modifier.padding(18.dp),
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style =
                            MaterialTheme.typography.titleMedium,
                        fontWeight =
                            FontWeight.ExtraBold,
                        color =
                            MaterialTheme.colorScheme.onSurface
                    )
                    InfoTrigger(
                        onClick = onInfo
                    )
                }

                content()
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement =
            Arrangement.SpaceBetween,
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Column(
            modifier =
                Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontWeight =
                    FontWeight.SemiBold
            )
            Text(
                modifier =
                    Modifier.padding(top = 2.dp),
                text = subtitle,
                style =
                    MaterialTheme.typography.bodySmall,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        GlassSwitch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun GlassSwitch(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val trackColor by animateColorAsState(
        targetValue = when {
            checked ->
                IndigoGlow.copy(alpha = if (enabled) 0.92f else 0.72f)
            else ->
                MaterialTheme.colorScheme.surfaceVariant
        },
        label = "glassSwitchTrack"
    )
    val knobOffset by animateFloatAsState(
        targetValue = if (checked) 22f else 2f,
        animationSpec = spring(
            stiffness = 300f,
            dampingRatio = 0.78f
        ),
        label = "glassSwitchKnob"
    )

    Card(
        modifier = Modifier
            .width(52.dp)
            .height(30.dp)
            .clickable(
                enabled = enabled,
                onClick = {
                    onCheckedChange(!checked)
                }
            ),
        shape = RoundedCornerShape(999.dp),
        border =
            if (
                MaterialTheme.colorScheme.background
                    .luminance() < 0.5f
            ) {
                BorderStroke(
                    1.dp,
                    if (checked) {
                        Lavender.copy(alpha = 0.36f)
                    } else {
                        Color.White.copy(alpha = 0.08f)
                    }
                )
            } else {
                null
            },
        colors = CardDefaults.cardColors(
            containerColor = trackColor,
            contentColor = Color.White
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .offset(x = knobOffset.dp)
                    .size(26.dp)
                    .background(
                        color = if (enabled) {
                            Color.White
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                                .copy(alpha = 0.55f)
                        },
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun ConnectionBadge(
    connected: Boolean,
    connectedLabel: String = "Live",
    disconnectedLabel: String = "Off"
) {
    val color =
        if (connected) {
            Mint
        } else {
            Sunrise
        }
    val transition =
        rememberInfiniteTransition(
            label = "connectionPulse"
        )
    val pulse by transition.animateFloat(
        initialValue = if (connected) 0.42f else 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(
                durationMillis = 900
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "connectionPulseAlpha"
    )

    Card(
        shape =
            RoundedCornerShape(999.dp),
        border = BorderStroke(
            1.dp,
            color.copy(alpha = 0.30f)
        ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    color.copy(alpha = 0.12f),
                contentColor = color
            )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 6.dp
            ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(
                        color = color.copy(
                            alpha =
                                if (connected) {
                                    pulse
                                } else {
                                    1f
                                }
                        ),
                        shape = CircleShape
                    )
            )
            Text(
                modifier =
                    Modifier.padding(start = 6.dp),
                text =
                    if (connected) {
                        connectedLabel
                    } else {
                        disconnectedLabel
                    },
                style =
                    MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun BottomNav(
    pagerPosition: Float,
    onSelected: (AppTab) -> Unit
) {
    val colors =
        MaterialTheme.colorScheme
    val isDark =
        colors.background
            .luminance() < 0.5f
    val tabs =
        listOf(
            Triple(
                AppTab.ALARMS,
                "Alarms",
                Icons.Default.AccessAlarm
            ),
            Triple(
                AppTab.SETTINGS,
                "Settings",
                Icons.Default.Settings
            )
        )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 8.dp,
                end = 8.dp,
                top = 8.dp,
                bottom = 20.dp
            ),
        shape =
            RoundedCornerShape(999.dp),
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(
                    Color.White.copy(
                        alpha =
                            if (isDark) {
                                0.22f
                            } else {
                                0.74f
                            }
                    ),
                    Color.White.copy(
                        alpha =
                            if (isDark) {
                                0.035f
                            } else {
                                0.16f
                            }
                    )
                )
            )
        ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.Transparent,
                contentColor =
                    colors.onSurface
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    if (isDark) {
                        8.dp
                    } else {
                        3.dp
                    }
            )
    ) {
        Box(
            modifier =
                Modifier.fillMaxWidth()
        ) {
            WakeGlassBackdrop(
                modifier =
                    Modifier.matchParentSize()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 7.dp,
                        vertical = 7.dp
                    ),
                horizontalArrangement =
                    Arrangement.SpaceEvenly,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                tabs.forEachIndexed {
                        index,
                        (tab, label, icon) ->
                    val activation =
                        (
                            1f -
                                abs(
                                    pagerPosition -
                                        index.toFloat()
                                )
                            )
                            .coerceIn(
                                0f,
                                1f
                            )
                    val selected =
                        activation >= 0.5f
                    val scale =
                        1f +
                            activation *
                            0.035f
                    val contentColor =
                        if (selected) {
                            colors.onSurface
                        } else {
                            colors.onSurfaceVariant
                        }

                    Card(
                        onClick = {
                            onSelected(tab)
                        },
                        modifier = Modifier
                            .scale(scale)
                            .height(58.dp),
                        shape =
                            RoundedCornerShape(
                                999.dp
                            ),
                        border =
                            if (
                                activation >
                                0.04f
                            ) {
                                BorderStroke(
                                    1.dp,
                                    Lavender.copy(
                                        alpha =
                                            0.08f +
                                                activation *
                                                0.24f
                                    )
                                )
                            } else {
                                null
                            },
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    Lavender.copy(
                                        alpha =
                                            activation *
                                                if (isDark) {
                                                    0.18f
                                                } else {
                                                    0.11f
                                                }
                                    ),
                                contentColor =
                                    contentColor
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(
                                    horizontal = 14.dp,
                                    vertical = 7.dp
                                ),
                            horizontalAlignment =
                                Alignment.CenterHorizontally,
                            verticalArrangement =
                                Arrangement.Center
                        ) {
                            Icon(
                                imageVector =
                                    icon,
                                contentDescription =
                                    label,
                                modifier =
                                    Modifier.size(
                                        24.dp
                                    ),
                                tint =
                                    contentColor
                            )
                            Text(
                                modifier =
                                    Modifier.padding(
                                        top = 2.dp
                                    ),
                                text = label,
                                style =
                                    MaterialTheme.typography.labelSmall,
                                fontWeight =
                                    if (selected) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Medium
                                    },
                                color =
                                    contentColor
                            )
                        }
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
                text = "WakeSync uses the Health Connect Integration with read-only sleep access.",
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
private fun PrivacyBanner(
    onInfo: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, colors.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface.copy(alpha = 0.64f),
            contentColor = colors.onSurfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🔒")
            Text(
                modifier = Modifier
                    .padding(start = 9.dp)
                    .weight(1f),
                text = "On-Device Processing · Private & Secure",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            InfoTrigger(onClick = onInfo)
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
private fun SleepMetricRow(
    night: SleepNight,
    goalsEnabled: Boolean,
    onInfo: (MetricInfo) -> Unit
) {
    val deep =
        stageMinutes(
            night,
            SleepStageType.DEEP
        )
    val light =
        stageMinutes(
            night,
            SleepStageType.LIGHT
        )
    val rem =
        stageMinutes(
            night,
            SleepStageType.REM
        )
    val unknown =
        stageMinutes(
            night,
            SleepStageType.UNKNOWN
        )
    val asleep =
        deep + light + rem + unknown
    val sessionMinutes =
        java.time.Duration
            .between(
                night.start,
                night.end
            )
            .toMinutes()
            .coerceAtLeast(1L)
    val efficiency =
        (
            asleep.toDouble() /
                sessionMinutes.toDouble() *
                100.0
            )
            .roundToInt()
            .coerceIn(0, 100)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(9.dp)
    ) {
        QuickMetricCard(
            modifier = Modifier.weight(1f),
            label = "Total Sleep",
            value = formatMinutes(asleep),
            accent = Cyan,
            onInfo = {
                onInfo(
                    MetricInfo(
                        title = "Total sleep",
                        meaning = "The total time WakeSync counts as sleep during the session.",
                        measurement = "Sum of Deep, Light, REM and generic sleep-stage intervals provided through Health Connect.",
                        importance = "Sleep duration is one of the strongest high-level signals for recovery and is weighted heavily in the WakeSync score."
                    )
                )
            }
        )

        if (goalsEnabled) {
            QuickMetricCard(
                modifier = Modifier.weight(1f),
                label = "Deep Sleep",
                value = formatMinutes(deep),
                accent = Lavender,
                onInfo = {
                    onInfo(
                        MetricInfo(
                            title = "Deep sleep",
                            meaning = "Time your wearable classified as deep or slow-wave sleep.",
                            measurement = "Sum of Health Connect stage intervals labeled Deep.",
                            importance = "Deep sleep is associated with physical restoration, but consumer wearable staging is an estimate and is best used as a personal trend."
                        )
                    )
                }
            )
        } else {
            QuickMetricCard(
                modifier = Modifier.weight(1f),
                label = "Efficiency",
                value = efficiency.toString() + "%",
                accent = Mint,
                onInfo = {
                    onInfo(
                        MetricInfo(
                            title = "Sleep efficiency",
                            meaning = "The percentage of the sleep-session window that WakeSync counts as asleep.",
                            measurement = "Estimated sleep-stage minutes divided by the Health Connect sleep-session duration.",
                            importance = "Efficiency helps distinguish enough time in bed from consolidated sleep."
                        )
                    )
                }
            )
        }

        QuickMetricCard(
            modifier = Modifier.weight(1f),
            label = "REM Sleep",
            value = formatMinutes(rem),
            accent = Indigo,
            onInfo = {
                onInfo(
                    MetricInfo(
                        title = "REM sleep",
                        meaning = "Time your wearable classified as rapid-eye-movement sleep.",
                        measurement = "Sum of Health Connect stage intervals labeled REM.",
                        importance = "REM is linked with memory and emotional processing, but the wearable classification should be interpreted as a trend rather than ground truth."
                    )
                )
            }
        )
    }
}

@Composable
private fun QuickMetricCard(
    modifier: Modifier,
    label: String,
    value: String,
    accent: Color,
    onInfo: () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surface
                    .copy(alpha = 0.68f),
            contentColor =
                MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp)
        ) {
            InfoTrigger(
                onClick = onInfo,
                modifier = Modifier.align(Alignment.TopEnd)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 7.dp),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = accent,
                            shape = CircleShape
                        )
                )
                Text(
                    modifier = Modifier.padding(top = 11.dp),
                    text = value,
                    style =
                        MaterialTheme.typography.titleMedium,
                    fontWeight =
                        FontWeight.ExtraBold,
                    color =
                        MaterialTheme.colorScheme.onSurface
                )
                Text(
                    modifier = Modifier.padding(top = 3.dp),
                    text = label,
                    style =
                        MaterialTheme.typography.labelSmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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

private fun sourceFriendlyName(
    packageName: String
): String =
    "Health Connect Integration"

private fun defaultSchedule(): AlarmSchedule =
    AlarmSchedule(
        id = UUID.randomUUID().toString(),
        label = "",
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

private fun scheduleDaysLabel(
    schedule: AlarmSchedule
): String {
    val oneTime = schedule.oneTimeDate
        ?.let {
            runCatching {
                LocalDate.parse(it)
            }.getOrNull()
        }

    return if (oneTime != null) {
        if (oneTime == LocalDate.now().plusDays(1)) {
            "Tomorrow only"
        } else {
            oneTime.format(
                DateTimeFormatter.ofPattern("EEE, MMM d")
            )
        }
    } else {
        daysLabel(schedule.days)
    }
}

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
