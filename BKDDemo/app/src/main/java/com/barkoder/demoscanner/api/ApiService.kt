package com.barkoder.demoscanner.api

import com.barkoder.demoscanner.models.BarcodeScanedData
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface ApiService {

    @POST
    suspend fun createPost(@Url endPoint: String, @Body post: BarcodeScanedData): Response<BarcodeScanedData>
}

