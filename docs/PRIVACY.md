# 🔒 WakeSync Privacy Model

<p align="center">
  <a href="README.md"><strong>Docs Hub</strong></a> ·
  <a href="BRANDING.md">Brand</a> ·
  <a href="DESIGN_SYSTEM.md">Design</a> ·
  <a href="ARCHITECTURE.md">Architecture</a> ·
  <a href="WAKE_WINDOW_ALGORITHM.md">Wake Algorithm</a> ·
  <a href="TESTING.md">Testing</a> ·
  <a href="PRIVACY.md">Privacy</a>
</p>

<p align="center">
  <img src="assets/privacy-card.svg" alt="WakeSync privacy — processed on your device" width="100%" />
</p>

WakeSync processes sleep data locally on the user's Android device.

## 🩺 Health data access

WakeSync requests read-only access to sleep sessions and sleep stages through Android Health Connect.

WakeSync may separately request **optional** read-only access to heart-rate variability (HRV) and resting heart rate for the sleep analytics dashboard. Smart Wake does not require those two metrics.

When supported, the user may also grant Health Connect's extended-history permission so WakeSync can analyze more than the default recent-history window.

When Smart Wake is enabled and the device supports it, WakeSync also requests Health Connect background-read access. This allows the app to re-check the newest sleep stage during the short monitoring period before and during the user's wake range.

WakeSync does not request permission to write sleep data.

## 📱 Local processing

Smart Wake and sleep-analytics calculations are made on-device. WakeSync stores wake preferences, UI preferences, and a compact derived wake-history profile locally. Raw Health Connect records are read as needed rather than copied to a WakeSync server.

CSV, PDF, and story-card exports are generated locally only when the user requests them. By default exports are temporary; the user can choose to retain generated copies locally and can clear them from Settings.

## 🚫 What WakeSync does not do

WakeSync does not:

- upload sleep, HRV, resting-heart-rate, or other Health Connect data to a WakeSync server;
- sell health data;
- share health data with advertisers or third parties;
- modify Health Connect records;
- infer that stale Health Connect records are live;
- measure or diagnose cortisol.

Users can revoke WakeSync's Health Connect access at any time.

## 🔭 Future changes

If a future feature requires network transfer or server-side storage of health data, that feature must not be enabled until this document, the in-app disclosure, and the privacy policy are updated accordingly.
