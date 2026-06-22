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
- folded one-value Screen Light color and dimension files back into the shared resource files

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
- matched the Screen Light swatch panel scrim to 45% black
- added a floating top-left Screen Light back button without adding a header
- kept the Screen Light swatches as clean borderless square color blocks

### 0.9.2
- changed Screen Light color swatches to square corners
- expanded Screen Light from sixteen fixed color presets to twenty common color presets
- kept the Screen Light swatch grid unlabeled and accessibility-named

### 0.9.1
- removed visible labels from the Screen Light color swatches while keeping accessibility names intact
- replaced the rail-style Screen Light swatch group with compact unlabeled rectangle tiles
- expanded Screen Light from four fixed color presets to sixteen common color presets
- removed the extra `Presets` label from the Screen Light panel
- removed unused Screen Light swatch text color resources after the labels were removed

### 0.9.0
- converted `ScreenLightActivity.kt` from manual `findViewById` view lookup to generated View Binding
- moved the Screen Light color label into a formatted string resource instead of building it with Kotlin string concatenation
- replaced always-visible Screen Light RGB sliders with simple fixed color preset tiles
- kept Screen Light color restore, preset selection, and keep-screen-on behavior intact

### 0.8.10
- updated the launcher icon foreground PNG while keeping the existing adaptive launcher icon wiring
- confirmed the launcher manifest icon still points through the existing `@mipmap/ic_launcher` resource

### 0.8.9
- renamed the shared segmented control rail drawable from `bg_mode_switch_rail.xml` to `bg_segmented_control_rail.xml`
- renamed the shared segmented control button color selector from `mode_switch_button_bg.xml` to `segmented_control_button_bg.xml`
- rewired the Mode and Auto-off segmented controls to use the new shared rail drawable and button color names
- removed the old narrow mode rail drawable and mode button color selector after confirming layout references were updated
- moved hardcoded resource color literals into `colors.xml` and rewired layouts, drawables, vectors, and color selectors to named colors
- added `dimens.xml` for shared layout, typography, slider, power button, dialog, and drawable dimensions
- removed the unused `AppCard` MaterialCardView style and theme hook from `themes.xml`
- kept the existing visual design and control behavior unchanged while cleaning resource names and orphans

### 0.8.8
- cleaned `MainActivity.kt` by splitting the large startup setup into focused helper methods
- organized Torch, Strobe, and SOS power button handling into smaller mode-specific start and stop helpers
- added named constants for countdown timing, Auto-off values, Strobe slider bounds, brightness label ratios, disabled alpha, and default torch strength
- cleaned service intent extras by importing the torch intensity extra directly alongside the other service extras
- kept the existing permission flow, Auto-off countdown, Strobe/SOS behavior, brightness controls, Screen Light launch, and warning dialog behavior unchanged

### 0.8.7
- cleaned `TorchService.kt` with clearer service state, intensity, and notification naming
- moved foreground notification channel setup into a dedicated helper while keeping the same persistent torch notification behavior
- added named constants for Auto-off timing, default torch intensity, notification request code, and SOS pattern timing
- simplified Auto-off scheduling math and removed duplicate handler cleanup while keeping Torch, Strobe, SOS, and notification shutdown behavior unchanged
- cleaned `ScreenLightActivity.kt` by splitting slider setup and preset button setup into focused helper methods
- reduced Screen Light RGB slider preference writes by saving custom colors when dragging stops while keeping live color preview
- added named Screen Light preset/color constants and locale-stable hex color formatting

### 0.8.6
- cleaned `AndroidManifest.xml` by removing the manifest package attribute and stale optional notification comment
- kept Camera and foreground service declarations focused on the active torch service behavior
- hardened saved preferences so invalid mode, Auto-off, Strobe Speed, and Screen Light color values fall back to supported defaults
- added a safe DataStore read fallback so preference restore can recover from temporary local read errors
- simplified Strobe Speed preset mapping with named constants for the five supported slider stops
- cleaned `TorchController.kt` by removing the reflection-based torch strength shim and using the Android 13+ API directly
- reset cached camera state when a camera ID becomes invalid so hardware detection can be retried cleanly

