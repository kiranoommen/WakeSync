package com.kiranoommen.wakesync.model

import java.time.Duration
import java.time.Instant

data class LiveSleepSnapshot(
    val stage: SleepStageType,
    val stageStart: Instant,
    val stageEnd: Instant,
    val sourcePackage: String
) {
    fun isFresh(now: Instant = Instant.now(), maxAgeMinutes: Long = 5): Boolean {
        if (stageStart.isAfter(now)) return false
        if (stageEnd.isAfter(now)) return true
        val age = Duration.between(stageEnd, now)
        return !age.isNegative && age <= Duration.ofMinutes(maxAgeMinutes)
    }
}
