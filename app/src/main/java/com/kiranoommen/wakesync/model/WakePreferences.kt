package com.kiranoommen.wakesync.model

import java.time.LocalTime

data class WakePreferences(
    val earliestHour: Int = 6,
    val earliestMinute: Int = 20,
    val latestHour: Int = 7,
    val latestMinute: Int = 0,
    val enabled: Boolean = false
) {
    val earliest: LocalTime
        get() = LocalTime.of(earliestHour, earliestMinute)

    val latest: LocalTime
        get() = LocalTime.of(latestHour, latestMinute)

    fun hasValidRange(): Boolean = latest.isAfter(earliest)
}
