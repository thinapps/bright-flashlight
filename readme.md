# Bright Flashlight

## Documentation

| Document | Description |
| --- | --- |
| [Main Controls](docs/main-controls.md) | Explains the main flashlight, screen light, mode, slider, and auto-off controls. |
| [App Preferences](docs/app-preferences.md) | Documents saved app preferences and how user choices persist locally. |
| [Build](docs/build.md) | Explains Gradle, R8, and ProGuard choices for the current GitHub Actions build. |
| [Design](docs/design.md) | Defines icon, dimming, layout, and visual design rules for the app. |
| [Permissions](docs/permissions.md) | Explains Camera permission behavior, fallback states, and recovery prompts. |

## Changelog

### 0.10.2
- aligned the XML default Strobe Speed slider position with the saved Medium default
- kept the active and dimmed Strobe Speed sliders starting at Medium (3 Hz) before preference restore completes
- enabled edge-to-edge layout handling in both activities for Android 15+ system bar behavior
- added main screen inset handling so the main content and Screen Light button avoid system bars and display cutouts
- added Screen Light inset handling so the bottom color tray and floating back button avoid system bars and display cutouts

### 0.10.1
- remembered whether the Screen Light color tray was collapsed or expanded when reopening Screen Light
- saved the Screen Light tray state locally only when the user taps the tray toggle
- remembered the selected Torch brightness level on devices with torch strength support
- restored saved Torch brightness only after clamping it to the current device max strength

### 0.10.0
- fixed Auto-off service updates so brightness and strobe changes no longer reset an active timer
- added a legacy launcher icon fallback for Android 7.x devices while keeping the Android 8+ adaptive icon path unchanged
- removed the transparent power halo gap cleanup layer, unused dimension, and dead drawable
- corrected SOS timing gaps so symbol, letter, and word pauses use separate durations
- moved Screen Light tray accessibility labels into string resources and removed small stale resource shims

### 0.9.10
- fixed the active power halo gradient so it no longer renders as a donut ring
- disabled the dark halo gap mask after testing showed it created a visible black ring around the power button
- kept the halo behind the unchanged 156dp power button while letting the button face naturally cover the glow center
- kept the active halo XML-only with no image assets

### 0.9.9
- enlarged the active power halo from 196dp to 228dp so the glow has enough room to bloom outside the 156dp button
- added a dark oval gap mask between the halo and the button so the glow no longer appears to touch the button edge
- widened the XML radial halo and reduced the lower crop so the glow wraps farther down the button while still fading out near the bottom
- increased the active halo alpha so the ON state reads clearly on the dark background

### 0.9.8
- moved the active power glow into a separate larger halo layer behind the unchanged 156dp power button
- kept the halo as a plain decorative view controlled by `MainActivity.kt` instead of a custom self-watching view
- kept the halo non-clickable while preserving the original power button touch target
- kept the ON power button background face-only so the button shape does not change between OFF and ON states

### 0.9.7
- restored the ON power button face sizing so the button does not visually shrink when active
- replaced the hard active glow arc strokes with a soft radial haze
- removed the bad ON glow inset dimensions that made the active button look smaller
- kept the active glow static and subtle so it reads like light spill instead of a border

### 0.9.6
- added a separated upper arc glow to the ON power button state
- added a dark glass Auto-off countdown pill inside the power button
- tightened countdown spacing to 4dp above and below while keeping the icon and ON/OFF label sizes unchanged

### 0.9.5
- changed the Screen Light back button ripple from translucent white to translucent black
- kept the Screen Light color swatch ripple behavior unchanged
- reduced the visible Screen Light back button oval and ripple to 40dp while keeping the 48dp tap target
- rebalanced the 20 Screen Light preset colors to reduce near-duplicates across warm, alert, blue-green, and playful tones
- updated Screen Light preset accessibility names to match the new color roles
- matched the Screen Light preset RGB values used when tapping swatches to the visible swatch colors
- defined the Screen Light back button background color resource explicitly

### 0.9.4
- fixed the Screen Light back button press feedback so it uses an oval ripple instead of a faint square foreground flash
- lightened the Screen Light back button background so it stays less distracting than the bottom control panel
- added tap haptic feedback to the Screen Light back button
- added tap haptic feedback to all Screen Light color swatches
- added a color-row chevron toggle to collapse and reopen the Screen Light color tray
- kept the floating back button position, size, icon, and behavior unchanged

### 0.9.3
- removed inactive and selected borders from Screen Light color swatches
- tightened Screen Light swatch spacing from 3dp to 2dp
