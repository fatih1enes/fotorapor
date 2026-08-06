# :core:domain

The `:core:domain` module encapsulates the core business logic of the PhotoReport application following clean architectural principles.

## Responsibilities
- **Repository Interfaces**: Defines abstract contracts for domain operations (`ProjectRepository`, `ReportRepository`, `SettingsRepository`, `BackupRepository`).
- **Use Cases / Interactors**: Encapsulates specific domain workflows (e.g., `ExportProjectUseCase`, `DeleteProjectUseCase`).
- **Decoupling**: Serves as the crucial boundary insulating feature UI layers and ViewModels from raw data implementation details.

## Dependencies
- `:core:model` (Domain models, value objects, and enums)
- `:core:common` (Result monads and dispatchers)

## Architectural Role
```
[:feature:*] --> [:core:domain] <-- [:core:database / :app]
```
This module relies strictly on pure domain contracts with zero dependency on the Android SDK framework or persistence frameworks like Room/DataStore.
