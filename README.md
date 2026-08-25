# PhonePad

PhonePad turns a Samsung Galaxy phone into a Bluetooth keyboard and five-button mouse for Windows. It is designed for personal, offline use with no account, analytics, advertising, or internet permission.

## Current milestone: functional alpha

Pure Bluetooth HID has been validated on a Samsung Galaxy S24 Ultra running Android 16 / One UI 8.5 and a Windows 11 Lenovo laptop. The functional alpha includes:

- Large touchpad with pointer acceleration
- Single-tap left click, two-finger right click, three-finger middle click and long-press right click
- Double-tap and drag, vertical scrolling and horizontal scrolling
- Dedicated left, right, middle, back and forward buttons
- Adjustable pointer speed and standard/natural scrolling
- Samsung Keyboard mode for English and Hinglish text
- Samsung composing-text and autocorrect handling, including mirrored backspace corrections
- Submission-safe phone typing: editor control characters are filtered and Enter is explicit
- Full PC keyboard with Escape, F1–F12, navigation, arrows and sticky modifiers
- Copy, paste, cut, undo, redo, select-all, app switching, close and search shortcuts
- Volume, playback and brightness consumer controls
- Portrait and landscape layouts with keep-screen-awake behavior
- Automatic HID registration/reconnection and a compact control view while connected

The Android HID Device API automatically unregisters an app that is not foreground. Keep PhonePad visible during this capability test.

## Remaining roadmap

- Foreground-service reliability while the screen locks
- Presentation mode, left-handed mode and customizable layouts
- Settings persistence and a Quick Settings tile
- Optional explicit Wi-Fi/USB companion fallback if Samsung's native HID profile is unreliable

## Privacy

PhonePad is offline-first and the Android app intentionally has no internet permission.
