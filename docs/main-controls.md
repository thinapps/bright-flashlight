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

A reserved countdown pill lives above the ON/OFF label. It stays invisible when no Auto-off countdown is active so the power button layout does not shift. When a light mode is running and Auto-off is enabled, the slot shows a compact dark glass `MM:SS` pill with `4dp` spacing above and below.

The ON power button state can use a soft warm radial haze as a subtle active-light accent. Keep it static, separate from the actual button face, and free of hard rings, hard arc strokes, button-face insets, or animation unless the design is intentionally revisited.

The active halo should sit behind the button face, which naturally covers the glow center so the halo does not render as a ring or donut.

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

The active Brightness overlay is only shown when:

- Torch mode is selected
- Camera permission exists
- a usable flash exists
- the device reports torch strength support
- the max torch strength is greater than `1`

Brightness values are real device torch strength levels from Camera2, not a fake `1..10` scale.

The activity sets the slider range to the device-supported strength range and sends the selected strength level to `TorchService`. The service should clamp that value to the current device max strength and pass it through to `TorchController`.

When torch strength control is supported, user brightness changes should save locally as `torch_strength_level`. On restore, apply the saved level only after the device max strength is known, clamp it into `1..maxStrength`, and keep fresh installs defaulting to max brightness.

The visible Brightness slider labels start at `1` and end at the device-reported max strength. `0` should never be shown because it implies off, not low brightness.

The Brightness header should mirror the Strobe Speed header pattern by keeping the title on the left and the current value label flush right. Use friendly relative words instead of raw device strength numbers.

Brightness value labels should adapt to the current device max strength:

| Device max strength | Labels |
| --- | --- |
| `1` or unsupported | `Max` |
| `2` | `Low`, `Max` |
| `3` | `Low`, `Medium`, `Max` |
| `4+` | `Low`, `Medium`, `High`, `Max` |

For `4+` strength levels, `Max` is reserved for the exact maximum value. Other values should be bucketed relative to the device's own supported range, so the words describe the current value honestly without implying every phone has the same hardware levels.

The dimmed Brightness placeholder should always show `Max`. This keeps the inactive placeholder simple and avoids presenting a stale or fake adjustable value when Brightness is unavailable or not active.

Do not remap brightness through an old generic UI scale in the service.

## Strobe Speed

Strobe Speed uses five linear preset values. Its visible slider value label should show `1` through `5`, because `0` implies off even though the slowest preset is still active.

The default Strobe Speed is `Medium (3 Hz)`. This keeps the default centered in the five-step scale while avoiding the most intense flashing rates.

| Visible slider label | Label | Hertz |
| --- | --- | --- |
| `1` | Very Slow | `1 Hz` |
| `2` | Slow | `2 Hz` |
| `3` | Medium | `3 Hz` |
| `4` | Fast | `4 Hz` |
| `5` | Very Fast | `5 Hz` |

The dimmed Strobe Speed preview should always mirror the saved/current Strobe Speed value. Do not leave it hardcoded to the default, because that can surprise users when they switch into Strobe mode.

The Strobe Speed header keeps the title fixed on the left, places the warning icon immediately after the title, and keeps the changing speed value flush right. The dimmed Strobe Speed preview should mirror this header so the warning icon and current value remain visible even when Strobe mode is not selected.

The active Strobe Speed warning icon opens a simple modal with a bold title and a content paragraph warning about flashing light sensitivity and photosensitive epilepsy. The warning icon in the dimmed preview remains visible but non-interactive. Do not replace the modal with an onboarding notice, persistent banner, or stored dismissal state.

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

Brightness uses a `32dp` header row. Strobe Speed uses a `48dp` header row so its warning control has a full accessible touch target. Both keep a `4dp` gap, `56dp` slider well, and `48dp` slider height.

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

Locked dimmed controls must not remain interactive. Auto-off is visually dimmed and locked while a light mode is active; touches on the Auto-off group and its child buttons should be consumed before they can change or clear the selected value.

## Auto-off

Auto-off applies to Torch, Strobe, and SOS modes.

The supported options are:

- Off
- 5 minutes
- 15 minutes
- 30 minutes
- 1 hour

One Auto-off option must always be selected. If the saved value is invalid or an interaction attempts to clear the selection, the app should normalize back to a supported option and re-check the matching button. The fallback for unsupported values is `Off`.

Once a light mode is turned on, Auto-off controls are locked until the light mode is turned off. This prevents accidental timer changes while the flashlight is already running.

