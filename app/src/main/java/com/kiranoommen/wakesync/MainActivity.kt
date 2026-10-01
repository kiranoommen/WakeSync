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
import com.kiranoommen.wakesync.data.HealthConnectManager
import com.kiranoommen.wakesync.domain.WakeWindowEngine
import com.kiranoommen.wakesync.model.AlarmSchedule
import com.kiranoommen.wakesync.model.SleepNight
import com.kiranoommen.wakesync.ui.WakeSyncScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var healthConnectManager: HealthConnectManager
    private lateinit var alarmStore: AlarmStore
    private lateinit var alarmScheduler: AlarmScheduler

    private val exactAlarmAccessState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        healthConnectManager = HealthConnectManager(this)
        alarmStore = AlarmStore(this)
        alarmScheduler = AlarmScheduler(this)
        exactAlarmAccessState.value = alarmScheduler.canScheduleExactAlarms()

        setContent {
            var hasPermission by remember { mutableStateOf(false) }
            var nights by remember { mutableStateOf<List<SleepNight>>(emptyList()) }
            var schedules by remember { mutableStateOf(alarmStore.load()) }
            var loading by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }

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

            val healthPermissionLauncher = rememberLauncherForActivityResult(
                contract = PermissionController.createRequestPermissionResultContract()
            ) { granted ->
                hasPermission = granted.containsAll(HealthConnectManager.requiredPermissions)

                if (hasPermission) {
                    loading = true
                    lifecycleScope.launch {
                        runCatching { healthConnectManager.readRecentSleep() }
                            .onSuccess(::applySleepHistory)
                            .onFailure {
                                errorMessage = it.message ?: "Unable to read sleep data."
                            }
                        loading = false
                    }
                }
            }

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { }

            LaunchedEffect(Unit) {
                if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }

                hasPermission = runCatching {
                    healthConnectManager.hasRequiredPermissions()
                }.getOrDefault(false)

                alarmScheduler.scheduleAll(schedules)

                if (hasPermission) {
                    loading = true
                    runCatching { healthConnectManager.readRecentSleep() }
                        .onSuccess(::applySleepHistory)
                        .onFailure {
                            errorMessage = it.message ?: "Unable to read sleep data."
                        }
                    loading = false
                }
            }

            WakeSyncScreen(
                sdkStatus = healthConnectManager.sdkStatus(),
                hasPermission = hasPermission,
                loading = loading,
                nights = nights,
                schedules = schedules,
                exactAlarmAccess = exactAlarmAccessState.value,
                errorMessage = errorMessage,
                onConnect = {
                    healthPermissionLauncher.launch(HealthConnectManager.requiredPermissions)
                },
                onRefresh = {
                    loading = true
                    errorMessage = null
                    lifecycleScope.launch {
                        runCatching { healthConnectManager.readRecentSleep() }
                            .onSuccess(::applySleepHistory)
                            .onFailure {
                                errorMessage = it.message ?: "Unable to read sleep data."
                            }
                        loading = false
                    }
                },
                onSaveSchedule = { schedule ->
                    val next = schedules
                        .filterNot { it.id == schedule.id } + schedule
                    persist(next)
                },
                onDeleteSchedule = { schedule ->
                    alarmScheduler.cancel(schedule.id)
                    persist(schedules.filterNot { it.id == schedule.id })
                },
                onToggleSchedule = { schedule, enabled ->
                    persist(
                        schedules.map {
                            if (it.id == schedule.id) it.copy(enabled = enabled) else it
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
                    val updatedSchedule = schedule.copy(skippedDates = emptySet())
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
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:$packageName")
                                }
                            )
                        }
                    }
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (::alarmScheduler.isInitialized) {
            exactAlarmAccessState.value = alarmScheduler.canScheduleExactAlarms()
        }
    }
}
