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
| `auto_off_minutes` | Int | `0` | Restores the selected Auto-off option. Supported values are `0`, `5`, `15`, `30`, and `60`. |
| `strobe_speed` | Int | `3` | Restores the selected Strobe speed in hertz. Supported preset values are `1`, `2`, `3`, `4`, and `5`. |
| `screen_light_r` | Int | `255` | Restores the red channel for Screen Light. |
| `screen_light_g` | Int | `255` | Restores the green channel for Screen Light. |
| `screen_light_b` | Int | `255` | Restores the blue channel for Screen Light. |
| `screen_light_preset` | String | none | Restores the selected Screen Light preset tile when a preset was tapped. |

## Auto-off presets

`auto_off_minutes` stores the selected Auto-off duration in minutes.

Supported values:

```text
0, 5, 15, 30, 60
```

These represent:

- `0`: Off
- `5`: 5 minutes
- `15`: 15 minutes
- `30`: 30 minutes
- `60`: 1 hour

Do not save arbitrary Auto-off durations unless the UI intentionally changes away from fixed presets.

Saved Auto-off values should be normalized when restored. If an unsupported value is found, the app should fall back to `0` (`Off`) and keep the matching Auto-off button checked so the bar never appears with no selected option.

Auto-off should save only when the user makes a real unlocked choice. While a light mode is active and Auto-off is locked/dimmed, touches should not change `auto_off_minutes` or write a new saved preference.

## Strobe speed presets

`strobe_speed` stores the selected preset as the actual hertz value, not the slider index.

Supported values:

```text
1, 2, 3, 4, 5
```

The default is `3` (`Medium`).

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

## Screen Light presets

Screen Light uses fixed color preset tiles instead of exposing RGB sliders on the main Screen Light panel.

Supported user-facing presets:

```text
White, Soft White, Study White, Warm White, Candle, Amber, Orange, Red, Rose, Magenta, Violet, Indigo, Blue, Sky Blue, Cyan, Turquoise, Green, Soft Green, Lime, Gray
```

The current palette is intentionally balanced across utility light, reading/study light, night light, emergency/alert colors, and playful color testing. Avoid adding multiple near-duplicate warm, cyan, or green tones unless the preset grid is intentionally expanded.

Tapping a Screen Light preset saves:

- the preset key in `screen_light_preset`
- the matching RGB values in `screen_light_r`, `screen_light_g`, and `screen_light_b`

The user-facing labels can change without changing the saved preset keys. Keep the current keys stable unless a migration is intentionally added, because existing installs may already have one of those keys saved.

The RGB keys remain because they are the simplest persisted color format and preserve compatibility with older installs that may already have a custom RGB color saved. If an older saved color has no preset key, Screen Light can still display that saved color, but new user-facing color changes should come from the fixed preset tiles.

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
- selected preset tile, when the saved color came from a preset

## Save behavior

The app saves preferences when the user changes normal UI choices:

- changing mode saves `last_mode`
- changing Auto-off while unlocked saves `auto_off_minutes`
- changing Strobe speed saves `strobe_speed`
- tapping a Screen Light preset saves RGB values and `screen_light_preset`

Locked or restored UI state should not save preferences as if the user changed them. In particular, Auto-off should not save when the dimmed locked bar rejects touches while Torch, Strobe, or SOS is active.

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
- confirm Auto-off restores to the same selected duration, including `1h`
- confirm Auto-off always restores to a checked option, with invalid stored values falling back to `Off`
- confirm locked/dimmed Auto-off touches do not change or save `auto_off_minutes`
- change Strobe speed, close app, reopen app
- confirm Strobe speed restores to the same named preset
- change Screen Light preset, close Screen Light, reopen Screen Light
- confirm Screen Light restores the selected preset tile and matching color
- confirm flashlight does not turn on automatically after reopening the app
