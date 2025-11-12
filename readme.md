# Bright Flashlight

### Changelog

### 0.5.3
- removed explicit Material3 style references from `activity_main.xml` to ensure compatibility with the current app theme and prevent potential layout inflation crashes during startup

### 0.5.2
- fixed app crash caused by unguarded access to `sliderBrightness` during activity startup (again)
- ensured the brightness slider is safely initialized (nullable, safe-call operator) to prevent Null Pointer Exceptions

### 0.5.1
- fixed app crash caused by unguarded access to `sliderBrightness` during activity startup
- made brightness slider nullable and safely initialized to prevent null pointer exceptions
- updated `activity_main.xml` to use explicit `Material3.Slider` style for consistent rendering across Android 12–14
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
- made flashlight modes exclusive so activating one automatically disables the others
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
