package com.kiranoommen.wakesync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Your sleep data stays on your device.",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        modifier = Modifier.padding(top = 16.dp),
                        text = "WakeSync requests read-only access to your sleep sessions and sleep stages through Health Connect. When Smart Wake is enabled, background read access lets WakeSync check for fresh sleep-stage updates shortly before your wake range."
                    )
                    Text(
                        modifier = Modifier.padding(top = 12.dp),
                        text = "WakeSync processes these reads locally on your phone. It does not upload, sell, share, or modify your health data."
                    )
                }
            }
        }
    }
}
