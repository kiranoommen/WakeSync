package com.kiranoommen.wakesync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.lifecycleScope
import com.kiranoommen.wakesync.data.HealthConnectManager
import com.kiranoommen.wakesync.model.SleepNight
import com.kiranoommen.wakesync.ui.WakeSyncScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var healthConnectManager: HealthConnectManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        healthConnectManager = HealthConnectManager(this)

        setContent {
            var hasPermission by remember { mutableStateOf(false) }
            var nights by remember { mutableStateOf<List<SleepNight>>(emptyList()) }
            var loading by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = PermissionController.createRequestPermissionResultContract()
            ) { granted ->
                hasPermission = granted.containsAll(HealthConnectManager.requiredPermissions)

                if (hasPermission) {
                    loading = true
                    lifecycleScope.launch {
                        runCatching { healthConnectManager.readRecentSleep() }
                            .onSuccess { nights = it }
                            .onFailure { errorMessage = it.message ?: "Unable to read sleep data." }
                        loading = false
                    }
                }
            }

            LaunchedEffect(Unit) {
                hasPermission = runCatching {
                    healthConnectManager.hasRequiredPermissions()
                }.getOrDefault(false)

                if (hasPermission) {
                    loading = true
                    runCatching { healthConnectManager.readRecentSleep() }
                        .onSuccess { nights = it }
                        .onFailure { errorMessage = it.message ?: "Unable to read sleep data." }
                    loading = false
                }
            }

            WakeSyncScreen(
                sdkStatus = healthConnectManager.sdkStatus(),
                hasPermission = hasPermission,
                loading = loading,
                nights = nights,
                errorMessage = errorMessage,
                onConnect = {
                    permissionLauncher.launch(HealthConnectManager.requiredPermissions)
                },
                onRefresh = {
                    loading = true
                    errorMessage = null
                    lifecycleScope.launch {
                        runCatching { healthConnectManager.readRecentSleep() }
                            .onSuccess { nights = it }
                            .onFailure { errorMessage = it.message ?: "Unable to read sleep data." }
                        loading = false
                    }
                }
            )
        }
    }
}
