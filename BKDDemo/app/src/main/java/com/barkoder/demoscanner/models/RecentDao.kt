package com.barkoder.demoscanner.models

import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface RecentDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addRecentScan(recent: RecentScan2)

    // Full list of RecentScan2 for operations
    @Query("SELECT * FROM recentScan_table ORDER BY id ASC")
    fun getAllScans(): LiveData<List<RecentScan2>>

    // Paginated list using PagingSource
    @Query("SELECT * FROM recentScan_table ORDER BY id ASC")
    fun getRecentScans(): PagingSource<Int, RecentScan2>

    @Update
    suspend fun updateRecentScan(recent: RecentScan2)

    @Query("SELECT * FROM recentScan_table WHERE id = :id")
    suspend fun getRecentScanById(id: Int): RecentScan2?

    @Delete
    suspend fun deleteRecentScan(recentScan: RecentScan2)

    @Query("DELETE FROM recentScan_table")
    suspend fun deleteAllRecentScans()

}