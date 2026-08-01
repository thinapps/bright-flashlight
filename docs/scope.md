# Scope

Bright Flashlight is a focused lighting utility, not a general phone-tool collection.

## Core product

The supported product scope is:

- steady camera-flash Torch control, preferring a rear-facing flash when available
- Strobe with simple speed presets and a flashing-light warning
- SOS using the standard repeating light pattern
- Screen Light with fixed color presets
- hardware brightness control where the device supports torch strength levels
- Auto-off presets for Torch, Strobe, and SOS

## Reliability and safety

Work that keeps the existing features dependable is in scope, including:

- Camera permission handling and no-flash fallback states
- foreground-service lifecycle and required notification behavior
- state restoration after activity recreation
- external camera or torch interruption handling
- device-specific torch strength fallback behavior
- accessibility, readable contrast, stable layouts, and appropriate touch targets
- flashing-light safety guidance

Reliability fixes should prefer Android platform APIs, existing dependencies, and small event-driven changes over polling, background work, or new architecture.

## Product boundaries

The following are intentionally out of scope:

- camera, QR, barcode, compass, ruler, battery, or other toolbox features
- custom Quick Settings tiles, widgets, shortcuts, or background automation
- custom Strobe sequences, programmable patterns, music synchronization, or sound effects
- custom Screen Light color input, color wheels, animations, or advanced effects
- themes, accounts, cloud sync, analytics, advertising, tracking, or remote configuration
- unnecessary notification customization beyond the required foreground-service notice and stop action
- features that require unrelated permissions, network access, large dependencies, or a second application architecture

Android already provides a built-in flashlight Quick Settings tile. This app should remain focused on its full in-app controls rather than duplicating that system feature.

## Decision rule

A proposed change belongs in Bright Flashlight only when it directly improves the existing lighting functions, safety, accessibility, compatibility, or reliability without materially increasing permissions, dependencies, background activity, or interface complexity.

When a proposal does not meet that rule, keep it out of this app even if similar flashlight apps include it.
