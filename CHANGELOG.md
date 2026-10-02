# Changelog

All notable WakeSync changes should be recorded here.

## Unreleased

### Planned
- real-device validation of live Health Connect sleep-stage freshness;
- expanded wake-history feedback and confidence handling;
- additional alarm reliability testing across Android versions and OEM battery policies.

## 0.2.0 — 2026-10-01

### Added
- live Health Connect sleep-stage monitoring;
- monitoring start 45 minutes before the user's earliest wake time;
- user-configurable earliest wake and hard deadline;
- live wake on fresh Awake or Light stages;
- predictive fallback during the final 15 minutes before the hard stop;
- locally saved derived historical wake profile;
- independent hard-stop alarm;
- exact-alarm handling;
- full-screen alarm handling;
- alarm audio and vibration service;
- reboot, clock-change, and timezone-change schedule restoration;
- Android CI build.

### Changed
- upgraded compile SDK and Android Gradle Plugin for Health Connect 1.1.0;
- updated privacy and architecture documentation;
- moved from prediction-only product planning to a live → historical prediction → hard-stop fallback model.

## 0.1.0 — 2026-10-01

### Added
- Health Connect sleep proof of concept;
- recent sleep-session reads;
- sleep-stage display;
- source-package visibility;
- initial WakeSync visual system and architecture documentation.
