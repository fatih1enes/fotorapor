package com.fatihenes.photoreport.core.export.pdf

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.fatihenes.photoreport.core.export.R

/**
 * FotoRapor PDF Tipografisi.
 *
 * Müşteriye giden rapor, uygulamanın kendisiyle aynı marka fontlarını
 * taşır: başlıklarda Space Grotesk, gövde metninde Manrope. Her iki
 * font da bu modülün içine gömülüdür (res/font) — çevrimdışı üretim
 * garantisi, harici bir font sağlayıcısına bağımlılık yok.
 *
 * Variable font ailesinden `Typeface.create(base, weight, italic)`
 * (API 29+) ile spesifik ağırlıklar türetilir.
 */
class PdfTypography(context: Context) {

    private val spaceGroteskBase: Typeface =
        ResourcesCompat.getFont(context, R.font.space_grotesk_variable) ?: Typeface.DEFAULT_BOLD

    private val manropeBase: Typeface =
        ResourcesCompat.getFont(context, R.font.manrope_variable) ?: Typeface.DEFAULT

    private fun display(weight: Int, italic: Boolean = false): Typeface =
        Typeface.create(spaceGroteskBase, weight, italic)

    private fun body(weight: Int, italic: Boolean = false): Typeface =
        Typeface.create(manropeBase, weight, italic)

    val titlePaint = TextPaint().apply {
        color = PdfTheme.COLOR_PRIMARY_NAVY
        textSize = 18f
        typeface = display(700)
        isAntiAlias = true
        letterSpacing = -0.01f
    }

    val subtitlePaint = TextPaint().apply {
        color = PdfTheme.COLOR_ACCENT
        textSize = 10f
        typeface = body(800)
        isAntiAlias = true
        letterSpacing = 0.08f
    }

    val metaHeaderPaint = TextPaint().apply {
        color = PdfTheme.COLOR_TEXT_SECONDARY
        textSize = 9.5f
        typeface = body(500)
        isAntiAlias = true
    }

    val dateSectionPaint = TextPaint().apply {
        color = PdfTheme.COLOR_PRIMARY_NAVY
        textSize = 13f
        typeface = display(600)
        isAntiAlias = true
    }

    val bodyPaint = TextPaint().apply {
        color = PdfTheme.COLOR_TEXT_PRIMARY
        textSize = 10.5f
        typeface = body(400)
        isAntiAlias = true
    }

    val captionPaint = TextPaint().apply {
        color = PdfTheme.COLOR_TEXT_SECONDARY
        textSize = 9f
        typeface = body(500, italic = true)
        isAntiAlias = true
    }

    val pageNumPaint = TextPaint().apply {
        color = PdfTheme.COLOR_TEXT_MUTED
        textSize = 9f
        typeface = body(500)
        isAntiAlias = true
    }

    val signOffHeaderPaint = TextPaint().apply {
        color = PdfTheme.COLOR_PRIMARY_NAVY
        textSize = 9.5f
        typeface = body(700)
        isAntiAlias = true
        letterSpacing = 0.04f
    }

    val signOffSubPaint = TextPaint().apply {
        color = PdfTheme.COLOR_TEXT_MUTED
        textSize = 8.5f
        typeface = body(500)
        isAntiAlias = true
    }

    val headerBannerPaint = Paint().apply {
        color = PdfTheme.COLOR_PRIMARY_NAVY
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    val dividerLinePaint = Paint().apply {
        color = PdfTheme.COLOR_BORDER
        strokeWidth = 1f
        isAntiAlias = true
    }

    val cardBackgroundPaint = Paint().apply {
        color = PdfTheme.COLOR_SURFACE
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    val frameBorderPaint = Paint().apply {
        color = PdfTheme.COLOR_BORDER
        strokeWidth = PdfTheme.FRAME_BORDER_WIDTH
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    val signLinePaint = Paint().apply {
        color = PdfTheme.COLOR_TEXT_SECONDARY
        strokeWidth = 1f
        style = Paint.Style.STROKE
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(5f, 3f), 0f)
        isAntiAlias = true
    }

    val bitmapPaint = Paint().apply {
        isFilterBitmap = true
        isAntiAlias = true
        isDither = true
    }
}
