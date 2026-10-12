# LK Makro v2.1 — Premium UI

LK Makro, Android erişilebilirlik hizmetiyle, seçilen sayıdaki ekrana dokunma noktalarına sırayla basan ve bunları zamanlayan kullanıcı kontrollü uygulamadır.

## Premium tasarım
- Kullanıcının LK logosu: Android uygulama ikonu, karşılama ekranı ve yüzen M butonu.
- Koyu lacivert / neon mavi – turkuaz teknoloji teması, parıltılı geçişli düğmeler, cam görünümlü kartlar.
- **Ana Sayfa**: LK Makro logolu karşılama, sistem durumu ve Ayarlar'a geçiş.
- **Ayarlar**: rastgele bekleme anahtarı, en az/en çok saniye, dokunuş gecikmesi, mola aralığı ve mola süresi; artı/eksi kontrolleri ve ayarları kaydetme.
- **Hakkında**: özellikler ve kullanım sınırlamaları.
- **Yüzen M**: logolu sürüklenebilir buton, sayısal tuş seçimi, noktaları yerleştir, konumları kaydet, başlat/durdur; neon mavi numaralı noktalar.


## v2.1: Yüzen LK düğmesini geri getirme

- **X**, yalnızca açık kontrol panelini kapatır; LK yüzen düğmesini kapatmamalıdır.
- Daha önce kaydedilen düğme koordinatları, Android ekran sınırlarına göre düzeltilir.
- Hizmet açık olduğu hâlde Android yüzen düğmeyi kaldırırsa görünümün tekrar açılması denenir.
- Ana Sayfa ve Ayarlar'da **Yüzen LK Düğmesini Geri Getir** düğmesi bulunur; görünür LK simgesini ekranın sol tarafına taşır. Güvenlik için o sırada çalışan makro durdurulur.
- Hizmet bağlantısı kaybolursa kullanıcıya Erişilebilirlik ayarlarına gitme olanağı gösterilir.
- Uygulama, bağlanma veya yüzen pencere hatasını uygulama ayarlarında saklar.
- GitHub Actions artık debug APK'yı indirmeye sunmadan önce `apksigner verify` ile imza ve ZIP bütünlüğünü kontrol eder.

**Önemli:** GitHub Actions geliştirme (debug) imzası her yeni çalışma ortamında değişebilir; önceki sürüm üstüne kurulum reddedilirse eskiyi kaldırmak gerekebilir. İleride imzanın kalıcı olması için özel imzalama anahtarı yalnızca GitHub Actions Secrets içinde saklanmalıdır, herkese açık depoya yüklenmemelidir.

## Makro mantığı
1. M panelindeki **Sayı Girerek Tuş Seç** ile 1–100 arasında istediğin sayı belirlenir.
2. **Noktaları Yerleştir** ile numaralı daireleri istediğin yerlere sürükle ve **Konumları Kaydet**.
3. **Başlat** seçilince 1→2→...→N noktalarına sırayla dokunulur; son dokunuştan sonra ayarlardaki iki saniye değeri arasında yeni rastgele bekleme başlar.
4. Mola, tur sayısına göre değil, belirlediğin rastgele *dakika aralıklarına* göre planlanır ve mevcut tur tamamlanınca normal beklemeye eklenir.
5. M üzerinden durdurulabilir. Ayarlar değiştirilirse durdurup yeniden başlat.

**Sınırlar:** Basış ve mola zamanları pozitif tam sayı değerleridir; verilen aralıkta üst sınır alt sınırdan küçük olamaz. 45–90 saniye yalnızca ilk başlangıç değeridir ve değiştirilebilir. Rastgele bekleme kapatılırsa en az saniye değerinden sabit bekleme uygulanır. OCR yoktur ve ekrandan otomatik hedef tanınmaz.

## Android APK derleme
GitHub Actions → **APK Olustur** → yeşil başarılı derleme → Artifacts → **LK-Makro-APK**. Kaynak workflow dosyası `.github/workflows/build-apk.yml`, kullanıcı logosu kaynağı `branding/LK_ikon.webp.base64` altında saklanır ve build sırasında `drawable-nodpi` içine hazırlanır.

Depomuz public ise standart GitHub-hosted runner'larda GitHub Actions ücretsizdir; ücretli larger runner veya kota aşan özel depo kullanımı için ayrı faturalandırma kuralları bulunur. GitHub Free hesabında private depo için aylık 2.000 dakika, 500 MB artifact depolaması vardır (2026 resmî GitHub belgelerine göre).

Android cihazda kurulum için Erişilebilirlik Hizmeti izni gerekir. Android 13'te güvenilir, dışarıdan yüklenmiş uygulamalar için sistemin Kısıtlı Ayarlar izin kontrolü açılmak zorunda olabilir. GitHub Actions debug imzaları değişirse mevcut uygulamanın üzerine kurulum yapılamayabilir; uygulama kaldırılırsa kayıtlı noktalar ve ayarlar silinir.

**Güvenlik:** Uygulama sabit koordinatlara dokunur; oyun arayüzü değişirse yanlış giriş yapabilir. Rastgele zamanlama, oyun hizmet şartlarına uyma veya ban yememe garantisi sağlamaz. APK GitHub Actions'da derlenmiş ve paket içeriği doğrulanmıştır; gerçek cihaz üzerinde otomatik UI testi henüz yapılmamıştır.
