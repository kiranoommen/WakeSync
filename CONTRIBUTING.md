<p align="center">
  <img src="docs/assets/wakesync-mark.svg" alt="WakeSync mark" width="110" />
</p>

# 🤝 Contributing to WakeSync

WakeSync is still early-stage, so contributions should stay focused on reliability, understandable behavior, privacy, and Android compatibility.

## Before changing behavior

Read:

- [Architecture](docs/ARCHITECTURE.md)
- [Wake algorithm](docs/WAKE_WINDOW_ALGORITHM.md)
- [Design system](docs/DESIGN_SYSTEM.md)
- [Brand guide](docs/BRANDING.md)
- [Privacy model](docs/PRIVACY.md)
- [Testing plan](docs/TESTING.md)

## Local build

Requirements:

- JDK 17
- Android SDK with API 36 installed
- Gradle compatible with the Android Gradle Plugin used by the project

Build the debug APK with:

```bash
gradle :app:assembleDebug
```

## Branch naming

Use short, descriptive branches:

- `feature/live-sleep-refresh`
- `fix/deadline-alarm`
- `docs/health-connect-notes`
- `test/predictive-fallback`

## Pull requests

A PR should:

- describe the user-visible behavior being changed;
- call out alarm/reliability implications;
- call out Health Connect permission changes;
- include testing notes;
- pass Android CI;
- avoid unrelated refactors.

For alarm-path changes, explicitly verify that the independent hard-stop alarm still works.

## Health data

Never commit:

- exported personal Health Connect data;
- screenshots containing identifiable health information;
- device identifiers tied to a real person;
- secrets, API keys, tokens, or account credentials.

Use synthetic or clearly anonymized test data in issues, tests, screenshots, and documentation.

## Product claims

Do not add copy implying WakeSync directly measures cortisol, clinically diagnoses sleep, or knows a biologically perfect wake time.

Consumer sleep stages are treated as a scheduling signal, not a medical measurement.

## Style

- Keep Kotlin straightforward and testable.
- Prefer deterministic alarm behavior over cleverness.
- Keep critical fallback logic easy to audit.
- Preserve the user-defined earliest wake and hard deadline.
- Keep health-data processing on-device unless a future architecture change is explicitly reviewed.
