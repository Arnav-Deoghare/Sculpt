# Contributing to Sculpt

Thanks for helping out. Sculpt is a small app, so the process is light.

## Ground rules

- **Privacy comes first.** Sculpt has no internet permission and that is a feature. Please don't add network access, analytics, crash reporters or ad libraries. If you think a feature really needs the network, open an issue to discuss it before writing code.
- Keep the app **local-only**: photos stay in app-private storage.
- Be kind. See the [Code of Conduct](CODE_OF_CONDUCT.md).

## Getting set up

1. Fork the repo and clone your fork.
2. Install JDK 17 and the Android SDK, or run `bash setup.sh` (macOS) / `setup.bat` (Windows) with a phone connected.
3. Build and test:

   ```bash
   ./gradlew assembleDebug testDebugUnitTest lintDebug
   ```

All three must pass before you open a pull request. CI runs the same commands.

## Making a change

1. For anything bigger than a small fix, **open an issue first** so we can agree on the approach.
2. Create a branch from `main`.
3. Keep the change focused: one fix or feature per pull request.
4. Follow the existing code style: Kotlin official style, comments only where the reason isn't obvious, and names that match the surrounding code.
5. Add or update unit tests when you change logic that can be tested without a device.
6. Test UI changes on a real phone or emulator, in both **dark and light** mode, and include screenshots in the pull request.

### Commit messages

Use `type: short summary`, where the type is one of `feature`, `fix`, `chore`, `docs`, `test` or `refactor`. For example: `fix: stop gallery crash when a photo file is missing`.

### Database changes

The Room schema is exported to `app/schemas/`. If you change an entity:

1. Bump the database `version`.
2. Write a `Migration`. Never rely on destructive migration, because users' photo records would be lost.
3. Commit the new schema JSON that Gradle generates.

### Performance

The app once froze for several seconds on launch because of an expensive draw call. Keep heavy work off the main thread, build textures and paths once and cache them, and check the first-frame time on a debug build when you touch drawing code.

## Project layout

See [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md) for a map of the repo and where new code belongs.

## Releasing (maintainers)

See [RELEASING.md](RELEASING.md) for the signing key, the GitHub secrets and the tagging steps.

## Reporting bugs and ideas

Use the issue templates. For bugs, include your phone model, Android version, the app version, and steps to reproduce. For a freeze or crash, `adb logcat -d` output helps a lot. Don't attach personal photos.

## License

By contributing you agree that your contributions are licensed under the [Apache License 2.0](../LICENSE).
