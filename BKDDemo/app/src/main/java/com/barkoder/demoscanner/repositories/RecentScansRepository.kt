package com.barkoder.demoscanner.repositories

import androidx.lifecycle.LiveData
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.barkoder.demoscanner.models.RecentDao
import com.barkoder.demoscanner.models.RecentScan
import com.barkoder.demoscanner.models.RecentScan2
import kotlinx.coroutines.flow.Flow

class RecentScansRepository (private val recentDao : RecentDao){

    // Full dataset from Room as LiveData
    fun getAllScans(): LiveData<List<RecentScan2>> {
        return recentDao.getAllScans()
    }

    // Paginated dataset from Room using PagingSource
    fun getPaginatedScans(): Flow<PagingData<RecentScan2>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,  // Load 20 items per page
                enablePlaceholders = false
            ),
            pagingSourceFactory = { recentDao.getRecentScans() }
        ).flow
    }
    suspend fun addRecentScan(recent: RecentScan2){
        recentDao.addRecentScan(recent)
    }

    suspend fun updateRecentScan(recent: RecentScan2){
        recentDao.updateRecentScan(recent)
    }

    suspend fun getRecentScanById(id: Int): RecentScan2? {
        return recentDao.getRecentScanById(id)
    }

    suspend fun deleteRecentScan(recentScan: RecentScan2) {
        recentDao.deleteRecentScan(recentScan)
    }

    suspend fun deleteAllRecentScans() {
        recentDao.deleteAllRecentScans()
    }
}