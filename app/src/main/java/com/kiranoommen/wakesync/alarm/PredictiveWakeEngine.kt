package com.kiranoommen.wakesync.alarm

import com.kiranoommen.wakesync.model.HistoricalWakeProfile
import com.kiranoommen.wakesync.model.SleepNight
import com.kiranoommen.wakesync.model.SleepStageType
import java.time.Instant
import java.time.ZonedDateTime

object PredictiveWakeEngine {

    private const val PROFILE_MINUTES = 60
    private const val MIN_USABLE_NIGHTS = 3
    private const val MIN_PREDICTIVE_SCORE = 0.45

    data class Prediction(
        val wakeAt: ZonedDateTime,
        val score: Double,
        val minutesBeforeDeadline: Int
    )

    fun buildProfile(nights: List<SleepNight>): HistoricalWakeProfile {
        val totals = DoubleArray(PROFILE_MINUTES + 1)
        val counts = IntArray(PROFILE_MINUTES + 1)
        var usableNights = 0

        nights.take(30).forEach { night ->
            if (night.stages.isEmpty()) return@forEach

            var contributed = false

            for (minutesBeforeEnd in 0..PROFILE_MINUTES) {
                val instant = night.end.minusSeconds(minutesBeforeEnd * 60L)
                val stage = stageAt(night, instant) ?: continue

                totals[minutesBeforeEnd] += stageScore(stage)
                counts[minutesBeforeEnd] += 1
                contributed = true
            }

            if (contributed) usableNights += 1
        }

        val averages = totals.indices.map { index ->
            if (counts[index] == 0) 0.0 else totals[index] / counts[index]
        }

        return HistoricalWakeProfile(
            usableNights = usableNights,
            averageScores = averages,
            sampleCounts = counts.toList()
        )
    }

    fun chooseWakeTime(
        profile: HistoricalWakeProfile,
        now: ZonedDateTime,
        deadline: ZonedDateTime,
        fallbackWindowMinutes: Int = 15
    ): Prediction? {
        if (profile.usableNights < MIN_USABLE_NIGHTS) return null

        var best: Prediction? = null

        for (minutesBeforeDeadline in fallbackWindowMinutes downTo 0) {
            val candidate = deadline.minusMinutes(minutesBeforeDeadline.toLong())
            if (candidate.isBefore(now)) continue

            val score = profile.scoreAt(minutesBeforeDeadline) ?: continue
            if (score < MIN_PREDICTIVE_SCORE) continue

            val prediction = Prediction(
                wakeAt = candidate,
                score = score,
                minutesBeforeDeadline = minutesBeforeDeadline
            )

            if (best == null ||
                prediction.score > best!!.score ||
                (prediction.score == best!!.score &&
                    prediction.wakeAt.isBefore(best!!.wakeAt))
            ) {
                best = prediction
            }
        }

        return best
    }

    private fun stageAt(
        night: SleepNight,
        instant: Instant
    ): SleepStageType? =
        night.stages.firstOrNull { stage ->
            !instant.isBefore(stage.start) && instant.isBefore(stage.end)
        }?.type

    private fun stageScore(stage: SleepStageType): Double =
        when (stage) {
            SleepStageType.AWAKE -> 1.0
            SleepStageType.LIGHT -> 0.8
            SleepStageType.REM -> 0.35
            SleepStageType.DEEP -> -1.0
            SleepStageType.UNKNOWN -> 0.0
        }
}
