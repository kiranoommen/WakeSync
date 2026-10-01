package com.kiranoommen.wakesync.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.kiranoommen.wakesync.model.SleepNight
import com.kiranoommen.wakesync.model.SleepStageSegment
import com.kiranoommen.wakesync.model.SleepStageType
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class HealthConnectManager(private val context: Context) {

    companion object {
        const val PROVIDER_PACKAGE = "com.google.android.apps.healthdata"

        val requiredPermissions = setOf(
            HealthPermission.getReadPermission(SleepSessionRecord::class)
        )

        val analyticsPermissions = setOf(
            HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
            HealthPermission.getReadPermission(RestingHeartRateRecord::class)
        )

        val historyPermissions = setOf(
            HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY
        )
    }

    fun sdkStatus(): Int =
        HealthConnectClient.getSdkStatus(context, PROVIDER_PACKAGE)

    fun clientOrNull(): HealthConnectClient? =
        if (sdkStatus() == HealthConnectClient.SDK_AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else {
            null
        }

    fun historyReadAvailable(): Boolean {
        val client = clientOrNull() ?: return false
        return client.features.getFeatureStatus(
            HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_HISTORY
        ) == HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
    }

    suspend fun hasRequiredPermissions(): Boolean {
        val client = clientOrNull() ?: return false
        return client.permissionController
            .getGrantedPermissions()
            .containsAll(requiredPermissions)
    }

    suspend fun hasAnalyticsPermissions(): Boolean {
        val client = clientOrNull() ?: return false
        return client.permissionController
            .getGrantedPermissions()
            .containsAll(analyticsPermissions)
    }

    suspend fun hasHistoryPermission(): Boolean {
        val client = clientOrNull() ?: return false
        return client.permissionController
            .getGrantedPermissions()
            .containsAll(historyPermissions)
    }

    suspend fun readRecentSleep(days: Long = 30): List<SleepNight> {
        val client = clientOrNull() ?: return emptyList()
        if (!hasRequiredPermissions()) return emptyList()

        val end = Instant.now()
        val start = end.minus(days, ChronoUnit.DAYS)

        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = false,
                pageSize = 500
            )
        )

        val canReadAnalytics = hasAnalyticsPermissions()

        val hrvRecords = if (canReadAnalytics) {
            runCatching {
                client.readRecords(
                    ReadRecordsRequest(
                        recordType = HeartRateVariabilityRmssdRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(start, end),
                        ascendingOrder = false,
                        pageSize = 500
                    )
                ).records
            }.getOrDefault(emptyList())
        } else {
            emptyList()
        }

        val restingRecords = if (canReadAnalytics) {
            runCatching {
                client.readRecords(
                    ReadRecordsRequest(
                        recordType = RestingHeartRateRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(start, end),
                        ascendingOrder = false,
                        pageSize = 500
                    )
                ).records
            }.getOrDefault(emptyList())
        } else {
            emptyList()
        }

        val zone = ZoneId.systemDefault()

        return response.records.map { record ->
            val wakeDate = record.endTime.atZone(zone).toLocalDate()

            val hrvForNight = hrvRecords
                .filter { hrv ->
                    !hrv.time.isBefore(record.startTime) &&
                        hrv.time.isBefore(record.endTime.plus(2, ChronoUnit.HOURS))
                }
                .map { it.heartRateVariabilityMillis }
                .takeIf { it.isNotEmpty() }
                ?.average()

            val rhrForNight = restingRecords
                .firstOrNull { it.time.atZone(zone).toLocalDate() == wakeDate }
                ?.beatsPerMinute

            SleepNight(
                start = record.startTime,
                end = record.endTime,
                sourcePackage = record.metadata.dataOrigin.packageName,
                restingHeartRateBpm = rhrForNight,
                averageHrvMs = hrvForNight,
                stages = record.stages.map { stage ->
                    SleepStageSegment(
                        start = stage.startTime,
                        end = stage.endTime,
                        type = when (stage.stage) {
                            SleepSessionRecord.STAGE_TYPE_AWAKE,
                            SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED,
                            SleepSessionRecord.STAGE_TYPE_OUT_OF_BED -> SleepStageType.AWAKE
                            SleepSessionRecord.STAGE_TYPE_LIGHT -> SleepStageType.LIGHT
                            SleepSessionRecord.STAGE_TYPE_DEEP -> SleepStageType.DEEP
                            SleepSessionRecord.STAGE_TYPE_REM -> SleepStageType.REM
                            else -> SleepStageType.UNKNOWN
                        }
                    )
                }
            )
        }
    }
}
