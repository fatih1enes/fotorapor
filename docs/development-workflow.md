# Developer Workflow

This document outlines the standard development workflow for the PhotoReport project. Following these steps ensures high code quality and smooth collaboration.

## Standard Workflow

1.  **Sync Project**: Ensure your local environment is up to date with the latest dependencies.
2.  **Assemble Project**: Run `./gradlew assembleDebug` to verify that the project builds correctly.
3.  **Inspect Code**: Use Android Studio's built-in inspections for real-time feedback.
4.  **Fix Findings**: Address any issues reported by the IDE or quality tools.
5.  **Run Detekt**: Run `./gradlew detekt` to check for code smells and complex logic.
6.  **Run Ktlint**: Run `./gradlew ktlintCheck` to verify code style.
    *   *Tip*: Use `./gradlew ktlintFormat` to automatically fix style issues.
7.  **Run Quality**: Run `./gradlew quality` to execute all checks (Detekt, Ktlint, Lint, and Tests).
8.  **Code Cleanup**: Perform final refactoring and cleanup.
9.  **Test on Device**: Verify the app's behavior on a physical device or emulator.
10. **Commit & Push**: Commit your changes and push them to the repository.

## Quality Tools

### Detekt
Detekt is used for static code analysis. It focuses on finding code smells, complexity, and potential bugs.
- **Run**: `./gradlew detekt`
- **Baseline**: Existing issues are stored in `[module]/detekt-baseline.xml`. New issues will fail the build.
- **Reports**: Generated in `[module]/build/reports/detekt/`.

### Ktlint
Ktlint is a Kotlin linter and formatter. It ensures a consistent code style across the project.
- **Check**: `./gradlew ktlintCheck`
- **Format**: `./gradlew ktlintFormat`
- **Reports**: Generated in `[module]/build/reports/ktlint/`.

### Quality Task
A unified task that runs all verification tools.
- **Run**: `./gradlew quality`
- **Included tasks**: `detekt`, `ktlintCheck`, `lint`, and `test`.

### LeakCanary
LeakCanary is integrated to detect memory leaks in debug builds.
- **Usage**: It runs automatically in debug builds. If a leak is detected, you will receive a notification on the device.

## GitHub Actions
Our CI pipeline runs the `quality` task and builds the debug APK on every push and pull request to `master` and `main` branches.
