# :feature:export

The `:feature:export` module isolates the presentation and UI state management for project formatting, sharing, and file export options (PDF / ZIP).

## Responsibilities
- **Export Dialog UI**: Houses `ExportDialog` and format option cards (PDF vs ZIP, compression quality selectors).
- **Export Calculation & State**: Employs `ExportViewModel` to asynchronously calculate predicted PDF/ZIP sizes and file byte metrics without locking UI threads.
- **Worker Coordination**: Triggers background export jobs via `ReportRepository` bindings.

## Dependencies
- `:core:ui`, `:core:model`, `:core:domain`, `:core:common`
- Jetpack Compose & Hilt ViewModel

## Architectural Role
```
[:feature:project] --> [:feature:export] --> [:core:*]
```
Provides modular components that any UI screen can present to share project logs and media.
