# PhonePad

PhonePad turns a Samsung Galaxy phone into a Bluetooth keyboard and five-button mouse for Windows. It is designed for personal, offline use with no account, analytics, advertising, or internet permission.

## Current milestone: functional alpha

Pure Bluetooth HID has been validated on a Samsung Galaxy S24 Ultra running Android 16 / One UI 8.5 and a Windows 11 Lenovo laptop. The functional alpha includes:

- Large touchpad with pointer acceleration
- Single-tap left click, two-finger right click, three-finger middle click and long-press right click
- Double-tap and drag, vertical scrolling and horizontal scrolling
- Dedicated left, right, middle, back and forward buttons
- Adjustable pointer speed and standard/natural scrolling
- Remembered pointer, scrolling, haptic, primary-button and last-control settings
- Left- or right-handed touchpad primary-click mode
- Samsung Keyboard bridge that emits ordinary HID key presses without mirroring the Windows document
- Current-word composition and autocorrect handling without whole-document delete-and-retype behaviour
- Physical-keyboard Enter semantics: newline in editors and submission in single-line fields
- Full PC keyboard with Escape, F1–F12, navigation, arrows and sticky modifiers
- Copy, paste, cut, undo, redo, select-all, app switching, close and search shortcuts
- Volume, playback and brightness consumer controls
- Presentation remote with previous/next, start, blank-screen and exit controls
- Portrait and landscape layouts with keep-screen-awake behavior
- Multiple paired-computer selection with remembered preferred host
- Automatic HID registration/reconnection and a compact control view while connected
- Persistent connected-device service that keeps Bluetooth HID active when PhonePad is in the background
- Ongoing connection notification that reopens the existing live control session
- Quick Settings tile for opening PhonePad controls

Android automatically unregisters a Bluetooth HID Device app that is not foreground. PhonePad avoids that disconnect by keeping HID ownership in a low-priority foreground connected-device service rather than in the visible screen.

## Remaining roadmap

- Optional explicit Wi-Fi/USB companion fallback if Samsung's native HID profile is unreliable

## Install and pair

1. Install the latest APK on an Android phone that exposes Android's Bluetooth HID Device profile.
2. Grant Nearby Devices access. Allow notifications so Android can show the persistent connection status.
3. Register PhonePad, make the phone discoverable, and pair it from the computer while the HID service is active.
4. Select any paired computer in PhonePad and connect. The last selected host is remembered.
5. Optionally add **PhonePad controls** from Android's Quick Settings tile editor.

After connecting, you can switch apps or return to the Home screen. The **PhonePad is active** notification keeps the standard Bluetooth keyboard and mouse registered; tap it to return to the controls.

Windows should recognize PhonePad as a standard Bluetooth keyboard and five-button mouse; no Windows companion application is required.

## Build from source

Requirements: JDK 17 or newer, Android SDK Platform 36, and Android Build Tools 35 or newer.

```powershell
.\gradlew.bat assembleDebug
```

The debug APK is produced under `app/build/outputs/apk/debug/`.

## Privacy

PhonePad is offline-first and the Android app intentionally has no internet permission.

## License

PhonePad is released under the MIT License. See [LICENSE](LICENSE).
