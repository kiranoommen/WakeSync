package com.kiranoommen.wakesync.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.kiranoommen.wakesync.MainActivity
import com.kiranoommen.wakesync.data.AlarmStore
import com.kiranoommen.wakesync.data.WakeHistoryStore
import com.kiranoommen.wakesync.model.AlarmMode
import com.kiranoommen.wakesync.model.AlarmSchedule
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class AlarmScheduler(private val context: Context) {

    private val alarmManager =
        context.getSystemService(AlarmManager::class.java)

    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()

    fun scheduleAll(schedules: List<AlarmSchedule>) {
        schedules.forEach { schedule ->
            cancel(schedule.id)
            if (schedule.enabled) {
                scheduleNext(
                    schedule = schedule,
                    clearExisting = false
                )
            }
        }
    }

    fun scheduleNext(
        schedule: AlarmSchedule,
        after: ZonedDateTime = ZonedDateTime.now(),
        clearExisting: Boolean = true
    ) {
        if (
            !schedule.enabled ||
            (
                schedule.oneTimeDate == null &&
                    schedule.days.isEmpty()
                )
        ) return

        val deadline = schedule.nextDeadline(after) ?: return

        if (clearExisting) {
            cancel(schedule.id)
        }

        if (schedule.mode == AlarmMode.STANDARD) {
            setAlarmClock(
                schedule = schedule,
                time = deadline,
                sequenceDeadline = deadline,
                kind = KIND_DEADLINE,
                reason = "Standard alarm"
            )
            scheduleBackupRings(schedule, deadline)
            return
        }

        val now = ZonedDateTime.now(deadline.zone)
        val windowMinutes =
            schedule.smartWindowMinutes.coerceAtLeast(10)
        val earliest =
            deadline.minusMinutes(windowMinutes.toLong())
        val requestedMonitorStart =
            earliest.minusMinutes(MONITOR_LEAD_MINUTES)
        val monitorStart =
            if (requestedMonitorStart.isAfter(now)) {
                requestedMonitorStart
            } else {
                now.plusSeconds(2)
            }

        if (monitorStart.isBefore(deadline)) {
            setExact(
                schedule = schedule,
                time = monitorStart,
                sequenceDeadline = deadline,
                kind = KIND_MONITOR,
                reason = "Start Smart Wake monitoring"
            )
        }

        val fallbackWindow = minOf(
            HISTORICAL_FALLBACK_WINDOW_MINUTES.toInt(),
            windowMinutes
        )

        val historical = WakeHistoryStore(context)
            .load()
            ?.let { profile ->
                PredictiveWakeEngine.chooseWakeTime(
                    profile = profile,
                    now = now,
                    deadline = deadline,
                    fallbackWindowMinutes = fallbackWindow
                )
            }
            ?.wakeAt
            ?.takeIf {
                it.isAfter(now.plusSeconds(30)) &&
                    it.isBefore(deadline)
            }

        historical?.let {
            setExact(
                schedule = schedule,
                time = it,
                sequenceDeadline = deadline,
                kind = KIND_HISTORICAL,
                reason =
                    "Historical fallback from your recent sleep pattern"
            )
        }

        setAlarmClock(
            schedule = schedule,
            time = deadline,
            sequenceDeadline = deadline,
            kind = KIND_DEADLINE,
            reason = "Hard wake deadline reached"
        )

        scheduleBackupRings(schedule, deadline)
    }

    fun cancel(scheduleId: String) {
        cancelSmartOptimizers(scheduleId)
        cancelKind(scheduleId, KIND_DEADLINE)
        cancelKind(scheduleId, KIND_SNOOZE)

        for (index in 1..MAX_BACKUP_RINGS) {
            cancelKind(
                scheduleId = scheduleId,
                kind = backupKind(index)
            )
        }
    }

    fun cancelSmartOptimizers(scheduleId: String) {
        cancelKind(scheduleId, KIND_MONITOR)
        cancelKind(scheduleId, KIND_HISTORICAL)
    }

    fun snooze(
        scheduleId: String,
        minutes: Int,
        sequenceDeadlineMillis: Long
    ) {
        if (minutes <= 0) return

        val schedule = AlarmStore(context)
            .load()
            .firstOrNull { it.id == scheduleId }
            ?: return

        val wakeAt =
            ZonedDateTime.now().plusMinutes(minutes.toLong())
        val sequenceDeadline =
            if (sequenceDeadlineMillis > 0L) {
                ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(sequenceDeadlineMillis),
                    ZoneId.systemDefault()
                )
            } else {
                wakeAt
            }

        setAlarmClock(
            schedule = schedule,
            time = wakeAt,
            sequenceDeadline = sequenceDeadline,
            kind = KIND_SNOOZE,
            reason = "Snoozed alarm"
        )
    }

    private fun scheduleBackupRings(
        schedule: AlarmSchedule,
        deadline: ZonedDateTime
    ) {
        val count =
            schedule.backupRingCount.coerceIn(0, MAX_BACKUP_RINGS)

        for (index in 1..count) {
            val time =
                deadline.plusMinutes(
                    BACKUP_INTERVAL_MINUTES * index.toLong()
                )

            setAlarmClock(
                schedule = schedule,
                time = time,
                sequenceDeadline = deadline,
                kind = backupKind(index),
                reason = "Backup ring $index of $count"
            )
        }
    }

    private fun setExact(
        schedule: AlarmSchedule,
        time: ZonedDateTime,
        sequenceDeadline: ZonedDateTime,
        kind: String,
        reason: String
    ) {
        val pi = pending(
            scheduleId = schedule.id,
            kind = kind,
            deadlineMillis =
                sequenceDeadline.toInstant().toEpochMilli(),
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
        sequenceDeadline: ZonedDateTime,
        kind: String,
        reason: String
    ) {
        val triggerMillis =
            time.toInstant().toEpochMilli()
        val operation = pending(
            scheduleId = schedule.id,
            kind = kind,
            deadlineMillis =
                sequenceDeadline.toInstant().toEpochMilli(),
            reason = reason,
            flags = PendingIntent.FLAG_UPDATE_CURRENT
        ) ?: return

        if (canScheduleExactAlarms()) {
            val showIntent = PendingIntent.getActivity(
                context,
                (schedule.id + ":show").hashCode(),
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(
                    triggerMillis,
                    showIntent
                ),
                operation
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                operation
            )
        }
    }

    private fun cancelKind(
        scheduleId: String,
        kind: String
    ) {
        pending(
            scheduleId = scheduleId,
            kind = kind,
            deadlineMillis = 0L,
            reason = "",
            flags = PendingIntent.FLAG_NO_CREATE
        )?.let(alarmManager::cancel)
    }

    private fun pending(
        scheduleId: String,
        kind: String,
        deadlineMillis: Long,
        reason: String,
        flags: Int
    ): PendingIntent? {
        val receiver =
            if (kind == KIND_MONITOR) {
                WakeMonitorReceiver::class.java
            } else {
                AlarmReceiver::class.java
            }

        val intent = Intent(context, receiver)
            .putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            .putExtra(EXTRA_KIND, kind)
            .putExtra(
                EXTRA_DEADLINE_MILLIS,
                deadlineMillis
            )
            .putExtra(EXTRA_REASON, reason)

        return PendingIntent.getBroadcast(
            context,
            requestCode(scheduleId, kind),
            intent,
            flags or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun requestCode(
        scheduleId: String,
        kind: String
    ): Int =
        (scheduleId + ":" + kind).hashCode()

    companion object {
        const val MONITOR_LEAD_MINUTES = 15L
        const val HISTORICAL_FALLBACK_WINDOW_MINUTES = 10L
        const val BACKUP_INTERVAL_MINUTES = 5L
        const val MAX_BACKUP_RINGS = 3

        const val EXTRA_SCHEDULE_ID = "schedule_id"
        const val EXTRA_KIND = "alarm_kind"
        const val EXTRA_DEADLINE_MILLIS = "deadline_millis"
        const val EXTRA_REASON = "alarm_reason"

        const val KIND_MONITOR = "monitor"
        const val KIND_HISTORICAL = "historical"
        const val KIND_DEADLINE = "deadline"
        const val KIND_SNOOZE = "snooze"

        fun backupKind(index: Int): String =
            "backup_${index.coerceIn(1, MAX_BACKUP_RINGS)}"

        fun backupIndex(kind: String): Int? =
            kind.removePrefix("backup_")
                .takeIf { kind.startsWith("backup_") }
                ?.toIntOrNull()
                ?.takeIf { it in 1..MAX_BACKUP_RINGS }
    }
}
