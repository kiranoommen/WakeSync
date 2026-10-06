package com.kiranoommen.wakesync.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.kiranoommen.wakesync.AlarmActivity
import com.kiranoommen.wakesync.data.AlarmStore
import com.kiranoommen.wakesync.model.AlarmMode

class AlarmRingingService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val scheduleId =
            intent?.getStringExtra(MultiAlarmController.EXTRA_SCHEDULE_ID)
        val reason =
            intent?.getStringExtra(MultiAlarmController.EXTRA_REASON)
                ?: "WakeSync alarm"
        val kind =
            intent?.getStringExtra(MultiAlarmController.EXTRA_KIND)
                ?: AlarmScheduler.KIND_DEADLINE
        val deadlineMillis =
            intent?.getLongExtra(
                MultiAlarmController.EXTRA_DEADLINE_MILLIS,
                0L
            ) ?: 0L

        val schedule = scheduleId?.let { id ->
            AlarmStore(this).load().firstOrNull { it.id == id }
        }

        startAsForeground(
            scheduleId = scheduleId,
            reason = reason,
            kind = kind,
            deadlineMillis = deadlineMillis,
            smart = schedule?.mode == AlarmMode.SMART_WAKE
        )

        if (schedule?.soundEnabled != false) {
            startAlarmSound(schedule?.soundUri)
        }
        if (schedule?.vibrationEnabled != false) {
            startVibration()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        mediaPlayer?.let { player ->
            runCatching {
                if (player.isPlaying) player.stop()
                player.release()
            }
        }
        mediaPlayer = null
        vibrator?.cancel()
        vibrator = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startAsForeground(
        scheduleId: String?,
        reason: String,
        kind: String,
        deadlineMillis: Long,
        smart: Boolean
    ) {
        val fullScreenIntent = PendingIntent.getActivity(
            this,
            (scheduleId ?: "wakesync").hashCode(),
            Intent(this, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(MultiAlarmController.EXTRA_SCHEDULE_ID, scheduleId)
                putExtra(MultiAlarmController.EXTRA_REASON, reason)
                putExtra(MultiAlarmController.EXTRA_KIND, kind)
                putExtra(
                    MultiAlarmController.EXTRA_DEADLINE_MILLIS,
                    deadlineMillis
                )
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(this, ALARM_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(
                when {
                    kind == "test" ->
                        "WakeSync · Test Alarm"
                    smart ->
                        "WakeSync · Smart Wake"
                    else ->
                        "WakeSync · Standard Alarm"
                }
            )
            .setContentText(
                when {
                    kind == "test" ->
                        "Testing alarm sound, vibration and full-screen behavior."
                    smart ->
                        "WakeSync chose this wake moment."
                    else ->
                        "Your alarm is ringing."
                }
            )
            .setCategory(Notification.CATEGORY_ALARM)
            .setPriority(Notification.PRIORITY_MAX)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullScreenIntent)
            .setFullScreenIntent(fullScreenIntent, true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                ALARM_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(ALARM_NOTIFICATION_ID, notification)
        }
    }

    private fun startAlarmSound(selectedSoundUri: String?) {
        if (mediaPlayer?.isPlaying == true) return

        val defaultAlarmUri =
            RingtoneManager.getActualDefaultRingtoneUri(
                this,
                RingtoneManager.TYPE_ALARM
            ) ?: RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_ALARM
            ) ?: RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_NOTIFICATION
            )

        val requestedUri =
            selectedSoundUri
                ?.takeIf { it.isNotBlank() }
                ?.let(android.net.Uri::parse)

        val candidates =
            listOfNotNull(requestedUri, defaultAlarmUri)
                .distinct()

        mediaPlayer = candidates.firstNotNullOfOrNull { alarmUri ->
            runCatching {
                MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(
                            AudioAttributes.CONTENT_TYPE_SONIFICATION
                        )
                        .build()
                )
                setWakeMode(
                    applicationContext,
                    PowerManager.PARTIAL_WAKE_LOCK
                )
                    setDataSource(applicationContext, alarmUri)
                    isLooping = true
                    prepare()
                    start()
                }
            }.getOrNull()
        }
    }

    @Suppress("DEPRECATION")
    private fun startVibration() {
        vibrator =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(VibratorManager::class.java)
                    .defaultVibrator
            } else {
                getSystemService(VIBRATOR_SERVICE) as Vibrator
            }

        vibrator?.vibrate(
            VibrationEffect.createWaveform(
                longArrayOf(0, 700, 300, 700, 600),
                0
            )
        )
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            ALARM_CHANNEL_ID,
            "Wake alarms",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "WakeSync alarm alerts."
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(null, null)
            enableVibration(false)
        }

        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private companion object {
        const val ALARM_CHANNEL_ID = "wake_alarm"
        const val ALARM_NOTIFICATION_ID = 4300
    }
}
