# 🧭 WakeSync Architecture

<p align="center">
  <a href="README.md"><strong>Docs Hub</strong></a> ·
  <a href="BRANDING.md">Brand</a> ·
  <a href="DESIGN_SYSTEM.md">Design</a> ·
  <a href="ARCHITECTURE.md">Architecture</a> ·
  <a href="WAKE_WINDOW_ALGORITHM.md">Wake Algorithm</a> ·
  <a href="TESTING.md">Testing</a> ·
  <a href="PRIVACY.md">Privacy</a>
</p>

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

## 🌅 Smart Wake flow

<p align="center">
  <img src="assets/smart-wake-flow.svg" alt="WakeSync smart wake fallback ladder" width="100%" />
</p>

The same flow in engineering terms:

```
Wearable / sleep app
        ↓
 Android Health Connect
        ↓
 recent history → PredictiveWakeEngine → saved compact profile
        ↓
 schedule historical fallback in final 10m (when usable)
        ↓
 schedule independent hard deadline L
        ↓
 exact monitor-start alarm (E - 15m)
        ↓
 WakeMonitorService
        ↓
 E reached → poll fresh live stages once/minute
        ↓
 Awake / Light before fallback → live wake wins
        ↓
 historical fallback fires if live has not already woken user
        ↓
 hard deadline L remains guaranteed
        ↓
 AlarmRingingService / AlarmActivity
```

The historical fallback and hard deadline are both armed independently. Live monitoring remains active through the entire allowed wake window until one of the wake paths fires.

## Timing

Given:

- earliest acceptable wake = E
- latest acceptable wake = L

WakeSync uses:

- monitor start = E − 15 minutes
- earliest possible wake = E
- historical fallback window start = max(E, L − 10 minutes)
- hard deadline = L

The 15 minutes before E are observation only.

## 🟣 Layer 1 — Live stage

From E until the user is awakened or L arrives, WakeSync polls Health Connect once per minute.

A stage is considered current only when it is ongoing or ended within the last five minutes.

- Awake → wake now
- Light → wake now
- REM → keep monitoring
- Deep → keep monitoring
- Unknown/stale/missing → keep monitoring

This avoids treating delayed wearable sync as live data.

## 🟪 Layer 2 — Saved historical fallback

Whenever recent sleep is loaded, WakeSync uses up to 30 nights to derive a compact wake profile.

For each minute in the final 60 minutes before historical sleep end, stage weights are:

- Awake: +1.0
- Light: +0.8
- REM: +0.35
- Deep: -1.0
- Unknown: 0.0

The saved profile contains average score and sample count for each minute-before-natural-wake position. Raw Health Connect records are not copied into local storage.

When the day's alarm chain is scheduled, WakeSync evaluates only candidates in the final 10 minutes before L, constrained so no candidate can be earlier than E.

A candidate requires at least three historical samples and an average score of at least 0.45.

The highest-scoring eligible minute before L is scheduled as an exact historical-fallback alarm. If no eligible minute exists, no historical alarm is added.

Live monitoring continues even after that fallback is armed.

## 🟠 Layer 3 — Hard deadline

The hard deadline remains scheduled with AlarmManager regardless of live or historical availability.

If live data is stale, the saved profile is weak, historical scheduling fails, or the monitor is stopped by the OS, the deadline alarm still fires at L.

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

- Monitor, historical fallback, and deadline are distinct exact-alarm paths.
- A successful live wake cancels the historical fallback and hard deadline for the current morning.
- A historical fallback wake cancels the hard deadline for the current morning.
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
