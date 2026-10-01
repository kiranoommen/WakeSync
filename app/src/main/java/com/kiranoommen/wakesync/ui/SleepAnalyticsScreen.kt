package com.kiranoommen.wakesync.ui

import android.app.DatePickerDialog
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kiranoommen.wakesync.domain.NightAnalytics
import com.kiranoommen.wakesync.domain.PeriodAnalytics
import com.kiranoommen.wakesync.domain.SleepAnalytics
import com.kiranoommen.wakesync.model.SleepNight
import com.kiranoommen.wakesync.model.SleepStageType
import com.kiranoommen.wakesync.ui.theme.Amber
import com.kiranoommen.wakesync.ui.theme.Indigo
import com.kiranoommen.wakesync.ui.theme.Lavender
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private enum class SleepRange(val label: String, val days: Long?) {
    DAYS_7("7 Days", 7),
    DAYS_14("14 Days", 14),
    DAYS_30("30 Days", 30),
    MONTHS_6("6 Months", 183),
    CUSTOM("Custom", null)
}

private enum class SleepSort { DATE, SCORE, DURATION }

@Composable
fun SleepAnalyticsScreen(
    nights: List<SleepNight>,
    loading: Boolean,
    targetSleepMinutes: Int,
    onRefresh: () -> Unit,
    onShareCsv: (List<NightAnalytics>) -> Unit,
    onSharePdf: (PeriodAnalytics) -> Unit
) {
    val context = LocalContext.current
    var range by remember { mutableStateOf(SleepRange.DAYS_14) }
    var compare by remember { mutableStateOf(false) }
    var sort by remember { mutableStateOf(SleepSort.DATE) }
    var customStart by remember { mutableStateOf<LocalDate?>(null) }
    var customEnd by remember { mutableStateOf<LocalDate?>(null) }
    var expandedDate by remember { mutableStateOf<LocalDate?>(null) }

    val today = LocalDate.now()
    val rangeEnd = if (range == SleepRange.CUSTOM) customEnd ?: today else today
    val rangeStart = when {
        range == SleepRange.CUSTOM -> customStart ?: today.minusDays(13)
        range.days != null -> rangeEnd.minusDays(range.days!! - 1)
        else -> rangeEnd.minusDays(13)
    }

    val currentNights = remember(nights, rangeStart, rangeEnd) {
        nights.filter { night ->
            val date = night.end.atZone(ZoneId.systemDefault()).toLocalDate()
            !date.isBefore(rangeStart) && !date.isAfter(rangeEnd)
        }
    }

    val analytics = remember(currentNights, targetSleepMinutes) {
        SleepAnalytics.analyze(currentNights, targetSleepMinutes)
    }

    val previousAnalytics = remember(nights, rangeStart, rangeEnd, compare, targetSleepMinutes) {
        if (!compare) null else {
            val span = java.time.temporal.ChronoUnit.DAYS.between(rangeStart, rangeEnd) + 1
            val previousEnd = rangeStart.minusDays(1)
            val previousStart = previousEnd.minusDays(span - 1)
            val previousNights = nights.filter { night ->
                val date = night.end.atZone(ZoneId.systemDefault()).toLocalDate()
                !date.isBefore(previousStart) && !date.isAfter(previousEnd)
            }
            SleepAnalytics.analyze(previousNights, targetSleepMinutes)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        FilterBar(
            selected = range,
            compare = compare,
            rangeStart = rangeStart,
            rangeEnd = rangeEnd,
            onSelected = { selected ->
                if (selected == SleepRange.CUSTOM) {
                    val seed = customStart ?: today.minusDays(13)
                    DatePickerDialog(
                        context,
                        { _, y, m, d ->
                            val start = LocalDate.of(y, m + 1, d)
                            DatePickerDialog(
                                context,
                                { _, y2, m2, d2 ->
                                    customStart = start
                                    customEnd = LocalDate.of(y2, m2 + 1, d2)
                                    range = SleepRange.CUSTOM
                                },
                                today.year,
                                today.monthValue - 1,
                                today.dayOfMonth
                            ).show()
                        },
                        seed.year,
                        seed.monthValue - 1,
                        seed.dayOfMonth
                    ).show()
                } else range = selected
            },
            onCompareChanged = { compare = it }
        )

        if (loading) {
            Skeleton()
            return@Column
        }

        if (analytics.nights.isEmpty()) {
            EmptyRange(rangeStart, rangeEnd, onRefresh)
            return@Column
        }

        ScoreHero(analytics, previousAnalytics, targetSleepMinutes)

        InsightCard(SleepAnalytics.insightFor(analytics, targetSleepMinutes))

        analytics.nights.firstOrNull()?.let { StageDistributionCard(it) }

        TrendCard(
            nights = analytics.nights.sortedBy { it.date },
            previous = previousAnalytics?.nights?.sortedBy { it.date }
        )

        ArchitectureCard(analytics)

        if (analytics.averageHrvMs != null || analytics.averageRestingHeartRateBpm != null) {
            RecoveryCard(analytics)
        } else {
            MissingRecoveryCard()
        }

        SleepLog(
            analytics = analytics,
            sort = sort,
            expandedDate = expandedDate,
            onSort = { sort = it },
            onToggleExpanded = {
                expandedDate = if (expandedDate == it) null else it
            }
        )

        ExportCard(
            onCsv = { onShareCsv(sortedNights(analytics.nights, sort)) },
            onPdf = { onSharePdf(analytics) }
        )

        ResearchNote()
    }
}

@Composable
private fun FilterBar(
    selected: SleepRange,
    compare: Boolean,
    rangeStart: LocalDate,
    rangeEnd: LocalDate,
    onSelected: (SleepRange) -> Unit,
    onCompareChanged: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                SleepRange.values().forEach { item ->
                    FilterChip(
                        selected = selected == item,
                        onClick = { onSelected(item) },
                        label = { Text(item.label) }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = rangeStart.format(DateTimeFormatter.ofPattern("MMM d")) +
                            " – " + rangeEnd.format(DateTimeFormatter.ofPattern("MMM d")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Compare with previous period",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(checked = compare, onCheckedChange = onCompareChanged)
            }
        }
    }
}

@Composable
private fun ScoreHero(
    analytics: PeriodAnalytics,
    previous: PeriodAnalytics?,
    targetSleepMinutes: Int
) {
    val score = analytics.averageScore ?: 0
    val delta = previous?.averageScore?.let { score - it }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        )
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScoreRing(
                score = score,
                modifier = Modifier.width(126.dp).height(126.dp)
            )

            Column(modifier = Modifier.weight(1f).padding(start = 18.dp)) {
                Text(
                    text = "Sleep performance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = when {
                        score >= 90 -> "Excellent"
                        score >= 80 -> "Strong"
                        score >= 70 -> "Fair"
                        else -> "Needs attention"
                    },
                    color = Amber,
                    fontWeight = FontWeight.SemiBold
                )

                if (delta != null && previous.nights.isNotEmpty()) {
                    Text(
                        modifier = Modifier.padding(top = 4.dp),
                        text = (if (delta >= 0) "+" else "") + delta + " vs previous period",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(12.dp))
                MetricLine("Avg sleep", formatMinutes(analytics.averageSleepMinutes))
                MetricLine(
                    "Efficiency",
                    analytics.averageEfficiencyPercent?.let { it.toString() + "%" } ?: "—"
                )
                MetricLine(
                    "Sleep debt",
                    if (analytics.sleepDebtMinutes > 0) {
                        "-" + formatMinutes(analytics.sleepDebtMinutes)
                    } else "0m"
                )
                MetricLine("Target", formatMinutes(targetSleepMinutes.toLong()))
            }
        }
    }
}

