package com.fatihenes.photoreport.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * FotoRapor — Kurumsal Renk Paleti
 *
 * Tasarım felsefesi: Şantiye ortamında güneş altında yüksek kontrast sağlarken
 * göz yormayan profesyonel tonlar. Tailwind Slate + doygun mavi aksanlı
 * endüstriyel-minimalist bir palettir.
 *
 * Light modda kağıt-beyaz yüzeyler + derin indigo aksanlar;
 * Dark modda karbon-siyah yüzeyler + canlı mavi aksanlar.
 */

// ─── Primary Accent ──────────────────────────────────────────────
val PrimaryLight         = Color(0xFF1D4ED8)   // Blue 700 — doygun, güçlü, güneşte okunaklı
val OnPrimaryLight       = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFDBEAFE)   // Blue 100
val OnPrimaryContainerLight = Color(0xFF172554) // Blue 950

val PrimaryDark          = Color(0xFF60A5FA)    // Blue 400 — OLED'de AAA kontrastlı
val OnPrimaryDark        = Color(0xFF0C1B3A)
val PrimaryContainerDark = Color(0xFF1E3A5F)
val OnPrimaryContainerDark = Color(0xFFDBEAFE)

// ─── Secondary ───────────────────────────────────────────────────
val SecondaryLight       = Color(0xFF475569)    // Slate 600
val OnSecondaryLight     = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFF1F5F9) // Slate 100
val OnSecondaryContainerLight = Color(0xFF1E293B)

val SecondaryDark        = Color(0xFF94A3B8)    // Slate 400
val OnSecondaryDark      = Color(0xFF0F172A)
val SecondaryContainerDark = Color(0xFF1E293B)
val OnSecondaryContainerDark = Color(0xFFE2E8F0)

// ─── Tertiary (Vurgu / Başarı / Tamamlama tonları) ───────────────
val TertiaryLight        = Color(0xFF0D9488)    // Teal 600
val OnTertiaryLight      = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFCCFBF1)  // Teal 100
val OnTertiaryContainerLight = Color(0xFF134E4A) // Teal 900

val TertiaryDark         = Color(0xFF2DD4BF)    // Teal 400
val OnTertiaryDark       = Color(0xFF042F2E)
val TertiaryContainerDark = Color(0xFF134E4A)
val OnTertiaryContainerDark = Color(0xFFCCFBF1)

// ─── Backgrounds ─────────────────────────────────────────────────
val BackgroundLight      = Color(0xFFF8FAFC)    // Slate 50
val OnBackgroundLight    = Color(0xFF0F172A)    // Slate 900

val BackgroundDark       = Color(0xFF020617)    // Slate 950 (OLED true-black)
val OnBackgroundDark     = Color(0xFFF1F5F9)    // Slate 100

// ─── Surfaces (çok-katmanlı yüzey hiyerarşisi) ──────────────────
val SurfaceLight         = Color(0xFFFFFFFF)
val OnSurfaceLight       = Color(0xFF0F172A)

val SurfaceDark          = Color(0xFF0F172A)    // Slate 900
val OnSurfaceDark        = Color(0xFFF1F5F9)

// Surface Variants
val SurfaceVariantLight  = Color(0xFFF1F5F9)    // Slate 100
val OnSurfaceVariantLight = Color(0xFF475569)   // Slate 600

val SurfaceVariantDark   = Color(0xFF1E293B)    // Slate 800
val OnSurfaceVariantDark = Color(0xFFCBD5E1)    // Slate 300

// Surface Containers (M3 tonal elevation yerine manuel kontrol)
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight    = Color(0xFFF8FAFC) // Slate 50
val SurfaceContainerLight       = Color(0xFFF1F5F9) // Slate 100
val SurfaceContainerHighLight   = Color(0xFFE2E8F0) // Slate 200

val SurfaceContainerLowestDark  = Color(0xFF020617) // Slate 950
val SurfaceContainerLowDark     = Color(0xFF0F172A) // Slate 900
val SurfaceContainerDark_       = Color(0xFF1E293B) // Slate 800
val SurfaceContainerHighDark    = Color(0xFF334155) // Slate 700

// ─── Outline & Border ────────────────────────────────────────────
val OutlineLight         = Color(0xFFCBD5E1)    // Slate 300
val OutlineVariantLight  = Color(0xFFE2E8F0)    // Slate 200

val OutlineDark          = Color(0xFF475569)    // Slate 600
val OutlineVariantDark   = Color(0xFF334155)    // Slate 700

// ─── Semantic / Feedback ─────────────────────────────────────────
val ErrorLight           = Color(0xFFDC2626)    // Red 600
val OnErrorLight         = Color(0xFFFFFFFF)
val ErrorContainerLight  = Color(0xFFFEE2E2)    // Red 100
val OnErrorContainerLight = Color(0xFF7F1D1D)   // Red 900

val ErrorDark            = Color(0xFFF87171)    // Red 400
val OnErrorDark          = Color(0xFF450A0A)
val ErrorContainerDark   = Color(0xFF7F1D1D)
val OnErrorContainerDark = Color(0xFFFECACA)

val SuccessColor         = Color(0xFF059669)    // Emerald 600
val SuccessContainerLight = Color(0xFFD1FAE5)   // Emerald 100

val WarningColor         = Color(0xFFD97706)    // Amber 600
val WarningContainerLight = Color(0xFFFEF3C7)   // Amber 100

// ─── Ek: İnverse (SnackBar vb.) ──────────────────────────────────
val InverseSurfaceLight  = Color(0xFF1E293B)
val InverseOnSurfaceLight = Color(0xFFF1F5F9)
val InversePrimaryLight  = Color(0xFF93C5FD)    // Blue 300

val InverseSurfaceDark   = Color(0xFFE2E8F0)
val InverseOnSurfaceDark = Color(0xFF1E293B)
val InversePrimaryDark   = Color(0xFF1D4ED8)
