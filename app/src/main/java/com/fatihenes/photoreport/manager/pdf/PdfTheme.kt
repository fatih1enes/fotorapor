package com.fatihenes.photoreport.manager.pdf

import android.graphics.Color

/**
 * FotoRapor Kurumsal PDF Tasarım Sistemi - Tema ve Boyut Sabitleri
 * PDF tasarımını iş mantığından ayrıştıran merkezi tema modülü.
 */
object PdfTheme {
    // Sayfa Boyutları (A4 - 72 DPI)
    const val PAGE_WIDTH = 595
    const val PAGE_HEIGHT = 842
    
    // Kenar ve Boşluk Standartları
    const val MARGIN = 40f
    const val INNER_PADDING = 12f
    const val HEADER_HEIGHT = 80f
    const val FOOTER_HEIGHT = 45f
    const val SIGN_OFF_HEIGHT = 100f
    
    // Fotoğraf Izgarası (Adaptive Column Layout)
    const val COLUMNS = 2
    const val IMAGE_WIDTH = 240f
    const val IMAGE_HEIGHT = 265f
    const val GRID_SPACING = 15f
    const val FRAME_BORDER_WIDTH = 1.2f

    // Adaptive Layout Thresholds (Configurable)
    const val ASPECT_RATIO_LANDSCAPE_THRESHOLD = 1.22f // Higher than this is treated as 1-column
    const val FULL_WIDTH_IMAGE_WIDTH = PAGE_WIDTH - (MARGIN * 2)
    const val MAX_LANDSCAPE_IMAGE_HEIGHT = 380f // Max vertical space for a single landscape image

    // Kurumsal Renk Paleti (Baskı ve Resmi Kurum Uyurlu)
    val COLOR_PRIMARY_NAVY = Color.rgb(24, 43, 73)      // Endüstriyel Derin Laci
    val COLOR_ACCENT = Color.rgb(37, 99, 235)           // Kurumsal Mavi
    val COLOR_TEXT_PRIMARY = Color.rgb(15, 23, 42)      // OLED Koyu Siyah/Slate
    val COLOR_TEXT_SECONDARY = Color.rgb(100, 116, 139) // Slate Gri
    val COLOR_TEXT_MUTED = Color.rgb(148, 163, 184)     // Açık Gri (Tarih ve Sayfa No)
    val COLOR_BORDER = Color.rgb(203, 213, 225)         // Ayırıcı ve Çerceve Gri
    val COLOR_SURFACE = Color.rgb(248, 250, 252)        // Zemin Tamponu (Card BG)
    val COLOR_WHITE = Color.WHITE
}
