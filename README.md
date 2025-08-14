# Scanner SDK

## ScannerView

Sebuah komponen Jetpack Compose yang menyediakan antarmuka pemindai dokumen yang lengkap. Komponen
ini menangani pratinjau kamera, deteksi dokumen, pengambilan gambar, dan interaksi pengguna seperti
kontrol lampu kilat (flash).

Composable ini dirancang agar sangat mudah disesuaikan melalui parameter-parameternya, memungkinkan
penyesuaian tema dan integrasi ke dalam berbagai alur aplikasi.

### Fungsi ScannerView Keseluruhan

```kotlin
public fun ScannerView(
    applicationId: Int,
    modifier: Modifier,
    saveToGallery: Boolean,
    showDetectedBoundary: Boolean,
    isFullScreen: Boolean,
    onNavigateBack: (() -> Unit)?,
    scannerState: ScannerState,
    onStateChanged: (ScannerState) -> Unit,
    isPaused: Boolean,
    colors: ScannerViewColors,
    flashButton: @Composable ((() -> Unit, Boolean, Color) -> Unit)
): Unit
```

### Parameter

| Parameter              |          Tipe           | Deskripsi                                                                                                                                                                                                                                           |
|------------------------|:-----------------------:|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `applicationId`        |           Int           | ID aplikasi unik Anda yang disediakan untuk menggunakan SDK ini.                                                                                                                                                                                    |
| `modifier`             |        Modifier         | Karena UI sudah diatur di dalam SDK, gunakan `modifier` ini hanya untuk tujuan semantik. Kecuali Anda ingin mengatur tampilan penuh layar ke `false`.                                                                                               |
| `saveToGallery`        |         Boolean         | Jika `true`, gambar dokumen yang diambil akan disimpan secara otomatis ke galeri publik perangkat.                                                                                                                                                  |
| `showDetectedBoundary` |         Boolean         | Jika `true`, overlay akan digambar pada pratinjau kamera untuk memvisualisasikan batas-batas objek yang terdeteksi.                                                                                                                                 |
| `isFullScreen`         |         Boolean         | Menentukan mode tata letak. Atur ke `true` agar pemindai menempati seluruh layar, atau `false` untuk menyematkannya di dalam tata letak lain.                                                                                                       |
| `onNavigateBack`       |     (() -\> Unit)?      | Sebuah lambda callback opsional yang dipanggil saat ada aksi kembali (back) atau tutup (close) dari dalam UI pemindai. Ketika parameter ini diisi, sebuah tombol panah kembali akan muncul di toolbar.                                              |
| `scannerState`         |      ScannerState       | Mengontrol status pemindai saat ini. Anda dapat mengubah status seperti `ScannerState.CAPTURING` untuk memulai pemindaian.                                                                                                                          |
| `onStateChanged`       | (ScannerState) -\> Unit | Callback yang memberi tahu aplikasi parent terkait perubahan status pemindaian (misalnya, saat dokumen terdeteksi atau dalam proses verifikasi sticker). Ini penting jika ingin menampilkan tampilan tertentu pada saat proses verifikasi berjalan. |
| `isPaused`             |         Boolean         | Atur ke `true` untuk menjeda pemindaian deteksi, dan `false` untuk melanjutkannya. Berguna untuk mengelola peristiwa siklus hidup. Sebagai contoh saat ingin menampilkan informasi dialog sebelum memulai proses scanning                           |
| `colors`               |    ScannerViewColors    | Sebuah objek data yang berisi nilai `Color` untuk menyesuaikan tema berbagai elemen UI seperti garis batas deteksi, tombol, dan overlay.                                                                                                            |
| `flashButton`          |   @Composable lambda    | Slot untuk menyediakan Composable kustom yang akan digunakan sebagai tombol lampu (flash). Lambda ini menyediakan `onClick handler`, status lampu flash saat ini (`isFlashOn`), dan warna `tint` yang direkomendasikan untuk konsistensi UI.        |

-----

