package com.kiranoommen.wakesync.model

import java.time.Instant

data class SleepStageSegment(
    val start: Instant,
    val end: Instant,
    val type: SleepStageType
)

enum class SleepStageType {
    AWAKE,
    LIGHT,
    DEEP,
    REM,
    UNKNOWN
}

data class SleepNight(
    val start: Instant,
    val end: Instant,
    val stages: List<SleepStageSegment>,
    val sourcePackage: String
)
