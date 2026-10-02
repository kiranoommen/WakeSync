# WakeSync Architecture

## Current objective

WakeSync is an Android smart alarm that combines sufficiently fresh Health Connect sleep-stage data with a locally saved historical wake profile, while guaranteeing the user's latest acceptable wake time.

## Technology

- Kotlin
- Jetpack Compose
- Android Health Connect
- AlarmManager exact alarms
- Foreground services for bounded live monitoring and ringing
- Local SharedPreferences for wake settings and the derived historical profile
- No required backend

## Smart Wake flow

```
Wearable / sleep app
        ↓
 Android Health Connect
        ↓
 recent history → PredictiveWakeEngine → saved compact profile
        ↓
 exact monitor-start alarm (E - 45m)
        ↓
 WakeMonitorService
        ↓
 fresh Awake / Light before cutoff
        ↓
 cutoff = max(E, L - 15m)
        ↓
 saved historical prediction
        ↓
 exact predictive alarm (when usable)
        ↓
 independent hard deadline L
        ↓
 AlarmRingingService / AlarmActivity
```

The deadline alarm is scheduled independently before live monitoring begins. Neither the live layer nor the predictive layer is allowed to remove that safety net unless the user has already been woken.

## Timing

Given:

- earliest acceptable wake = E
- latest acceptable wake = L

WakeSync uses:

- monitor start = E − 45 minutes
- earliest possible wake = E
- predictive cutoff = max(E, L − 15 minutes)
- hard deadline = L

The 45 minutes before E are observation only.

## Layer 1 — Live stage

From E until the predictive cutoff, WakeSync polls Health Connect once per minute.

A stage is considered current only when it is ongoing or ended within the last five minutes.

- Awake → wake now
- Light → wake now
- REM → keep monitoring
- Deep → keep monitoring
- Unknown/stale/missing → keep monitoring

This avoids treating delayed wearable sync as live data.

## Layer 2 — Saved historical prediction

Whenever recent sleep is loaded, WakeSync uses up to 30 nights to derive a compact wake profile.

For each minute in the final 60 minutes before historical sleep end, stage weights are:

- Awake: +1.0
- Light: +0.8
- REM: +0.35
- Deep: -1.0
- Unknown: 0.0

The saved profile contains average score and sample count for each minute-before-natural-wake position. Raw Health Connect records are not copied into local storage.

At the predictive cutoff, WakeSync evaluates only candidates that remain inside the user's allowed range. A candidate requires at least three historical samples and an average score of at least 0.45.

The highest-scoring eligible minute is scheduled as an exact predictive alarm. If no eligible minute exists, no predictive alarm is added.

## Layer 3 — Hard deadline

The hard deadline remains scheduled with AlarmManager regardless of live or predictive availability.

If live data is stale, the saved profile is weak, predictive scheduling fails, or the monitor is stopped by the OS, the deadline alarm still fires at L.

## Permissions

Base sleep access:

- `READ_SLEEP`

Live Smart Wake additionally uses:

- `READ_HEALTH_DATA_IN_BACKGROUND` when available;
- exact alarm special access;
- notifications;
- full-screen intent access on Android 14+.

WakeSync requests no Health Connect write permission.

## Reliability

- Monitor, predictive wake, and deadline are distinct exact-alarm paths.
- A successful live or predictive wake cancels the current day's remaining alarms and schedules the next day.
- Boot, clock changes, timezone changes, and exact-alarm permission changes restore the schedule.
- The live monitor runs for a bounded period and handles Android 15 foreground-service timeout callbacks.
- Alarm audio uses alarm audio attributes and repeats until the user taps **I’m awake**.

## Privacy architecture

- sleep analysis stays on-device;
- Health Connect access is read-only;
- the saved history is a derived score profile, not raw sleep records;
- no health-data upload;
- no advertising SDK with health-data access;
- no selling or sharing health information;
- permissions remain revocable by the user.
