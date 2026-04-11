package com.example.apartmantakip

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private lateinit var printerService: BluetoothPrinterService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        printerService = BluetoothPrinterService(this)

        webView = WebView(this)
        setContentView(webView)

        setupWebView()
        checkPermissions()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        webSettings.databaseEnabled = true
        
        // Bu ayar yerel dosyalara ve karışık içeriğe (http/https) izin verir
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Sayfa yüklendiğinde gerekirse bir JS fonksiyonu çağrılabilir
            }
        }
        
        webView.addJavascriptInterface(WebAppInterface(this, printerService), "Android")
        
        // Emülatör için 10.0.2.2, gerçek cihaz için sunucu IP'si girilmeli.
        // index.php zaten Android köprüsü bekliyor.
        webView.loadUrl("http://10.0.2.2/apartantakip/index.php")
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        }
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.INTERNET)

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), 1)
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    class WebAppInterface(private val context: Context, private val printerService: BluetoothPrinterService) {
        
        @JavascriptInterface
        @SuppressLint("MissingPermission")
        fun printToBluetooth(printerName: String, text: String) {
            val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                showToast("Bluetooth kapalı veya desteklenmiyor")
                return
            }

            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
            val printer = pairedDevices?.firstOrNull { device ->
                device.name?.contains(printerName, ignoreCase = true) == true ||
                        device.name?.contains("Printer", ignoreCase = true) == true ||
                        device.name?.contains("K329", ignoreCase = true) == true
            }

            if (printer != null) {
                showToast("Yazıcıya bağlanılıyor: ${printer.name}")
                
                Thread {
                    try {
                        val connected = printerService.connectToDevice(printer.address)
                        if (connected) {
                            printerService.printText(text)
                            // Verilerin tampondan boşalması için kısa bir bekleme
                            Thread.sleep(500)
                            printerService.closeConnection()
                            showToast("Yazdırma başarılı")
                        } else {
                            showToast("Yazıcıya bağlanılamadı")
                        }
                    } catch (e: Exception) {
                        showToast("Yazdırma hatası: ${e.message}")
                    }
                }.start()
            } else {
                showToast("Eşleşmiş $printerName yazıcı bulunamadı")
            }
        }

        private fun showToast(message: String) {
            (context as? MainActivity)?.runOnUiThread {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
