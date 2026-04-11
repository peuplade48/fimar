package com.example.apartmantakip

import com.google.gson.annotations.SerializedName

data class Building(
    val id: Int,
    val ad: String,
    val adres: String?,
    @SerializedName("db_name") val dbName: String?,
    @SerializedName("wa_sablon") val waSablon: String?,
    val aktif: Int
)

data class User(
    val id: Int,
    val username: String,
    val role: String,
    @SerializedName("bina_id") val binaId: Int?,
    @SerializedName("ad_soyad") val adSoyad: String?,
    val email: String?,
    val telefon: String?,
    @SerializedName("apartman_adi") val apartmanAdi: String?,
    @SerializedName("apartman_adresi") val apartmanAdresi: String?,
    val aktif: Int
)

data class BuildingUnit(
    val id: Int,
    @SerializedName("bina_id") val binaId: Int,
    val blok: String?,
    @SerializedName("daire_no") val daireNo: String,
    val kat: String,
    val aciklama: String?,
    // Joined fields
    @SerializedName("ad_soyad") val adSoyad: String? = null,
    val telefon: String? = null,
    @SerializedName("ilk_endeks") val ilkEndeks: Double? = null,
    @SerializedName("son_endeks") val sonEndeks: Double? = null,
    @SerializedName("okuma_tarihi") val okumaTarihi: String? = null,
    val toplam: Double? = null,
    val odendi: Int? = null,
    val gonderildi: Int? = null,
    @SerializedName("fatura_id") val faturaId: Int? = null
)

data class Resident(
    val id: Int,
    @SerializedName("daire_id") val daireId: Int,
    @SerializedName("ad_soyad") val adSoyad: String,
    val telefon: String,
    val aktif: Int,
    @SerializedName("kayit_tarihi") val kayitTarihi: String
)

data class Price(
    val id: Int,
    @SerializedName("bina_id") val binaId: Int,
    val donem: String,
    @SerializedName("su_kademeler") val suKademeler: String?, // JSON string
    @SerializedName("ekstra_giderler") val ekstraGiderler: String?, // JSON string
    @SerializedName("atiksu_katsayi") val atiksuKatsayi: Double?,
    @SerializedName("kati_atik") val katiAtik: Double,
    val bertaraf: Double,
    val ctv: Double,
    val aidat: Double,
    @SerializedName("su_kdv_orani") val suKdvOrani: Double,
    @SerializedName("atiksu_kdv_orani") val atiksuKdvOrani: Double,
    @SerializedName("kati_atik_kdv_orani") val katiAtikKdvOrani: Double,
    @SerializedName("bertaraf_kdv_orani") val bertarafKdvOrani: Double,
    @SerializedName("ana_fatura_tutar") val anaFaturaTutar: Double,
    @SerializedName("ana_fatura_tuketim") val anaFaturaTuketim: Double,
    @SerializedName("ana_ilk_endeks") val anaIlkEndeks: Double,
    @SerializedName("ana_son_endeks") val anaSonEndeks: Double,
    @SerializedName("ana_ilk_okuma_tarihi") val anaIlkOkumaTarihi: String?,
    @SerializedName("ana_son_okuma_tarihi") val anaSonOkumaTarihi: String?,
    @SerializedName("ana_fatura_detay") val anaFaturaDetay: String?, // JSON string
    @SerializedName("ana_katsayi") val anaKatsayi: Double,
    // Calculated/Joined fields
    @SerializedName("dagitilan_tuketim") val dagitilanTuketim: Double? = null,
    @SerializedName("dagitilan_tutar") val dagitilanTutar: Double? = null,
    @SerializedName("fark_tuketim") val farkTuketim: Double? = null,
    @SerializedName("fark_tutar") val farkTutar: Double? = null
)

data class MeterReading(
    val id: Int,
    @SerializedName("daire_id") val daireId: Int,
    val donem: String,
    @SerializedName("ilk_endeks") val ilkEndeks: Double,
    @SerializedName("son_endeks") val sonEndeks: Double?,
    @SerializedName("okuma_tarihi") val okumaTarihi: String?,
    val notlar: String?
)

