# App Preferences

Bright Flashlight stores a few user interface choices locally so the app feels consistent when reopened. This is intentionally lightweight and private to the device.

## Storage choice

The app uses **Preferences DataStore** for small key/value preferences.

Current helper:

```text
app/src/main/java/top/thinapps/brightflashlight/prefs/AppPreferences.kt
```

Current package:

```text
top.thinapps.brightflashlight.prefs
```

Current DataStore name:

```text
bright_flashlight_preferences
```

## Why DataStore

Preferences DataStore is used because these values are simple local preferences, not structured app data.

This avoids:

- user accounts
- remote databases
- local SQLite/Room tables
- a settings screen just to remember normal UI choices

The saved values stay on the device and are private to the app. They are cleared if the user uninstalls the app or clears app data.

## Saved values

Current saved values:

| Key | Type | Default | Purpose |
| --- | --- | --- | --- |
| `last_mode` | String | `TORCH` | Restores the last selected flashlight mode. |
| `auto_off_minutes` | Int | `0` | Restores the selected Auto-off option. |
| `strobe_speed` | Int | `2` | Restores the selected Strobe speed in hertz. Supported preset values are `1`, `2`, `3`, `4`, and `6`. |
| `screen_light_r` | Int | `255` | Restores the red channel for Screen Light. |
| `screen_light_g` | Int | `255` | Restores the green channel for Screen Light. |
| `screen_light_b` | Int | `255` | Restores the blue channel for Screen Light. |
| `screen_light_preset` | String | none | Restores the selected Screen Light preset button when a preset was tapped. |

## Strobe speed presets

`strobe_speed` stores the selected preset as the actual hertz value, not the slider index.

Supported values:

```text
1, 2, 3, 4, 6
```

The canonical strobe preset mapping lives in:

```text
app/src/main/java/top/thinapps/brightflashlight/torch/StrobeSpeedPreset.kt
```

Use `StrobeSpeedPreset` for:

- slider value to hertz mapping
- hertz to slider value mapping
- saved preference normalization
- service interval calculation

This keeps `MainActivity`, `AppPreferences`, and `TorchService` aligned when strobe behavior changes.

## Intentionally not saved

The app should **not** save active flashlight state.

Do not persist:

- `torchOn`
- `strobeRunning`
- `sosRunning`
- whether the foreground service was running
- whether the physical torch was active

Reason: reopening the app should not unexpectedly turn on the flashlight. Saved preferences should restore choices, not active power state.

## Restore behavior

On app launch, `MainActivity` restores:

- selected mode
- Auto-off option
- Strobe speed

On Screen Light launch, `ScreenLightActivity` restores:

- last RGB color
- selected preset button, when the saved color came from a preset

## Save behavior

The app saves preferences when the user changes normal UI choices:

- changing mode saves `last_mode`
- changing Auto-off saves `auto_off_minutes`
- changing Strobe speed saves `strobe_speed`
- tapping a Screen Light preset saves RGB values and `screen_light_preset`
- manually changing Screen Light RGB sliders saves RGB values and clears `screen_light_preset`

## Adding new preferences

When adding a new remembered choice:

1. Add a new key in `AppPreferences.Keys`.
2. Add a field to `SavedPreferences` with a safe default.
3. Add restore behavior in the relevant Activity.
4. Add save behavior only when the user explicitly changes the UI value.
5. Do not save active flashlight/session state.

Use plain snake_case keys and safe defaults.

## Testing checklist

Before shipping preference changes, test:

- change mode, close app, reopen app
- change Auto-off, close app, reopen app
- change Strobe speed, close app, reopen app
- confirm Strobe speed restores to the same named preset
- change Screen Light preset, close Screen Light, reopen Screen Light
- change Screen Light sliders manually, close Screen Light, reopen Screen Light
- confirm manual Screen Light slider changes clear the selected preset button
- confirm flashlight does not turn on automatically after reopening the app
