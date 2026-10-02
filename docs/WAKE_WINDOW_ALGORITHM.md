# ⏰ Live Smart Wake Algorithm — Version 3

<p align="center">
  <a href="README.md"><strong>Docs Hub</strong></a> ·
  <a href="BRANDING.md">Brand</a> ·
  <a href="DESIGN_SYSTEM.md">Design</a> ·
  <a href="ARCHITECTURE.md">Architecture</a> ·
  <a href="WAKE_WINDOW_ALGORITHM.md">Wake Algorithm</a> ·
  <a href="TESTING.md">Testing</a> ·
  <a href="PRIVACY.md">Privacy</a>
</p>

## Goal

Wake the user at a favorable point inside the exact time range they chose while guaranteeing an alarm by the end of that range.

Example:

- earliest acceptable wake: 6:20 AM
- must be awake by: 7:00 AM
- live monitoring begins: 6:05 AM
- historical fallback window begins: 6:50 AM
- hard stop: 7:00 AM

The 15-minute pre-window period is observation only. WakeSync never intentionally wakes the user before 6:20 AM.

## 🌅 Wake hierarchy

WakeSync uses three independent layers:

1. **Live sleep stage — primary**
2. **Saved historical fallback — final 10 minutes**
3. **Hard deadline — guaranteed**

The historical fallback and hard deadline are scheduled independently. Live monitoring does **not** shut off when the historical fallback window begins.

## 🟣 Layer 1 — Live sleep

Starting 15 minutes before the earliest allowed wake, WakeSync checks Health Connect once per minute.

Before the earliest wake time, it only observes.

From the earliest wake time until the user is awakened or the hard deadline arrives:

| Fresh live stage | Action |
| --- | --- |
| Awake | Wake now |
| Light | Wake now |
| REM | Keep monitoring |
| Deep | Keep monitoring |
| Unknown / stale / missing | Keep monitoring |

A Health Connect stage counts as fresh only when it is still ongoing or ended no more than five minutes ago.

WakeSync does not extrapolate a current stage from stale records.

## 🟪 Layer 2 — Saved historical fallback

The fallback window starts at:

`max(earliest allowed wake, hard deadline - 10 minutes)`

WakeSync chooses the strongest eligible historical candidate inside that final window and arms an exact alarm for it ahead of time.

**Important:** arming the historical fallback does not replace live monitoring.

If a fresh Awake or Light stage appears before the historical fallback fires, live sleep wins and WakeSync cancels the remaining alarms for that morning.

If the historical fallback time arrives first, it wakes the user even if live data has remained REM, Deep, stale, missing, or unavailable.

### Building the saved profile

Whenever WakeSync loads recent sleep history, it uses up to the most recent 30 nights.

For each historical night it looks at the final 60 minutes before that night's natural sleep end and scores the recorded stage:

- Awake: +1.0
- Light: +0.8
- REM: +0.35
- Deep: -1.0
- Unknown: 0.0

For each minute-before-wake position, WakeSync stores:

- average historical score;
- number of usable historical samples.

Raw sleep records do not need to be duplicated locally; the stored profile is a compact derived summary.

### Choosing the fallback point

A candidate is eligible only when:

- at least 3 historical nights contributed data for that minute-before-wake position; and
- its average score is at least 0.45.

WakeSync chooses the highest-scoring eligible candidate. Ties go to the earlier candidate.

If the best historical candidate would be exactly the hard deadline, WakeSync does not add a duplicate fallback alarm; the independent hard-stop alarm already covers that time.

If no candidate qualifies, live monitoring continues and the hard stop remains.

## 🟠 Layer 3 — Hard deadline

The hard deadline is always scheduled independently.

If live data is stale or unavailable, there is not enough history, the historical candidate is weak, the monitor service stops, or anything else fails, the hard-stop alarm still fires at the user's latest acceptable time.

## Why 15 minutes before the range?

Starting 45 minutes early did not materially improve the wake decision because WakeSync cannot intentionally wake before the user's range. A 15-minute lead gives the monitoring service time to start, verify background Health Connect access, and observe freshness without running the foreground service unnecessarily early.

WakeSync still polls only once per minute.

## Why a final 10-minute historical fallback?

Historical prediction is useful as a fallback, but it should not take a large portion of the user's live wake window away from fresh data.

Using only the final 10 minutes keeps live sleep primary for most of the allowed range while retaining a meaningful pre-deadline fallback.

## Data-source limitation

WakeSync reads Health Connect; it does not control when Fitbit, Samsung Health, another wearable, or another sleep app writes its data.

If the source does not publish stages during sleep, the live layer may contribute nothing. The separately armed historical fallback can still fire, followed by the hard deadline if needed.

## Interpretation

Consumer sleep staging is an estimate. WakeSync uses wearable-provided stage labels as a scheduling signal, not as a medical diagnosis and not as a measurement of cortisol.