### 0.8.5
- removed the unused Camera permission rationale string after keeping the simpler built-in Android permission dialog plus inline recovery message flow
- removed legacy Jetifier from `gradle.properties` and kept the simple root Gradle buildscript setup
- tightened the Android release workflow with safer Gradle download and keystore restore steps while keeping manual GitHub Actions releases simple
- deleted the unused empty `app/proguard-rules.pro` file because release minification and resource shrinking are disabled
- cleaned `app/build.gradle` by removing unused vector drawable support config and broad `META-INF/*` packaging exclusions

### 0.8.4
- cleaned Auto-off service scheduling so the service uses one reusable Auto-off check instead of stacking repeated delayed checks
- moved the Brightness value labels into `activity_main.xml` and aligned Brightness slider header spacing with Strobe Speed
- removed the custom Quick Settings tile because Android already provides a built-in flashlight tile and this app is focused on the full in-app control surface
- cleaned temporary Gradle verbosity settings from `gradle.properties`
- removed unused AndroidX dependencies and leftover unused strings that were not referenced by code or layouts

### 0.8.3
- renamed Strobe Speed labels to Very Slow, Slow, Medium, Fast, and Very Fast
- changed the default Strobe Speed to Medium (3 Hz)
- added adaptive Brightness labels so supported devices show Low, Medium, High, or Max based on their own torch strength range

### 0.8.2
- fixed Strobe Speed active/dimmed state syncing when switching between Torch, Strobe, and SOS
- hardened the Strobe Speed active and preview layers so only the active Strobe slider can receive touches

### 0.8.1
- restored the 0.7.5 home-screen visual design, including the original control layout, colors, icon treatment, and footer placement
- kept the simplified `ic_mobile.xml`, `ic_power.xml`, and `ic_warning.xml` filenames while restoring the 0.7.5 icon visuals
- retained the 0.8.0 locked Auto-off behavior fixes while restoring the 0.7.5 visual design

### 0.8.0
- hardened locked Auto-off behavior so the dimmed Auto-off bar rejects touches while a light mode is active
- enforced a valid Auto-off selection so the Auto-off bar cannot end up with nothing highlighted
- normalized unsupported saved Auto-off values back to `Off`
- kept locked Auto-off changes from saving new preferences while Torch, Strobe, or SOS is active
- cleaned and renamed the warning, main power, and mobile icons as standard Material vectors with layout-controlled sizing and tinting

### 0.7.5
- moved the Strobe Speed warning icon immediately after the Strobe Speed title so it reads as part of the label
- kept the Strobe Speed value flush right in the header with no trailing right gap
- mirrored the warning icon and speed value in the dimmed Strobe Speed preview state
- kept the dimmed warning icon wired to the same flashing-light warning modal
- synced the dimmed Strobe Speed value with the selected/current Strobe Speed value

### 0.7.4
- reduced the visible Strobe Speed warning icon size while keeping its tap area stable
- fixed blurry dimmed Strobe Speed rendering by preventing the active slider from overlapping the preview slider
- moved the changing Strobe Speed value to the right side of the header immediately before the warning icon

### 0.7.3
- fixed the Strobe Speed warning modal title so it reliably appears on devices where the default dialog title was suppressed
- kept the Strobe Speed warning icon space stable across modes so switching modes no longer causes the layout to jump
- moved the Strobe Speed warning icon inward from the right edge so it has more breathing room near the slider
- kept the selected Auto-off value highlighted while the locked Auto-off section is dimmed

### 0.7.2
- restored the Strobe Speed warning modal title so the dialog shows both a bold title and content paragraph
- updated the warning title and message strings for clearer flashing-light sensitivity guidance
- reduced the visible Strobe Speed warning icon size while keeping the same tap area

### 0.7.1
- added a Strobe Speed warning icon that opens a simple flashing-light sensitivity warning modal
- added a local warning vector icon plus warning title and message strings for the modal
- dimmed the locked Auto-off section while a light mode is active so disabled controls match the slider placeholder style
