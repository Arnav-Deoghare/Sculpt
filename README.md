# Sculpt

A private, local-only Android photo logbook for tracking your physique over time.

**Current state:** camera capture (front/back flip), a gallery, a home screen, configurable poses (toggle, rename, remove, reorder) and a settings screen. Photos are saved to app-private storage, so they never appear in the system gallery.

## Quick start

1. Install JDK 17+ and the Android SDK (command-line tools + platform-tools). Set `ANDROID_HOME` if the SDK is not in the default location.
2. Run the setup script for your OS from this folder:
   - macOS: `bash setup.sh`
   - Windows: double-click `setup.bat` (or run it from a terminal)
3. Follow the prompts to pair your phone over wireless debugging.

Both scripts are safe to re-run. They reconnect to the last phone, rebuild, reinstall and launch the app. `setup.sh` also installs a missing JDK, Android SDK and Gradle into `~/.sculpt-tools`, with no Homebrew, sudo or Android Studio needed.

### Phone prerequisites

- Developer options enabled (Settings, About phone, tap Build number 7 times).
- Wireless debugging on (Developer options, Wireless debugging). Android 11+.
- Phone and computer on the same Wi-Fi (a phone hotspot works too).

If wireless debugging will not connect (some routers isolate devices), plug in a USB cable with USB debugging enabled; the script detects any connected device and skips pairing.

### Troubleshooting

- `INSTALL_FAILED_UPDATE_INCOMPATIBLE`: the phone has a copy of Sculpt signed with a different debug key. Run `adb uninstall com.arnav.sculpt` (this deletes the app's data) and re-run the setup script.
- Gradle download fails or resets: the wrapper timeout is already raised to 3 minutes; on a flaky connection just re-run the script, the downloads resume.

## Manual commands

```
./gradlew installDebug          # macOS/Linux (gradlew.bat on Windows)
adb shell am start -n com.arnav.sculpt/.MainActivity
adb logcat -s SculptCamera
```

## Stack

Kotlin, Jetpack Compose (Material 3), CameraX, Room. minSdk 26, targetSdk 35, single `app` module, package `com.arnav.sculpt`.

## Roadmap

Guided capture with pose templates and alignment, pose-matched comparison, body metrics, app lock, encrypted backup. Not started yet.
