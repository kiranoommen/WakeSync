package com.kiranoommen.wakesync.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.feature.HealthConnectFeatures
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.permission.HealthPermission.Companion.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.kiranoommen.wakesync.model.LiveSleepSnapshot
import com.kiranoommen.wakesync.model.SleepNight
import com.kiranoommen.wakesync.model.SleepStageSegment
import com.kiranoommen.wakesync.model.SleepStageType
import java.time.Instant
import java.time.temporal.ChronoUnit

class HealthConnectManager(private val context: Context) {

    companion object {
        const val PROVIDER_PACKAGE = "com.google.android.apps.healthdata"
        const val BACKGROUND_READ_PERMISSION =
            PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND

        val requiredSleepPermissions = setOf(
            HealthPermission.getReadPermission(SleepSessionRecord::class)
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

    fun backgroundReadAvailable(): Boolean {
        val client = clientOrNull() ?: return false
        return client.features.getFeatureStatus(
            HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND
        ) == HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
    }

    fun requestedPermissions(): Set<String> = buildSet {
        addAll(requiredSleepPermissions)
        if (backgroundReadAvailable()) {
            add(BACKGROUND_READ_PERMISSION)
        }
    }

    suspend fun hasRequiredPermissions(): Boolean {
        val client = clientOrNull() ?: return false
        return client.permissionController
            .getGrantedPermissions()
            .containsAll(requiredSleepPermissions)
    }

    suspend fun hasBackgroundReadPermission(): Boolean {
        if (!backgroundReadAvailable()) return false
        val client = clientOrNull() ?: return false
        return client.permissionController
            .getGrantedPermissions()
            .contains(BACKGROUND_READ_PERMISSION)
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
                pageSize = 100
            )
        )

        return response.records.map { record ->
            SleepNight(
                start = record.startTime,
                end = record.endTime,
                sourcePackage = record.metadata.dataOrigin.packageName,
                stages = record.stages.map { stage ->
                    SleepStageSegment(
                        start = stage.startTime,
                        end = stage.endTime,
                        type = stageType(stage.stage)
                    )
                }
            )
        }
    }

    suspend fun readLatestSleepStage(
        lookbackHours: Long = 12
    ): LiveSleepSnapshot? {
        val client = clientOrNull() ?: return null
        if (!hasRequiredPermissions()) return null

        val end = Instant.now()
        val start = end.minus(lookbackHours, ChronoUnit.HOURS)

        val response = client.readRecords(
            ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = false,
                pageSize = 100
            )
        )

        val latest = response.records
            .asSequence()
            .flatMap { record ->
                record.stages.asSequence().map { stage ->
                    Triple(record.metadata.dataOrigin.packageName, record, stage)
                }
            }
            .filter { (_, _, stage) -> !stage.startTime.isAfter(end) }
            .maxByOrNull { (_, _, stage) -> stage.endTime }
            ?: return null

        val sourcePackage = latest.first
        val stage = latest.third

        return LiveSleepSnapshot(
            stage = stageType(stage.stage),
            stageStart = stage.startTime,
            stageEnd = stage.endTime,
            sourcePackage = sourcePackage
        )
    }

    private fun stageType(stage: Int): SleepStageType =
        when (stage) {
            SleepSessionRecord.STAGE_TYPE_AWAKE,
            SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED,
            SleepSessionRecord.STAGE_TYPE_OUT_OF_BED -> SleepStageType.AWAKE
            SleepSessionRecord.STAGE_TYPE_LIGHT -> SleepStageType.LIGHT
            SleepSessionRecord.STAGE_TYPE_DEEP -> SleepStageType.DEEP
            SleepSessionRecord.STAGE_TYPE_REM -> SleepStageType.REM
            else -> SleepStageType.UNKNOWN
        }
}