@Composable
private fun ScoreRing(score: Int, modifier: Modifier) {
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val animatedScore by animateFloatAsState(
        targetValue = score.coerceIn(0, 100).toFloat(),
        label = "sleepScore"
    )

    Box(
        modifier = modifier.semantics {
            contentDescription = "Sleep performance score " + score + " out of 100"
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(126.dp)) {
            val stroke = 11.dp.toPx()
            val diameter = size.minDimension - stroke
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
                size = Size(diameter, diameter),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = Amber,
                startAngle = -90f,
                sweepAngle = 360f * animatedScore / 100f,
                useCenter = false,
                topLeft = origin,
                size = Size(diameter, diameter),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = score.toString(),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "/ 100",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InsightCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Lavender.copy(alpha = 0.10f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("WakeSync insight", fontWeight = FontWeight.Bold, color = Lavender)
            Text(modifier = Modifier.padding(top = 7.dp), text = text)
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = "Generated locally from your trend data.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StageDistributionCard(night: NightAnalytics) {
    val deepMinutes = stageMinutes(night.night, SleepStageType.DEEP)
    val lightMinutes = stageMinutes(night.night, SleepStageType.LIGHT)
    val remMinutes = stageMinutes(night.night, SleepStageType.REM)
    val awakeMinutes = stageMinutes(night.night, SleepStageType.AWAKE)
    val stageTotal = (deepMinutes + lightMinutes + remMinutes + awakeMinutes).coerceAtLeast(1L)

    fun pct(value: Long): Int =
        (value.toDouble() / stageTotal.toDouble() * 100.0)
            .roundToInt()
            .coerceIn(0, 100)

    val deepPct = pct(deepMinutes)
    val lightPct = pct(lightMinutes)
    val remPct = pct(remMinutes)
    val awakePct = pct(awakeMinutes)
    val awakeColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Last night · Sleep stages",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                modifier = Modifier.padding(top = 3.dp),
                text = night.date.format(DateTimeFormatter.ofPattern("EEE, MMM d")),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Canvas(
                modifier = Modifier.fillMaxWidth().height(44.dp).padding(top = 14.dp)
            ) {
                val y = 4.dp.toPx()
                val h = 18.dp.toPx()
                var x = 0f

                fun add(percent: Int, color: Color) {
                    val w = size.width * percent.toFloat() / 100f
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, y),
                        size = Size(w.coerceAtLeast(1f), h),
                        cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
                    )
                    x += w
                }

                add(deepPct, Indigo)
                add(lightPct, Lavender)
                add(remPct, Amber)
                add(awakePct, awakeColor)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StageLegend("Deep", deepPct, Indigo)
                StageLegend("Light", lightPct, Lavender)
                StageLegend("REM", remPct, Amber)
                StageLegend("Awake", awakePct, awakeColor)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniMetric(
                    Modifier.weight(1f),
                    "Latency",
                    night.onsetLatencyMinutes?.let { formatMinutes(it) } ?: "—"
                )
                MiniMetric(Modifier.weight(1f), "WASO", formatMinutes(night.wasoMinutes))
                MiniMetric(
                    Modifier.weight(1f),
                    "Efficiency",
                    night.efficiencyPercent?.let { it.toString() + "%" } ?: "—"
                )
            }
        }
    }
}