## Dokumentasi ScannerState

`ScannerState` adalah sealed class yang mewakili berbagai status operasi pemindai deteksi. Ini
memungkinkan cara yang jelas dan ringkas untuk mengelola UI dan logika bisnis berdasarkan status
pemindaian saat ini.

#### Daftar Status:

|`Scanning` |
|`Processing`   |
|`Error`    |
|`Success`  |

#### `data object Scanning`

Status ini menunjukkan bahwa **pemindai secara aktif mencari item yang dapat dipindai**. Selama
pemindaian, ada kemungkinan gambar terdeteksi ada pantulan cahaya (glare) yang terjadi karena
menggunakan lampu kilat atau di bawah cahaya lampu yang menutupi atau pada area sticker. Ketika
terdeteksi, terdapat jeda 3 detik untuk memungkinkan pengguna menyesuaikan posisi atau menghilangkan
pantulan cahaya sebelum pemindai memindai kembali dan menampilkan informasi pada bagian atas
scanner. Status ini dianggap selesai ketika lampu flash menyala (jika dalam kondisi mati), layar
menjadi kosong (jika dalam kondisi menyala), dan status berubah.

Selama status `Scanning`, ada beberapa proses yang terjadi sebelum gambar diverifikasi, Salah
satunya adalah pendeteksian qr code, jika dalam satu preview atau gambar tidak terdaftar qr code
maka proses deteksi sticker tidak dijalankan. Dalam proses ini juga dilakukan verifikasi apakah
value dari qr code beruba domain / link yang valid dan sudah terdaftar pada Qtrust. Jika tidak maka
akan SDK akan otomatis return `ScannerState.Success` dengan status fake atau qr number unknown.
Jika terdeteksi valid maka akan dilakukan proses verifikasi, proses ini dijalankan dengan indikasi
saat lampu flass mati dan status menjadi `ScannerState.Processing` dengan teks "
`Hold tight while we process your QR`" atau "`Memproses kode QR anda`".

---

#### `data class Processing(val text: String)`

Status ini menandakan bahwa gambar telah berhasil ditangkap, dan **aplikasi sedang memproses gambar
dan data yang dipindai**. UI Anda mungkin menampilkan indikator loading atau placeholder selama fase
ini untuk memberi tahu pengguna. Parameter `text` berisi string teks hasil dari pemindaian, yang
digunakan untuk UI loading (agar proses lebih jelas). String teks akan berisi "
`Uploading QR Image…`" atau "`Mengunggah Gambar QR…`" dalam bahasa Indonesia.

Setelah proses verifikasi sticker, SDK akan memantau hasil yang dikembalikan. Saat:

- `Error` terpicu, jika terdapat kesalahan dari sisi verifikasi, data yang diverifikasi maupun
  unexpected occurance.

- `Success` terpicu, akan memicu penyimpanan ke galeri jika diaktifkan. Status akan berubah menjadi
  `Success` dengan hasil fallback (kembalian default) ke `ScanResult`=>`QR Number Unknown`.

---

#### `data class Error(val exception: Exception, val errorCode: Int? = null)`

Status ini mewakili **kegagalan selama bagian mana pun dari operasi pemindaian atau pemrosesan**.

- `exception`: Objek `Exception` aktual yang memberikan rincian tentang kesalahan.

- `errorCode`: Kode integer opsional yang dapat digunakan untuk identifikasi atau penanganan
  kesalahan yang lebih spesifik.

---

####

`data class Success(val scanResult: ScanResult, val bitmap: Bitmap?, val scanProductResponse: ScanProductResponse?)`

Status ini menunjukkan bahwa **pemindaian dan pemrosesan selanjutnya telah berhasil diselesaikan**.

`scanResult`: Objek yang berisi hasil utama dari pemindaian (misalnya, nilai barcode, data kode QR).

`bitmap`: Gambar `Bitmap` opsional dari item yang dipindai, jika ditangkap dan tersedia.

