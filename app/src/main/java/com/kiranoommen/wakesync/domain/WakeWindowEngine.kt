package com.kiranoommen.wakesync.domain

import com.kiranoommen.wakesync.model.SleepNight
import com.kiranoommen.wakesync.model.SleepStageType
import java.time.Duration

object WakeWindowEngine {

    data class Recommendation(
        val offsetMinutes: Int,
        val confidenceLabel: String,
        val reason: String
    )

    fun recommend(
        nights: List<SleepNight>,
        windowMinutes: Int
    ): Recommendation {
        if (windowMinutes <= 0) {
            return Recommendation(
                offsetMinutes = 0,
                confidenceLabel = "Exact time",
                reason = "Smart window is off."
            )
        }

        val usable = nights
            .filter { it.stages.isNotEmpty() }
            .take(21)

        if (usable.size < 3) {
            return Recommendation(
                offsetMinutes = 0,
                confidenceLabel = "Learning",
                reason = "More sleep history is needed before WakeSync moves the alarm earlier."
            )
        }

        val candidates = (0..windowMinutes step 5).toList()
        val scores = candidates.associateWith { offset ->
            val stageScore = usable.mapNotNull { night ->
                val point = night.end.minusSeconds(offset.toLong() * 60L)
                val stage = night.stages.firstOrNull {
                    !point.isBefore(it.start) && point.isBefore(it.end)
                }?.type ?: return@mapNotNull null

                when (stage) {
                    SleepStageType.AWAKE -> 1.0
                    SleepStageType.LIGHT -> 0.75
                    SleepStageType.REM -> 0.25
                    SleepStageType.DEEP -> -1.0
                    SleepStageType.UNKNOWN -> 0.0
                }
            }.averageOrZero()

            // Preserve sleep unless an earlier candidate is meaningfully better.
            val earlinessPenalty = offset * 0.018
            stageScore - earlinessPenalty
        }

        val exactScore = scores[0] ?: 0.0
        val best = scores.maxByOrNull { it.value } ?: return Recommendation(0, "Learning", "Not enough data.")
        val chosenOffset = if (best.key > 0 && best.value > exactScore + 0.15) best.key else 0

        val confidence = when {
            usable.size >= 14 -> "Strong pattern"
            usable.size >= 7 -> "Moderate pattern"
            else -> "Learning"
        }

        return Recommendation(
            offsetMinutes = chosenOffset,
            confidenceLabel = confidence,
            reason = if (chosenOffset == 0) {
                "WakeSync is preserving sleep and using your deadline."
            } else {
                "Recent nights suggest a more favorable wake point about $chosenOffset minutes before your deadline."
            }
        )
    }

    private fun List<Double>.averageOrZero(): Double =
        if (isEmpty()) 0.0 else average()
}
