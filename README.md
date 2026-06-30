# Elektrik - Electrical Project Photo Management

Elektrik is an Android application designed for electrical engineers and technicians to document and manage project photos efficiently. It allows users to organize photos by project and date, add notes, and export project data.

## Features

- **Project Management**: Create and organize multiple electrical projects.
- **Daily Logging**: Group photos and notes by date for each project.
- **Advanced Camera**: 
    - Real-time optimization.
    - WebP support for efficient storage.
    - Integrated with CameraX.
- **Photo Editing**: Basic photo rotation and management.
- **Export Options**: Export project data and photos (supports multiple formats).
- **Dark/Light Mode**: Full Material 3 support with theme switching.
- **Trash System**: Recover deleted projects or permanently remove them.

## Tech Stack

- **UI**: Jetpack Compose (Material 3)
- **Architecture**: MVVM with Clean Architecture principles
- **Dependency Injection**: Hilt
- **Database**: Room
- **Navigation**: Compose Navigation
- **Image Loading**: Coil 3
- **Media**: CameraX & Media3
- **Background Tasks**: WorkManager

## Getting Started

1. Clone the repository.
2. Open in Android Studio (Ladybug or newer).
3. Build and Run.

> **Note**: Make sure to configure your own `keystore.properties` for release builds.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
