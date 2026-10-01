package com.kiranoommen.wakesync.data

import android.content.Context

class AppSettingsStore(context: Context) {

    private val prefs =
        context.getSharedPreferences("wakesync_settings", Context.MODE_PRIVATE)

    var themeMode: String
        get() = prefs.getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM
        set(value) {
            prefs.edit().putString(KEY_THEME, value).apply()
        }

    var sleepGoalMinutes: Int
        get() = prefs.getInt(KEY_SLEEP_GOAL, 480)
        set(value) {
            prefs.edit()
                .putInt(KEY_SLEEP_GOAL, value.coerceIn(360, 600))
                .apply()
        }

    companion object {
        const val THEME_SYSTEM = "SYSTEM"
        const val THEME_DARK = "DARK"
        const val THEME_LIGHT = "LIGHT"

        private const val KEY_THEME = "theme_mode"
        private const val KEY_SLEEP_GOAL = "sleep_goal_minutes"
    }
}
