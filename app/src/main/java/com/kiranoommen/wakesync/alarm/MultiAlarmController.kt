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
    const val EXTRA_DEADLINE_MILLIS =
        "ring_deadline_millis"

    fun ring(
        context: Context,
        scheduleId: String,
        kind: String,
        deadlineMillis: Long,
        reason: String
    ) {
        val appContext = context.applicationContext
        val store = AlarmStore(appContext)
        val schedule =
            store.load().firstOrNull { it.id == scheduleId }
                ?: return
        val scheduler = AlarmScheduler(appContext)

        when {
            kind == KIND_LIVE ||
                kind == AlarmScheduler.KIND_HISTORICAL -> {
                // The early Smart Wake ring should stop competing smart
                // triggers, but the hard deadline and backup rings stay armed.
                scheduler.cancelSmartOptimizers(scheduleId)
            }

            AlarmScheduler.backupIndex(kind) != null -> {
                val index =
                    AlarmScheduler.backupIndex(kind) ?: 0
                if (index >= schedule.backupRingCount) {
                    scheduleNextOccurrence(
                        scheduler = scheduler,
                        schedule = schedule,
                        deadlineMillis = deadlineMillis
                    )
                }
            }

            kind == AlarmScheduler.KIND_DEADLINE -> {
                if (schedule.backupRingCount <= 0) {
                    scheduleNextOccurrence(
                        scheduler = scheduler,
                        schedule = schedule,
                        deadlineMillis = deadlineMillis
                    )
                }
            }
        }

        val serviceIntent =
            Intent(appContext, AlarmRingingService::class.java)
                .putExtra(EXTRA_SCHEDULE_ID, scheduleId)
                .putExtra(EXTRA_REASON, reason)
                .putExtra(EXTRA_KIND, kind)
                .putExtra(
                    EXTRA_DEADLINE_MILLIS,
                    deadlineMillis
                )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startForegroundService(serviceIntent)
        } else {
            appContext.startService(serviceIntent)
        }
    }

    fun dismissCurrent(context: Context) {
        context.applicationContext.stopService(
            Intent(
                context.applicationContext,
                AlarmRingingService::class.java
            )
        )
    }

    fun stopSequence(
        context: Context,
        scheduleId: String,
        deadlineMillis: Long
    ) {
        val appContext = context.applicationContext
        val schedule = AlarmStore(appContext)
            .load()
            .firstOrNull { it.id == scheduleId }

        val scheduler = AlarmScheduler(appContext)
        scheduler.cancel(scheduleId)

        if (schedule != null && schedule.enabled) {
            scheduleNextOccurrence(
                scheduler = scheduler,
                schedule = schedule,
                deadlineMillis = deadlineMillis
            )
        }

        dismissCurrent(appContext)
    }

    fun snooze(
        context: Context,
        scheduleId: String,
        minutes: Int,
        deadlineMillis: Long
    ) {
        if (minutes <= 0) return

        AlarmScheduler(context.applicationContext)
            .snooze(
                scheduleId = scheduleId,
                minutes = minutes,
                sequenceDeadlineMillis = deadlineMillis
            )

        dismissCurrent(context)
    }

    private fun scheduleNextOccurrence(
        scheduler: AlarmScheduler,
        schedule: com.kiranoommen.wakesync.model.AlarmSchedule,
        deadlineMillis: Long
    ) {
        val after =
            if (deadlineMillis > 0L) {
                ZonedDateTime.ofInstant(
                    Instant.ofEpochMilli(deadlineMillis),
                    ZoneId.systemDefault()
                ).plusMinutes(
                    AlarmScheduler.BACKUP_INTERVAL_MINUTES *
                        schedule.backupRingCount.coerceAtLeast(0) +
                        1L
                )
            } else {
                ZonedDateTime.now().plusMinutes(1)
            }

        scheduler.scheduleNext(
            schedule = schedule,
            after = after,
            clearExisting = true
        )
    }

    private const val KIND_LIVE = "live"
}
