# PhotoReport

> A modern, offline-first Android application designed for field documentation, media organization, and seamless PDF/ZIP exports.

<div>
  <img src="https://img.shields.io/badge/Kotlin-1.9.0-purple.svg?logo=kotlin" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-blue.svg?logo=android" alt="Compose" />
  <img src="https://img.shields.io/badge/CameraX-Hardware%20Accelerated-green.svg" alt="CameraX" />
  <img src="https://img.shields.io/badge/AVIF-Zero%20Loss%20Compression-orange.svg" alt="AVIF" />
</div>

## Overview

PhotoReport simplifies the daily workflow of professionals and teams by allowing them to quickly snap, organize, and annotate photos, grouped by day and project. It ensures data is never lost by operating completely offline with robust SQLite storage via Room, and provides one-click generation of professional PDF reports and ZIP archives.

## Key Features

- **Project & Daily Log Hierarchy:** Organize your media by separate projects, and within those projects, automatically group photos by the day they were taken.
- **Hardware-Accelerated CameraX Pipeline:** A highly optimized custom camera that utilizes native HDR and hardware optimizations specific to device manufacturers.
- **AVIF Image Compression:** Dramatically reduce storage usage (up to 70% smaller than JPEG) without sacrificing visual quality via `HeifCoder`.
- **Zero-Crash Background Export:** Export massive projects to high-quality PDF reports or ZIP files in the background using `WorkManager`, with real-time notification progress.
- **GPS Watermarking:** Automatically overlay project name, date/time, and physical location (latitude/longitude and address) on captured photos using Fused Location Provider.
- **Performance Focused:** Built heavily on Jetpack Compose with strict adherence to unidirectional data flow (MVI) to guarantee 60 FPS scrolling even with thousands of local images.
- **Offline First:** No servers, no accounts. All data is managed locally on the device using Room Database with built-in backup and restore utilities.

## Tech Stack & Architecture

PhotoReport adheres to modern Android Development (MAD) best practices, utilizing a clean architecture pattern and single-responsibility principles.

- **UI:** Jetpack Compose, Material Design 3, Coil (Video & AVIF Support)
- **Architecture:** MVVM / MVI State Management
- **Local Storage:** Room Database, DataStore Preferences
- **Media Pipeline:** CameraX (Photo/Video capture), AVIF-Coder (Compression), Native Android PDF API (Report Generation)
- **Dependency Injection:** Hilt / Dagger
- **Background Processing:** WorkManager (Media Processing & Exporting), Coroutines, Flow

## Permissions

The app strictly requests only the permissions it actively uses:
- `CAMERA`: Core functionality for capturing logs.
- `RECORD_AUDIO`: Optional video recording support.
- `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`: Optional, strictly used for stamping physical location coordinates as a visual watermark on media.
- `FOREGROUND_SERVICE`: Ensuring background exports aren't killed by the OS.

## License
MIT License. Open source and free to use.
