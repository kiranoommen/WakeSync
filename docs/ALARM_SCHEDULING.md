# WakeSync Alarm Scheduling

## Product principle

WakeSync should be as simple as a normal alarm app while allowing real-life schedules.

A user should not be forced into one global wake-by time.

## Core model

Each recurring alarm schedule contains:

- wake-by time;
- selected weekdays;
- enabled/disabled state;
- wake-window length;
- sound;
- vibration;
- snooze settings;
- optional label.

Example:

- **Workdays** — 7:00 AM — Mon, Tue, Thu, Fri
- **Wednesday** — 6:00 AM — Wed
- **Weekend** — Off

## Wake window

The displayed time is the user's **latest acceptable wake time**.

WakeSync calculates an earlier favorable window automatically.

Example:

- Wake by: 7:00 AM
- Window: 30 minutes
- WakeSync may alarm between 6:30 and 7:00 AM
- 7:00 AM remains the hard deadline

The user should not need to manually choose the exact optimized alarm time.

## Simple setup

Default alarm editor:

1. Wake by
2. Repeat days
3. Wake-window length
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

## One-time override

Users need a fast way to change tomorrow without editing their recurring schedule.

Examples:

- Wake me by 5:30 AM tomorrow
- Skip tomorrow
- Let me sleep in tomorrow

After the one-time override, the recurring schedule resumes automatically.

## No-alarm days

A day may have no WakeSync alarm at all.

This is important for users who want unrestricted wake times on weekends or days off.

## Multiple schedules

Allow multiple named schedules rather than forcing seven separate alarm entries.

Schedules may overlap; if they do, WakeSync should warn the user and resolve the active deadline clearly.

## Morning behavior

WakeSync runs automatically.

The user does not need to:

- open WakeSync before sleeping;
- open WakeSync when waking;
- tap an "I'm awake" button every morning.

Stopping/dismissing the alarm records the wake event locally.

Optional wake-quality feedback may appear as a notification later, but it must never be required.

## UX direction

Use the familiarity of a standard Android alarm editor:

- large time picker;
- weekday chips;
- simple toggles for sound, vibration, and snooze;
- minimal visual clutter.

WakeSync-specific intelligence should appear as a secondary layer:

> Wake by 7:00 AM  
> Smart window: 6:30–7:00 AM  
> WakeSync chooses the best point automatically.

The user controls the deadline. WakeSync controls the optimization inside that deadline.
