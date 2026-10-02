# WakeSync privacy model

WakeSync processes sleep data locally on the user's Android device.

## Health data access

WakeSync requests read-only access to sleep sessions and sleep stages through Android Health Connect.

When Smart Wake is enabled and the device supports it, WakeSync also requests Health Connect background-read access. This allows the app to re-check the newest sleep stage during the short monitoring period before and during the user's wake range.

WakeSync does not request permission to write sleep data.

## Local processing

Smart Wake decisions are made on-device. WakeSync stores only the user's wake settings locally for the current implementation; raw sleep records are read from Health Connect as needed rather than uploaded to a WakeSync server.

## What WakeSync does not do

WakeSync does not:

- upload sleep or health data to a WakeSync server;
- sell health data;
- share health data with advertisers or third parties;
- modify Health Connect records;
- infer that stale Health Connect records are live;
- measure or diagnose cortisol.

Users can revoke WakeSync's Health Connect access at any time.

## Future changes

If a future feature requires network transfer or server-side storage of health data, that feature must not be enabled until this document, the in-app disclosure, and the privacy policy are updated accordingly.
