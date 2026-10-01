# WakeSync privacy model

WakeSync is designed to process sleep data locally on the user's Android device.

## Health data access

WakeSync requests **read-only** access to sleep sessions and sleep stages through Android Health Connect.

WakeSync does not request permission to write sleep data.

## What WakeSync does not do

For the MVP, WakeSync does not:

- upload sleep or health data to a WakeSync server;
- store health data outside the user's device;
- sell health data;
- share health data with advertisers or third parties;
- modify Health Connect records.

Users can revoke WakeSync's access at any time in Health Connect.

## Future changes

If a future feature requires network transfer or server-side storage of health data, that feature must not be enabled until this document, the in-app disclosure, and the privacy policy are updated accordingly.
