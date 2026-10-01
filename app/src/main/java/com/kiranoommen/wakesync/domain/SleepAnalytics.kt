package com.kiranoommen.wakesync.domain

import com.kiranoommen.wakesync.model.SleepNight
import com.kiranoommen.wakesync.model.SleepStageType
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

data class NightAnalytics(
    val night: SleepNight,
    val date: LocalDate,
    val totalMinutes: Long,
    val asleepMinutes: Long,
    val awakeMinutes: Long,
    val efficiencyPercent: Int?,
    val onsetLatencyMinutes: Long?,
    val wasoMinutes: Long,
    val deepPercent: Int,
    val remPercent: Int,
    val lightPercent: Int,
    val bedtimeMinute: Int,
    val wakeMinute: Int,
    val score: Int,
    val scoreLabel: String
)

data class ScoreBreakdown(
    val duration: Int,
    val efficiency: Int,
    val stageRatios: Int,
    val consistency: Int
)

data class PeriodAnalytics(
    val nights: List<NightAnalytics>,
    val averageSleepMinutes: Long,
    val averageEfficiencyPercent: Int?,
    val sleepDebtMinutes: Long,
    val regularityScore: Int?,
    val averageScore: Int?,
    val averageDeepPercent: Int?,
    val averageRemPercent: Int?,
    val averageHrvMs: Double?,
    val averageRestingHeartRateBpm: Double?,
    val scoreBreakdown: ScoreBreakdown
)

object SleepAnalytics {

    fun analyze(
        nights: List<SleepNight>,
        targetSleepMinutes: Int = 480
    ): PeriodAnalytics {
        val base = nights
            .sortedByDescending { it.end }
            .map { analyzeNight(it) }

        val regularity = sleepRegularityIndex(nights)
        val consistencyScore = (regularity ?: 50).coerceIn(0, 100)

        val scored = base.map { night ->
            val duration = durationScore(night.asleepMinutes)
            val efficiency = night.efficiencyPercent
                ?.let { efficiencyScore(it) }
                ?: 50.0
            val stages = architectureScore(
                night.deepPercent,
                night.remPercent
            )
            val overall = (
                duration * 0.40 +
                    efficiency * 0.25 +
                    stages * 0.20 +
                    consistencyScore * 0.15
                ).roundToInt().coerceIn(0, 100)

            night.copy(
                score = overall,
                scoreLabel = scoreLabel(overall)
            )
        }

        val avgSleep = scored
            .map { it.asleepMinutes }
            .averageLongOrNull()
            ?.roundToInt()
            ?.toLong()
            ?: 0L
        val avgEfficiency = scored
            .mapNotNull { it.efficiencyPercent }
            .averageIntOrNull()
            ?.roundToInt()
        val debt = scored.sumOf {
            max(0L, targetSleepMinutes.toLong() - it.asleepMinutes)
        }
        val avgDeep = scored
            .map { it.deepPercent }
            .averageIntOrNull()
            ?.roundToInt()
        val avgRem = scored
            .map { it.remPercent }
            .averageIntOrNull()
            ?.roundToInt()

        val durationSubscore = scored
            .map { durationScore(it.asleepMinutes) }
            .averageDoubleOrNull()
            ?.roundToInt()
            ?: 0
        val efficiencySubscore = scored
            .map {
                it.efficiencyPercent
                    ?.let(::efficiencyScore)
                    ?: 50.0
            }
            .averageDoubleOrNull()
            ?.roundToInt()
            ?: 0
        val stageSubscore = scored
            .map {
                architectureScore(
                    it.deepPercent,
                    it.remPercent
                )
            }
            .averageDoubleOrNull()
            ?.roundToInt()
            ?: 0

        val breakdown = ScoreBreakdown(
            duration = durationSubscore.coerceIn(0, 100),
            efficiency = efficiencySubscore.coerceIn(0, 100),
            stageRatios = stageSubscore.coerceIn(0, 100),
            consistency = consistencyScore
        )

        val avgScore = (
            breakdown.duration * 0.40 +
                breakdown.efficiency * 0.25 +
                breakdown.stageRatios * 0.20 +
                breakdown.consistency * 0.15
            ).roundToInt().coerceIn(0, 100)

        return PeriodAnalytics(
            nights = scored,
            averageSleepMinutes = avgSleep,
            averageEfficiencyPercent = avgEfficiency,
            sleepDebtMinutes = debt,
            regularityScore = regularity,
            averageScore = avgScore,
            averageDeepPercent = avgDeep,
            averageRemPercent = avgRem,
            averageHrvMs = nights
                .mapNotNull { it.averageHrvMs }
                .averageDoubleOrNull(),
            averageRestingHeartRateBpm = nights
                .mapNotNull { it.restingHeartRateBpm?.toDouble() }
                .averageDoubleOrNull(),
            scoreBreakdown = breakdown
        )
    }

