package com.kiranoommen.wakesync.alarm

import android.content.Context
import android.content.Intent
import android.os.Build
import com.kiranoommen.wakesync.data.AlarmStore
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

object MultiAlarmController {

    const val EXTRA_SCHEDULE_ID = "ring_schedule_id"
    const val EXTRA_REASON = "ring_reason"
    const val EXTRA_KIND = "ring_kind"

    fun ring(
        context: Context,
        scheduleId: String,
        kind: String,
        deadlineMillis: Long,
        reason: String
    ) {
        val appContext = context.applicationContext
        val store = AlarmStore(appContext)
        val schedule = store.load().firstOrNull { it.id == scheduleId } ?: return
        val scheduler = AlarmScheduler(appContext)

        if (kind != AlarmScheduler.KIND_SNOOZE) {
            scheduler.cancel(scheduleId)

            val after = if (deadlineMillis > 0L) {
                ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(deadlineMillis),
                    ZoneId.systemDefault()
                ).plusMinutes(1)
            } else {
                ZonedDateTime.now().plusMinutes(1)
            }

            scheduler.scheduleNext(schedule, after)
        }

        val serviceIntent = Intent(appContext, AlarmRingingService::class.java)
            .putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            .putExtra(EXTRA_REASON, reason)
            .putExtra(EXTRA_KIND, kind)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startForegroundService(serviceIntent)
        } else {
            appContext.startService(serviceIntent)
        }
    }

    fun dismiss(context: Context) {
        context.applicationContext.stopService(
            Intent(context.applicationContext, AlarmRingingService::class.java)
        )
    }

    fun snooze(context: Context, scheduleId: String, minutes: Int) {
        if (minutes <= 0) return
        AlarmScheduler(context.applicationContext).snooze(scheduleId, minutes)
        dismiss(context)
    }
}