@Composable
private fun StageLegend(label: String, value: Int, color: Color) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(text = value.toString() + "%", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun TrendCard(
    nights: List<NightAnalytics>,
    previous: List<NightAnalytics>?
) {
    var selectedIndex by remember(nights) {
        mutableIntStateOf((nights.size - 1).coerceAtLeast(0))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Duration & efficiency",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                modifier = Modifier.padding(top = 3.dp),
                text = "Tap a day to inspect it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (nights.isNotEmpty()) {
                TrendChart(
                    nights = nights,
                    previous = previous,
                    selectedIndex = selectedIndex,
                    onSelect = { selectedIndex = it }
                )

                val selected = nights[selectedIndex.coerceIn(0, nights.lastIndex)]
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = selected.date.format(DateTimeFormatter.ofPattern("EEE, MMM d")),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = formatMinutes(selected.asleepMinutes) + " · " +
                            (selected.efficiencyPercent?.let { it.toString() + "%" } ?: "—"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (previous != null && previous.isNotEmpty() && nights.isNotEmpty()) {
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = "Muted bars show the previous equivalent period.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val currentAvg = nights.map { it.asleepMinutes }.average().roundToInt()
                val previousAvg = previous.map { it.asleepMinutes }.average().roundToInt()
                val delta = currentAvg - previousAvg

                Text(
                    modifier = Modifier.padding(top = 10.dp),
                    text = "Previous period · " +
                        (if (delta >= 0) "+" else "-") +
                        formatMinutes(kotlin.math.abs(delta).toLong()) +
                        " average sleep",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
    val barColor = Lavender
    val lineColor = Amber
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val selectionColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    val previousBarColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .padding(top = 16.dp)
            .semantics {
                contentDescription =
                    "Sleep duration bars and sleep efficiency line for the selected period. Tap a day for details."
            }
            .pointerInput(nights) {
                detectTapGestures { offset ->
                    if (nights.isNotEmpty()) {
                        val widthPer = size.width.toFloat() / nights.size.toFloat()
                        onSelect(
                            (offset.x / widthPer)
                                .toInt()
                                .coerceIn(0, nights.lastIndex)
                        )
                    }
                }
            }
    ) {
        if (nights.isEmpty()) return@Canvas

        val maxMinutes = maxOf(540L, nights.maxOf { it.asleepMinutes }.coerceAtLeast(1L))
        val widthPer = size.width / nights.size.toFloat()
        val barWidth = widthPer * 0.48f

        repeat(4) { index ->
            val y = size.height * (index + 1) / 5f
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        previous
            ?.take(nights.size)
            ?.forEachIndexed { index, night ->
                val centerX = widthPer * index + widthPer / 2f
                val previousHeight =
                    night.asleepMinutes.toFloat() / maxMinutes.toFloat() * size.height * 0.82f

                drawRoundRect(
                    color = previousBarColor,
                    topLeft = Offset(
                        centerX - barWidth * 0.68f,
                        size.height - previousHeight
                    ),
                    size = Size(barWidth * 1.36f, previousHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }

        nights.forEachIndexed { index, night ->
            val centerX = widthPer * index + widthPer / 2f

            if (index == selectedIndex) {
                drawRoundRect(
                    color = selectionColor,
                    topLeft = Offset(widthPer * index, 0f),
                    size = Size(widthPer, size.height),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )
            }

            val barHeight =
                night.asleepMinutes.toFloat() / maxMinutes.toFloat() * size.height * 0.82f

            drawRoundRect(
                color = barColor.copy(alpha = 0.68f),
                topLeft = Offset(centerX - barWidth / 2f, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )
        }

        var previousPoint: Offset? = null

        nights.forEachIndexed { index, night ->
            val centerX = widthPer * index + widthPer / 2f
            val efficiency = (night.efficiencyPercent ?: 0).coerceIn(0, 100)
            val y = size.height - efficiency / 100f * size.height * 0.82f
            val point = Offset(centerX, y)

            previousPoint?.let {
                drawLine(
                    color = lineColor,
                    start = it,
                    end = point,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            drawCircle(
                color = lineColor,
                radius = if (index == selectedIndex) 5.dp.toPx() else 3.dp.toPx(),
                center = point
            )
            previousPoint = point
        }
    }
}

@Composable
private fun ArchitectureCard(analytics: PeriodAnalytics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Architecture & regularity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            MetricLine(
                "Deep sleep",
                analytics.averageDeepPercent?.let { it.toString() + "% avg" } ?: "—"
            )
            MetricLine(
                "REM sleep",
                analytics.averageRemPercent?.let { it.toString() + "% avg" } ?: "—"
            )
            MetricLine(
                "Sleep Regularity Index",
                analytics.regularityScore?.let { it.toString() + " / 100" } ?: "Learning"
            )
            Text(
                modifier = Modifier.padding(top = 10.dp),
                text = "Stage percentages are trend signals, not clinical measurements. Wearable sleep staging can differ from polysomnography.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RecoveryCard(analytics: PeriodAnalytics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Indigo.copy(alpha = 0.10f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Biometric recovery",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            MetricLine(
                "HRV",
                analytics.averageHrvMs?.let { it.roundToInt().toString() + " ms avg" } ?: "—"
            )
            MetricLine(
                "Resting heart rate",
                analytics.averageRestingHeartRateBpm?.let {
                    it.roundToInt().toString() + " bpm avg"
                } ?: "—"
            )
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = "HRV is most useful relative to your own baseline rather than a universal target.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MissingRecoveryCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Biometric recovery",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                modifier = Modifier.padding(top = 6.dp),
                text = "HRV and resting-heart-rate data are not available yet. These appear only when Health Connect provides them and you grant that access.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SleepLog(
    analytics: PeriodAnalytics,
    sort: SleepSort,
    expandedDate: LocalDate?,
    onSort: (SleepSort) -> Unit,
    onToggleExpanded: (LocalDate) -> Unit
) {
    val rows = sortedNights(analytics.nights, sort)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Sleep log",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SleepSort.values().forEach { item ->
                    FilterChip(
                        selected = sort == item,
                        onClick = { onSort(item) },
                        label = {
                            Text(
                                when (item) {
                                    SleepSort.DATE -> "Date"
                                    SleepSort.SCORE -> "Score"
                                    SleepSort.DURATION -> "Duration"
                                }
                            )
                        }
                    )
                }
            }

            rows.forEach { night ->
                val expanded = expandedDate == night.date
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onToggleExpanded(night.date) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = night.date.format(DateTimeFormatter.ofPattern("MMM d, EEE")),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = formatMinutes(night.asleepMinutes) + " · " +
                                    (night.efficiencyPercent?.let { it.toString() + "%" } ?: "—"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = night.score.toString(),
                                fontWeight = FontWeight.Bold,
                                color = Amber
                            )
                            Text(
                                text = night.scoreLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (expanded) ExpandedDetails(night)
            }
        }
    }
}

@Composable
private fun ExpandedDetails(night: NightAnalytics) {
    val zone = ZoneId.systemDefault()
    val start = night.night.start.atZone(zone)
    val end = night.night.end.atZone(zone)

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f)
        )
    ) {
        Column(modifier = Modifier.padding(13.dp)) {
            MetricLine(
                "Bed / wake",
                start.format(DateTimeFormatter.ofPattern("h:mm a")) +
                    " – " + end.format(DateTimeFormatter.ofPattern("h:mm a"))
            )
            MetricLine(
                "Deep / REM",
                night.deepPercent.toString() + "% / " + night.remPercent.toString() + "%"
            )
            MetricLine("WASO", formatMinutes(night.wasoMinutes))
            MetricLine(
                "Latency",
                night.onsetLatencyMinutes?.let { formatMinutes(it) } ?: "—"
            )
            MetricLine(
                "HRV / RHR",
                (night.night.averageHrvMs?.let { it.roundToInt().toString() + " ms" } ?: "—") +
                    " / " +
                    (night.night.restingHeartRateBpm?.let { it.toString() + " bpm" } ?: "—")
            )
        }
    }
}

@Composable
private fun ExportCard(onCsv: () -> Unit, onPdf: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Export & share",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                modifier = Modifier.padding(top = 5.dp),
                text = "Files are generated locally only when you choose to export.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onCsv) { Text("Share CSV") }
                Button(
                    onClick = onPdf,
                    colors = ButtonDefaults.buttonColors(containerColor = Lavender)
                ) { Text("Share PDF") }
            }
        }
    }
}

@Composable
private fun ResearchNote() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("How to read these metrics", fontWeight = FontWeight.Bold)
            Text(
                modifier = Modifier.padding(top = 6.dp),
                text = "WakeSync treats 7–9 hours as a broad adult sleep-duration target. Efficiency, latency, stage mix, SRI, HRV, and heart-rate values are best interpreted as personal trends. The score is a WakeSync wellness summary, not a diagnosis.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyRange(
    rangeStart: LocalDate,
    rangeEnd: LocalDate,
    onRefresh: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "No sleep data in this range",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                modifier = Modifier.padding(top = 6.dp),
                text = rangeStart.format(DateTimeFormatter.ofPattern("MMM d, yyyy")) +
                    " – " + rangeEnd.format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                modifier = Modifier.padding(top = 6.dp),
                text = "Health Connect requires separate historical-data access for records older than 30 days.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(modifier = Modifier.padding(top = 12.dp), onClick = onRefresh) {
                Text("Refresh")
            }
        }
    }
}

