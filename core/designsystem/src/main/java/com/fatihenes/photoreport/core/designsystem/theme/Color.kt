package com.fatihenes.photoreport.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * FotoRapor — "Ink & Brass" Kurumsal Renk Sistemi
 *
 * ── Tasarım Felsefesi ──────────────────────────────────────────────
 * Bu palet jenerik bir "SaaS mavisi" değil; saha mühendisliği ve teknik
 * denetim dünyasından ilham alır: harita/plan mürekkebi kadar derin ve
 * güvenilir bir Indigo ana renk, hassas ölçüm aletlerini çağrıştıran
 * sıcak bir Brass (pirinç/bakır) imza vurgusu, ve kağıt/karbon dokulu
 * sıcak nötrler (soğuk Tailwind slate DEĞİL — hafif sıcak, "kağıt" tonlu
 * bir gri skalası).
 *
 * Rol ayrımı bilinçli:
 *  - Primary  → Ink Indigo   (güven, otorite, ana aksiyonlar)
 *  - Tertiary → Brass        (imza vurgu — rozet, öne çıkan istatistik,
 *                              aktif nokta; markanın "parmak izi")
 *  - Success/Warning/Error   → Indigo ve Brass'tan kasıtlı olarak uzak
 *                              tutulan, karışmayan bağımsız tonlar
 *
 * Light modda kağıt-sıcaklığında yüzeyler + derin indigo aksanlar;
 * Dark modda sıcak karbon-siyahı yüzeyler + parlak periwinkle aksanlar.
 */

// ════════════════════════════════════════════════════════════════════
// RAW PALETTE — ham renk skalaları (bileşenlerde/logoda doğrudan da
// kullanılabilir; aşağıdaki semantik roller bunlardan türetilir)
// ════════════════════════════════════════════════════════════════════

// ─── Graphite — sıcak nötr skala (kağıt beyazından karbon siyahına) ──
val Graphite025 = Color(0xFFFAF9F6)
val Graphite050 = Color(0xFFF5F3EE)
val Graphite100 = Color(0xFFEDEAE3)
val Graphite200 = Color(0xFFDDD9CF)
val Graphite300 = Color(0xFFC4BFB2)
val Graphite400 = Color(0xFF9C9585)
val Graphite500 = Color(0xFF766F60)
val Graphite600 = Color(0xFF5A5448)
val Graphite700 = Color(0xFF423D34)
val Graphite800 = Color(0xFF2C2822)
val Graphite850 = Color(0xFF211D17)
val Graphite900 = Color(0xFF1C1915)
val Graphite950 = Color(0xFF121009)

// ─── Ink Indigo — marka ana rengi ────────────────────────────────────
val Indigo050 = Color(0xFFEDEFF9)
val Indigo100 = Color(0xFFD3D8F0)
val Indigo200 = Color(0xFFA8B0E0)
val Indigo300 = Color(0xFF8B95D8)
val Indigo400 = Color(0xFF5762B0)
val Indigo500 = Color(0xFF3D4791)
val Indigo600 = Color(0xFF2F386F)
val Indigo700 = Color(0xFF262C58)
val Indigo800 = Color(0xFF1D2242)
val Indigo900 = Color(0xFF15182F)

// ─── Brass — imza vurgu rengi ─────────────────────────────────────────
val Brass050 = Color(0xFFFAF1E4)
val Brass100 = Color(0xFFF0DBB6)
val Brass200 = Color(0xFFE2BE82)
val Brass300 = Color(0xFFD2A25C)
val Brass400 = Color(0xFFBE8843)
val Brass500 = Color(0xFFA6712F)
val Brass600 = Color(0xFF895C26)
val Brass700 = Color(0xFF6D481E)
val Brass800 = Color(0xFF503511)
val Brass900 = Color(0xFF3A2610)

// ─── Proje etiket paleti için ek tonlar (bkz. FotoRaporTokens.ProjectColors) ─
val PinePrimary = Color(0xFF2F6D4C)
val TerracottaPrimary = Color(0xFFB5502E)
val WinePrimary = Color(0xFF7C3048)
val PetrolPrimary = Color(0xFF1F6E72)
val PlumPrimary = Color(0xFF6B4C7A)
val OlivePrimary = Color(0xFF6B6B2E)

// ════════════════════════════════════════════════════════════════════
// SEMANTİK ROLLER — Material 3 renk şeması buradan beslenir
// ════════════════════════════════════════════════════════════════════

