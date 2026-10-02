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
    val mode: AlarmMode = AlarmMode.SMART_WAKE,
    val smartWindowMinutes: Int = 20,
    val smartOffsetMinutes: Int = 0,
    val enabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val backupRingCount: Int = 0,
    val oneTimeDate: String? = null,
    val skippedDates: Set<String> = emptySet()
) {
    val isOneTime: Boolean
        get() = oneTimeDate != null

    fun isScheduledOn(date: LocalDate): Boolean =
        isBaseScheduledOn(date) &&
            !skippedDates.contains(date.toString())

    fun isBaseScheduledOn(date: LocalDate): Boolean {
        val oneTime = parsedOneTimeDate()
        return if (oneTime != null) {
            date == oneTime
        } else {
            days.contains(date.dayOfWeek.value)
        }
    }

    fun nextDeadline(
        after: ZonedDateTime = ZonedDateTime.now()
    ): ZonedDateTime? =
        nextOccurrence(
            after = after,
            respectSkips = true
        )

    fun nextBaseDeadline(
        after: ZonedDateTime = ZonedDateTime.now()
    ): ZonedDateTime? =
        nextOccurrence(
            after = after,
            respectSkips = false
        )

    fun isNextOccurrenceSkipped(
        after: ZonedDateTime = ZonedDateTime.now()
    ): Boolean {
        val nextBase =
            nextBaseDeadline(after) ?: return false
        return skippedDates.contains(
            nextBase.toLocalDate().toString()
        )
    }

    fun nextResumeDeadline(
        after: ZonedDateTime = ZonedDateTime.now()
    ): ZonedDateTime? {
        if (isOneTime) return null
        val nextBase =
            nextBaseDeadline(after) ?: return null
        return nextDeadline(
            nextBase.plusMinutes(1)
        )
    }

    fun futureSkippedDates(
        from: LocalDate = LocalDate.now()
    ): Set<String> =
        skippedDates.filterTo(mutableSetOf()) { value ->
            runCatching {
                !LocalDate.parse(value).isBefore(from)
            }.getOrDefault(false)
        }

    private fun parsedOneTimeDate(): LocalDate? =
        oneTimeDate?.let {
            runCatching {
                LocalDate.parse(it)
            }.getOrNull()
        }

    private fun nextOccurrence(
        after: ZonedDateTime,
        respectSkips: Boolean
    ): ZonedDateTime? {
        if (!enabled) return null

        val oneTime = parsedOneTimeDate()
        if (oneTime != null) {
            if (
                respectSkips &&
                skippedDates.contains(oneTime.toString())
            ) {
                return null
            }

            val candidate =
                oneTime
                    .atTime(hour, minute)
                    .atZone(after.zone)

            return candidate.takeIf {
                it.isAfter(after)
            }
        }

        if (days.isEmpty()) return null

        // Search a full year so temporary skips can never make a valid
        // recurring schedule appear to disappear from the Home screen.
        for (offset in 0..370) {
            val date =
                after.toLocalDate()
                    .plusDays(offset.toLong())

            if (!days.contains(date.dayOfWeek.value)) {
                continue
            }

            if (
                respectSkips &&
                skippedDates.contains(date.toString())
            ) {
                continue
            }

            val candidate =
                date
                    .atTime(hour, minute)
                    .atZone(after.zone)

            if (candidate.isAfter(after)) {
                return candidate
            }
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

        val EVERY_DAY = (1..7).toSet()
    }
}
