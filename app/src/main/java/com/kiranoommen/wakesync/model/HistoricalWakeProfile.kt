package com.kiranoommen.wakesync.model

data class HistoricalWakeProfile(
    val usableNights: Int,
    val averageScores: List<Double>,
    val sampleCounts: List<Int>
) {
    fun scoreAt(minutesBeforeWake: Int): Double? {
        if (minutesBeforeWake !in averageScores.indices) return null
        if (sampleCounts.getOrNull(minutesBeforeWake)?.let { it < MIN_SAMPLES } != false) {
            return null
        }
        return averageScores[minutesBeforeWake]
    }

    private companion object {
        const val MIN_SAMPLES = 3
    }
}
