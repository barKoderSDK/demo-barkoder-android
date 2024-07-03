package com.barkoder.demoscanner.repositories

import com.barkoder.demoscanner.api.RetrofitIInstance
import com.barkoder.demoscanner.models.BarcodeScanedData
import retrofit2.Response

class BarcodeDataRepository {

    suspend fun createPost(endPoint: String, post: BarcodeScanedData) : Response<BarcodeScanedData> {
        return RetrofitIInstance.api.createPost(endPoint, post)
    }
}