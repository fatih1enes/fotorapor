package com.fatihenes.photoreport.ui.launch

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * PhotoReport Launch Experience — Master Timeline Controller
 *
 * Tek bir `Animatable` (0f → 1f) üzerinden tüm animasyon fazlarını
 * yüzde-tabanlı bir timeline'dan derive eder. Bu sayede:
 *
 * - Fazlar arası overlap doğal şekilde ifade edilir
 * - Her composable kendi faz progress'ini observe eder
 * - Birbirinden bağımsız `LaunchedEffect` + `delay()` yerine
 *   deterministik, tek kontrol noktası
 *
 * Timeline yüzdeleri toplam ~1400ms'lik bir deneyime ayarlanmıştır.
 */
@Stable
class LaunchTimeline(
    val masterProgress: Animatable<Float, *>,
    val isReducedMotion: Boolean
) {
    // ── Phase Boundaries (master progress 0..1 üzerinde yüzde) ───────
    // Phase'ler kasıtlı olarak overlap eder — sinematik flow.

    // DarkIntro:    0.00 → 0.07  (~100ms)
    // BrandReveal:  0.07 → 0.43  (~500ms)
    // CameraReveal: 0.32 → 0.64  (~450ms) — BrandReveal ile overlap
    // Capture:      0.61 → 0.79  (~250ms) — CameraReveal ile overlap
    // Flash:        0.79 → 0.86  (~100ms)
    // HomeReveal:   0.86 → 1.00  (~200ms)

    companion object {
        // Master timeline toplam süresi (ms)
        const val TOTAL_DURATION_MS = 1400

        // Phase boundary constants
        private const val DARK_INTRO_END = 0.07f

        private const val BRAND_REVEAL_START = 0.07f
        private const val BRAND_REVEAL_END = 0.43f

        private const val CAMERA_REVEAL_START = 0.32f
        private const val CAMERA_REVEAL_END = 0.64f

        private const val CAPTURE_START = 0.61f
        private const val CAPTURE_END = 0.79f

        private const val FLASH_START = 0.79f
        private const val FLASH_END = 0.86f

        private const val HOME_REVEAL_START = 0.86f
        private const val HOME_REVEAL_END = 1.00f

        /** Reduced motion durumunda kısa fade süresi */
        const val REDUCED_MOTION_DURATION_MS = 300
    }

    // ── Derived Phase States ─────────────────────────────────────────

    /** Mevcut aktif faz. */
    val currentPhase: State<LaunchPhase> = derivedStateOf {
        val p = masterProgress.value
        when {
            p < DARK_INTRO_END -> LaunchPhase.DarkIntro
            p < BRAND_REVEAL_END -> LaunchPhase.BrandReveal
            p < CAPTURE_START -> LaunchPhase.CameraReveal
            p < FLASH_START -> LaunchPhase.Capture
            p < HOME_REVEAL_START -> LaunchPhase.Flash
            else -> LaunchPhase.HomeReveal
        }
    }

    /** Animasyon tamamen bitti mi? */
    val isComplete: State<Boolean> = derivedStateOf {
        masterProgress.value >= 1f
    }

    // ── Per-Phase Progress (0..1) ────────────────────────────────────
    // Her biri kendi faz aralığında 0→1 normalize edilmiş progress.

    val brandRevealProgress: State<Float> = derivedStateOf {
        phaseProgress(BRAND_REVEAL_START, BRAND_REVEAL_END)
    }

    val cameraRevealProgress: State<Float> = derivedStateOf {
        phaseProgress(CAMERA_REVEAL_START, CAMERA_REVEAL_END)
    }

    val captureProgress: State<Float> = derivedStateOf {
        phaseProgress(CAPTURE_START, CAPTURE_END)
    }

    val flashProgress: State<Float> = derivedStateOf {
        phaseProgress(FLASH_START, FLASH_END)
    }

    val homeRevealProgress: State<Float> = derivedStateOf {
        phaseProgress(HOME_REVEAL_START, HOME_REVEAL_END)
    }

    /** Master timeline'ı başlat. */
    suspend fun start() {
        val duration = if (isReducedMotion) REDUCED_MOTION_DURATION_MS else TOTAL_DURATION_MS
        masterProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = duration,
                easing = LinearEasing // Faz-bazlı easing'ler composable'larda uygulanır
            )
        )
    }

    // ── Internal ─────────────────────────────────────────────────────

    private fun phaseProgress(start: Float, end: Float): Float {
        val p = masterProgress.value
        return ((p - start) / (end - start)).coerceIn(0f, 1f)
    }
}

/**
 * Remember ile oluşturulan timeline instance.
 * Reduced-motion ayarını otomatik kontrol eder.
 */
@Composable
fun rememberLaunchTimeline(): LaunchTimeline {
    val context = LocalContext.current
    val isReducedMotion = remember {
        try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
            scale == 0f
        } catch (_: Exception) {
            false
        }
    }

    return remember {
        LaunchTimeline(
            masterProgress = Animatable(0f),
            isReducedMotion = isReducedMotion
        )
    }
}
