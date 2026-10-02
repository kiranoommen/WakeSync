package com.kiranoommen.wakesync.data

import android.content.Context
import com.kiranoommen.wakesync.model.WakePreferences

class WakePreferencesStore(context: Context) {

    private val preferences =
        context.getSharedPreferences("wake_preferences", Context.MODE_PRIVATE)

    fun load(): WakePreferences = WakePreferences(
        earliestHour = preferences.getInt(KEY_EARLIEST_HOUR, 6),
        earliestMinute = preferences.getInt(KEY_EARLIEST_MINUTE, 20),
        latestHour = preferences.getInt(KEY_LATEST_HOUR, 7),
        latestMinute = preferences.getInt(KEY_LATEST_MINUTE, 0),
        enabled = preferences.getBoolean(KEY_ENABLED, false)
    )

    fun save(value: WakePreferences) {
        preferences.edit()
            .putInt(KEY_EARLIEST_HOUR, value.earliestHour)
            .putInt(KEY_EARLIEST_MINUTE, value.earliestMinute)
            .putInt(KEY_LATEST_HOUR, value.latestHour)
            .putInt(KEY_LATEST_MINUTE, value.latestMinute)
            .putBoolean(KEY_ENABLED, value.enabled)
            .apply()
    }

    private companion object {
        const val KEY_EARLIEST_HOUR = "earliest_hour"
        const val KEY_EARLIEST_MINUTE = "earliest_minute"
        const val KEY_LATEST_HOUR = "latest_hour"
        const val KEY_LATEST_MINUTE = "latest_minute"
        const val KEY_ENABLED = "enabled"
    }
}
