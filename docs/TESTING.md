# 🧪 WakeSync Testing Plan

<p align="center">
  <a href="README.md"><strong>Docs Hub</strong></a> ·
  <a href="BRANDING.md">Brand</a> ·
  <a href="DESIGN_SYSTEM.md">Design</a> ·
  <a href="ARCHITECTURE.md">Architecture</a> ·
  <a href="WAKE_WINDOW_ALGORITHM.md">Wake Algorithm</a> ·
  <a href="TESTING.md">Testing</a> ·
  <a href="PRIVACY.md">Privacy</a>
</p>

Alarm reliability is the highest-priority behavior in WakeSync. Live and predictive wake logic are optimizations; the hard stop is the safety net.

<p align="center">
  <img src="assets/smart-wake-flow.svg" alt="WakeSync fallback ladder under test" width="100%" />
</p>

## ✅ CI baseline

Every pull request should pass:

```bash
gradle :app:assembleDebug
```

CI proves the project compiles. It does not prove overnight alarm reliability.

## 📋 Required manual test matrix

### 1. Basic setup

Verify:
- Health Connect sleep permission can be granted;
- background-read permission can be granted where supported;
- exact-alarm access can be granted;
- exact-alarm access can be skipped during OOBE, then granted later from Settings → Permissions & Reliability;
- returning from Android exact-alarm settings refreshes WakeSync's granted/not-granted state;
- notification permission can be granted;
- full-screen alarm access is handled on Android 14+;
- Smart Wake cannot be enabled until required setup is complete.

### 2. Schedule presets and Standard Alarm

Verify:
- Tomorrow only schedules the next calendar day and auto-disables after its wake sequence completes;
- Weekdays schedules Monday through Friday;
- Every day schedules all seven days;
- Custom day chips preserve arbitrary weekday selections;
- swipe right toggles an alarm on/off;
- swipe left opens an editable duplicate with a new ID;
- duplicated alarms do not overwrite the source schedule.

### 2b. Standard Alarm

Verify:
- Standard Alarm can be selected without Health Connect permission;
- one exact alarm time can be configured independently of the Smart Wake range;
- enabling Standard Alarm does not schedule the live monitor or historical fallback;
- switching from Smart Wake to Standard Alarm stops any active live monitor;
- Standard Alarm rings with the Standard Alarm UI/reason;
- with Backup Alarms off, the next recurrence remains scheduled normally;
- with 2 Backup Alarms enabled, the primary alarm is followed by rings at +5 and +10 minutes unless the user explicitly stops the remaining sequence.

### 3. Wake-range boundaries

For a test range such as 6:20–7:00:
- monitor starts at 6:05;
- no alarm fires before 6:20;
- live monitoring remains active from 6:20 until a wake path fires;
- historical fallback, when available, is constrained to 6:50–7:00;
- must-be-awake alarm remains scheduled for 7:00.

Also test a wake range shorter than 10 minutes. Historical fallback must never schedule earlier than the user's earliest wake time.

### 4. Fresh live data

During the live portion of the range:
- fresh Awake → wake;
- fresh Light → wake;
- fresh REM → continue;
- fresh Deep → continue.

Confirm that triggering a live wake cancels competing live/history triggers but **does not cancel the hard deadline**. A normal “Dismiss this ring” must leave the deadline armed; only “I’m awake — stop remaining alarms” may cancel the rest of the current wake sequence.

### 5. Stale or missing live data

Test:
- latest stage older than five minutes;
- no current sleep session;
- Health Connect read failure;
- wearable has not synced overnight.

Expected:
- do not pretend the stage is live;
- do not wake early based on stale data;
- allow the separately armed historical fallback to fire if live data never produces a favorable stage;
- preserve the hard stop.

### 6. Predictive fallback

With at least three usable historical nights:
- build and store a profile;
- enter the final 10-minute fallback window;
- verify the highest-scoring eligible candidate is armed without stopping live monitoring;
- verify the predictive alarm never exceeds the hard deadline.

With insufficient history:
- no predictive alarm should be trusted;
- hard stop should remain the final wake path.

### 7. Backup alarms and hard stop

This is mandatory.

With Backup Alarms enabled:
- verify 1, 2, and 3-ring configurations;
- verify rings occur every five minutes after the hard deadline;
- verify “Dismiss this ring” stops only the current sound/vibration;
- verify the remaining backup alarms still fire;
- verify “I’m awake — stop remaining alarms” cancels all remaining alarms in the current sequence;
- verify the next recurring occurrence is still scheduled;
- verify Snooze is not part of WakeSync; Backup Alarms provide the intentional repeat-wake behavior.

Hard-stop validation:

Test with:
- live data unavailable;
- history unavailable;
- history below threshold;
- monitor service stopped;
- wearable disconnected.

Expected:
- must-be-awake alarm still rings at the configured deadline.

### 8. Device state

Test alarm behavior with:
- screen off;
- device locked;
- Doze / idle state;
- app removed from recent apps;
- app not manually opened after scheduling;
- low-power mode where practical.

OEM-specific battery management should be documented when discovered.

### 9. Restart and clock changes

Verify schedule restoration after:
- device reboot;
- manual clock change;
- timezone change;
- exact-alarm permission state change.

### 10. Alarm UI

Verify:
- full-screen alarm appears when permitted;
- alarm sound loops;
- vibration works;
- when no safety alarms remain, “I’m awake” stops sound/vibration and preserves the next recurring schedule;
- when safety alarms remain, “Dismiss this ring” preserves them;
- “I’m awake — stop remaining alarms” cancels the current sequence and preserves the next recurring schedule.

### 11. Smart Wake readiness and Wake History

Verify:
- readiness distinguishes live Smart Wake from history fallback and alarm-only protection;
- history shows the actual ring path that fired;
- live wake records include source and data age when available;
- historical fallback, must-be-awake, Standard, and Backup Alarm reasons render clearly;
- Too early / Good / Too late feedback persists after relaunch;
- clearing Wake History removes only WakeSync's local wake-event log and does not alter Health Connect data.

### 12. Alarm reliability card

Verify:
- exact-alarm status matches Android system state;
- notification status matches app notification access;
- full-screen status matches Android 14+ special access;
- background Smart Wake reflects Health Connect background-read permission;
- alarm-volume percentage matches Android alarm stream;
- Test Alarm uses the normal ringing screen, sound, vibration, and dismiss path.

### 13. Privacy regression

Verify:
- no raw sleep records are written to logs;
- no health information leaves the device;
- notification content does not expose sensitive stage/history detail unnecessarily;
- only required Health Connect permissions are requested;
- HRV and resting-heart-rate permissions are no longer requested.

## 🌙 Real-world overnight validation

Before treating live wake as production-ready, collect repeated tests across actual overnight wearable sync behavior.

Record only non-sensitive diagnostics such as:
- whether a stage was available;
- age of the newest stage record;
- source package;
- which fallback layer fired;
- scheduled and actual alarm timestamps.

Do not store or publish raw personal sleep records for debugging.
