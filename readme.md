# Bright Flashlight

Bright Flashlight is a focused Android utility for controlling the device flash with Torch, Strobe, and SOS modes. Supported devices can also adjust torch brightness, choose among five fixed strobe speeds, and set an automatic shutoff timer.

For devices without a usable camera flash—or when a softer light is needed—the built-in Screen Light turns the display into a full-screen lamp with a compact set of color presets.

The app keeps preferences locally and is designed as a simple privacy-friendly tool with no accounts, ads, analytics, or Internet permission.

## Documentation

| Document | Description |
| --- | --- |
| [Main Controls](docs/main-controls.md) | Details the home-screen layout and behavior for Torch, Strobe, SOS, Screen Light, Brightness, Auto-off, accessibility, and control testing. |
| [App Preferences](docs/app-preferences.md) | Documents locally saved mode, brightness, Strobe Speed, Auto-off, and Screen Light choices, including validation and restore behavior. |
| [Build](docs/build.md) | Records the Android and Gradle toolchain, GitHub Actions release workflow, signing setup, and the decisions around R8 and resource shrinking. |
| [Design](docs/design.md) | Defines visual and interaction rules for layout rhythm, icons, touch targets, dimmed states, the power control, and Screen Light. |
| [Permissions](docs/permissions.md) | Explains Camera permission requirements, flash-hardware detection, denied or unavailable states, recovery behavior, and the Screen Light fallback. |
| [Scope](docs/scope.md) | Defines the supported product surface, reliability priorities, deferred ideas, and intentional boundaries that keep the app focused. |
| [Google Play](docs/google-play.md) | Tracks the unfinished Google Play listing and keeps unconfirmed store details clearly marked until the listing is ready. |
| [Changelog](changelog.md) | Lists every released version and the complete history of fixes, features, interface changes, reliability work, and build updates. |
