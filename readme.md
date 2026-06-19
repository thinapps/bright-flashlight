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

### 0.9.5
- changed the Screen Light back button ripple from translucent white to translucent black
- kept the Screen Light color swatch ripple behavior unchanged
- reduced the visible Screen Light back button oval and ripple to 40dp while keeping the 48dp tap target

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

### 0.7.0
- added a small white Auto-off countdown above the ON/OFF label on the main power button while a light mode is active
- kept the countdown slot invisible when inactive so the power button layout does not shift
- locked Auto-off controls while Torch, Strobe, or SOS is active to prevent accidental timer changes

### 0.6.9
- changed Brightness and Strobe Speed slider haptics to use the same tap feedback as the other working controls
- changed the Strobe Speed value bubble to show `1` through `5` instead of `0` through `4`
- changed the Max Strobe Speed preset from `6 Hz` to `5 Hz` so the five presets are linear and safety-conscious
- documented `Medium (2 Hz)` as the safety-conscious default Strobe Speed
- synced the dimmed Strobe Speed preview slider with the saved/current Strobe Speed value
- increased the ON/OFF label size and reserved an invisible countdown slot above it inside the power button

### 0.6.8
- moved the `24dp` control gutter into XML and removed the runtime gutter normalizer
- removed secondary orange/neutral outlines from the Screen pill and slider wells so orange is reserved for active controls
- added haptic feedback to Screen, Mode, Auto-off, Brightness, and Strobe Speed interactions
- replaced the power button icon with a cleaner rounded power symbol

### 0.6.7
- matched the main control gutters to a `24dp` effective screen gutter
- restyled the Brightness and Strobe Speed wells with a subtle recessed bevel that matches the power button's hardware feel
- moved Screen Light to a real top-right `Screen` overlay pill so it no longer competes with the primary flashlight controls

### 0.6.6
- bottom-anchored the home controls so the app feels more like a fixed flashlight tool on normal phone screens
- kept the ScrollView as an emergency fallback for small screens, landscape, split-screen, and large accessibility text
- disabled the old dynamic centering spacer so it no longer pushes the power button toward the middle
- removed obsolete dynamic power button centering code from `MainActivity.kt`
- centralized Strobe speed preset mapping so the activity, preferences, and service use the same values
- changed Auto-off presets from `1m`, `5m`, `15m`, and `30m` to `5m`, `15m`, `30m`, and `1h`
- fixed Torch brightness updates so the service uses real device strength levels instead of remapping them through an old 1-10 scale

### 0.6.5
- added a home screen footer noting that lighting effects happen locally on your device, with no internet connection required, and the app is open source on GitHub
- kept Brightness and Strobe Speed layout areas stable so switching modes no longer rearranges the screen
- restyled Brightness and Strobe Speed as matching chunky hardware-style slider controls

### 0.6.4
- forced the main power button center to align with the visible screen midpoint on every device
- kept lower controls scrollable below the centered power button when needed
- flattened card backgrounds to match the main screen background
- removed default card elevation and shadow padding for a more uniform dark screen
- removed the outer stroke from segmented control rails to avoid a double-border look
- matched the outer height and radius of the segmented rails and Screen Light button
- removed the remaining card wrappers so secondary controls sit directly on the dark screen

### 0.6.3
- lowered the main control area slightly for easier one-handed power button reach
- restyled Auto-off as a darker segmented switch matching the Mode control
- removed the duplicate Screen Light section label above the button
- replaced the Screen Light button icon with a dedicated mobile icon drawable
- removed unused screen icon drawable after switching to the mobile icon

### 0.6.2
- moved the flashlight mode selector below the main power button
- restyled Torch, Strobe, and SOS as a darker hardware-style segmented switch
- repositioned Screen Light as a secondary control in the main control card

### 0.6.1
- changed permission approval flow so controls are enabled without starting Torch mode
- added haptic feedback when touching the main power button

### 0.6.0
- kept the selected Screen Light preset button visually checked after tapping White, Warm, Red, or Blue
- restored the selected Screen Light preset button when reopening Screen Light
- cleared the selected Screen Light preset when RGB sliders are manually adjusted
- replaced the 1–10 Strobe speed slider with a 5-stop preset slider
- mapped Strobe presets to Slow 1 Hz, Medium 2 Hz, Alert 3 Hz, Fast 4 Hz, and Max 6 Hz
- changed the Strobe speed label to show the selected named preset and hertz value
- updated saved Strobe speed behavior so the app stores the actual preset hertz value
- documented the saved Screen Light preset and Strobe speed preference behavior

### 0.5.10
- kept Camera permission request on app launch while adding a clearer inline recovery message when permission is missing
- added an `Allow Camera Permission` button for users who deny or dismiss the first prompt
- kept Torch, Strobe, SOS, brightness, speed, and Auto-off unavailable until Camera permission exists
- kept Screen Light available without Camera permission
- centralized the Strobe speed service extra so activity and service use the same constant
- documented Camera permission behavior and testing expectations