    fun analyzeNight(night: SleepNight): NightAnalytics {
        val zone = ZoneId.systemDefault()
        val ordered = night.stages.sortedBy { it.start }
        val totalMinutes = Duration.between(night.start, night.end).toMinutes().coerceAtLeast(0)

        val deep = stageMinutes(night, SleepStageType.DEEP)
        val light = stageMinutes(night, SleepStageType.LIGHT)
        val rem = stageMinutes(night, SleepStageType.REM)
        val awake = stageMinutes(night, SleepStageType.AWAKE)
        val unknown = stageMinutes(night, SleepStageType.UNKNOWN)

        val asleep = deep + light + rem + unknown
        val stagedTotal = (deep + light + rem).coerceAtLeast(1)

        val firstAsleep = ordered.firstOrNull { it.type != SleepStageType.AWAKE }?.start
        val lastAsleep = ordered.lastOrNull { it.type != SleepStageType.AWAKE }?.end

        val latency = firstAsleep?.let {
            Duration.between(night.start, it).toMinutes().coerceAtLeast(0)
        }

        val waso = if (firstAsleep != null && lastAsleep != null) {
            ordered
                .filter {
                    it.type == SleepStageType.AWAKE &&
                        !it.end.isBefore(firstAsleep) &&
                        !it.start.isAfter(lastAsleep)
                }
                .sumOf { Duration.between(it.start, it.end).toMinutes().coerceAtLeast(0) }
        } else {
            awake
        }

        val denominator = when {
            totalMinutes > 0 -> totalMinutes
            asleep + awake > 0 -> asleep + awake
            else -> 0
        }

        val efficiency = if (denominator > 0) {
            ((asleep.toDouble() / denominator.toDouble()) * 100.0)
                .roundToInt()
                .coerceIn(0, 100)
        } else null

        val deepPct = ((deep.toDouble() / stagedTotal.toDouble()) * 100.0)
            .roundToInt().coerceIn(0, 100)
        val lightPct = ((light.toDouble() / stagedTotal.toDouble()) * 100.0)
            .roundToInt().coerceIn(0, 100)
        val remPct = ((rem.toDouble() / stagedTotal.toDouble()) * 100.0)
            .roundToInt().coerceIn(0, 100)

        val bedtime = night.start.atZone(zone)
        val wake = night.end.atZone(zone)

        val durationScore = durationScore(asleep)
        val continuityScore = efficiency?.let { efficiencyScore(it) } ?: 50.0
        val architectureScore = architectureScore(deepPct, remPct)

        val score = (
            durationScore * 0.40 +
                continuityScore * 0.25 +
                architectureScore * 0.20 +
                50.0 * 0.15
            ).roundToInt().coerceIn(0, 100)

        return NightAnalytics(
            night = night,
            date = wake.toLocalDate(),
            totalMinutes = totalMinutes,
            asleepMinutes = asleep,
            awakeMinutes = awake,
            efficiencyPercent = efficiency,
            onsetLatencyMinutes = latency,
            wasoMinutes = waso,
            deepPercent = deepPct,
            remPercent = remPct,
            lightPercent = lightPct,
            bedtimeMinute = bedtime.hour * 60 + bedtime.minute,
            wakeMinute = wake.hour * 60 + wake.minute,
            score = score,
            scoreLabel = scoreLabel(score)
        )
    }

    fun sleepRegularityIndex(nights: List<SleepNight>): Int? {
        if (nights.size < 3) return null

        val zone = ZoneId.systemDefault()
        val sessions = nights.sortedBy { it.start }
        val dates = sessions
            .map { it.end.atZone(zone).toLocalDate() }
            .distinct()
            .sorted()

        if (dates.size < 2) return null

        var matches = 0
        var comparisons = 0

        for (index in 0 until dates.lastIndex) {
            val dayA = dates[index]
            val dayB = dates[index + 1]

            // Missing tracker days should not be treated as 24 hours awake.
            if (java.time.temporal.ChronoUnit.DAYS.between(dayA, dayB) != 1L) {
                continue
            }

            for (slot in 0 until 96) {
                val minute = slot * 15
                val a = isAsleepAt(dayA, minute, sessions, zone)
                val b = isAsleepAt(dayB, minute, sessions, zone)
                if (a == b) matches++
                comparisons++
            }
        }

        if (comparisons == 0) return null

        val rawAgreement = matches.toDouble() / comparisons.toDouble()
        val sri = ((rawAgreement - 0.5) * 200.0).roundToInt()
        return sri.coerceIn(0, 100)
    }

