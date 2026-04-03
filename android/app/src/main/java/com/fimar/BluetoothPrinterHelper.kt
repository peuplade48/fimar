package com.fimar

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

/**
 * ESC/POS Bluetooth Termal Yazıcı Yardımcısı
 * 58mm (32 karakter) ve 80mm (48 karakter) destekler
 */
class BluetoothPrinterHelper(private val device: BluetoothDevice) {

    private var socket: BluetoothSocket? = null
    private var out: OutputStream? = null

    companion object {
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        // ── ESC/POS Komut Sabitleri ──
        private val ESC:  Byte = 0x1B
        private val GS:   Byte = 0x1D
        private val FS:   Byte = 0x1C

        val CMD_INIT          = byteArrayOf(ESC, 0x40)
        val CMD_LF            = byteArrayOf(0x0A)
        val CMD_CUT_FULL      = byteArrayOf(GS, 0x56, 0x41, 0x00)
        val CMD_CUT_PARTIAL   = byteArrayOf(GS, 0x56, 0x42, 0x01)
        val CMD_FEED_N        = byteArrayOf(ESC, 0x64, 0x04)  // 4 satır

        val CMD_BOLD_ON       = byteArrayOf(ESC, 0x45, 0x01)
        val CMD_BOLD_OFF      = byteArrayOf(ESC, 0x45, 0x00)
        val CMD_UNDERLINE_ON  = byteArrayOf(ESC, 0x2D, 0x01)
        val CMD_UNDERLINE_OFF = byteArrayOf(ESC, 0x2D, 0x00)

        val CMD_ALIGN_LEFT    = byteArrayOf(ESC, 0x61, 0x00)
        val CMD_ALIGN_CENTER  = byteArrayOf(ESC, 0x61, 0x01)
        val CMD_ALIGN_RIGHT   = byteArrayOf(ESC, 0x61, 0x02)

        val CMD_SIZE_NORMAL   = byteArrayOf(GS, 0x21, 0x00)
        val CMD_SIZE_2H       = byteArrayOf(GS, 0x21, 0x01)  // 2x yükseklik
        val CMD_SIZE_2W       = byteArrayOf(GS, 0x21, 0x10)  // 2x genişlik
        val CMD_SIZE_2X2      = byteArrayOf(GS, 0x21, 0x11)  // 2x2
        val CMD_SIZE_3X3      = byteArrayOf(GS, 0x21, 0x22)  // 3x3

        val CMD_CHARSET_TR    = byteArrayOf(ESC, 0x74, 0x00) // PC437 - Türkçe karakter desteği

        // Karakter genişlikleri
        const val WIDTH_58MM = 32
        const val WIDTH_80MM = 48
        
        // Teşekkür mesajı sabitini buraya taşıdık
        const val CMD_THANKS_STR = "Tesekkur Ederiz!\n"
    }

    // ── Bağlantı ────────────────────────────────────────────────────────
    @Suppress("MissingPermission")
    fun connect(): Boolean {
        return try {
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            BluetoothAdapter.getDefaultAdapter()?.cancelDiscovery()
            socket?.connect()
            out = socket?.outputStream
            true
        } catch (e: IOException) {
            try {
                // Fallback: createRfcommSocket(channel=1)
                val m = device.javaClass.getMethod("createRfcommSocket", Int::class.java)
                socket = m.invoke(device, 1) as BluetoothSocket
                socket?.connect()
                out = socket?.outputStream
                true
            } catch (e2: Exception) {
                disconnect()
                false
            }
        }
    }

    fun isConnected(): Boolean = socket?.isConnected == true

