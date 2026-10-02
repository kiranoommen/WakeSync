package com.kiranoommen.wakesync.model

data class HistoricalWakeProfile(
    val usableNights: Int,
    val averageScores: List<Double>,
    val sampleCounts: List<Int>
) {
    fun scoreAt(minutesBeforeWake: Int): Double? {
        val score = averageScores.getOrNull(minutesBeforeWake) ?: return null
        val samples = sampleCounts.getOrNull(minutesBeforeWake) ?: return null
        if (samples < MIN_SAMPLES) return null
        return score
    }

    private companion object {
        const val MIN_SAMPLES = 3
    }
}
