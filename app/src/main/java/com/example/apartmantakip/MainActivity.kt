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
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var offlineLayout: View
    private lateinit var printerService: BluetoothPrinterService
    private val serverUrl = "https://apartmantakip.peuplade.com.tr/index.php"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        printerService = BluetoothPrinterService(this)
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        offlineLayout = findViewById(R.id.offlineLayout)

        val tvServerUrl: TextView = findViewById(R.id.tvServerUrl)
        tvServerUrl.text = serverUrl

        val btnRetry: Button = findViewById(R.id.btnRetry)
        btnRetry.setOnClickListener {
            offlineLayout.visibility = View.GONE
            webView.visibility = View.VISIBLE
            webView.reload()
        }

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
        
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
                if (newProgress == 100) {
                    progressBar.visibility = View.GONE
                } else {
                    progressBar.visibility = View.VISIBLE
                }
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                // Sadece ana sayfa yüklemesindeki hataları göster (statik kaynaklardaki hataları yoksay)
                if (request?.isForMainFrame == true) {
                    webView.visibility = View.GONE
                    offlineLayout.visibility = View.VISIBLE
                }
            }
        }
        
        webView.addJavascriptInterface(WebAppInterface(this, printerService), "Android")
        webView.loadUrl(serverUrl)
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
