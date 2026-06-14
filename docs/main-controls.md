# Main Controls

Bright Flashlight keeps the home screen focused on fast flashlight control instead of behaving like a scrolling settings page.

## Layout behavior

The main screen uses a `ScrollView` with `fillViewport="true"` so the controls can behave like a fixed tool on normal phone screens while still protecting against clipping on edge cases.

Expected behavior:

- normal portrait phones: the control stack sits near the bottom of the screen with little or no scrolling
- tiny screens, landscape, split-screen, large accessibility text, or OEM display scaling: the `ScrollView` remains available as a fallback
- the old dynamic power button centering code should not be reintroduced

The power button should be positioned by the layout, not by runtime spacer calculations in `MainActivity.kt`.

The effective horizontal screen gutter for the main controls is `24dp`, matching the visual rhythm used by Recover Deleted Photos. Keep the full-width rails and buttons aligned to that gutter unless a future layout redesign intentionally changes the app-wide spacing.

## Control visibility

Torch-related controls require Camera permission and a usable device flash.

When Camera permission or flash support is missing:

- Torch, Strobe, SOS, Brightness, Strobe Speed, and Auto-off stay disabled or hidden
- Screen Light remains available because it does not require Camera permission

## Secondary Screen shortcut

Screen Light is intentionally secondary to the main flashlight controls.

On the main screen, it should appear as a small top-right pill labeled `Screen`, not as a full-width primary button. Keep the existing `btnScreenLight` behavior, but avoid placing it in the main control stack where it can compete with Torch, Strobe, SOS, Auto-off, Brightness, or Strobe Speed.

The Screen Light destination can keep the full `Screen Light` title.

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

## Hardware-style slider wells

Brightness and Strobe Speed use a shared recessed well drawable:

```text
app/src/main/res/drawable/bg_slider_hardware_well.xml
```

The well should feel related to the main power button, but less visually dominant. Keep its 3D effect subtle: dark recessed slot, soft underside shadow, warm orange edge, and faint top highlight.

Do not make the slider wells brighter or more raised than the power button.

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
- full-width controls align to a `24dp` effective screen gutter
- Screen Light appears as a smaller top-right `Screen` pill, not a full-width primary button
- Screen Light remains available without Camera permission
- slider wells look slightly recessed/beveled without overpowering the power button
- landscape or split-screen remains scrollable instead of clipping
- large accessibility text remains scrollable instead of clipping
- Torch mode shows Brightness only on devices with torch strength support
- Strobe mode shows Strobe Speed and updates while running
- SOS mode hides Brightness and Strobe Speed but keeps Auto-off available
- Auto-off remains a single normal section, not a duplicate placeholder section
- Auto-off options show `Off`, `5m`, `15m`, `30m`, and `1h`
