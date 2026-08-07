package com.fatihenes.photoreport.core.designsystem.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.fatihenes.photoreport.core.designsystem.R

/**
 * FotoRapor Tipografi — Font Ailesi Tanımları
 *
 * İki fontla kasıtlı bir eşleştirme:
 *  - Space Grotesk (Display/Headline) → teknik, geometrik karakterli;
 *    büyük rakamlar ve başlıklarda markaya kimlik kazandırır.
 *  - Manrope (Body/Title/Label)       → sıcak, okunaklı, modern gövde
 *    yazısı; uzun form raporlarda ve arayüzde göz yormaz.
 *
 * Her iki font da değişken (variable) font olarak APK içine gömülüdür
 * (res/font). Bu, çevrimdışı-öncelikli (offline-first) bir saha
 * uygulaması için kritik: harici bir font sağlayıcısına (ör. Google
 * Play Services üzerinden çalışma zamanında indirilen fontlar) bağımlı
 * kalmadan, internet olmayan şantiye/saha koşullarında bile tutarlı
 * marka tipografisi garanti edilir.
 *
 * Lisans: SIL Open Font License 1.1 — bkz. /licenses/fonts
 */

@OptIn(ExperimentalTextApi::class)
private fun variableFont(resId: Int, weight: FontWeight): Font = Font(
    resId = resId,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

val DisplayFontFamily = FontFamily(
    variableFont(R.font.space_grotesk_variable, FontWeight.Medium),
    variableFont(R.font.space_grotesk_variable, FontWeight.SemiBold),
    variableFont(R.font.space_grotesk_variable, FontWeight.Bold),
)

val BodyFontFamily = FontFamily(
    variableFont(R.font.manrope_variable, FontWeight.Light),
    variableFont(R.font.manrope_variable, FontWeight.Normal),
    variableFont(R.font.manrope_variable, FontWeight.Medium),
    variableFont(R.font.manrope_variable, FontWeight.SemiBold),
    variableFont(R.font.manrope_variable, FontWeight.Bold),
    variableFont(R.font.manrope_variable, FontWeight.ExtraBold),
)
