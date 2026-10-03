package com.kiranoommen.wakesync.ui.alarmringing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kiranoommen.wakesync.alarm.AlarmScheduler
import com.kiranoommen.wakesync.model.AlarmMode
import com.kiranoommen.wakesync.model.AlarmSchedule
import com.kiranoommen.wakesync.ui.WakeSyncBrandMark
import com.kiranoommen.wakesync.ui.theme.Amber
import com.kiranoommen.wakesync.ui.theme.Indigo
import com.kiranoommen.wakesync.ui.theme.Lavender
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun AlarmRingingScreen(
    reason: String,
    kind: String,
    schedule: AlarmSchedule?,
    onDismissCurrent: () -> Unit,
    onStopSequence: () -> Unit
) {
    var now by remember {
        mutableStateOf(LocalDateTime.now())
    }

    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1_000)
        }
    }

    val smart =
        schedule?.mode == AlarmMode.SMART_WAKE
    val backupIndex =
        AlarmScheduler.backupIndex(kind)
    val backupCount =
        schedule?.backupRingCount
            ?.coerceIn(
                0,
                AlarmScheduler.MAX_BACKUP_RINGS
            ) ?: 0

    val earlySmartRing =
        smart &&
            (
                kind == "live" ||
                    kind == AlarmScheduler.KIND_HISTORICAL
                )

    val remainingSequence =
        earlySmartRing ||
            (
                kind == AlarmScheduler.KIND_DEADLINE &&
                    backupCount > 0
                ) ||
            (
                backupIndex != null &&
                    backupIndex < backupCount
                )

    val modeLabel =
        if (smart) {
            "SMART WAKE"
        } else {
            "STANDARD ALARM"
        }

    val label =
        schedule?.label
            ?.takeIf { it.isNotBlank() }

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
            .padding(
                horizontal = 26.dp,
                vertical = 24.dp
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                WakeSyncBrandMark(
                    modifier = Modifier.size(44.dp)
                )
                Text(
                    modifier =
                        Modifier.padding(start = 11.dp),
                    text = "WakeSync",
                    style =
                        MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            Spacer(Modifier.weight(1f))

            Card(
                shape =
                    RoundedCornerShape(999.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            if (smart) {
                                Lavender.copy(alpha = 0.18f)
                            } else {
                                Amber.copy(alpha = 0.18f)
                            }
                    )
            ) {
                Text(
                    modifier = Modifier.padding(
                        horizontal = 18.dp,
                        vertical = 9.dp
                    ),
                    text = modeLabel,
                    style =
                        MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color =
                        if (smart) Lavender else Amber
                )
            }

            Text(
                modifier =
                    Modifier.padding(top = 24.dp),
                text =
                    now.format(
                        DateTimeFormatter.ofPattern("h:mm")
                    ),
                fontSize = 78.sp,
                lineHeight = 82.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Text(
                text =
                    now.format(
                        DateTimeFormatter.ofPattern("a")
                    ),
                style =
                    MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color =
                    Color.White.copy(alpha = 0.66f)
            )

            Text(
                modifier =
                    Modifier.padding(top = 24.dp),
                text = label ?: "Good morning",
                style =
                    MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Text(
                modifier =
                    Modifier.padding(top = 8.dp),
                text = alarmReasonCopy(reason),
                style =
                    MaterialTheme.typography.bodyLarge,
                color =
                    Color.White.copy(alpha = 0.78f)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 26.dp),
                shape =
                    RoundedCornerShape(24.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color.White.copy(alpha = 0.09f)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = 18.dp,
                        vertical = 16.dp
                    ),
                    verticalArrangement =
                        Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text =
                            if (smart) {
                                "Wake plan"
                            } else {
                                "Scheduled alarm"
                            },
                        style =
                            MaterialTheme.typography.labelMedium,
                        color =
                            Color.White.copy(alpha = 0.58f)
                    )

                    Text(
                        text =
                            alarmScheduleCopy(schedule),
                        style =
                            MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    if (remainingSequence) {
                        Text(
                            text =
                                remainingSequenceCopy(
                                    kind = kind,
                                    backupIndex = backupIndex,
                                    backupCount = backupCount
                                ),
                            style =
                                MaterialTheme.typography.bodySmall,
                            color =
                                Amber.copy(alpha = 0.92f)
                        )
                    } else if (smart) {
                        Text(
                            text = reason,
                            style =
                                MaterialTheme.typography.bodySmall,
                            color =
                                Color.White.copy(alpha = 0.62f)
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp),
                shape =
                    RoundedCornerShape(26.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Amber,
                        contentColor =
                            Color(0xFF15192A)
                    ),
                onClick =
                    if (remainingSequence) {
                        onDismissCurrent
                    } else {
                        onStopSequence
                    }
            ) {
                Text(
                    text =
                        if (remainingSequence) {
                            "DISMISS — NEXT ALARM STAYS ON"
                        } else {
                            "I’M AWAKE"
                        },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            if (remainingSequence) {
                TextButton(
                    modifier =
                        Modifier.padding(top = 6.dp),
                    onClick = onStopSequence
                ) {
                    Text(
                        text =
                            "I’M AWAKE — STOP REMAINING ALARMS",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Text(
                    text =
                        "Dismiss keeps your remaining alarms armed.",
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        Color.White.copy(alpha = 0.52f)
                )
            } else {
                Text(
                    modifier =
                        Modifier.padding(top = 12.dp),
                    text =
                        "Stops this alarm. Your recurring schedule stays enabled.",
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        Color.White.copy(alpha = 0.52f)
                )
            }
        }
    }
}

private fun alarmReasonCopy(reason: String): String =
    when {
        reason == "Standard alarm" ->
            "Your standard alarm is ringing."

        reason.startsWith("Live sleep stage:") -> {
            val stage =
                reason.substringAfter(":").trim()
            "WakeSync found $stage sleep inside your wake window."
        }

        reason.startsWith("Historical fallback") ->
            "Your saved sleep pattern reached its fallback wake point."

        reason.startsWith("Hard wake deadline") ->
            "You reached your must-be-awake time."

        reason.startsWith("Backup alarm") ->
            "This is one of your backup alarms."

        reason.startsWith("Snoozed") ->
            "Your snooze is over."

        else ->
            "It’s time to wake up."
    }

private fun remainingSequenceCopy(
    kind: String,
    backupIndex: Int?,
    backupCount: Int
): String =
    when {
        kind == "live" ||
            kind == AlarmScheduler.KIND_HISTORICAL ->
            "Your hard deadline is still armed."

        kind == AlarmScheduler.KIND_DEADLINE &&
            backupCount > 0 ->
            "$backupCount backup alarm" +
                if (backupCount == 1) {
                    " remains."
                } else {
                    "s remain."
                }

        backupIndex != null ->
            "${backupCount - backupIndex} backup alarm" +
                if (backupCount - backupIndex == 1) {
                    " remains."
                } else {
                    "s remain."
                }

        else -> ""
    }

private fun alarmScheduleCopy(
    schedule: AlarmSchedule?
): String {
    if (schedule == null) {
        return "WakeSync alarm"
    }

    val formatter =
        DateTimeFormatter.ofPattern("h:mm a")
    val deadline =
        LocalTime.of(
            schedule.hour,
            schedule.minute
        )

    return if (
        schedule.mode == AlarmMode.STANDARD
    ) {
        deadline.format(formatter)
    } else {
        val start =
            deadline.minusMinutes(
                schedule.smartWindowMinutes.toLong()
            )
        "${start.format(formatter)} – " +
            deadline.format(formatter)
    }
}

