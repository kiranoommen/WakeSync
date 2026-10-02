<p align="center">
  <img src="assets/wakesync-hero.svg" alt="WakeSync brand banner" width="100%" />
</p>

# 🎨 WakeSync Brand Guide

<p align="center">
  <a href="README.md"><strong>Docs Hub</strong></a> ·
  <a href="BRANDING.md">Brand</a> ·
  <a href="DESIGN_SYSTEM.md">Design</a> ·
  <a href="ARCHITECTURE.md">Architecture</a> ·
  <a href="WAKE_WINDOW_ALGORITHM.md">Wake Algorithm</a> ·
  <a href="TESTING.md">Testing</a> ·
  <a href="PRIVACY.md">Privacy</a>
</p>

WakeSync should feel like the transition from night to morning: calm, precise, reassuring, then gently energizing.

This document defines the outward-facing brand. For implementation-level UI tokens and component behavior, see [DESIGN_SYSTEM.md](DESIGN_SYSTEM.md).

## Brand name

**WakeSync**

Always write the product name as one word with a capital **W** and **S**.

Preferred:
- WakeSync
- WakeSync Smart Wake
- WakeSync for Android

Avoid:
- Wake Sync
- wakesync
- WAKE SYNC
- Wake-Sync

## Positioning

WakeSync is an Android smart alarm that uses sleep-stage data already available through Health Connect to choose a better wake moment inside a time range the user controls.

The product should be positioned around three ideas:

1. **The user controls the boundary.** WakeSync never intentionally wakes before the chosen range and never goes beyond the hard stop.
2. **Live data when available, graceful fallback when it is not.**
3. **Private by design.** Sleep analysis stays on the device.

## Core promise

> A smarter wake-up without giving up control of when you need to be awake.

## Primary tagline

**Better mornings, in sync with you.**

Use this as the default marketing tagline, onboarding subhead, and repository-facing brand line.

## Supporting copy

### Short description

WakeSync is a private Android smart alarm that combines live Health Connect sleep stages with personalized sleep-history prediction while preserving a guaranteed hard wake deadline.

### One-line technical description

Android sleep-stage smart alarm using Health Connect, on-device prediction, and exact-alarm fallbacks.

### Store-style description

WakeSync watches for a favorable wake point inside the time range you choose. It uses fresh sleep-stage data when available, falls back to patterns from your recent sleep history near the deadline, and keeps a hard-stop alarm scheduled so you are not left relying on a prediction.

## Brand personality

WakeSync should sound:

- calm;
- concise;
- trustworthy;
- technical when needed, but never clinical;
- reassuring without being patronizing;
- confident about product behavior, careful about biological claims.

WakeSync should not sound:

- mystical;
- biohacking-heavy;
- alarmist;
- overly medical;
- certain about consumer sleep-stage accuracy;
- as if it directly measures cortisol or diagnoses sleep quality.

## Voice examples

Preferred:
- “Live sleep monitoring starts 15 minutes before your wake range.”
- “No fresh sleep data? WakeSync will use your saved sleep pattern near the deadline.”
- “Your hard-stop alarm stays scheduled.”

Avoid:
- “Wake at the perfect point in your sleep cycle.”
- “Optimize your cortisol peak.”
- “WakeSync knows exactly when your body is ready.”
- “Clinically optimized wake timing.”

## 🌌 Visual direction

The visual language moves from **deep night** to **warm wake light**.

<p align="center">
  <img src="assets/brand-palette.svg" alt="WakeSync brand palette" width="100%" />
</p>

### Primary colors

| Token | Hex | Use |
| --- | --- | --- |
| Midnight | `#080D1F` | Dark backgrounds |
| Deep Surface | `#11172C` | Dark cards |
| Indigo | `#6F63FF` | Sleep / primary cool accent |
| Lavender | `#9A84FF` | Light-stage / secondary accent |
| Wake Amber | `#FFB44A` | Primary wake action |
| Bright Amber | `#FFD17A` | Highlight / glow |
| Pearl | `#E9ECFF` | Light neutral / awake accent |
| Day Background | `#F8F9FF` | Light-mode background |
| Day Text | `#15192A` | Light-mode primary text |

