package com.barkoder.demoscanner.enums

import android.content.res.Resources
import com.barkoder.demoscanner.R
import com.barkoder.enums.BarkoderConfigTemplate

enum class ScanMode(
    val title: String,
    val prefKey: String,
    val template: BarkoderConfigTemplate? = null,
    private val supportedSymbologyKeyResources: Array<Int>? = null,
) {
    INDUSTRIAL_1D(
        "1D Industrial",
        "_industrial_1d_mode_settings",
        BarkoderConfigTemplate.INDUSTRIAL_1D,
        arrayOf(
            R.string.key_symbology_c128,
            R.string.key_symbology_c93,
            R.string.key_symbology_c39,
            R.string.key_symbology_c25,
            R.string.key_symbology_codabar,
            R.string.key_symbology_c11,
            R.string.key_symbology_msi,
            R.string.key_symbology_c32,
            R.string.key_symbology_i2o5,
            R.string.key_symbology_itf14,
            R.string.key_symbology_iata25,
            R.string.key_symbology_matrix25,
            R.string.key_symbology_dataLogic25,
            R.string.key_symbology_coop25,
            R.string.key_symbology_telepen,
            R.string.key_symbology_databar14,
            R.string.key_symbology_databarExpanded,
            R.string.key_symbology_databarLimited,
        )
    ),
    RETAIL_1D(
        "1D Retail",
        "_retail_1d_mode_settings",
        BarkoderConfigTemplate.RETAIL_1D,
        arrayOf(
            R.string.key_vibrate,
            R.string.key_symbology_c128,
            R.string.key_symbology_upca,
            R.string.key_symbology_upce,
            R.string.key_symbology_upce1,
            R.string.key_symbology_ean13,
            R.string.key_symbology_ean8,
            R.string.key_symbology_databar14,
            R.string.key_symbology_databarExpanded,
            R.string.key_symbology_databarLimited
        )
    ),
    PDF(
        "PDF417", "_pdf_optimized_mode_settings", BarkoderConfigTemplate.PDF_OPTIMIZED,
        arrayOf(
            R.string.key_symbology_pdf417,
            R.string.key_symbology_pdf417_micro
        )
    ),
    QR(
        "QR Codes", "_qr_mode_settings", BarkoderConfigTemplate.QR,
        arrayOf(
            R.string.key_symbology_qr,
            R.string.key_symbology_qr_micro
        )
    ),
    ALL_2D(
        "All 2D Codes", "_all_2d_mode_settings", BarkoderConfigTemplate.ALL_2D,
        arrayOf(
            R.string.key_symbology_aztec,
            R.string.key_symbology_aztec_compact,
            R.string.key_symbology_qr,
            R.string.key_symbology_qr_micro,
            R.string.key_symbology_pdf417,
            R.string.key_symbology_pdf417_micro,
            R.string.key_symbology_datamatrix,
            R.string.key_symbology_dotcode,
            R.string.key_symbology_maxicode
        )
    ),
    CONTINUOUS("Batch MultiScan", "_continuous_mode_settings"),
    ANYSCAN("Anycode", "_anyscan_mode_settings"),
    AR_MODE("AR Mode", "_ar_mode_settings", BarkoderConfigTemplate.AR,
        arrayOf(
            R.string.key_symbology_aztec,
            R.string.key_symbology_aztec_compact,
            R.string.key_symbology_qr,
            R.string.key_symbology_qr_micro,
            R.string.key_symbology_pdf417,
            R.string.key_symbology_pdf417_micro,
            R.string.key_symbology_datamatrix,
            R.string.key_symbology_dotcode,
            R.string.key_symbology_maxicode,
            R.string.key_symbology_c128,
            R.string.key_symbology_c93,
            R.string.key_symbology_c39,
            R.string.key_symbology_c25,
            R.string.key_symbology_codabar,
            R.string.key_symbology_c11,
            R.string.key_symbology_msi,
            R.string.key_symbology_c32,
            R.string.key_symbology_i2o5,
            R.string.key_symbology_itf14,
            R.string.key_symbology_iata25,
            R.string.key_symbology_matrix25,
            R.string.key_symbology_dataLogic25,
            R.string.key_symbology_coop25,
            R.string.key_symbology_telepen,
            R.string.key_symbology_upca,
            R.string.key_symbology_upce,
            R.string.key_symbology_upce1,
            R.string.key_symbology_ean13,
            R.string.key_symbology_ean8,
            R.string.key_symbology_databar14,
            R.string.key_symbology_databarExpanded,
            R.string.key_symbology_databarLimited,
            R.string.key_symbology_postalImb,
            R.string.key_symbology_postnet,
            R.string.key_symbology_planet,
            R.string.key_symbology_australianPost,
            R.string.key_symbology_royalMail,
            R.string.key_symbology_kix,
            R.string.key_symbology_japanasePost,

            )
        ),
    DPM("DPM Mode", "_dpm_mode_settings", BarkoderConfigTemplate.DPM,
        arrayOf(
            R.string.key_symbology_qr,
            R.string.key_symbology_qr_micro,
            R.string.key_symbology_datamatrix,

        )
    ),
    VIN("VIN Mode","_vin_mode_settings", BarkoderConfigTemplate.VIN,
        arrayOf(
            R.string.key_symbology_c128,
            R.string.key_symbology_datamatrix,
            R.string.key_symbology_qr,
            R.string.key_symbology_c39,
        ),),
    DOTCODE("Dot Code", "_dotcode_mode_settings", BarkoderConfigTemplate.DOTCODE,
       ),
    UPC_EAN_DEBLUR("Deblur","_blurred1d_settings", null ,
        arrayOf(
            R.string.key_symbology_upca,
            R.string.key_symbology_upce,
            R.string.key_symbology_upce1,
            R.string.key_symbology_ean13,
            R.string.key_symbology_ean8,
        ),
        ),
    MISSHAPED_1D("Misshaped", "_misshaped1d_settings", null,
        arrayOf(
            R.string.key_symbology_c128,
            R.string.key_symbology_c93,
            R.string.key_symbology_c39,
            R.string.key_symbology_c25,
            R.string.key_symbology_codabar,
            R.string.key_symbology_c11,
            R.string.key_symbology_msi,
            R.string.key_symbology_c32,
            R.string.key_symbology_i2o5,
            R.string.key_symbology_itf14,
            R.string.key_symbology_iata25,
            R.string.key_symbology_matrix25,
            R.string.key_symbology_dataLogic25,
            R.string.key_symbology_coop25,
            R.string.key_symbology_telepen
        ),),
    ALL_1D("All 1D Codes", "_all_1d_mode_settings", BarkoderConfigTemplate.ALL_1D,
        arrayOf(
            R.string.key_symbology_c128,
            R.string.key_symbology_c93,
            R.string.key_symbology_c39,
            R.string.key_symbology_c25,
            R.string.key_symbology_codabar,
            R.string.key_symbology_c11,
            R.string.key_symbology_msi,
            R.string.key_symbology_c32,
            R.string.key_symbology_i2o5,
            R.string.key_symbology_itf14,
            R.string.key_symbology_iata25,
            R.string.key_symbology_matrix25,
            R.string.key_symbology_dataLogic25,
            R.string.key_symbology_coop25,
            R.string.key_symbology_telepen,
            R.string.key_symbology_upca,
            R.string.key_symbology_upce,
            R.string.key_symbology_upce1,
            R.string.key_symbology_ean13,
            R.string.key_symbology_ean8,
            R.string.key_symbology_databar14,
            R.string.key_symbology_databarExpanded,
            R.string.key_symbology_databarLimited,
            R.string.key_symbology_postalImb,
            R.string.key_symbology_postnet,
            R.string.key_symbology_planet,
            R.string.key_symbology_australianPost,
            R.string.key_symbology_royalMail,
            R.string.key_symbology_kix,
            R.string.key_symbology_japanasePost,
        )
        ),
    GLOBAL("Scan Mode", ""),
    MRZ("MRZ Mode", "_mrz_mode_settings", BarkoderConfigTemplate.MRZ
    ),
    GALLERY_SCAN("Gallery Scan", "_gallery_scan_mode_settings", BarkoderConfigTemplate.GALLERY_SCAN
    ),
    COMPOSITE("Composite", "composite_mode_settings", BarkoderConfigTemplate.COMPOSITE,
        arrayOf(
            R.string.key_symbology_ean8,
            R.string.key_symbology_ean13,
            R.string.key_symbology_c128,
            R.string.key_symbology_upca,
            R.string.key_symbology_upce,
            R.string.key_symbology_upce1,
            R.string.key_symbology_pdf417,
            R.string.key_symbology_pdf417_micro,
            R.string.key_symbology_databar14,
            R.string.key_symbology_databarExpanded,
            R.string.key_symbology_databarLimited,
        )),
    POSTAL_CODES("Postal codes", "postal_code_mode_settings", BarkoderConfigTemplate.POSTAL_CODES,
        arrayOf(
            R.string.key_symbology_postalImb,
            R.string.key_symbology_postnet,
            R.string.key_symbology_planet,
            R.string.key_symbology_australianPost,
            R.string.key_symbology_royalMail,
            R.string.key_symbology_kix,
            R.string.key_symbology_japanasePost,
        )),
   ;


    fun supportedSymbologyKeys(resources: Resources): List<String>? {
        return supportedSymbologyKeyResources?.let { keyResources ->
            val supportedSymbologyKeys = mutableListOf<String>()

            keyResources.forEach {
                supportedSymbologyKeys.add(resources.getString(it))
            }

            supportedSymbologyKeys
        }
    }

}
