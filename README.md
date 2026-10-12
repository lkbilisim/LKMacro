# LK Makro v1.3 — Android

Android Erişilebilirlik hizmetiyle, kullanıcının seçtiği sayıda noktaya sırayla dokunan, yüzen M kontrollü makro.

## Tuş sayısını belirle

Ana ekranda tuş sayısı sorulmaz; burası yalnızca süre ve mola ayarları içindir. Oyundayken yüzen **M** düğmesine dokun, **“Sayı girerek tuş seç”** seçeneğine bas ve açılan soruya **1–100** arasında istediğin tuş sayısını yaz. Başlangıç değeri 3'tür. Dilersen M içindeki **+ / −** ile birer birer de değiştirebilirsin.

**M → Noktaları yerleştir** ile seçilen sayı kadar numaralı ve sürüklenebilir nokta oluştur; istediğin butonların üzerine yerleştir. **M → Konumları kaydet** ile tamamla. Makro sırayla **1 → 2 → 3 → 4 → ... → N** tuşlarına basıp son dokunuştan sonra bir sonraki tur için rastgele bekleme seçer. Tuş sayısı değişince koordinatları yeniden kaydetmek gerekir. Mevcut ilk üç koordinat, sayı aynı kaldığı sürece korunur.

## Serbest zamanlama

- Son dokunuş sonrası rastgele bekleme: Başlangıç 45–90 saniye, ama **1 veya daha büyük istediğin aralık** seçilebilir.
- Adımlar arasındaki dokunma gecikmesi: Başlangıç 1100 milisaniye, ayarlanabilir.
- Zaman bazlı ek mola aralığı: Başlangıç 4–8 dakika, ayarlanabilir.
- Ek mola süresi: Başlangıç 1–5 dakika, ayarlanabilir.

Mola tıklama sayısına bağlı değildir. Sırası geldiğinde devam eden bütün dokunmalar tamamlanır, ardından normal beklemeye ek rastgele mola eklenir. Döngüyü durdurup tekrar başlatmak değiştirilen değerleri uygular.

## APK

GitHub **Actions → APK Olustur → yeşil son çalışma → Artifacts → LK-Makro-APK**. ZIP içindeki `app-debug.apk` kurulur.

Kurulumdan sonra Ayarlar → Erişilebilirlik → indirilen hizmetlerde **LK Makro - dokunma hizmeti** etkinleştirilir. Android 13 kısıtlı ayar engeli çıkarabilir.

Paket kimliği (`com.lodoskent.makro`) ve mevcut ayar saklama anahtarı yükseltmelerde korunur. GitHub Actions debug imzaları yeniden üretilirse önceki APK'yı kaldırmak gerekebilir (ayarlar silinir).

**Sınırlama:** Ekranı OCR ile okumaz; sabit koordinatlarla çalışır. Oyun arayüzü veya hedef değişirse yanlış dokunabilir. Rastgele zamanlama hesabın yaptırıma uğramayacağı anlamına gelmez.
