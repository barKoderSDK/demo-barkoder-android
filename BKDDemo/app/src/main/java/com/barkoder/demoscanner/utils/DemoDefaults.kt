package com.barkoder.demoscanner.utils

import com.barkoder.Barkoder
import com.barkoder.demoscanner.customcontrols.CustomRangePreference
import com.barkoder.enums.BarkoderARMode
import com.barkoder.enums.BarkoderResolution

object DemoDefaults {

    const val AUTO_START_SCAN_DEFAULT = false
    val DECODING_SPEED_DEFAULT = Barkoder.DecodingSpeed.Normal
    val DECODING_SPEED_DEFAULT_TEMPLATE = Barkoder.DecodingSpeed.Slow
    val DECODING_SPEED_DEFAULT_GALLERY_RIGORIUS = Barkoder.DecodingSpeed.Rigorous
    val BARKODER_RESOLUTION_DEFAULT = BarkoderResolution.HD
    val BARKODER_RESOLUTION_DEFAULT_TEMPLATES_VIN_DPM = BarkoderResolution.FHD
    val BARKODER_AR_MODE = BarkoderARMode.OFF
    const val CLOSE_SESSION_ON_RESULT_DEFAULT = true
    const val ENABLE_LOCATION_IN_PREVIEW_DEFAULT = true
    const val ALLOW_PINCH_TO_ZOOM_DEFAULT = false
    const val ENABLE_ROI_DEFAULT = false
    const val ROI_LEFT_DEFAULT_VALUE = 3f
    const val ROI_TOP_DEFAULT_VALUE = 20f
    const val ROI_WIDTH_DEFAULT_VALUE = 94f
    const val ROI_HEIGHT_DEFAULT_VALUE = 60f
    const val BEEP_ON_SUCCESS_DEFAULT = true
    const val VIBRATE_ON_SUCCESS_DEFAULT = false
    const val CONTINUOUS_MODE_DEFAULT = false
    const val AUTOMATIC_SHOWBOTTOMSHEET_DEFAULT = true
    const val ENABLED_WEBHOOK_DEFAULT = true
    const val ENABLED_SEARCHWEB_DEFAULT = true
    const val CONTINUOUS_TRESHOLD_DEFAULT = "5"
    const val DEBLUR_UPC_EAN_DEFAULT = false
    const val MISSHAPED_1D_DEFAULT = false
    const val DPM_MODE = true
    const val BIGGER_VIEWFINDER_DEFAULT = false

