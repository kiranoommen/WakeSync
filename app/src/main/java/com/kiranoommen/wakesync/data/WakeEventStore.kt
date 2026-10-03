package com.kiranoommen.wakesync.data

import android.content.Context
import com.kiranoommen.wakesync.model.AlarmMode
import com.kiranoommen.wakesync.model.WakeEvent
import org.json.JSONArray
import org.json.JSONObject

class WakeEventStore(context: Context) {

    private val preferences =
        context.getSharedPreferences(
            "wakesync_wake_events",
            Context.MODE_PRIVATE
        )

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
            }
        }
            .getOrDefault(emptyList())
            .sortedByDescending { it.firedAtMillis }
    }

    fun record(event: WakeEvent) {
        val updated =
            (listOf(event) + load())
                .distinctBy { it.id }
                .sortedByDescending { it.firedAtMillis }
                .take(MAX_EVENTS)

        save(updated)
    }

    fun setFeedback(
        eventId: String,
        feedback: String
    ) {
        val normalized =
            feedback.takeIf {
                it in VALID_FEEDBACK
            } ?: return

        val updated = load().map { event ->
            if (event.id == eventId) {
                event.copy(feedback = normalized)
            } else {
                event
            }
        }

        save(updated)
    }

    private fun save(events: List<WakeEvent>) {
        val array = JSONArray()
        events.forEach { event ->
            array.put(encode(event))
        }

        preferences.edit()
            .putString(KEY_EVENTS, array.toString())
            .apply()
    }

    private fun encode(
        event: WakeEvent
    ): JSONObject =
        JSONObject()
            .put("id", event.id)
            .put("scheduleId", event.scheduleId)
            .put("label", event.label)
            .put("firedAtMillis", event.firedAtMillis)
            .put("deadlineMillis", event.deadlineMillis)
            .put("mode", event.mode.name)
            .put("kind", event.kind)
            .put("reason", event.reason)
            .putOpt("sleepStage", event.sleepStage)
            .putOpt("sourcePackage", event.sourcePackage)
            .putOpt("dataAgeSeconds", event.dataAgeSeconds)
            .putOpt("feedback", event.feedback)

    private fun decode(
        value: JSONObject
    ): WakeEvent? =
        runCatching {
            WakeEvent(
                id = value.getString("id"),
                scheduleId =
                    value.optString("scheduleId"),
                label =
                    value.optString("label"),
                firedAtMillis =
                    value.getLong("firedAtMillis"),
                deadlineMillis =
                    value.optLong("deadlineMillis", 0L),
                mode =
                    runCatching {
                        AlarmMode.valueOf(
                            value.optString(
                                "mode",
                                AlarmMode.SMART_WAKE.name
                            )
                        )
                    }.getOrDefault(
                        AlarmMode.SMART_WAKE
                    ),
                kind =
                    value.optString("kind"),
                reason =
                    value.optString("reason"),
                sleepStage =
                    value.optString(
                        "sleepStage",
                        ""
                    ).takeIf { it.isNotBlank() },
                sourcePackage =
                    value.optString(
                        "sourcePackage",
                        ""
                    ).takeIf { it.isNotBlank() },
                dataAgeSeconds =
                    if (
                        value.has("dataAgeSeconds") &&
                        !value.isNull("dataAgeSeconds")
                    ) {
                        value.getLong("dataAgeSeconds")
                    } else {
                        null
                    },
                feedback =
                    value.optString(
                        "feedback",
                        ""
                    ).takeIf { it.isNotBlank() }
            )
        }.getOrNull()

    companion object {
        const val FEEDBACK_TOO_EARLY =
            "TOO_EARLY"
        const val FEEDBACK_GOOD =
            "GOOD"
        const val FEEDBACK_TOO_LATE =
            "TOO_LATE"

        private val VALID_FEEDBACK =
            setOf(
                FEEDBACK_TOO_EARLY,
                FEEDBACK_GOOD,
                FEEDBACK_TOO_LATE
            )

        private const val KEY_EVENTS =
            "events"
        private const val MAX_EVENTS = 60
    }
}
