package com.kiranoommen.wakesync.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import com.kiranoommen.wakesync.model.SleepNight
import com.kiranoommen.wakesync.model.SleepStageType
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
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Text("WakeSync", style = MaterialTheme.typography.headlineLarge)
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = "Find a better wake window from your own sleep-stage history."
            )

            Spacer(Modifier.height(20.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Private by design", style = MaterialTheme.typography.titleMedium)
                    Text(
                        modifier = Modifier.padding(top = 8.dp),
                        text = "Your sleep data stays on this device. WakeSync requests read-only Health Connect access and does not upload, sell, share, or modify your health data."
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            when {
                sdkStatus == HealthConnectClient.SDK_UNAVAILABLE -> {
                    Text("Health Connect is not supported on this device.")
                }

                sdkStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                    Text("Health Connect needs to be installed or updated before WakeSync can read sleep data.")
                }

                !hasPermission -> {
                    Text("Connect Health Connect to let WakeSync read your sleep sessions and stages.")
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onConnect) { Text("Connect sleep data") }
                }

                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recent sleep", style = MaterialTheme.typography.titleLarge)
                        Button(onClick = onRefresh, enabled = !loading) { Text("Refresh") }
                    }

                    if (loading) {
                        Spacer(Modifier.height(16.dp))
                        CircularProgressIndicator()
                    } else if (nights.isEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text("No sleep sessions were returned. If you use Fitbit, confirm Fitbit is writing sleep data to Health Connect.")
                    } else {
                        Spacer(Modifier.height(8.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(nights.take(14)) { night -> SleepNightCard(night) }
                        }
                    }
                }
            }

            errorMessage?.let {
                Spacer(Modifier.height(12.dp))
                Text("Error: " + it)
            }
        }
    }
}

@Composable
private fun SleepNightCard(night: SleepNight) {
    val zone = ZoneId.systemDefault()
    val dateFormat = DateTimeFormatter.ofPattern("EEE, MMM d")
    val timeFormat = DateTimeFormatter.ofPattern("h:mm a")

    val startLocal = night.start.atZone(zone)
    val endLocal = night.end.atZone(zone)
    val durationMinutes = ChronoUnit.MINUTES.between(night.start, night.end)
    val stageCounts = night.stages.groupingBy { it.type }.eachCount()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(endLocal.format(dateFormat), style = MaterialTheme.typography.titleMedium)
            Text(
                startLocal.format(timeFormat) + " – " + endLocal.format(timeFormat) +
                    " · " + (durationMinutes / 60) + "h " + (durationMinutes % 60) + "m"
            )
            Text(
                modifier = Modifier.padding(top = 6.dp),
                text = "REM " + (stageCounts[SleepStageType.REM] ?: 0) + " · " +
                    "Light " + (stageCounts[SleepStageType.LIGHT] ?: 0) + " · " +
                    "Deep " + (stageCounts[SleepStageType.DEEP] ?: 0) + " · " +
                    "Awake " + (stageCounts[SleepStageType.AWAKE] ?: 0)
            )
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = "Source: " + night.sourcePackage,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
