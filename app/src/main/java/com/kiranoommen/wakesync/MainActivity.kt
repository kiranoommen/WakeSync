package com.kiranoommen.wakesync

import android.Manifest
import android.app.NotificationManager
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.health.connect.client.PermissionController
import com.kiranoommen.wakesync.alarm.PredictiveWakeEngine
import com.kiranoommen.wakesync.alarm.WakeAlarmController
import com.kiranoommen.wakesync.alarm.WakeAlarmScheduler
import com.kiranoommen.wakesync.alarm.WakeMonitorService
import com.kiranoommen.wakesync.data.AppSettingsStore
import com.kiranoommen.wakesync.data.HealthConnectManager
import com.kiranoommen.wakesync.data.SleepExporter
import com.kiranoommen.wakesync.data.WakeHistoryStore
import com.kiranoommen.wakesync.data.WakePreferencesStore
import com.kiranoommen.wakesync.model.AlarmMode
import com.kiranoommen.wakesync.model.SleepNight
import com.kiranoommen.wakesync.model.WakePreferences
import com.kiranoommen.wakesync.ui.OobeScreen
import com.kiranoommen.wakesync.ui.WakeSyncScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var healthConnectManager: HealthConnectManager
    private lateinit var wakePreferencesStore: WakePreferencesStore
    private lateinit var wakeHistoryStore: WakeHistoryStore
    private lateinit var appSettings: AppSettingsStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        healthConnectManager = HealthConnectManager(this)
        wakePreferencesStore = WakePreferencesStore(this)
        wakeHistoryStore = WakeHistoryStore(this)
        appSettings = AppSettingsStore(this)

        setContent {
            val scope = rememberCoroutineScope()

            var hasPermission by remember { mutableStateOf(false) }
            var backgroundReadAvailable by remember {
                mutableStateOf(healthConnectManager.backgroundReadAvailable())
            }
            var hasBackgroundReadPermission by remember { mutableStateOf(false) }
            var hasAnalyticsPermission by remember { mutableStateOf(false) }
            var hasHistoryPermission by remember { mutableStateOf(false) }
            var exactAlarmAccess by remember {
                mutableStateOf(WakeAlarmScheduler.canScheduleExactAlarms(this))
            }
            var notificationsAllowed by remember {
                mutableStateOf(areNotificationsAllowed())
            }
            var fullScreenAlarmAccess by remember {
                mutableStateOf(canUseFullScreenAlarms())
            }
            var wakePreferences by remember {
                mutableStateOf(wakePreferencesStore.load())
            }
            var nights by remember { mutableStateOf<List<SleepNight>>(emptyList()) }
            var loading by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }

            var themeMode by remember { mutableStateOf(appSettings.themeMode) }
            var sleepGoalMinutes by remember { mutableStateOf(appSettings.sleepGoalMinutes) }
            var goalsEnabled by remember { mutableStateOf(appSettings.goalsEnabled) }
            var displayName by remember { mutableStateOf(appSettings.displayName) }
            var retainGeneratedExports by remember {
                mutableStateOf(appSettings.retainGeneratedExports)
            }
            var oobeCompleted by remember { mutableStateOf(appSettings.oobeCompleted) }

            val historyReadAvailable = healthConnectManager.historyReadAvailable()

            fun setupReady(preferences: WakePreferences = wakePreferences): Boolean {
                val alarmBasicsReady =
                    exactAlarmAccess &&
                        notificationsAllowed &&
                        fullScreenAlarmAccess &&
                        preferences.hasValidSchedule()

                if (!alarmBasicsReady) return false

                return when (preferences.mode) {
                    AlarmMode.STANDARD -> true
                    AlarmMode.SMART_WAKE ->
                        hasPermission &&
                            backgroundReadAvailable &&
                            hasBackgroundReadPermission &&
                            preferences.hasValidRange()
                }
            }

            fun refreshSleep() {
                if (!hasPermission) return
                loading = true
                errorMessage = null

                scope.launch {
                    val days = if (hasHistoryPermission) 190L else 30L

                    runCatching { healthConnectManager.readRecentSleep(days) }
                        .onSuccess {
                            nights = it
                            wakeHistoryStore.save(
                                PredictiveWakeEngine.buildProfile(it)
                            )
                        }
                        .onFailure {
                            errorMessage = it.message ?: "Unable to read sleep data."
                        }

                    loading = false
                }
            }

            val healthPermissionLauncher = rememberLauncherForActivityResult(
                contract = PermissionController.createRequestPermissionResultContract()
            ) {
                scope.launch {
                    hasPermission = healthConnectManager.hasRequiredPermissions()
                    backgroundReadAvailable = healthConnectManager.backgroundReadAvailable()
                    hasBackgroundReadPermission =
                        healthConnectManager.hasBackgroundReadPermission()

                    if (hasPermission) refreshSleep()

                    if (wakePreferences.enabled && setupReady()) {
                        WakeAlarmScheduler.schedule(this@MainActivity, wakePreferences)
                    }
                }
            }

            val analyticsPermissionLauncher = rememberLauncherForActivityResult(
                contract = PermissionController.createRequestPermissionResultContract()
            ) {
                scope.launch {
                    hasAnalyticsPermission =
                        healthConnectManager.hasAnalyticsPermissions()
                    if (hasPermission) refreshSleep()
                }
            }

            val historyPermissionLauncher = rememberLauncherForActivityResult(
                contract = PermissionController.createRequestPermissionResultContract()
            ) {
                scope.launch {
                    hasHistoryPermission =
                        healthConnectManager.hasHistoryPermission()
                    if (hasPermission) refreshSleep()
                }
            }

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { granted ->
                notificationsAllowed = granted || areNotificationsAllowed()

                if (wakePreferences.enabled && setupReady()) {
                    WakeAlarmScheduler.schedule(this, wakePreferences)
                }
            }

            val fullScreenAlarmAccessLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) {
                fullScreenAlarmAccess = canUseFullScreenAlarms()

                if (wakePreferences.enabled && setupReady()) {
                    WakeAlarmScheduler.schedule(this, wakePreferences)
                }
            }

            val exactAlarmAccessLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) {
                exactAlarmAccess = WakeAlarmScheduler.canScheduleExactAlarms(this)

                if (wakePreferences.enabled && setupReady()) {
                    WakeAlarmScheduler.schedule(this, wakePreferences)
                }
            }

            LaunchedEffect(Unit) {
                backgroundReadAvailable = healthConnectManager.backgroundReadAvailable()
                hasPermission = runCatching {
                    healthConnectManager.hasRequiredPermissions()
                }.getOrDefault(false)
                hasBackgroundReadPermission = runCatching {
                    healthConnectManager.hasBackgroundReadPermission()
                }.getOrDefault(false)
                hasAnalyticsPermission = runCatching {
                    healthConnectManager.hasAnalyticsPermissions()
                }.getOrDefault(false)
                hasHistoryPermission = runCatching {
                    healthConnectManager.hasHistoryPermission()
                }.getOrDefault(false)
                exactAlarmAccess =
                    WakeAlarmScheduler.canScheduleExactAlarms(this@MainActivity)
                notificationsAllowed = areNotificationsAllowed()
                fullScreenAlarmAccess = canUseFullScreenAlarms()

                // Existing users should not be forced through first-run onboarding
                // after upgrading from the earlier Smart Wake build.
                if (!appSettings.oobeCompleted &&
                    !appSettings.oobeStarted &&
                    (hasPermission || wakePreferences.enabled)
                ) {
                    appSettings.oobeCompleted = true
                    oobeCompleted = true
                }

                if (hasPermission) {
                    loading = true
                    val days = if (hasHistoryPermission) 190L else 30L

                    runCatching { healthConnectManager.readRecentSleep(days) }
                        .onSuccess {
                            nights = it
                            wakeHistoryStore.save(
                                PredictiveWakeEngine.buildProfile(it)
                            )
                        }
                        .onFailure {
                            errorMessage = it.message ?: "Unable to read sleep data."
                        }

                    loading = false
                }

                if (wakePreferences.enabled && setupReady()) {
                    WakeAlarmScheduler.schedule(this@MainActivity, wakePreferences)
                }
            }

            if (!oobeCompleted) {
                LaunchedEffect(Unit) {
                    if (!appSettings.oobeStarted) {
                        appSettings.oobeStarted = true
                    }
                }

                OobeScreen(
                    sdkStatus = healthConnectManager.sdkStatus(),
                    themeMode = themeMode,
                    displayName = displayName,
                    hasPermission = hasPermission,
                    exactAlarmAccess = exactAlarmAccess,
                    onThemeModeChange = { value ->
                        themeMode = value
                        appSettings.themeMode = value
                    },
                    onDisplayNameChange = { value ->
                        displayName = value
                        appSettings.displayName = value
                    },
                    onConnect = {
                        healthPermissionLauncher.launch(
                            healthConnectManager.requestedPermissions()
                        )
                    },
                    onRequestExactAlarmAccess = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            exactAlarmAccessLauncher.launch(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:$packageName")
                                }
                            )
                        } else {
                            exactAlarmAccess = true
                        }
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
                    backgroundReadAvailable = backgroundReadAvailable,
                    hasBackgroundReadPermission = hasBackgroundReadPermission,
                    exactAlarmAccess = exactAlarmAccess,
                    notificationsAllowed = notificationsAllowed,
                    fullScreenAlarmAccess = fullScreenAlarmAccess,
                    hasAnalyticsPermission = hasAnalyticsPermission,
                    hasHistoryPermission = hasHistoryPermission,
                    historyReadAvailable = historyReadAvailable,
                    wakePreferences = wakePreferences,
                    loading = loading,
                    nights = nights,
                    themeMode = themeMode,
                    sleepGoalMinutes = sleepGoalMinutes,
                    goalsEnabled = goalsEnabled,
                    displayName = displayName,
                    retainGeneratedExports = retainGeneratedExports,
                    errorMessage = errorMessage,
                    onConnect = {
                        healthPermissionLauncher.launch(
                            healthConnectManager.requestedPermissions()
                        )
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
                    onRequestExactAlarmAccess = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            exactAlarmAccessLauncher.launch(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:$packageName")
                                }
                            )
                        } else {
                            exactAlarmAccess = true
                        }
                    },
                    onRequestNotifications = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                            PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                        } else if (!areNotificationsAllowed()) {
                            startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                                }
                            )
                        } else {
                            notificationsAllowed = true
                        }
                    },
                    onRequestFullScreenAlarmAccess = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            fullScreenAlarmAccessLauncher.launch(
                                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                                    data = Uri.parse("package:$packageName")
                                }
                            )
                        } else {
                            fullScreenAlarmAccess = true
                        }
                    },
                    onWakePreferencesChanged = { updated ->
                        wakePreferences = updated
                        wakePreferencesStore.save(updated)

                        if (updated.enabled) {
                            if (setupReady(updated)) {
                                WakeAlarmScheduler.schedule(this, updated)
                            } else {
                                WakeAlarmScheduler.cancelAll(this)
                            }
                        }
                    },
                    onAlarmEnabledChanged = { enabled ->
                        val updated = wakePreferences.copy(enabled = enabled)
                        wakePreferences = updated
                        wakePreferencesStore.save(updated)

                        if (enabled && setupReady(updated)) {
                            WakeAlarmScheduler.schedule(this, updated)
                        } else {
                            WakeAlarmScheduler.cancelAll(this)
                            stopService(Intent(this, WakeMonitorService::class.java))
                        }
                    },
                    onThemeModeChange = { value ->
                        themeMode = value
                        appSettings.themeMode = value
                    },
                    onSleepGoalChange = { value ->
                        sleepGoalMinutes = value
                        appSettings.sleepGoalMinutes = value
                    },
                    onGoalsEnabledChange = { value ->
                        goalsEnabled = value
                        appSettings.goalsEnabled = value
                    },
                    onDisplayNameChange = { value ->
                        displayName = value
                        appSettings.displayName = value
                    },
                    onRetainGeneratedExportsChange = { value ->
                        retainGeneratedExports = value
                        appSettings.retainGeneratedExports = value
                    },
                    onClearGeneratedExports = {
                        SleepExporter.clearGeneratedExports(this)
                    },
                    onImAwake = {
                        WakeAlarmController.markAwake(this)
                    },
                    onRefresh = ::refreshSleep
                )
            }
        }
    }

    private fun canUseFullScreenAlarms(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
        return getSystemService(NotificationManager::class.java)
            .canUseFullScreenIntent()
    }

    private fun areNotificationsAllowed(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        return getSystemService(NotificationManager::class.java)
            .areNotificationsEnabled()
    }
}
