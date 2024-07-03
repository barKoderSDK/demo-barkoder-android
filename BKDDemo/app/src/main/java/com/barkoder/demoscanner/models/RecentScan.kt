package com.barkoder.demoscanner.models

import androidx.annotation.Keep
import com.barkoder.Barkoder
import com.barkoder.demoscanner.utils.ScannedResultsUtil
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Keep
class RecentScan(
    @Expose
    @SerializedName("recent_scan_date") val scanDate: String,
    scanResult: Barkoder.Result? = null,
    val asHeaderOnly: Boolean = false
) {
    @Expose
    @SerializedName("recent_scan_type")
    val scanTypeIndex = scanResult?.barcodeType?.ordinal

    @Expose
    @SerializedName("recent_scan_type_name")
    val scanTypeName = scanResult?.barcodeTypeName

    @Expose
    @SerializedName("recent_scan_text")
    val scanText = scanResult?.textualData

    @Expose
    @SerializedName("recent_scan_extras")
    val scanExtras = scanResult?.extra?.associate { it.key to it.value }
    //BKKeyValue can't be serialized because no getters and setters

    fun scanType(): Barkoder.BarcodeType? {
        return scanTypeIndex?.let {
            Barkoder.BarcodeType.valueOf(scanTypeIndex)
        }
    }

    fun readString(): String? {
        return scanText?.let { ScannedResultsUtil.getResultReadString(scanExtras, it) }
    }
}
