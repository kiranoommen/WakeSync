# WakeSync MVP Roadmap

## Milestone 1 — Health Connect proof of concept

Status: **in progress**

Success criteria:

- app installs on Android;
- read-only sleep permission can be granted;
- recent sleep sessions load;
- stage segments display;
- source package is visible;
- Fitbit-originated data can be identified or its absence clearly diagnosed.

## Milestone 2 — Production UI shell

Build the locked WakeSync visual direction in both dark and light mode.

Screens:

1. Onboarding / permission
2. Home
3. Sleep details
4. Settings / wake preferences

Components:

- wake-window hero card;
- “I’m awake” CTA;
- fluid sleep-stage chart;
- sleep-stage summary cards;
- stage detail rows;
- privacy banner;
- Health Connect source/status.

## Milestone 3 — Local persistence

Add Room for:

- wake preferences;
- derived sleep summaries;
- morning wake feedback;
- recommendation history.

No cloud account required.

## Milestone 4 — Wake Window Engine v1

Implement:

- earliest wake boundary;
- hard wake deadline;
- historical stage normalization;
- nightly personalized calculation;
- learning/confidence state;
- fallback behavior when data is sparse.

## Milestone 5 — Alarm behavior

Implement reliable Android alarm scheduling with flexible per-day wake schedules.

Requirements:

- wake-by time is **not** one global setting;
- users can assign different wake-by times to different weekdays;
- days can be left off entirely (for example, weekends with no alarm);
- support grouped schedules such as Mon/Tue/Thu/Fri at one time and Wed at another;
- support one-time overrides for tomorrow without changing the recurring schedule;
- final deadline alarm cannot be skipped by recommendation logic;
- clear handling for battery optimization and exact alarms;
- the user should **not** need to open WakeSync or tap "I'm awake" every morning;
- dismissing/stopping the alarm should be recorded automatically as the wake event;
- optional wake-quality feedback can be offered later, but must not block or require morning use;
- snooze strategy;
- wake feedback.

See [ALARM_SCHEDULING.md](ALARM_SCHEDULING.md).

## Milestone 6 — Real-world Fitbit testing

Test on multiple nights with Fitbit-originated sleep data.

Validate:

- when Health Connect receives stage records;
- whether data is available before the user has already woken;
- whether predictive mode or live mode is practical;
- source attribution consistency;
- stage completeness.

## Milestone 7 — Broader Android ecosystem

Only after Fitbit MVP is solid:

- Samsung Health / Galaxy Watch via Health Connect;
- Amazfit / Zepp via Health Connect where supported;
- WHOOP direct integration only if Health Connect is insufficient.

## Free-app cost strategy

Keep recurring cost close to zero:

- on-device processing;
- no health-data backend;
- no required account;
- no paid analytics dependency;
- Health Connect as the common Android data layer.

A backend should only be introduced when a feature clearly requires it.
