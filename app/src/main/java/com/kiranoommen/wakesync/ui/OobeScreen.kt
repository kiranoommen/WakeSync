package com.kiranoommen.wakesync.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import com.kiranoommen.wakesync.data.AppSettingsStore
import com.kiranoommen.wakesync.ui.theme.Cyan
import com.kiranoommen.wakesync.ui.theme.IndigoGlow
import com.kiranoommen.wakesync.ui.theme.Lavender
import com.kiranoommen.wakesync.ui.theme.Sunrise
import com.kiranoommen.wakesync.ui.theme.WakeSyncTheme

@Composable
fun OobeScreen(
    sdkStatus: Int,
    themeMode: String,
    displayName: String,
    hasPermission: Boolean,
    exactAlarmAccess: Boolean,
    onThemeModeChange: (String) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onConnect: () -> Unit,
    onRequestExactAlarmAccess: () -> Unit,
    onFinish: () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        AppSettingsStore.THEME_DARK -> true
        AppSettingsStore.THEME_LIGHT -> false
        else -> systemDark
    }

    WakeSyncTheme(darkTheme = darkTheme) {
        var step by remember { mutableIntStateOf(0) }
        var nameDraft by remember(displayName) {
            mutableStateOf(displayName)
        }
        val colors = MaterialTheme.colorScheme

        Box(
            modifier =
                Modifier.fillMaxSize()
        ) {
            AmbientGradientBackground(
                pagePosition =
                    step.toFloat(),
                darkTheme =
                    darkTheme,
                modifier =
                    Modifier.fillMaxSize()
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 22.dp, vertical = 18.dp)
            ) {
                OobeBrand()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    repeat(5) { index ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(5.dp)
                                .background(
                                    color = if (index <= step) {
                                        Lavender
                                    } else {
                                        colors.onSurface.copy(alpha = 0.10f)
                                    },
                                    shape = RoundedCornerShape(999.dp)
                                )
                        )
                    }
                }

                Crossfade(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    targetState = step,
                    animationSpec = spring(
                        stiffness = 300f,
                        dampingRatio = 0.78f
                    ),
                    label = "oobeStep"
                ) { current ->
                    when (current) {
                        0 -> OobeWelcome(
                            modifier = Modifier.fillMaxSize()
                        )

                        1 -> OobeName(
                            modifier = Modifier.fillMaxSize(),
                            name = nameDraft,
                            onNameChange = {
                                nameDraft = it.take(24)
                            }
                        )

                        2 -> OobeTheme(
                            modifier = Modifier.fillMaxSize(),
                            selected = themeMode,
                            onSelected = onThemeModeChange
                        )

                        3 -> OobeHealthConnect(
                            modifier = Modifier.fillMaxSize(),
                            sdkStatus = sdkStatus,
                            connected = hasPermission,
                            onConnect = onConnect
                        )

                        else -> OobeExactAlarm(
                            modifier = Modifier.fillMaxSize(),
                            granted = exactAlarmAccess,
                            onGrant = onRequestExactAlarmAccess
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step > 0) {
                        TextButton(
                            onClick = {
                                step--
                            }
                        ) {
                            Text("Back")
                        }
                    } else {
                        Spacer(Modifier.weight(0.01f))
                    }

                    Spacer(Modifier.weight(1f))

                    Button(
                        onClick = {
                            when (step) {
                                1 -> {
                                    onDisplayNameChange(
                                        nameDraft.trim()
                                    )
                                    step++
                                }

                                4 -> {
                                    onDisplayNameChange(
                                        nameDraft.trim()
                                    )
                                    onFinish()
                                }

                                else -> step++
                            }
                        },
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.onSurface,
                            contentColor = colors.surface
                        )
                    ) {
                        Text(
                            text = if (step == 4) {
                                "Finish setup"
                            } else {
                                "Continue"
                            },
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OobeBrand() {
    val colors = MaterialTheme.colorScheme

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(
            modifier = Modifier
                .size(width = 42.dp, height = 30.dp)
        ) {
            fun wave(y: Float): Path = Path().apply {
                moveTo(0f, y)
                cubicTo(
                    size.width * 0.22f,
                    y - size.height * 0.20f,
                    size.width * 0.40f,
                    y + size.height * 0.20f,
                    size.width * 0.60f,
                    y
                )
                cubicTo(
                    size.width * 0.76f,
                    y - size.height * 0.18f,
                    size.width * 0.88f,
                    y + size.height * 0.12f,
                    size.width,
                    y - size.height * 0.06f
                )
            }

            drawPath(
                path = wave(size.height * 0.34f),
                color = Lavender,
                style = Stroke(
                    width = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
            drawPath(
                path = wave(size.height * 0.66f),
                color = Sunrise,
                style = Stroke(
                    width = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }

        Text(
            modifier = Modifier.padding(start = 10.dp),
            text = "WakeSync",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = colors.onBackground
        )
    }
}

@Composable
private fun OobeWelcome(
    modifier: Modifier
) {
    OobePage(
        modifier = modifier,
        eyebrow = "WELCOME",
        title = "Wake better, without guessing.",
        body = "WakeSync turns your Health Connect sleep history into a private, personalized morning experience — with protected alarm deadlines and clear sleep trends.",
        footer = "Your health data stays on your device."
    )
}

@Composable
private fun OobeName(
    modifier: Modifier,
    name: String,
    onNameChange: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 42.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "PERSONALIZE",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = Lavender
        )
        Text(
            text = "What should WakeSync call you?",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = colors.onBackground
        )
        Text(
            text = "This is only used for greetings like “Good morning, Alex” and is stored locally.",
            color = colors.onSurfaceVariant
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = colors.surface.copy(alpha = 0.68f),
                contentColor = colors.onSurface
            )
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                value = name,
                onValueChange = onNameChange,
                singleLine = true,
                label = {
                    Text("Preferred name")
                },
                placeholder = {
                    Text("Alex")
                }
            )
        }
    }
}

@Composable
private fun OobeTheme(
    modifier: Modifier,
    selected: String,
    onSelected: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 42.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "APPEARANCE",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = Lavender
        )
        Text(
            text = "Choose your starting look.",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = colors.onBackground
        )
        Text(
            text = "You can change this anytime in Settings.",
            color = colors.onSurfaceVariant
        )

        Column(
            modifier = Modifier.padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf(
                AppSettingsStore.THEME_SYSTEM to "System Default",
                AppSettingsStore.THEME_DARK to "Dark Mode",
                AppSettingsStore.THEME_LIGHT to "Light Mode"
            ).forEach { (value, label) ->
                FilterChip(
                    modifier = Modifier.fillMaxWidth(),
                    selected = selected == value,
                    onClick = {
                        onSelected(value)
                    },
                    label = {
                        Text(
                            text = label,
                            modifier = Modifier.padding(vertical = 7.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun OobeHealthConnect(
    modifier: Modifier,
    sdkStatus: Int,
    connected: Boolean,
    onConnect: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val unavailable =
        sdkStatus == HealthConnectClient.SDK_UNAVAILABLE ||
            sdkStatus ==
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 42.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "HEALTH CONNECT",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = Cyan
        )
        Text(
            text = "Connect your sleep data.",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = colors.onBackground
        )
        Text(
            text = "WakeSync uses read-only Health Connect access for sleep sessions and stages. No vendor-specific account is required.",
            color = colors.onSurfaceVariant
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = colors.surface.copy(alpha = 0.68f),
                contentColor = colors.onSurface
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (connected) {
                        "✓ Health Connect Integration"
                    } else {
                        "Health Connect Integration"
                    },
                    fontWeight = FontWeight.ExtraBold,
                    color = if (connected) {
                        Color(0xFF10B981)
                    } else {
                        colors.onSurface
                    }
                )
                Text(
                    text = "Compatible with Pixel Watch, Galaxy Watch, Garmin, Oura, Fitbit, and all Health Connect wearables.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )

                if (!connected && !unavailable) {
                    Button(
                        onClick = onConnect,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "Connect Health Connect",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (unavailable) {
                    Text(
                        text = "Health Connect is not currently available on this device. You can finish setup and use WakeSync alarms, then connect later.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Sunrise
                    )
                }

                if (!connected && !unavailable) {
                    Text(
                        text = "You can also finish setup now and connect later from Settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun OobeExactAlarm(
    modifier: Modifier,
    granted: Boolean,
    onGrant: () -> Unit
) {
    val colors =
        MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 42.dp),
        verticalArrangement =
            Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "ALARM RELIABILITY",
            style =
                MaterialTheme.typography.labelLarge,
            fontWeight =
                FontWeight.ExtraBold,
            color = Sunrise
        )

        Text(
            text =
                "Protect your wake deadline.",
            style =
                MaterialTheme.typography.headlineLarge,
            fontWeight =
                FontWeight.ExtraBold,
            color = colors.onBackground
        )

        Text(
            text =
                "Android restricts exact timers by default. Granting Exact Alarm access ensures your wake schedule fires precisely on time without battery-saver delays.",
            color =
                colors.onSurfaceVariant
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            shape =
                RoundedCornerShape(24.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        colors.surface.copy(
                            alpha = 0.68f
                        ),
                    contentColor =
                        colors.onSurface
                )
        ) {
            Column(
                modifier =
                    Modifier.padding(18.dp),
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Column(
                        modifier =
                            Modifier.weight(1f)
                    ) {
                        Text(
                            text =
                                "Exact Alarm Access",
                            fontWeight =
                                FontWeight.ExtraBold
                        )
                        Text(
                            modifier =
                                Modifier.padding(
                                    top = 3.dp
                                ),
                            text =
                                if (granted) {
                                    "Granted · precise wake scheduling is ready"
                                } else {
                                    "Action required for precise wake deadlines"
                                },
                            style =
                                MaterialTheme.typography.bodySmall,
                            color =
                                if (granted) {
                                    Color(0xFF10B981)
                                } else {
                                    Sunrise
                                }
                        )
                    }

                    Text(
                        text =
                            if (granted) {
                                "✓ Granted"
                            } else {
                                "Required"
                            },
                        style =
                            MaterialTheme.typography.labelMedium,
                        fontWeight =
                            FontWeight.ExtraBold,
                        color =
                            if (granted) {
                                Color(0xFF10B981)
                            } else {
                                Sunrise
                            }
                    )
                }

                if (!granted) {
                    Button(
                        modifier =
                            Modifier.fillMaxWidth(),
                        onClick = onGrant,
                        shape =
                            RoundedCornerShape(
                                999.dp
                            ),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    Sunrise,
                                contentColor =
                                    Color(0xFF0F172A)
                            )
                    ) {
                        Text(
                            text =
                                "Grant Exact Alarm Permission",
                            fontWeight =
                                FontWeight.ExtraBold
                        )
                    }
                }

                Text(
                    text =
                        "After granting access in Android settings, return to WakeSync. The status above updates automatically.",
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        colors.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun OobePage(
    modifier: Modifier,
    eyebrow: String,
    title: String,
    body: String,
    footer: String
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 54.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = eyebrow,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = Lavender
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = colors.onBackground
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant
        )

        Card(
            modifier = Modifier.padding(top = 10.dp),
            shape = RoundedCornerShape(999.dp),
            colors = CardDefaults.cardColors(
                containerColor = Cyan.copy(alpha = 0.10f),
                contentColor = Cyan
            )
        ) {
            Text(
                modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 8.dp
                ),
                text = "🔒 $footer",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