`scanProductResponse`: Objek opsional yang berisi respons dari pencarian produk atau panggilan API
terkait, berdasarkan `scanResult`.

#### Contoh Penggunaan

Anda dapat mengamati `ScannerState` di lapisan UI Anda (misalnya, di ViewModel atau
Activity/Fragment) untuk memperbarui antarmuka pengguna sesuai dengan itu:

Implementasi:

```kotlin
/// controll scanner state
var scannerState by remember { mutableStateOf<ScannerState>(ScannerState.Scanning) }

ScannerView(
    applicationId = applicationId,
    saveToGallery = true,
    scannerState = scannerState,
    showDetectedBoundary = true,
    onStateChanged = { state -> ... },
)

// Contoh dalam ViewModel atau pengontrol UI

fun changeScannerState(newState: ScannerState) {
    viewModelScope.launch {
        when (newState) {
            is ScannerState.Error -> {
                when (val exception = newState.errorCode) {
                    // Tangani error berdasarkan kode ketika ada UI spesifik untuk ditampilkan
                    408 -> {
                        _scannedQRUiState.value = UiState.Error(
                            exception,
                            errorMessage = newState.exception.message.toString(),
                        )
                    }

                    // Fallback ke default jika kode error yang ditentukan tidak ada di atas
                    else -> {
                        _scannedQRUiState.value = UiState.Error(
                            null,
                            errorMessage = newState.exception.message.toString()
                        )
                    }
                }
            }

            is ScannerState.Processing -> {
                // SDK sedang memproses gambar, tangani dengan menampilkan loading atau UI lain untuk pengguna
                _scannedQRUiState.value = UiState.Loading
            }

            ScannerState.Scanning -> {
                // SDK dalam mode standby menunggu objek masuk ke dalam frame
                _scannedQRUiState.value = UiState.Idle
            }

            is ScannerState.Success -> {
                // SDK mengembalikan scanResult, bitmap, scanProductResponse
                setScanResult(state.scanResult.value)
                setBitmap(state.bitmap)
                setScanProductResponse(newState.scanProductResponse)
            }
        }
    }
}
```

-----

## Catatan penting

Jika terjadi error, `ScannerState` akan berubah nilai menjadi `ScannerState.Error` dan jika ingin
melanjutkan scanning maka harus merubah data scannerState menjadi Scanning
Sebagai contoh:

```kotlin
/// controll scanner state
var scannerState by remember { mutableStateOf<ScannerState>(ScannerState.Scanning) }

ScannerView(
    applicationId = applicationId,
    saveToGallery = true,
    scannerState = scannerState,
    showDetectedBoundary = true,
    /// update changes 
    onStateChanged = { state -> scannerState.value = state },
)

/// listen for scannerState changes
when (scannerState) {
    /// example error occured due to internet connection
    is ScannerState.Error -> {
        ModalBottomSheet(
            onDismissRequest = {
                /// resume scanning state
                scannerState = ScannerState.Scanning
            },
        ) { ... }
    }
}

```

## Dokumentasi ScannerViewColors

Kelas data yang mewakili skema warna untuk `ScannerView`. Kelas ini mendefinisikan warna yang
digunakan untuk berbagai elemen UI di dalam antarmuka pemindai.

```kotlin
data class ScannerViewColors(
    val detectionFrameColor: Color = DarkBlue,
    val detectionFrameOptimalColor: Color = LimeGreen,
    val surfaceColor: Color = Color(0xFF232333),
    val onSurfaceColor: Color = Color.White,
    val appBarContainerColor: Color = Color(0xFF232333),
    val appBarContentColor: Color = Color.White,
    val glareIndicatorContainerColor: Color = Color(0xD94A4A4A),
    val glareIndicatorContentColor: Color = Color.White,
)
```

### Parameter