// ─── Primary (Ink Indigo) ─────────────────────────────────────────
val PrimaryLight            = Indigo600   // güneşte de okunaklı, derin ve otoriter
val OnPrimaryLight          = Color(0xFFFFFFFF)
val PrimaryContainerLight   = Indigo100
val OnPrimaryContainerLight = Indigo900

val PrimaryDark             = Indigo300   // OLED'de AAA kontrastlı periwinkle
val OnPrimaryDark           = Indigo900
val PrimaryContainerDark    = Indigo700
val OnPrimaryContainerDark  = Indigo100

// ─── Secondary (nötr destek — ikincil buton/chip) ─────────────────
val SecondaryLight            = Graphite600
val OnSecondaryLight          = Color(0xFFFFFFFF)
val SecondaryContainerLight   = Graphite100
val OnSecondaryContainerLight = Graphite800

val SecondaryDark            = Graphite300
val OnSecondaryDark          = Graphite900
val SecondaryContainerDark   = Graphite800
val OnSecondaryContainerDark = Graphite100

// ─── Tertiary (Brass — markanın imza vurgusu) ─────────────────────
val TertiaryLight            = Brass500
val OnTertiaryLight          = Color(0xFFFFFFFF)
val TertiaryContainerLight   = Brass100
val OnTertiaryContainerLight = Brass800

val TertiaryDark             = Brass300
val OnTertiaryDark           = Brass900
val TertiaryContainerDark    = Brass700
val OnTertiaryContainerDark  = Brass100

// ─── Backgrounds ────────────────────────────────────────────────────
val BackgroundLight   = Graphite025   // sıcak "kağıt" beyazı — soğuk slate değil
val OnBackgroundLight = Graphite900

val BackgroundDark    = Graphite950   // sıcak karbon-siyahı (true-black değil)
val OnBackgroundDark  = Graphite050

// ─── Surfaces (çok-katmanlı yüzey hiyerarşisi) ──────────────────────
val SurfaceLight   = Color(0xFFFFFFFF)
val OnSurfaceLight = Graphite900

val SurfaceDark    = Graphite900
val OnSurfaceDark  = Graphite050

// Surface Variants
val SurfaceVariantLight   = Graphite050
val OnSurfaceVariantLight = Graphite600

val SurfaceVariantDark    = Graphite800
val OnSurfaceVariantDark  = Graphite300

// Surface Containers (M3 tonal elevation yerine manuel kontrol)
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight    = Graphite025
val SurfaceContainerLight       = Graphite100
val SurfaceContainerHighLight   = Graphite200

val SurfaceContainerLowestDark  = Graphite950
val SurfaceContainerLowDark     = Graphite900
val SurfaceContainerDark_       = Graphite850
val SurfaceContainerHighDark    = Graphite700

// ─── Outline & Border ────────────────────────────────────────────────
val OutlineLight        = Graphite300
val OutlineVariantLight = Graphite200

val OutlineDark         = Graphite600
val OutlineVariantDark  = Graphite700

// ─── Semantic / Feedback (Indigo ve Brass'tan kasıtlı olarak ayrık) ──
val ErrorLight            = Color(0xFFB23327)   // sıcak, "pas" tonlu kırmızı
val OnErrorLight          = Color(0xFFFFFFFF)
val ErrorContainerLight   = Color(0xFFF6DAD5)
val OnErrorContainerLight = Color(0xFF5C160E)

val ErrorDark             = Color(0xFFE2887C)
val OnErrorDark           = Color(0xFF3D0E09)
val ErrorContainerDark    = Color(0xFF5C160E)
val OnErrorContainerDark  = Color(0xFFF6DAD5)

val SuccessColor          = Color(0xFF1D7A4C)   // zümrüt — Brass'tan (altın) net ayrışır
val SuccessColorDark      = Color(0xFF5FC98B)
val SuccessContainerLight = Color(0xFFD8F0E1)
val SuccessContainerDark  = Color(0xFF163F2A)

val WarningColor          = Color(0xFFB54A23)   // terrakota — Brass'tan (altın) ve Error'dan (kırmızı) ayrışır
val WarningColorDark      = Color(0xFFE38A5C)
val WarningContainerLight = Color(0xFFF6E1D2)
val WarningContainerDark  = Color(0xFF5C2E12)

// ─── Ek: İnverse (SnackBar vb.) ──────────────────────────────────────
val InverseSurfaceLight   = Graphite800
val InverseOnSurfaceLight = Graphite050
val InversePrimaryLight   = Indigo300

val InverseSurfaceDark    = Graphite100
val InverseOnSurfaceDark  = Graphite800
val InversePrimaryDark    = Indigo600
