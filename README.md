# Sculpt

A private, local-only Android photo logbook for tracking your physique over time.

**Current state: skeleton.** The app is a bare camera: live preview, capture button, front/back flip. Photos are saved to app-private storage, so they never appear in the gallery.

## Quick start (Windows)

1. Install JDK 17+ and the Android SDK (command-line tools + platform-tools). Set `ANDROID_HOME` if the SDK is not in the default location.
2. Double-click `setup.bat` (or run it from a terminal in this folder).
3. Follow the prompts to pair your phone over wireless debugging.

`setup.bat` is safe to re-run. It will reconnect to the last phone, rebuild, reinstall and launch the app.

### Phone prerequisites

- Developer options enabled (Settings, About phone, tap Build number 7 times).
- Wireless debugging on (Developer options, Wireless debugging). Android 11+.
- Phone and PC on the same Wi-Fi (a phone hotspot works too).

If wireless debugging will not connect (some routers isolate devices), plug in a USB cable with USB debugging enabled; the script detects any connected device and skips pairing.

## Manual commands

```
gradlew.bat installDebug
adb shell am start -n com.arnav.sculpt/.MainActivity
adb logcat -s SculptCamera
```

## Stack

Kotlin, Jetpack Compose (Material 3), CameraX. minSdk 26, targetSdk 35, single `app` module, package `com.arnav.sculpt`.

## Roadmap

See the scope proposal: guided capture with pose templates and alignment, pose-matched comparison, body metrics, app lock, encrypted backup. Not started yet.
