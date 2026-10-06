package com.fatihenes.photoreport.feature.camera.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object CameraTokens {
    // Colors
    val Amber = Color(0xFFFFD60A)
    val AmberGlow = Color(0x66FFD60A)
    val BackgroundDark = Color(0xFF000000)
    val SurfaceFrosted = Color(0x99000000)
    val SurfaceTranslucent = Color(0x40000000)
    val ControlBg = Color(0x661A1A1A)
    val ControlBorder = Color(0x33FFFFFF)
    val RecordRed = Color(0xFFFF3B30)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0x99FFFFFF)
    val GridLineColor = Color(0x33FFFFFF)
    val LevelInactive = Color(0x55FFFFFF)

    // Dimensions
    val TopBarHeight = 56.dp
    val ShutterOuterSize = 76.dp
    val ShutterInnerSize = 64.dp
    val ShutterInnerRecordingSize = 32.dp
    val ThumbnailSize = 48.dp
    val LensSwitchSize = 48.dp
    val ZoomPillHeight = 36.dp
    val MinTouchTarget = 48.dp

    // Typography
    val StatusBadgeFontSize = 11.sp
    val ZoomLabelFontSize = 12.sp
    val ModeLabelFontSize = 14.sp
    val WatermarkMonospaceFontSize = 10.sp

    // Aspect Ratios
    const val AspectRatio4_3 = 3f / 4f
    const val AspectRatio16_9 = 9f / 16f

    // Photo Target Resolutions (12MP - iPhone default)
    const val PhotoTarget4_3Width = 4032
    const val PhotoTarget4_3Height = 3024
    const val PhotoTarget16_9Width = 4032
    const val PhotoTarget16_9Height = 2268

    // Zoom Tiers (iPhone-style)
    const val ZoomTierUltraWide = 0.5f
    const val ZoomTierWide = 1f
    const val ZoomTierTelephoto2x = 2f
    const val ZoomTierTelephoto3x = 3f
    const val ZoomTierTelephoto5x = 5f

    // Zoom Tier Tolerances
    const val ZoomTierTolerancePercent = 0.15f
    const val ZoomTierMinTolerance = 0.12f
    const val ZoomTierMaxTolerance = 0.5f
    const val UltraWideActiveThreshold = 0.95f
    const val UltraWideActiveOffset = 0.15f
    const val Telephoto2xThreshold = 1.8f
    const val Telephoto3xThreshold = 3.5f
    const val Telephoto5xThreshold = 4.5f

    // Animation Specs
    // Shutter press: snappy and immediate — no bouncy rebound which would look
    // amateurish on a professional photography tool.
    val ShutterSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh
    )
    // Focus reticle pop-in: slight overshoot communicates "acquired" tactilely.
    val FocusSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
    val SmoothSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
    val SmoothSpringDp = spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
    // Crossfade between lens/mode switches: fast enough to feel instant,
    // slow enough not to glitch on slower devices.
    const val CrossfadeDuration = 120
    // Shutter blink: 25 ms in, 60 ms out — mirrors Apple's timing
    const val ShutterBlinkInMs = 25
    const val ShutterBlinkOutMs = 60
}
