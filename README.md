Apartman Su Faturası Yönetim Sistemi (Android & Web)
Apartman ve site yöneticilerinin su sayaçlarını yerinde okuyup faturaya dönüştürmelerini sağlayan, Kotlin ile geliştirilmiş bir mobil uygulama ve PHP tabanlı bir yönetim panelinden oluşan hibrit bir sistemdir.

📱 Uygulama Akışı
Kullanıcı Kaydı: Yöneticiler uygulama üzerinden hesap oluşturur ve apartman bilgilerini tanımlar.

Yönetici Onayı: Güvenlik gereği yeni kayıtlar süper yönetici onayından sonra aktifleşir.

Sayaç Okuma: Yerinde yapılan sayaç okumaları doğrudan mobil uygulama arayüzü ile sisteme işlenir.

Otomatik Faturalandırma: Girilen endekslere göre borçlar anında hesaplanır ve PDF/Görsel olarak hazır hale getirilir.

✨ Öne Çıkan Özellikler
Kotlin Mobil Arayüz: Sahada kolay kullanım için optimize edilmiş Android uygulaması.

Kademeli Fiyatlandırma: Manuel veya otomatik olarak 1., 2. ve 3. kademe su tüketim bedeli tanımlama.

WhatsApp Bildirimi: Oluşturulan faturaları tek tuşla sakinin telefonuna iletme.

İki Aşamalı Doğrulama (2FA): E-posta tabanlı 6 haneli doğrulama kodu ile üst düzey hesap güvenliği.

Android Yazdırma Desteği: Faturaların Bluetooth veya ağ yazıcıları üzerinden çıktı alınabilmesi için hazır fonksiyonlar.

🛠️ Teknik Özellikler
Mobil: Kotlin (APK sürümü).

Backend API: PHP (Uygulama ile JSON üzerinden haberleşir).

Veritabanı: MySQL (Bina, daire, tüketim ve kullanıcı verileri için).

Güvenlik: Oturum yönetimi, yetki tabanlı erişim (Admin/Yönetici) ve 2FA.
