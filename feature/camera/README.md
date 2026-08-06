# :feature:camera

The `:feature:camera` module contains the self-contained photo capture and live preview camera UI, built on top of AndroidX CameraX and Jetpack Compose.

## Responsibilities
- **Camera Capture Flow**: Encapsulates `CameraScreen` and its dedicated state management (`CameraStateHolder`, `CameraViewModel`).
- **Media Optimization**: Coordinates background photo saving, GPS metadata embedding, orientation correction, and AVIF compression toggles.
- **Deconstructed UI**: Splits controls, shutter buttons, and focus overlays into clean composables without God-classes.

## Dependencies
- `:core:ui`, `:core:model`, `:core:domain`, `:core:common`, `:core:media`
- AndroidX CameraX (`camera-camera2`, `camera-lifecycle`, `camera-view`) & Hilt Navigation Compose

## Architectural Role
```
[:app NavGraph] --> [:feature:camera] --> [:core:*]
```
Completely decoupled from other feature modules; navigation outcomes return via Kotlin lambdas to the primary routing layer.
