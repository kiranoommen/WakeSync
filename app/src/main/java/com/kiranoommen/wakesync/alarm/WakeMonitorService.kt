package com.kiranoommen.wakesync.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.kiranoommen.wakesync.MainActivity
import com.kiranoommen.wakesync.data.HealthConnectManager
import com.kiranoommen.wakesync.data.WakePreferencesStore
import com.kiranoommen.wakesync.model.SleepStageType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.ZonedDateTime

class WakeMonitorService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitorJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startAsForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (monitorJob?.isActive != true) {
            monitorJob = serviceScope.launch { monitorUntilWake() }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun monitorUntilWake() {
        val store = WakePreferencesStore(applicationContext)
        val healthConnect = HealthConnectManager(applicationContext)

        val preferences = store.load()
        if (!preferences.enabled || !preferences.hasValidRange()) {
            stopSelf()
            return
        }

        if (!healthConnect.backgroundReadAvailable() ||
            !healthConnect.hasBackgroundReadPermission()
        ) {
            stopSelf()
            return
        }

        while (serviceScope.isActive) {
            val now = ZonedDateTime.now()
            val latestPreferences = store.load()

            if (!latestPreferences.enabled || !latestPreferences.hasValidRange()) {
                stopSelf()
                return
            }

            val (earliestWake, hardDeadline) = resolveCurrentWakeWindow(
                now = now,
                earliestHour = latestPreferences.earliestHour,
                earliestMinute = latestPreferences.earliestMinute,
                latestHour = latestPreferences.latestHour,
                latestMinute = latestPreferences.latestMinute
            )

            val monitorStart = earliestWake.minusMinutes(WakeAlarmScheduler.MONITOR_LEAD_MINUTES)

            if (now.isBefore(monitorStart.minusMinutes(2))) {
                stopSelf()
                return
            }

            if (!now.isBefore(hardDeadline)) {
                WakeAlarmController.ring(
                    context = applicationContext,
                    reason = "Hard wake deadline reached"
                )
                stopSelf()
                return
            }

            if (!now.isBefore(earliestWake)) {
                val snapshot = runCatching {
                    healthConnect.readLatestSleepStage()
                }.getOrNull()

                if (snapshot != null && snapshot.isFresh()) {
                    val minutesRemaining = Duration.between(now, hardDeadline)
                        .toMinutes()
                        .coerceAtLeast(0)

                    val shouldWake = when (snapshot.stage) {
                        SleepStageType.AWAKE -> true
                        SleepStageType.LIGHT -> true
                        SleepStageType.REM -> minutesRemaining <= 10
                        SleepStageType.DEEP,
                        SleepStageType.UNKNOWN -> false
                    }

                    if (shouldWake) {
                        WakeAlarmController.ring(
                            context = applicationContext,
                            reason = "Live sleep stage: ${snapshot.stage.name.lowercase()}"
                        )
                        stopSelf()
                        return
                    }
                }
            }

            delay(POLL_INTERVAL_MILLIS)
        }
    }

    private fun resolveCurrentWakeWindow(
        now: ZonedDateTime,
        earliestHour: Int,
        earliestMinute: Int,
        latestHour: Int,
        latestMinute: Int
    ): Pair<ZonedDateTime, ZonedDateTime> {
        var date = now.toLocalDate()
        var earliest = date.atTime(earliestHour, earliestMinute).atZone(now.zone)
        var deadline = date.atTime(latestHour, latestMinute).atZone(now.zone)

        if (!now.isBefore(deadline)) {
            date = date.plusDays(1)
            earliest = date.atTime(earliestHour, earliestMinute).atZone(now.zone)
            deadline = date.atTime(latestHour, latestMinute).atZone(now.zone)
        }

        return earliest to deadline
    }

    private fun startAsForeground() {
        val openAppIntent = PendingIntent.getActivity(
            this,
            4201,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(this, MONITOR_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("WakeSync is watching your sleep")
            .setContentText("Live sleep monitoring is active for your wake window.")
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                MONITOR_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(MONITOR_NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            MONITOR_CHANNEL_ID,
            "Wake monitoring",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows while WakeSync is checking live sleep stages."
            setSound(null, null)
        }

        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private companion object {
        const val MONITOR_CHANNEL_ID = "wake_monitoring"
        const val MONITOR_NOTIFICATION_ID = 4200
        const val POLL_INTERVAL_MILLIS = 60_000L
    }
}
