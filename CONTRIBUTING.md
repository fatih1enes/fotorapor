# Contributing to PhotoReport

First off, thank you for considering contributing to PhotoReport! 

## Getting Started

1. **Fork the repository** on GitHub.
2. **Clone your fork** locally: `git clone https://github.com/your-username/PhotoReport.git`
3. **Set up the keystore**: 
   - Rename `keystore.properties.example` to `keystore.properties`.
   - Update the variables inside with your local debug/release keystore details.
   - If you don't have one, Android Studio will generate a debug keystore automatically, but for `release` builds you need to specify a `.jks` file.
4. **Build the project** in Android Studio to ensure everything is working correctly. (Requires Android Studio Iguana or newer, and JDK 17).

## Branching Strategy

- `master` is the main development branch.
- For new features or bug fixes, create a new branch branching off from `master` (e.g., `feature/gps-watermark` or `bugfix/crash-on-export`).

## Submitting Pull Requests

1. **Keep it focused:** A Pull Request should ideally address a single bug or feature.
2. **Test your code:** Ensure your changes do not break existing functionality. Run `./gradlew lintDebug` and `./gradlew assembleDebug` before submitting.
3. **Describe your changes:** Provide a clear and comprehensive description of what your PR does and why it is needed.

## Code Style

- The project heavily uses **Kotlin Coroutines**, **Flow**, and **Jetpack Compose**.
- Follow the official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html).
- Keep UI components separate from business logic (MVI / MVVM architecture).

Thank you for helping to improve PhotoReport!
