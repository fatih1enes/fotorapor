# :core:datastore

The `:core:datastore` module houses reactive user preference management for the PhotoReport application using Jetpack Preferences DataStore.

## Responsibilities
- **Preferences Storage**: Manages non-relational key-value configurations (Theme modes, language preferences, GPS watermark toggles, AVIF optimization switches).
- **Data Source Abstraction**: Provides `SettingsDataSource` to decouple repositories from DataStore APIs.

## Dependencies
- `:core:model` (AppSettings domain representations)
- Jetpack Preferences DataStore & Coroutines Flow

## Architectural Role
```
[:app / :core:domain] <-- [:core:datastore]
```
Ensures thread-safe, non-blocking asynchronous preferences reading and writing across application lifecycles.
