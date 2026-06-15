# Main Controls

Bright Flashlight keeps the home screen focused on fast flashlight control instead of behaving like a scrolling settings page.

## Layout behavior

The main screen uses a `ScrollView` with `fillViewport="true"` so the controls can behave like a fixed tool on normal phone screens while still protecting against clipping on edge cases.

Expected behavior:

- normal portrait phones: the control stack sits near the bottom of the screen with little or no scrolling
- tiny screens, landscape, split-screen, large accessibility text, or OEM display scaling: the `ScrollView` remains available as a fallback
- the old dynamic power button centering code should not be reintroduced

The power button should be positioned by the layout, not by runtime spacer calculations in `MainActivity.kt`.

The effective horizontal screen gutter for the main controls is `24dp`, matching the visual rhythm used by Recover Deleted Photos. This gutter is owned by `activity_main.xml`; do not recreate it with runtime Kotlin padding helpers.

## Power button

The main power control uses a clickable circular layout, not a plain text `Button`, so the icon, Auto-off countdown timer, and ON/OFF label can be positioned independently.

The ON/OFF label should stay large and simple. The current label size is `18sp`.

A reserved countdown slot lives above the ON/OFF label. It stays invisible when no Auto-off countdown is active so the power button layout does not shift. When a light mode is running and Auto-off is enabled, the slot shows a small white `MM:SS` countdown.

## Haptics

Main control interactions should feel tactile:

- Power button touch uses tap haptic feedback.
- Screen shortcut click uses tap haptic feedback.
- Mode and Auto-off changes use tap haptic feedback only for real user changes, not during saved-preference restore.
- Brightness and Strobe Speed sliders use the same tap haptic feedback as the other working controls on real user step changes.

## Control visibility

Torch-related controls require Camera permission and a usable device flash.

When Camera permission or flash support is missing:

- Torch, Strobe, SOS, Brightness, Strobe Speed, and Auto-off stay disabled or hidden
- Screen Light remains available because it does not require Camera permission

## Secondary Screen shortcut

Screen Light is intentionally secondary to the main flashlight controls.

On the main screen, it should appear as a real top-right overlay pill labeled `Screen`, not as a full-width primary button and not as a child of the bottom-anchored control stack. Keep the existing `btnScreenLight` behavior, but avoid placing it where it can compete with Torch, Strobe, SOS, Auto-off, Brightness, or Strobe Speed.

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

The visible Brightness slider labels start at `1` and end at the device-reported max strength. `0` should never be shown because it implies off, not low brightness.

Do not remap brightness through an old generic UI scale in the service.

## Strobe Speed

Strobe Speed uses five linear preset values. Its visible slider value label should show `1` through `5`, because `0` implies off even though the slowest preset is still active.

The default Strobe Speed is `Medium (2 Hz)`. Keep this default because it is safety-conscious: it is clearly noticeable, slower and less aggressive than the Alert/Fast/Max presets, and avoids starting users at the most intense flashing rate.

| Visible slider label | Label | Hertz |
| --- | --- | --- |
| `1` | Slow | `1 Hz` |
| `2` | Medium | `2 Hz` |
| `3` | Alert | `3 Hz` |
| `4` | Fast | `4 Hz` |
| `5` | Max | `5 Hz` |

The dimmed Strobe Speed preview should always mirror the saved/current Strobe Speed value. Do not leave it hardcoded to the default, because that can surprise users when they switch into Strobe mode.

The active Strobe Speed header includes a small warning icon on the far right. Tapping it opens a simple modal with a bold title and a content paragraph warning about flashing light sensitivity and photosensitive epilepsy. Do not replace this with an onboarding notice, persistent banner, or stored dismissal state.

The canonical speed mapping lives in:

```text
app/src/main/java/top/thinapps/brightflashlight/torch/StrobeSpeedPreset.kt
```

Use this helper when adding or changing strobe behavior so the activity, preferences, and service stay in sync.

## Hardware-style slider wells

Brightness and Strobe Speed use a shared recessed well drawable:

```text
app/src/main/res/drawable/bg_slider_hardware_well.xml
```

