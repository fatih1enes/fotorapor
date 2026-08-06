package com.fatihenes.photoreport.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * FotoRapor Kurumsal Renk Paleti - MD3 & Yüksek Kontrastlı Saha Ergonomisi
 * Şantiye ortamında güneş altında görünürlüğü artıran ve göz yormayan resmi kurumsal tonlar.
 */

// Primary Accent (Kurumsal Derin İndigo / Mavi)
val PrimaryLight = Color(0xFF2563EB) // Blue 600 (Güneş altında en yüksek kontrast)
val PrimaryContainerLight = Color(0xFFDBEAFE)
val OnPrimaryContainerLight = Color(0xFF1E3A8A)

val PrimaryDark = Color(0xFF60A5FA) // Blue 400 (OLED ekranlarda parlamadan AAA yüksek kontrast)
val PrimaryContainerDark = Color(0xFF1E293B)
val OnPrimaryContainerDark = Color(0xFFE2E8F0)

// Secondary & Auxiliary
val SecondaryLight = Color(0xFF475569) // Slate 600
val SecondaryDark = Color(0xFF94A3B8) // Slate 400

// Backgrounds - Göz Yormayan Kurumsal Slate Dokusu
val BackgroundLight = Color(0xFFF8FAFC) // Slate 50
val BackgroundDark = Color(0xFF020617) // Slate 950 (Deep OLED Black)

// Surface & Surfaces
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceDark = Color(0xFF0F172A) // Slate 900

val SurfaceVariantLight = Color(0xFFF1F5F9) // Slate 100
val SurfaceVariantDark = Color(0xFF1E293B) // Slate 800

// Semantic Feedback Colors
val Error = Color(0xFFEF4444) // Red 500
val ErrorContainer = Color(0xFFFEE2E2)
val Success = Color(0xFF10B981) // Emerald 500

// Text & Border Contrast Hierarchy
val OnBackgroundLight = Color(0xFF0F172A) // Slate 900
val OnBackgroundSecondaryLight = Color(0xFF334155) // Slate 700

val OnBackgroundDark = Color(0xFFF8FAFC)
val OnBackgroundSecondaryDark = Color(0xFFCBD5E1)

val BorderLight = Color(0xFFE2E8F0) // Slate 200
val BorderDark = Color(0xFF334155) // Slate 700
