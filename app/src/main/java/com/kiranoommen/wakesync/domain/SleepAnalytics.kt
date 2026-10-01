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
    val averageRestingHeartRateBpm: Double?
)

object SleepAnalytics {

    fun analyze(
        nights: List<SleepNight>,
        targetSleepMinutes: Int = 480
    ): PeriodAnalytics {
        val sorted = nights
            .sortedByDescending { it.end }
            .map { analyzeNight(it) }

        val avgSleep = sorted.map { it.asleepMinutes }.averageOrNull()?.roundToInt()?.toLong() ?: 0L
        val avgEfficiency = sorted.mapNotNull { it.efficiencyPercent }.averageOrNull()?.roundToInt()
        val debt = sorted.sumOf { max(0L, targetSleepMinutes.toLong() - it.asleepMinutes) }
        val regularity = sleepRegularityIndex(nights)
        val avgDeep = sorted.map { it.deepPercent }.averageOrNull()?.roundToInt()
        val avgRem = sorted.map { it.remPercent }.averageOrNull()?.roundToInt()
        val avgScore = sorted.map { it.score }.averageOrNull()?.roundToInt()

        return PeriodAnalytics(
            nights = sorted,
            averageSleepMinutes = avgSleep,
            averageEfficiencyPercent = avgEfficiency,
            sleepDebtMinutes = debt,
            regularityScore = regularity,
            averageScore = avgScore,
            averageDeepPercent = avgDeep,
            averageRemPercent = avgRem,
            averageHrvMs = nights.mapNotNull { it.averageHrvMs }.averageOrNull(),
            averageRestingHeartRateBpm = nights
                .mapNotNull { it.restingHeartRateBpm?.toDouble() }
                .averageOrNull()
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
            durationScore * 0.50 +
                continuityScore * 0.35 +
                architectureScore * 0.15
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
            scoreLabel = when {
                score >= 90 -> "Excellent"
                score >= 80 -> "Strong"
                score >= 70 -> "Fair"
                else -> "Low"
            }
        )
    }

    fun sleepRegularityIndex(nights: List<SleepNight>): Int? {
        if (nights.size < 3) return null

        val zone = ZoneId.systemDefault()
        val sessions = nights.sortedBy { it.start }
        val firstDate = sessions.first().start.atZone(zone).toLocalDate()
        val lastDate = sessions.last().end.atZone(zone).toLocalDate()

        val dates = generateSequence(firstDate) { previous ->
            previous.plusDays(1).takeIf { !it.isAfter(lastDate) }
        }.toList()

        if (dates.size < 2) return null

        var matches = 0
        var comparisons = 0

        for (index in 0 until dates.lastIndex) {
            val dayA = dates[index]
            val dayB = dates[index + 1]

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
            minutes in 420..540 -> 100.0
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

    private fun Iterable<Int>.averageOrNull(): Double? {
        val list = toList()
        return if (list.isEmpty()) null else list.average()
    }

    private fun Iterable<Long>.averageOrNull(): Double? {
        val list = toList()
        return if (list.isEmpty()) null else list.average()
    }

    private fun Iterable<Double>.averageOrNull(): Double? {
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
