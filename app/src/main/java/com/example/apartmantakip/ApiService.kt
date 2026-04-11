package com.example.apartmantakip

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("api.php")
    suspend fun getInvoiceList(
        @Query("action") action: String = "fatura_listesi",
        @Query("donem") donem: String
    ): InvoiceListResponse

    @GET("api.php")
    suspend fun getInvoiceDetail(
        @Query("action") action: String = "fatura_detay",
        @Query("id") id: Int
    ): InvoiceDetailResponse
}

data class InvoiceListResponse(
    @SerializedName("faturalar") val faturalar: List<InvoiceSummary>
)

data class InvoiceSummary(
    @SerializedName("id") val id: Int,
    @SerializedName("daire_no") val daireNo: String,
    @SerializedName("blok") val blok: String?,
    @SerializedName("ad_soyad") val adSoyad: String?,
    @SerializedName("toplam") val toplam: Double,
    @SerializedName("donem") val donem: String
)

data class InvoiceDetailResponse(
    @SerializedName("fatura") val fatura: InvoiceDetail
)

data class InvoiceDetail(
    val id: Int,
    @SerializedName("daire_no") val daireNo: String,
    @SerializedName("blok") val blok: String?,
    @SerializedName("ad_soyad") val adSoyad: String?,
    @SerializedName("telefon") val telefon: String?,
    @SerializedName("donem") val donem: String,
    @SerializedName("ilk_endeks") val ilkEndeks: Double,
    @SerializedName("son_endeks") val sonEndeks: Double,
    @SerializedName("tuketim") val tuketim: Double,
    @SerializedName("su_bedeli") val suBedeli: Double,
    @SerializedName("atiksu_bedeli") val atiksuBedeli: Double,
    @SerializedName("kati_atik") val katiAtik: Double,
    @SerializedName("bertaraf") val bertaraf: Double,
    @SerializedName("ctv") val ctv: Double,
    @SerializedName("aidat") val aidat: Double,
    @SerializedName("kdv") val kdv: Double,
    @SerializedName("toplam") val toplam: Double,
    @SerializedName("ekstra_gider_toplam") val ekstraGiderToplam: Double,
    @SerializedName("su_tuketim_detay") val suTuketimDetay: String?, // JSON string
    @SerializedName("ekstra_gider_detay") val ekstraGiderDetay: String? // JSON string
)
