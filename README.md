# PhotoReport

> A modern, offline-first Android application designed for field documentation, media organization, and seamless PDF/ZIP exports.

<div align="center">
  <img src="https://img.shields.io/badge/Kotlin-2.0-purple.svg?logo=kotlin" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-blue.svg?logo=android" alt="Compose" />
  <img src="https://img.shields.io/badge/Architecture-MVVM-green.svg" alt="Architecture" />
  <img src="https://img.shields.io/badge/Database-Room-orange.svg" alt="Room" />
  <img src="https://img.shields.io/badge/DI-Hilt-red.svg" alt="Hilt" />
</div>

## Overview

**PhotoReport** is a professional-grade utility for field engineers, site inspectors, and project managers. It enables high-speed site documentation by organizing visual data into structured project logs. Operating entirely offline, it ensures data privacy and reliability in environments with poor connectivity.

## Key Features

- **📂 Project-Based Organization:** Hierarchical structure grouping media by specific projects and auto-categorized by daily inspection logs.
- **📷 Advanced Camera Module:** Custom CameraX implementation with hardware HDR support, grid overlays, and manufacturer-specific optimizations.
- **🖼️ AVIF & WebP Support:** Zero-loss and high-efficiency image compression to maximize device storage without compromising audit quality.
- **📍 GPS & Timestamp Watermarking:** Automatic overlay of project names, precise coordinates, and time-stamped addresses directly onto the visual evidence.
- **📄 Professional Reporting:** One-touch generation of PDF field reports and HTML-based ZIP archives for desktop review.
- **♻️ Smart Trash System:** 30-day retention for deleted items to prevent accidental data loss.
- **💾 Local Backup & Restore:** Encrypted ZIP-based backup utility for seamless data migration between devices.

## Tech Stack

- **UI Layer:** Jetpack Compose with Material 3 (MD3) Design System.
- **Async & Streams:** Kotlin Coroutines & Flow for reactive state management.
- **Persistence:** Room (SQLite) with multi-instance invalidation for background workers.
- **Background Tasks:** WorkManager for heavy media processing and export operations.
- **Image Loading:** Coil 3.x with Video and AVIF extensions.
- **Dependency Injection:** Dagger Hilt for robust and testable architecture.

## Getting Started

1. **Clone the repository:**
   ```bash
   git clone https://github.com/yourusername/PhotoReport.git
   ```
2. **Open in Android Studio:** Ladybug (2024.2.1) or newer is recommended.
3. **Build & Run:** The project uses standard Gradle configuration. No external API keys are required for the core build.

## Project Structure

```text
app/
├── src/main/java/.../data/        # Room Entities, DAOs, and Database
├── src/main/java/.../di/          # Hilt Modules
├── src/main/java/.../manager/     # PDF, Export, and Backup Logic
├── src/main/java/.../ui/          # Compose Screens & ViewModels
├── src/main/java/.../util/        # Image processing & helper classes
└── src/main/res/                  # Resources & Strings (TR/EN support)
```

## License

Copyright © 2026. Distributed under the MIT License. See `LICENSE` for more information.
