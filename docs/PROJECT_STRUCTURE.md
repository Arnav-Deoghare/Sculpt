# Project structure

A map of the repository, from the top level down to the code. Sculpt is a single-module Android app: Kotlin, Jetpack Compose (Material 3), CameraX and Room, package `com.arnav.sculpt`.

## Top level

```
Sculpt/
├── app/                  The Android app (the only Gradle module)
├── docs/                 Documentation (you are here)
├── fastlane/             App store listing text, used by F-Droid
├── tools/                Scripts that are not part of the app (promo video renderer)
├── gradle/               Gradle wrapper and the version catalog
├── .github/              Issue forms, PR template and CI workflows
├── build.gradle.kts      Root build file (plugin versions only)
├── settings.gradle.kts   Declares the :app module and repositories
├── gradle.properties     Gradle and Android build flags
├── gradlew, gradlew.bat  Gradle wrapper scripts
├── setup.sh, setup.bat   One-command install-and-run for macOS and Windows
├── LICENSE               Apache-2.0
└── README.md             What Sculpt is and how to build it
```

| Folder | What it's for | When you'd touch it |
| --- | --- | --- |
| `app/` | All the code, resources, tests and the Room schema | Almost every change |
| `docs/` | Contributor and project documentation | Changing a process or the docs |
| `fastlane/` | Store title, descriptions and per-version changelogs | Each release |
| `tools/` | `promo-video/` draws and renders the promo videos | The look of the app changes |
| `gradle/` | `libs.versions.toml` lists every dependency version | Adding or upgrading a library |
| `.github/` | Bug and feature forms, PR template, `build.yml` and `release.yml` | Changing CI or templates |

## Inside `app/`

```
app/
├── build.gradle.kts      Module build: signing, R8, dependencies
├── proguard-rules.pro    R8 shrinker rules
├── schemas/              Room's exported database schema (commit these)
└── src/
    ├── main/
    │   ├── AndroidManifest.xml
    │   ├── java/com/arnav/sculpt/   The app code, see below
    │   └── res/                     Icon, strings and theme resources
    └── test/                        Unit tests that run on your computer
```

## The code, `app/src/main/java/com/arnav/sculpt/`

The code is grouped by what the user sees (a folder per screen), plus shared layers underneath.

```
com/arnav/sculpt/
├── MainActivity.kt          Entry point; sets the theme and waits for the database
├── SculptApplication.kt     Creates the shared database and repositories
├── SculptApp.kt             Tab bar and screen switching, draws the mat floor
│
├── home/                    Home tab
│   └── HomeScreen.kt           "Capture today" buttons
│
├── camera/                  Camera tab
│   ├── CameraScreen.kt         Preview, capture, timer, ghost overlay, hands-free
│   ├── PoseSelector.kt         The pose wheel
│   ├── PoseGuide.kt            Framing lines and body silhouettes drawn over the preview
│   └── CameraIcons.kt          Flip and flash icons
│
├── gallery/                 Gallery tab
│   ├── GalleryScreen.kt        Day-grouped grid, viewer, selection and delete
│   └── CompareView.kt          Same-pose comparison: side by side and fade
│
├── settings/                Settings tab
│   └── SettingsScreen.kt       Theme, pose list (toggle, rename, reorder), options
│
├── data/                    Storage, with no UI code
│   ├── PhotoRepository.kt      Photos: database rows plus image files
│   ├── PoseRepository.kt       Built-in and custom poses
│   ├── PhotoFiles.kt           Where photos live and how files are named
│   ├── SettingsStore.kt        Small preferences (theme, timer, flash, ...)
│   └── db/                     Room: SculptDatabase, entities and DAOs
│
├── ui/                      Shared UI
│   ├── WheelPill.kt            Reusable wheel picker control
│   └── theme/
│       ├── Theme.kt            Maroon and black colors, dark and light
│       └── MatFloor.kt         The interlocking gym-mat tile background
│
└── util/
    └── ImageUtils.kt           Decoding photos off the main thread
```

## How the pieces connect

1. `SculptApplication` builds an `AppContainer` holding the database and the two repositories. Screens reach it with `context.appContainer`.
2. `MainActivity` waits until the container reports it's ready (built-in poses created), then shows `SculptApp`.
3. `SculptApp` draws the `MatFloor` background and one screen per tab. Each screen reads from the repositories as Kotlin flows, so the UI updates when photos or poses change.
4. Photos are stored as JPEG files in the app's private folder. The database row holds the file name, the time taken and the pose id.

## Where do I put new code?

| You're adding... | Put it in |
| --- | --- |
| A new screen or tab | A new folder next to `home/`, then add it to `SculptApp.kt` |
| A new table or column | `data/db/` (bump the version, write a migration, commit the new `app/schemas` file) |
| Logic that reads or writes photos or poses | `data/` |
| A control used by more than one screen | `ui/` |
| A color or the look of the app | `ui/theme/` |
| A test | `app/src/test/java/com/arnav/sculpt/`, mirroring the folder of the code it tests |
| A new dependency | `gradle/libs.versions.toml`, then `app/build.gradle.kts` |
