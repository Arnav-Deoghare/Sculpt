# Changelog

All notable changes to Sculpt are listed here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added
- `tools/promo-video`, a script that renders the promo videos from vector art: a 720p portrait cut and a 60 fps 1080p landscape cut for X and YouTube.

## [1.0.0] - 2026-11-13

First public release.

### Features
- Camera with front and back lens, flash, self-timer, hands-free capture and a ghost overlay of your last photo.
- Camera guide: tap **Guide** to cycle Off, Frame (a centre line with head and feet lines) or Body (a faint silhouette matched to the pose). It shows on the live view only, never in the saved photo, and the choice is remembered.
- Poses: five built-in plus custom poses that can be reordered, renamed and removed.
- Gallery grouped by day, with multi-select delete.
- Compare two photos of the same pose, side by side or faded over each other, with shared pinch-zoom.
- Maroon and black interlocking gym-mat theme in dark and light, with a mat-tile app icon.

### Project
- Release builds with R8 shrinking, optional signing, and a `-PdebugSignRelease` flag for testing them.
- GitHub Actions workflows to build, test and lint, and to publish tagged releases.
- Unit tests for photo file naming, the built-in poses and the guide modes.
- Room schema export, so database migrations can be checked.
- Apache-2.0 license, contributing guide, code of conduct, security policy, and issue and pull request templates.
- F-Droid style store listing under `fastlane/`.

### Fixed
- App not responding on launch, caused by an expensive grain texture.
