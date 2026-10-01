package com.kiranoommen.wakesync.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.kiranoommen.wakesync.R
import com.kiranoommen.wakesync.data.AlarmStore

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getStringExtra(AlarmScheduler.EXTRA_SCHEDULE_ID) ?: return
        val kind = intent.getStringExtra(AlarmScheduler.EXTRA_KIND) ?: AlarmScheduler.KIND_DEADLINE

        val store = AlarmStore(context)
        val schedule = store.load().firstOrNull { it.id == scheduleId } ?: return

        createChannel(context)

        val fullScreenIntent = Intent(context, AlarmActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(AlarmScheduler.EXTRA_SCHEDULE_ID, scheduleId)
            .putExtra(AlarmScheduler.EXTRA_KIND, kind)

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            scheduleId.hashCode(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(schedule.label.ifBlank { "WakeSync" })
            .setContentText(
                if (kind == AlarmScheduler.KIND_SMART) {
                    "Smart wake point found. Your deadline is still protected."
                } else {
                    "Wake-by deadline reached."
                }
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(scheduleId.hashCode(), notification)

        // Keep the current deadline backup intact when an early smart alarm fires.
        // Once the hard deadline itself fires, move the recurring schedule forward.
        if (kind == AlarmScheduler.KIND_DEADLINE) {
            AlarmScheduler(context).scheduleNext(
                schedule = schedule,
                after = java.time.ZonedDateTime.now().plusMinutes(1)
            )
        }
    }

    private fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "WakeSync alarms",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "WakeSync alarm notifications"
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "wakesync_alarm"
    }
}