| Parameter                      |  Tipe   | Deskripsi                                                                  |
|:-------------------------------|:-------:|:---------------------------------------------------------------------------|
| `detectionFrameColor`          | `Color` | Warna bingkai deteksi saat dokumen tidak berada pada posisi optimal.       |
| `detectionFrameOptimalColor`   | `Color` | Warna bingkai deteksi saat dokumen berada pada posisi optimal.             |
| `surfaceColor`                 | `Color` | Warna latar belakang permukaan tampilan pemindai.                          |
| `onSurfaceColor`               | `Color` | Warna konten (teks, ikon) yang ditampilkan di permukaan tampilan pemindai. |
| `appBarContainerColor`         | `Color` | Warna latar belakang app bar.                                              |
| `appBarContentColor`           | `Color` | Warna konten (teks, ikon) di dalam app bar.                                |
| `glareIndicatorContainerColor` | `Color` | Warna latar belakang indikator pantulan cahaya.                            |
| `glareIndicatorContentColor`   | `Color` | Warna konten (teks, ikon) di dalam indikator pantulan cahaya.              |

-----

## Dokumentasi ScanResult

### Parameter

| Tipe                | Deskripsi                                                                                             |
|:--------------------|:------------------------------------------------------------------------------------------------------|
| `QR_GENUINE`        | Menunjukkan bahwa kode QR yang dipindai terverifikasi dan asli.                                       |
| `QR_POOR_IMAGE`     | Hasil pemindaian ini terpicu saat gambar yang diambil tidak cukup jelas atau baik untuk diverifikasi. |
| `QR_FAKE`           | Ketika kode QR yang dipindai bukan dari qtrust, menghasilkan status palsu.                            |
| `QR_NUMBER_UNKNOWN` | Terpicu saat domain valid dari qtrust, tetapi nomor QR berikutnya tidak terdaftar.                    |

-----

## Dokumentasi ScanProductResponse

ScanProductResponse merupakan object yang berisi hasil-hasil indentifikasi dari hasil proses
verifikasi.

```kotlin
data class ScanProductResponse(
    val message: String? = null,
    /// value dari sticker
    var qrcode: Qrcode? = null,
    /// unique value of each classification
    val pid: String? = null,
    /// classification result
    val classification: Classification? = null,
    /// detected qr object
    val obj: Object? = null,
)
```

### ScanProductResponse

| Parameter        |       Tipe       | Deskripsi                              |
|:-----------------|:----------------:|:---------------------------------------|
| `qrcode`         |     `Qrcode`     | Objek kode QR yang dipindai.           |
| `pid`            |     `String`     | ID unik untuk setiap pemindaian.       |
| `classification` | `Classification` | Hasil klasifikasi dari model `tflite`. |

### Qrcode

Objek hasil ini berisi data untuk kode QR.

| Parameter |   Tipe   | Deskripsi              |
|:----------|:--------:|:-----------------------|
| `text`    | `String` | Kode QR yang dipindai. |

### Classification

Objek hasil ini terutama berasal dari model `tflite`, dan kami akan menggunakan variabel di dalamnya
untuk menentukan alur selanjutnya.

| Parameter |   Tipe   | Deskripsi                                                                     |
|:----------|:--------:|:------------------------------------------------------------------------------|
| `label`   | `String` | Ini adalah variabel terpenting, karena hasil akhir berasal dari variabel ini. |
| `score`   | `String` | Skor dari proses klasifikasi `tflite`.                                        |

Seperti yang saya sebutkan di tabel, variabel `label` adalah hasil akhir dari SDK ini. Hasilnya akan
digunakan untuk menentukan alur aplikasi selanjutnya. Ada 3 variabel yang tersedia:

| Label     | Deskripsi                                                                                                 |
|:----------|:----------------------------------------------------------------------------------------------------------|
| `genuine` | Menunjukkan bahwa kode QR yang dipindai terverifikasi dan asli.                                           |
| `fake`    | Ketika kode QR yang dipindai bukan dari qtrust, hasilnya palsu.                                           |
| `poor`    | Hasil pemindaian ini terpicu saat gambar yang diambil tidak cukup jelas atau baik untuk diklasifikasikan. |