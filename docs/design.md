# Design

Bright Flashlight should feel like a simple hardware tool: dark, stable, readable, and safe to use quickly.

## Core UI philosophy

- Keep the app focused on the main light controls.
- Avoid extra settings screens, onboarding, dismissible notices, ads, tracking, login, or database features.
- Do not add a custom Quick Settings tile. Android already provides a built-in flashlight tile, and this app should focus on the full in-app control surface.
- Keep controls visually stable so switching modes does not rearrange the screen.
- Use XML Material UI only. Do not introduce Jetpack Compose.
- Reserve orange for active, selected, or primary controls.
- Use the dark app background for the main surface, not stacked card backgrounds.

## Icons

- Prefer Google Material Icons for core UI icons.
- Keep original Material icon paths whenever possible.
- Convert SVG files to Android VectorDrawable format without changing the icon geometry.
- SVG `viewBox="0 -960 960 960"` icons should be converted to a positive Android `960 x 960` viewport.
- Remove hardcoded SVG fill colors such as `#1f1f1f`; use neutral white fill in the vector and apply the actual color from layout tint.
- Keep icon vector dimensions at the standard `24dp` unless there is a strong reason not to.
- Control visible icon size from layout XML, using button size, padding, `iconSize`, or `scaleType`.
- Do not shrink icons by scaling the vector path or editing the Material path data.
- Keep tap targets larger than visible glyphs when needed.

## Dimmed and disabled controls

- Dimmed unavailable controls should use `0.45` alpha.
- Disabled previews should preserve layout rhythm but must not accept value-changing input.
- If a disabled-looking control must keep an active selection visible, consume touches before they can change or clear the current value.
- Informational warning icons may remain tappable in dimmed preview states when they only open safety information and do not change app state.
- Locked controls should not save preferences, fire haptics, or leave the UI with no selected value.

## Layout rhythm

- Maintain the current 24dp effective screen gutters.
- Preserve stable vertical slots for Brightness, Strobe Speed, and Auto-off so the UI does not jump between modes.
- Keep Brightness and Strobe Speed header and slider well heights matched.
- Use larger tappable areas around small icons instead of making visible glyphs oversized.
- Prefer clear text labels and predictable placement over dense controls.

## Screen Light

- Screen Light should stay available even when Camera permission or flash hardware is unavailable.
- Keep Screen Light simpler than the main flashlight controls.
- Prefer fixed, tappable color preset tiles over always-visible RGB sliders.
- Keep color preset tiles visually unlabeled so they read as simple swatches, while preserving accessibility names with content descriptions.
- Do not use a segmented-control rail or toggle-bar layout for Screen Light color swatches.
- Use compact square-corner swatch tiles so the color choices stay visual instead of reading like navigation buttons.
- Do not use rounded corners on Screen Light color swatches.
- Do not show an extra `Presets` label above the swatches.
- Do not add custom color input, color wheels, or advanced controls unless there is a clear user need.

## Safety and clarity

- Strobe controls should keep the flashing-light warning visible and easy to open.
- Strobe Speed should use simple numbered presets with hertz shown in the header.
- Auto-off should always show one selected value and should lock while a light mode is active.
