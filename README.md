# WakeSync

WakeSync is an Android sleep-stage smart alarm that uses Health Connect data to estimate a personalized wake window locally on-device.

## MVP

The first proof of concept is intentionally small:

- request **read-only** Health Connect sleep permission;
- read recent sleep sessions and their REM/light/deep/awake stages;
- show the source package so we can verify Fitbit-originated data is reaching Health Connect;
- keep health-data processing on the device;
- do not upload, sell, share, or modify health data.

The next milestone, after Fitbit data is verified, is the personalized wake-window algorithm.

## Privacy

WakeSync is private by design. Health data is read from Health Connect and processed locally. See [docs/PRIVACY.md](docs/PRIVACY.md).
