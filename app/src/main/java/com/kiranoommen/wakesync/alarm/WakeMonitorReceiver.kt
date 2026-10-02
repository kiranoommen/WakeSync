package com.kiranoommen.wakesync.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class WakeMonitorReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val scheduleId =
            intent?.getStringExtra(AlarmScheduler.EXTRA_SCHEDULE_ID) ?: return
        val deadlineMillis =
            intent.getLongExtra(AlarmScheduler.EXTRA_DEADLINE_MILLIS, 0L)

        val serviceIntent = Intent(context, WakeMonitorService::class.java)
            .putExtra(AlarmScheduler.EXTRA_SCHEDULE_ID, scheduleId)
            .putExtra(AlarmScheduler.EXTRA_DEADLINE_MILLIS, deadlineMillis)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
