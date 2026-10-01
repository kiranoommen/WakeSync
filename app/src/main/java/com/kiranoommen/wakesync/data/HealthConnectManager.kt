package com.kiranoommen.wakesync.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.kiranoommen.wakesync.model.SleepNight
import com.kiranoommen.wakesync.model.SleepStageSegment
import com.kiranoommen.wakesync.model.SleepStageType
import java.time.Instant
import java.time.temporal.ChronoUnit

class HealthConnectManager(private val context: Context) {

    companion object {
        const val PROVIDER_PACKAGE = "com.google.android.apps.healthdata"

        val requiredPermissions = setOf(
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

    suspend fun hasRequiredPermissions(): Boolean {
        val client = clientOrNull() ?: return false
        return client.permissionController
            .getGrantedPermissions()
            .containsAll(requiredPermissions)
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
                        type = when (stage.stage) {
                            SleepSessionRecord.STAGE_TYPE_AWAKE,
                            SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED -> SleepStageType.AWAKE
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
