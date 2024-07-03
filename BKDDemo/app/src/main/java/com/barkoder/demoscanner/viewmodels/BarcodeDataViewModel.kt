package com.barkoder.demoscanner.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barkoder.demoscanner.models.BarcodeScanedData
import com.barkoder.demoscanner.repositories.BarcodeDataRepository
import kotlinx.coroutines.launch
import retrofit2.Response



class BarcodeDataViewModel (private val repository: BarcodeDataRepository) : ViewModel() {

    var barcodeDataResponse : MutableLiveData<Response<BarcodeScanedData>> = MutableLiveData()


    fun createPost(endPoint: String, post: BarcodeScanedData) {
        viewModelScope.launch {
            val response = repository.createPost(endPoint,post)
                barcodeDataResponse.value = response

        }
    }

}