@Composable
private fun Skeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(3) { index ->
            Card(
                modifier = Modifier.fillMaxWidth().height(if (index == 0) 170.dp else 120.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.56f)
                )
            ) {}
        }
    }
}

@Composable
private fun MetricLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MiniMetric(modifier: Modifier, label: String, value: String) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = value, fontWeight = FontWeight.Bold)
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun stageMinutes(
    night: SleepNight,
    type: SleepStageType
): Long =
    night.stages
        .asSequence()
        .filter { it.type == type }
        .sumOf {
            java.time.Duration.between(it.start, it.end)
                .toMinutes()
                .coerceAtLeast(0)
        }

private fun sortedNights(
    nights: List<NightAnalytics>,
    sort: SleepSort
): List<NightAnalytics> =
    when (sort) {
        SleepSort.DATE -> nights.sortedByDescending { it.date }
        SleepSort.SCORE -> nights.sortedByDescending { it.score }
        SleepSort.DURATION -> nights.sortedByDescending { it.asleepMinutes }
    }

private fun formatMinutes(minutes: Long): String {
    if (minutes <= 0) return "0m"
    val hours = minutes / 60
    val remainder = minutes % 60
    return if (hours > 0) hours.toString() + "h " + remainder.toString() + "m"
    else remainder.toString() + "m"
}
