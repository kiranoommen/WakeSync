package com.kiranoommen.wakesync.alarm

import android.content.Context
import android.content.Intent
import android.os.Build
import com.kiranoommen.wakesync.data.WakePreferencesStore

object WakeAlarmController {

    const val EXTRA_REASON = "wake_reason"

    fun ring(context: Context, reason: String) {
        val appContext = context.applicationContext
        val preferences = WakePreferencesStore(appContext).load()

        WakeAlarmScheduler.cancelAll(appContext)
        if (preferences.enabled) {
            WakeAlarmScheduler.scheduleTomorrow(appContext, preferences)
        }

        appContext.stopService(Intent(appContext, WakeMonitorService::class.java))

        val intent = Intent(appContext, AlarmRingingService::class.java)
            .putExtra(EXTRA_REASON, reason)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startForegroundService(intent)
        } else {
            appContext.startService(intent)
        }
    }

    fun markAwake(context: Context) {
        val appContext = context.applicationContext
        val preferences = WakePreferencesStore(appContext).load()

        WakeAlarmScheduler.cancelAll(appContext)
        appContext.stopService(Intent(appContext, WakeMonitorService::class.java))
        appContext.stopService(Intent(appContext, AlarmRingingService::class.java))

        if (preferences.enabled) {
            WakeAlarmScheduler.scheduleTomorrow(appContext, preferences)
        }
    }

    fun dismiss(context: Context) {
        context.applicationContext.stopService(
            Intent(context.applicationContext, AlarmRingingService::class.java)
        )
    }
}
