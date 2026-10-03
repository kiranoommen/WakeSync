package com.kiranoommen.wakesync.data

import android.content.Context
import com.kiranoommen.wakesync.model.AlarmMode
import com.kiranoommen.wakesync.model.WakeEvent
import com.kiranoommen.wakesync.model.WakeFeedback
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class WakeEventStore(context: Context) {

    private val preferences =
        context.getSharedPreferences(
            "wake_event_history",
            Context.MODE_PRIVATE
        )

    fun record(
        scheduleId: String,
        scheduleLabel: String,
        mode: AlarmMode,
        kind: String,
        reason: String,
        deadlineMillis: Long,
        sourcePackage: String? = null,
        dataAgeMinutes: Long? = null
    ): WakeEvent {
        val event = WakeEvent(
            id = UUID.randomUUID().toString(),
            firedAtMillis = System.currentTimeMillis(),
            scheduleId = scheduleId,
            scheduleLabel = scheduleLabel,
            mode = mode,
            kind = kind,
            reason = reason,
            deadlineMillis = deadlineMillis,
            sourcePackage = sourcePackage,
            dataAgeMinutes = dataAgeMinutes
        )

        save(
            listOf(event) +
                load().filterNot {
                    it.id == event.id
                }.take(MAX_EVENTS - 1)
        )

        return event
    }

    fun load(): List<WakeEvent> {
        val raw =
            preferences.getString(KEY_EVENTS, null)
                ?: return emptyList()

        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    decode(array.getJSONObject(index))
                        ?.let(::add)
                }
            }.sortedByDescending {
                it.firedAtMillis
            }
        }.getOrDefault(emptyList())
    }

    fun markSequenceCompleted(
        scheduleId: String,
        deadlineMillis: Long
    ) {
        val current = load()
        val target = current
            .filter {
                it.scheduleId == scheduleId &&
                    it.deadlineMillis == deadlineMillis
            }
            .maxByOrNull { it.firedAtMillis }
            ?: return

        save(
            current.map { event ->
                if (event.id == target.id) {
                    event.copy(
                        completedAtMillis =
                            System.currentTimeMillis()
                    )
                } else {
                    event
                }
            }
        )
    }

    fun setFeedback(
        eventId: String,
        feedback: WakeFeedback
    ) {
        save(
            load().map { event ->
                if (event.id == eventId) {
                    event.copy(feedback = feedback)
                } else {
                    event
                }
            }
        )
    }

    fun clear() {
        preferences.edit()
            .remove(KEY_EVENTS)
            .apply()
    }

    private fun save(events: List<WakeEvent>) {
        val array = JSONArray()
        events.take(MAX_EVENTS).forEach { event ->
            array.put(encode(event))
        }

        preferences.edit()
            .putString(KEY_EVENTS, array.toString())
            .apply()
    }

    private fun encode(event: WakeEvent): JSONObject =
        JSONObject().apply {
            put("id", event.id)
            put("firedAtMillis", event.firedAtMillis)
            put("scheduleId", event.scheduleId)
            put("scheduleLabel", event.scheduleLabel)
            put("mode", event.mode.name)
            put("kind", event.kind)
            put("reason", event.reason)
            put("deadlineMillis", event.deadlineMillis)
            event.sourcePackage?.let {
                put("sourcePackage", it)
            }
            event.dataAgeMinutes?.let {
                put("dataAgeMinutes", it)
            }
            event.completedAtMillis?.let {
                put("completedAtMillis", it)
            }
            event.feedback?.let {
                put("feedback", it.name)
            }
        }

    private fun decode(value: JSONObject): WakeEvent? =
        runCatching {
            WakeEvent(
                id = value.getString("id"),
                firedAtMillis =
                    value.getLong("firedAtMillis"),
                scheduleId =
                    value.getString("scheduleId"),
                scheduleLabel =
                    value.optString(
                        "scheduleLabel",
                        "Wake alarm"
                    ),
                mode =
                    AlarmMode.valueOf(
                        value.getString("mode")
                    ),
                kind = value.getString("kind"),
                reason = value.getString("reason"),
                deadlineMillis =
                    value.optLong(
                        "deadlineMillis",
                        0L
                    ),
                sourcePackage =
                    value.optString(
                        "sourcePackage"
                    ).takeIf {
                        it.isNotBlank()
                    },
                dataAgeMinutes =
                    if (
                        value.has("dataAgeMinutes")
                    ) {
                        value.getLong(
                            "dataAgeMinutes"
                        )
                    } else {
                        null
                    },
                completedAtMillis =
                    if (
                        value.has("completedAtMillis")
                    ) {
                        value.getLong(
                            "completedAtMillis"
                        )
                    } else {
                        null
                    },
                feedback =
                    value.optString(
                        "feedback"
                    ).takeIf {
                        it.isNotBlank()
                    }?.let {
                        WakeFeedback.valueOf(it)
                    }
            )
        }.getOrNull()

    private companion object {
        const val KEY_EVENTS = "events"
        const val MAX_EVENTS = 30
    }
}
