package com.kiranoommen.wakesync.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.kiranoommen.wakesync.model.AlarmSchedule
import java.time.ZonedDateTime

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun scheduleAll(schedules: List<AlarmSchedule>) {
        schedules.forEach { schedule ->
            cancel(schedule.id)
            if (schedule.enabled) scheduleNext(schedule)
        }
    }

    fun scheduleNext(
        schedule: AlarmSchedule,
        after: ZonedDateTime = ZonedDateTime.now()
    ) {
        val deadline = schedule.nextDeadline(after) ?: return
        val smartOffset = schedule.smartOffsetMinutes
            .coerceIn(0, schedule.smartWindowMinutes)

        if (smartOffset > 0) {
            val smartTime = deadline.minusMinutes(smartOffset.toLong())
            setAlarm(schedule, smartTime, KIND_SMART)
            setAlarm(schedule, deadline, KIND_DEADLINE)
        } else {
            setAlarm(schedule, deadline, KIND_DEADLINE)
        }
    }

    fun cancel(scheduleId: String) {
        pending(scheduleId, KIND_SMART, PendingIntent.FLAG_NO_CREATE)?.let(alarmManager::cancel)
        pending(scheduleId, KIND_DEADLINE, PendingIntent.FLAG_NO_CREATE)?.let(alarmManager::cancel)
        pending(scheduleId, KIND_SNOOZE, PendingIntent.FLAG_NO_CREATE)?.let(alarmManager::cancel)
    }

    fun cancelDeadline(scheduleId: String) {
        pending(scheduleId, KIND_DEADLINE, PendingIntent.FLAG_NO_CREATE)?.let(alarmManager::cancel)
    }

    fun snooze(scheduleId: String, minutes: Int) {
        val trigger = System.currentTimeMillis() + minutes * 60_000L
        val intent = Intent(context, AlarmReceiver::class.java)
            .putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            .putExtra(EXTRA_KIND, KIND_SNOOZE)

        val pi = PendingIntent.getBroadcast(
            context,
            requestCode(scheduleId, KIND_SNOOZE),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        set(trigger, pi)
    }

    private fun setAlarm(schedule: AlarmSchedule, time: ZonedDateTime, kind: String) {
        val pi = pending(schedule.id, kind, PendingIntent.FLAG_UPDATE_CURRENT)
        set(time.toInstant().toEpochMilli(), pi)
    }

    private fun set(triggerAtMillis: Long, pi: PendingIntent?) {
        if (pi == null) return

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAtMillis, pi),
                pi
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pi
            )
        }
    }

    private fun pending(scheduleId: String, kind: String, flags: Int): PendingIntent? {
        val intent = Intent(context, AlarmReceiver::class.java)
            .putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            .putExtra(EXTRA_KIND, kind)

        return if (flags and PendingIntent.FLAG_NO_CREATE != 0) {
            PendingIntent.getBroadcast(
                context,
                requestCode(scheduleId, kind),
                intent,
                flags or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getBroadcast(
                context,
                requestCode(scheduleId, kind),
                intent,
                flags or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    private fun requestCode(scheduleId: String, kind: String): Int =
        (scheduleId + ":" + kind).hashCode()

    companion object {
        const val EXTRA_SCHEDULE_ID = "schedule_id"
        const val EXTRA_KIND = "alarm_kind"

        const val KIND_SMART = "smart"
        const val KIND_DEADLINE = "deadline"
        const val KIND_SNOOZE = "snooze"
    }
}
