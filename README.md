# Şantiye Günlüğü - Proje Fotoğraf ve Kayıt Yönetimi

Şantiye Günlüğü, şantiye sahasında çalışan mühendis ve teknisyenlerin proje fotoğraflarını ve günlük kayıtlarını verimli bir şekilde yönetmeleri için tasarlanmış bir Android uygulamasıdır. Fotoğrafları projeye ve tarihe göre düzenlemenize, notlar eklemenize ve rapor olarak dışa aktarmanıza olanak tanır.

## 🚀 Özellikler

- **Proje Yönetimi**: Birden fazla projeyi kolayca oluşturun ve yönetin.
- **Günlük Kayıtlar**: Fotoğrafları ve notları tarihe göre gruplandırın.
- **Gelişmiş Kamera**: 
    - Donanım bazlı gerçek zamanlı optimizasyon (HDR/Gece modu).
    - Yüksek kaliteli **AVIF** ve WebP desteği ile depolama tasarrufu.
    - CameraX entegrasyonu ile stabil çekim deneyimi.
- **Widget Desteği**: Ana ekrandan hızlıca kamera veya galeriye erişim.
- **Raporlama**: Projeleri PDF (görsel döküm) veya HTML/ZIP (web arşivi) olarak dışa aktarın.
- **Karanlık/Aydınlık Tema**: Modern Material 3 tasarımı ve dinamik renk desteği.
- **Çöp Kutusu**: Yanlışlıkla silinen kayıtları 30 gün içinde geri yükleme imkanı.
- **Performans**: Baseline Profiles ile %30'a varan daha hızlı uygulama başlatma ve akıcı UI.

## 🛠 Teknolojiler

- **UI**: Jetpack Compose (Material 3)
- **Dil**: Kotlin (Coroutines & Flow)
- **Mimari**: MVVM & Clean Architecture
- **Dependency Injection**: Hilt
- **Veritabanı**: Room (KSP ile)
- **Görüntü İşleme**: Coil 3 (Video & AVIF desteği)
- **Medya**: CameraX & Media3 (ExoPlayer)
- **Arka Plan Görevleri**: WorkManager
- **Performans**: Android Baseline Profiles
- **Yerel Depolama**: DataStore Preferences

## 🏗 Proje Yapısı

Proje iki ana modülden oluşmaktadır:
- `:app`: Uygulamanın tüm iş mantığı ve UI katmanını içeren ana modül.
- `:baselineprofile`: Uygulama performansını optimize etmek için kullanılan Baseline Profile ve Benchmark testlerini içeren modül.

## 🏁 Başlangıç

### Gereksinimler
- Android Studio Ladybug veya daha yeni bir sürüm.
- JDK 17.
- Android SDK 37 (Compile SDK).

### Kurulum
1. Depoyu klonlayın: `git clone https://github.com/kullanici/santiye-gunlugu.git`
2. Android Studio'da projeyi açın.
3. Gradle senkronizasyonunun tamamlanmasını bekleyin.
4. Derleyin ve çalıştırın.

## 📄 Lisans

Bu proje MIT Lisansı ile lisanslanmıştır. Daha fazla bilgi için `LICENSE` dosyasına göz atın.
