package com.kiranoommen.wakesync.model

data class WakeEvent(
    val id: String,
    val scheduleId: String,
    val label: String,
    val firedAtMillis: Long,
    val deadlineMillis: Long,
    val mode: AlarmMode,
    val kind: String,
    val reason: String,
    val sleepStage: String? = null,
    val sourcePackage: String? = null,
    val dataAgeSeconds: Long? = null,
    val feedback: String? = null
) {
    val minutesBeforeDeadline: Long
        get() =
            if (deadlineMillis <= 0L) {
                0L
            } else {
                ((deadlineMillis - firedAtMillis) / 60_000L)
                    .coerceAtLeast(0L)
            }

    val isBackup: Boolean
        get() = kind.startsWith("backup_")
}
