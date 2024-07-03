package com.barkoder.demoscanner.models

data class BarcodeScanedData(
    val security_data: String,
    val security_hash: String,
    val data: ArrayList<Map<String, String>>
)