    fun disconnect() {
        try { out?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
        out = null; socket = null
    }

    // ── Ham bayt yazdır ──────────────────────────────────────────────────
    fun printRaw(data: ByteArray) {
        val stream = out ?: throw IOException("Yazıcı bağlı değil")
        stream.write(CMD_INIT)
        stream.write(data)
        stream.write(CMD_FEED_N)
        stream.write(CMD_CUT_FULL)
        stream.flush()
    }

    // ── Biçimlendirilmiş fatura yazdır ───────────────────────────────────
    fun printFaturaFormatted(
        binaAdi:      String,
        daire:        String,
        kat:          String,
        sakin:        String,
        telefon:      String,
        donem:        String,
        ilkEndeks:    String,
        sonEndeks:    String,
        tuketim:      String,
        suBedeli:     String,
        atiksuBedeli: String,
        katiAtik:     String,
        ctv:          String,
        kdv:          String,
        toplam:       String,
        tarih:        String,
        charWidth:    Int = WIDTH_80MM
    ) {
        val stream = out ?: throw IOException("Yazıcı bağlı değil")
        val LINE  = "-".repeat(charWidth) + "\n"
        val LINE2 = "=".repeat(charWidth) + "\n"
        val ENC   = Charsets.ISO_8859_1   // ESC/POS standart

        fun centerStr(s: String): String {
            val pad = maxOf(0, (charWidth - s.length) / 2)
            return " ".repeat(pad) + s + "\n"
        }
        fun rightCol(label: String, value: String): String {
            val sp = charWidth - label.length - value.length
            return label + " ".repeat(maxOf(1, sp)) + value + "\n"
        }
        // Türkçe karakterleri ASCII benzerine dönüştür
        fun tr(s: String) = s
            .replace("ş","s").replace("Ş","S")
            .replace("ğ","g").replace("Ğ","G")
            .replace("ı","i").replace("İ","I")
            .replace("ö","o").replace("Ö","O")
            .replace("ü","u").replace("Ü","U")
            .replace("ç","c").replace("Ç","C")

        stream.write(CMD_INIT)
        stream.write(CMD_CHARSET_TR)

        // ── Başlık ──────────────────────────────────────────────────────
        stream.write(CMD_ALIGN_CENTER)
        stream.write(CMD_BOLD_ON)
        stream.write(CMD_SIZE_2X2)
        stream.write((tr(binaAdi) + "\n").toByteArray(ENC))
        stream.write(CMD_SIZE_2W)
        stream.write("SU FATURASI\n".toByteArray(ENC))
        stream.write(CMD_SIZE_NORMAL)
        stream.write(CMD_BOLD_OFF)
        stream.write(LINE2.toByteArray(ENC))

        // ── Daire & Sakin ───────────────────────────────────────────────
        stream.write(CMD_ALIGN_LEFT)
        stream.write(rightCol("Donem :", donem).toByteArray(ENC))
        stream.write(CMD_BOLD_ON)
        stream.write(rightCol("Daire :", "No $daire / Kat $kat").toByteArray(ENC))
        stream.write(CMD_BOLD_OFF)
        stream.write(rightCol("Sakin :", tr(sakin)).toByteArray(ENC))
        if (telefon.isNotBlank())
            stream.write(rightCol("Tel   :", telefon).toByteArray(ENC))
        stream.write(LINE.toByteArray(ENC))

        // ── Endeksler ───────────────────────────────────────────────────
        stream.write(rightCol("Ilk Endeks :", "$ilkEndeks m3").toByteArray(ENC))
        stream.write(rightCol("Son Endeks :", "$sonEndeks m3").toByteArray(ENC))
        stream.write(CMD_BOLD_ON)
        stream.write(rightCol("Tuketim    :", "$tuketim m3").toByteArray(ENC))
        stream.write(CMD_BOLD_OFF)
        stream.write(LINE.toByteArray(ENC))

        // ── Fatura Kalemleri ────────────────────────────────────────────
        stream.write(rightCol("Su Bedeli            :", "$suBedeli TL").toByteArray(ENC))
        stream.write(rightCol("Atiksu Bedeli        :", "$atiksuBedeli TL").toByteArray(ENC))
        stream.write(rightCol("Kati Atik Top.+Tas.  :", "$katiAtik TL").toByteArray(ENC))
        stream.write(rightCol("CTV                  :", "$ctv TL").toByteArray(ENC))
        stream.write(rightCol("KDV                  :", "$kdv TL").toByteArray(ENC))
        stream.write(LINE2.toByteArray(ENC))

        // ── Toplam ──────────────────────────────────────────────────────
        stream.write(CMD_ALIGN_CENTER)
        stream.write(CMD_SIZE_2H)
        stream.write(CMD_BOLD_ON)
        stream.write("ODENECEK TOPLAM\n".toByteArray(ENC))
        stream.write(CMD_SIZE_2X2)
        stream.write("$toplam TL\n".toByteArray(ENC))
        stream.write(CMD_SIZE_NORMAL)
        stream.write(CMD_BOLD_OFF)
        stream.write(LINE2.toByteArray(ENC))

        // ── Footer ──────────────────────────────────────────────────────
        stream.write(CMD_ALIGN_CENTER)
        stream.write("Tarih: $tarih\n".toByteArray(ENC))
        stream.write(CMD_THANKS_STR.toByteArray(ENC))

        stream.write(CMD_FEED_N)
        stream.write(CMD_CUT_FULL)
        stream.flush()
    }
}
