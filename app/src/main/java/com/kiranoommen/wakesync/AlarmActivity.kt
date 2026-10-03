package com.kiranoommen.wakesync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.kiranoommen.wakesync.alarm.AlarmScheduler
import com.kiranoommen.wakesync.alarm.MultiAlarmController
import com.kiranoommen.wakesync.data.AlarmStore
import com.kiranoommen.wakesync.ui.alarmringing.AlarmRingingScreen
import com.kiranoommen.wakesync.ui.theme.WakeSyncTheme

class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setShowWhenLocked(true)
        setTurnScreenOn(true)

        val scheduleId =
            intent.getStringExtra(
                MultiAlarmController.EXTRA_SCHEDULE_ID
            )
        val reason =
            intent.getStringExtra(
                MultiAlarmController.EXTRA_REASON
            ) ?: "WakeSync alarm"
        val kind =
            intent.getStringExtra(
                MultiAlarmController.EXTRA_KIND
            ) ?: AlarmScheduler.KIND_DEADLINE
        val deadlineMillis =
            intent.getLongExtra(
                MultiAlarmController.EXTRA_DEADLINE_MILLIS,
                0L
            )

        val schedule = scheduleId?.let { id ->
            AlarmStore(this)
                .load()
                .firstOrNull { it.id == id }
        }

        setContent {
            WakeSyncTheme(darkTheme = true) {
                AlarmRingingScreen(
                    reason = reason,
                    kind = kind,
                    schedule = schedule,
                    onDismissCurrent = {
                        MultiAlarmController.dismissCurrent(
                            this@AlarmActivity
                        )
                        finish()
                    },
                    onStopSequence = {
                        val id = schedule?.id
                        if (id != null) {
                            MultiAlarmController.stopSequence(
                                context = this@AlarmActivity,
                                scheduleId = id,
                                deadlineMillis = deadlineMillis
                            )
                        } else {
                            MultiAlarmController.dismissCurrent(
                                this@AlarmActivity
                            )
                        }
                        finish()
                    },
                )
            }
        }
    }
}

