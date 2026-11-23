# Truong Dual Desktop — 0.2 Beta

Target device: Xiaomi Pad 6 (23043RP34G), HyperOS 2 / Android 14, non-root.

## Current implementation
- Detects external presentation displays.
- Shows an app-owned desktop launcher on HDMI.
- Enumerates launcher-visible Android apps.
- Requests launch of a selected app on the HDMI display using Android's public ActivityOptions.setLaunchDisplayId API.
- Reports display and input-device information.

## Important limits
This build tests whether the target HyperOS firmware permits third-party activities to launch on the presentation display. Some apps may reject secondary-display launch or be redirected to the built-in display. The app does not claim system-wide extended desktop or universal mouse crossing: Android 14 exposes no ordinary third-party API to rewrite system cursor topology. Mouse crossing outside the app requires OS/OEM support or privileged/system integration.

## Build
JDK 17 + Gradle 8.9: `gradle --no-daemon assembleDebug`.
Artifact: `app/build/outputs/apk/debug/app-debug.apk`.
