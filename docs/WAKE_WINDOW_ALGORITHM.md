# Wake Window Algorithm — Version 1

## Goal

Choose a wake time inside a **user-controlled smart window** that may reduce sleep inertia without stealing unnecessary sleep.

Example:

- wake-by deadline: 7:00 AM
- smart window: 20 minutes
- WakeSync may alarm between 6:40 and 7:00 AM
- 7:00 AM is the hard deadline

## Scientific guardrails

WakeSync must not assume that everyone has fixed 90-minute sleep cycles.

Normal sleep cycles vary within a night and between people. Consumer wearables also estimate sleep stages rather than measure them with clinical polysomnography.

The algorithm therefore treats sleep-stage data as a useful signal, not ground truth.

Research basis:

- Slow-wave/deep sleep awakenings are often associated with more sleep inertia than lighter-stage awakenings.
- Circadian timing and prior sleep loss also affect sleep inertia.
- Consumer wearables can provide useful longitudinal estimates, but sleep-stage classification remains imperfect.
- Adequate sleep duration matters more than optimizing a single stage transition.

## User-controlled smart window

The user chooses how early WakeSync is allowed to wake them.

Recommended presets:

- **10 min — Tight**
- **20 min — Balanced** (default product setting)
- **30 min — Flexible**
- **45 min — Wide**
- **Off — exact wake-by time only**

The 20-minute default is a product choice, not a claim of a scientifically universal optimum.

WakeSync must never wake earlier than the selected window.

## Deadline guarantee

The wake-by time is always authoritative.

If WakeSync cannot find a favorable point inside the smart window:

> Alarm at the wake-by deadline anyway.

This behavior should be enabled by design rather than hidden behind an advanced toggle. If a user does not want an alarm that day, they should disable or skip that day's schedule.

## V1 approach

### Step 1 — Build personal history

Use approximately 14–30 usable nights where possible.

For each night derive:

- sleep onset;
- final wake time;
- total sleep;
- sleep-stage transitions;
- timing of late-night Deep / Light / REM / Awake;
- source package;
- optional alarm-dismissal and wake-feedback history.

### Step 2 — Normalize nights by sleep onset

Represent stage transitions as minutes since sleep onset instead of relying only on clock time.

This makes nights with different bedtimes comparable.

### Step 3 — Score candidate wake moments

Within the user's allowed smart window, rank candidate times using:

- likely Awake / Light sleep: positive signal;
- likely Deep sleep: negative signal;
- REM: neutral-to-moderate signal rather than a hard rule;
- proximity to natural historical wake transitions;
- consistency across recent nights;
- amount of sleep preserved;
- prior successful alarm-dismissal or feedback patterns.

The algorithm should include an **earliness penalty** so it does not wake the user 40–45 minutes early merely to avoid a predicted deep-sleep period.

### Step 4 — Protect sleep duration

If the user appears sleep-deprived or the night's sleep opportunity is short, WakeSync should prefer preserving sleep and choose a later candidate unless there is a strong reason not to.

Do not present this as diagnosis.

### Step 5 — Choose an alarm point

V1 should select the best-scoring time inside the allowed window.

If confidence is weak, prefer the later candidate.

If no candidate is clearly favorable, alarm at the deadline.

## Confidence

Confidence should depend on:

- number of usable nights;
- consistency of stage timing;
- consistency of source data;
- recency of data;
- quality/completeness of the current night's available data;
- prior alarm-dismissal or optional feedback patterns.

Suggested labels:

- Learning
- Moderate pattern
- Strong pattern

Avoid fake precision such as "93% accurate" unless validated against appropriate data.

## Predictive vs live mode

### Predictive mode

Use history and current-night timing to estimate the best wake point when live stage updates are unavailable.

### Live mode

Only use current sleep stage if the connected ecosystem provides sufficiently fresh overnight data.

Never imply live detection when the data is actually delayed or finalized after waking.

## Future versions

Potential additions:

- heart rate / HRV;
- movement;
- sleep-duration protection preference;
- day-of-week models;
- separate workday / weekend patterns;
- adaptive weighting from alarm behavior;
- local machine learning after enough user data exists.

V1 should remain explainable and deterministic so behavior can be validated.
