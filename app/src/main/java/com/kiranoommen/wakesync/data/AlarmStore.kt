package com.kiranoommen.wakesync.data

import android.content.Context
import com.kiranoommen.wakesync.model.AlarmSchedule

class AlarmStore(context: Context) {

    private val prefs = context.getSharedPreferences("wakesync_alarms", Context.MODE_PRIVATE)

    fun load(): List<AlarmSchedule> {
        return prefs.getStringSet(KEY_ALARMS, emptySet())
            .orEmpty()
            .mapNotNull(::decode)
            .sortedWith(compareBy<AlarmSchedule> { it.hour }.thenBy { it.minute })
    }

    fun save(schedules: List<AlarmSchedule>) {
        prefs.edit()
            .putStringSet(KEY_ALARMS, schedules.map(::encode).toSet())
            .apply()
    }

    private fun encode(schedule: AlarmSchedule): String {
        val safeLabel = schedule.label
            .replace("|", " ")
            .replace("\n", " ")
        return listOf(
            schedule.id,
            safeLabel,
            schedule.hour.toString(),
            schedule.minute.toString(),
            schedule.days.sorted().joinToString(","),
            schedule.smartWindowMinutes.toString(),
            schedule.smartOffsetMinutes.toString(),
            schedule.enabled.toString(),
            schedule.skippedDates.sorted().joinToString(",")
        ).joinToString("|")
    }

    private fun decode(value: String): AlarmSchedule? {
        val parts = value.split("|")
        if (parts.size < 9) return null

        return runCatching {
            AlarmSchedule(
                id = parts[0],
                label = parts[1],
                hour = parts[2].toInt(),
                minute = parts[3].toInt(),
                days = parts[4]
                    .split(",")
                    .filter { it.isNotBlank() }
                    .map { it.toInt() }
                    .toSet(),
                smartWindowMinutes = parts[5].toInt(),
                smartOffsetMinutes = parts[6].toInt(),
                enabled = parts[7].toBoolean(),
                skippedDates = parts[8]
                    .split(",")
                    .filter { it.isNotBlank() }
                    .toSet()
            )
        }.getOrNull()
    }

    companion object {
        private const val KEY_ALARMS = "alarms"
    }
}
