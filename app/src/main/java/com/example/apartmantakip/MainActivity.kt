package com.example.apartmantakip

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.apartmantakip.ui.theme.ApartmantakipTheme
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : ComponentActivity() {
    private lateinit var printerService: BluetoothPrinterService
    
    private val retrofit = Retrofit.Builder()
        .baseUrl("http://10.0.2.2/apartantakip/") // Android emülatör için localhost erişimi. Gerçek cihazda IP adresi verilmeli.
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val apiService: ApiService = retrofit.create(ApiService::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        printerService = BluetoothPrinterService(this)
        enableEdgeToEdge()
        setContent {
            ApartmantakipTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PrinterScreen(
                        printerService = printerService,
                        apiService = apiService,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun PrinterScreen(
    printerService: BluetoothPrinterService, 
    apiService: ApiService,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isPrinting by remember { mutableStateOf(false) }
    var invoices by remember { mutableStateOf<List<InvoiceSummary>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    // Dönem bilgisi (Örn: 2024-05)
    val currentPeriod = "2024-05" // Dinamik hale getirilebilir

    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            Toast.makeText(context, "İzinler onaylandı.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Bluetooth izinleri gerekli!", Toast.LENGTH_SHORT).show()
        }
    }

    // Faturaları yükle
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val response = apiService.getInvoiceList(donem = currentPeriod)
            invoices = response.faturalar
        } catch (e: Exception) {
            Toast.makeText(context, "Faturalar yüklenemedi: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Urovo K329 Fatura Basımı", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Donem: $currentPeriod", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(invoices) { invoice ->
                    InvoiceItem(invoice) {
                        val hasPermissions = permissionsToRequest.all {
                            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                        }

                        if (!hasPermissions) {
                            launcher.launch(permissionsToRequest)
                        } else {
                            coroutineScope.launch {
                                isPrinting = true
                                fetchAndPrint(context, printerService, apiService, invoice.id) {
                                    isPrinting = false
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (isPrinting) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            Text("Yazdırılıyor...", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun InvoiceItem(invoice: InvoiceSummary, onPrintClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val blokStr = if (!invoice.blok.isNullOrEmpty()) "${invoice.blok} Blok " else ""
                Text(text = "$blokStr No: ${invoice.daireNo}", fontWeight = FontWeight.Bold)
                Text(text = invoice.adSoyad ?: "Sakin Belirtilmemiş", style = MaterialTheme.typography.bodySmall)
                Text(text = "Tutar: %.2f TL".format(invoice.toplam), color = MaterialTheme.colorScheme.primary)
            }
            Button(onClick = onPrintClick) {
                Text("Bas")
            }
        }
    }
}

@SuppressLint("MissingPermission")
private suspend fun fetchAndPrint(
    context: Context,
    printerService: BluetoothPrinterService,
    apiService: ApiService,
    invoiceId: Int,
    onFinished: () -> Unit
) {
    try {
        // API'den detaylı veriyi çek
        val response = apiService.getInvoiceDetail(id = invoiceId)
        val invoiceDetail = response.fatura
        
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Toast.makeText(context, "Bluetooth kapalı veya desteklenmiyor", Toast.LENGTH_SHORT).show()
            onFinished()
            return
        }

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
        val printer = pairedDevices?.firstOrNull { device ->
            device.name?.contains("K329", ignoreCase = true) == true || 
            device.name?.contains("Printer", ignoreCase = true) == true
        }

        if (printer != null) {
            val connected = printerService.connectToDevice(printer.address)
            if (connected) {
                printerService.printInvoice(
                    invoice = invoiceDetail,
                    apartmentName = "DAMLA APARTMANI" // Bu da API'den gelebilir
                )
                printerService.closeConnection()
                Toast.makeText(context, "Fatura başarıyla basıldı", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Yazıcıya bağlanılamadı", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Eşleşmiş K329 yazıcı bulunamadı", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_LONG).show()
    } finally {
        onFinished()
    }
}
