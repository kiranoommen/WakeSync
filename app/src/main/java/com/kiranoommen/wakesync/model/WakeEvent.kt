package com.kiranoommen.wakesync.model

enum class WakeFeedback {
    TOO_EARLY,
    GOOD,
    TOO_LATE
}

data class WakeEvent(
    val id: String,
    val firedAtMillis: Long,
    val scheduleId: String,
    val scheduleLabel: String,
    val mode: AlarmMode,
    val kind: String,
    val reason: String,
    val deadlineMillis: Long,
    val sourcePackage: String? = null,
    val dataAgeMinutes: Long? = null,
    val completedAtMillis: Long? = null,
    val feedback: WakeFeedback? = null
)
