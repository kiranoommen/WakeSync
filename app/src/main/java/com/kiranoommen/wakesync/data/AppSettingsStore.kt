package com.kiranoommen.wakesync.data

import android.content.Context

class AppSettingsStore(context: Context) {

    private val prefs =
        context.getSharedPreferences("wakesync_settings", Context.MODE_PRIVATE)

    var themeMode: String
        get() = prefs.getString(KEY_THEME, THEME_DARK) ?: THEME_DARK
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

    var maxSmartWindowMinutes: Int
        get() = prefs.getInt(KEY_MAX_SMART_WINDOW, 30)
        set(value) {
            prefs.edit()
                .putInt(KEY_MAX_SMART_WINDOW, value.coerceIn(10, 45))
                .apply()
        }

    var retainGeneratedExports: Boolean
        get() = prefs.getBoolean(KEY_RETAIN_EXPORTS, false)
        set(value) {
            prefs.edit()
                .putBoolean(KEY_RETAIN_EXPORTS, value)
                .apply()
        }

    var dashboardWidgets: List<String>
        get() = prefs.getString(KEY_DASHBOARD_WIDGETS, null)
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?: DEFAULT_DASHBOARD_WIDGETS
        set(value) {
            prefs.edit()
                .putString(KEY_DASHBOARD_WIDGETS, value.distinct().joinToString(","))
                .apply()
        }

    companion object {
        const val THEME_SYSTEM = "SYSTEM"
        const val THEME_DARK = "DARK"
        const val THEME_LIGHT = "LIGHT"

        const val WIDGET_GOAL = "GOAL"
        const val WIDGET_SLEEP = "SLEEP"
        const val WIDGET_INSIGHT = "INSIGHT"

        val DEFAULT_DASHBOARD_WIDGETS =
            listOf(WIDGET_GOAL, WIDGET_SLEEP, WIDGET_INSIGHT)

        private const val KEY_THEME = "theme_mode"
        private const val KEY_SLEEP_GOAL = "sleep_goal_minutes"
        private const val KEY_MAX_SMART_WINDOW = "max_smart_window_minutes"
        private const val KEY_RETAIN_EXPORTS = "retain_generated_exports"
        private const val KEY_DASHBOARD_WIDGETS = "dashboard_widgets"
    }
}
