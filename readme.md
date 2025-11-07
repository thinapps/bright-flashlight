# Bright Flashlight

### Changelog

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
