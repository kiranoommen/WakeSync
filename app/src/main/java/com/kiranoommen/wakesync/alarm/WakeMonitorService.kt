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
import com.kiranoommen.wakesync.model.AlarmMode
import com.kiranoommen.wakesync.model.SleepStageType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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

    override fun onTimeout(startId: Int, fgsType: Int) {
        stopSelf(startId)
    }

    private suspend fun monitorUntilWake() {
        val preferencesStore = WakePreferencesStore(applicationContext)
        val healthConnect = HealthConnectManager(applicationContext)

        val initialPreferences = preferencesStore.load()
        if (!initialPreferences.enabled ||
            initialPreferences.mode != AlarmMode.SMART_WAKE ||
            !initialPreferences.hasValidRange()
        ) {
            stopSelf()
            return
        }

        if (!healthConnect.backgroundReadAvailable() ||
            !healthConnect.hasBackgroundReadPermission()
        ) {
            // The historical fallback and hard-stop alarms were scheduled
            // independently, so they remain intact even if live reads cannot run.
            stopSelf()
            return
        }

        while (serviceScope.isActive) {
            val now = ZonedDateTime.now()
            val preferences = preferencesStore.load()

            if (!preferences.enabled ||
                preferences.mode != AlarmMode.SMART_WAKE ||
                !preferences.hasValidRange()
            ) {
                stopSelf()
                return
            }

            val (earliestWake, hardDeadline) = resolveCurrentWakeWindow(
                now = now,
                earliestHour = preferences.earliestHour,
                earliestMinute = preferences.earliestMinute,
                latestHour = preferences.latestHour,
                latestMinute = preferences.latestMinute
            )

            val monitorStart = earliestWake.minusMinutes(
                WakeAlarmScheduler.MONITOR_LEAD_MINUTES
            )

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

                if (snapshot != null &&
                    snapshot.isFresh() &&
                    isLiveWakeStage(snapshot.stage)
                ) {
                    WakeAlarmController.ring(
                        context = applicationContext,
                        reason = "Live sleep stage: ${snapshot.stage.name.lowercase()}"
                    )
                    stopSelf()
                    return
                }
            }

            // Live monitoring intentionally continues through the final part
            // of the wake window. The separately scheduled historical fallback
            // will interrupt this service only if its chosen wake time arrives
            // before a favorable fresh live stage is observed.
            delay(POLL_INTERVAL_MILLIS)
        }
    }

    private fun isLiveWakeStage(stage: SleepStageType): Boolean =
        when (stage) {
            SleepStageType.AWAKE,
            SleepStageType.LIGHT -> true
            SleepStageType.REM,
            SleepStageType.DEEP,
            SleepStageType.UNKNOWN -> false
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
            .setContentText(
                "Live sleep stays active through your wake window. Historical fallback and hard stop remain armed."
            )
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
