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
import kotlin.math.PI

private const val PI_F = PI.toFloat()

/**
 * Visual and animation state for the Camera Symbol.
 */
data class CameraSymbolAnimationState(
    val revealProgress: Float,
    val captureProgress: Float,
    val flashPopupProgress: Float,
    val flashBurstProgress: Float,
    val focusDotScale: Float,
)

/**
 * Colors for the Camera Symbol.
 */
data class CameraSymbolColors(
    val primary: Color,
    val accent: Color,
)

private data class SymbolGeometry(
    val cx: Float,
    val cy: Float,
    val bodyLeft: Float,
    val bodyTop: Float,
    val bodyWidth: Float,
    val bodyHeight: Float,
    val bodyCenterY: Float,
)

private data class SymbolStyle(
    val strokeWidth: Float,
    val thinStroke: Float,
    val mainColor: Color,
    val mutedColor: Color,
    val subtleColor: Color,
    val colors: CameraSymbolColors,
    val alpha: Float,
)

private data class FlashDrawParams(
    val geo: SymbolGeometry,
    val hL: Float,
    val hT: Float,
    val hW: Float,
    val hH: Float,
    val style: SymbolStyle,
    val fp: Float,
)

@Suppress("FunctionNaming")
@Composable
fun CameraSymbolElement(
    animationState: CameraSymbolAnimationState,
    colors: CameraSymbolColors,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val alpha = animationState.revealProgress.coerceIn(0f, 1f)
        if (alpha <= 0f) return@Canvas
        drawCameraSymbol(alpha, animationState, colors)
    }
}

private fun DrawScope.drawCameraSymbol(
    alpha: Float,
    anim: CameraSymbolAnimationState,
    colors: CameraSymbolColors,
) {
    val recoil = calculateRecoil(anim.captureProgress.coerceIn(0f, 1f))
    val bWidth = minOf(size.width, size.height) * 0.76f * recoil
    val bHeight = bWidth * 0.62f
    val bCenterY = (size.height / 2f) + (bHeight * 0.08f)
    val bTop = bCenterY - (bHeight / 2f)

    val geo = SymbolGeometry(
        cx = size.width / 2f,
        cy = size.height / 2f,
        bodyLeft = (size.width / 2f) - (bWidth / 2f),
        bodyTop = bTop,
        bodyWidth = bWidth,
        bodyHeight = bHeight,
        bodyCenterY = bCenterY,
    )
    val style = SymbolStyle(
        strokeWidth = 1.8.dp.toPx(),
        thinStroke = 1.1.dp.toPx(),
        mainColor = colors.primary.copy(alpha * 0.9f),
        mutedColor = colors.primary.copy(alpha * 0.45f),
        subtleColor = colors.primary.copy(alpha * 0.22f),
        colors = colors,
        alpha = alpha,
    )

    drawFlashAndBloom(geo, anim, style)
    drawHousing(geo, style)
    drawOptics(geo, anim, style)
}

private fun calculateRecoil(cp: Float): Float = if (cp <= 0f) 1f else when {
    cp <= 0.4f -> 1f - (0.05f * (cp / 0.4f))
    cp <= 0.7f -> 0.95f + (0.07f * ((cp - 0.4f) / 0.3f))
    else -> 1.02f - (0.02f * ((cp - 0.7f) / 0.3f))
}

private fun DrawScope.drawFlashAndBloom(
    geo: SymbolGeometry,
    anim: CameraSymbolAnimationState,
    style: SymbolStyle,
) {
    val fp = anim.flashPopupProgress.coerceIn(0f, 1f)
    if (fp > 0f) {
        val tW = geo.bodyWidth * 0.32f
        val tH = geo.bodyHeight * 0.18f
        val lift = 14.dp.toPx() * fp
        val hW = tW * 0.75f
        val hH = 9.dp.toPx()
        val hL = geo.cx - (hW / 2f)
        val hT = geo.bodyTop - tH - lift - hH

        if (fp > 0.05f) {
            drawFlashArms(FlashDrawParams(geo, hL, hT, hW, hH, style, fp))
        }
        drawRoundRect(
            color = style.colors.primary.copy(style.alpha * fp * 0.95f),
            topLeft = Offset(hL, hT),
            size = Size(hW, hH),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = Stroke(style.strokeWidth),
        )
        val gC = if (anim.flashBurstProgress > 0f) Color.White else style.colors.accent.copy(style.alpha * fp * 0.7f)
        drawRoundRect(
            color = gC,
            topLeft = Offset(geo.cx - ((hW * 0.6f) / 2f), hT + ((hH - (hH * 0.5f)) / 2f)),
            size = Size(hW * 0.6f, hH * 0.5f),
            cornerRadius = CornerRadius(1.5.dp.toPx()),
        )
    }
    if (anim.flashBurstProgress > 0f) {
        drawFlashBurst(geo, anim, style)
    }
}

