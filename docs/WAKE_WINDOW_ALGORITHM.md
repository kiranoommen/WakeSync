# Live Smart Wake Algorithm — Version 2

## Goal

Wake the user at a favorable point inside the exact time range they chose while guaranteeing an alarm by the end of that range.

Example:

- earliest acceptable wake: 6:20 AM
- must be awake by: 7:00 AM
- monitoring begins: 5:35 AM

Monitoring before 6:20 AM does **not** mean the alarm may ring before 6:20 AM.

## Inputs

- earliest acceptable wake time;
- latest acceptable wake time;
- newest Health Connect sleep-stage record;
- timestamp of that record.

## Step 1 — Protect the deadline

Schedule the latest acceptable wake time as an independent hard alarm before live monitoring begins.

Live optimization must never be allowed to remove the guarantee that the alarm will ring by the deadline.

## Step 2 — Start observing 45 minutes early

At earliest wake minus 45 minutes, start the bounded live monitor.

Poll Health Connect once per minute.

Before the earliest acceptable wake time, only observe. Never trigger Smart Wake.

## Step 3 — Reject stale data

Treat the latest sleep stage as live only if:

- the stage is currently ongoing; or
- its end time is no more than five minutes old.

If the newest stage is older than that, consider live state unknown.

Do not extrapolate a current stage from stale records.

## Step 4 — Decide only inside the user range

From the earliest acceptable wake time through the deadline:

| Stage | Action |
| --- | --- |
| Awake | Wake now |
| Light | Wake now |
| REM | Wake when 10 minutes or less remain |
| Deep | Wait |
| Unknown / stale / missing | Wait |

The REM rule is a deadline-pressure compromise so the app does not ignore a non-deep stage and then force a wake at the final second.

## Step 5 — Hard deadline

At the latest acceptable wake time, ring regardless of sleep stage or live-data availability.

This is the non-negotiable fallback.

## Data-source limitation

WakeSync reads Health Connect; it does not control when Fitbit, Samsung Health, another wearable, or another sleep app writes its data.

If the source does not publish stages during sleep, the live algorithm safely degrades to the user's hard deadline instead of claiming to know the current stage.

## Interpretation

Consumer sleep staging is an estimate. WakeSync uses the source's stage labels as a scheduling signal, not as a medical diagnosis and not as a measurement of cortisol.