The signature brand transition is **midnight indigo → lavender → wake amber**.

Amber should remain special. It represents waking, the primary CTA, and the transition into morning. Do not use it on every control.

## Typography

Use the Android system sans / Roboto family until a deliberate custom type decision is made.

- Wake time / hero: 40–48sp, semibold
- Screen title: 28–32sp, semibold
- Card title: 18–20sp, medium or semibold
- Body: 14–16sp
- Supporting text: 12–13sp

Avoid thin weights for wake times, permission states, or other critical information.

## 🌅 Mark and wordmark

<table>
<tr>
<td width="34%" align="center"><img src="assets/wakesync-mark.svg" alt="WakeSync sunrise and sleep-wave mark" width="240" /></td>
<td>

### Current launcher mark

The current WakeSync mark combines:

- an **amber sunrise arc** for waking;
- a **pearl horizon** for the user-defined boundary;
- **indigo and lavender waves** for sleep-stage motion;
- a **midnight field** for the night state.

The Android launcher icon now uses this same brand language, including a monochrome Android 13+ variant.

The **WakeSync** text wordmark remains the default written brand. Do not invent alternate marks per screen or document.

</td>
</tr>
</table>

### Usage

- Keep the mark on Midnight, white, or other high-contrast surfaces.
- Preserve clear space of at least one quarter of the mark width.
- Do not recolor individual elements outside the approved palette.
- Do not rotate, stretch, add drop shadows, or replace the sunrise/waves with generic alarm-clock art.
- Emoji may decorate docs, but they are not the WakeSync logo.

## Iconography

Prefer simple rounded line icons that match Material conventions.

Good motifs:
- horizon / sunrise;
- gentle waveform;
- sleep-stage curve;
- clock boundary;
- subtle sync motion.

Avoid:
- medical crosses;
- ECG imagery unless the product actually uses heart data;
- brain imagery implying neurological measurement;
- cortisol or hormone imagery;
- generic “AI sparkle” branding.

## 🖼️ Product imagery

<p align="center">
  <img src="assets/smart-wake-flow.svg" alt="WakeSync live to predictive to hard stop product story" width="100%" />
</p>

Screenshots should show a realistic wake range and clearly expose the fallback behavior.

Recommended demo range:
- earliest wake: 6:20 AM
- hard stop: 7:00 AM
- live monitor start: 6:05 AM
- historical fallback window: 6:50–7:00 AM

Do not use screenshots that imply WakeSync can see live stages when the underlying data source has not actually synced them.

## Sleep-stage colors

Use stage colors consistently:

- Deep — Indigo
- Light — Lavender / cool blue
- REM — Amber
- Awake — Pearl / neutral

Never communicate stage exclusively through color; always include text or another accessible indicator.

## 🔒 Privacy language

<p align="center">
  <img src="assets/privacy-card.svg" alt="WakeSync privacy language" width="100%" />
</p>

Preferred short form:

**Processed on your device**

Supporting copy:

> Read-only access. Your sleep data stays on this device.

Do not make broader privacy promises that the implementation cannot guarantee.

## Product-claim boundary

WakeSync may describe:
- sleep stages supplied by Health Connect;
- freshness of the most recent record;
- historical patterns derived from prior sleep records;
- alarm timing decisions;
- confidence or data sufficiency.

WakeSync should not claim:
- to directly measure cortisol;
- to diagnose sleep disorders;
- clinical-grade sleep staging;
- an objectively perfect wake time;
- guaranteed improvement in health, mood, or performance.

## Repository and social presentation

Preferred repository description:

> Private Android smart alarm using Health Connect live sleep stages, on-device prediction, and a guaranteed hard wake deadline.

Preferred short social copy:

> WakeSync keeps live sleep primary inside the range you choose, arms history only near the deadline, and keeps a hard stop always.

## Related documents

- [Design system](DESIGN_SYSTEM.md)
- [Architecture](ARCHITECTURE.md)
- [Wake algorithm](WAKE_WINDOW_ALGORITHM.md)
- [Privacy model](PRIVACY.md)
- [Testing](TESTING.md)
