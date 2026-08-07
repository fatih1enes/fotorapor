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

    // FotoRapor "Ink & Brass" kurumsal kimliği — müşteriye giden rapor da
    // uygulamayla aynı marka dilini taşır.
    val COLOR_PRIMARY_NAVY = Color.rgb(0x1D, 0x22, 0x42)   // Ink Indigo 800
    val COLOR_ACCENT = Color.rgb(0xA6, 0x71, 0x2F)          // Brass 500 — imza vurgu
    val COLOR_TEXT_PRIMARY = Color.rgb(0x1C, 0x19, 0x15)    // Graphite 900
    val COLOR_TEXT_SECONDARY = Color.rgb(0x5A, 0x54, 0x48)  // Graphite 600
    val COLOR_TEXT_MUTED = Color.rgb(0x9C, 0x95, 0x85)      // Graphite 400
    val COLOR_BORDER = Color.rgb(0xDD, 0xD9, 0xCF)          // Graphite 200
    val COLOR_SURFACE = Color.rgb(0xFA, 0xF9, 0xF6)         // Graphite 025 — sıcak kağıt
}
