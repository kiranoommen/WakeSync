# WakeSync Feature Backlog

## High-value MVP+

### Next alarm card

Home screen shows only the next active alarm:

- wake-by deadline;
- smart window;
- active schedule name;
- next occurrence;
- quick actions: Edit / Skip once.

### One-time exceptions

Support:

- Skip next occurrence
- Change tomorrow only
- Sleep in tomorrow
- Resume automatically on the next recurring day

### Adjustable smart window

User-selectable presets:

- Off
- 10 min
- 20 min
- 30 min
- 45 min

### Deadline guarantee

If WakeSync does not find a favorable wake point, alarm at the user's deadline.

### Gentle alarm escalation

Optional alarm sequence:

1. vibration / low volume;
2. gradual audio increase;
3. full alarm at deadline.

Never let a gentle phase cause the deadline alarm to be missed.

### Explain "Why this time?"

After an alarm, optionally show a short explanation such as:

> Woke at 6:48 AM because your recent nights suggest lighter sleep near this point. Your deadline was 7:00 AM.

Avoid overstating certainty.

## Useful later features

### Quick-settings tile / widget

Actions:

- Skip next alarm
- Change tomorrow's wake time
- Show next smart window

### Bedtime target

Based on the user's preferred sleep duration and next wake deadline, show an optional target bedtime.

### Vacation mode

Pause recurring schedules until a selected date.

### Time-zone travel handling

Detect time-zone changes and ask whether schedules should follow local time.

### Nap mode

One-off smart wake window for naps, separate from recurring nighttime schedules.

### Source health

Warn when:

- no recent sleep data exists;
- Health Connect permission was revoked;
- Fitbit stopped writing sleep stages;
- only partial-stage data is available.

### Wake reliability trends

Track:

- alarm dismiss time;
- snooze count;
- how often deadline fallback was needed;
- optional user-rated grogginess.

Use this to personalize WakeSync without creating an opaque "sleep score."

## Privacy

All personalization should remain on-device for the free MVP unless a future feature explicitly requires sync.
