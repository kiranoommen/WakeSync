<p align="center">
  <img src="assets/wakesync-hero.svg" alt="WakeSync — Better mornings, in sync with you." width="100%" />
</p>

# WakeSync Documentation

<p align="center">
  <strong>Product, engineering, design, privacy, and validation docs for WakeSync.</strong>
</p>

<p align="center">
  <a href="../README.md">← Repository home</a>
</p>

## Explore

| | Document | What it covers |
| --- | --- | --- |
| 🎨 | **[Brand guide](BRANDING.md)** | Identity, voice, colors, logo usage, product claims, imagery |
| ✨ | **[Design system](DESIGN_SYSTEM.md)** | UI tokens, components, motion, accessibility |
| 🧭 | **[Architecture](ARCHITECTURE.md)** | Android stack, alarm layers, Health Connect flow |
| ⏰ | **[Wake algorithm](WAKE_WINDOW_ALGORITHM.md)** | Live → predictive → hard-stop decision logic |
| 🧪 | **[Testing plan](TESTING.md)** | Manual matrix, overnight validation, reliability checks |
| 🔒 | **[Privacy model](PRIVACY.md)** | On-device processing and Health Connect boundaries |
| 🗺️ | **[MVP roadmap](MVP_ROADMAP.md)** | Delivered milestones and next priorities |

## The product in one picture

<p align="center">
  <img src="assets/smart-wake-flow.svg" alt="WakeSync smart wake fallback ladder" width="100%" />
</p>

## Brand at a glance

<p align="center">
  <img src="assets/brand-palette.svg" alt="WakeSync brand palette" width="100%" />
</p>

WakeSync is intentionally built around a simple contract: use the best sleep signal available **inside the user's chosen wake range**, while preserving an independent hard deadline.

## Engineering priority

The project is currently in **real-device validation**. The key question is not whether more prediction logic can be added; it is whether live wearable sleep stages arrive in Health Connect with enough freshness and consistency to improve real mornings.

---

<p align="center">
  🌙 <strong>WakeSync</strong> · Better mornings, in sync with you.
</p>
