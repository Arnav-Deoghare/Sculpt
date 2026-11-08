# Changelog

All notable changes to Sculpt are listed here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added
- Release builds with R8 shrinking, optional signing, and a `-PdebugSignRelease` flag for testing them.
- GitHub Actions workflows to build, test and lint, and to publish tagged releases.
- Unit tests for photo file naming and the built-in poses.
- Room schema export, so database migrations can be checked.
- Apache-2.0 license, contributing guide, code of conduct, security policy, and issue and pull request templates.
- F-Droid style store listing under `fastlane/`.
- A `docs/` folder holding the contributor docs, plus a project structure guide.

## [0.1.0]

### Added
- Camera with flip, flash, timer, hands-free capture and a ghost overlay of the last photo.
- Poses: five built-in plus custom poses that can be reordered, renamed and removed.
- Gallery grouped by day with multi-select delete.
- Compare two photos of the same pose, side by side or faded, with shared pinch-zoom.
- Maroon and black interlocking gym-mat theme in dark and light, with a mat-tile app icon.

### Fixed
- App not responding on launch, caused by an expensive grain texture.
