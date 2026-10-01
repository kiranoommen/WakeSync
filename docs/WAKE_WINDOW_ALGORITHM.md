# Wake Window Algorithm — Version 1

## Goal

Choose a wake time inside a user-defined acceptable window that is more likely to coincide with an easier waking point.

Example:

- earliest acceptable wake: 6:20 AM
- must be awake by: 7:00 AM

WakeSync may choose any recommendation inside that 40-minute interval.

## Important constraint

WakeSync should not claim that consumer wearables measure sleep stages with clinical precision.

The algorithm is a personalized estimate built from wearable-provided stage data and the user's own history.

## V1 approach

### Step 1 — Build personal history

Use approximately 14–30 usable nights where possible.

For each night derive:

- sleep onset;
- final wake time;
- total sleep;
- stage transitions;
- timing of late-night Deep / Light / REM / Awake;
- source package;
- optional morning feedback.

### Step 2 — Normalize nights by sleep onset

Instead of comparing clock time alone, represent each stage transition as minutes since sleep onset.

This allows a 10 PM night and a midnight night to be compared meaningfully.

### Step 3 — Estimate favorable wake periods

For the final 60–90 minutes of historical sleep, score each minute based on:

- Awake: highest base score
- Light: high score
- REM: moderate score
- Deep: strong penalty
- proximity to natural end of prior sleep sessions
- prior successful wake feedback at similar relative timing

Example baseline weights:

- Awake: +1.0
- Light: +0.8
- REM: +0.35
- Deep: -1.0

These are implementation defaults, not medical claims.

### Step 4 — Project tonight

Given tonight's estimated sleep onset, shift the learned relative timing forward into clock time.

Only consider candidate times inside the user's acceptable wake window.

### Step 5 — Choose a window, not a magic minute

Return an interval, usually 10–20 minutes, centered around the highest-scoring region.

Example:

> Best wake window: 6:34–6:49 AM

If the historical signal is weak, widen the window and lower confidence.

## Confidence

Initial confidence should depend on:

- number of usable nights;
- consistency of stage timing;
- consistency of source data;
- presence of recent data;
- prior wake feedback.

Suggested labels:

- Learning
- Moderate confidence
- Strong pattern

Avoid presenting a fake precise percentage before the model has enough validation.

## Safety / UX behavior

If the user specifies “must be awake by 7:00 AM”, WakeSync must always trigger the final alarm by that time even if the predicted window is poor.

Wake-window optimization can move the alarm earlier, never later than the deadline.

## Future versions

Potential additions:

- heart rate / HRV;
- movement;
- sleep debt;
- day-of-week patterns;
- adaptive wake-feedback weighting;
- separate weekday/weekend models;
- on-device ML after enough data exists.

V1 should remain explainable and deterministic so behavior can be validated.
