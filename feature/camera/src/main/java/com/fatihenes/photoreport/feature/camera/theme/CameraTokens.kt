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

    // Animation Specs
    val ShutterSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
    val SmoothSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
    val SmoothSpringDp = spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
    val CrossfadeDuration = 180
}
