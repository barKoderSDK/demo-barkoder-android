package com.barkoder.demoscanner.models

import java.io.Serializable

data class SessionScan (
    val scanDate: String,
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

) : Serializable