### 0.5.9
- added local Preferences DataStore storage for saved app choices
- restored the last selected flashlight mode on app launch
- restored the last Auto-off option and Strobe speed
- restored the last Screen Light RGB color after reopening Screen Light
- saved Screen Light preset choices and manual RGB changes locally on-device

### 0.5.8
- added a notification action to turn off active flashlight modes without reopening the app
- updated the Quick Settings tile to open the app when Camera permission is missing
- made the main controls mode-specific so brightness only appears for Torch and speed only appears for Strobe
- added a 30m Auto-off option
- added Screen Light preset buttons for White, Warm, Red, and Blue

### 0.5.7
- completed basic Auto-off controls with Off, 1m, 5m, and 15m options for torch, strobe, and SOS modes
- improved foreground service shutdown so the service and notification stop cleanly when flashlight modes are off
- synced the Quick Settings tile with saved service state instead of a local toggle guess
- added clearer no-flash-device handling while keeping Screen Light available

### 0.5.6
- fixed startup crash on devices without torch strength support by hiding the brightness slider instead of setting an invalid `1..1` slider range
- corrected Camera runtime permission handling across the activity, torch controller, service, and Quick Settings tile paths

### 0.5.5  
- fixed app crash on startup caused by `lateinit` UI binding and Camera2 vendor issues  
- migrated `MainActivity` to View Binding for safer and cleaner UI access  
- ensured Camera permission check runs reliably before any torch actions on Android 13+  
- updated `TorchController` with lazy camera initialization, safe strength detection, and fallback when vendors misbehave  
- refactored brightness slider setup to be null-safe and stable on all devices  

### 0.5.4
- hardened Camera2 flash detection in `TorchController.kt` with safe fallbacks for devices that throw errors during camera characteristic reads

### 0.5.3
- removed explicit Material3 style references from `activity_main.xml` to ensure compatibility with the current app theme and prevent potential layout inflation crashes during startup

### 0.5.2
- fixed app crash caused by unguarded access to `sliderBrightness` during activity startup (again)
- ensured the brightness slider is safely initialized to prevent Null Pointer Exceptions

### 0.5.1
- fixed app crash caused by unguarded access to `sliderBrightness` during activity startup
- made brightness slider nullable and safely initialized to prevent null pointer exceptions
- updated `activity_main.xml` to use explicit Material3.Slider style for consistent rendering across Android 12–14
- cleaned `AndroidManifest.xml` by removing deprecated `android.permission.FLASHLIGHT`

### 0.5.0
- rebuilt torch brightness control using reflection-safe shim to `setTorchStrengthLevel()` on Android 13+ (API 33+) while maintaining compatibility with older devices
- refactored `TorchController.kt` for consistent brightness handling and reliable fallback behavior
- added logic in `MainActivity.kt` to auto-lock slider at max brightness on unsupported devices

### 0.4.0
- correctly mapped `colorSurface` and `colorOnSurface` in the application theme
- fixed visual bugs where unselected mode buttons displayed with poor contrast
- implemented Variable Brightness Control for all modes (Torch, Strobe, SOS) via a new slider
- UI brightness setting maps to the device's Camera2 API intensity levels (API 33+)

### 0.3.2
- adjusted the Strobe speed slider range from 5-20 Hz to a slower 1-10 Hz
- this provides slower, more distinct flashing speeds at the low end (down to 1 flash per second)

### 0.3.1
- improved strobe mode with real-time speed adjustment from the slider  
- added `ACTION_STROBE_UPDATE` intent to TorchService for live interval changes  
- fixed timing logic to ensure smoother, consistent flash rhythm across all speeds  
- maintained 50/50 duty cycle (equal on/off time) for stable brightness perception  
- minor cleanup in handler scheduling and interval mapping for better accuracy  

### 0.3.0
- replaced separate strobe and SOS buttons with a single radio-style mode selector (Torch / Strobe / SOS)
- unified all flashlight controls under one large Power button for simpler UX
- made flashlight modes exclusive so activating one automatically disables them
- removed fake auto-off slider and timer UI (commented out for future use)
- cleaned up main activity logic and strings for new mode-based layout

### 0.2.0
- fixed crash on startup caused by manifest and `MainActivity.kt` package mismatch  
- gated all torch actions behind runtime camera permission
- verified all UI element IDs and bindings for `activity_main.xml`

### 0.1.0
- initial test release (signed)
- general project structure with GitHub Actions support
- includes full LED flashlight functionality with on/off toggle
- supports strobe mode with adjustable flashing speed
- supports SOS mode using accurate Morse code
- includes auto-off timer to save battery
- uses Foreground Service for stable background operation
- adds Quick Settings tile for fast torch control (Android 7.0+)
