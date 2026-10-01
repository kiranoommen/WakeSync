package com.kiranoommen.wakesync.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import com.kiranoommen.wakesync.model.SleepNight
import com.kiranoommen.wakesync.model.SleepStageType
import com.kiranoommen.wakesync.ui.theme.Amber
import com.kiranoommen.wakesync.ui.theme.Indigo
import com.kiranoommen.wakesync.ui.theme.Lavender
import com.kiranoommen.wakesync.ui.theme.WakeSyncTheme
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun WakeSyncScreen(
    sdkStatus: Int,
    hasPermission: Boolean,
    loading: Boolean,
    nights: List<SleepNight>,
    errorMessage: String?,
    onConnect: () -> Unit,
    onRefresh: () -> Unit
) {
    WakeSyncTheme {
        val colors = MaterialTheme.colorScheme

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            colors.background,
                            colors.surfaceVariant.copy(alpha = 0.82f),
                            colors.background
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Text(
                    text = "WakeSync",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    modifier = Modifier.padding(top = 3.dp),
                    text = "Better mornings, in sync with you.",
                    color = colors.onSurfaceVariant
                )

                Spacer(Modifier.height(18.dp))

                WakeWindowHero(hasPermission = hasPermission)

                Spacer(Modifier.height(14.dp))

                PrivacyBanner()

                Spacer(Modifier.height(18.dp))

                when {
                    sdkStatus == HealthConnectClient.SDK_UNAVAILABLE -> {
                        StatusCard(
                            title = "Health Connect unavailable",
                            body = "This device does not support the Health Connect connection WakeSync needs."
                        )
                    }

                    sdkStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                        StatusCard(
                            title = "Health Connect needs attention",
                            body = "Install or update Health Connect, then return to WakeSync."
                        )
                    }

                    !hasPermission -> {
                        ConnectCard(onConnect = onConnect)
                    }

                    else -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Last night's sleep",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Read from Health Connect",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = onRefresh,
                                enabled = !loading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.surfaceVariant,
                                    contentColor = colors.onSurface
                                )
                            ) {
                                Text("Refresh")
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        if (loading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Amber)
                            }
                        } else if (nights.isEmpty()) {
                            StatusCard(
                                title = "Connected, waiting for sleep data",
                                body = "No sleep sessions were returned yet. For Fitbit testing, confirm Fitbit is writing sleep records to Health Connect."
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(nights.take(14)) { night ->
                                    SleepNightCard(night)
                                }
                            }
                        }
                    }
                }

                errorMessage?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Error: " + it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun WakeWindowHero(hasPermission: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 18.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Amber.copy(alpha = 0.20f),
                spotColor = Amber.copy(alpha = 0.28f)
            ),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Indigo.copy(alpha = 0.88f),
                            Lavender.copy(alpha = 0.72f),
                            Amber.copy(alpha = 0.78f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "Your wake window",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )

                Text(
                    modifier = Modifier.padding(top = 12.dp),
                    text = if (hasPermission) "Learning your pattern" else "Connect sleep data",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                Text(
                    modifier = Modifier.padding(top = 6.dp),
                    text = if (hasPermission) {
                        "We’ll calculate a different wake window each night from your own sleep history."
                    } else {
                        "Read-only Health Connect access is the first step."
                    },
                    color = Color.White.copy(alpha = 0.82f)
                )

                Spacer(Modifier.height(18.dp))

                Button(
                    onClick = { },
                    enabled = false,
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = Color.White.copy(alpha = 0.16f),
                        disabledContentColor = Color.White.copy(alpha = 0.74f)
                    )
                ) {
                    Text("I’m awake")
                }
            }
        }
    }
}

@Composable
private fun PrivacyBanner() {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface.copy(alpha = 0.76f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "◈",
                color = Lavender,
                style = MaterialTheme.typography.headlineSmall
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = "Processed on your device",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    modifier = Modifier.padding(top = 2.dp),
                    text = "Read-only access. Your sleep data stays on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ConnectCard(onConnect: () -> Unit) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface.copy(alpha = 0.86f)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Connect your sleep data",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                modifier = Modifier.padding(top = 6.dp),
                text = "WakeSync uses Android Health Connect so one permission can provide the sleep data already on your phone.",
                color = colors.onSurfaceVariant
            )
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                onClick = onConnect,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Amber,
                    contentColor = Color(0xFF15192A)
                )
            ) {
                Text(
                    text = "Connect sleep data",
                    modifier = Modifier.padding(vertical = 4.dp),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun StatusCard(title: String, body: String) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface.copy(alpha = 0.82f)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                modifier = Modifier.padding(top = 6.dp),
                text = body,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SleepNightCard(night: SleepNight) {
    val colors = MaterialTheme.colorScheme
    val zone = ZoneId.systemDefault()
    val dateFormat = DateTimeFormatter.ofPattern("EEE, MMM d")
    val timeFormat = DateTimeFormatter.ofPattern("h:mm a")

    val startLocal = night.start.atZone(zone)
    val endLocal = night.end.atZone(zone)
    val durationMinutes = ChronoUnit.MINUTES.between(night.start, night.end)

    val deep = stageMinutes(night, SleepStageType.DEEP)
    val light = stageMinutes(night, SleepStageType.LIGHT)
    val rem = stageMinutes(night, SleepStageType.REM)
    val awake = stageMinutes(night, SleepStageType.AWAKE)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface.copy(alpha = 0.84f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = endLocal.format(dateFormat),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = startLocal.format(timeFormat) + " – " + endLocal.format(timeFormat),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }

                Text(
                    text = (durationMinutes / 60).toString() + "h " + (durationMinutes % 60).toString() + "m",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StagePill(
                    modifier = Modifier.weight(1f),
                    label = "Deep",
                    minutes = deep,
                    accent = Indigo
                )
                StagePill(
                    modifier = Modifier.weight(1f),
                    label = "Light",
                    minutes = light,
                    accent = Lavender
                )
                StagePill(
                    modifier = Modifier.weight(1f),
                    label = "REM",
                    minutes = rem,
                    accent = Amber
                )
                StagePill(
                    modifier = Modifier.weight(1f),
                    label = "Awake",
                    minutes = awake,
                    accent = colors.onSurfaceVariant
                )
            }

            Text(
                modifier = Modifier.padding(top = 12.dp),
                text = "Source: " + night.sourcePackage,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StagePill(
    modifier: Modifier = Modifier,
    label: String,
    minutes: Long,
    accent: Color
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = accent.copy(alpha = 0.12f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = formatMinutes(minutes),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurface
            )
        }
    }
}

private fun stageMinutes(night: SleepNight, type: SleepStageType): Long =
    night.stages
        .asSequence()
        .filter { it.type == type }
        .sumOf { ChronoUnit.MINUTES.between(it.start, it.end) }

private fun formatMinutes(minutes: Long): String {
    if (minutes <= 0) return "—"
    val hours = minutes / 60
    val remainder = minutes % 60
    return if (hours > 0) hours.toString() + "h " + remainder + "m" else remainder.toString() + "m"
}
