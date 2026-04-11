package com.example.apartmantakip

data class InvoiceDetail(
    val blok: String? = null,
    val daireNo: String? = null,
    val adSoyad: String? = null,
    val donem: String? = null,
    val ilkEndeks: Double = 0.0,
    val sonEndeks: Double = 0.0,
    val tuketim: Double = 0.0,
    val suBedeli: Double = 0.0,
    val atiksuBedeli: Double = 0.0,
    val ctv: Double = 0.0,
    val katiAtik: Double = 0.0,
    val bertaraf: Double = 0.0,
    val aidat: Double = 0.0,
    val ekstraGiderToplam: Double = 0.0,
    val kdv: Double = 0.0,
    val toplam: Double = 0.0
)
