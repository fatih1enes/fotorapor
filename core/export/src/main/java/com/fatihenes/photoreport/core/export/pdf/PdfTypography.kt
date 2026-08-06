package com.fatihenes.photoreport.core.export.pdf

import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint

class PdfTypography {

    val titlePaint = TextPaint().apply {
        color = PdfTheme.COLOR_PRIMARY_NAVY
        textSize = 18f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

    val subtitlePaint = TextPaint().apply {
        color = PdfTheme.COLOR_ACCENT
        textSize = 10f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
        letterSpacing = 0.05f
    }

    val metaHeaderPaint = TextPaint().apply {
        color = PdfTheme.COLOR_TEXT_SECONDARY
        textSize = 9.5f
        typeface = Typeface.DEFAULT
        isAntiAlias = true
    }

    val dateSectionPaint = TextPaint().apply {
        color = PdfTheme.COLOR_PRIMARY_NAVY
        textSize = 13f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

    val bodyPaint = TextPaint().apply {
        color = PdfTheme.COLOR_TEXT_PRIMARY
        textSize = 10.5f
        typeface = Typeface.DEFAULT
        isAntiAlias = true
    }

    val captionPaint = TextPaint().apply {
        color = PdfTheme.COLOR_TEXT_SECONDARY
        textSize = 9f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        isAntiAlias = true
    }

    val pageNumPaint = TextPaint().apply {
        color = PdfTheme.COLOR_TEXT_MUTED
        textSize = 9f
        typeface = Typeface.DEFAULT
        isAntiAlias = true
    }

    val signOffHeaderPaint = TextPaint().apply {
        color = PdfTheme.COLOR_PRIMARY_NAVY
        textSize = 9.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

    val signOffSubPaint = TextPaint().apply {
        color = PdfTheme.COLOR_TEXT_MUTED
        textSize = 8.5f
        typeface = Typeface.DEFAULT
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
