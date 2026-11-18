# MiniBank Mobile

[![Android CI](https://github.com/KADAFERHIDUP/minibank-mobile/actions/workflows/android.yml/badge.svg)](https://github.com/KADAFERHIDUP/minibank-mobile/actions/workflows/android.yml)

Aplikasi Android mobile banking untuk [MiniBank API](https://github.com/KADAFERHIDUP/minibank). Nasabah bisa masuk atau buka rekening, cek saldo, transfer pakai PIN, melihat mutasi, dan membagikan nomor rekening untuk menerima transfer.

**Stack:** Kotlin · Jetpack Compose (Material 3) · Navigation Compose · ViewModel · Retrofit · OkHttp · kotlinx.serialization · Coroutines · JUnit + MockWebServer · GitHub Actions

## Fitur

| Layar | Isi |
|---|---|
| Masuk / Buka rekening | Validasi per kolom sama dengan aturan server, pesan galat per kolom dari respons `fields` API |
| Beranda | Kartu rekening dengan tombol sembunyikan saldo dan salin nomor, aksi cepat (Transfer, Mutasi, Terima), 5 transaksi terakhir, tarik untuk muat ulang |
| Transfer | Empat langkah: cek nama penerima → nominal → konfirmasi dengan PIN → struk |
| Mutasi | Filter 7 / 30 / 90 hari, ringkasan uang masuk dan keluar, dikelompokkan per hari |
| Akun | Profil, daftar rekening (maks. 3), buka rekening baru, keluar |

## Keputusan desain

| Masalah | Solusinya di aplikasi |
|---|---|
| Tombol kirim kepencet dua kali, atau koneksi putus lalu dikirim ulang | Satu niat transfer memakai satu **`Idempotency-Key`** (UUID dibuat saat masuk langkah konfirmasi). Kirim ulang memakai kunci yang sama, jadi saldo terpotong sekali. Kunci baru hanya dibuat kalau tujuan atau nominal diubah, atau transfer ditolak karena aturan (saldo, limit). Kalau server menjawab `Idempotent-Replayed: true`, struk memberi tahu bahwa transfer itu sudah tercatat. |
| Nominal uang meleset karena `Double` | Saldo dibaca sebagai **`BigDecimal`** lewat serializer khusus; nominal transfer dikirim sebagai rupiah bulat (`Long`). |
| Token tertinggal di HP | Token **hanya di memori**, tidak disimpan ke penyimpanan. Aplikasi ditutup = harus masuk lagi. |
| HP ditinggal dalam keadaan terbuka | Aplikasi di latar belakang lebih dari **5 menit** otomatis keluar. |
| Token kedaluwarsa | Setiap respons `401` langsung mengakhiri sesi dan kembali ke layar masuk dengan pesan yang jelas. Login salah (`INVALID_CREDENTIALS`) tidak dianggap sesi habis. |
| Data nasabah sebelumnya terbawa | ViewModel layar setelah masuk disimpan di wadah khusus per sesi yang dibuang saat keluar. |
| Saldo terlihat di tangkapan layar / daftar aplikasi terakhir | `FLAG_SECURE` di jendela aplikasi. |
| Salah ketik nomor rekening tujuan | Digit pemeriksa **Luhn** dicek di aplikasi (algoritma sama dengan generator di server), lalu nama penerima dicek ke server sebelum nominal diisi. |
| Lalu lintas tidak terenkripsi | `network_security_config` hanya mengizinkan HTTPS (kecuali `10.0.2.2` untuk server lokal di emulator). Backup data aplikasi dimatikan. |
| Server demo tidur | Timeout baca 90 detik, dan layar masuk menampilkan pesan "membangunkan server" kalau respons lebih dari 6 detik. |

## Struktur

```
app/src/main/java/io/github/kadaferhidup/minibank/mobile
├── data/            API Retrofit, DTO, galat RFC 7807, sesi + interceptor, format rupiah, validasi
├── ui/auth/         masuk dan buka rekening
├── ui/home/         beranda dan data rekening bersama (BankViewModel)
├── ui/transfer/     alur transfer 4 langkah
├── ui/statement/    mutasi
├── ui/account/      profil, rekening, terima transfer
├── ui/components/   tombol, kolom isian, kartu, teks nominal
└── ui/theme/        warna dan tipografi, sama dengan versi web MiniBank
```

## Menjalankan

1. Buka folder proyek di **Android Studio** (Ladybug atau lebih baru), tunggu Gradle sync.
2. Jalankan ke emulator atau HP (Android 8.0 / API 26 ke atas).

Secara bawaan aplikasi memakai server demo `https://minibank-465d7.containers.snapdeploy.app/`. Server ini tidur setelah 15 menit tanpa pengunjung, jadi permintaan pertama bisa butuh sekitar satu menit.

Untuk memakai backend yang jalan di laptop (`docker compose up` di repo MiniBank) dari emulator:

```bash
./gradlew installDebug -PapiBaseUrl=http://10.0.2.2:8080/
```

APK siap pasang juga tersedia di setiap build GitHub Actions (tab **Actions** → build terbaru → artifact `minibank-debug-apk`).

## Testing

```bash
./gradlew testDebugUnitTest
```

- **Format dan validasi**: rupiah gaya Indonesia, isian nominal, Luhn sama dengan generator server, batas transfer, aturan daftar, daftar PIN lemah sama dengan server.
- **Lapisan data dengan MockWebServer**: token Bearer terpasang setelah masuk, galat `problem+json` beserta `fields`, `401` mengakhiri sesi, header `Idempotency-Key` terkirim dan `Idempotent-Replayed` terbaca, galat bisnis tidak mengeluarkan pengguna, login salah tidak dianggap sesi habis.

---

Dibuat oleh **Ahmad Umraithi** · Teknik Informatika, Universitas Malikussaleh
