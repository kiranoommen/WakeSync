package com.kiranoommen.wakesync.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class WakeAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        WakeAlarmController.ring(
            context = context,
            reason = "Hard wake deadline reached"
        )
    }
}
