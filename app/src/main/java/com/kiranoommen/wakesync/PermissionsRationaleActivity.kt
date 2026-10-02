package com.kiranoommen.wakesync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kiranoommen.wakesync.ui.AmbientGradientBackground
import com.kiranoommen.wakesync.ui.WakeSyncBrandMark
import com.kiranoommen.wakesync.ui.theme.WakeSyncTheme

class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WakeSyncTheme(
                darkTheme = true
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxSize()
                ) {
                    AmbientGradientBackground(
                        pagePosition = 3f,
                        darkTheme = true,
                        modifier =
                            Modifier.fillMaxSize()
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement =
                            Arrangement.Center,
                        horizontalAlignment =
                            Alignment.Start
                    ) {
                        WakeSyncBrandMark(
                            modifier =
                                Modifier.size(
                                    width = 58.dp,
                                    height = 46.dp
                                )
                        )

                        Text(
                            modifier =
                                Modifier.padding(
                                    top = 18.dp
                                ),
                            text =
                                "Your sleep data stays on your device.",
                            style =
                                MaterialTheme.typography.headlineSmall,
                            fontWeight =
                                FontWeight.ExtraBold
                        )
                        Text(
                            modifier =
                                Modifier.padding(
                                    top = 16.dp
                                ),
                            text =
                                "WakeSync requests read-only access to sleep sessions and stages through Health Connect so it can calculate personalized wake windows locally on your phone."
                        )
                        Text(
                            modifier =
                                Modifier.padding(
                                    top = 12.dp
                                ),
                            text =
                                "WakeSync does not upload, sell, share, or modify your health data."
                        )
                    }
                }
            }
        }
    }
}
