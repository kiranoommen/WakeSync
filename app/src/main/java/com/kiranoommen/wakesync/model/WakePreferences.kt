package com.kiranoommen.wakesync.model

import java.time.LocalTime

enum class AlarmMode {
    SMART_WAKE,
    STANDARD
}

data class WakePreferences(
    val earliestHour: Int = 6,
    val earliestMinute: Int = 20,
    val latestHour: Int = 7,
    val latestMinute: Int = 0,
    val standardHour: Int = 7,
    val standardMinute: Int = 0,
    val mode: AlarmMode = AlarmMode.SMART_WAKE,
    val enabled: Boolean = false
) {
    val earliest: LocalTime
        get() = LocalTime.of(earliestHour, earliestMinute)

    val latest: LocalTime
        get() = LocalTime.of(latestHour, latestMinute)

    val standardTime: LocalTime
        get() = LocalTime.of(standardHour, standardMinute)

    fun hasValidRange(): Boolean = latest.isAfter(earliest)

    fun hasValidSchedule(): Boolean =
        when (mode) {
            AlarmMode.SMART_WAKE -> hasValidRange()
            AlarmMode.STANDARD -> true
        }
}
