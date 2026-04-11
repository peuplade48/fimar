package com.example.apartmantakip

import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("api.php")
    suspend fun login(
        @Query("action") action: String = "login",
        @Body body: Map<String, String>
    ): Response<ApiResponse<User>>

    @POST("api.php")
    suspend fun loginVerify(
        @Query("action") action: String = "login_verify",
        @Body body: Map<String, Any?>
    ): Response<ApiResponse<Void>>

    @GET("api.php")
    suspend fun getDashboard(
        @Query("action") action: String = "dashboard",
        @Query("donem") donem: String? = null
    ): Response<ApiResponse<DashboardStats>>

    @GET("api.php")
    suspend fun getUnitList(
        @Query("action") action: String = "daire_listesi",
        @Query("donem") donem: String? = null
    ): Response<ApiResponse<List<BuildingUnit>>>

    @GET("api.php")
    suspend fun getResidentList(
        @Query("action") action: String = "sakin_listesi",
        @Query("daire_id") daireId: Int
    ): Response<ApiResponse<Resident>>

    @POST("api.php")
    suspend fun saveResident(
        @Query("action") action: String = "sakin_kaydet",
        @Body body: Map<String, Any>
    ): Response<ApiResponse<Int>>

    @POST("api.php")
    suspend fun deleteResident(
        @Query("action") action: String = "sakin_sil",
        @Body body: Map<String, Int>
    ): Response<ApiResponse<Void>>

    @GET("api.php")
    suspend fun getPriceList(
        @Query("action") action: String = "fiyat_listesi"
    ): Response<ApiResponse<Price>>

    @POST("api.php")
    suspend fun savePrice(
        @Query("action") action: String = "fiyat_kaydet",
        @Body body: Map<String, Any>
    ): Response<ApiResponse<Void>>

    @POST("api.php")
    suspend fun deletePrice(
        @Query("action") action: String = "fiyat_sil",
        @Body body: Map<String, Int>
    ): Response<ApiResponse<Void>>

    @POST("api.php")
    suspend fun saveReading(
        @Query("action") action: String = "okuma_kaydet",
        @Body body: Map<String, Any>
    ): Response<ApiResponse<Double>>

    @POST("api.php")
    suspend fun saveBatchReadings(
        @Query("action") action: String = "toplu_okuma_kaydet",
        @Body body: Map<String, Any>
    ): Response<ApiResponse<Void>>

    @POST("api.php")
    suspend fun createInvoice(
        @Query("action") action: String = "fatura_olustur",
        @Body body: Map<String, Any>
    ): Response<ApiResponse<Int>>

    @GET("api.php")
    suspend fun createBatchInvoices(
        @Query("action") action: String = "toplu_fatura_olustur",
        @Query("donem") donem: String
    ): Response<ApiResponse<Int>>

    @GET("api.php")
    suspend fun getInvoiceList(
        @Query("action") action: String = "fatura_listesi",
        @Query("donem") donem: String? = null
    ): Response<ApiResponse<Invoice>>

    @GET("api.php")
    suspend fun getInvoiceDetail(
        @Query("action") action: String = "fatura_detay",
        @Query("id") id: Int
    ): Response<ApiResponse<Invoice>>

    @POST("api.php")
    suspend fun updateInvoice(
        @Query("action") action: String = "fatura_guncelle",
        @Body body: Map<String, Any>
    ): Response<ApiResponse<Void>>

    @POST("api.php")
    suspend fun markAsSent(
        @Query("action") action: String = "gonderildi_isaretle",
        @Body body: Map<String, Int>
    ): Response<ApiResponse<Void>>

    @POST("api.php")
    suspend fun markAsPaid(
        @Query("action") action: String = "odendi_isaretle",
        @Body body: Map<String, Int>
    ): Response<ApiResponse<Void>>

    @GET("api.php")
    suspend fun getSettings(
        @Query("action") action: String = "ayarlar_getir"
    ): Response<ApiResponse<Map<String, Any>>>

    @POST("api.php")
    suspend fun saveSettings(
        @Query("action") action: String = "ayarlar_kaydet",
        @Body body: Map<String, String?>
    ): Response<ApiResponse<Void>>

    @POST("api.php")
    suspend fun createUnits(
        @Query("action") action: String = "daireleri_olustur",
        @Body body: Map<String, Any>
    ): Response<ApiResponse<Void>>

    @GET("api.php")
    suspend fun transferToCommonMeter(
        @Query("action") action: String = "ortak_sayaca_aktar",
        @Query("donem") donem: String? = null
    ): Response<ApiResponse<Void>>

    @POST("api.php")
    suspend fun logout(
        @Query("action") action: String = "logout"
    ): Response<ApiResponse<Void>>
}
