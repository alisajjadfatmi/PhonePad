# PhonePad

PhonePad turns a Samsung Galaxy phone into a Bluetooth keyboard and five-button mouse for Windows. It is designed for personal, offline use with no account, analytics, advertising, or internet permission.

## Current milestone: Bluetooth capability test

The first APK validates the exact hardware path before the full touchpad is built:

1. Grant Android's Nearby Devices permission.
2. Register PhonePad as a composite Bluetooth keyboard and mouse.
3. Connect to an already-paired computer.
4. Test pointer movement, left click, and typing.

The Android HID Device API automatically unregisters an app that is not foreground. Keep PhonePad visible during this capability test.

## Planned controls

- Full-screen touchpad with tap, double-click, drag, long-press, and two-finger scrolling
- Left, right, middle, back, and forward mouse buttons
- Samsung Keyboard input mode and a full PC keyboard mode
- Sticky Ctrl, Alt, Shift, and Windows keys
- Media, shortcut, presentation, portrait, landscape, and left-handed layouts
- Multiple remembered computers with automatic reconnection
- Optional explicit Wi-Fi/USB companion fallback if Samsung's native HID profile is unreliable

## Privacy

PhonePad is offline-first and the Android app intentionally has no internet permission.

