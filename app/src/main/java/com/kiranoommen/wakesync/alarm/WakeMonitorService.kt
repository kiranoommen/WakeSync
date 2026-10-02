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
import com.kiranoommen.wakesync.data.AlarmStore
import com.kiranoommen.wakesync.data.HealthConnectManager
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
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap

class WakeMonitorService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val monitorJobs = ConcurrentHashMap<String, Job>()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startAsForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val scheduleId =
            intent?.getStringExtra(AlarmScheduler.EXTRA_SCHEDULE_ID)
                ?: return START_NOT_STICKY
        val deadlineMillis =
            intent.getLongExtra(AlarmScheduler.EXTRA_DEADLINE_MILLIS, 0L)

        if (deadlineMillis <= 0L) return START_NOT_STICKY

        if (monitorJobs[scheduleId]?.isActive != true) {
            val job = serviceScope.launch {
                monitorSchedule(scheduleId, deadlineMillis)
            }
            monitorJobs[scheduleId] = job
            job.invokeOnCompletion {
                monitorJobs.remove(scheduleId)
                if (monitorJobs.isEmpty()) {
                    stopSelf()
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        monitorJobs.values.forEach { it.cancel() }
        monitorJobs.clear()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTimeout(startId: Int, fgsType: Int) {
        stopSelf(startId)
    }

    private suspend fun monitorSchedule(
        scheduleId: String,
        deadlineMillis: Long
    ) {
        val healthConnect = HealthConnectManager(applicationContext)
        val zone = ZoneId.systemDefault()
        val deadline = ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(deadlineMillis),
            zone
        )

        if (!healthConnect.backgroundReadAvailable() ||
            !healthConnect.hasBackgroundReadPermission()
        ) {
            return
        }

        while (serviceScope.isActive) {
            val schedule = AlarmStore(applicationContext)
                .load()
                .firstOrNull { it.id == scheduleId }
                ?: return

            if (!schedule.enabled ||
                schedule.mode != AlarmMode.SMART_WAKE ||
                schedule.smartWindowMinutes <= 0
            ) {
                return
            }

            val now = ZonedDateTime.now(zone)
            val earliestWake =
                deadline.minusMinutes(schedule.smartWindowMinutes.toLong())

            if (!now.isBefore(deadline)) {
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
                    MultiAlarmController.ring(
                        context = applicationContext,
                        scheduleId = scheduleId,
                        kind = KIND_LIVE,
                        deadlineMillis = deadlineMillis,
                        reason = "Live sleep stage: ${snapshot.stage.name.lowercase()}"
                    )
                    return
                }
            }

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

    private fun startAsForeground() {
        val openAppIntent = PendingIntent.getActivity(
            this,
            4201,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(this, MONITOR_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("WakeSync is watching your Smart Wake alarms")
            .setContentText(
                "Live sleep stays primary. History fallback and hard deadlines remain armed."
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
        const val KIND_LIVE = "live"
        const val MONITOR_CHANNEL_ID = "wake_monitoring"
        const val MONITOR_NOTIFICATION_ID = 4200
        const val POLL_INTERVAL_MILLIS = 60_000L
    }
}
