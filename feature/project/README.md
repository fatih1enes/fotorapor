# :feature:project

The `:feature:project` module drives the detailed project view, interactive weekly log calendar, and media timeline experiences.

## Responsibilities
- **Timeline & Calendar Rendering**: Hosts `ProjectDetailScreen`, date selection interfaces, and `TimelineBlock` log list views.
- **Media Inspection**: Implements full-screen immersive gallery previews and photo note management dialogs.
- **Project Detail ViewModel**: Manages project state, log creation, photo importing, and triggers navigation to camera and export dialogs.

## Dependencies
- `:core:ui`, `:core:model`, `:core:domain`, `:core:common`, `:core:media`, `:feature:export`
- Jetpack Compose & Coil AsyncImage

## Architectural Role
```
[:app NavGraph] --> [:feature:project] --> [:feature:export / :core:*]
```
Serves as the main feature orchestration screen for any specific project selected from the user's dashboard.
