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

/**
 * PhotoReport Launch Experience — Sinematik Brand Reveal
 *
 * 6 fazlı sinematik animasyon:
 * DARK INTRO → BRAND REVEAL → CAMERA REVEAL → CAPTURE (POP-UP FLASH) → FLASH BURST → HOME REVEAL
 *
 * Flaş zirveye ulaştığı an (100% beyaz ekran), kamera ve koyu arka plan anında kaybolur.
 * Beyaz flaş parlaklığı sönümlenirken (fade out), arkadaki gerçek Ana Ekran (Home/Dashboard)
 * ortaya çıkar.
 *
 * @param isReady Uygulama verisi yüklendi mi (settings vs.)
 * @param onComplete Animasyon tamamlandığında çağrılır
 */
@Composable
fun PhotoReportLaunchExperience(
    isReady: Boolean,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeline = rememberLaunchTimeline()

    // ── Animasyonu yalnızca uygulama hazır olduğunda başlat ──────────
    LaunchedEffect(isReady) {
        if (isReady) {
            timeline.start()
        }
    }

    val isComplete by timeline.isComplete
    LaunchedEffect(isComplete) {
        if (isComplete) onComplete()
    }

    // ── Phase progress ───────────────────────────────────────────────
    val brandProgress by timeline.brandRevealProgress
    val cameraProgress by timeline.cameraRevealProgress
    val captureProgress by timeline.captureProgress
    val flashProgress by timeline.flashProgress
    val homeRevealProgress by timeline.homeRevealProgress

    val easing = remember { CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f) }

    // ── Derived values ───────────────────────────────────────────────

    // Brand Reveal (Yazı ve Genel Marka Girişi)
    val brandAlpha = easing.transform(brandProgress)
    val brandScale = lerp(0.94f, 1.0f, easing.transform(brandProgress))

    // Camera Symbol Reveal
    val symbolReveal = easing.transform(cameraProgress)

    // Mechanical Pop-up Flash Progress (Flaşın vizör üstünden yaylı açılması)
    val flashPopupProgress = when {
        captureProgress <= 0.25f -> 0f
        captureProgress <= 0.85f -> easing.transform((captureProgress - 0.25f) / 0.60f)
        else -> 1f
    }

    // Flash Burst Progress (Optik parlama başlangıcı)
    val flashBurstProgress = when {
        flashProgress <= 0f -> 0f
        flashProgress <= 0.35f -> flashProgress / 0.35f
        else -> 1f
    }

    // Focus dot pulse (Çekim anında odak noktası parlaklığı)
    val focusPulse = if (captureProgress > 0f) {
        when {
            captureProgress <= 0.4f -> lerp(1.0f, 1.4f, captureProgress / 0.4f)
            captureProgress <= 0.7f -> 1.4f
            else -> lerp(1.4f, 1.0f, (captureProgress - 0.7f) / 0.3f)
        }
    } else 1f

    // Kamera, yazı ve koyu arka plan flaş zirveye ulaştığı an (veya HomeReveal başlayınca) gizlenir.
    // Böylece flaş sönümlenirken arkada doğrudan GERÇEK ANA EKRAN görünür.
    val launchUiAlpha = if (flashProgress >= 0.70f || homeRevealProgress > 0f) {
        0f
    } else 1f

    // Home reveal overlay opacity (Flaş kaplamasının kaybolarak arkadaki ana ekranı açması)
    val homeOverlayAlpha = if (homeRevealProgress > 0f) {
        1f - easing.transform(homeRevealProgress)
    } else 1f

    if (isComplete) return

    // ── Render ───────────────────────────────────────────────────────

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // ── Launch UI Elements (Kamera + Tipografi + Koyu Arka Plan) ───
        if (launchUiAlpha > 0f) {
            // Cinematic background — radial vignette
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = launchUiAlpha }
                    .background(Graphite950)
                    .background(
                        Brush.radialGradient(
                            0.0f to Indigo900.copy(alpha = 0.55f),
                            0.50f to Indigo900.copy(alpha = 0.20f),
                            1.0f to Color.Transparent
                        )
                    )
            )

            // Bespoke DSLR Camera Symbol
            CameraSymbolElement(
                revealProgress = symbolReveal,
                captureProgress = captureProgress,
                flashPopupProgress = flashPopupProgress,
                flashBurstProgress = flashBurstProgress,
                focusDotScale = if (cameraProgress > 0f) symbolReveal * focusPulse else 0f,
                primaryColor = Graphite050,
                accentColor = Brass300,
                modifier = Modifier
                    .size(180.dp)
                    .offset(y = (-32).dp)
                    .graphicsLayer { alpha = launchUiAlpha }
            )

            // "PhotoReport" Typography
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
                        alpha = brandAlpha * launchUiAlpha
                        scaleX = brandScale
                        scaleY = brandScale
                    }
            )
        }

        // ── Radial Flash Expansion & Transition Overlay ──────────────
        if (flashProgress > 0f || homeRevealProgress in 0.001f..0.999f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val flashOriginX = size.width / 2f
                val flashOriginY = (size.height / 2f) - 32.dp.toPx() - 54.dp.toPx()

                val maxRadius = max(size.width, size.height) * 1.5f

                val expansionProgress = if (flashProgress > 0f && homeRevealProgress <= 0f) {
                    easing.transform(flashProgress.coerceIn(0f, 1f))
                } else 1f

                val currentRadius = max(12.dp.toPx(), maxRadius * expansionProgress)
                val alphaVal = if (homeRevealProgress > 0f) homeOverlayAlpha else 1f

                if (currentRadius < maxRadius && homeRevealProgress <= 0f) {
                    // Flaş kafasından ekrana büyüyen dairesel ışık patlaması
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.0f to Color.White,
                                0.70f to Color.White,
                                0.92f to Color.White.copy(alpha = 0.85f),
                                1.0f to Color.Transparent
                            ),
                            center = Offset(flashOriginX, flashOriginY),
                            radius = currentRadius
                        ),
                        radius = currentRadius,
                        center = Offset(flashOriginX, flashOriginY),
                        alpha = alphaVal
                    )
                } else {
                    // Tüm ekranı kaplayan beyaz flaş katmanı (Sönümlenirken arkadaki Home görünür)
                    drawRect(
                        color = Color.White,
                        alpha = alphaVal
                    )
                }
            }
        }
    }
}

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
