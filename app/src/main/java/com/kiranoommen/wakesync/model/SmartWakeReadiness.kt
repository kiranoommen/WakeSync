package com.kiranoommen.wakesync.model

data class SmartWakeReadiness(
    val sleepPermission: Boolean = false,
    val backgroundSleepPermission: Boolean = false,
    val exactAlarmAccess: Boolean = false,
    val notificationsAllowed: Boolean = false,
    val fullScreenAllowed: Boolean = false,
    val usableHistoryNights: Int = 0,
    val latestStageSource: String? = null,
    val latestStageAgeMinutes: Long? = null
) {
    val alarmReliabilityReady: Boolean
        get() =
            exactAlarmAccess &&
                notificationsAllowed &&
                fullScreenAllowed

    val liveSmartWakeReady: Boolean
        get() =
            sleepPermission &&
                backgroundSleepPermission &&
                alarmReliabilityReady

    val historyReady: Boolean
        get() = usableHistoryNights >= 3

    val historyStrength: String
        get() =
            when {
                usableHistoryNights >= 14 -> "Strong"
                usableHistoryNights >= 5 -> "Ready"
                else -> "Learning"
            }
}
