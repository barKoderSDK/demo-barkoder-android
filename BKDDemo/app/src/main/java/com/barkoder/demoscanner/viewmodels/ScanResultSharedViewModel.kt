package com.barkoder.demoscanner.viewmodels

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.barkoder.demoscanner.models.SessionScan

/**
 * SharedViewModel to pass large data between Activities and ResultBottomDialogFragment
 * This prevents TransactionTooLargeException by avoiding Fragment arguments for large data
 */
class ScanResultSharedViewModel : ViewModel() {
    
    var currentImage: Bitmap? = null
    var sessionScans: MutableList<SessionScan> = mutableListOf()
    var resultsList: MutableList<String> = mutableListOf()
    var typesList: MutableList<String> = mutableListOf()
    var datesList: MutableList<String> = mutableListOf()
    var resultsSize: String? = null
    
    fun setData(
        image: Bitmap?,
        sessions: MutableList<SessionScan>,
        results: MutableList<String>,
        types: MutableList<String>,
        dates: MutableList<String>,
        size: String?
    ) {
        this.currentImage = image
        this.sessionScans = sessions
        this.resultsList = results
        this.typesList = types
        this.datesList = dates
        this.resultsSize = size
    }
    
    fun clearData() {
        currentImage = null
        sessionScans.clear()
        resultsList.clear()
        typesList.clear()
        datesList.clear()
        resultsSize = null
    }
}
