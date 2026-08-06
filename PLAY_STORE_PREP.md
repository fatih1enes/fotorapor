Play Store Yayına Hazırlık - PhotoReport

1) İmzalama (Release key)
- `keystore.properties` dosyasını proje köküne oluşturun ve `.gitignore` ekleyin.
  Örnek içeriği:
  ```properties
  storeFile=C:\path\to\your\keystore.jks
  storePassword=<store-password>
  keyAlias=<key-alias>
  keyPassword=<key-password>
  ```
- Sonra release AAB oluşturun:

```bash
./gradlew bundleRelease
``` 

2) Sürüm numaraları
- `app/build.gradle.kts` içindeki `versionCode` ve `versionName` bilgisini güncelleyin (sürüm yükseltme gerektiriyse).

3) Gizlilik Politikası ve İletişim
- `PRIVACY_POLICY.md` dosyasındaki e-posta adresini gerçek bir destek e-postası ile değiştirin.
- Play Console'da "Privacy Policy" alanına uygulamanın URL'sini ekleyin.

4) İzinler ve Play Console beyanları
- Uygulama konum, kamera, medya erişimi gibi hassas izinler istiyor. Play Console'da bu izinlerin kullanım amacını ve gizlilik açıklamalarını girin.
- `ACCESS_FINE_LOCATION` gibi hassas izinler için gerekliyse ek kullanım açıklamalarını sağlayın.

5) ProGuard ve Küçültme
- `app/proguard-rules.pro` dosyası proje için hazır görünüyor. Minify ve shrinkResources `release` için etkin.

6) Test
- Oluşturulan `.aab` dosyasını Play Console içindeki iç test (internal testing) kanalına yükleyip test edin.

7) SSS
- Eğer release imzası yoksa derleme artık `assembleRelease`/`bundleRelease` görevleri `keystore.properties` yoksa hatayla duracak (proje `app/build.gradle.kts` içinde doğrulama eklendi).

İsterseniz şimdi `PLAY_STORE_PREP.md`'yi daha da genişleteyim veya `bundleRelease` oluşturup hata/çıktı kontrolü yapayım (yerel keystore gereklidir).