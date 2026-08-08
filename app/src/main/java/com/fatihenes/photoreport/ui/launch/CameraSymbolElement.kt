@file:Suppress("MagicNumber")

package com.fatihenes.photoreport.ui.launch

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * PhotoReport Bespoke Camera Symbol — Mechanical Pop-up Flash DSLR Variant
 *
 * Modern, minimalist, endüstriyel standartlarda özgün DSLR kamera sembolü.
 * Geometrik kamera gövdesi, yaylı pop-up mekanik flaş ünitesi, çok katmanlı
 * optik lens ve mekanik diafram (aperture) bıçakları.
 *
 * @param revealProgress      0→1: Sembolün belirme ilerlemesi
 * @param captureProgress     0→1: Deklanşör/fotoğraf çekim anı (bıçak kapanması + tepme)
 * @param flashPopupProgress  0→1: Pop-up flaş kafasının yukarı açılma hareketi
 * @param flashBurstProgress  0→1: Flaş kafasından yayılan optik parlama
 * @param focusDotScale       0→1: Merkez odak noktasının ölçeği
 * @param primaryColor        Ana gövde ve lens kontur rengi
 * @param accentColor         Pirinç (Brass) odak noktası rengi
 */
@Composable
fun CameraSymbolElement(
    revealProgress: Float,
    captureProgress: Float,
    flashPopupProgress: Float,
    flashBurstProgress: Float,
    focusDotScale: Float,
    primaryColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val alpha = revealProgress.coerceIn(0f, 1f)
        if (alpha <= 0f) return@Canvas

        drawCameraSymbol(
            alpha = alpha,
            revealProgress = revealProgress.coerceIn(0f, 1f),
            captureProgress = captureProgress.coerceIn(0f, 1f),
            flashPopupProgress = flashPopupProgress.coerceIn(0f, 1f),
            flashBurstProgress = flashBurstProgress.coerceIn(0f, 1f),
            focusDotScale = focusDotScale.coerceIn(0f, 2f),
            primaryColor = primaryColor,
            accentColor = accentColor
        )
    }
}

