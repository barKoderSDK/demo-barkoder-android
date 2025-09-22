package com.barkoder.demoscanner.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barkoder.demoscanner.api.RetrofitIInstance.api
import com.barkoder.demoscanner.models.BarcodeScanedData
import com.barkoder.demoscanner.repositories.BarcodeDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Response



class BarcodeDataViewModel (private val repository: BarcodeDataRepository) : ViewModel() {

    var barcodeDataResponse: MutableLiveData<Response<BarcodeScanedData>> = MutableLiveData()


//    fun createPost(endPoint: String, post: BarcodeScanedData) {
//        viewModelScope.launch {
//            val response = repository.createPost(endPoint,post)
//                barcodeDataResponse.value = response
//
//        }
//    }


    fun createPost(
        url: String,
        payload: BarcodeScanedData,
        onResult: (success: Boolean, code: Int?, message: String?) -> Unit
    ) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val res = api.createPost(url, payload)
            if (res.isSuccessful) {
                val bodyMessage = res.message() // or res.body()?.toString() if it's not empty
                Log.d("Webhook", "Success: $bodyMessage")
                onResult(true, res.code(), bodyMessage)
            } else {
                val errorMsg = res.message()
                Log.w("Webhook", "Error ${res.code()}: $errorMsg")
                onResult(false, res.code(), errorMsg)
            }
        } catch (t: Throwable) {
            Log.e("Webhook", "Exception: ${t.message}", t)
            onResult(false, null, t.message)
        }
    }
}
