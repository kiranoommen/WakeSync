# WakeSync Alarm Scheduling

## Product principle

WakeSync should feel as simple as a normal alarm app while supporting real-life schedules.

A user should not be forced into one global wake-by time.

## Core model

Each recurring alarm schedule contains:

- wake-by time;
- selected weekdays;
- enabled/disabled state;
- smart-window length;
- sound;
- vibration;
- snooze settings;
- optional label.

Example:

- **Workdays** — 7:00 AM — Mon, Tue, Thu, Fri
- **Wednesday** — 6:00 AM — Wed
- **Weekend** — Off

## Smart window

The displayed time is the user's **latest acceptable wake time**.

WakeSync may wake the user earlier only inside the chosen smart window.

Example:

- Wake by: 7:00 AM
- Smart window: 20 minutes
- WakeSync may alarm between 6:40 and 7:00 AM
- If no favorable point appears, alarm at 7:00 AM

Suggested presets:

- 10 minutes — Tight
- 20 minutes — Balanced
- 30 minutes — Flexible
- 45 minutes — Wide
- Off — Exact time

Do not default to a very wide window. A user who says 7:00 AM should not unexpectedly be awakened at 6:15 AM unless they explicitly chose a 45-minute window.

## Simple setup

Default alarm editor:

1. Wake by
2. Repeat days
3. Smart-window length
4. Sound
5. Vibration
6. Snooze

Keep advanced controls collapsed by default.

## Useful shortcuts

- Weekdays
- Every day
- Weekends
- Custom
- Copy this time to selected days

## Temporary disable / schedule exception

Turning off a recurring alarm should offer a lightweight exception flow.

Example:

> Skip this Wednesday only?

Options:

- Skip once
- Turn schedule off
- Cancel

If the user skips only the upcoming occurrence, WakeSync automatically restores the recurring schedule for the next matching weekday.

For example, skipping one Wednesday leaves the following Wednesday enabled.

## One-time override

Users need a fast way to change tomorrow without editing the recurring schedule.

Examples:

- Wake me by 5:30 AM tomorrow
- Skip tomorrow
- Sleep in tomorrow

After the one-time override, the recurring schedule resumes automatically.

## No-alarm days

A day may have no WakeSync alarm at all.

This is important for weekends, vacations, and days off.

## Multiple schedules

Allow multiple named schedules rather than forcing seven separate alarm entries.

Schedules may overlap; if they do, WakeSync should warn the user and clearly show which deadline is active.

## Morning behavior

WakeSync runs automatically.

The user does not need to:

- open WakeSync before sleeping;
- open WakeSync when waking;
- tap an "I'm awake" button every morning.

Stopping/dismissing the alarm records the wake event locally.

Optional wake-quality feedback may appear later, but it must never be required.

## UX direction

Use familiar alarm concepts:

- large time picker;
- weekday chips;
- simple toggles for sound, vibration, and snooze;
- one-tap enable/disable;
- minimal visual clutter.

WakeSync-specific intelligence should appear as a secondary layer:

> Wake by 7:00 AM  
> Smart window: 6:40–7:00 AM  
> If no favorable point is found, alarm at 7:00 AM.

The user controls the deadline and allowed flexibility. WakeSync optimizes only inside those boundaries.
