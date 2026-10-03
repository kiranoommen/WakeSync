# 🗺️ WakeSync MVP Roadmap

<p align="center">
  <a href="README.md"><strong>Docs Hub</strong></a> ·
  <a href="BRANDING.md">Brand</a> ·
  <a href="DESIGN_SYSTEM.md">Design</a> ·
  <a href="ARCHITECTURE.md">Architecture</a> ·
  <a href="WAKE_WINDOW_ALGORITHM.md">Wake Algorithm</a> ·
  <a href="TESTING.md">Testing</a> ·
  <a href="PRIVACY.md">Privacy</a>
</p>

## Milestone 1 — Health Connect proof of concept

Status: **complete**

Delivered:
- Android app shell;
- read-only sleep permission;
- recent sleep-session loading;
- sleep-stage parsing and display;
- source package visibility;
- Fitbit/Health Connect data-path validation tooling.

## Milestone 2 — Production UI shell

Status: **substantially implemented; polish and device validation remain**

Delivered:
- core WakeSync visual direction;
- branded adaptive launcher icon and Android 13+ monochrome icon;
- visual repository/documentation identity;
- dark/light-aware color system;
- wake-window hero;
- Smart Wake setup card;
- privacy messaging;
- sleep-session summaries;
- alarm screen;
- onboarding;
- simplified sleep context and log;
- alarm-first Home experience;
- four-tab swipe navigation.

Remaining:
- richer sleep-detail presentation;
- final wordmark lockup / store artwork;
- onboarding polish;
- final screenshot and accessibility pass.

## Milestone 3 — Local persistence

Status: **partially complete**

Delivered:
- wake preferences stored locally;
- derived historical wake profile stored locally.

Remaining:
- decide whether Room is actually necessary;
- persist morning feedback if/when feedback-driven learning is added;
- persist recommendation/wake-event history only if it materially improves the product.

A database should not be added only because it appeared in the original roadmap.

## Milestone 4 — Wake Window Engine v1

Status: **implemented; needs real-world validation**

Delivered:
- earliest wake boundary;
- hard wake deadline;
- live-stage freshness checks;
- live Awake / Light wake decisions;
- live-through historical fallback limited to the final 10 minutes;
- historical stage normalization;
- minimum-history threshold;
- hard-stop fallback.

Remaining:
- validate thresholds using real overnight data;
- add user-facing confidence/readiness state only after measurements justify it.

## Milestone 5 — Alarm behavior

Status: **implemented; reliability testing in progress**

Delivered:
- exact monitor-start alarm;
- independent hard-stop alarm;
- exact predictive fallback alarm;
- foreground monitoring service;
- alarm sound and vibration;
- full-screen alarm flow;
- reboot / time / timezone restoration;
- “I’m awake” handling.

Remaining:
- OEM battery-management testing;
- repeated overnight device testing;
- continue OEM battery-management and overnight reliability validation.

## Milestone 6 — Real-world Fitbit testing

Status: **next priority**

Validate across multiple nights:

- when Fitbit-originated stage records reach Health Connect;
- whether stages are fresh enough during the wake window;
- source attribution consistency;
- completeness of stage data;
- which fallback layer actually fires;
- whether background reads behave consistently with the phone locked and idle.

Use the [testing plan](TESTING.md).

## Milestone 7 — Feedback and personalization

Status: **planned after reliability validation**

Potential work:
- morning “how did that wake feel?” feedback;
- confidence based on data sufficiency;
- history weighting by recency;
- source-specific reliability adjustments;
- explainable reason for the selected wake time.

Do not add complexity until the overnight data path is understood.

## Milestone 8 — Broader Android ecosystem

Status: **later**

Only after the Fitbit/Health Connect path is solid:

- Samsung Health / Galaxy Watch through Health Connect;
- Amazfit / Zepp where supported;
- direct integrations only when Health Connect is insufficient.

## Free-app cost strategy

Keep recurring cost close to zero:

- on-device processing;
- no required health-data backend;
- no required account;
- no paid analytics dependency;
- Health Connect as the shared Android data layer.

Introduce a backend only when a feature genuinely requires it.
