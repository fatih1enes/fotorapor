package com.fatihenes.photoreport.ui.theme

import androidx.compose.ui.unit.dp

/**
 * FotoRapor Kurumsal Tasarım Sistemi - Boşluk (Spacing) ve Dokunma Alanı Standartları
 * Tüm ekranlardaki kenar boşlukları ve eldiven uyullu touch target boyutlarını standartlaştırır.
 */
object Spacing {
    // Küçük boşluklar (İkon ve metin aralığı)
    val ExtraSmall = 4.dp
    val Small = 8.dp
    
    // Orta ve standart boşluklar (Kart içi padding, satır aralıkları)
    val Medium = 16.dp
    val Large = 24.dp
    
    // Ekran geneli kenar boşlukları
    val ExtraLarge = 32.dp
    val Huge = 48.dp

    // Eldivenle Saha Kullanımı Standart Dokunma Hedefi (Touch Target)
    val MinTouchTarget = 48.dp
    val ButtonHeight = 52.dp
    val CardElevation = 2.dp
}
