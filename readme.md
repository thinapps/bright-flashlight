# Bright Flashlight

### Changelog

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
