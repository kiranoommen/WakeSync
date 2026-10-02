package com.kiranoommen.wakesync.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kiranoommen.wakesync.data.WakePreferencesStore

class ScheduleRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val preferences = WakePreferencesStore(context).load()
        if (preferences.enabled) {
            WakeAlarmScheduler.schedule(context, preferences)
        }
    }
}