    fun insightFor(
        analytics: PeriodAnalytics,
        targetSleepMinutes: Int
    ): String {
        val nights = analytics.nights
        if (nights.size < 3) {
            return "Keep wearing your tracker. WakeSync needs a few more nights before it can show a reliable trend."
        }

        val avg = analytics.averageSleepMinutes
        val target = targetSleepMinutes.toLong()

        if (avg < target - 45) {
            return "Your recent average is ${formatMinutes(avg)}, about ${formatMinutes(target - avg)} below your ${formatMinutes(target)} target. Preserving sleep should take priority over waking early in the smart window."
        }

        val regularity = analytics.regularityScore
        if (regularity != null && regularity < 70) {
            return "Your sleep timing has been fairly irregular. A steadier bedtime and wake time may improve how predictable your morning wake window feels."
        }

        val early = nights.filter { it.bedtimeMinute <= 23 * 60 }
        val late = nights.filter { it.bedtimeMinute > 23 * 60 }

        if (early.size >= 2 && late.size >= 2) {
            val earlyDeep = early.map { it.deepPercent }.average()
            val lateDeep = late.map { it.deepPercent }.average()
            val difference = (earlyDeep - lateDeep).roundToInt()

            if (difference >= 5) {
                return "In your recent data, nights starting before 11 PM averaged about $difference percentage points more deep sleep than later nights. Treat this as a personal trend, not a clinical rule."
            }
        }

        return "Your recent sleep pattern is relatively stable. WakeSync will keep favoring later wake points unless an earlier point looks meaningfully better."
    }

    private fun scoreLabel(score: Int): String =
        when {
            score >= 90 -> "Excellent"
            score >= 75 -> "Good"
            score >= 60 -> "Fair"
            else -> "Low"
        }

    private fun isAsleepAt(
        date: LocalDate,
        minuteOfDay: Int,
        nights: List<SleepNight>,
        zone: ZoneId
    ): Boolean {
        val point = date
            .atStartOfDay(zone)
            .plusMinutes(minuteOfDay.toLong())
            .toInstant()

        return nights.any { night ->
            !point.isBefore(night.start) && point.isBefore(night.end)
        }
    }

    private fun durationScore(minutes: Long): Double {
        return when {
            minutes in 420L..540L -> 100.0
            minutes < 420 -> (minutes.toDouble() / 420.0 * 100.0).coerceIn(0.0, 100.0)
            else -> max(70.0, 100.0 - (minutes - 540) * 0.20)
        }
    }

    private fun efficiencyScore(percent: Int): Double =
        when {
            percent >= 90 -> 100.0
            percent >= 85 -> 90.0 + (percent - 85) * 2.0
            percent >= 75 -> 60.0 + (percent - 75) * 3.0
            else -> (percent.toDouble() / 75.0 * 60.0).coerceIn(0.0, 60.0)
        }

    private fun architectureScore(
        deepPercent: Int,
        remPercent: Int
    ): Double {
        val deepPenalty = distanceOutside(deepPercent.toDouble(), 8.0, 28.0)
        val remPenalty = distanceOutside(remPercent.toDouble(), 12.0, 32.0)
        return (100.0 - deepPenalty * 1.5 - remPenalty * 1.5).coerceIn(50.0, 100.0)
    }

    private fun distanceOutside(value: Double, low: Double, high: Double): Double =
        when {
            value < low -> low - value
            value > high -> value - high
            else -> 0.0
        }

    private fun stageMinutes(
        night: SleepNight,
        type: SleepStageType
    ): Long =
        night.stages
            .asSequence()
            .filter { it.type == type }
            .sumOf { Duration.between(it.start, it.end).toMinutes().coerceAtLeast(0) }

    private fun Iterable<Int>.averageIntOrNull(): Double? {
        val list = toList()
        return if (list.isEmpty()) null else list.average()
    }

    private fun Iterable<Long>.averageLongOrNull(): Double? {
        val list = toList()
        return if (list.isEmpty()) null else list.average()
    }

    private fun Iterable<Double>.averageDoubleOrNull(): Double? {
        val list = toList()
        return if (list.isEmpty()) null else list.average()
    }

    private fun formatMinutes(minutes: Long): String {
        val absolute = abs(minutes)
        val h = absolute / 60
        val m = absolute % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }
}
