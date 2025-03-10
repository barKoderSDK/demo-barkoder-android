package com.barkoder.demoscanner.models

import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recentScan_table")
data class RecentScan2(
    var scanDate: String,
    var scanText: String,
    var scanTypeName : String,
    var pictureBitmap : String? = null,
    var documentBitmap : String? = null,
    var signatureBitmap : String? = null,
    var mainBitmap : String? = null,
    var thumbnailBitmap : String? = null,
    var formattedText : String,
    val asHeaderOnly: Boolean = false,
    var checkboxActive : Boolean = false,
    var checkedRecentItem : Boolean = false,
    var scannedTimesInARow : Int = 1,
    var highlighted : Boolean = false,

    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
) {

//    @Expose
//    @SerializedName("recent_scan_extras")
//    val scanExtras = scanResult?.extra?.associate { it.key to it.value }
//    //BKKeyValue can't be serialized because no getters and setters

//    fun scanType(): Barkoder.BarcodeType? {
//        return scanTypeIndex?.let {
//            Barkoder.BarcodeType.valueOf(scanTypeIndex)
//        }
//    }
//
//    fun readString(): String? {
//        return scanText?.let { ScannedResultsUtil.getResultReadString(scanExtras, it) }
//    }
}
