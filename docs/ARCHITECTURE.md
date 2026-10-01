# WakeSync Architecture

## MVP objective

Prove that WakeSync can read Fitbit-originated sleep-stage data through Android Health Connect, normalize it locally, and produce a personalized wake-window recommendation without uploading health data.

## Technology

- Kotlin
- Jetpack Compose
- Android Health Connect
- Local persistence only
- No mandatory backend for MVP

## Package structure

```
com.kiranoommen.wakesync
├── data
│   ├── healthconnect
│   └── local
├── domain
│   ├── model
│   ├── repository
│   └── wakewindow
├── ui
│   ├── home
│   ├── sleepdetails
│   ├── onboarding
│   └── components
└── MainActivity
```

The current proof of concept is intentionally flatter and can migrate toward this structure as functionality grows.

## Data flow

```
Fitbit / Pixel Watch
        ↓
      Fitbit
        ↓
 Android Health Connect
        ↓
 HealthConnectDataSource
        ↓
 SleepRepository
        ↓
 normalized SleepNight
        ↓
 WakeWindowEngine
        ↓
 Home UI / Sleep Details UI
```

WakeSync should not require a WakeSync cloud service to perform this flow.

## Health permissions

MVP permission:

- Read sleep

Do not request write access.

Only request additional permissions such as heart rate or HRV when a concrete feature needs them.

## Domain models

### SleepNight

Represents one sleep session:

- start
- end
- data origin
- stage segments
- total sleep duration

### SleepStageSegment

- start
- end
- type: Awake / REM / Light / Deep / Unknown

### WakePreferences

- latest acceptable wake time
- earliest acceptable wake time
- alarm enabled
- optional weekday schedule

### WakeRecommendation

- recommended wake start
- recommended wake end
- selected alarm time
- confidence
- reason / explanation

### WakeFeedback

Optional morning feedback:

- easy
- okay
- groggy
- slept through

This data remains local in the MVP.

## Local persistence

Use Room when persistent personalization begins.

Suggested tables:

- sleep_night_summary
- wake_preferences
- wake_feedback
- wake_recommendation_history

Avoid duplicating raw Health Connect records unless needed. Prefer storing derived summaries and user feedback.

## Wake-window engine

The engine should not assume a universal 90-minute sleep cycle.

Initial algorithm inputs:

1. user wake deadline;
2. earliest acceptable wake time;
3. recent sleep-stage history;
4. estimated current-night sleep onset;
5. personal historical stage timing;
6. prior wake feedback when available.

The engine outputs a bounded recommendation inside the user's allowed window.

## Live vs predictive behavior

Health Connect may not receive finalized Fitbit stage data continuously during the night.

Therefore WakeSync should support two logical modes:

### Predictive mode

Uses prior nights and current sleep onset assumptions to predict a favorable wake window.

This can work even if stage data is only finalized after waking.

### Live mode

Only enable if the connected ecosystem exposes sufficiently current overnight stage data to WakeSync.

The UI should never imply live stage detection unless the data source actually supports it.

## Privacy architecture

MVP requirements:

- all sleep analysis on-device;
- read-only Health Connect permission;
- no health-data upload;
- no advertising SDK with access to health data;
- no selling or sharing health information;
- user can revoke Health Connect permission at any time.

If a future feature introduces sync or cloud backup, it should be opt-in and separately reviewed.
