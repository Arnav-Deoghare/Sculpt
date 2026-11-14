# Releasing Sculpt

How a version gets from the code to a signed APK on the Releases page. This is for maintainers.

## The signing key

Every release must be signed with the **same key**, or phones that already have Sculpt can't update. Treat the key like a password:

- Keep it **outside the repo**. `*.jks`, `*.keystore` and `keystore.properties` are git-ignored as a safety net.
- Keep **two backups** in different places, such as a password manager and an encrypted drive. Back up the key file and its passwords together.
- Never paste it, its passwords, or the base64 text into an issue, a chat or a commit.

If the key is lost, the only fix is a new app id, and every user has to reinstall.

### Create a key (once)

```bash
mkdir -p ~/.sculpt-signing
keytool -genkeypair -keystore ~/.sculpt-signing/release.jks -alias sculpt \
  -keyalg RSA -keysize 4096 -validity 10000
```

Pick a strong password and use it for both the store and the key.

### Use it for local release builds

Create `keystore.properties` in the project root (it is git-ignored):

```properties
storeFile=/Users/you/.sculpt-signing/release.jks
storePassword=...
keyAlias=sculpt
keyPassword=...
```

Then `./gradlew assembleRelease` produces a signed APK in `app/build/outputs/apk/release/`.

Check any APK's signature and fingerprint with:

```bash
apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```

Publish the certificate's SHA-256 fingerprint (for example in the README) so people can confirm a download is yours.

## GitHub secrets (once)

The release workflow signs the APK from four repository secrets. Add them under **Settings, Secrets and variables, Actions**:

| Secret | Value |
| --- | --- |
| `SCULPT_STORE_BASE64` | The key file, base64 encoded: `base64 -i release.jks \| tr -d '\n' \| pbcopy` |
| `SCULPT_STORE_PASSWORD` | The store password |
| `SCULPT_KEY_ALIAS` | `sculpt` |
| `SCULPT_KEY_PASSWORD` | The key password |

The workflow refuses to publish if any secret is missing, so an unsigned APK can never go out.

## Cutting a release

1. Make sure `main` is green: `./gradlew assembleDebug testDebugUnitTest lintDebug assembleRelease`.
2. Update `versionName` (and raise `versionCode` by one) in `app/build.gradle.kts`.
3. In `docs/CHANGELOG.md`, rename `[Unreleased]` to `[X.Y.Z] - date` and add a fresh empty `[Unreleased]` above it. The release notes on GitHub are taken from this section.
4. Add `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` (one or two sentences, 500 characters at most).
5. Commit, push, and wait for the **Build** workflow to pass.
6. Tag and push:

   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

7. The **Release** workflow checks the tag matches `versionName`, runs the tests, builds and signs the APK, verifies the signature, and publishes `Sculpt-X.Y.Z.apk` and `Sculpt-X.Y.Z.apk.sha256` on the Releases page. The project page asks GitHub's API for the newest release and links to that APK, so it never needs editing for a new version. Keep the APK name ending in `.apk`, because the page finds it by that.
8. Download the APK from the release, install it on a phone, and open every tab before announcing it.

If the workflow fails, fix the problem, delete the tag (`git tag -d v1.0.0 && git push origin :refs/tags/v1.0.0`), and tag again.

## Release file names

The project page finds the download by asking GitHub's API for the newest release and picking the asset named `Sculpt-<version>.apk` (any `.apk` as a fallback). Don't rename it, and don't remove the `.apk` ending. The release workflow already names it correctly. The `.apk.sha256` file is shown next to the download automatically.

## Updating from a debug build

Debug builds are signed with a different key, so a release APK can't install over one. Uninstall the debug app first (`adb uninstall com.arnav.sculpt`). That deletes its data.

## F-Droid (optional)

The `fastlane/metadata` folder is already laid out for F-Droid. To list the app, open a merge request on [fdroiddata](https://gitlab.com/fdroid/fdroiddata) with a metadata file pointing at this repository. Screenshots go in `fastlane/metadata/android/en-US/images/phoneScreenshots/`.

## The project page

The project's web page is not in this repo. It lives in the maintainer's portfolio site, at `https://arnav-deoghare.github.io/sculpt/`. It repeats the signing certificate fingerprint and links to the latest release, so if the signing key ever changes, update the fingerprint there as well as in the README.
