# Main Controls

Bright Flashlight keeps the home screen focused on fast flashlight control instead of behaving like a scrolling settings page.

## Layout behavior

The main screen uses a `ScrollView` with `fillViewport="true"` so the controls can behave like a fixed tool on normal phone screens while still protecting against clipping on edge cases.

Expected behavior:

- normal portrait phones: the control stack sits near the bottom of the screen with little or no scrolling
- tiny screens, landscape, split-screen, large accessibility text, or OEM display scaling: the `ScrollView` remains available as a fallback
- the old dynamic power button centering code should not be reintroduced

The power button should be positioned by the XML layout, not by runtime spacer calculations in `MainActivity.kt`.

## Control visibility

Torch-related controls require Camera permission and a usable device flash.

When Camera permission or flash support is missing:

- Torch, Strobe, SOS, Brightness, Strobe Speed, and Auto-off stay disabled or hidden
- Screen Light remains available because it does not require Camera permission

## Brightness

Brightness is only shown when:

- Torch mode is selected
- Camera permission exists
- a usable flash exists
- the device reports torch strength support
- the max torch strength is greater than `1`

Brightness values are real device torch strength levels from Camera2, not a fake `1..10` scale.

The activity sets the slider range to the device-supported strength range and sends the selected strength level to `TorchService`. The service should clamp that value to the current device max strength and pass it through to `TorchController`.

Do not remap brightness through an old generic UI scale in the service.

## Strobe Speed

Strobe Speed uses five preset values:

| Slider value | Label | Hertz |
| --- | --- | --- |
| `0` | Slow | `1 Hz` |
| `1` | Medium | `2 Hz` |
| `2` | Alert | `3 Hz` |
| `3` | Fast | `4 Hz` |
| `4` | Max | `6 Hz` |

The canonical mapping lives in:

```text
app/src/main/java/top/thinapps/brightflashlight/torch/StrobeSpeedPreset.kt
```

Use this helper when adding or changing strobe behavior so the activity, preferences, and service stay in sync.

## Stable lower controls

Brightness and Strobe Speed intentionally reserve their layout areas with dim placeholder controls underneath the active overlay. This keeps the lower half of the screen stable when switching modes or when a control is unsupported.

Auto-off is different. It is a normal always-relevant flashlight setting once torch controls are available, so it should remain a single normal section without a duplicate dim placeholder.

## Auto-off

Auto-off applies to Torch, Strobe, and SOS modes.

The supported options are:

- Off
- 5 minutes
- 15 minutes
- 30 minutes
- 1 hour

Changing Auto-off while a light mode is running should update the running service state without changing the selected mode.

## Testing checklist

Before shipping main-control changes, test:

- normal portrait screen has little or no scrolling
- landscape or split-screen remains scrollable instead of clipping
- large accessibility text remains scrollable instead of clipping
- Torch mode shows Brightness only on devices with torch strength support
- Strobe mode shows Strobe Speed and updates while running
- SOS mode hides Brightness and Strobe Speed but keeps Auto-off available
- Auto-off remains a single normal section, not a duplicate placeholder section
- Auto-off options show `Off`, `5m`, `15m`, `30m`, and `1h`
- Screen Light remains available without Camera permission