When locked, the whole Auto-off section dims to `0.45` alpha so it matches the visual language used by the disabled Brightness and Strobe Speed placeholder controls. The selected Auto-off value should remain highlighted inside the dimmed section so users can still see the active timer choice.

The locked Auto-off section should preserve the selected value visually but reject interaction. Touches on the dimmed Auto-off bar or any Auto-off option should not change the value, clear the checked option, trigger haptics, or save a new preference.

When Auto-off is enabled before starting Torch, Strobe, or SOS, the power button shows a compact dark glass `MM:SS` countdown pill between the icon and the ON/OFF label. The countdown is Activity-side UI only; `TorchService` still owns the real shutdown timer.

`TorchService` should use a single reusable Auto-off check runnable and clear any pending check before posting the next one. Do not stack anonymous delayed Auto-off checks.

## External torch interruption

Android can turn off or temporarily reserve the flash when another app opens the camera or higher-priority camera resources are needed.

`TorchService` should observe the selected flash camera while it exists. If steady Torch is externally switched off, or the flash becomes unavailable while Torch, Strobe, or SOS is active, the service should stop and clear its active state. Strobe and SOS must also stop if any scheduled flash pulse fails instead of continuing a pattern that is no longer controlling the hardware.

Normal Strobe and SOS off-pulses are intentional and must not be mistaken for an external interruption. When the user returns to the app after an interruption, the activity should restore the cleared service state and show the power control as off.

## Testing checklist

Before shipping main-control changes, test:

- normal portrait screen has little or no scrolling
- full-width controls align to a `24dp` effective screen gutter
- Screen Light appears as a real top-right `Screen` overlay pill, not as a full-width primary button or part of the bottom stack
- Screen Light remains available without Camera permission
- power, Screen, Mode, Auto-off, Brightness, and Strobe Speed provide haptic feedback on user interactions
- Torch brightness restores to the saved level on supported devices after closing and reopening the app
- power button ON/OFF label is clear at `18sp`
- power button keeps the countdown slot above ON/OFF without shifting the layout
- Auto-off countdown appears as a compact dark glass `MM:SS` pill only when a light mode is active and Auto-off is enabled
- ON power button glow appears only in the active ON state as a separated soft radial haze around the upper 75% of the button, not a hard arc, border, face-size change, or donut ring
- Auto-off controls lock while Torch, Strobe, or SOS is active, dim to match the disabled slider placeholders, keep the selected Auto-off value highlighted, reject all touches while locked, then unlock and return to full opacity when the light mode stops
- Auto-off always has exactly one selected option; no interaction should leave the Auto-off bar with nothing highlighted
- all intentional dimmed sections use `0.45` alpha unless a documented reason says otherwise
- Brightness value is flush right in the header and updates as Low, Medium, High, or Max based on device-supported strength levels
- dimmed or unsupported Brightness placeholder shows `Max`
- Brightness uses a `32dp` header and Strobe Speed uses a `48dp` header while both retain the same well and slider dimensions
- active Strobe Speed warning icon appears immediately after the Strobe Speed title and opens a simple warning modal with a bold title and content paragraph when tapped
- dimmed Strobe Speed preview warning icon remains visible but does not accept taps
- Strobe Speed value is flush right in the header with no trailing right gap
- dimmed Strobe Speed preview mirrors the warning icon and saved/current speed value before Strobe mode is enabled
- Brightness value bubble starts at `1`, not `0`, on devices with torch strength support
- Strobe Speed value bubble shows `1` through `5`, not `0` through `4`
- Strobe Speed defaults to `Medium (3 Hz)`
- Strobe Speed presets map linearly to `1 Hz`, `2 Hz`, `3 Hz`, `4 Hz`, and `5 Hz`
- slider wells look slightly recessed/beveled without overpowering the power button
- slider wells and the Screen pill do not use visible strokes
- landscape or split-screen remains scrollable instead of clipping
- large accessibility text remains scrollable instead of clipping
- Torch mode shows the active Brightness overlay only on devices with torch strength support
- Strobe mode shows Strobe Speed and updates while running
- SOS mode hides Brightness and Strobe Speed but keeps Auto-off available
- opening another camera app while Torch is active stops the service and restores the power control to off when returning
- opening another camera app while Strobe or SOS is active stops the pattern instead of leaving stale active state
- normal Strobe and SOS off-pulses continue without being treated as external interruption
- Auto-off remains a single normal section, not a duplicate placeholder section
- Auto-off options show `Off`, `5m`, `15m`, `30m`, and `1h`
