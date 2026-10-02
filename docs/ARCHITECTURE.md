# WakeSync Architecture

## Current objective

WakeSync is an Android smart alarm that uses sufficiently fresh sleep-stage records from Health Connect to choose a wake point inside a user-defined range without uploading health data.

## Technology

- Kotlin
- Jetpack Compose
- Android Health Connect
- AlarmManager exact alarms
- Foreground services for the bounded live-monitoring and ringing periods
- Local SharedPreferences for wake settings
- No required backend

## Live Smart Wake flow

```
Wearable / sleep app
        ↓
 Android Health Connect
        ↓
 exact monitor-start alarm
        ↓
 WakeMonitorService
        ↓
 read newest sleep stage once/minute
        ↓
 freshness + wake-stage decision
        ↓
 WakeAlarmController
        ↓
 AlarmRingingService / AlarmActivity
```

The latest acceptable wake time is also scheduled independently as an AlarmManager alarm. The live monitor is therefore an optimization layer, not the only path to waking the user.

## Timing

Given:

- earliest acceptable wake = E
- latest acceptable wake = L

WakeSync schedules:

- monitor start = E − 45 minutes
- earliest possible smart wake = E
- hard deadline = L

The 45 minutes before E are used only to establish whether Health Connect is supplying current data. No smart alarm is fired before E.

## Live data rule

A Health Connect sleep-stage record is considered current only when the newest stage is ongoing or ended no more than five minutes ago.

This protects against treating a delayed wearable sync as a live stage.

If the newest stage is stale or absent:

- do not infer a current sleep stage;
- do not move the alarm earlier;
- keep the independent deadline alarm.

## Wake decision

Inside E…L:

- Awake → wake
- Light → wake
- REM → wake only when 10 minutes or less remain
- Deep → wait
- Unknown → wait

At L, wake regardless of stage.

These rules are deterministic product heuristics, not a claim that consumer wearables measure sleep stages with clinical precision.

## Permissions

Base sleep history:

- `READ_SLEEP`

Live Smart Wake additionally needs:

- `READ_HEALTH_DATA_IN_BACKGROUND` when the Health Connect feature is available;
- exact alarm access;
- notifications;
- full-screen intent access on Android 14+ for the alarm UI.

WakeSync requests no Health Connect write permission.

## Reliability

- Monitor and deadline are separate exact alarms.
- A successful smart wake cancels the current deadline and schedules the next day.
- Boot, clock changes, timezone changes, and exact-alarm permission changes trigger schedule restoration.
- If the live foreground monitor is stopped by the OS, the hard-deadline AlarmManager alarm remains scheduled.
- Alarm audio uses alarm audio attributes and repeats until the user taps **I’m awake**.

## Persistence

Wake range and enabled state are stored locally. Raw Health Connect records are not duplicated into a WakeSync database.

Room can be added later if feedback/history-based personalization requires persistent derived records.

## Privacy architecture

- all sleep analysis on-device;
- read-only Health Connect access;
- no health-data upload;
- no advertising SDK with health-data access;
- no selling or sharing health information;
- permissions remain revocable by the user.