private fun DrawScope.drawCameraSymbol(
    alpha: Float,
    revealProgress: Float,
    captureProgress: Float,
    flashPopupProgress: Float,
    flashBurstProgress: Float,
    focusDotScale: Float,
    primaryColor: Color,
    accentColor: Color
) {
    val cx = size.width / 2f
    val cy = size.height / 2f

    // ── Shutter Recoil Effect (Çekim anında gövdede mekanik tepme) ──────
    val recoilScale = if (captureProgress > 0f) {
        when {
            captureProgress <= 0.4f -> 1.0f - (0.05f * (captureProgress / 0.4f))
            captureProgress <= 0.7f -> 0.95f + (0.07f * ((captureProgress - 0.4f) / 0.3f))
            else -> 1.02f - (0.02f * ((captureProgress - 0.7f) / 0.3f))
        }
    } else 1.0f

    val baseWidth = minOf(size.width, size.height) * 0.76f * recoilScale
    val bodyWidth = baseWidth
    val bodyHeight = baseWidth * 0.62f

    val bodyCenterY = cy + (bodyHeight * 0.08f)
    val bodyLeft = cx - (bodyWidth / 2f)
    val bodyTop = bodyCenterY - (bodyHeight / 2f)

    val strokeWidth = 1.8.dp.toPx()
    val thinStroke = 1.1.dp.toPx()

    val mainColor = primaryColor.copy(alpha = alpha * 0.90f)
    val mutedColor = primaryColor.copy(alpha = alpha * 0.45f)
    val subtleColor = primaryColor.copy(alpha = alpha * 0.22f)

    // ── 1. Pop-up Flash Housing & Mechanism (Mekanik Pop-up Flaş) ─────
    val topWidth = bodyWidth * 0.32f
    val topHeight = bodyHeight * 0.18f
    val popupLiftMax = 14.dp.toPx()
    val currentLift = popupLiftMax * flashPopupProgress

    val flashHeadWidth = topWidth * 0.75f
    val flashHeadHeight = 9.dp.toPx()
    val flashHeadLeft = cx - (flashHeadWidth / 2f)
    val flashHeadBaseY = bodyTop - topHeight
    val flashHeadTopY = flashHeadBaseY - currentLift - flashHeadHeight

    // Pop-up Flaş Destek Kolları
    if (flashPopupProgress > 0.05f) {
        val armColor = primaryColor.copy(alpha = alpha * 0.65f * flashPopupProgress)
        drawLine(
            color = armColor,
            start = Offset(cx - (topWidth * 0.30f), bodyTop - (topHeight * 0.5f)),
            end = Offset(flashHeadLeft + 2.dp.toPx(), flashHeadTopY + flashHeadHeight),
            strokeWidth = thinStroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = armColor,
            start = Offset(cx + (topWidth * 0.30f), bodyTop - (topHeight * 0.5f)),
            end = Offset(flashHeadLeft + flashHeadWidth - 2.dp.toPx(), flashHeadTopY + flashHeadHeight),
            strokeWidth = thinStroke,
            cap = StrokeCap.Round
        )
    }

    // Pop-up Flaş Kafası (Head & Strobe Lens)
    if (flashPopupProgress > 0f) {
        val headAlpha = alpha * flashPopupProgress
        val headColor = primaryColor.copy(alpha = headAlpha * 0.95f)

        drawRoundRect(
            color = headColor,
            topLeft = Offset(flashHeadLeft, flashHeadTopY),
            size = Size(flashHeadWidth, flashHeadHeight),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            style = Stroke(width = strokeWidth)
        )

        val glassWidth = flashHeadWidth * 0.60f
        val glassHeight = flashHeadHeight * 0.50f
        val glassLeft = cx - (glassWidth / 2f)
        val glassTop = flashHeadTopY + ((flashHeadHeight - glassHeight) / 2f)

        val glassColor = if (flashBurstProgress > 0f) {
            Color.White
        } else {
            accentColor.copy(alpha = headAlpha * 0.70f)
        }

        drawRoundRect(
            color = glassColor,
            topLeft = Offset(glassLeft, glassTop),
            size = Size(glassWidth, glassHeight),
            cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
        )
    }

    // ── 2. Top Viewfinder / Prism Hump (Sabit Üst Vizör Tabanı) ───────
    val topPath = Path().apply {
        moveTo(cx - (topWidth / 2f), bodyTop)
        lineTo(cx - (topWidth * 0.36f), bodyTop - topHeight)
        lineTo(cx + (topWidth * 0.36f), bodyTop - topHeight)
        lineTo(cx + (topWidth / 2f), bodyTop)
    }

    drawPath(
        path = topPath,
        color = mainColor,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )

    // ── 3. Shutter Button Accent (Deklanşör Düğmesi) ──────────────────
    val btnWidth = bodyWidth * 0.14f
    val btnHeight = 3.5.dp.toPx()
    val btnLeft = cx + (bodyWidth * 0.24f)
    val btnTop = bodyTop - btnHeight - 1.dp.toPx()

    drawRoundRect(
        color = mainColor,
        topLeft = Offset(btnLeft, btnTop),
        size = Size(btnWidth, btnHeight),
        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
    )

    // ── 4. Main Camera Body (Kamera Gövdesi Konturu) ──────────────────
    val cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
    drawRoundRect(
        color = mainColor,
        topLeft = Offset(bodyLeft, bodyTop),
        size = Size(bodyWidth, bodyHeight),
        cornerRadius = cornerRadius,
        style = Stroke(width = strokeWidth)
    )

    drawRoundRect(
        color = subtleColor,
        topLeft = Offset(bodyLeft, bodyTop),
        size = Size(bodyWidth, bodyHeight),
        cornerRadius = cornerRadius
    )

    // ── 5. Rangefinder / Sensor Window (Sensör Penceresi) ────────────
    val sensorWidth = bodyWidth * 0.10f
    val sensorHeight = bodyHeight * 0.12f
    val sensorLeft = bodyLeft + (bodyWidth * 0.12f)
    val sensorTop = bodyTop + (bodyHeight * 0.18f)

    drawRoundRect(
        color = mutedColor,
        topLeft = Offset(sensorLeft, sensorTop),
        size = Size(sensorWidth, sensorHeight),
        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
        style = Stroke(width = thinStroke)
    )

    // ── 6. Central Lens Assembly (Merkezi Optik Lens Grubu) ───────────
    val lensCenterX = cx
    val lensCenterY = bodyCenterY + (bodyHeight * 0.02f)
    val outerLensRadius = bodyHeight * 0.40f
    val innerLensRadius = outerLensRadius * 0.72f
    val apertureRadius = innerLensRadius * 0.55f

    drawCircle(
        color = mainColor,
        radius = outerLensRadius,
        center = Offset(lensCenterX, lensCenterY),
        style = Stroke(width = strokeWidth)
    )

    drawCircle(
        color = mutedColor,
        radius = innerLensRadius,
        center = Offset(lensCenterX, lensCenterY),
        style = Stroke(width = thinStroke)
    )

    // ── 7. Mechanical Aperture Blades (Diafram Bıçakları) ─────────────
    val bladeClosure = if (captureProgress > 0f) {
        when {
            captureProgress <= 0.4f -> (captureProgress / 0.4f)
            captureProgress <= 0.7f -> 1.0f
            else -> 1.0f - ((captureProgress - 0.7f) / 0.3f)
        }
    } else 0f

    val bladeCount = 6
    val currentApertureR = innerLensRadius * (0.85f - (0.35f * bladeClosure))
    val rotationOffset = Math.toRadians((30f * bladeClosure).toDouble())

    for (i in 0 until bladeCount) {
        val angle = (2 * Math.PI / bladeCount * i) + rotationOffset
        val startX = lensCenterX + (innerLensRadius * cos(angle).toFloat())
        val startY = lensCenterY + (innerLensRadius * sin(angle).toFloat())
        val endX = lensCenterX + (currentApertureR * cos(angle + 0.65).toFloat())
        val endY = lensCenterY + (currentApertureR * sin(angle + 0.65).toFloat())

        drawLine(
            color = mutedColor.copy(alpha = alpha * (0.40f + 0.45f * bladeClosure)),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = thinStroke,
            cap = StrokeCap.Round
        )
    }

    // ── 8. Signature Brass Focus Core (Pirinç Odak Noktası) ───────────
    if (focusDotScale > 0f) {
        val baseDotRadius = apertureRadius * 0.38f
        val currentDotRadius = baseDotRadius * focusDotScale

        drawCircle(
            color = accentColor.copy(alpha = alpha * 0.28f),
            radius = currentDotRadius * 1.8f,
            center = Offset(lensCenterX, lensCenterY)
        )

        drawCircle(
            color = accentColor.copy(alpha = alpha),
            radius = currentDotRadius,
            center = Offset(lensCenterX, lensCenterY)
        )
    }

    // ── 9. Soft Optical Strobe Lens Bloom (Flaş Kafasında Yumuşak Parlama) ──
    if (flashBurstProgress > 0f) {
        val flashOriginX = cx
        val flashOriginY = flashHeadTopY + (flashHeadHeight / 2f)
        val burstAlpha = flashBurstProgress.coerceIn(0f, 1f)

        // Yumuşak dairesel atmosferik lens halo / bloom
        val bloomRadius = 36.dp.toPx() * burstAlpha
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White,
                    Color.White.copy(alpha = 0.70f * burstAlpha),
                    accentColor.copy(alpha = 0.30f * burstAlpha),
                    Color.Transparent
                ),
                center = Offset(flashOriginX, flashOriginY),
                radius = bloomRadius * 1.6f
            ),
            radius = bloomRadius * 1.6f,
            center = Offset(flashOriginX, flashOriginY)
        )
    }
}
