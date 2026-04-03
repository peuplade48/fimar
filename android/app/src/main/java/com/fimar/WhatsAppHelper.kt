package com.fimar

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * WhatsApp mesaj ve görsel gönderim yardımcısı.
 * - sendWithImage : Yazı + JPEG faturayı WhatsApp'a gönderir
 * - sendTextOnly  : Sadece metin gönderir (wa.me linki)
 * - sendPdf       : PDF fatura gönderir
 * - showImagePreview : Fatura görselini diyalog ile önizler
 */
class WhatsAppHelper(private val activity: Activity) {

    private val WA_PACKAGE = "com.whatsapp"

    // ─── Görsel + Metin Gönder ──────────────────────────────────────────
    fun sendWithImage(telefon: String, mesaj: String, bitmap: Bitmap) {
        try {
            val file = saveBitmapToCache(bitmap, "fatura_${System.currentTimeMillis()}.jpg")
            val uri  = getUriForFile(file)

            // WhatsApp doğrudan gönderim (jid ile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type    = "image/jpeg"
                setPackage(WA_PACKAGE)
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, mesaj)
                putExtra("jid", "${normalizePhone(telefon)}@s.whatsapp.net")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            if (isWhatsAppInstalled()) {
                activity.startActivity(intent)
            } else {
                openWaMe(telefon, mesaj)
            }
        } catch (e: Exception) {
            sendTextOnly(telefon, mesaj)
        }
    }

    // ─── Sadece Metin Gönder ─────────────────────────────────────────────
    fun sendTextOnly(telefon: String, mesaj: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage(WA_PACKAGE)
                putExtra(Intent.EXTRA_TEXT, mesaj)
                putExtra("jid", "${normalizePhone(telefon)}@s.whatsapp.net")
            }
            if (isWhatsAppInstalled()) {
                activity.startActivity(intent)
            } else {
                openWaMe(telefon, mesaj)
            }
        } catch (e: Exception) {
            openWaMe(telefon, mesaj)
        }
    }

    // ─── PDF Gönder ──────────────────────────────────────────────────────
    fun sendPdf(telefon: String, mesaj: String, pdfFile: File) {
        try {
            val uri = getUriForFile(pdfFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type    = "application/pdf"
                setPackage(WA_PACKAGE)
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, mesaj)
                putExtra("jid", "${normalizePhone(telefon)}@s.whatsapp.net")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            if (isWhatsAppInstalled()) activity.startActivity(intent)
            else Toast.makeText(activity, "WhatsApp yüklü değil", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(activity, "Gönderim hatası: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ─── Fatura Görsel Önizlemesi ─────────────────────────────────────────
    fun showImagePreview(bitmap: Bitmap, faturaId: Int) {
        val iv = ImageView(activity).apply {
            setImageBitmap(bitmap)
            adjustViewBounds = true
            val pad = dpToPx(12)
            setPadding(pad, pad, pad, 0)
        }
        val scroll = ScrollView(activity).apply { addView(iv) }

        AlertDialog.Builder(activity)
            .setTitle("📄 Fatura Görseli #$faturaId")
            .setView(scroll)
            .setPositiveButton("📤 Paylaş") { _, _ ->
                shareImage(bitmap)
            }
            .setNeutralButton("💾 Kaydet") { _, _ ->
                saveToDownloads(bitmap, "fatura_$faturaId.jpg")
            }
            .setNegativeButton("Kapat", null)
            .show()
    }

    // ─── Paylaş (genel) ──────────────────────────────────────────────────
    private fun shareImage(bitmap: Bitmap) {
        val file = saveBitmapToCache(bitmap, "fatura_share.jpg")
        val uri  = getUriForFile(file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.startActivity(Intent.createChooser(intent, "Faturayı Paylaş"))
    }

    // ─── Downloads'a Kaydet ───────────────────────────────────────────────
    private fun saveToDownloads(bitmap: Bitmap, filename: String) {
        try {
            val dir = android.os.Environment
                .getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            dir.mkdirs()
            val file = File(dir, filename)
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
            Toast.makeText(activity, "İndirildi: $filename", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(activity, "Kayıt hatası: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ─── Yardımcı Metodlar ────────────────────────────────────────────────
    private fun saveBitmapToCache(bitmap: Bitmap, name: String): File {
        val file = File(activity.cacheDir, name)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        return file
    }

    private fun getUriForFile(file: File): Uri =
        FileProvider.getUriForFile(activity, "${activity.packageName}.provider", file)

    private fun openWaMe(telefon: String, mesaj: String) {
        val tel = normalizePhone(telefon)
        val uri = Uri.parse("https://wa.me/$tel?text=${Uri.encode(mesaj)}")
        activity.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    private fun isWhatsAppInstalled(): Boolean = try {
        activity.packageManager.getPackageInfo(WA_PACKAGE, 0)
        true
    } catch (_: Exception) { false }

    /**
     * Telefonu uluslararası formata çevirir
     * 05xx... → 905xx...
     * +90... → 90...
     */
    fun normalizePhone(raw: String): String {
        var tel = raw.replace(Regex("[^0-9]"), "")
        if (tel.startsWith("0")) tel = "90" + tel.substring(1)
        if (!tel.startsWith("90") && tel.length == 10) tel = "90$tel"
        return tel
    }

    private fun dpToPx(dp: Int): Int =
        (dp * activity.resources.displayMetrics.density).toInt()
}
