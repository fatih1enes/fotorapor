# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.0] - 2026-07-31

### Added
- **Multi-language Support:** Added manual language toggle (TR/EN) in Settings.
- **Localized Exports:** PDF and HTML/ZIP reports now fully support Turkish and English headers and date formatting.
- **Improved Testing:** Robust assertions added to Backup and Date utilities.

### Fixed
- **UI Encoding:** Resolved corrupted Turkish characters (mojibake) in Image Cropper and HTML reports.
- **Cleanup:** Removed legacy project schemas and temporary backup configurations.
- **Documentation:** Updated README to accurately reflect the use of native Android PDF APIs.

## [1.1.0] - 2026-06-17

### Added
- **Localization:** Complete English translation for all strings and menus.
- **Export Feedback:** Visual "Please wait" indicator and background success/failure notifications for heavy PDF/ZIP exports.
- **Edge-to-Edge Support:** The application now draws beautifully behind the navigation and status bars for a premium look.

### Changed
- **Performance:** Coil image caching significantly increased to support faster gallery loading of high-resolution construction photos.
- **Security:** Disabled `allowBackup` to prevent sensitive project photos/data from leaking to user cloud accounts.
- **Network:** Explicitly disabled cleartext network traffic via network security configurations.

### Fixed
- **Coroutines:** Fixed a memory leak in the camera recording timer.
- **OOM Errors:** Added strict bounding options and safe downsizing when loading enormous photos into memory for PDF reports.
- **Build Configurations:** Resolved Gradle extension conflicts and updated the `compileSdk` to 36 for optimal compatibility.
