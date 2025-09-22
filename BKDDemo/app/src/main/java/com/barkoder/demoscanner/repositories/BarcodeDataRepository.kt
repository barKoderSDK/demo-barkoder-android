package com.barkoder.demoscanner.repositories

import com.barkoder.demoscanner.api.RetrofitIInstance
import com.barkoder.demoscanner.models.BarcodeScanedData
import retrofit2.Response

class BarcodeDataRepository {

    suspend fun createPost(endPoint: String, post: BarcodeScanedData): retrofit2.Response<Void> {
        return RetrofitIInstance.api.createPost(endPoint, post)
    }
}