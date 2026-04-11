package com.example.apartmantakip

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import java.io.IOException
import java.io.OutputStream
import java.util.*

class BluetoothPrinterService(private val context: Context) {

    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private val PRINTER_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @SuppressLint("MissingPermission")
    fun connectToDevice(deviceAddress: String): Boolean {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null) return false
        val device = bluetoothAdapter.getRemoteDevice(deviceAddress)

        return try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(PRINTER_UUID)
            bluetoothSocket?.connect()
            outputStream = bluetoothSocket?.outputStream
            true
        } catch (e: IOException) {
            Log.e("PrinterService", "Bağlantı hatası: ${e.message}")
            closeConnection()
            false
        }
    }

    fun printText(text: String) {
        try {
            outputStream?.write(text.toByteArray(Charsets.UTF_8))
            outputStream?.flush()
        } catch (e: IOException) {
            Log.e("PrinterService", "Yazdırma hatası: ${e.message}")
        }
    }

    fun printInvoice(invoice: Invoice, apartmentName: String) {
        val esc = EscPosCommands()
        try {
            outputStream?.write(esc.initPrinter())
            outputStream?.write(esc.selectAlignment(1)) // Center
            outputStream?.write(esc.selectSize(1)) // Large
            outputStream?.write("$apartmentName\n".toByteArray(Charsets.UTF_8))
            
            outputStream?.write(esc.selectSize(0)) // Normal
            outputStream?.write("SU VE GIDER FATURASI\n".toByteArray(Charsets.UTF_8))
            outputStream?.write("--------------------------------\n".toByteArray())
            
            outputStream?.write(esc.selectAlignment(0)) // Left
            val blokStr = if (!invoice.blok.isNullOrEmpty()) "${invoice.blok} Blok " else ""
            outputStream?.write("Daire: $blokStr No: ${invoice.daireNo}\n".toByteArray(Charsets.UTF_8))
            outputStream?.write("Sakin: ${invoice.adSoyad ?: ""}\n".toByteArray(Charsets.UTF_8))
            outputStream?.write("Donem: ${invoice.donem}\n".toByteArray(Charsets.UTF_8))
            outputStream?.write("--------------------------------\n".toByteArray())
            
            outputStream?.write("Ilk Endeks : ${"%.3f".format(invoice.ilkEndeks)}\n".toByteArray(Charsets.UTF_8))
            outputStream?.write("Son Endeks : ${"%.3f".format(invoice.sonEndeks)}\n".toByteArray(Charsets.UTF_8))
            outputStream?.write("Tuketim    : ${"%.3f".format(invoice.tuketim)} m3\n".toByteArray(Charsets.UTF_8))
            outputStream?.write("--------------------------------\n".toByteArray())

            if (invoice.suBedeli > 0) 
                outputStream?.write("Su Bedeli      : %7.2f TL\n".format(invoice.suBedeli).toByteArray(Charsets.UTF_8))
            if (invoice.atiksuBedeli > 0)
                outputStream?.write("Atiksu Bedeli  : %7.2f TL\n".format(invoice.atiksuBedeli).toByteArray(Charsets.UTF_8))
            if (invoice.ctv > 0)
                outputStream?.write("C.T.V.         : %7.2f TL\n".format(invoice.ctv).toByteArray(Charsets.UTF_8))
            if (invoice.katiAtik > 0)
                outputStream?.write("Kati Atik      : %7.2f TL\n".format(invoice.katiAtik).toByteArray(Charsets.UTF_8))
            if (invoice.bertaraf > 0)
                outputStream?.write("Bertaraf       : %7.2f TL\n".format(invoice.bertaraf).toByteArray(Charsets.UTF_8))
            if (invoice.aidat > 0)
                outputStream?.write("Aidat          : %7.2f TL\n".format(invoice.aidat).toByteArray(Charsets.UTF_8))
            if (invoice.ekstraGiderToplam > 0)
                outputStream?.write("Ekstra Giderler: %7.2f TL\n".format(invoice.ekstraGiderToplam).toByteArray(Charsets.UTF_8))
            
            outputStream?.write("K.D.V.         : %7.2f TL\n".format(invoice.kdv).toByteArray(Charsets.UTF_8))
            outputStream?.write("--------------------------------\n".toByteArray())
            
            outputStream?.write(esc.selectSize(1)) // Large
            outputStream?.write(esc.selectAlignment(2)) // Right
            outputStream?.write("TOPLAM: %.2f TL\n".format(invoice.toplam).toByteArray(Charsets.UTF_8))
            
            outputStream?.write(esc.selectSize(0)) // Normal
            outputStream?.write(esc.selectAlignment(1)) // Center
            outputStream?.write("\nIyi gunler dileriz.\n".toByteArray(Charsets.UTF_8))
            
            outputStream?.write("\n\n\n\n".toByteArray()) // Feed paper
            outputStream?.flush()
        } catch (e: IOException) {
            Log.e("PrinterService", "Fatura yazdırma hatası: ${e.message}")
        }
    }

    fun closeConnection() {
        try {
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {
            Log.e("PrinterService", "Kapatma hatası: ${e.message}")
        }
    }

    class EscPosCommands {
        fun initPrinter() = byteArrayOf(0x1B, 0x40)
        fun selectAlignment(alignment: Int) = byteArrayOf(0x1B, 0x61, alignment.toByte()) // 0:Left, 1:Center, 2:Right
        fun selectSize(size: Int): ByteArray {
            val s = when(size) {
                1 -> 0x11.toByte() // Double height and width
                else -> 0x00.toByte() // Normal
            }
            return byteArrayOf(0x1D, 0x21, s)
        }
    }
}
