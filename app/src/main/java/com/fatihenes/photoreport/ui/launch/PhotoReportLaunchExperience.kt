@file:Suppress("MagicNumber")

package com.fatihenes.photoreport.ui.launch

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatihenes.photoreport.core.designsystem.theme.Brass300
import com.fatihenes.photoreport.core.designsystem.theme.DisplayFontFamily
import com.fatihenes.photoreport.core.designsystem.theme.Graphite050
import com.fatihenes.photoreport.core.designsystem.theme.Graphite950
import com.fatihenes.photoreport.core.designsystem.theme.Indigo900
import kotlin.math.max

@Composable
fun PhotoReportLaunchExperience(
    isReady: Boolean,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeline = rememberLaunchTimeline()
    LaunchedEffect(isReady) { if (isReady) timeline.start() }

    val isComplete by timeline.isComplete
    LaunchedEffect(isComplete) { if (isComplete) onComplete() }

    if (isComplete) return

    val easing = remember { CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f) }
    val animState = calculateAnimationState(timeline, easing)

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (animState.launchUiAlpha > 0f) {
            LaunchBackground(animState.launchUiAlpha)
            LaunchBrandElements(animState)
        }
        if (timeline.flashProgress.value > 0f || timeline.homeRevealProgress.value in 0.001f..0.999f) {
            FlashOverlay(timeline, animState, easing)
        }
    }
}

private data class DerivedAnimState(
    val brandAlpha: Float,
    val brandScale: Float,
    val symbolReveal: Float,
    val captureProgress: Float,
    val flashPopupProgress: Float,
    val flashBurstProgress: Float,
    val focusDotScale: Float,
    val launchUiAlpha: Float,
    val homeOverlayAlpha: Float
)

@Composable
private fun calculateAnimationState(timeline: LaunchTimeline, easing: CubicBezierEasing): DerivedAnimState {
    val brandProgress by timeline.brandRevealProgress
    val cameraProgress by timeline.cameraRevealProgress
    val captureProgress by timeline.captureProgress
    val flashProgress by timeline.flashProgress
    val homeRevealProgress by timeline.homeRevealProgress

    val flashPopup = when {
        captureProgress <= 0.25f -> 0f
        captureProgress <= 0.85f -> easing.transform((captureProgress - 0.25f) / 0.60f)
        else -> 1f
    }

    val focusPulse = if (captureProgress > 0f) {
        when {
            captureProgress <= 0.4f -> lerp(1.0f, 1.4f, captureProgress / 0.4f)
            captureProgress <= 0.7f -> 1.4f
            else -> lerp(1.4f, 1.0f, (captureProgress - 0.7f) / 0.3f)
        }
    } else 1f

    return DerivedAnimState(
        brandAlpha = easing.transform(brandProgress),
        brandScale = lerp(0.94f, 1.0f, easing.transform(brandProgress)),
        symbolReveal = easing.transform(cameraProgress),
        captureProgress = captureProgress,
        flashPopupProgress = flashPopup,
        flashBurstProgress = if (flashProgress <= 0.35f) (flashProgress / 0.35f).coerceIn(0f, 1f) else 1f,
        focusDotScale = if (cameraProgress > 0f) easing.transform(cameraProgress) * focusPulse else 0f,
        launchUiAlpha = if (flashProgress >= 0.70f || homeRevealProgress > 0f) 0f else 1f,
        homeOverlayAlpha = if (homeRevealProgress > 0f) 1f - easing.transform(homeRevealProgress) else 1f
    )
}

@Composable
private fun LaunchBackground(alphaVal: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = alphaVal }
            .background(Graphite950)
            .background(
                Brush.radialGradient(
                    0.0f to Indigo900.copy(0.55f),
                    0.50f to Indigo900.copy(0.20f),
                    1.0f to Color.Transparent
                )
            )
    )
}

@Composable
private fun LaunchBrandElements(state: DerivedAnimState) {
    CameraSymbolElement(
        animationState = CameraSymbolAnimationState(
            revealProgress = state.symbolReveal,
            captureProgress = state.captureProgress,
            flashPopupProgress = state.flashPopupProgress,
            flashBurstProgress = state.flashBurstProgress,
            focusDotScale = state.focusDotScale
        ),
        colors = CameraSymbolColors(Graphite050, Brass300),
        modifier = Modifier
            .size(180.dp)
            .offset(y = (-32).dp)
            .graphicsLayer { alpha = state.launchUiAlpha }
    )
    Text(
        text = "PhotoReport",
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        letterSpacing = 1.5.sp,
        color = Graphite050,
        modifier = Modifier
            .offset(y = 76.dp)
            .graphicsLayer {
                alpha = state.brandAlpha * state.launchUiAlpha
                scaleX = state.brandScale
                scaleY = state.brandScale
            }
    )
}

@Composable
private fun FlashOverlay(timeline: LaunchTimeline, state: DerivedAnimState, easing: CubicBezierEasing) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val flashOrigin = Offset(size.width / 2f, (size.height / 2f) - 86.dp.toPx())
        val maxRadius = max(size.width, size.height) * 1.5f
        val alphaVal = if (timeline.homeRevealProgress.value > 0f) state.homeOverlayAlpha else 1f

        if (timeline.homeRevealProgress.value <= 0f) {
            val radius = max(
                12.dp.toPx(),
                maxRadius * easing.transform(timeline.flashProgress.value.coerceIn(0f, 1f))
            )
            if (radius < maxRadius) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color.White, Color.White, Color.White.copy(0.85f), Color.Transparent),
                        flashOrigin,
                        radius
                    ),
                    radius,
                    flashOrigin,
                    alphaVal
                )
            } else drawRect(Color.White, alpha = alphaVal)
        } else drawRect(Color.White, alpha = alphaVal)
    }
}

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
