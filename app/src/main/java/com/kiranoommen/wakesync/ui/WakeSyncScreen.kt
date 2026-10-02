package com.kiranoommen.wakesync.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import com.kiranoommen.wakesync.data.AppSettingsStore
import com.kiranoommen.wakesync.data.SleepExporter
import com.kiranoommen.wakesync.model.AlarmMode
import com.kiranoommen.wakesync.model.SleepNight
import com.kiranoommen.wakesync.model.SleepStageType
import com.kiranoommen.wakesync.model.WakePreferences
import com.kiranoommen.wakesync.ui.theme.Amber
import com.kiranoommen.wakesync.ui.theme.Indigo
import com.kiranoommen.wakesync.ui.theme.Lavender
import com.kiranoommen.wakesync.ui.theme.WakeSyncTheme
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private enum class MainTab(val label: String) {
    HOME("Home"),
    SLEEP("Sleep"),
    SETTINGS("Settings")
}

@Composable
fun WakeSyncScreen(
    sdkStatus: Int,
    hasPermission: Boolean,
    backgroundReadAvailable: Boolean,
    hasBackgroundReadPermission: Boolean,
    exactAlarmAccess: Boolean,
    notificationsAllowed: Boolean,
    fullScreenAlarmAccess: Boolean,
    hasAnalyticsPermission: Boolean,
    hasHistoryPermission: Boolean,
    historyReadAvailable: Boolean,
    wakePreferences: WakePreferences,
    loading: Boolean,
    nights: List<SleepNight>,
    themeMode: String,
    sleepGoalMinutes: Int,
    goalsEnabled: Boolean,
    displayName: String,
    retainGeneratedExports: Boolean,
    errorMessage: String?,
    onConnect: () -> Unit,
    onRequestAnalyticsAccess: () -> Unit,
    onRequestHistoryAccess: () -> Unit,
    onRequestExactAlarmAccess: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestFullScreenAlarmAccess: () -> Unit,
    onWakePreferencesChanged: (WakePreferences) -> Unit,
    onAlarmEnabledChanged: (Boolean) -> Unit,
    onThemeModeChange: (String) -> Unit,
    onSleepGoalChange: (Int) -> Unit,
    onGoalsEnabledChange: (Boolean) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onRetainGeneratedExportsChange: (Boolean) -> Unit,
    onClearGeneratedExports: () -> Unit,
    onImAwake: () -> Unit,
    onRefresh: () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        AppSettingsStore.THEME_DARK -> true
        AppSettingsStore.THEME_LIGHT -> false
        else -> systemDark
    }

    WakeSyncTheme(darkTheme = darkTheme) {
        var tabIndex by remember { mutableIntStateOf(0) }
        val tab = MainTab.entries[tabIndex.coerceIn(0, MainTab.entries.lastIndex)]
        val context = LocalContext.current

        Box(modifier = Modifier.fillMaxSize()) {
            AmbientGradientBackground(
                pagePosition = tabIndex.toFloat(),
                darkTheme = darkTheme,
                modifier = Modifier.fillMaxSize()
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                BrandHeader(
                    title = when (tab) {
                        MainTab.HOME -> greeting(displayName)
                        MainTab.SLEEP -> "Sleep"
                        MainTab.SETTINGS -> "Settings"
                    },
                    subtitle = when (tab) {
                        MainTab.HOME -> "Better mornings, in sync with you."
                        MainTab.SLEEP -> "Your sleep trends, kept on this device."
                        MainTab.SETTINGS -> "Personalize WakeSync and data access."
                    }
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                ) {
                    when (tab) {
                        MainTab.HOME -> HomeTab(
                            sdkStatus = sdkStatus,
                            hasPermission = hasPermission,
                            backgroundReadAvailable = backgroundReadAvailable,
                            hasBackgroundReadPermission = hasBackgroundReadPermission,
                            exactAlarmAccess = exactAlarmAccess,
                            notificationsAllowed = notificationsAllowed,
                            fullScreenAlarmAccess = fullScreenAlarmAccess,
                            wakePreferences = wakePreferences,
                            loading = loading,
                            nights = nights,
                            errorMessage = errorMessage,
                            onConnect = onConnect,
                            onRequestExactAlarmAccess = onRequestExactAlarmAccess,
                            onRequestNotifications = onRequestNotifications,
                            onRequestFullScreenAlarmAccess = onRequestFullScreenAlarmAccess,
                            onWakePreferencesChanged = onWakePreferencesChanged,
                            onAlarmEnabledChanged = onAlarmEnabledChanged,
                            onImAwake = onImAwake,
                            onRefresh = onRefresh
                        )

                        MainTab.SLEEP -> LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                SleepAnalyticsScreen(
                                    nights = nights,
                                    loading = loading,
                                    targetSleepMinutes = sleepGoalMinutes,
                                    goalsEnabled = goalsEnabled,
                                    onRefresh = onRefresh,
                                    onShareCsv = {
                                        SleepExporter.shareCsv(
                                            context,
                                            it,
                                            retainGeneratedExports
                                        )
                                    },
                                    onSharePdf = {
                                        SleepExporter.sharePdf(
                                            context,
                                            it,
                                            retainGeneratedExports
                                        )
                                    },
                                    onShareStory = {
                                        SleepExporter.shareStoryCard(
                                            context,
                                            it,
                                            retainGeneratedExports
                                        )
                                    }
                                )
                            }
                            item { Spacer(Modifier.height(12.dp)) }
                        }

                        MainTab.SETTINGS -> SettingsTab(
                            hasPermission = hasPermission,
                            hasAnalyticsPermission = hasAnalyticsPermission,
                            hasHistoryPermission = hasHistoryPermission,
                            historyReadAvailable = historyReadAvailable,
                            themeMode = themeMode,
                            sleepGoalMinutes = sleepGoalMinutes,
                            goalsEnabled = goalsEnabled,
                            displayName = displayName,
                            retainGeneratedExports = retainGeneratedExports,
                            onConnect = onConnect,
                            onRequestAnalyticsAccess = onRequestAnalyticsAccess,
                            onRequestHistoryAccess = onRequestHistoryAccess,
                            onThemeModeChange = onThemeModeChange,
                            onSleepGoalChange = onSleepGoalChange,
                            onGoalsEnabledChange = onGoalsEnabledChange,
                            onDisplayNameChange = onDisplayNameChange,
                            onRetainGeneratedExportsChange = onRetainGeneratedExportsChange,
                            onClearGeneratedExports = onClearGeneratedExports
                        )
                    }
                }

                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                ) {
                    MainTab.entries.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = tabIndex == index,
                            onClick = { tabIndex = index },
                            icon = {
                                Icon(
                                    imageVector = when (item) {
                                        MainTab.HOME -> Icons.Default.Home
                                        MainTab.SLEEP -> Icons.Default.Bedtime
                                        MainTab.SETTINGS -> Icons.Default.Settings
                                    },
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandHeader(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WakeSyncBrandMark(modifier = Modifier.size(46.dp))
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HomeTab(
    sdkStatus: Int,
    hasPermission: Boolean,
    backgroundReadAvailable: Boolean,
    hasBackgroundReadPermission: Boolean,
    exactAlarmAccess: Boolean,
    notificationsAllowed: Boolean,
    fullScreenAlarmAccess: Boolean,
    wakePreferences: WakePreferences,
    loading: Boolean,
    nights: List<SleepNight>,
    errorMessage: String?,
    onConnect: () -> Unit,
    onRequestExactAlarmAccess: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestFullScreenAlarmAccess: () -> Unit,
    onWakePreferencesChanged: (WakePreferences) -> Unit,
    onAlarmEnabledChanged: (Boolean) -> Unit,
    onImAwake: () -> Unit,
    onRefresh: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            WakeWindowHero(
                hasPermission = hasPermission,
                preferences = wakePreferences,
                onImAwake = onImAwake
            )
        }
        item {
            WakeSettingsCard(
                preferences = wakePreferences,
                sleepPermissionGranted = hasPermission,
                backgroundReadAvailable = backgroundReadAvailable,
                backgroundReadGranted = hasBackgroundReadPermission,
                exactAlarmAccess = exactAlarmAccess,
                notificationsAllowed = notificationsAllowed,
                fullScreenAlarmAccess = fullScreenAlarmAccess,
                onPreferencesChanged = onWakePreferencesChanged,
                onRequestHealthPermissions = onConnect,
                onRequestExactAlarmAccess = onRequestExactAlarmAccess,
                onRequestNotifications = onRequestNotifications,
                onRequestFullScreenAlarmAccess = onRequestFullScreenAlarmAccess,
                onAlarmEnabledChanged = onAlarmEnabledChanged
            )
        }
        item { PrivacyBanner() }

        when {
            preferences.mode == AlarmMode.STANDARD && !hasPermission -> item {
                ConnectCard(onConnect = onConnect, optional = true)
            }
            sdkStatus == HealthConnectClient.SDK_UNAVAILABLE -> item {
                StatusCard(
                    "Health Connect unavailable",
                    "This device does not support the Health Connect connection WakeSync needs."
                )
            }
            sdkStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> item {
                StatusCard(
                    "Health Connect needs attention",
                    "Install or update Health Connect, then return to WakeSync."
                )
            }
            !hasPermission -> item { ConnectCard(onConnect = onConnect) }
            else -> {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Recent sleep",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Read from Health Connect",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(onClick = onRefresh, enabled = !loading) {
                            Text("Refresh")
                        }
                    }
                }
                if (loading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 26.dp),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator(color = Amber) }
                    }
                } else if (nights.isEmpty()) {
                    item {
                        StatusCard(
                            "Connected, waiting for sleep data",
                            "No sleep sessions were returned yet. Confirm your wearable is writing sleep records to Health Connect."
                        )
                    }
                } else {
                    items(nights.take(3)) { SleepSummaryCard(it) }
                }
            }
        }

        errorMessage?.let { error ->
            item {
                Text(
                    text = "Error: $error",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun WakeWindowHero(
    hasPermission: Boolean,
    preferences: WakePreferences,
    onImAwake: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 18.dp,
                shape = RoundedCornerShape(30.dp),
                ambientColor = Amber.copy(alpha = 0.20f),
                spotColor = Amber.copy(alpha = 0.28f)
            ),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Indigo.copy(alpha = 0.94f),
                            Lavender.copy(alpha = 0.76f),
                            Amber.copy(alpha = 0.86f)
                        )
                    )
                )
                .padding(22.dp)
        ) {
            Column {
                Text(
                    text = if (preferences.mode == AlarmMode.STANDARD) {
                        "STANDARD ALARM"
                    } else {
                        "YOUR WAKE WINDOW"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.82f)
                )
                Text(
                    modifier = Modifier.padding(top = 10.dp),
                    text = when {
                        preferences.mode == AlarmMode.STANDARD ->
                            formatAlarmTime(preferences.standardTime)
                        !hasPermission -> "Connect sleep data"
                        preferences.enabled -> formatWakeRange(preferences)
                        else -> "Set your wake range"
                    },
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = when {
                        preferences.mode == AlarmMode.STANDARD && preferences.enabled ->
                            "Exact alarm · no sleep monitoring"
                        preferences.mode == AlarmMode.STANDARD ->
                            "Choose one exact time and turn the alarm on."
                        !hasPermission ->
                            "Read-only Health Connect access is required for Smart Wake."
                        preferences.enabled ->
                            "Live stage first · history fallback in final 10m · hard stop always"
                        else ->
                            "Choose the earliest and latest time you are willing to wake."
                    },
                    color = Color.White.copy(alpha = 0.86f)
                )
                Button(
                    modifier = Modifier.padding(top = 18.dp),
                    onClick = onImAwake,
                    enabled = preferences.enabled,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF15192A),
                        disabledContainerColor = Color.White.copy(alpha = 0.18f),
                        disabledContentColor = Color.White.copy(alpha = 0.70f)
                    )
                ) { Text("I’m awake", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun SettingsTab(
    hasPermission: Boolean,
    hasAnalyticsPermission: Boolean,
    hasHistoryPermission: Boolean,
    historyReadAvailable: Boolean,
    themeMode: String,
    sleepGoalMinutes: Int,
    goalsEnabled: Boolean,
    displayName: String,
    retainGeneratedExports: Boolean,
    onConnect: () -> Unit,
    onRequestAnalyticsAccess: () -> Unit,
    onRequestHistoryAccess: () -> Unit,
    onThemeModeChange: (String) -> Unit,
    onSleepGoalChange: (Int) -> Unit,
    onGoalsEnabledChange: (Boolean) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onRetainGeneratedExportsChange: (Boolean) -> Unit,
    onClearGeneratedExports: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SettingsCard("Appearance") {
                Text("Theme", fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        AppSettingsStore.THEME_DARK to "Dark",
                        AppSettingsStore.THEME_LIGHT to "Light",
                        AppSettingsStore.THEME_SYSTEM to "System"
                    ).forEach { (value, label) ->
                        FilterChip(
                            selected = themeMode == value,
                            onClick = { onThemeModeChange(value) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }

        item {
            SettingsCard("Sleep target") {
                Text(
                    text = if (goalsEnabled) {
                        "Target: ${formatMinutes(sleepGoalMinutes)}"
                    } else {
                        "Sleep goals are hidden"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onSleepGoalChange((sleepGoalMinutes - 15).coerceAtLeast(240)) },
                        enabled = goalsEnabled
                    ) { Text("−15m") }
                    OutlinedButton(
                        onClick = { onSleepGoalChange((sleepGoalMinutes + 15).coerceAtMost(720)) },
                        enabled = goalsEnabled
                    ) { Text("+15m") }
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = goalsEnabled,
                        onCheckedChange = onGoalsEnabledChange
                    )
                }
            }
        }

        item {
            SettingsCard("Optional analytics") {
                PermissionSettingRow(
                    "Core sleep access",
                    "Required for Smart Wake and sleep-stage history.",
                    hasPermission,
                    true,
                    onConnect
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                PermissionSettingRow(
                    "HRV + resting heart rate",
                    "Optional read-only wellness context for the sleep dashboard.",
                    hasAnalyticsPermission,
                    true,
                    onRequestAnalyticsAccess
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                PermissionSettingRow(
                    "Extended Health Connect history",
                    "Optional. Lets WakeSync analyze more than the default recent-history window.",
                    hasHistoryPermission,
                    historyReadAvailable,
                    onRequestHistoryAccess
                )
            }
        }

        item {
            SettingsCard("Local exports") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Keep generated exports", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Off keeps CSV/PDF/story files temporary. On retains generated copies in WakeSync local storage.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = retainGeneratedExports,
                        onCheckedChange = onRetainGeneratedExportsChange
                    )
                }
                OutlinedButton(
                    modifier = Modifier.padding(top = 10.dp),
                    onClick = onClearGeneratedExports
                ) { Text("Clear generated exports") }
            }
        }

        item {
            SettingsCard("Local profile") {
                Text(
                    text = if (displayName.isBlank()) {
                        "Greeting name: not set"
                    } else {
                        "Greeting name: $displayName"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("", "Morning", "Sleeper").forEach { name ->
                        FilterChip(
                            selected = displayName == name,
                            onClick = { onDisplayNameChange(name) },
                            label = { Text(if (name.isBlank()) "None" else name) }
                        )
                    }
                }
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = "A custom name can be set during onboarding. It stays in app-private local preferences.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item { Spacer(Modifier.height(10.dp)) }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun PermissionSettingRow(
    title: String,
    description: String,
    ready: Boolean,
    available: Boolean,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(
                text = when {
                    !available -> "$description Not supported by this Health Connect version."
                    ready -> "$description Ready."
                    else -> description
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (available && !ready) {
            Button(onClick = onAction) { Text("Allow") }
        } else {
            Text(
                text = if (ready) "Ready" else "Unavailable",
                style = MaterialTheme.typography.labelLarge,
                color = if (ready) Lavender else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PrivacyBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.80f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 17.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("◈", color = Lavender, style = MaterialTheme.typography.headlineMedium)
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text("Processed on your device", fontWeight = FontWeight.Bold)
                Text(
                    "Read-only health access. No WakeSync health-data backend.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ConnectCard(
    onConnect: () -> Unit,
    optional: Boolean = false
) {
    SettingsCard(
        if (optional) "Optional sleep insights" else "Connect your sleep data"
    ) {
        Text(
            text = if (optional) {
                "Standard Alarm works without Health Connect. Connect later if you also want sleep analytics or Smart Wake."
            } else {
                "WakeSync reads the sleep records already available through Android Health Connect."
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            onClick = onConnect,
            colors = ButtonDefaults.buttonColors(
                containerColor = Amber,
                contentColor = Color(0xFF15192A)
            )
        ) {
            Text(
                if (optional) "Connect Health Connect" else "Connect sleep data",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StatusCard(title: String, body: String) {
    SettingsCard(title) {
        Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SleepSummaryCard(night: SleepNight) {
    val zone = ZoneId.systemDefault()
    val dateFormat = DateTimeFormatter.ofPattern("EEE, MMM d")
    val timeFormat = DateTimeFormatter.ofPattern("h:mm a")
    val durationMinutes = ChronoUnit.MINUTES.between(night.start, night.end)
    val deep = stageMinutes(night, SleepStageType.DEEP)
    val light = stageMinutes(night, SleepStageType.LIGHT)
    val rem = stageMinutes(night, SleepStageType.REM)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
        )
    ) {
        Column(modifier = Modifier.padding(17.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    night.end.atZone(zone).format(dateFormat),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    formatMinutes(durationMinutes.toInt()),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = "${night.start.atZone(zone).format(timeFormat)} – ${night.end.atZone(zone).format(timeFormat)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                StageStat("Deep", deep, Indigo)
                StageStat("Light", light, Lavender)
                StageStat("REM", rem, Amber)
            }
        }
    }
}

@Composable
private fun StageStat(label: String, minutes: Long, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall, color = color)
        Text(formatMinutes(minutes.toInt()), fontWeight = FontWeight.SemiBold)
    }
}

private fun stageMinutes(night: SleepNight, type: SleepStageType): Long =
    night.stages
        .filter { it.type == type }
        .sumOf { ChronoUnit.MINUTES.between(it.start, it.end).coerceAtLeast(0) }

private val alarmTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

private fun formatAlarmTime(time: java.time.LocalTime): String =
    time.format(alarmTimeFormatter)

private fun formatWakeRange(preferences: WakePreferences): String =
    "${formatAlarmTime(preferences.earliest)} – ${formatAlarmTime(preferences.latest)}"

private fun formatMinutes(minutes: Int): String {
    val safe = minutes.coerceAtLeast(0)
    val hours = safe / 60
    val mins = safe % 60
    return when {
        hours == 0 -> "${mins}m"
        mins == 0 -> "${hours}h"
        else -> "${hours}h ${mins}m"
    }
}

private fun greeting(displayName: String): String =
    if (displayName.isBlank()) "WakeSync" else "Good morning, $displayName"
