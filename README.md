# WakeSync

WakeSync is an Android smart alarm that uses Health Connect sleep-stage data to choose a wake point inside a user-defined wake range.

## Smart Wake 0.2

WakeSync now uses a three-step fallback ladder:

1. **Live sleep stage first**
2. **Saved historical prediction at 15 minutes before the hard stop**
3. **Hard-stop alarm if all else fails**

For a user range of 6:20–7:00 AM:

- 5:35 AM — live monitoring starts;
- 6:20 AM — earliest time the app is allowed to wake the user;
- 6:20–6:45 AM — fresh live Awake/Light stages can trigger the alarm;
- 6:45 AM — live timing hands off to the locally saved historical profile;
- 6:45–7:00 AM — the predictor can choose the strongest historical wake point;
- 7:00 AM — independent hard-stop alarm fires no matter what.

The 45-minute pre-window period is observation only. WakeSync never intentionally wakes the user before the start of their range.

## Historical prediction

Whenever recent sleep history is loaded, WakeSync derives and saves a compact profile from up to 30 nights. The profile stores stage quality by minute-before-natural-wake, rather than duplicating raw health records.

The predictive fallback requires at least three historical samples for a candidate minute and ignores weak candidates. If no trustworthy prediction is available, WakeSync simply keeps the hard-stop alarm.

## Android requirements

Smart Wake needs:

- read-only Health Connect sleep access;
- Health Connect background-read access when supported;
- exact-alarm access;
- notifications;
- full-screen alarm access on Android 14+.

All sleep-stage analysis remains on-device.

## Data source reality

WakeSync can only react to sleep stages that a watch or sleep app has already written into Health Connect. A connected wearable may publish data live, in batches, or only after a session ends. WakeSync only treats a stage as live when it is ongoing or no more than five minutes old.

## Privacy

WakeSync reads sleep data from Health Connect and processes it locally. It does not upload, sell, share, or modify health data. See [docs/PRIVACY.md](docs/PRIVACY.md).
