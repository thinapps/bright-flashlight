# Bright Flashlight

### Documentation

- [Main Controls](docs/main-controls.md)
- [App Preferences](docs/app-preferences.md)
- [Permissions](docs/permissions.md)

### Changelog

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
- added a home screen footer noting that lighting effects run locally, no internet connection is required, and the app is open source on GitHub
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
- saved Screen Light preset choices and manual RGB slider changes locally on-device

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
- updated `TorchController` with lazy camera initialization, safe strength detection, and fallback to on/off when vendors misbehave  
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
- adds color screen light mode with RGB sliders
- uses Foreground Service for stable background operation
- adds Quick Settings tile for fast torch control (Android 7.0+)
- XML-based Material UI (no Jetpack Compose)
