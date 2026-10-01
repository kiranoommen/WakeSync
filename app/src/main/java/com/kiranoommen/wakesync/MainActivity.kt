package com.kiranoommen.wakesync

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.lifecycleScope
import com.kiranoommen.wakesync.alarm.AlarmScheduler
import com.kiranoommen.wakesync.data.AlarmStore
import com.kiranoommen.wakesync.data.AppSettingsStore
import com.kiranoommen.wakesync.data.HealthConnectManager
import com.kiranoommen.wakesync.data.SleepExporter
import com.kiranoommen.wakesync.domain.WakeWindowEngine
import com.kiranoommen.wakesync.model.AlarmSchedule
import com.kiranoommen.wakesync.model.SleepNight
import com.kiranoommen.wakesync.ui.OobeScreen
import com.kiranoommen.wakesync.ui.WakeSyncScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var healthConnectManager: HealthConnectManager
    private lateinit var alarmStore: AlarmStore
    private lateinit var alarmScheduler: AlarmScheduler
    private lateinit var appSettings: AppSettingsStore

    private val exactAlarmAccessState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        healthConnectManager = HealthConnectManager(this)
        alarmStore = AlarmStore(this)
        alarmScheduler = AlarmScheduler(this)
        appSettings = AppSettingsStore(this)
        exactAlarmAccessState.value = alarmScheduler.canScheduleExactAlarms()

        setContent {
            var hasPermission by remember { mutableStateOf(false) }
            var hasAnalyticsPermission by remember { mutableStateOf(false) }
            var hasHistoryPermission by remember { mutableStateOf(false) }
            var nights by remember { mutableStateOf<List<SleepNight>>(emptyList()) }
            var schedules by remember { mutableStateOf(alarmStore.load()) }
            var loading by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }
            var themeMode by remember { mutableStateOf(appSettings.themeMode) }
            var sleepGoalMinutes by remember {
                mutableStateOf(appSettings.sleepGoalMinutes)
            }
            var goalsEnabled by remember {
                mutableStateOf(appSettings.goalsEnabled)
            }
            var dashboardWidgets by remember {
                mutableStateOf(appSettings.dashboardWidgets)
            }
            var displayName by remember {
                mutableStateOf(appSettings.displayName)
            }
            var maxSmartWindowMinutes by remember {
                mutableStateOf(appSettings.maxSmartWindowMinutes)
            }
            var retainGeneratedExports by remember {
                mutableStateOf(appSettings.retainGeneratedExports)
            }
            var oobeCompleted by remember {
                mutableStateOf(
                    appSettings.oobeCompleted ||
                        schedules.isNotEmpty() ||
                        appSettings.hasExistingUserState()
                )
            }

            val historyReadAvailable = healthConnectManager.historyReadAvailable()

            fun withRecommendations(
                input: List<AlarmSchedule>,
                sleepHistory: List<SleepNight> = nights
            ): List<AlarmSchedule> =
                input.map { schedule ->
                    val recommendation = WakeWindowEngine.recommend(
                        nights = sleepHistory,
                        windowMinutes = schedule.smartWindowMinutes
                    )
                    schedule.copy(
                        smartOffsetMinutes = recommendation.offsetMinutes
                            .coerceAtMost(schedule.smartWindowMinutes)
                    )
                }

            fun persist(input: List<AlarmSchedule>) {
                val today = java.time.LocalDate.now()
                val cleaned = input.map { schedule ->
                    schedule.copy(
                        skippedDates = schedule.futureSkippedDates(today)
                    )
                }
                val updated = withRecommendations(cleaned)
                schedules = updated
                alarmStore.save(updated)
                alarmScheduler.scheduleAll(updated)
            }

            fun applySleepHistory(input: List<SleepNight>) {
                nights = input

                val updated = schedules.map { schedule ->
                    val recommendation = WakeWindowEngine.recommend(
                        nights = input,
                        windowMinutes = schedule.smartWindowMinutes
                    )
                    schedule.copy(
                        smartOffsetMinutes = recommendation.offsetMinutes
                            .coerceAtMost(schedule.smartWindowMinutes)
                    )
                }

                if (updated != schedules) {
                    schedules = updated
                    alarmStore.save(updated)
                    alarmScheduler.scheduleAll(updated)
                }
            }

            fun refreshSleep() {
                if (!hasPermission) return

                loading = true
                errorMessage = null

                lifecycleScope.launch {
                    val days = if (hasHistoryPermission) 190L else 30L

                    runCatching {
                        healthConnectManager.readRecentSleep(days)
                    }
                        .onSuccess(::applySleepHistory)
                        .onFailure {
                            errorMessage = it.message ?: "Unable to read sleep data."
                        }

                    loading = false
                }
            }

            val healthPermissionLauncher = rememberLauncherForActivityResult(
                contract = PermissionController.createRequestPermissionResultContract()
            ) {
                lifecycleScope.launch {
                    hasPermission = healthConnectManager.hasRequiredPermissions()
                    hasAnalyticsPermission =
                        healthConnectManager.hasAnalyticsPermissions()
                    hasHistoryPermission =
                        healthConnectManager.hasHistoryPermission()

                    if (hasPermission) refreshSleep()
                }
            }

            val analyticsPermissionLauncher = rememberLauncherForActivityResult(
                contract = PermissionController.createRequestPermissionResultContract()
            ) {
                lifecycleScope.launch {
                    hasAnalyticsPermission =
                        healthConnectManager.hasAnalyticsPermissions()
                    if (hasPermission) refreshSleep()
                }
            }

            val historyPermissionLauncher = rememberLauncherForActivityResult(
                contract = PermissionController.createRequestPermissionResultContract()
            ) {
                lifecycleScope.launch {
                    hasHistoryPermission =
                        healthConnectManager.hasHistoryPermission()
                    if (hasPermission) refreshSleep()
                }
            }

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { }

            LaunchedEffect(Unit) {
                if (
                    !appSettings.oobeCompleted &&
                    (
                        schedules.isNotEmpty() ||
                            appSettings.hasExistingUserState()
                        )
                ) {
                    appSettings.oobeCompleted = true
                    oobeCompleted = true
                }

                if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermissionLauncher.launch(
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                }

                hasPermission = runCatching {
                    healthConnectManager.hasRequiredPermissions()
                }.getOrDefault(false)

                hasAnalyticsPermission = runCatching {
                    healthConnectManager.hasAnalyticsPermissions()
                }.getOrDefault(false)

                hasHistoryPermission = runCatching {
                    healthConnectManager.hasHistoryPermission()
                }.getOrDefault(false)

                alarmScheduler.scheduleAll(schedules)

                if (hasPermission) refreshSleep()
            }

            if (!oobeCompleted) {
                OobeScreen(
                    sdkStatus = healthConnectManager.sdkStatus(),
                    themeMode = themeMode,
                    displayName = displayName,
                    hasPermission = hasPermission,
                    onThemeModeChange = { newMode ->
                        themeMode = newMode
                        appSettings.themeMode = newMode
                    },
                    onDisplayNameChange = { name ->
                        displayName = name
                        appSettings.displayName = name
                    },
                    onConnect = {
                        healthPermissionLauncher.launch(
                            HealthConnectManager.requiredPermissions
                        )
                    },
                    onFinish = {
                        appSettings.oobeCompleted = true
                        oobeCompleted = true
                    }
                )
            } else {
                WakeSyncScreen(
                    sdkStatus = healthConnectManager.sdkStatus(),
                    hasPermission = hasPermission,
                    loading = loading,
                    nights = nights,
                    schedules = schedules,
                    exactAlarmAccess = exactAlarmAccessState.value,
                    hasAnalyticsPermission = hasAnalyticsPermission,
                    hasHistoryPermission = hasHistoryPermission,
                    historyReadAvailable = historyReadAvailable,
                    themeMode = themeMode,
                    sleepGoalMinutes = sleepGoalMinutes,
                    goalsEnabled = goalsEnabled,
                    dashboardWidgets = dashboardWidgets,
                    displayName = displayName,
                    maxSmartWindowMinutes = maxSmartWindowMinutes,
                    retainGeneratedExports = retainGeneratedExports,
                    errorMessage = errorMessage,
                    onConnect = {
                        healthPermissionLauncher.launch(
                            HealthConnectManager.requiredPermissions
                        )
                    },
                    onRefresh = {
                        refreshSleep()
                    },
                    onSaveSchedule = { schedule ->
                        val cappedWindow = schedule.smartWindowMinutes
                            .coerceAtMost(maxSmartWindowMinutes)
                        val capped = schedule.copy(
                            smartWindowMinutes = cappedWindow,
                            smartOffsetMinutes = schedule.smartOffsetMinutes
                                .coerceAtMost(cappedWindow)
                        )
                        val next = schedules
                            .filterNot { it.id == capped.id } + capped
                        persist(next)
                    },
                    onDeleteSchedule = { schedule ->
                        alarmScheduler.cancel(schedule.id)
                        persist(schedules.filterNot { it.id == schedule.id })
                    },
                    onToggleSchedule = { schedule, enabled ->
                        persist(
                            schedules.map {
                                if (it.id == schedule.id) {
                                    it.copy(enabled = enabled)
                                } else {
                                    it
                                }
                            }
                        )
                    },
                    onSkipNext = { schedule ->
                        val nextBase = schedule.nextBaseDeadline()

                        if (nextBase != null) {
                            val dateKey = nextBase.toLocalDate().toString()
                            val updatedSkips =
                                if (schedule.skippedDates.contains(dateKey)) {
                                    schedule.skippedDates - dateKey
                                } else {
                                    schedule.skippedDates + dateKey
                                }

                            val updatedSchedule = schedule.copy(
                                skippedDates = updatedSkips
                            )

                            persist(
                                schedules.map {
                                    if (it.id == schedule.id) updatedSchedule else it
                                }
                            )
                        }
                    },
                    onClearSkips = { schedule ->
                        val updatedSchedule =
                            schedule.copy(skippedDates = emptySet())

                        persist(
                            schedules.map {
                                if (it.id == schedule.id) updatedSchedule else it
                            }
                        )
                    },
                    onRequestExactAlarmAccess = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            runCatching {
                                startActivity(
                                    Intent(
                                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                                    ).apply {
                                        data = Uri.parse("package:$packageName")
                                    }
                                )
                            }
                        }
                    },
                    onRequestAnalyticsAccess = {
                        analyticsPermissionLauncher.launch(
                            HealthConnectManager.analyticsPermissions
                        )
                    },
                    onRequestHistoryAccess = {
                        if (historyReadAvailable) {
                            historyPermissionLauncher.launch(
                                HealthConnectManager.historyPermissions
                            )
                        }
                    },
                    onThemeModeChange = { newMode ->
                        themeMode = newMode
                        appSettings.themeMode = newMode
                    },
                    onSleepGoalChange = { minutes ->
                        sleepGoalMinutes = minutes
                        appSettings.sleepGoalMinutes = minutes
                    },
                    onGoalsEnabledChange = { enabled ->
                        goalsEnabled = enabled
                        appSettings.goalsEnabled = enabled
                    },
                    onDashboardWidgetsChange = { widgets ->
                        dashboardWidgets = widgets
                        appSettings.dashboardWidgets = widgets
                    },
                    onDisplayNameChange = { name ->
                        displayName = name
                        appSettings.displayName = name
                    },
                    onMaxSmartWindowChange = { minutes ->
                        maxSmartWindowMinutes = minutes
                        appSettings.maxSmartWindowMinutes = minutes

                        persist(
                            schedules.map { schedule ->
                                val capped = schedule.smartWindowMinutes
                                    .coerceAtMost(minutes)
                                schedule.copy(
                                    smartWindowMinutes = capped,
                                    smartOffsetMinutes = schedule.smartOffsetMinutes
                                        .coerceAtMost(capped)
                                )
                            }
                        )
                    },
                    onRetainGeneratedExportsChange = { retain ->
                        retainGeneratedExports = retain
                        appSettings.retainGeneratedExports = retain

                        if (!retain) {
                            SleepExporter.clearGeneratedExports(this@MainActivity)
                        }
                    },
                    onClearGeneratedExports = {
                        SleepExporter.clearGeneratedExports(this@MainActivity)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (::alarmScheduler.isInitialized) {
            exactAlarmAccessState.value =
                alarmScheduler.canScheduleExactAlarms()
        }
    }
}
