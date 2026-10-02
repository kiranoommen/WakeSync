package com.kiranoommen.wakesync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiranoommen.wakesync.alarm.WakeAlarmController
import com.kiranoommen.wakesync.data.WakePreferencesStore
import com.kiranoommen.wakesync.model.AlarmMode
import com.kiranoommen.wakesync.model.WakePreferences
import com.kiranoommen.wakesync.ui.WakeSyncBrandMark
import com.kiranoommen.wakesync.ui.theme.Amber
import com.kiranoommen.wakesync.ui.theme.Indigo
import com.kiranoommen.wakesync.ui.theme.Lavender
import com.kiranoommen.wakesync.ui.theme.WakeSyncTheme
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setShowWhenLocked(true)
        setTurnScreenOn(true)

        val reason = intent.getStringExtra(WakeAlarmController.EXTRA_REASON)
            ?: "WakeSync alarm"
        val preferences = WakePreferencesStore(this).load()

        setContent {
            WakeSyncTheme(darkTheme = true) {
                AlarmRingingScreen(
                    reason = reason,
                    preferences = preferences,
                    onDismiss = {
                        WakeAlarmController.dismiss(this@AlarmActivity)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun AlarmRingingScreen(
    reason: String,
    preferences: WakePreferences,
    onDismiss: () -> Unit
) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1_000)
        }
    }

    val standard = reason == "Standard alarm"
    val modeLabel = if (standard) "STANDARD ALARM" else "SMART WAKE"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF080D1F),
                        Color(0xFF11172C),
                        Indigo.copy(alpha = 0.72f),
                        Color(0xFF2D2431)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 26.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WakeSyncBrandMark(modifier = Modifier.size(44.dp))
                Text(
                    modifier = Modifier.padding(start = 11.dp),
                    text = "WakeSync",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            Spacer(Modifier.weight(1f))

            Card(
                shape = RoundedCornerShape(999.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (standard) {
                        Amber.copy(alpha = 0.18f)
                    } else {
                        Lavender.copy(alpha = 0.18f)
                    }
                )
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                    text = modeLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (standard) Amber else Lavender
                )
            }

            Text(
                modifier = Modifier.padding(top = 24.dp),
                text = now.format(DateTimeFormatter.ofPattern("h:mm")),
                fontSize = 78.sp,
                lineHeight = 82.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Text(
                text = now.format(DateTimeFormatter.ofPattern("a")),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.66f)
            )

            Text(
                modifier = Modifier.padding(top = 24.dp),
                text = "Good morning",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = alarmReasonCopy(reason),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.78f)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 26.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.09f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = if (standard) "Scheduled alarm" else "Wake plan",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.58f)
                    )
                    Text(
                        text = alarmScheduleCopy(
                            standard = standard,
                            preferences = preferences
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (!standard) {
                        Text(
                            text = reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.62f)
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Amber,
                    contentColor = Color(0xFF15192A)
                ),
                onClick = onDismiss
            ) {
                Text(
                    text = "I’M AWAKE",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Text(
                modifier = Modifier.padding(top = 12.dp),
                text = "Stops the alarm and keeps your next scheduled wake.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.52f)
            )
        }
    }
}

private fun alarmReasonCopy(reason: String): String =
    when {
        reason == "Standard alarm" ->
            "Your standard alarm is ringing."
        reason.startsWith("Live sleep stage:") -> {
            val stage = reason.substringAfter(":").trim()
            "WakeSync found $stage sleep inside your wake window."
        }
        reason.startsWith("Historical fallback") ->
            "Your saved sleep pattern reached its fallback wake point."
        reason.startsWith("Hard wake deadline") ->
            "You reached the latest time you asked WakeSync to let you sleep."
        else ->
            "It’s time to wake up."
    }

private fun alarmScheduleCopy(
    standard: Boolean,
    preferences: WakePreferences
): String {
    val formatter = DateTimeFormatter.ofPattern("h:mm a")
    return if (standard || preferences.mode == AlarmMode.STANDARD) {
        preferences.standardTime.format(formatter)
    } else {
        "${preferences.earliest.format(formatter)} – ${preferences.latest.format(formatter)}"
    }
}
