# WakeSync Design System

For outward-facing naming, voice, positioning, and brand usage, see [BRANDING.md](BRANDING.md).

## Product direction

WakeSync should feel calm at night, energizing in the morning, and trustworthy at all times.

The visual language is built around a transition from sleep to wakefulness:

- deep midnight indigo for sleep/night states;
- electric liquid amber for wake/primary actions;
- soft lavender for sleep-stage accents;
- holographic pearl for light surfaces and glass effects.

The UI should feel premium and fluid, but production usability takes priority over decorative effects.

## Principles

1. **Wake time first** — the personalized wake window is the most important element on the home screen.
2. **Data stays understandable** — sleep-stage charts should be fluid but still readable.
3. **Private by design** — read-only, on-device processing should be visible without dominating the interface.
4. **Comfortable touch targets** — interactive rows and buttons should target at least 48dp height.
5. **Dark and light mode are first-class** — neither should look like an afterthought.

## Color tokens

These are starting values, not final brand lockups.

### Dark mode

- Background: `#080D1F`
- Surface: `#11172C`
- Surface elevated: `#171E36`
- Primary text: `#F7F8FF`
- Secondary text: `#AEB7D4`
- Indigo: `#6F63FF`
- Lavender: `#9A84FF`
- Amber: `#FFB44A`
- Amber bright: `#FFD17A`
- Pearl: `#E9ECFF`

### Light mode

- Background: `#F8F9FF`
- Surface: `#FFFFFF`
- Surface elevated: `#F1F3FB`
- Primary text: `#15192A`
- Secondary text: `#68708A`
- Indigo: `#6056E8`
- Lavender: `#9B8CF4`
- Amber: `#F2A43B`
- Pearl: `#E8EAF5`

## Typography

Use the Android system sans/Roboto family initially for zero-cost distribution and consistent rendering.

Recommended scale:

- Display wake time: 40–48sp, semibold
- Screen title: 28–32sp, semibold
- Card title: 18–20sp, medium/semibold
- Body: 14–16sp
- Supporting metadata: 12–13sp

Avoid overly thin weights for important health/sleep information.

## Core components

### WakeWindowCard

Contains:

- personalized wake window;
- optional “in X min” helper;
- confidence/readiness copy;
- primary “I’m awake” CTA.

The CTA should use the amber accent and a subtle glow in dark mode. Animation should be low-frequency and unobtrusive.

### SleepStageChart

Production implementation should keep the organic visual language without falsifying data.

Preferred approach:

- render each real sleep-stage interval exactly;
- use rounded corners and softened transitions;
- optionally overlay a subtle smoothed path for visual continuity;
- never imply interpolated sleep stages that are not present in the source data.

Stage colors:

- Deep — indigo
- Light — lavender/blue
- REM — amber
- Awake — pearl/neutral

### SleepStageRow

Each stage gets its own rounded row/card with:

- color indicator;
- stage name;
- duration;
- percentage;
- optional short explanation;
- chevron when detail is available.

Minimum row height: 56dp.

### PrivacyBanner

Single compact banner:

**Processed on your device**

Supporting copy:

> Read-only access. Your sleep data stays on this device.

Use an indigo/lavender monochrome shield or lock. Do not introduce unrelated green as the primary privacy accent.

### ConnectionStatus

Shows the source or ecosystem currently contributing sleep records, for example:

- Health Connect — Connected
- Source: Fitbit

Health Connect is the Android data layer. Fitbit should only be shown as directly connected if WakeSync can verify it as the data origin or if a direct Fitbit integration is added later.

## Motion

Motion should reinforce state, not decorate every element.

Good uses:

- subtle amber CTA pulse when wake action is available;
- soft card elevation on press;
- chart reveal animation;
- smooth theme transitions.

Avoid:

- continuous large background movement;
- aggressive pulsing;
- animation that could interfere with someone using the app immediately after waking.

## Accessibility

- 48dp minimum interactive target.
- Do not communicate sleep stages by color alone.
- Support Android font scaling.
- Maintain WCAG-appropriate text contrast.
- Respect reduced-motion preferences.
- Wake alarm interactions must remain usable without fine motor precision.
