# :core:database

The `:core:database` module provides local relational data persistence for the PhotoReport application using Android Room.

## Responsibilities
- **Room Database**: Defines `AppDatabase`, version management, and automated database migrations.
- **DAOs & Entities**: Encapsulates raw table definitions and SQL queries (`ProjectDao`, `LogDao`, `PhotoDao`).
- **Data Sources & Mappers**: Implements explicit Data Source abstractions (`ProjectLocalDataSourceImpl`) and bidirectional mappers to decouple raw Room entities from clean domain models.

## Dependencies
- `:core:model` (Target domain models for mapper layer)
- `:core:domain` (Local data source abstractions)
- Room Runtime, KSP Compiler & Paging

## Architectural Role
```
[:app / :core:domain] <-- [:core:database]
```
No UI or feature modules directly import `:core:database`; all database interaction occurs solely through interface contracts exposed by `:core:domain`.
