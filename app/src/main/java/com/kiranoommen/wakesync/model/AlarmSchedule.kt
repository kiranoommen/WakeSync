package com.kiranoommen.wakesync.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZonedDateTime

data class AlarmSchedule(
    val id: String,
    val label: String,
    val hour: Int,
    val minute: Int,
    val days: Set<Int>,
    val smartWindowMinutes: Int = 20,
    val smartOffsetMinutes: Int = 0,
    val enabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val snoozeMinutes: Int = 5,
    val skippedDates: Set<String> = emptySet()
) {
    fun isScheduledOn(date: LocalDate): Boolean =
        days.contains(date.dayOfWeek.value) && !skippedDates.contains(date.toString())

    fun nextDeadline(after: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime? {
        if (!enabled || days.isEmpty()) return null

        for (offset in 0..8) {
            val date = after.toLocalDate().plusDays(offset.toLong())
            if (!isScheduledOn(date)) continue

            val candidate = date
                .atTime(hour, minute)
                .atZone(after.zone)

            if (candidate.isAfter(after)) return candidate
        }
        return null
    }

    companion object {
        val WEEKDAYS = setOf(
            DayOfWeek.MONDAY.value,
            DayOfWeek.TUESDAY.value,
            DayOfWeek.WEDNESDAY.value,
            DayOfWeek.THURSDAY.value,
            DayOfWeek.FRIDAY.value
        )
    }
}