private fun DrawScope.drawFlashArms(p: FlashDrawParams) {
    val tW = p.geo.bodyWidth * 0.32f
    val tH = p.geo.bodyHeight * 0.18f
    val aC = p.style.colors.primary.copy(p.style.alpha * 0.65f * p.fp)
    drawLine(
        color = aC,
        start = Offset(p.geo.cx - (tW * 0.3f), p.geo.bodyTop - (tH * 0.5f)),
        end = Offset(p.hL + 2.dp.toPx(), p.hT + p.hH),
        strokeWidth = p.style.thinStroke,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = aC,
        start = Offset(p.geo.cx + (tW * 0.3f), p.geo.bodyTop - (tH * 0.5f)),
        end = Offset(p.hL + (p.hW - 2.dp.toPx()), p.hT + p.hH),
        strokeWidth = p.style.thinStroke,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawFlashBurst(
    geo: SymbolGeometry,
    anim: CameraSymbolAnimationState,
    style: SymbolStyle,
) {
    val hT = geo.bodyTop - (geo.bodyHeight * 0.18f) - 14.dp.toPx() - 9.dp.toPx()
    val fO = Offset(geo.cx, hT + 4.5.dp.toPx())
    val r = 36.dp.toPx() * anim.flashBurstProgress.coerceIn(0f, 1f)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(
                Color.White,
                Color.White.copy(0.7f * anim.flashBurstProgress),
                style.colors.accent.copy(0.3f * anim.flashBurstProgress),
                Color.Transparent,
            ),
            center = fO,
            radius = r * 1.6f,
        ),
        radius = r * 1.6f,
        center = fO,
    )
}

private fun DrawScope.drawHousing(geo: SymbolGeometry, s: SymbolStyle) {
    val tW = geo.bodyWidth * 0.32f
    val tH = geo.bodyHeight * 0.18f
    drawPath(Path().apply {
        moveTo(geo.cx - (tW / 2f), geo.bodyTop)
        lineTo(geo.cx - (tW * 0.36f), geo.bodyTop - tH)
        lineTo(geo.cx + (tW * 0.36f), geo.bodyTop - tH)
        lineTo(geo.cx + (tW / 2f), geo.bodyTop)
    }, s.mainColor, style = Stroke(s.strokeWidth, cap = StrokeCap.Round))
    drawRoundRect(
        color = s.mainColor,
        topLeft = Offset(
            geo.cx + (geo.bodyWidth * 0.24f),
            geo.bodyTop - (3.5.dp.toPx() + 1.dp.toPx())
        ),
        size = Size(geo.bodyWidth * 0.14f, 3.5.dp.toPx()),
        cornerRadius = CornerRadius(2.dp.toPx())
    )
    val r = CornerRadius(12.dp.toPx())
    drawRoundRect(
        color = s.mainColor,
        topLeft = Offset(geo.bodyLeft, geo.bodyTop),
        size = Size(geo.bodyWidth, geo.bodyHeight),
        cornerRadius = r,
        style = Stroke(s.strokeWidth)
    )
    drawRoundRect(s.subtleColor, Offset(geo.bodyLeft, geo.bodyTop), Size(geo.bodyWidth, geo.bodyHeight), r)
    drawRoundRect(s.mutedColor, Offset(geo.bodyLeft + (geo.bodyWidth * 0.12f),
        geo.bodyTop + (geo.bodyHeight * 0.18f)), Size(geo.bodyWidth * 0.1f, geo.bodyHeight * 0.12f),
        CornerRadius(3.dp.toPx()), Stroke(s.thinStroke))
}

private fun DrawScope.drawOptics(
    geo: SymbolGeometry,
    anim: CameraSymbolAnimationState,
    s: SymbolStyle,
) {
    val lCX = geo.cx
    val lCY = geo.bodyCenterY + (geo.bodyHeight * 0.02f)
    val oLR = geo.bodyHeight * 0.4f
    val iLR = oLR * 0.72f
    drawCircle(s.mainColor, oLR, Offset(lCX, lCY), style = Stroke(s.strokeWidth))
    drawCircle(s.mutedColor, iLR, Offset(lCX, lCY), style = Stroke(s.thinStroke))
    val cp = anim.captureProgress.coerceIn(0f, 1f)
    val closure = if (cp <= 0f) 0f else when {
        cp <= 0.4f -> cp / 0.4f
        cp <= 0.7f -> 1f
        else -> 1f - ((cp - 0.7f) / 0.3f)
    }
    val apR = iLR * (0.85f - (0.35f * closure))
    val rot = (30f * closure) * (PI_F / 180f)
    for (i in 0 until 6) {
        val angle = (2 * PI_F / 6 * i) + rot
        val start = Offset(
            x = lCX + (iLR * cos(angle.toDouble()).toFloat()),
            y = lCY + (iLR * sin(angle.toDouble()).toFloat()),
        )
        val end = Offset(
            x = lCX + (apR * cos((angle + 0.65f).toDouble()).toFloat()),
            y = lCY + (apR * sin((angle + 0.65f).toDouble()).toFloat()),
        )
        drawLine(
            color = s.mutedColor.copy(s.alpha * (0.4f + 0.45f * closure)),
            start = start,
            end = end,
            strokeWidth = s.thinStroke,
            cap = StrokeCap.Round,
        )
    }
    val fds = anim.focusDotScale.coerceIn(0f, 2f)
    if (fds > 0f) {
        val dotR = (iLR * 0.55f) * 0.38f * fds
        drawCircle(s.colors.accent.copy(s.alpha * 0.28f), dotR * 1.8f, Offset(lCX, lCY))
        drawCircle(s.colors.accent.copy(s.alpha), dotR, Offset(lCX, lCY))
    }
}
