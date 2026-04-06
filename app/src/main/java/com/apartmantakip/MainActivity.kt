package com.apartmantakip

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.net.http.SslError
import android.os.*
import android.window.OnBackInvokedDispatcher
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.*
import java.net.URL

class MainActivity : AppCompatActivity() {

    // ── Sunucu adresi ── Kendi IP adresinizle değiştirin ─────────────────
    private val SERVER_URL = "https://apartmantakip.peuplade.com.tr"
    // ─────────────────────────────────────────────────────────────────────

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private var bluetoothPrinter: BluetoothPrinterHelper? = null
    private var whatsAppHelper: WhatsAppHelper? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val enableBtLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) showBluetoothDeviceChooser()
    }

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val allGranted = perms.values.all { it }
        if (allGranted) connectBluetoothPrinter()
        else showToast("Bluetooth izni reddedildi")
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Pencereyi tam ekran yap ve navigasyon çubuğu alanını kullanma
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContentView(R.layout.activity_main)

        // UI elementlerini bul
        webView    = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        whatsAppHelper = WhatsAppHelper(this)

        setupWebView()
        webView.loadUrl(SERVER_URL)

        // Navigasyon Tuşlarını Gizle
        hideSystemUI()

        // Güncelleme Kontrolü Yap
        checkUpdates()

        // Android 13+ JESTLERİ İÇİN (Xiaomi vb.)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_OVERLAY
            ) {
                performBackAction()
            }
        }
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        
        // Hem durum çubuğunu hem navigasyon çubuğunu gizle
        controller.hide(WindowInsetsCompat.Type.systemBars())
        
        // Kullanıcı ekranın kenarından kaydırdığında çubukların geçici olarak görünmesini sağla
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    private fun checkUpdates() {
        val githubUser = "falzer4" 
        val repoName   = "Apartman_Takip" 
        val apiUrl = "https://api.github.com/repos/$githubUser/$repoName/releases/latest"

        android.util.Log.i("UpdateCheck", "Güncelleme kontrolü başlatılıyor: $apiUrl")

        scope.launch(Dispatchers.IO) {
            try {
                val url = java.net.URL(apiUrl)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.setRequestProperty("User-Agent", "ApartmanTakip-App")
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val responseCode = connection.responseCode
                android.util.Log.i("UpdateCheck", "GitHub Yanıt Kodu: $responseCode")

                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = org.json.JSONObject(response)
                    val latestTag = json.getString("tag_name").replace("v", "")
                    
                    val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
                    } else {
                        @Suppress("DEPRECATION")
                        packageManager.getPackageInfo(packageName, 0)
                    }
                    val currentVer = pInfo.versionName ?: "1.0.0"

                    android.util.Log.i("UpdateCheck", "Sürüm Karşılaştırma -> Mevcut: $currentVer | GitHub: $latestTag")

                    if (isNewerVersion(currentVer, latestTag)) {
                        val downloadUrl = json.getString("html_url")
                        android.util.Log.i("UpdateCheck", "Yeni sürüm bulundu! Dialog gösteriliyor.")
                        withContext(Dispatchers.Main) {
                            showUpdateDialog(latestTag, downloadUrl)
                        }
                    } else {
                        android.util.Log.i("UpdateCheck", "Uygulama güncel.")
                    }
                } else {
                    val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    android.util.Log.e("UpdateCheck", "API Hatası ($responseCode): $errorBody")
                }
            } catch (e: Exception) {
                android.util.Log.e("UpdateCheck", "Kritik Hata: ${e.message}", e)
            }
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        return try {
            val currParts = current.split(".").map { it.toInt() }
            val lateParts = latest.split(".").map { it.toInt() }
            for (i in 0 until Math.min(currParts.size, lateParts.size)) {
                if (lateParts[i] > currParts[i]) return true
                if (lateParts[i] < currParts[i]) return false
            }
            lateParts.size > currParts.size
        } catch (_: Exception) {
            latest > current
        }
    }

    private fun showUpdateDialog(newVersion: String, url: String) {
        AlertDialog.Builder(this)
            .setTitle("🚀 Yeni Sürüm Hazır!")
            .setMessage("Apartman Takip v$newVersion sürümü yayınlandı. Uygulamanızı güncelleyerek yeni özellikleri kullanmaya başlayabilirsiniz.")
            .setPositiveButton("Şimdi Güncelle") { _, _ ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
            .setNegativeButton("Daha Sonra", null)
            .setCancelable(false)
            .show()
    }

    private fun performBackAction() {
        // WebView içinde JS çalıştır
        webView.evaluateJavascript(
            "(function() { " +
            "  var btn = document.getElementById('btnBackToMain'); " +
            "  if (btn && window.getComputedStyle(btn).display !== 'none') { " +
            "    btn.click(); return 'OK'; " +
            "  } " +
            "  return 'FAIL'; " +
            "})()", 
            { result ->
                if (result == null || !result.contains("OK")) {
                    if (webView.canGoBack()) {
                        webView.goBack()
                    } else {
                        showToast("Çıkış Kilitlendi - Ana Sayfadasınız")
                    }
                }
            }
        )
    }

    // BU KISIM ÇOK KRİTİK: Geri tuşu sinyalini Android'e ASLA ulaştırmıyoruz.
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.action == KeyEvent.ACTION_UP) {
                performBackAction()
            }
            return true // TRUE dönerek sinyali burada öldürüyoruz. super çağırmıyoruz!
        }
        return super.dispatchKeyEvent(event)
    }

    // Diğer tüm geri gitme metodlarını siliyoruz/boş bırakıyoruz ki karışıklık olmasın.
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        performBackAction()
    }

    // ─── WebView Kurulum ─────────────────────────────────────────────────
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled      = true
            domStorageEnabled      = true
            databaseEnabled        = true
            allowFileAccess        = true
            loadWithOverviewMode   = true
            useWideViewPort        = true
            setSupportZoom(false)
            builtInZoomControls    = false
            displayZoomControls    = false
            cacheMode              = WebSettings.LOAD_DEFAULT
            mixedContentMode       = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            userAgentString        = userAgentString + " ApartmanTakipApp/1.0"
        }

        webView.addJavascriptInterface(JSBridge(), "AndroidBridge")

        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedSslError(v: WebView?, h: SslErrorHandler?, e: SslError?) {
                h?.proceed()
            }
            override fun onPageStarted(v: WebView?, url: String?, f: android.graphics.Bitmap?) {
                progressBar.visibility = View.VISIBLE
            }
            override fun onPageFinished(v: WebView?, url: String?) {
                progressBar.visibility = View.GONE
                notifyBtStatus()
            }
            override fun onReceivedError(v: WebView?, req: WebResourceRequest?, err: WebResourceError?) {
                progressBar.visibility = View.GONE
                showOfflinePage()
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(v: WebView?, p: Int) {
                progressBar.progress = p
                if (p == 100) Handler(Looper.getMainLooper()).postDelayed(
                    { progressBar.visibility = View.GONE }, 400
                )
            }
            override fun onConsoleMessage(m: ConsoleMessage?): Boolean {
                m?.let { android.util.Log.d("ApartmanWebView", "[${it.messageLevel()}] ${it.message()}") }
                return true
            }
        }
    }

    private fun showOfflinePage() {
        val html = """
            <html><body style="background:#0a0f1e;color:#94a3b8;font-family:sans-serif;
            display:flex;flex-direction:column;align-items:center;justify-content:center;height:100vh;margin:0">
            <div style="font-size:48px">📡</div>
            <h2 style="color:#e2e8f0;margin:16px 0 8px">Sunucuya Ulaşılamıyor</h2>
            <p>$SERVER_URL</p>
            <button onclick="location.reload()" style="margin-top:20px;padding:12px 24px;
            background:#00bcd4;color:#000;border:none;border-radius:8px;font-size:16px;cursor:pointer">
            🔄 Yeniden Dene</button></body></html>
        """.trimIndent()
        webView.loadData(html, "text/html", "UTF-8")
    }

    // ─── JavaScript → Android Köprüsü ────────────────────────────────────
    inner class JSBridge {

        /** WhatsApp mesajı + görsel gönder */
        @JavascriptInterface
        fun whatsappGonder(telefon: String, mesaj: String, faturaId: Int) {
            runOnUiThread {
                scope.launch(Dispatchers.IO) {
                    try {
                        val imgUrl = "$SERVER_URL?action=gorsel_olustur&id=$faturaId"
                        URL(imgUrl).openConnection().connect()
                        val bitmap = BitmapFactory.decodeStream(
                            URL("$SERVER_URL?gorsel&fatura_id=$faturaId").openStream()
                        )
                        withContext(Dispatchers.Main) {
                            if (bitmap != null)
                                whatsAppHelper?.sendWithImage(telefon, mesaj, bitmap)
                            else
                                whatsAppHelper?.sendTextOnly(telefon, mesaj)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            whatsAppHelper?.sendTextOnly(telefon, mesaj)
                        }
                    }
                }
            }
        }

        /** Görsel URL hazır → önizle */
        @JavascriptInterface
        fun onGorselOlusturuldu(gorselUrl: String, faturaId: Int) {
            scope.launch(Dispatchers.IO) {
                try {
                    val bmp = BitmapFactory.decodeStream(URL(gorselUrl).openStream())
                    withContext(Dispatchers.Main) {
                        bmp?.let { whatsAppHelper?.showImagePreview(it, faturaId) }
                    }
                } catch (_: Exception) {}
            }
        }

        /** Bluetooth yazıcı seçim diyaloğu aç */
        @JavascriptInterface
        fun bluetoothYaziciSec() = runOnUiThread { connectBluetoothPrinter() }

        /** JSON faturayı Bluetooth yazıcıdan bas */
        @JavascriptInterface
        fun yazdir(faturaJson: String) = runOnUiThread { printFatura(faturaJson) }

        /** Sunucu adresini güncelle */
        @JavascriptInterface
        fun setSunucuAdresi(url: String) {
            runOnUiThread {
                getSharedPreferences("apartman_takip", Context.MODE_PRIVATE)
                    .edit().putString("server_url", url).apply()
                showToast("Sunucu adresi güncellendi")
            }
        }

        /** Google ile giriş yap */
        @JavascriptInterface
        fun googleIleGirisYap() {
            runOnUiThread {
                signInWithGoogle()
            }
        }
    }

    // ─── Google Sign-In ───────────────────────────────────────────────────
    private fun signInWithGoogle() {
        val credentialManager = CredentialManager.create(this)
        val webClientId = getString(R.string.default_web_client_id)

        if (webClientId == "YOUR_WEB_CLIENT_ID_HERE") {
            showToast("Hata: Google Web Client ID yapılandırılmadı!")
            return
        }

        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(true)
            .build()

        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(
                    context = this@MainActivity,
                    request = request
                )
                handleSignIn(result)
            } catch (e: GetCredentialException) {
                android.util.Log.e("GoogleSignIn", "Hata: ${e.message}")
                showToast("Giriş iptal edildi veya hata oluştu")
            }
        }
    }

    private fun handleSignIn(result: GetCredentialResponse) {
        val credential = result.credential

        if (credential is GoogleIdTokenCredential) {
            val idToken = credential.idToken
            val email = credential.id
            val displayName = credential.displayName
            
            android.util.Log.d("GoogleSignIn", "Token: $idToken")
            
            // WebView'e bildir
            webView.evaluateJavascript(
                "try{window.fromAndroid.onGoogleLoginSuccess('$idToken', '$email', '$displayName')}catch(e){}", null
            )
            showToast("Hoş geldiniz, $displayName")
        }
    }

    // ─── Bluetooth ────────────────────────────────────────────────────────
    private fun connectBluetoothPrinter() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null) { showToast("Bluetooth desteklenmiyor"); return }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val needed = mutableListOf<String>()
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.BLUETOOTH_CONNECT)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) needed.add(Manifest.permission.BLUETOOTH_SCAN)
            if (needed.isNotEmpty()) { permLauncher.launch(needed.toTypedArray()); return }
        }

        if (!adapter.isEnabled) {
            enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            return
        }
        showBluetoothDeviceChooser()
    }

    @SuppressLint("MissingPermission")
    private fun showBluetoothDeviceChooser() {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return
        val paired = adapter.bondedDevices?.toList() ?: emptyList()
        if (paired.isEmpty()) {
            showToast("Eşleştirilmiş Bluetooth cihazı bulunamadı")
            return
        }
        val names = paired.map { "${it.name}\n${it.address}" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("📠 Yazıcı Seç")
            .setItems(names) { _, i -> connectToDevice(paired[i]) }
            .setNegativeButton("İptal", null)
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        showToast("Bağlanıyor: ${device.name}...")
        scope.launch(Dispatchers.IO) {
            bluetoothPrinter = BluetoothPrinterHelper(device)
            val ok = bluetoothPrinter!!.connect()
            withContext(Dispatchers.Main) {
                val msg = if (ok) "✓ Bağlandı: ${device.name}" else "✗ Bağlantı başarısız!"
                showToast(msg)
                notifyBtStatus()
            }
        }
    }

    private fun notifyBtStatus() {
        val status = when {
            bluetoothPrinter?.isConnected() == true -> "Yazıcı Bağlı ✓"
            else -> "Yazıcı Bağlı Değil"
        }
        webView.evaluateJavascript(
            "try{window.fromAndroid.setBluetoothStatus('$status')}catch(e){}", null
        )
    }

    private fun printFatura(faturaJson: String) {
        val printer = bluetoothPrinter
        if (printer == null || !printer.isConnected()) {
            AlertDialog.Builder(this)
                .setTitle("Yazıcı Bağlı Değil")
                .setMessage("Bluetooth yazıcı bağlı değil. Şimdi bağlanmak ister misiniz?")
                .setPositiveButton("Bağlan") { _, _ -> connectBluetoothPrinter() }
                .setNegativeButton("İptal", null)
                .show()
            return
        }
        scope.launch(Dispatchers.IO) {
            try {
                val json = org.json.JSONObject(faturaJson)
                printer.printFaturaFormatted(
                    binaAdi       = json.optString("bina_adi", "APARTMAN YÖNETİMİ"),
                    daire         = json.optString("daire_no"),
                    kat           = json.optString("kat"),
                    sakin         = json.optString("ad_soyad", "-"),
                    telefon       = json.optString("telefon", ""),
                    donem         = json.optString("donem"),
                    ilkEndeks     = json.optString("ilk_endeks"),
                    sonEndeks     = json.optString("son_endeks"),
                    tuketim       = json.optString("tuketim"),
                    suBedeli      = json.optString("su_bedeli"),
                    atiksuBedeli  = json.optString("atiksu_bedeli"),
                    katiAtik      = json.optString("kati_atik"),
                    ctv           = json.optString("ctv"),
                    kdv           = json.optString("kdv"),
                    toplam        = json.optString("toplam"),
                    tarih         = json.optString("olusturma_tarihi")
                )
                withContext(Dispatchers.Main) {
                    webView.evaluateJavascript(
                        "try{window.fromAndroid.onPrintComplete()}catch(e){}", null
                    )
                    showToast("✓ Yazdırma tamamlandı")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    webView.evaluateJavascript(
                        "try{window.fromAndroid.onPrintError('${e.message}')}catch(e){}", null
                    )
                    showToast("Yazdırma hatası: ${e.message}")
                }
            }
        }
    }

    // ─── Yardımcılar ──────────────────────────────────────────────────────
    fun showToast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        bluetoothPrinter?.disconnect()
        webView.destroy()
    }
}
