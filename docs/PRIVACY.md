# WakeSync privacy model

WakeSync is designed to process sleep data locally on the user's Android device.

## Health data access

WakeSync requests **read-only** access to sleep sessions and sleep stages through Android Health Connect.

Optional read-only permissions can be granted separately for HRV, resting heart rate, and extended historical access. WakeSync does not require those optional permissions for the core alarm experience.

WakeSync does not request permission to write health data.

## What WakeSync does not do

For the MVP, WakeSync does not:

- upload sleep or health data to a WakeSync server;
- store health data outside the user's device;
- sell health data;
- share health data with advertisers or third parties;
- modify Health Connect records.

Users can revoke WakeSync's access at any time in Health Connect.

## Exports and local files

PDF, CSV, and story-card exports are generated locally only after the user explicitly chooses an export/share action.

By default, generated files use Android app cache. If the user enables **Keep generated exports** in WakeSync Settings, generated export files are retained in WakeSync's private app storage until the user clears them or removes the app.

WakeSync's export controls include a transparency preview before sharing. Exports do not intentionally include location information, raw sensor feeds, or unrelated Health Connect records.

## Future changes

If a future feature requires network transfer or server-side storage of health data, that feature must not be enabled until this document, the in-app disclosure, and the privacy policy are updated accordingly.
