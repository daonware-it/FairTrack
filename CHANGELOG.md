# Changelog

All notable changes to FairTrack are recorded here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## How to use this file for a release

The **Added / Changed / Fixed** bullets of a released version are written to be lifted straight into
the Play Console release notes. Two constraints to keep in mind while writing them:

- Play allows **500 characters per language**. Trim to the entries a user would notice.
- The first public release should introduce the app, not list bugfixes — nobody saw the version that
  had the bug.

Keep an `[Unreleased]` section at the top and move entries down into a dated version on release.

<!-- markdownlint-disable MD024 -- repeated "Added"/"Fixed" headings are the point of this format -->

## [Unreleased]

### Added
- Macrobenchmark module measuring cold and warm startup, run weekly on an emulator.
- CodeQL static analysis on pull requests and weekly.
- Dependabot version updates for Gradle dependencies and GitHub Actions.
- GitHub release workflow: builds and signs the tester APK, publishes it with checksums.

### Changed
- GitHub Actions are pinned to commit SHAs instead of movable tags, and every workflow now starts
  from `permissions: {}`. A hijacked action can no longer read the signing key.
- The Azure pipeline builds only the Play bundle; the tester APK comes from GitHub Actions. Each
  signing key now lives in exactly one place.

### Fixed
- Weekday labels in the statistics chart did not redraw when the app language changed.

## [1.0.1] - 2026-07-10

First public release.

### Added
- Log food by barcode, by searching [Open Food Facts](https://openfoodfacts.org), or from your own
  recipes and meal templates.
- Track calories, macros, 14 micronutrients and water against goals derived from your body metrics.
- Follow weight and body measurements (waist, body fat, chest, arm) over time.
- Import steps and burned calories from Health Connect.
- Intermittent fasting timer with presets and a live countdown.
- Export the diary to a file and restore it on another device.
- Home-screen widgets, light and dark themes, metric and imperial units.
- German, English and Spanish translations.

### Changed
- Android's automatic cloud backup is disabled. It would have placed the diary, weight and body
  measurements in Google Drive, contradicting the promise that data stays on the device. Use the
  built-in export instead.

### Fixed
- The barcode scanner failed to recognise products.
- Activity sync with Health Connect did not import steps or burned calories.

[Unreleased]: https://github.com/daonware-it/FairTrack/compare/v1.0.1...HEAD
[1.0.1]: https://github.com/daonware-it/FairTrack/releases/tag/v1.0.1
