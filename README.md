# WakeSync

WakeSync is an Android smart alarm and sleep-trend app built around Health Connect.

The core idea is simple:

- the user chooses the **latest acceptable wake time**;
- the user chooses how much earlier WakeSync is allowed to wake them;
- WakeSync uses recent sleep patterns to choose a conservative wake point inside that window;
- the hard wake-by deadline is always protected.

## Current prototype

### Smart alarms

- recurring weekday schedules;
- different times for different day groups;
- adjustable smart windows: Off / 10 / 20 / 30 / 45 minutes;
- exact Android alarm scheduling;
- hard deadline backup;
- skip-once with automatic recurring resume;
- visible next-run and resume dates;
- reversible skip state;
- sound, vibration and snooze controls;
- reboot rescheduling.

### Sleep analytics

- Past Week, Past 2 Weeks, Past Month and dual-calendar custom ranges;
- previous-period comparison mode;
- WakeSync Sleep Performance score;
- total sleep, estimated efficiency, latency and WASO;
- Deep / Light / REM / Awake stage distribution;
- interactive duration + efficiency trend chart;
- wearable-derived Sleep Regularity Index estimate;
- personal sleep goal, sleep-debt estimate and streaks;
- expandable sortable sleep log;
- optional HRV and resting-heart-rate metrics when Health Connect provides them;
- local trend-based WakeSync insights;
- CSV and PDF export generated locally.

### Dashboard

- contextual morning briefing and color-coded Sleep Score hero;
- transparent four-pillar score breakdown;
- customizable Home widgets;
- pin/unpin and reorder;
- next wake-window hero;
- sleep-goal progress ring and streak;
- last-night bento metrics;
- local personalized insight card;
- universal metric info drawers explaining meaning, measurement and relevance.

### Appearance

- System / Dark / Light mode;
- WakeSync indigo / lavender / amber design system;
- safe-area handling for Android status/navigation bars.

## Privacy

WakeSync is private by design.

- Health data is read through Health Connect.
- Sleep access is read-only.
- Recovery metrics and extended history use separate optional permissions.
- Raw health data is not uploaded to WakeSync servers.
- Personalization and trend analysis run on-device.
- Export occurs only when the user explicitly chooses to create/share a file.
- Android backup is disabled for WakeSync private app data.

See [docs/PRIVACY.md](docs/PRIVACY.md) and [docs/SLEEP_METRICS.md](docs/SLEEP_METRICS.md).

## Research guardrails

WakeSync does not treat consumer wearable sleep stages as clinical ground truth.

The score and smart-wake recommendation are wellness features, not medical diagnoses. Stage mix is deliberately low-weight, sleep duration is protected, and weak wake predictions fall back to the user's later deadline.

See [docs/SLEEP_METRICS.md](docs/SLEEP_METRICS.md) for the methodology and research references.
