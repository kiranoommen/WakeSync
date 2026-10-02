package com.kiranoommen.wakesync.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kiranoommen.wakesync.data.AlarmStore

class ScheduleRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        AlarmScheduler(context).scheduleAll(
            AlarmStore(context).load()
        )
    }
}
