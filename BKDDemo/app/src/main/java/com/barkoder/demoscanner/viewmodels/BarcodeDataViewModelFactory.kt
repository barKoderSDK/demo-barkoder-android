package com.barkoder.demoscanner.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.barkoder.demoscanner.repositories.BarcodeDataRepository

class BarcodeDataViewModelFactory (private val repository: BarcodeDataRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return BarcodeDataViewModel(repository) as T
    }
}