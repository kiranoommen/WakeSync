package com.kiranoommen.wakesync.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.kiranoommen.wakesync.MainActivity
import com.kiranoommen.wakesync.data.WakeHistoryStore
import com.kiranoommen.wakesync.model.AlarmMode
import com.kiranoommen.wakesync.model.AlarmSchedule
import java.time.Duration
import java.time.ZonedDateTime

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()

    fun scheduleAll(schedules: List<AlarmSchedule>) {
        schedules.forEach { schedule ->
            cancel(schedule.id)
            if (schedule.enabled) {
                scheduleNext(schedule)
            }
        }
    }

    fun scheduleNext(
        schedule: AlarmSchedule,
        after: ZonedDateTime = ZonedDateTime.now()
    ) {
        if (!schedule.enabled || schedule.days.isEmpty()) return
        val deadline = schedule.nextDeadline(after) ?: return

        cancel(schedule.id)

        if (schedule.mode == AlarmMode.STANDARD) {
            setAlarmClock(
                schedule = schedule,
                time = deadline,
                kind = KIND_DEADLINE,
                reason = "Standard alarm"
            )
            return
        }

        val windowMinutes = schedule.smartWindowMinutes.coerceAtLeast(10)
        val earliest = deadline.minusMinutes(windowMinutes.toLong())
        val monitorStart = earliest.minusMinutes(MONITOR_LEAD_MINUTES)

        setExact(
            schedule = schedule,
            time = monitorStart,
            kind = KIND_MONITOR,
            reason = "Start Smart Wake monitoring"
        )

        val fallbackWindow = minOf(
            HISTORICAL_FALLBACK_WINDOW_MINUTES.toInt(),
            windowMinutes
        )

        val historical = WakeHistoryStore(context)
            .load()
            ?.let { profile ->
                PredictiveWakeEngine.chooseWakeTime(
                    profile = profile,
                    now = ZonedDateTime.now(),
                    deadline = deadline,
                    fallbackWindowMinutes = fallbackWindow
                )
            }
            ?.wakeAt
            ?.takeIf { it.isAfter(ZonedDateTime.now().plusSeconds(30)) && it.isBefore(deadline) }

        historical?.let {
            setExact(
                schedule = schedule,
                time = it,
                kind = KIND_HISTORICAL,
                reason = "Historical fallback from your recent sleep pattern"
            )
        }

        setAlarmClock(
            schedule = schedule,
            time = deadline,
            kind = KIND_DEADLINE,
            reason = "Hard wake deadline reached"
        )
    }

    fun cancel(scheduleId: String) {
        listOf(KIND_MONITOR, KIND_HISTORICAL, KIND_DEADLINE, KIND_SNOOZE)
            .forEach { kind ->
                pending(scheduleId, kind, 0L, "", PendingIntent.FLAG_NO_CREATE)
                    ?.let(alarmManager::cancel)
            }
    }

    fun snooze(scheduleId: String, minutes: Int) {
        if (minutes <= 0) return
        val schedule = com.kiranoommen.wakesync.data.AlarmStore(context)
            .load()
            .firstOrNull { it.id == scheduleId }
            ?: return

        val wakeAt = ZonedDateTime.now().plusMinutes(minutes.toLong())
        setAlarmClock(
            schedule = schedule,
            time = wakeAt,
            kind = KIND_SNOOZE,
            reason = "Snoozed alarm"
        )
    }

    private fun setExact(
        schedule: AlarmSchedule,
        time: ZonedDateTime,
        kind: String,
        reason: String
    ) {
        val pi = pending(
            scheduleId = schedule.id,
            kind = kind,
            deadlineMillis = schedule
                .nextBaseDeadline(time.minusDays(1))
                ?.toInstant()
                ?.toEpochMilli()
                ?: time.toInstant().toEpochMilli(),
            reason = reason,
            flags = PendingIntent.FLAG_UPDATE_CURRENT
        ) ?: return

        if (canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                time.toInstant().toEpochMilli(),
                pi
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                time.toInstant().toEpochMilli(),
                pi
            )
        }
    }

    private fun setAlarmClock(
        schedule: AlarmSchedule,
        time: ZonedDateTime,
        kind: String,
        reason: String
    ) {
        val deadlineMillis = time.toInstant().toEpochMilli()
        val operation = pending(
            scheduleId = schedule.id,
            kind = kind,
            deadlineMillis = deadlineMillis,
            reason = reason,
            flags = PendingIntent.FLAG_UPDATE_CURRENT
        ) ?: return

        if (canScheduleExactAlarms()) {
            val showIntent = PendingIntent.getActivity(
                context,
                (schedule.id + ":show").hashCode(),
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(deadlineMillis, showIntent),
                operation
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                deadlineMillis,
                operation
            )
        }
    }

    private fun pending(
        scheduleId: String,
        kind: String,
        deadlineMillis: Long,
        reason: String,
        flags: Int
    ): PendingIntent? {
        val receiver = if (kind == KIND_MONITOR) {
            WakeMonitorReceiver::class.java
        } else {
            AlarmReceiver::class.java
        }

        val intent = Intent(context, receiver)
            .putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            .putExtra(EXTRA_KIND, kind)
            .putExtra(EXTRA_DEADLINE_MILLIS, deadlineMillis)
            .putExtra(EXTRA_REASON, reason)

        return PendingIntent.getBroadcast(
            context,
            requestCode(scheduleId, kind),
            intent,
            flags or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun requestCode(scheduleId: String, kind: String): Int =
        (scheduleId + ":" + kind).hashCode()

    companion object {
        const val MONITOR_LEAD_MINUTES = 15L
        const val HISTORICAL_FALLBACK_WINDOW_MINUTES = 10L

        const val EXTRA_SCHEDULE_ID = "schedule_id"
        const val EXTRA_KIND = "alarm_kind"
        const val EXTRA_DEADLINE_MILLIS = "deadline_millis"
        const val EXTRA_REASON = "alarm_reason"

        const val KIND_MONITOR = "monitor"
        const val KIND_HISTORICAL = "historical"
        const val KIND_DEADLINE = "deadline"
        const val KIND_SNOOZE = "snooze"
    }
}
