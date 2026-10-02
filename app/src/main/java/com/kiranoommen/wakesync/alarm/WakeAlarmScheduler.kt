package com.kiranoommen.wakesync.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.kiranoommen.wakesync.MainActivity
import com.kiranoommen.wakesync.data.WakeHistoryStore
import com.kiranoommen.wakesync.model.WakePreferences
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

object WakeAlarmScheduler {

    const val MONITOR_LEAD_MINUTES = 15L
    const val HISTORICAL_FALLBACK_WINDOW_MINUTES = 10L

    const val EXTRA_ALARM_REASON = "alarm_reason"

    private const val MONITOR_REQUEST_CODE = 4101
    private const val DEADLINE_REQUEST_CODE = 4102
    private const val PREDICTIVE_REQUEST_CODE = 4104

    data class ScheduleResult(
        val scheduled: Boolean,
        val monitorStart: ZonedDateTime? = null,
        val earliestWake: ZonedDateTime? = null,
        val historicalFallbackWake: ZonedDateTime? = null,
        val hardDeadline: ZonedDateTime? = null
    )

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        return alarmManager.canScheduleExactAlarms()
    }

    fun schedule(context: Context, preferences: WakePreferences): ScheduleResult {
        if (!preferences.enabled || !preferences.hasValidRange()) {
            cancelAll(context)
            return ScheduleResult(false)
        }

        val now = ZonedDateTime.now()
        var date = now.toLocalDate()
        val todayDeadline = date.atTime(preferences.latest).atZone(now.zone)

        if (!now.isBefore(todayDeadline)) {
            date = date.plusDays(1)
        }

        return scheduleForDate(context, preferences, date)
    }

    fun scheduleTomorrow(
        context: Context,
        preferences: WakePreferences
    ): ScheduleResult {
        if (!preferences.enabled || !preferences.hasValidRange()) {
            return ScheduleResult(false)
        }

        return scheduleForDate(
            context = context,
            preferences = preferences,
            date = ZonedDateTime.now().toLocalDate().plusDays(1)
        )
    }

    fun schedulePredictiveWake(
        context: Context,
        wakeAt: ZonedDateTime,
        reason: String
    ): Boolean {
        if (!canScheduleExactAlarms(context)) return false

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            wakeAt.toInstant().toEpochMilli(),
            predictivePendingIntent(context, reason)
        )
        return true
    }

    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(monitorPendingIntent(context))
        alarmManager.cancel(deadlinePendingIntent(context))
        alarmManager.cancel(predictivePendingIntent(context, ""))
    }

    fun cancelDeadline(context: Context) {
        context.getSystemService(AlarmManager::class.java)
            .cancel(deadlinePendingIntent(context))
    }

    fun cancelPredictive(context: Context) {
        context.getSystemService(AlarmManager::class.java)
            .cancel(predictivePendingIntent(context, ""))
    }

    private fun scheduleForDate(
        context: Context,
        preferences: WakePreferences,
        date: LocalDate
    ): ScheduleResult {
        if (!canScheduleExactAlarms(context)) return ScheduleResult(false)

        cancelPredictive(context)

        val now = ZonedDateTime.now()
        val zone = now.zone
        val earliest = date.atTime(preferences.earliest).atZone(zone)
        val deadline = date.atTime(preferences.latest).atZone(zone)

        val requestedMonitorStart = earliest.minusMinutes(MONITOR_LEAD_MINUTES)
        val monitorStart = if (requestedMonitorStart.isAfter(now)) {
            requestedMonitorStart
        } else {
            now.plusSeconds(2)
        }

        val requestedFallbackStart =
            deadline.minusMinutes(HISTORICAL_FALLBACK_WINDOW_MINUTES)
        val fallbackStart = if (requestedFallbackStart.isAfter(earliest)) {
            requestedFallbackStart
        } else {
            earliest
        }

        val fallbackWindowMinutes = Duration.between(
            fallbackStart,
            deadline
        ).toMinutes().toInt().coerceAtLeast(0)

        val historicalFallbackWake = WakeHistoryStore(context)
            .load()
            ?.let { profile ->
                PredictiveWakeEngine.chooseWakeTime(
                    profile = profile,
                    now = now,
                    deadline = deadline,
                    fallbackWindowMinutes = fallbackWindowMinutes
                )
            }
            ?.wakeAt
            ?.takeIf { candidate ->
                candidate.isAfter(now.plusSeconds(30)) &&
                    candidate.isBefore(deadline)
            }

        val alarmManager = context.getSystemService(AlarmManager::class.java)

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            monitorStart.toInstant().toEpochMilli(),
            monitorPendingIntent(context)
        )

        historicalFallbackWake?.let { wakeAt ->
            schedulePredictiveWake(
                context = context,
                wakeAt = wakeAt,
                reason = "Historical fallback from your recent sleep pattern"
            )
        }

        val showAlarmIntent = PendingIntent.getActivity(
            context,
            4103,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(
                deadline.toInstant().toEpochMilli(),
                showAlarmIntent
            ),
            deadlinePendingIntent(context)
        )

        return ScheduleResult(
            scheduled = true,
            monitorStart = monitorStart,
            earliestWake = earliest,
            historicalFallbackWake = historicalFallbackWake,
            hardDeadline = deadline
        )
    }

    private fun monitorPendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            MONITOR_REQUEST_CODE,
            Intent(context, WakeMonitorReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun deadlinePendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            DEADLINE_REQUEST_CODE,
            Intent(context, WakeAlarmReceiver::class.java).putExtra(
                EXTRA_ALARM_REASON,
                "Hard wake deadline reached"
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun predictivePendingIntent(
        context: Context,
        reason: String
    ): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            PREDICTIVE_REQUEST_CODE,
            Intent(context, WakeAlarmReceiver::class.java).putExtra(
                EXTRA_ALARM_REASON,
                reason
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
