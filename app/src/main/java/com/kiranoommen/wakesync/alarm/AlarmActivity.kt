package com.kiranoommen.wakesync.alarm

import android.app.NotificationManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kiranoommen.wakesync.data.AlarmStore
import com.kiranoommen.wakesync.ui.theme.Amber
import com.kiranoommen.wakesync.ui.theme.Indigo
import com.kiranoommen.wakesync.ui.theme.Lavender
import com.kiranoommen.wakesync.ui.theme.WakeSyncTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class AlarmActivity : ComponentActivity() {

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setShowWhenLocked(true)
        setTurnScreenOn(true)

        val scheduleId = intent.getStringExtra(AlarmScheduler.EXTRA_SCHEDULE_ID).orEmpty()
        val kind = intent.getStringExtra(AlarmScheduler.EXTRA_KIND).orEmpty()
        val schedule = AlarmStore(this).load().firstOrNull { it.id == scheduleId }

        startAlarmFeedback()

        setContent {
            WakeSyncTheme(darkTheme = true) {
                AlarmScreen(
                    label = schedule?.label?.ifBlank { "WakeSync" } ?: "WakeSync",
                    isSmart = kind == AlarmScheduler.KIND_SMART,
                    onDismiss = {
                        AlarmScheduler(this).cancelDeadline(scheduleId)
                        stopAlarmFeedback()
                        getSystemService(NotificationManager::class.java)
                            .cancel(scheduleId.hashCode())
                        finish()
                    },
                    onSnooze = {
                        AlarmScheduler(this).cancelDeadline(scheduleId)
                        AlarmScheduler(this).snooze(scheduleId, 5)
                        stopAlarmFeedback()
                        getSystemService(NotificationManager::class.java)
                            .cancel(scheduleId.hashCode())
                        finish()
                    }
                )
            }
        }
    }

    private fun startAlarmFeedback() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ringtone = RingtoneManager.getRingtone(this, uri)?.also { it.play() }

        vibrator = getSystemService(Vibrator::class.java)
        vibrator?.vibrate(
            VibrationEffect.createWaveform(longArrayOf(0, 500, 500), 0)
        )
    }

    private fun stopAlarmFeedback() {
        ringtone?.stop()
        vibrator?.cancel()
    }

    override fun onDestroy() {
        stopAlarmFeedback()
        super.onDestroy()
    }
}

@Composable
private fun AlarmScreen(
    label: String,
    isSmart: Boolean,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        Indigo.copy(alpha = 0.35f),
                        Lavender.copy(alpha = 0.18f)
                    )
                )
            )
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm")),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = label,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = if (isSmart) "WakeSync smart wake" else "Wake-by deadline",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp),
            onClick = onDismiss,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Amber)
        ) {
            Text(
                modifier = Modifier.padding(vertical = 10.dp),
                text = "Dismiss",
                color = MaterialTheme.colorScheme.background,
                fontWeight = FontWeight.SemiBold
            )
        }

        OutlinedButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            onClick = onSnooze,
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(
                modifier = Modifier.padding(vertical = 8.dp),
                text = "Snooze 5 minutes"
            )
        }
    }
}
