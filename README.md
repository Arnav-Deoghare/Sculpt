# Sculpt

A private, local-only Android photo logbook for tracking your physique over time.

Take progress photos in the same pose every time, line them up with a ghost of your last photo, and compare any two photos of a pose side by side or faded over each other.

## Privacy

Your photos never leave your phone.

- The app asks for **one permission: the camera**. It has no internet permission, so it cannot send anything anywhere.
- No accounts, no analytics, no ads, no tracking.
- Photos are saved to the app's private storage, so they don't appear in your gallery or in other apps.
- Android backups of the app are switched off.

## Features

- **Camera** with front/back flip, flash, a self-timer, and hands-free capture that shoots every pose in turn.
- **Ghost overlay** of your previous photo, so you can match your position.
- **Poses**: Front, Front bicep, Side, Back and Back flexed, plus your own custom poses. Turn them on or off, rename them, and drag to set the shooting order.
- **Gallery** grouped by day, with pose labels and multi-select delete.
- **Compare** two photos of the same pose, defaulting to your oldest and newest. View them **side by side** or **faded** over each other, with shared pinch-zoom.
- Dark and light themes built around interlocking maroon and black gym-mat tiles.

## Install

Release APKs will be published on the [Releases page](../../releases). Until then, build it yourself, as below. Android 8.0 (API 26) or newer.

## Build from source

You need JDK 17 and the Android SDK (platform 35). Then:

```bash
./gradlew assembleDebug        # debug APK in app/build/outputs/apk/debug
./gradlew installDebug         # build and install on a connected phone
./gradlew testDebugUnitTest    # unit tests
./gradlew lintDebug            # Android lint
```

### One-command setup

The setup scripts install what's missing, connect your phone over wireless debugging (or USB), then build, install and launch the app. Both are safe to re-run.

- macOS: `bash setup.sh` (installs JDK 17, the Android SDK and Gradle into `~/.sculpt-tools`, with no Homebrew, sudo or Android Studio needed)
- Windows: double-click `setup.bat`

Phone prerequisites: Developer options on, and wireless debugging on (Android 11+) or a USB cable with USB debugging.

### Release build

```bash
./gradlew assembleRelease
```

This runs R8 shrinking. Without a signing key it produces an unsigned APK. To sign it, copy your keystore details into a git-ignored `keystore.properties` in the project root:

```properties
storeFile=release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

To install a release build on your own phone for testing, add `-PdebugSignRelease`, which signs it with your debug key:

```bash
./gradlew installRelease -PdebugSignRelease
```

### Troubleshooting

- `INSTALL_FAILED_UPDATE_INCOMPATIBLE`: the phone has a copy of Sculpt signed with a different key. Run `adb uninstall com.arnav.sculpt` (this deletes the app's data) and install again.
- Gradle download resets on a slow connection: the wrapper timeout is already raised to 3 minutes; re-run and it resumes.

## Project layout

Single `app` module, package `com.arnav.sculpt`. Kotlin, Jetpack Compose (Material 3), CameraX and Room.

```
app/       the Android app: code, resources, tests and the Room schema
docs/      contributing guide, code of conduct, security policy, changelog
fastlane/  store listing text for F-Droid
gradle/    Gradle wrapper and dependency versions
.github/   issue forms, PR template and CI workflows
```

Inside the app, each tab has its own folder (`home/`, `camera/`, `gallery/`, `settings/`) on top of shared `data/`, `ui/` and `util/` layers. The full map, and where new code should go, is in [docs/PROJECT_STRUCTURE.md](docs/PROJECT_STRUCTURE.md).

## Documentation

All project docs are indexed in [docs/](docs/README.md): [contributing](docs/CONTRIBUTING.md), [code of conduct](docs/CODE_OF_CONDUCT.md), [security policy](docs/SECURITY.md), [changelog](docs/CHANGELOG.md) and [project structure](docs/PROJECT_STRUCTURE.md).

## Roadmap

1. Reference layouts for images
2. Auto-capture when the live view matches an old photo
3. Post-processing to align pose and orientation
4. Weight and body metrics
5. Reminder notifications
6. Release-ready app: signing, shrinking, CI and F-Droid metadata (in progress)

Ideas and bug reports are welcome. See [docs/CONTRIBUTING.md](docs/CONTRIBUTING.md).

## Contributing

Contributions are welcome, from typo fixes to whole features. Read [docs/CONTRIBUTING.md](docs/CONTRIBUTING.md) first, and please follow the [Code of Conduct](docs/CODE_OF_CONDUCT.md). To report a security problem, see [docs/SECURITY.md](docs/SECURITY.md).

## License

Licensed under the [Apache License 2.0](LICENSE).
