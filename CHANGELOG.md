<p align="center">
  <img src="docs/assets/wakesync-mark.svg" alt="WakeSync mark" width="110" />
</p>

# 📜 Changelog

All notable WakeSync changes should be recorded here.

## Unreleased

## 0.3.0 — 2026-10-01

### Added
- branded multi-tab app shell and onboarding;
- detailed sleep analytics and sleep log;
- optional HRV and resting-heart-rate analytics;
- optional extended Health Connect history access;
- local CSV, PDF, and story-card exports;
- theme, sleep-goal, profile, and export-retention settings;
- sleep-metrics calculation and product-claim guardrails.

### Changed
- merged the mature UI/analytics work onto the newer live Smart Wake architecture without restoring the retired multi-alarm scheduler;
- expanded the Health Connect permission rationale and privacy documentation;
- preserved the live → T-15 historical prediction → independent hard-stop alarm flow.

### Existing repository improvements
- branded adaptive launcher icon using the current WakeSync palette;
- visual repository banner, smart-wake flow, palette, and privacy artwork;
- visual documentation hub and branded cross-navigation.

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
