# WakeSync

WakeSync is an Android smart alarm that uses sleep-stage data from Health Connect to choose a wake point inside a user-defined wake range.

## Smart Wake 0.2

The live Smart Wake flow is designed around a hard user boundary:

1. The user chooses the earliest and latest acceptable wake time.
2. WakeSync schedules the latest time as an independent hard-deadline alarm.
3. Live monitoring starts **45 minutes before the earliest wake time**.
4. During the allowed wake range, WakeSync re-reads the newest Health Connect sleep stage once per minute.
5. A stage is only treated as live when it is still ongoing or ended within the last five minutes.
6. WakeSync wakes immediately in **Awake** or **Light** sleep, may use **REM** in the final 10 minutes, and avoids waking from **Deep** sleep when time remains.
7. If live data is missing, stale, or delayed, WakeSync does not guess. The hard-deadline alarm still fires at the user's latest time.

The 45-minute pre-window period is observation only. WakeSync never intentionally wakes the user before the start of the range.

## Android requirements

Smart Wake needs:

- read-only Health Connect sleep access;
- Health Connect background-read access when supported;
- exact-alarm access;
- notifications;
- full-screen alarm access on Android 14+.

All sleep-stage analysis remains on-device.

## Data source reality

WakeSync can only react to sleep stages that a watch/app has already written into Health Connect. A connected wearable may publish data in real time, in batches, or only after a sleep session ends. WakeSync checks record freshness so delayed data is never presented as live.

## Privacy

WakeSync reads sleep data from Health Connect and processes it locally. It does not upload, sell, share, or modify health data. See [docs/PRIVACY.md](docs/PRIVACY.md).
