package com.barkoder.demoscanner.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.barkoder.demoscanner.models.RecentDatabase
import com.barkoder.demoscanner.models.RecentScan
import com.barkoder.demoscanner.models.RecentScan2
import com.barkoder.demoscanner.repositories.RecentScansRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RecentScanViewModel(application: Application) : AndroidViewModel(application) {
    val readAllScans: LiveData<List<RecentScan2>>

    // LiveData for the paginated dataset
    val recentScansLiveData: LiveData<PagingData<RecentScan2>>

    val repository: RecentScansRepository

    init {
        val recentDao = RecentDatabase.getDatabase(application).recentDaio()
        repository = RecentScansRepository(recentDao)

        // Full list for operations like forEach
        readAllScans = repository.getAllScans()  // LiveData<List<RecentScan2>>

        // Paginated list for RecyclerView
        recentScansLiveData = repository.getPaginatedScans()
            .cachedIn(viewModelScope)
            .asLiveData()  // LiveData<PagingData<RecentScan2>>
    }

    fun addRecentScan(recent : RecentScan2) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addRecentScan(recent)
        }
    }

    fun updateRecentScan(recent: RecentScan2) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateRecentScan(recent)
        }
    }

    fun getRecentScanById(id: Int): LiveData<RecentScan2?> {
        return liveData {
            emit(repository.getRecentScanById(id))
        }
    }

    fun deleteRecentScan(recentScan: RecentScan2) {
        viewModelScope.launch {
            repository.deleteRecentScan(recentScan)
        }
    }

    fun deleteAllRecentScans() = viewModelScope.launch {
        repository.deleteAllRecentScans()
    }

}