package com.kiranoommen.wakesync.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kiranoommen.wakesync.domain.NightAnalytics
import com.kiranoommen.wakesync.domain.PeriodAnalytics
import com.kiranoommen.wakesync.domain.ScoreBreakdown
import com.kiranoommen.wakesync.domain.SleepAnalytics
import com.kiranoommen.wakesync.model.SleepNight
import com.kiranoommen.wakesync.model.SleepStageType
import com.kiranoommen.wakesync.ui.theme.Cyan
import com.kiranoommen.wakesync.ui.theme.Indigo
import com.kiranoommen.wakesync.ui.theme.Lavender
import com.kiranoommen.wakesync.ui.theme.Mint
import com.kiranoommen.wakesync.ui.theme.Sunrise
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class SleepRange(
    val label: String,
    val days: Long?
) {
    WEEK("Past Week", 7),
    TWO_WEEKS("Past 2 Weeks", 14),
    MONTH("Past Month", 30),
    CUSTOM("Custom", null)
}

private enum class SleepSort {
    DATE,
    SCORE,
    DURATION
}

private val SleepScoreGreen = Color(0xFF10B981)
private val SleepScoreCyan = Color(0xFF06B6D4)
private val SleepScoreAmber = Color(0xFFF59E0B)
private val SleepScoreCoral = Color(0xFFEF4444)
private val StageRemBlue = Color(0xFF3B82F6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepAnalyticsScreen(
    nights: List<SleepNight>,
    loading: Boolean,
    targetSleepMinutes: Int,
    onRefresh: () -> Unit,
    onShareCsv: (List<NightAnalytics>) -> Unit,
    onSharePdf: (PeriodAnalytics) -> Unit,
    onShareStory: (PeriodAnalytics) -> Unit
) {
    var range by remember { mutableStateOf(SleepRange.TWO_WEEKS) }
    var compare by remember { mutableStateOf(false) }
    var sort by remember { mutableStateOf(SleepSort.DATE) }
    var customStart by remember { mutableStateOf<LocalDate?>(null) }
    var customEnd by remember { mutableStateOf<LocalDate?>(null) }
    var showCustomRange by remember { mutableStateOf(false) }
    var selectedNight by remember { mutableStateOf<NightAnalytics?>(null) }
    var showExportPreview by remember { mutableStateOf(false) }
    var showScoreBreakdown by remember { mutableStateOf(false) }
    var infoSheet by remember { mutableStateOf<MetricInfo?>(null) }

    val today = LocalDate.now()
    val rangeEnd = if (range == SleepRange.CUSTOM) {
        customEnd ?: today
    } else {
        today
    }
    val rangeStart = when {
        range == SleepRange.CUSTOM ->
            customStart ?: today.minusDays(13)
        range.days != null ->
            rangeEnd.minusDays(range.days!! - 1)
        else ->
            today.minusDays(13)
    }

    val currentNights = remember(nights, rangeStart, rangeEnd) {
        nights.filter { night ->
            val date = night.end
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            !date.isBefore(rangeStart) &&
                !date.isAfter(rangeEnd)
        }
    }

    val analytics = remember(currentNights, targetSleepMinutes) {
        SleepAnalytics.analyze(
            currentNights,
            targetSleepMinutes
        )
    }

    val previousAnalytics = remember(
        nights,
        rangeStart,
        rangeEnd,
        compare,
        targetSleepMinutes
    ) {
        if (!compare) {
            null
        } else {
            val span =
                java.time.temporal.ChronoUnit.DAYS
                    .between(rangeStart, rangeEnd) + 1
            val previousEnd = rangeStart.minusDays(1)
            val previousStart =
                previousEnd.minusDays(span - 1)

            val previousNights = nights.filter { night ->
                val date = night.end
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                !date.isBefore(previousStart) &&
                    !date.isAfter(previousEnd)
            }

            SleepAnalytics.analyze(
                previousNights,
                targetSleepMinutes
            )
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        RangeControlCard(
            selected = range,
            compare = compare,
            rangeStart = rangeStart,
            rangeEnd = rangeEnd,
            onSelect = { selected ->
                if (selected == SleepRange.CUSTOM) {
                    showCustomRange = true
                } else {
                    range = selected
                }
            },
            onCompareChanged = { compare = it },
            onInfo = {
                infoSheet = MetricInfo(
                    title = "Date range & comparison",
                    meaning = "Choose the period WakeSync summarizes. Comparison mode checks the immediately preceding period of the same length.",
                    measurement = "Sleep sessions are filtered by their wake date using Health Connect timestamps already on this device.",
                    importance = "Short ranges show recent changes quickly; longer ranges help reveal consistency and direction without overreacting to one night."
                )
            }
        )

        if (loading) {
            SleepSkeleton()
        } else if (analytics.nights.isEmpty()) {
            EmptyRangeCard(
                rangeStart = rangeStart,
                rangeEnd = rangeEnd,
                onRefresh = onRefresh,
                onInfo = {
                    infoSheet = MetricInfo(
                        title = "No data in this period",
                        meaning = "WakeSync has no sleep sessions available for the selected dates.",
                        measurement = "WakeSync only displays records Health Connect has permission to return.",
                        importance = "Missing tracker days are not treated as bad sleep or as 24 hours awake."
                    )
                }
            )
        } else {
            ScoreHeroCard(
                analytics = analytics,
                previous = previousAnalytics,
                targetSleepMinutes = targetSleepMinutes,
                onScoreClick = { showScoreBreakdown = true },
                onInfo = {
                    infoSheet = MetricInfo(
                        title = "WakeSync Sleep Score",
                        meaning = "A 0–100 wellness summary of how your recent sleep compares with your own goals and pattern.",
                        measurement = "40% Duration + 25% Efficiency + 20% Stage Ratios + 15% Consistency. It is a WakeSync score, not a clinical score.",
                        importance = "The score compresses several signals into one quick glance while the pillar breakdown keeps the math transparent."
                    )
                }
            )

            InsightCard(
                text = SleepAnalytics.insightFor(
                    analytics,
                    targetSleepMinutes
                ),
                onInfo = {
                    infoSheet = MetricInfo(
                        title = "WakeSync Insight",
                        meaning = "A plain-language observation generated from your recent local sleep trend.",
                        measurement = "WakeSync compares recent duration, consistency and stage trends on-device. No health data is sent to a cloud AI service.",
                        importance = "A useful insight should help you notice repeatable patterns, not make a diagnosis from one night."
                    )
                }
            )

            analytics.nights.firstOrNull()?.let { latest ->
                HypnogramCard(
                    night = latest,
                    onInfo = {
                        infoSheet = MetricInfo(
                            title = "Sleep hypnogram",
                            meaning = "A timeline showing when the wearable classified you as Awake, REM, Light or Deep sleep.",
                            measurement = "Built directly from the stage intervals provided in the Health Connect sleep session.",
                            importance = "The shape helps you see fragmentation and stage transitions. Consumer wearable stages are estimates, not polysomnography."
                        )
                    }
                )
            }

            TrendCard(
                nights = analytics.nights
                    .sortedBy { it.date },
                previous = previousAnalytics
                    ?.nights
                    ?.sortedBy { it.date },
                onInfo = {
                    infoSheet = MetricInfo(
                        title = "Duration & efficiency trend",
                        meaning = "Bars show total sleep duration; the line shows estimated sleep efficiency.",
                        measurement = "Duration is sleep-stage minutes. Efficiency is estimated sleep minutes divided by the Health Connect sleep-session duration.",
                        importance = "Looking at both together helps distinguish short sleep from fragmented or inefficient sleep."
                    )
                }
            )

            ArchitectureCard(
                analytics = analytics,
                onInfo = {
                    infoSheet = MetricInfo(
                        title = "Architecture & regularity",
                        meaning = "Stage ratios summarize Deep and REM trends. SRI estimates how consistent your sleep/wake timing is from day to day.",
                        measurement = "Stage ratios use wearable-classified stage minutes. SRI compares sleep/wake state in 15-minute clock-time epochs on adjacent tracked days.",
                        importance = "Regularity can make sleep timing more predictable, while stage ratios are best treated as personal trends rather than fixed targets."
                    )
                }
            )

            RecoveryCard(
                analytics = analytics,
                onInfo = {
                    infoSheet = MetricInfo(
                        title = "Biometric recovery",
                        meaning = "HRV and resting heart rate add autonomic and cardiovascular context when your source provides them.",
                        measurement = "WakeSync reads optional HRV and resting-heart-rate records from Health Connect and summarizes the values associated with recent nights.",
                        importance = "These metrics are usually most useful relative to your own baseline, not a universal good/bad threshold."
                    )
                }
            )

            SleepLogCard(
                analytics = analytics,
                sort = sort,
                onSort = { sort = it },
                onNightSelected = { selectedNight = it },
                onInfo = {
                    infoSheet = MetricInfo(
                        title = "Sleep log",
                        meaning = "A sortable list of nightly sleep summaries.",
                        measurement = "Each row is calculated from one Health Connect sleep session and its available stage and recovery records.",
                        importance = "Night-level detail lets you inspect outliers without losing the larger trend."
                    )
                }
            )

            ExportCard(
                onOpenPreview = {
                    showExportPreview = true
                },
                onInfo = {
                    infoSheet = MetricInfo(
                        title = "Export & share",
                        meaning = "WakeSync can generate a PDF summary, CSV log or shareable visual card only when you explicitly request it.",
                        measurement = "Exports are created locally from the currently selected period.",
                        importance = "The preview tells you exactly what leaves the app before Android's share sheet opens."
                    )
                }
            )
        }
    }

    MetricInfoBottomSheet(
        info = infoSheet,
        onDismiss = { infoSheet = null }
    )

    if (showScoreBreakdown) {
        ScoreBreakdownSheet(
            analytics = analytics,
            onDismiss = {
                showScoreBreakdown = false
            }
        )
    }

    if (selectedNight != null) {
        NightBreakdownSheet(
            night = selectedNight!!,
            targetSleepMinutes = targetSleepMinutes,
            onDismiss = {
                selectedNight = null
            }
        )
    }

    if (showExportPreview) {
        ExportPreviewSheet(
            analytics = analytics,
            onDismiss = {
                showExportPreview = false
            },
            onPdf = {
                showExportPreview = false
                onSharePdf(analytics)
            },
            onCsv = {
                showExportPreview = false
                onShareCsv(
                    sortedNights(
                        analytics.nights,
                        sort
                    )
                )
            },
            onStory = {
                showExportPreview = false
                onShareStory(analytics)
            }
        )
    }

    if (showCustomRange) {
        CustomRangeSheet(
            initialStart = customStart ?: rangeStart,
            initialEnd = customEnd ?: rangeEnd,
            onDismiss = {
                showCustomRange = false
            },
            onApply = { start, end ->
                customStart = start
                customEnd = end
                range = SleepRange.CUSTOM
                showCustomRange = false
            }
        )
    }
}

@Composable
private fun RangeControlCard(
    selected: SleepRange,
    compare: Boolean,
    rangeStart: LocalDate,
    rangeEnd: LocalDate,
    onSelect: (SleepRange) -> Unit,
    onCompareChanged: (Boolean) -> Unit,
    onInfo: () -> Unit
) {
    BentoCard {
        Column {
            CardTitleRow(
                title = "Sleep range",
                onInfo = onInfo
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {
                SleepRange.values().forEach { item ->
                    SegmentButton(
                        modifier = Modifier.weight(1f),
                        label = when (item) {
                            SleepRange.WEEK -> "Week"
                            SleepRange.TWO_WEEKS -> "2 Weeks"
                            SleepRange.MONTH -> "Month"
                            SleepRange.CUSTOM -> "Custom"
                        },
                        selected = selected == item,
                        onClick = {
                            onSelect(item)
                        }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text =
                            rangeStart.format(
                                DateTimeFormatter.ofPattern("MMM d")
                            ) +
                                " – " +
                                rangeEnd.format(
                                    DateTimeFormatter.ofPattern("MMM d")
                                ),
                        style =
                            MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color =
                            MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        modifier =
                            Modifier.padding(top = 2.dp),
                        text = "Compare with previous period",
                        style =
                            MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = compare,
                    onCheckedChange = onCompareChanged
                )
            }
        }
    }
}

@Composable
private fun SegmentButton(
    modifier: Modifier,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(
            1.dp,
            if (selected) {
                Lavender.copy(alpha = 0.42f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                Lavender.copy(alpha = 0.17f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
                    .copy(alpha = 0.34f)
            },
            contentColor = if (selected) {
                Color.White
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight =
                    if (selected) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Medium
                    }
            )
        }
    }
}

@Composable
private fun ScoreHeroCard(
    analytics: PeriodAnalytics,
    previous: PeriodAnalytics?,
    targetSleepMinutes: Int,
    onScoreClick: () -> Unit,
    onInfo: () -> Unit
) {
    val score = analytics.averageScore ?: 0
    val scoreColor = scoreColor(score)
    val delta = previous
        ?.averageScore
        ?.let { score - it }

    BentoCard(
        borderColor = scoreColor.copy(alpha = 0.28f)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    color = Lavender.copy(alpha = 0.12f),
                    radius =
                        size.minDimension * 0.72f,
                    center = Offset(
                        size.width * 0.06f,
                        size.height * 0.08f
                    )
                )
                drawCircle(
                    color = scoreColor.copy(alpha = 0.10f),
                    radius =
                        size.minDimension * 0.58f,
                    center = Offset(
                        size.width * 0.94f,
                        size.height * 0.92f
                    )
                )
            }

            Column {
                CardTitleRow(
                    title = "Sleep performance",
                    onInfo = onInfo
                )

                Row(
                    modifier = Modifier.padding(top = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ScoreRing(
                        score = score,
                        color = scoreColor,
                        modifier = Modifier
                            .size(134.dp),
                        onClick = onScoreClick
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 18.dp)
                    ) {
                        Text(
                            text = scoreStatus(score),
                            color = scoreColor,
                            fontWeight = FontWeight.ExtraBold
                        )

                        if (
                            delta != null &&
                            previous.nights.isNotEmpty()
                        ) {
                            StatusPill(
                                modifier =
                                    Modifier.padding(top = 8.dp),
                                text =
                                    (if (delta >= 0) "+" else "") +
                                        delta +
                                        " vs previous",
                                color =
                                    if (delta >= 0) {
                                        Mint
                                    } else {
                                        SleepScoreCoral
                                    }
                            )
                        }

                        Spacer(
                            modifier =
                                Modifier.height(12.dp)
                        )

                        MetricLine(
                            "Avg sleep",
                            formatMinutes(
                                analytics.averageSleepMinutes
                            )
                        )
                        MetricLine(
                            "Efficiency",
                            analytics.averageEfficiencyPercent
                                ?.let {
                                    it.toString() + "%"
                                }
                                ?: "—"
                        )
                        MetricLine(
                            "Sleep debt",
                            if (
                                analytics.sleepDebtMinutes > 0
                            ) {
                                "-" +
                                    formatMinutes(
                                        analytics.sleepDebtMinutes
                                    )
                            } else {
                                "0m"
                            }
                        )
                        MetricLine(
                            "Target",
                            formatMinutes(
                                targetSleepMinutes.toLong()
                            )
                        )
                    }
                }

                Text(
                    modifier = Modifier.padding(top = 12.dp),
                    text =
                        "Tap the ring to see exactly how the score is calculated.",
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ScoreRing(
    score: Int,
    color: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val animatedScore by animateFloatAsState(
        targetValue =
            score.coerceIn(0, 100).toFloat(),
        animationSpec = spring(
            stiffness = 300f,
            dampingRatio = 0.78f
        ),
        label = "sleepScoreRing"
    )

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
            contentColor =
                MaterialTheme.colorScheme.onSurface
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val track =
                MaterialTheme.colorScheme.onSurface
                    .copy(alpha = 0.08f)

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(7.dp)
                    .semantics {
                        contentDescription =
                            "Sleep score " +
                                score +
                                " out of 100. Tap for score breakdown."
                    }
            ) {
                val stroke = 11.dp.toPx()
                val diameter =
                    size.minDimension - stroke
                val origin = Offset(
                    (size.width - diameter) / 2f,
                    (size.height - diameter) / 2f
                )

                drawArc(
                    color = track,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = origin,
                    size = Size(
                        diameter,
                        diameter
                    ),
                    style = Stroke(
                        width = stroke,
                        cap = StrokeCap.Round
                    )
                )

                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(
                            color.copy(alpha = 0.72f),
                            color,
                            Color.White.copy(alpha = 0.92f),
                            color
                        )
                    ),
                    startAngle = -90f,
                    sweepAngle =
                        360f *
                            animatedScore /
                            100f,
                    useCenter = false,
                    topLeft = origin,
                    size = Size(
                        diameter,
                        diameter
                    ),
                    style = Stroke(
                        width = stroke,
                        cap = StrokeCap.Round
                    )
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = score.toString(),
                    style =
                        MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color =
                        MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "/ 100",
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InsightCard(
    text: String,
    onInfo: () -> Unit
) {
    BentoCard(
        borderColor = Lavender.copy(alpha = 0.30f)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    color = Lavender.copy(alpha = 0.13f),
                    radius =
                        size.minDimension * 0.66f,
                    center = Offset(
                        size.width * 0.94f,
                        size.height * 0.08f
                    )
                )
            }

            Column {
                CardTitleRow(
                    title = "✨ WakeSync Insight",
                    titleColor = Lavender,
                    onInfo = onInfo
                )

                Text(
                    modifier = Modifier.padding(top = 10.dp),
                    text = text,
                    style =
                        MaterialTheme.typography.bodyLarge,
                    color =
                        MaterialTheme.colorScheme.onSurface
                )

                Text(
                    modifier = Modifier.padding(top = 9.dp),
                    text =
                        "Generated locally from your on-device trend data",
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HypnogramCard(
    night: NightAnalytics,
    onInfo: () -> Unit
) {
    BentoCard {
        Column {
            CardTitleRow(
                title = "Last night · Hypnogram",
                onInfo = onInfo
            )

            Text(
                modifier = Modifier.padding(top = 3.dp),
                text = night.date.format(
                    DateTimeFormatter.ofPattern(
                        "EEE, MMM d"
                    )
                ),
                style = MaterialTheme.typography.bodySmall,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Hypnogram(
                night = night.night,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                MiniMetric(
                    modifier = Modifier.weight(1f),
                    label = "Latency",
                    value = night.onsetLatencyMinutes
                        ?.let(::formatMinutes)
                        ?: "—"
                )
                MiniMetric(
                    modifier = Modifier.weight(1f),
                    label = "WASO",
                    value =
                        formatMinutes(
                            night.wasoMinutes
                        )
                )
                MiniMetric(
                    modifier = Modifier.weight(1f),
                    label = "Efficiency",
                    value = night.efficiencyPercent
                        ?.let {
                            it.toString() + "%"
                        }
                        ?: "—"
                )
            }
        }
    }
}

@Composable
private fun Hypnogram(
    night: SleepNight,
    modifier: Modifier = Modifier
) {
    val labels = listOf(
        "Awake",
        "REM",
        "Light",
        "Deep"
    )
    val totalMillis =
        Duration.between(
            night.start,
            night.end
        )
            .toMillis()
            .coerceAtLeast(1L)
    val ordered =
        night.stages.sortedBy { it.start }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier
                    .width(50.dp)
                    .height(164.dp),
                verticalArrangement =
                    Arrangement.SpaceAround
            ) {
                labels.forEach { label ->
                    Text(
                        text = label,
                        style =
                            MaterialTheme.typography.labelSmall,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val gridColor =
                MaterialTheme.colorScheme.onSurface
                    .copy(alpha = 0.08f)

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(164.dp)
                    .semantics {
                        contentDescription =
                            "Hypnogram with Awake, REM, Light and Deep stages from " +
                                formatInstant(
                                    night.start,
                                    "h:mm a"
                                ) +
                                " to " +
                                formatInstant(
                                    night.end,
                                    "h:mm a"
                                )
                    }
            ) {
                val rowHeight =
                    size.height / 4f

                repeat(4) { index ->
                    val y =
                        rowHeight *
                            (index + 0.5f)

                    drawLine(
                        color = gridColor,
                        start = Offset(
                            0f,
                            y
                        ),
                        end = Offset(
                            size.width,
                            y
                        ),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                fun yFor(
                    type: SleepStageType
                ): Float =
                    when (type) {
                        SleepStageType.AWAKE ->
                            rowHeight * 0.5f
                        SleepStageType.REM ->
                            rowHeight * 1.5f
                        SleepStageType.LIGHT ->
                            rowHeight * 2.5f
                        SleepStageType.DEEP ->
                            rowHeight * 3.5f
                        SleepStageType.UNKNOWN ->
                            rowHeight * 2.5f
                    }

                fun colorFor(
                    type: SleepStageType
                ): Color =
                    when (type) {
                        SleepStageType.AWAKE ->
                            SleepScoreAmber
                        SleepStageType.REM ->
                            StageRemBlue
                        SleepStageType.LIGHT ->
                            Cyan
                        SleepStageType.DEEP ->
                            Lavender
                        SleepStageType.UNKNOWN ->
                            MaterialTheme.colorScheme.onSurfaceVariant
                    }

                ordered.forEachIndexed {
                        index,
                        segment ->
                    val startFraction =
                        Duration.between(
                            night.start,
                            segment.start
                        )
                            .toMillis()
                            .toFloat() /
                            totalMillis.toFloat()
                    val endFraction =
                        Duration.between(
                            night.start,
                            segment.end
                        )
                            .toMillis()
                            .toFloat() /
                            totalMillis.toFloat()

                    val xStart =
                        size.width *
                            startFraction
                                .coerceIn(
                                    0f,
                                    1f
                                )
                    val xEnd =
                        size.width *
                            endFraction
                                .coerceIn(
                                    0f,
                                    1f
                                )
                    val y =
                        yFor(
                            segment.type
                        )

                    drawLine(
                        color = colorFor(
                            segment.type
                        ),
                        start = Offset(
                            xStart,
                            y
                        ),
                        end = Offset(
                            xEnd,
                            y
                        ),
                        strokeWidth =
                            11.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    if (
                        index <
                        ordered.lastIndex
                    ) {
                        val next =
                            ordered[index + 1]
                        drawLine(
                            color = colorFor(
                                next.type
                            ).copy(alpha = 0.72f),
                            start = Offset(
                                xEnd,
                                y
                            ),
                            end = Offset(
                                xEnd,
                                yFor(
                                    next.type
                                )
                            ),
                            strokeWidth =
                                2.dp.toPx(),
                            cap =
                                StrokeCap.Round
                        )
                    }
                }
            }
        }

        val ticks = remember(
            night.start,
            night.end
        ) {
            listOf(
                night.start,
                night.start.plusMillis(
                    totalMillis / 3
                ),
                night.start.plusMillis(
                    totalMillis * 2 / 3
                ),
                night.end
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 50.dp, top = 5.dp),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {
            ticks.forEach { instant ->
                Text(
                    text =
                        formatInstant(
                            instant,
                            "h:mm a"
                        ),
                    style =
                        MaterialTheme.typography.labelSmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TrendCard(
    nights: List<NightAnalytics>,
    previous: List<NightAnalytics>?,
    onInfo: () -> Unit
) {
    var selectedIndex by remember(nights) {
        mutableIntStateOf(
            (nights.size - 1)
                .coerceAtLeast(0)
        )
    }

    BentoCard {
        Column {
            CardTitleRow(
                title = "Duration & efficiency",
                onInfo = onInfo
            )

            Row(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                LegendPill(
                    label = "▮  Bar · Total sleep",
                    color = Lavender
                )
                LegendPill(
                    label = "╱  Line · Efficiency",
                    color = Cyan
                )
            }

            Text(
                modifier = Modifier.padding(top = 8.dp),
                text =
                    "Tap or drag across the chart to inspect a night.",
                style = MaterialTheme.typography.bodySmall,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (nights.isNotEmpty()) {
                TrendChart(
                    nights = nights,
                    previous = previous,
                    selectedIndex = selectedIndex,
                    onSelect = {
                        selectedIndex = it
                    }
                )

                val selected =
                    nights[
                        selectedIndex.coerceIn(
                            0,
                            nights.lastIndex
                        )
                    ]

                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(
                        animationSpec = spring(
                            stiffness = 300f,
                            dampingRatio = 0.78f
                        )
                    ) + fadeIn(),
                    exit = slideOutVertically() +
                        fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape =
                            RoundedCornerShape(18.dp),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant
                        ),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme.colorScheme.surfaceVariant
                                        .copy(
                                            alpha = 0.45f
                                        ),
                                contentColor =
                                    MaterialTheme.colorScheme.onSurface
                            )
                    ) {
                        Row(
                            modifier =
                                Modifier.padding(
                                    12.dp
                                ),
                            horizontalArrangement =
                                Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text =
                                        selected.date.format(
                                            DateTimeFormatter.ofPattern(
                                                "EEE, MMM d"
                                            )
                                        ),
                                    fontWeight =
                                        FontWeight.Bold
                                )
                                Text(
                                    modifier =
                                        Modifier.padding(
                                            top = 2.dp
                                        ),
                                    text =
                                        "Total sleep",
                                    style =
                                        MaterialTheme.typography.bodySmall,
                                    color =
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(
                                horizontalAlignment =
                                    Alignment.End
                            ) {
                                Text(
                                    text =
                                        formatMinutes(
                                            selected.asleepMinutes
                                        ),
                                    fontWeight =
                                        FontWeight.ExtraBold
                                )
                                Text(
                                    text =
                                        "Efficiency " +
                                            (
                                                selected.efficiencyPercent
                                                    ?.toString()
                                                    ?: "—"
                                                ) +
                                            "%",
                                    style =
                                        MaterialTheme.typography.bodySmall,
                                    color =
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (
                previous != null &&
                previous.isNotEmpty() &&
                nights.isNotEmpty()
            ) {
                val currentAvg =
                    nights.map {
                        it.asleepMinutes
                    }
                        .average()
                        .roundToInt()
                val previousAvg =
                    previous.map {
                        it.asleepMinutes
                    }
                        .average()
                        .roundToInt()
                val delta =
                    currentAvg - previousAvg

                Text(
                    modifier =
                        Modifier.padding(top = 10.dp),
                    text =
                        "Previous period · " +
                            (
                                if (delta >= 0) {
                                    "+"
                                } else {
                                    "-"
                                }
                                ) +
                            formatMinutes(
                                abs(delta).toLong()
                            ) +
                            " average sleep",
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TrendChart(
    nights: List<NightAnalytics>,
    previous: List<NightAnalytics>?,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    val gridColor =
        MaterialTheme.colorScheme.onSurface
            .copy(alpha = 0.08f)
    val previousBarColor =
        MaterialTheme.colorScheme.onSurfaceVariant
            .copy(alpha = 0.16f)

    fun selectFromX(
        x: Float,
        width: Float
    ) {
        if (
            nights.isEmpty() ||
            width <= 0f
        ) {
            return
        }

        val widthPer =
            width /
                nights.size.toFloat()

        onSelect(
            (x / widthPer)
                .toInt()
                .coerceIn(
                    0,
                    nights.lastIndex
                )
        )
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(205.dp)
            .padding(top = 14.dp)
            .semantics {
                contentDescription =
                    "Total sleep bars and sleep efficiency line. Drag across the chart to inspect nights."
            }
            .pointerInput(nights) {
                detectTapGestures { offset ->
                    selectFromX(
                        offset.x,
                        size.width.toFloat()
                    )
                }
            }
            .pointerInput(nights) {
                detectDragGestures(
                    onDragStart = { offset ->
                        selectFromX(
                            offset.x,
                            size.width.toFloat()
                        )
                    },
                    onDrag = {
                            change,
                            _ ->
                        selectFromX(
                            change.position.x,
                            size.width.toFloat()
                        )
                    }
                )
            }
    ) {
        if (nights.isEmpty()) {
            return@Canvas
        }

        val maxMinutes = maxOf(
            540L,
            nights.maxOf {
                it.asleepMinutes
            }.coerceAtLeast(1L)
        )
        val widthPer =
            size.width /
                nights.size.toFloat()
        val barWidth =
            widthPer * 0.46f

        repeat(4) { index ->
            val y =
                size.height *
                    (index + 1) /
                    5f

            drawLine(
                color = gridColor,
                start = Offset(
                    0f,
                    y
                ),
                end = Offset(
                    size.width,
                    y
                ),
                strokeWidth =
                    1.dp.toPx()
            )
        }

        previous
            ?.take(nights.size)
            ?.forEachIndexed {
                    index,
                    night ->
                val centerX =
                    widthPer *
                        index +
                        widthPer /
                        2f
                val height =
                    night.asleepMinutes
                        .toFloat() /
                        maxMinutes.toFloat() *
                        size.height *
                        0.82f

                drawRoundRect(
                    color = previousBarColor,
                    topLeft = Offset(
                        centerX -
                            barWidth *
                            0.68f,
                        size.height -
                            height
                    ),
                    size = Size(
                        barWidth * 1.36f,
                        height
                    ),
                    cornerRadius =
                        CornerRadius(
                            6.dp.toPx(),
                            6.dp.toPx()
                        )
                )
            }

        nights.forEachIndexed {
                index,
                night ->
            val centerX =
                widthPer *
                    index +
                    widthPer /
                    2f
            val selected =
                index == selectedIndex
            val barHeight =
                night.asleepMinutes
                    .toFloat() /
                    maxMinutes.toFloat() *
                    size.height *
                    0.82f

            drawRoundRect(
                color =
                    Lavender.copy(
                        alpha =
                            if (selected) {
                                0.94f
                            } else {
                                0.32f
                            }
                    ),
                topLeft = Offset(
                    centerX -
                        barWidth /
                        2f,
                    size.height -
                        barHeight
                ),
                size = Size(
                    barWidth,
                    barHeight
                ),
                cornerRadius =
                    CornerRadius(
                        8.dp.toPx(),
                        8.dp.toPx()
                    )
            )
        }

        var previousPoint: Offset? = null

        nights.forEachIndexed {
                index,
                night ->
            val centerX =
                widthPer *
                    index +
                    widthPer /
                    2f
            val efficiency =
                (night.efficiencyPercent ?: 0)
                    .coerceIn(
                        0,
                        100
                    )
            val y =
                size.height -
                    efficiency /
                    100f *
                    size.height *
                    0.82f
            val point =
                Offset(
                    centerX,
                    y
                )

            previousPoint?.let {
                drawLine(
                    color =
                        Cyan.copy(
                            alpha = 0.92f
                        ),
                    start = it,
                    end = point,
                    strokeWidth =
                        3.dp.toPx(),
                    cap =
                        StrokeCap.Round
                )
            }

            drawCircle(
                color =
                    if (
                        index ==
                        selectedIndex
                    ) {
                        Color.White
                    } else {
                        Cyan
                    },
                radius =
                    if (
                        index ==
                        selectedIndex
                    ) {
                        5.dp.toPx()
                    } else {
                        3.dp.toPx()
                    },
                center = point
            )

            previousPoint = point
        }
    }
}

@Composable
private fun ArchitectureCard(
    analytics: PeriodAnalytics,
    onInfo: () -> Unit
) {
    BentoCard {
        Column {
            CardTitleRow(
                title = "Architecture & regularity",
                onInfo = onInfo
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            MetricLine(
                "Deep sleep",
                analytics.averageDeepPercent
                    ?.let {
                        it.toString() +
                            "% avg"
                    }
                    ?: "—"
            )
            MetricLine(
                "REM sleep",
                analytics.averageRemPercent
                    ?.let {
                        it.toString() +
                            "% avg"
                    }
                    ?: "—"
            )
            MetricLine(
                "Sleep Regularity Index",
                analytics.regularityScore
                    ?.let {
                        it.toString() +
                            " / 100"
                    }
                    ?: "Learning"
            )

            Text(
                modifier = Modifier.padding(top = 10.dp),
                text =
                    "Wearable stage percentages are trend signals, not clinical ground truth.",
                style = MaterialTheme.typography.bodySmall,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RecoveryCard(
    analytics: PeriodAnalytics,
    onInfo: () -> Unit
) {
    BentoCard(
        borderColor = Indigo.copy(alpha = 0.26f)
    ) {
        Column {
            CardTitleRow(
                title = "Biometric recovery",
                onInfo = onInfo
            )

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            MetricLine(
                "HRV",
                analytics.averageHrvMs
                    ?.let {
                        it.roundToInt()
                            .toString() +
                            " ms avg"
                    }
                    ?: "Not available"
            )
            MetricLine(
                "Resting heart rate",
                analytics.averageRestingHeartRateBpm
                    ?.let {
                        it.roundToInt()
                            .toString() +
                            " bpm avg"
                    }
                    ?: "Not available"
            )

            Text(
                modifier = Modifier.padding(top = 8.dp),
                text =
                    if (
                        analytics.averageHrvMs == null &&
                        analytics.averageRestingHeartRateBpm == null
                    ) {
                        "Grant optional recovery access in Settings if your Health Connect source provides these records."
                    } else {
                        "Use recovery metrics relative to your own baseline."
                    },
                style =
                    MaterialTheme.typography.bodySmall,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SleepLogCard(
    analytics: PeriodAnalytics,
    sort: SleepSort,
    onSort: (SleepSort) -> Unit,
    onNightSelected: (NightAnalytics) -> Unit,
    onInfo: () -> Unit
) {
    val rows =
        sortedNights(
            analytics.nights,
            sort
        )

    BentoCard {
        Column {
            CardTitleRow(
                title = "Sleep log",
                onInfo = onInfo
            )

            Row(
                modifier = Modifier
                    .horizontalScroll(
                        rememberScrollState()
                    )
                    .padding(top = 8.dp),
                horizontalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {
                SleepSort.values()
                    .forEach { item ->
                        FilterChip(
                            selected =
                                sort == item,
                            onClick = {
                                onSort(item)
                            },
                            label = {
                                Text(
                                    when (item) {
                                        SleepSort.DATE ->
                                            "Date"
                                        SleepSort.SCORE ->
                                            "Score"
                                        SleepSort.DURATION ->
                                            "Duration"
                                    }
                                )
                            }
                        )
                    }
            }

            rows.forEach { night ->
                Card(
                    onClick = {
                        onNightSelected(night)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape =
                        RoundedCornerShape(18.dp),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant
                    ),
                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                MaterialTheme.colorScheme.surfaceVariant
                                    .copy(
                                        alpha = 0.38f
                                    ),
                            contentColor =
                                MaterialTheme.colorScheme.onSurface
                        )
                ) {
                    Row(
                        modifier =
                            Modifier.padding(
                                horizontal = 13.dp,
                                vertical = 11.dp
                            ),
                        horizontalArrangement =
                            Arrangement.SpaceBetween,
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text =
                                    night.date.format(
                                        DateTimeFormatter.ofPattern(
                                            "MMM d, EEE"
                                        )
                                    ),
                                fontWeight =
                                    FontWeight.Bold
                            )
                            Text(
                                modifier =
                                    Modifier.padding(
                                        top = 2.dp
                                    ),
                                text =
                                    formatMinutes(
                                        night.asleepMinutes
                                    ) +
                                        " · " +
                                        (
                                            night.efficiencyPercent
                                                ?.toString()
                                                ?: "—"
                                            ) +
                                        "%",
                                style =
                                    MaterialTheme.typography.bodySmall,
                                color =
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            Text(
                                text =
                                    night.score.toString(),
                                style =
                                    MaterialTheme.typography.titleLarge,
                                fontWeight =
                                    FontWeight.ExtraBold,
                                color =
                                    scoreColor(
                                        night.score
                                    )
                            )
                            Text(
                                modifier =
                                    Modifier.padding(
                                        start = 8.dp
                                    ),
                                text = "›",
                                color =
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportCard(
    onOpenPreview: () -> Unit,
    onInfo: () -> Unit
) {
    BentoCard {
        Column {
            CardTitleRow(
                title = "Export & share",
                onInfo = onInfo
            )

            Text(
                modifier = Modifier.padding(top = 6.dp),
                text =
                    "Preview exactly what will be included before anything leaves WakeSync.",
                style =
                    MaterialTheme.typography.bodySmall,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                onClick = onOpenPreview,
                shape = RoundedCornerShape(999.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            Color.White,
                        contentColor =
                            Color(0xFF0F172A)
                    )
            ) {
                Text(
                    text = "Export Data",
                    fontWeight =
                        FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun EmptyRangeCard(
    rangeStart: LocalDate,
    rangeEnd: LocalDate,
    onRefresh: () -> Unit,
    onInfo: () -> Unit
) {
    BentoCard {
        Column {
            CardTitleRow(
                title = "No sleep data",
                onInfo = onInfo
            )
            Text(
                modifier = Modifier.padding(top = 7.dp),
                text =
                    rangeStart.format(
                        DateTimeFormatter.ofPattern(
                            "MMM d, yyyy"
                        )
                    ) +
                        " – " +
                        rangeEnd.format(
                            DateTimeFormatter.ofPattern(
                                "MMM d, yyyy"
                            )
                        ),
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                modifier =
                    Modifier.padding(top = 14.dp),
                onClick = onRefresh,
                shape =
                    RoundedCornerShape(999.dp)
            ) {
                Text("Refresh")
            }
        }
    }
}

@Composable
private fun SleepSkeleton() {
    Column(
        verticalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {
        repeat(4) { index ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        if (index == 0) {
                            175.dp
                        } else {
                            125.dp
                        }
                    ),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant
                ),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.surface
                                .copy(
                                    alpha = 0.45f
                                )
                    )
            ) {}
        }
    }
}

@Composable
private fun BentoCard(
    borderColor: Color =
        MaterialTheme.colorScheme.outlineVariant,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(
            1.dp,
            borderColor
        ),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surface
                    .copy(alpha = 0.68f),
            contentColor =
                MaterialTheme.colorScheme.onSurface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(17.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun CardTitleRow(
    title: String,
    titleColor: Color =
        MaterialTheme.colorScheme.onSurface,
    onInfo: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween,
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style =
                MaterialTheme.typography.titleMedium,
            fontWeight =
                FontWeight.ExtraBold,
            color = titleColor
        )
        InfoTrigger(onClick = onInfo)
    }
}

@Composable
private fun LegendPill(
    label: String,
    color: Color
) {
    Card(
        shape = RoundedCornerShape(999.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                color.copy(alpha = 0.12f),
            contentColor = color
        )
    ) {
        Text(
            modifier = Modifier.padding(
                horizontal = 9.dp,
                vertical = 6.dp
            ),
            text = label,
            style =
                MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StatusPill(
    modifier: Modifier = Modifier,
    text: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(
            1.dp,
            color.copy(alpha = 0.28f)
        ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    color.copy(alpha = 0.12f),
                contentColor = color
            )
    ) {
        Text(
            modifier = Modifier.padding(
                horizontal = 9.dp,
                vertical = 5.dp
            ),
            text = text,
            style =
                MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MiniMetric(
    modifier: Modifier,
    label: String,
    value: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surfaceVariant
                    .copy(alpha = 0.42f),
            contentColor =
                MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Text(
                text = value,
                fontWeight =
                    FontWeight.ExtraBold
            )
            Text(
                text = label,
                style =
                    MaterialTheme.typography.labelSmall,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MetricLine(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style =
                MaterialTheme.typography.bodySmall,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style =
                MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color =
                MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScoreBreakdownSheet(
    analytics: PeriodAnalytics,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(
            topStart = 30.dp,
            topEnd = 30.dp
        ),
        containerColor =
            MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 22.dp,
                    vertical = 20.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Score Pillar Breakdown",
                style =
                    MaterialTheme.typography.headlineMedium,
                fontWeight =
                    FontWeight.ExtraBold
            )

            Text(
                text =
                    "Sleep Score = 40%(Duration) + 25%(Efficiency) + 20%(Stage Ratios) + 15%(Consistency)",
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            PillarRow(
                "Duration · 40%",
                analytics.scoreBreakdown.duration
            )
            PillarRow(
                "Efficiency · 25%",
                analytics.scoreBreakdown.efficiency
            )
            PillarRow(
                "Stage Ratios · 20%",
                analytics.scoreBreakdown.stageRatios
            )
            PillarRow(
                "Consistency · 15%",
                analytics.scoreBreakdown.consistency
            )

            Text(
                text =
                    "This is a WakeSync wellness score. It is not a validated medical or diagnostic score.",
                style =
                    MaterialTheme.typography.bodySmall,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            TextButton(
                modifier =
                    Modifier.align(Alignment.End),
                onClick = onDismiss
            ) {
                Text("Done")
            }
        }
    }
}

@Composable
private fun PillarRow(
    label: String,
    value: Int
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontWeight =
                    FontWeight.SemiBold
            )
            Text(
                text = value.toString(),
                fontWeight =
                    FontWeight.ExtraBold,
                color =
                    scoreColor(value)
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .padding(top = 4.dp)
        ) {
            drawRoundRect(
                color =
                    MaterialTheme.colorScheme.onSurface
                        .copy(alpha = 0.08f),
                size = size,
                cornerRadius =
                    CornerRadius(
                        size.height,
                        size.height
                    )
            )
            drawRoundRect(
                color =
                    scoreColor(value),
                size = Size(
                    size.width *
                        value.coerceIn(
                            0,
                            100
                        ) /
                        100f,
                    size.height
                ),
                cornerRadius =
                    CornerRadius(
                        size.height,
                        size.height
                    )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NightBreakdownSheet(
    night: NightAnalytics,
    targetSleepMinutes: Int,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(
            topStart = 30.dp,
            topEnd = 30.dp
        ),
        containerColor =
            MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 20.dp,
                    vertical = 18.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = night.date.format(
                    DateTimeFormatter.ofPattern(
                        "EEEE, MMM d"
                    )
                ),
                style =
                    MaterialTheme.typography.headlineMedium,
                fontWeight =
                    FontWeight.ExtraBold
            )

            Hypnogram(
                night = night.night,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                MiniMetric(
                    modifier = Modifier.weight(1f),
                    label = "Latency",
                    value =
                        night.onsetLatencyMinutes
                            ?.let(::formatMinutes)
                            ?: "—"
                )
                MiniMetric(
                    modifier = Modifier.weight(1f),
                    label = "WASO",
                    value =
                        formatMinutes(
                            night.wasoMinutes
                        )
                )
                MiniMetric(
                    modifier = Modifier.weight(1f),
                    label = "Debt impact",
                    value =
                        if (
                            night.asleepMinutes <
                            targetSleepMinutes
                        ) {
                            "-" +
                                formatMinutes(
                                    targetSleepMinutes
                                        .toLong() -
                                        night.asleepMinutes
                                )
                        } else {
                            "0m"
                        }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                MiniMetric(
                    modifier = Modifier.weight(1f),
                    label = "HRV",
                    value =
                        night.night.averageHrvMs
                            ?.roundToInt()
                            ?.toString()
                            ?.plus(" ms")
                            ?: "—"
                )
                MiniMetric(
                    modifier = Modifier.weight(1f),
                    label = "RHR",
                    value =
                        night.night.restingHeartRateBpm
                            ?.toString()
                            ?.plus(" bpm")
                            ?: "—"
                )
            }

            if (
                night.night.averageHrvMs != null ||
                night.night.restingHeartRateBpm != null
            ) {
                Text(
                    text =
                        "WakeSync currently receives nightly recovery values, not a full time-series trace, so it does not fabricate HRV/RHR overlay graphs.",
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(
                modifier =
                    Modifier.align(Alignment.End),
                onClick = onDismiss
            ) {
                Text("Done")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportPreviewSheet(
    analytics: PeriodAnalytics,
    onDismiss: () -> Unit,
    onPdf: () -> Unit,
    onCsv: () -> Unit,
    onStory: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(
            topStart = 30.dp,
            topEnd = 30.dp
        ),
        containerColor =
            MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 22.dp,
                    vertical = 20.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Data Transparency Preview",
                style =
                    MaterialTheme.typography.headlineMedium,
                fontWeight =
                    FontWeight.ExtraBold
            )

            TransparencySection(
                title = "Included in export",
                lines = listOf(
                    "Selected date range",
                    "WakeSync sleep scores",
                    "Total sleep duration",
                    "Stage percentages",
                    "Efficiency and timing summaries"
                ),
                accent = Mint
            )

            TransparencySection(
                title = "Excluded / private",
                lines = listOf(
                    "Personal identifiers",
                    "Raw sensor feeds",
                    "Location information",
                    "Other unrelated Health Connect records"
                ),
                accent =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                modifier =
                    Modifier.fillMaxWidth(),
                onClick = onPdf,
                shape =
                    RoundedCornerShape(999.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            Color.White,
                        contentColor =
                            Color(0xFF0F172A)
                    )
            ) {
                Text(
                    "📄 Export PDF Health Report",
                    fontWeight =
                        FontWeight.Bold
                )
            }

            Button(
                modifier =
                    Modifier.fillMaxWidth(),
                onClick = onCsv,
                shape =
                    RoundedCornerShape(999.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            Lavender.copy(alpha = 0.20f),
                        contentColor =
                            Color.White
                    )
            ) {
                Text(
                    "📊 Export CSV",
                    fontWeight =
                        FontWeight.Bold
                )
            }

            Button(
                modifier =
                    Modifier.fillMaxWidth(),
                onClick = onStory,
                shape =
                    RoundedCornerShape(999.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            Cyan.copy(alpha = 0.18f),
                        contentColor =
                            Color.White
                    )
            ) {
                Text(
                    "🎨 Share Story Card",
                    fontWeight =
                        FontWeight.Bold
                )
            }

            Text(
                text =
                    analytics.nights.size.toString() +
                        " nights are currently in the selected period.",
                style =
                    MaterialTheme.typography.bodySmall,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TransparencySection(
    title: String,
    lines: List<String>,
    accent: Color
) {
    Column {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            color = accent
        )
        lines.forEach { line ->
            Text(
                modifier =
                    Modifier.padding(top = 4.dp),
                text = "• " + line,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomRangeSheet(
    initialStart: LocalDate,
    initialEnd: LocalDate,
    onDismiss: () -> Unit,
    onApply: (
        LocalDate,
        LocalDate
    ) -> Unit
) {
    val state =
        rememberDateRangePickerState(
            initialSelectedStartDateMillis =
                initialStart
                    .atStartOfDay(
                        ZoneOffset.UTC
                    )
                    .toInstant()
                    .toEpochMilli(),
            initialSelectedEndDateMillis =
                initialEnd
                    .atStartOfDay(
                        ZoneOffset.UTC
                    )
                    .toInstant()
                    .toEpochMilli()
        )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(
            topStart = 30.dp,
            topEnd = 30.dp
        ),
        containerColor =
            MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 14.dp,
                    vertical = 12.dp
                )
        ) {
            Text(
                modifier =
                    Modifier.padding(
                        horizontal = 8.dp,
                        bottom = 8.dp
                    ),
                text = "Custom sleep range",
                style =
                    MaterialTheme.typography.headlineMedium,
                fontWeight =
                    FontWeight.ExtraBold
            )

            DateRangePicker(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp),
                showModeToggle = false,
                title = null,
                headline = null
            )

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 8.dp,
                        vertical = 10.dp
                    ),
                enabled =
                    state.selectedStartDateMillis != null &&
                        state.selectedEndDateMillis != null,
                onClick = {
                    val start =
                        Instant.ofEpochMilli(
                            state.selectedStartDateMillis!!
                        )
                            .atZone(
                                ZoneOffset.UTC
                            )
                            .toLocalDate()
                    val end =
                        Instant.ofEpochMilli(
                            state.selectedEndDateMillis!!
                        )
                            .atZone(
                                ZoneOffset.UTC
                            )
                            .toLocalDate()

                    onApply(
                        start,
                        end
                    )
                },
                shape =
                    RoundedCornerShape(999.dp)
            ) {
                Text(
                    "Apply range",
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    }
}

private fun sortedNights(
    nights: List<NightAnalytics>,
    sort: SleepSort
): List<NightAnalytics> =
    when (sort) {
        SleepSort.DATE ->
            nights.sortedByDescending {
                it.date
            }
        SleepSort.SCORE ->
            nights.sortedByDescending {
                it.score
            }
        SleepSort.DURATION ->
            nights.sortedByDescending {
                it.asleepMinutes
            }
    }

private fun scoreColor(
    score: Int
): Color =
    when {
        score >= 90 ->
            SleepScoreGreen
        score >= 75 ->
            SleepScoreCyan
        score >= 60 ->
            SleepScoreAmber
        else ->
            SleepScoreCoral
    }

private fun scoreStatus(
    score: Int
): String =
    when {
        score >= 90 ->
            "Optimal recovery"
        score >= 75 ->
            "Strong recovery"
        score >= 60 ->
            "Sleep debt detected"
        else ->
            "Recovery needs attention"
    }

private fun formatMinutes(
    minutes: Long
): String {
    if (minutes <= 0) {
        return "0m"
    }

    val hours =
        minutes / 60
    val remainder =
        minutes % 60

    return if (hours > 0) {
        hours.toString() +
            "h " +
            remainder.toString() +
            "m"
    } else {
        remainder.toString() +
            "m"
    }
}

private fun formatInstant(
    instant: Instant,
    pattern: String
): String =
    instant
        .atZone(
            ZoneId.systemDefault()
        )
        .format(
            DateTimeFormatter.ofPattern(
                pattern
            )
        )
