# Live Smart Wake Algorithm — Version 2

## Goal

Wake the user at a favorable point inside the exact time range they chose while guaranteeing an alarm by the end of that range.

Example:

- earliest acceptable wake: 6:20 AM
- must be awake by: 7:00 AM
- live monitoring begins: 5:35 AM
- predictive fallback begins: 6:45 AM

The 45-minute pre-window period is observation only. WakeSync never intentionally wakes the user before 6:20 AM.

## Fallback ladder

WakeSync uses three layers, in this order:

1. **Live sleep stage**
2. **Saved historical prediction at T-15**
3. **Hard deadline**

The hard deadline is scheduled independently before the other two layers run.

## Layer 1 — Live sleep

Starting 45 minutes before the earliest allowed wake, WakeSync checks Health Connect once per minute.

Before the earliest wake time, it only observes.

From the earliest wake time until the predictive cutoff:

| Fresh live stage | Action |
| --- | --- |
| Awake | Wake now |
| Light | Wake now |
| REM | Keep monitoring |
| Deep | Keep monitoring |
| Unknown / stale / missing | Keep monitoring |

A Health Connect stage counts as fresh only when it is still ongoing or ended no more than five minutes ago.

WakeSync does not extrapolate a current stage from stale records.

## Layer 2 — Saved historical prediction

The predictive cutoff is:

`max(earliest allowed wake, hard deadline - 15 minutes)`

At that point WakeSync stops relying on live-stage timing and uses the sleep-history profile it has already saved locally.

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

### Choosing the final-15-minute wake point

At the predictive cutoff, WakeSync evaluates candidate times between the cutoff and the hard deadline.

A candidate is eligible only when:

- at least 3 historical nights contributed data for that minute-before-wake position; and
- its average score is at least 0.45.

WakeSync chooses the highest-scoring eligible candidate. Ties go to the earlier candidate.

If the best predicted time is effectively now, the alarm rings immediately. Otherwise WakeSync schedules an exact predictive alarm for that time.

The independent hard-deadline alarm stays scheduled.

## Layer 3 — Hard deadline

If there is not enough saved history, every historical candidate is weak, the predictive alarm cannot be scheduled, or anything else fails, WakeSync does not guess.

It waits for the already-scheduled hard deadline and rings then.

## Data-source limitation

WakeSync reads Health Connect; it does not control when Fitbit, Samsung Health, another wearable, or another sleep app writes its data.

If the source does not publish stages during sleep, the live layer may contribute nothing. WakeSync then falls through to saved history and finally the hard deadline.

## Interpretation

Consumer sleep staging is an estimate. WakeSync uses wearable-provided stage labels as a scheduling signal, not as a medical diagnosis and not as a measurement of cortisol.