The well should feel related to the main power button, but less visually dominant. Keep its 3D effect subtle: dark recessed slot, soft underside shadow, and faint top highlight.

Do not make the slider wells brighter or more raised than the power button. Keep orange on the active slider fill and thumb, not on the well outline.

## Stable lower controls

Brightness and Strobe Speed intentionally reserve their layout areas with dim placeholder controls underneath the active overlay. This keeps the lower half of the screen stable when switching modes or when a control is unsupported.

Auto-off is different. It is a normal always-relevant flashlight setting once torch controls are available, so it should remain a single normal section without a duplicate dim placeholder.

## Dimmed controls

Intentional dimmed sections should all use `0.45` alpha on the whole section. Do not mix different dim amounts unless there is a clear accessibility or hierarchy reason.

This currently applies to:

- Brightness placeholder controls in `activity_main.xml`
- Strobe Speed placeholder controls in `activity_main.xml`
- inactive Strobe Speed controls in `MainActivity.kt`
- locked Auto-off controls in `MainActivity.kt`

Dimmed controls should preserve their selected or current value when possible. For Auto-off, the selected option should stay highlighted while the whole section dims, instead of falling into a fully disabled color state that hides the selected value.

## Auto-off

Auto-off applies to Torch, Strobe, and SOS modes.

The supported options are:

- Off
- 5 minutes
- 15 minutes
- 30 minutes
- 1 hour

Once a light mode is turned on, Auto-off controls are locked until the light mode is turned off. This prevents accidental timer changes while the flashlight is already running.

When locked, the whole Auto-off section dims to `0.45` alpha so it matches the visual language used by the disabled Brightness and Strobe Speed placeholder controls. The selected Auto-off value should remain highlighted inside the dimmed section so users can still see the active timer choice.

When Auto-off is enabled before starting Torch, Strobe, or SOS, the power button shows a small white `MM:SS` countdown above the ON/OFF label. The countdown is Activity-side UI only; `TorchService` still owns the real shutdown timer.

## Testing checklist

Before shipping main-control changes, test:

- normal portrait screen has little or no scrolling
- full-width controls align to a `24dp` effective screen gutter
- Screen Light appears as a real top-right `Screen` overlay pill, not a full-width primary button or part of the bottom stack
- Screen Light remains available without Camera permission
- power, Screen, Mode, Auto-off, Brightness, and Strobe Speed provide haptic feedback on user interactions
- power button ON/OFF label is clear at `18sp`
- power button keeps the countdown slot above ON/OFF without shifting the layout
- Auto-off countdown appears as small white `MM:SS` text only when a light mode is active and Auto-off is enabled
- Auto-off controls lock while Torch, Strobe, or SOS is active, dim to match the disabled slider placeholders, keep the selected Auto-off value highlighted, then unlock and return to full opacity when the light mode stops
- all intentional dimmed sections use `0.45` alpha unless a documented reason says otherwise
- Strobe Speed warning icon appears far right above the active Strobe Speed slider and opens a simple warning modal with a bold title and content paragraph when tapped
- Brightness value bubble starts at `1`, not `0`, on devices with torch strength support
- Strobe Speed value bubble shows `1` through `5`, not `0` through `4`
- Strobe Speed defaults to `Medium (2 Hz)`
- Strobe Speed presets map linearly to `1 Hz`, `2 Hz`, `3 Hz`, `4 Hz`, and `5 Hz`
- dimmed Strobe Speed preview mirrors the saved/current Strobe Speed value before Strobe mode is enabled
- slider wells look slightly recessed/beveled without overpowering the power button
- slider wells and the Screen pill do not use visible strokes
- landscape or split-screen remains scrollable instead of clipping
- large accessibility text remains scrollable instead of clipping
- Torch mode shows Brightness only on devices with torch strength support
- Strobe mode shows Strobe Speed and updates while running
- SOS mode hides Brightness and Strobe Speed but keeps Auto-off available
- Auto-off remains a single normal section, not a duplicate placeholder section
- Auto-off options show `Off`, `5m`, `15m`, `30m`, and `1h`
