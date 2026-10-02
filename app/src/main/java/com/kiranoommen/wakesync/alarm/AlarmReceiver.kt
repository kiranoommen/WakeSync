package com.kiranoommen.wakesync.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId =
            intent.getStringExtra(AlarmScheduler.EXTRA_SCHEDULE_ID) ?: return
        val kind =
            intent.getStringExtra(AlarmScheduler.EXTRA_KIND)
                ?: AlarmScheduler.KIND_DEADLINE
        val deadlineMillis =
            intent.getLongExtra(AlarmScheduler.EXTRA_DEADLINE_MILLIS, 0L)
        val reason =
            intent.getStringExtra(AlarmScheduler.EXTRA_REASON)
                ?: "WakeSync alarm"

        MultiAlarmController.ring(
            context = context,
            scheduleId = scheduleId,
            kind = kind,
            deadlineMillis = deadlineMillis,
            reason = reason
        )
    }
}
