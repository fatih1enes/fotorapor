package com.fatihenes.photoreport.ui.launch

/**
 * PhotoReport Launch Experience — Deterministic Phase Enum
 *
 * Her fazın tek bir sorumluluğu vardır ve timeline controller
 * tarafından sırayla ilerlenir. Composable katman hangi fazda
 * olunduğuna bakarak ilgili animasyonu render eder.
 */
enum class LaunchPhase {
    /** Koyu, sade arka plan. Hiçbir element görünmez. */
    DarkIntro,

    /** Logo + "PhotoReport" yazısı sinematik reveal ile ortaya çıkar. */
    BrandReveal,

    /** Viewfinder bracket'leri ve focus dot animate olur. */
    CameraReveal,

    /** Shutter close → hold → release. Fotoğraf çekme anı. */
    Capture,

    /** Kontrollü, kısa flaş patlaması. */
    Flash,

    /** Flaş sönerken altındaki Home ekranı ortaya çıkar. */
    HomeReveal
}
