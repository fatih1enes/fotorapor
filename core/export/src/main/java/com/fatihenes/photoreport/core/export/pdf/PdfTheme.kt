package com.fatihenes.photoreport.core.export.pdf

import android.graphics.Color

object PdfTheme {
    const val PAGE_WIDTH = 595
    const val PAGE_HEIGHT = 842
    
    const val MARGIN = 40f
    const val FOOTER_HEIGHT = 45f
    const val SIGN_OFF_HEIGHT = 100f
    
    const val COLUMNS = 2
    const val IMAGE_WIDTH = 240f
    const val IMAGE_HEIGHT = 265f
    const val GRID_SPACING = 15f
    const val FRAME_BORDER_WIDTH = 1.2f

    const val ASPECT_RATIO_LANDSCAPE_THRESHOLD = 1.22f
    const val FULL_WIDTH_IMAGE_WIDTH = PAGE_WIDTH - (MARGIN * 2)
    const val MAX_LANDSCAPE_IMAGE_HEIGHT = 380f

    val COLOR_PRIMARY_NAVY = Color.rgb(24, 43, 73)
    val COLOR_ACCENT = Color.rgb(37, 99, 235)
    val COLOR_TEXT_PRIMARY = Color.rgb(15, 23, 42)
    val COLOR_TEXT_SECONDARY = Color.rgb(100, 116, 139)
    val COLOR_TEXT_MUTED = Color.rgb(148, 163, 184)
    val COLOR_BORDER = Color.rgb(203, 213, 225)
    val COLOR_SURFACE = Color.rgb(248, 250, 252)
}
