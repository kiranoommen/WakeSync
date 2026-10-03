<p align="center">
  <img src="docs/assets/wakesync-mark.svg" alt="WakeSync mark" width="110" />
</p>

# 📜 Changelog

All notable WakeSync changes should be recorded here.

## Unreleased

## 0.7.0 — 2026-10-02

### Changed
- shifted Home back toward an alarm-first experience instead of a customizable sleep dashboard;
- renamed user-facing **Guardrail Wake Time** language to **Must be awake by**;
- renamed **Backup Rings** to the clearer **Backup Alarms**;
- simplified the Sleep experience and removed export/share actions from the active UI;
- simplified privacy settings now that generated report retention is no longer exposed;
- onboarding now explains that Smart Wake depends on Health Connect and is most useful with a fitness tracker or smartwatch that syncs sleep stages overnight.

### Removed
- Snooze from the alarm editor and ringing screen;
- dashboard customization from the active Home experience;
- CSV, PDF, and story-card export controls from the active Sleep experience.

### Product direction
WakeSync is an alarm app first. Sleep data exists to improve and explain Smart Wake, not to compete with full sleep-tracking apps.

## 0.6.0 — 2026-10-02

### Added
- optional **Backup Rings** per alarm: 1–3 extra rings at fixed five-minute intervals after the hard deadline;
- a two-action ringing flow when safety alarms remain: **Dismiss this ring** keeps the remaining sequence armed, while **I’m awake — stop remaining alarms** explicitly cancels it;
- an in-Settings action to grant exact-alarm access when that permission was skipped during onboarding.

### Changed
- early Smart Wake and historical-fallback rings no longer cancel the independently scheduled hard deadline;
- snooze is paused while Backup Rings are enabled to avoid overlapping wake sequences;
- the Guardrail Wake Time editor is now a clearly interactive, high-emphasis time card instead of a notice-like outlined row.

### Fixed
- users who bypassed exact-alarm setup in OOBE can now recover the permission path from Settings.

## 0.5.0 — 2026-10-02

### Restored
- four-tab **Home / Alarms / Sleep / Settings** interface;
- horizontal swipe navigation between tabs;
- recovered ambient/glass visual treatment and dashboard layout;
- multi-alarm recurring schedules, weekday selection, skip-next/resume, and per-alarm editing;
- donation button in Settings.

### Added
- explicit **Smart Wake / Standard Alarm** choice for every alarm;
- multi-alarm Smart Wake scheduling with live Health Connect monitoring, final-10-minute historical fallback, and independent hard deadline;
- per-alarm snooze, sound, vibration, label, and full-screen ringing context.

### Removed
- greeting-name setting and greeting-name onboarding.

### Fixed
- dark-theme text contrast regression introduced by the simplified 0.4 UI shell;
- loss of swipe navigation and the dedicated Alarms tab.

## 0.4.0 — 2026-10-02

### Added
- first-class **Standard Alarm** mode with a separate exact alarm time;
- Smart Wake / Standard Alarm mode selector;
- Standard Alarm operation without Health Connect or background sleep access;
- redesigned full-screen alarm UI with a large live clock, alarm-mode badge, wake reason, configured schedule, and oversized dismiss control;
- mode-aware alarm notification copy.

### Changed
- alarm readiness now requires Health Connect only for Smart Wake;
- switching to Standard Alarm cancels Smart Wake monitoring and predictive fallback while preserving exact alarm reliability;
- existing Smart Wake range and Standard Alarm time are stored independently.

### Previous unreleased timing changes

### Changed
- shortened live-monitor lead time from 45 minutes to 15 minutes before the earliest allowed wake;
- kept live Health Connect stage monitoring active throughout the full user wake window;
- changed historical prediction from a T-15 handoff to an independently armed fallback limited to the final 10 minutes before the hard stop;
- preserved the independent hard-stop alarm as the final safety net.


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
