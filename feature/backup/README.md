# :feature:backup

The `:feature:backup` module encapsulates archive generation and system recovery flows for application data and media files.

## Responsibilities
- **Backup & Restore UI**: Provides `BackupSection`, presenting self-contained user action triggers for ZIP archive creation and document restoration.
- **Progress Tracking**: Consumes OperationResult streams in `BackupViewModel`, reporting live loading indicators and feedback snackbars to users during intensive disk IO.
- **Decoupled Settings**: Removes heavy backup logic and repository dependencies from general app settings screens.

## Dependencies
- `:core:ui`, `:core:model`, `:core:domain`, `:core:common`
- Jetpack Compose & Hilt ViewModel

## Architectural Role
```
[:feature:settings] --> [:feature:backup] --> [:core:*]
```
Ensures isolation of storage operations from simple application settings and display logic.