    const val SYMBOLOGY_AZTEC_DEFAULT = true
    const val SYMBOLOGY_MAXICODE_DEFAULT = true
    const val SYMBOLOGY_AZTEC_COMPACT_DEFAULT = true
    const val SYMBOLOGY_QR_DEFAULT = true
    const val SYMBOLOGY_QR_MICRO_DEFAULT = true
    const val SYMBOLOGY_C11_DEFAULT = true
    const val SYMBOLOGY_C11_MIN_DEFAULT = CustomRangePreference.MIN_ALLOWED_VALUE
    const val SYMBOLOGY_C11_MAX_DEFAULT = CustomRangePreference.MAX_ALLOWED_VALUE
    val SYMBOLOGY_C11_CHK_DEFAULT = Barkoder.Code11ChecksumType.Disabled
    const val SYMBOLOGY_C39_DEFAULT = true
    const val SYMBOLOGY_C39_MIN_DEFAULT = CustomRangePreference.MIN_ALLOWED_VALUE
    const val SYMBOLOGY_C39_MAX_DEFAULT = CustomRangePreference.MAX_ALLOWED_VALUE
    val SYMBOLOGY_C39_CHK_DEFAULT = Barkoder.Code39ChecksumType.Disabled
    const val SYMBOLOGY_C93_DEFAULT = true
    const val SYMBOLOGY_C93_MIN_DEFAULT = CustomRangePreference.MIN_ALLOWED_VALUE
    const val SYMBOLOGY_C93_MAX_DEFAULT = CustomRangePreference.MAX_ALLOWED_VALUE
    const val SYMBOLOGY_C128_DEFAULT = true
    const val SYMBOLOGY_C128_MIN_DEFAULT = CustomRangePreference.MIN_ALLOWED_VALUE
    const val SYMBOLOGY_C128_MAX_DEFAULT = CustomRangePreference.MAX_ALLOWED_VALUE
    const val SYMBOLOGY_CODABAR_DEFAULT = true
    const val SYMBOLOGY_CODABAR_MIN_DEFAULT = 4
    const val SYMBOLOGY_CODABAR_MAX_DEFAULT = CustomRangePreference.MAX_ALLOWED_VALUE
    const val SYMBOLOGY_MSI_DEFAULT = true
    const val SYMBOLOGY_MSI_MIN_DEFAULT = 5
    const val SYMBOLOGY_MSI_MAX_DEFAULT = CustomRangePreference.MAX_ALLOWED_VALUE
    val SYMBOLOGY_MSI_CHK_DEFAULT = Barkoder.MsiChecksumType.Mod10
    const val SYMBOLOGY_UPCA_DEFAULT = true
    const val SYMBOLOGY_UPCE_DEFAULT = true
    const val SYMBOLOGY_UPCE_EXPAND_DEFAULT = false
    const val SYMBOLOGY_UPCE1_DEFAULT = false
    const val SYMBOLOGY_UPCE1_EXPAND_DEFAULT = false
    const val SYMBOLOGY_EAN13_DEFAULT = true
    const val SYMBOLOGY_EAN8_DEFAULT = true
    const val SYMBOLOGY_PDF417_DEFAULT = true
    const val SYMBOLOGY_PDF417_MICRO_DEFAULT = true
    const val SYMBOLOGY_DM_DEFAULT = true
    const val SYMBOLOGY_C25_DEFAULT = true
    const val SYMBOLOGY_C25_MIN_DEFAULT = CustomRangePreference.MIN_ALLOWED_VALUE
    const val SYMBOLOGY_C25_MAX_DEFAULT = CustomRangePreference.MAX_ALLOWED_VALUE
    val SYMBOLOGY_C25_CHK_DEFAULT = Barkoder.StandardChecksumType.Disabled
    const val SYMBOLOGY_I2O5_DEFAULT = true
    const val SYMBOLOGY_I2O5_MIN_DEFAULT = CustomRangePreference.MIN_ALLOWED_VALUE
    const val SYMBOLOGY_I2O5_MAX_DEFAULT = CustomRangePreference.MAX_ALLOWED_VALUE
    val SYMBOLOGY_I2O5_CHK_DEFAULT = Barkoder.StandardChecksumType.Disabled
    const val SYMBOLOGY_ITF14_DEFAULT = true
    const val SYMBOLOGY_IATA25_DEFAULT = true
    const val SYMBOLOGY_IATA25_MIN_DEFAULT = CustomRangePreference.MIN_ALLOWED_VALUE
    const val SYMBOLOGY_IATA25_MAX_DEFAULT = CustomRangePreference.MAX_ALLOWED_VALUE
    val SYMBOLOGY_IATA25_CHK_DEFAULT = Barkoder.StandardChecksumType.Disabled
    const val SYMBOLOGY_MATRIX25_DEFAULT = true
    const val SYMBOLOGY_MATRIX25_MIN_DEFAULT = CustomRangePreference.MIN_ALLOWED_VALUE
    const val SYMBOLOGY_MATRIX25_MAX_DEFAULT = CustomRangePreference.MAX_ALLOWED_VALUE
    val SYMBOLOGY_MATRIX25_CHK_DEFAULT = Barkoder.StandardChecksumType.Disabled
    const val SYMBOLOGY_DATALOGIC25_DEFAULT = false
    const val SYMBOLOGY_DATALOGIC25_MIN_DEFAULT = CustomRangePreference.MIN_ALLOWED_VALUE
    const val SYMBOLOGY_DATALOGIC25_MAX_DEFAULT = CustomRangePreference.MAX_ALLOWED_VALUE
    val SYMBOLOGY_DATALOGIC25_CHK_DEFAULT = Barkoder.StandardChecksumType.Disabled
    const val SYMBOLOGY_COOP25_DEFAULT = true
    const val SYMBOLOGY_COOP25_MIN_DEFAULT = CustomRangePreference.MIN_ALLOWED_VALUE
    const val SYMBOLOGY_COOP25_MAX_DEFAULT = CustomRangePreference.MAX_ALLOWED_VALUE
    val SYMBOLOGY_COOP25_CHK_DEFAULT = Barkoder.StandardChecksumType.Disabled
    const val SYMBOLOGY_C32_DEFAULT = true
    const val COMPOSITE_MODE_ANYSCAN = false
    const val SYMBOLOGY_C32_MIN_DEFAULT = CustomRangePreference.MIN_ALLOWED_VALUE
    const val SYMBOLOGY_C32_MAX_DEFAULT = CustomRangePreference.MAX_ALLOWED_VALUE
    const val SYMBOLOGY_TELEPEN_DEFAULT = true
    const val SYMBOLOGY_TELEPEN_MIN_DEFAULT = CustomRangePreference.MIN_ALLOWED_VALUE
    const val SYMBOLOGY_TELEPEN_MAX_DEFAULT = CustomRangePreference.MAX_ALLOWED_VALUE
    const val SYMBOLOGY_DOTCODE_DEFAULT = true
    const val SYMBOLOGY_DOTCODE_MIN_DEFAULT = CustomRangePreference.MAX_ALLOWED_VALUE
    const val SYMBOLOGY_DOTCODE_MAX_DEFAULT = CustomRangePreference.MAX_ALLOWED_VALUE

    val PARSER_TYPE_DEFAULT = Barkoder.FormattingType.Disabled
    val PARSER_TYPE_PDF_DEFAULT = Barkoder.FormattingType.AAMVA
    val PARSER_TYPE_GALLERY_DEFAULT = Barkoder.FormattingType.Automatic
    const val RESULT_CHARSET_DEFAULT = ""

    const val SEARCH_ENGINE_BROWSER_DEFAULT = "Google"
    const val WEBHOOK_AUTOSEND_DEFAULT = false
    const val WEBHOOK_FEEDBACK_DEFAULT = false
    const val WEBHOOK_ENCODE_DEFAULT = false

}
