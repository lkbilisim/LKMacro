# LDK Makro v1.1 (Android)

Üç sürüklenebilir dokunma noktası ve yüzen M kontrolüyle çalışan Android erişilebilirlik makrosu.

## İşlem sırası

1. Hedefe dokun
2. Saldır düğmesine dokun
3. Konuşlandır düğmesine dokun
4. **Girilen saniye aralığından rastgele bir değer** kadar bekle ve işlemi tekrarla

## Süreler kullanıcı tarafından ayarlanır

Uygulama ana ekranında sürelerin **minimum ve maksimumunu** kendin yazıp **Ayarları Kaydet** düğmesine dokunabilirsin:

- İşlemler arasındaki bekleme (saniye): Başlangıçta 45–90, **sabit sınır değil**; örneğin 25–45 geçerlidir.
- Dokunmalar arasında gecikme (milisaniye): Varsayılan 1100; pozitif değer değiştirilebilir.
- **Molanın kaç dakika arayla gerçekleşeceği** (min / max dakika): Varsayılan 4–8.
- **Ek molanın ne kadar süreceği** (min / max dakika): Varsayılan 1–5.

Mola **dokunma veya saldırı sayısına göre değil, geçen süreye göre** planlanır. Mola zamanı geldiyse mevcut üç adımlı işlem tamamlandıktan sonra normal rastgele beklemeye **ek** mola süresi katılır. Mola sonrası yeni zaman aralığı rastgele seçilir. Ayarları değiştirirken çalışan makroyu durdurup yeniden başlat.

Tüm süre alanlarına pozitif tam sayı gir; üst sınır alt sınırdan düşük olamaz. Aralıkları kodda 45–90 ile kilitleyen denetim kaldırıldı.

## APK indirme

**Actions → APK Olustur → başarılı (yeşil) son çalışma → Artifacts → LDK-Makro-APK**.

ZIP içindeki **app-debug.apk** Android cihazına yüklenebilir. Daha önceki APK ile farklı debug imza kullanılmış olabileceği için güncelleme yüklemesi başarısız olursa eski uygulamayı kaldırıp yeniden yüklemek gerekebilir (eski ayarlar silinir).

Kurulumdan sonra uygulamanın Erişilebilirlik Hizmeti'ni etkinleştir; Android 13'te uygulama kısıtlı ayarları engelleniyorsa Uygulama bilgisi > üç nokta > Kısıtlı ayarlara izin ver gerekebilir. Oyuna dön, M'ye bas, noktaları yerleştir ve kaydet, ardından Başlat.

**Sınırlama:** Sabit koordinatlar kullanılır; OCR veya ekrandan hedef tanıma yok. Oyun arayüzü değişirse yanlış dokunabilir. Rastgele süreler ban veya oyun politikaları açısından koruma garantisi sağlamaz.