data class Invoice(
    val id: Int,
    @SerializedName("daire_id") val daireId: Int,
    val donem: String,
    @SerializedName("ilk_endeks") val ilkEndeks: Double,
    @SerializedName("son_endeks") val sonEndeks: Double,
    val tuketim: Double,
    @SerializedName("su_tuketim_detay") val suTuketimDetay: String?, // JSON string
    @SerializedName("ekstra_gider_detay") val ekstraGiderDetay: String?, // JSON string
    @SerializedName("ekstra_gider_toplam") val ekstraGiderToplam: Double,
    @SerializedName("su_bedeli") val suBedeli: Double,
    @SerializedName("atiksu_bedeli") val atiksuBedeli: Double,
    @SerializedName("kati_atik") val katiAtik: Double,
    val bertaraf: Double,
    val ctv: Double,
    val aidat: Double,
    val kdv: Double,
    val toplam: Double,
    val odendi: Int,
    val gonderildi: Int,
    @SerializedName("gorsel_yolu") val gorselYolu: String?,
    @SerializedName("olusturma_tarihi") val olusturmaTarihi: String,
    // Joined fields
    val blok: String? = null,
    @SerializedName("daire_no") val daireNo: String? = null,
    val kat: String? = null,
    @SerializedName("ad_soyad") val adSoyad: String? = null,
    val telefon: String? = null
)

data class SuKademe(
    val limit: Double?,
    val fiyat: Double
)

data class EkstraGider(
    val ad: String,
    val tutar: Double
)

data class SuTuketimDetay(
    val kademe: Int,
    val limit: Double?,
    val tuketim: Double,
    val fiyat: Double,
    val tutar: Double
)

data class ApiResponse<T>(
    val basarili: Boolean = false,
    val hata: String? = null,
    val mesaj: String? = null,
    @SerializedName("auth_required") val authRequired: Boolean = false,
    @SerializedName("user_id") val userId: Int? = null,
    @SerializedName("iki_adimli") val ikiAdimli: Boolean = false,
    @SerializedName("email_onay_gerekli") val emailOnayGerekli: Boolean = false,
    val data: T? = null,
    // Special cases for some endpoints
    val daireler: List<BuildingUnit>? = null,
    val sakinler: List<Resident>? = null,
    val fiyatlar: List<Price>? = null,
    val faturalar: List<Invoice>? = null,
    val fatura: Invoice? = null,
    val istatistik: DashboardStats? = null
)

data class DashboardStats(
    @SerializedName("toplam_fatura") val toplamFatura: Int,
    @SerializedName("toplam_tutar") val toplamTutar: Double,
    @SerializedName("toplam_aidat") val toplamAidat: Double,
    @SerializedName("toplam_ekstra") val toplamEkstra: Double,
    @SerializedName("toplam_su_bedeli") val toplamSuBedeli: Double,
    @SerializedName("toplam_atiksu_bedeli") val toplamAtiksuBedeli: Double,
    @SerializedName("toplam_kati_atik") val toplamKatiAtik: Double,
    @SerializedName("toplam_bertaraf") val toplamBertaraf: Double,
    @SerializedName("toplam_ctv") val toplamCtv: Double,
    @SerializedName("toplam_kdv") val toplamKdv: Double,
    @SerializedName("odenen_adet") val odenenAdet: Int,
    @SerializedName("gonderilen_adet") val gonderilenAdet: Int,
    val okunan: Int,
    val toplam: Int,
    @SerializedName("normal_daire") val normalDaire: Int,
    @SerializedName("ortak_sayac") val ortakSayac: Int,
    @SerializedName("ana_fatura_tutar") val anaFaturaTutar: Double,
    @SerializedName("ana_fatura_tuketim") val anaFaturaTuketim: Double,
    @SerializedName("toplam_tuketim") val toplamTuketim: Double,
    @SerializedName("fark_tutar") val farkTutar: Double,
    @SerializedName("fark_tuketim") val farkTuketim: Double,
    @SerializedName("kademeye_girenler") val kademeyeGirenler: List<KademeyeGirenDaire>? = null
)

data class KademeyeGirenDaire(
    val daire: String,
    val m3: Double
)
