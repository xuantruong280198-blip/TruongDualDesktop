# Truong Dual Desktop — Diagnostic 0.1

Android diagnostic prototype for Xiaomi Pad 6 (HyperOS 2.0.16.0.UMZMIXM, Android 14, 6 GB RAM, non-root).

## Scope
- Enumerates displays and reports display IDs, types, flags, dimensions, refresh rate and presentation capability.
- Enumerates mouse, keyboard, touch and stylus input devices.
- Listens for display and input-device add/remove/change events.
- Attempts an app-owned Presentation on a secondary display that advertises FLAG_PRESENTATION.

## Important limitation
This diagnostic app does not change Android's system display topology, turn mirroring into OS-level extended desktop, or move the system mouse pointer between displays. Those behaviors are controlled by Android/OEM system services and must be proven possible on the target HyperOS build before promising a no-root implementation.

## Build
JDK 17 + Gradle 8.9:
`gradle --no-daemon assembleDebug`

APK: `app/build/outputs/apk/debug/app-debug.apk`

The GitHub Actions workflow builds on pushes to main and manual dispatch. Download artifact `TruongDualDesktop-diagnostic-debug` from Actions.
