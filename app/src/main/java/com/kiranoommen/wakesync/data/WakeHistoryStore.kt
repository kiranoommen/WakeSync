package com.kiranoommen.wakesync.data

import android.content.Context
import com.kiranoommen.wakesync.model.HistoricalWakeProfile

class WakeHistoryStore(context: Context) {

    private val preferences =
        context.getSharedPreferences("wake_history", Context.MODE_PRIVATE)

    fun save(profile: HistoricalWakeProfile) {
        if (profile.usableNights < 3) return

        preferences.edit()
            .putInt(KEY_USABLE_NIGHTS, profile.usableNights)
            .putString(
                KEY_AVERAGE_SCORES,
                profile.averageScores.joinToString(",")
            )
            .putString(
                KEY_SAMPLE_COUNTS,
                profile.sampleCounts.joinToString(",")
            )
            .apply()
    }

    fun load(): HistoricalWakeProfile? {
        val scores = preferences.getString(KEY_AVERAGE_SCORES, null)
            ?.split(",")
            ?.mapNotNull { it.toDoubleOrNull() }
            ?: return null

        val counts = preferences.getString(KEY_SAMPLE_COUNTS, null)
            ?.split(",")
            ?.mapNotNull { it.toIntOrNull() }
            ?: return null

        if (scores.isEmpty() || scores.size != counts.size) return null

        return HistoricalWakeProfile(
            usableNights = preferences.getInt(KEY_USABLE_NIGHTS, 0),
            averageScores = scores,
            sampleCounts = counts
        )
    }

    private companion object {
        const val KEY_USABLE_NIGHTS = "usable_nights"
        const val KEY_AVERAGE_SCORES = "average_scores"
        const val KEY_SAMPLE_COUNTS = "sample_counts"
    }
}
