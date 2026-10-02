package com.kiranoommen.wakesync.data

import android.content.Context

class AppSettingsStore(context: Context) {

    private val prefs =
        context.getSharedPreferences("wakesync_settings", Context.MODE_PRIVATE)

    var oobeStarted: Boolean
        get() = prefs.getBoolean(KEY_OOBE_STARTED, false)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_OOBE_STARTED, value)
                .apply()
        }

    var oobeCompleted: Boolean
        get() = prefs.getBoolean(KEY_OOBE_COMPLETED, false)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_OOBE_COMPLETED, value)
                .apply()
        }

    var themeMode: String
        get() = prefs.getString(KEY_THEME, THEME_DARK) ?: THEME_DARK
        set(value) {
            prefs.edit().putString(KEY_THEME, value).apply()
        }

    var sleepGoalMinutes: Int
        get() {
            val stored =
                prefs.getInt(
                    KEY_SLEEP_GOAL,
                    480
                )
                    .coerceIn(
                        240,
                        720
                    )
            return (
                (stored + 7) /
                    15 *
                    15
                )
                .coerceIn(
                    240,
                    720
                )
        }
        set(value) {
            val clamped =
                value.coerceIn(240, 720)
            val quarterHour =
                ((clamped + 7) / 15) * 15
            prefs.edit()
                .putInt(
                    KEY_SLEEP_GOAL,
                    quarterHour.coerceIn(240, 720)
                )
                .apply()
        }

    var goalsEnabled: Boolean
        get() = prefs.getBoolean(KEY_GOALS_ENABLED, true)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_GOALS_ENABLED, value)
                .apply()
        }

    var maxSmartWindowMinutes: Int
        get() = prefs.getInt(KEY_MAX_SMART_WINDOW, 30).coerceIn(10, 30)
        set(value) {
            prefs.edit()
                .putInt(KEY_MAX_SMART_WINDOW, value.coerceIn(10, 30))
                .apply()
        }

    fun hasExistingUserState(): Boolean =
        prefs.contains(KEY_THEME) ||
            prefs.contains(KEY_SLEEP_GOAL) ||
            prefs.contains(KEY_GOALS_ENABLED) ||
            prefs.contains(KEY_DISPLAY_NAME) ||
            prefs.contains(KEY_DASHBOARD_WIDGETS) ||
            prefs.contains(KEY_MAX_SMART_WINDOW) ||
            prefs.contains(KEY_RETAIN_EXPORTS)

    companion object {
        const val THEME_SYSTEM = "SYSTEM"
        const val THEME_DARK = "DARK"
        const val THEME_LIGHT = "LIGHT"

        const val WIDGET_GOAL = "GOAL"
        const val WIDGET_SLEEP = "SLEEP"
        const val WIDGET_INSIGHT = "INSIGHT"
        const val WIDGET_DEBT = "DEBT"
        const val WIDGET_HYPNOGRAM = "HYPNOGRAM"
        const val WIDGET_ALARM = "ALARM"

        private const val KEY_OOBE_STARTED = "oobe_started"
        private const val KEY_OOBE_COMPLETED = "oobe_completed"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_SLEEP_GOAL = "sleep_goal_minutes"
        private const val KEY_GOALS_ENABLED = "goals_enabled"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_MAX_SMART_WINDOW = "max_smart_window_minutes"
        private const val KEY_RETAIN_EXPORTS = "retain_generated_exports"
        private const val KEY_DASHBOARD_WIDGETS = "dashboard_widgets"
    }
}
