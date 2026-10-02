# WakeSync Sleep Metrics and Research Guardrails

WakeSync is a consumer wellness product, not a medical or diagnostic tool.

This document defines how sleep metrics are calculated and how the UI should communicate uncertainty.

## Data source

WakeSync reads user-authorized Health Connect records locally on the Android device.

Sleep sessions come from `SleepSessionRecord`, which can contain awake, light, deep and REM stage intervals.

Android documentation notes that a `SleepSessionRecord` requires an end time and is written after a sleep session has finished. Granular associated records may be written separately.

References:

- Android Health Connect sleep sessions: https://developer.android.com/health-and-fitness/health-connect/features/sleep-sessions
- Android comprehensive sleep experience: https://developer.android.com/health-and-fitness/health-connect/experiences/sleep
- Health Connect data types: https://developer.android.com/health-and-fitness/health-connect/data-types

## Total sleep time

WakeSync counts light, deep, REM and generic/unknown sleep-stage intervals as sleep.

For adults, the app uses **7–9 hours** as a broad informational range, while allowing the user to choose their own sleep goal.

The AASM/Sleep Research Society consensus states that adults should obtain at least 7 hours regularly, and its methodology discussion found consensus that 7–9 hours was appropriate for adult health.

References:

- https://pmc.ncbi.nlm.nih.gov/articles/PMC4442216/
- https://pmc.ncbi.nlm.nih.gov/articles/PMC4513271/

## Estimated sleep efficiency

WakeSync estimates sleep efficiency as:

```
estimated sleep minutes / session duration
```

This is explicitly labeled an estimate because a consumer-device sleep-session boundary is not guaranteed to be identical to clinically measured time in bed.

The score uses 85% only as a soft wellness benchmark, not a diagnosis or pass/fail threshold.

## Sleep onset latency

When the source provides an initial awake interval, latency is estimated as the time between session start and the first non-awake sleep interval.

If the source does not encode this reliably, WakeSync should show unavailable rather than invent a number.

## WASO

Wake After Sleep Onset is estimated as the sum of awake intervals after the first detected sleep interval and before the last sleep interval.

## Sleep stages

WakeSync reports Deep, Light and REM proportions primarily as **personal trends**.

Do not present a fixed stage percentage as a universal ideal. Normal sleep architecture varies with age, health, medications and night-to-night conditions.

Consumer wearable stage classification also differs from polysomnography.

References:

- Sleep architecture overview: https://pmc.ncbi.nlm.nih.gov/articles/PMC4024062/
- Fitbit/Garmin/WHOOP systematic review: https://pmc.ncbi.nlm.nih.gov/articles/PMC11004611/
- 2024 wearable umbrella review: https://pmc.ncbi.nlm.nih.gov/articles/PMC11560992/
- 2024 PSG comparison meta-analysis: https://pmc.ncbi.nlm.nih.gov/articles/PMC11874098/

## Sleep Regularity Index

WakeSync follows the SRI concept: compare whether a person is in the same sleep/wake state at the same clock time on adjacent days.

The product implementation uses 15-minute epochs derived from available Health Connect sleep sessions.

Because WakeSync may not have complete daytime nap data or clinical actigraphy, the app must describe this as a wearable-derived SRI estimate.

References:

- SRI methods/review: https://pmc.ncbi.nlm.nih.gov/articles/PMC8503839/
- Original regularity application: https://pmc.ncbi.nlm.nih.gov/articles/PMC5468315/

## Sleep debt

WakeSync displays a simple **sleep debt estimate**:

```
sum(max(0, personal nightly target - estimated nightly sleep))
```

The personal nightly target is optional and stored as total minutes. The Settings wheel supports 4h 00m through 12h 00m in 15-minute increments, and the same precise stored value is used anywhere WakeSync calculates goal progress or sleep debt.

It is a user-facing planning metric, not a physiological measurement of an exact biological debt.

## HRV and resting heart rate

HRV and resting heart rate are optional.

WakeSync requests these Health Connect permissions separately so the sleep/alarm experience still works when the user does not want to share recovery metrics.

HRV should be interpreted primarily relative to the individual user's baseline. Avoid universal "good HRV" thresholds.

## WakeSync Sleep Performance score

The score is proprietary wellness summarization, not a validated medical score.

Current WakeSync Sleep Score weighting:

- 40% duration
- 25% estimated sleep efficiency
- 20% regularity / wearable-derived Sleep Regularity Index
- 15% sleep-onset latency

The app exposes all four pillar subscores in the Score Pillar Breakdown sheet so the user can see the math instead of receiving an opaque score.

Consumer wearable sleep stages remain visible as trends and in the hypnogram, but stage ratios are not used as a direct score pillar. When regularity cannot yet be estimated, WakeSync uses a neutral regularity placeholder until enough consecutive tracked nights are available. Latency is treated as a WakeSync heuristic rather than a clinical diagnosis.

Period views also expose sleep debt and optional biometric trends separately rather than hiding them inside the score.

Do not label the score as diagnostic accuracy or imply clinical validation.

## Historical access

Health Connect normally limits third-party apps to data from up to 30 days before permission was granted.

WakeSync requests the separate historical-data permission only when the user wants longer analysis such as six-month views.

Reference:

- https://developer.android.com/health-and-fitness/health-connect/read-data
- https://developer.android.com/reference/androidx/health/connect/client/permission/HealthPermission#PERMISSION_READ_HEALTH_DATA_HISTORY

## Product language rules

Prefer:

- "estimated"
- "trend"
- "your baseline"
- "recent pattern"
- "WakeSync score"
- "consumer wearable data"

Avoid:

- "diagnosis"
- "clinically optimal" unless specifically validated
- "93% accurate" without validation
- claiming exact sleep stage as ground truth
- universal HRV targets
- promising that a smart alarm will improve health outcomes
