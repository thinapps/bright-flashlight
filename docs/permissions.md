# Permissions

Bright Flashlight uses Camera permission to control the device flashlight.

## Camera permission

Android exposes the physical flashlight through the camera APIs, so Camera permission is required for:

- Torch
- Strobe
- SOS

The app does not take photos, record video, upload camera data, or use Camera permission for anything beyond controlling the flashlight.

## First launch behavior

The app requests Camera permission on launch because flashlight control is the main purpose of the app.

This is intentional. Waiting until the user taps Torch would make the app feel broken or delayed for many users.

## Permission missing behavior

If Camera permission is missing or denied:

- Torch, Strobe, SOS, brightness, speed, and Auto-off controls stay disabled or hidden.
- A short inline message explains that Camera permission is required for flashlight control.
- An `Allow Camera Permission` button lets the user request permission again.
- Screen Light remains available because it does not need Camera permission.

## No-flash devices

Camera permission and flash availability are separate states.

If permission is granted but the device does not expose a usable camera flash:

- flashlight controls stay hidden
- the no-flash message is shown
- Screen Light remains available

## What not to add

Do not add a long onboarding screen for Camera permission.

Keep permission UX direct:

- request on app launch
- show a short recovery message if permission is missing
- keep Screen Light available
- avoid long privacy copy in the main UI

## Testing checklist

Before shipping permission changes, test:

- fresh install with no Camera permission yet
- grant permission from the launch prompt
- deny permission from the launch prompt
- tap `Allow Camera Permission` after denial
- confirm Torch/Strobe/SOS controls stay unavailable without permission
- confirm Screen Light opens without permission
- confirm no-flash devices show the no-flash message only after permission is granted
