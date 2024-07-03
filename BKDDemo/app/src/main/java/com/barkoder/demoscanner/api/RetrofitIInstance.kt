package com.barkoder.demoscanner.api

import android.content.Context
import com.barkoder.demoscanner.utils.Constants
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitIInstance {

    private var retrofit: Retrofit? = null

    fun rebuild(baseUrl: String) {
        retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: ApiService by lazy {
        retrofit?.create(ApiService::class.java) ?: throw UninitializedPropertyAccessException("Retrofit is not initialized")
    }


}