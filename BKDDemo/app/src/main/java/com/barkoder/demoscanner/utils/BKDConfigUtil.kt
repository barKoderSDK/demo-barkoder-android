package com.barkoder.demoscanner.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.util.Log
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.barkoder.Barkoder
import com.barkoder.Barkoder.DecodingSpeed
import com.barkoder.BarkoderConfig
import com.barkoder.BarkoderHelper
import com.barkoder.demoscanner.R
import com.barkoder.demoscanner.enums.ScanMode
import com.barkoder.enums.BarkoderConfigTemplate
import com.barkoder.enums.BarkoderResolution
import com.barkoder.exceptions.BarkoderException
import com.barkoder.interfaces.BarkoderConfigCallback

object BKDConfigUtil {
    //TODO use Hilt for prefs and gson
    var TAG = BKDConfigUtil::class.java.simpleName

    fun configureBKD(
        context: Context,
        scanMode: ScanMode,
        forImage: Boolean = false

    ): BarkoderConfig {

        val config = createConfig(context.applicationContext)

        val sharedPref =
            if (scanMode == ScanMode.GLOBAL)
                PreferenceManager.getDefaultSharedPreferences(context)
            else
                context.getSharedPreferences(
                    context.packageName + scanMode.prefKey,
                    Context.MODE_PRIVATE
                )

        if (scanMode.template != null) {
            BarkoderHelper.applyConfigSettingsFromTemplate(context, config, scanMode.template,
                object : BarkoderConfigCallback { // This is sync callback
                    override fun onSuccess() {
                        setDefaultValuesInPrefs(sharedPref, context, true, scanMode, config)
                        setSymbologies(
                            config,
                            context.resources,
                            sharedPref,
                            scanMode.template
                        )

                        if (scanMode.template == BarkoderConfigTemplate.QR || scanMode.template == BarkoderConfigTemplate.ALL_2D) {
                            config.setRegionOfInterest(
                                DemoDefaults.ROI_LEFT_DEFAULT_VALUE,
                                DemoDefaults.ROI_TOP_DEFAULT_VALUE,
                                DemoDefaults.ROI_WIDTH_DEFAULT_VALUE,
                                DemoDefaults.ROI_HEIGHT_DEFAULT_VALUE,
                            )
                            config.isRegionOfInterestVisible = true
                        }

                    }

                    override fun onFail(exception: BarkoderException?) {
                        if (exception != null) {
                            Toast.makeText(
                                context,
                                context.getString(
                                    R.string.configuration_from_template_failed,
                                    exception.message
                                ),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                })
        } else {
            adaptConfigForContinuesScanning(config)
            setBarkoderSettings(config, context.resources, sharedPref)
            setSymbologies(config, context.resources, sharedPref)
            setResultSettings(config, context.resources, sharedPref)

            if(scanMode == ScanMode.UPC_EAN_DEBLUR) {
                adaptConfigForDeblurScanning(config)
            }

            if(scanMode == ScanMode.MISSHAPED_1D) {
                adaptConfigForMisshapedScanning(config)
            }

            if (forImage)
                config.setRegionOfInterest(0f, 0f, 100f, 100f)

            if (scanMode == ScanMode.CONTINUOUS) {
                adaptConfigForContinuesScanning(config)
                val continusThreshHold = sharedPref.getString("pref_key_continuous_treshold2", "5")
                if(continusThreshHold == "unlimited") {
                    config.thresholdBetweenDuplicatesScans = 100
                } else {
                    config.thresholdBetweenDuplicatesScans = continusThreshHold!!.toInt()
                }


            }

        }

        return config
    }

    private fun createConfig(appContext: Context): BarkoderConfig {
        return BarkoderConfig(
            appContext,
            appContext.getString(R.string.barkoderLicenseKey)
        ) {
            Log.i(TAG, it.message)
        }
    }


    private fun setBarkoderSettings(
        config: BarkoderConfig,
        resources: Resources,
        sharedPref: SharedPreferences
    ) {

        config.decoderConfig.decodingSpeed = Barkoder.DecodingSpeed.values()[
            sharedPref.getString(
                resources.getString(R.string.key_scanner_decoding_speed)
            ).toInt()
        ]

        if(sharedPref.getString(resources.getString(R.string.key_checksum_mrz))  == "Enabled") {
                config.decoderConfig.IDDocument.masterChecksumType = Barkoder.StandardChecksumType.Enabled
            } else if (sharedPref.getString(resources.getString(R.string.key_checksum_mrz))  == "Disabled") {
            config.decoderConfig.IDDocument.masterChecksumType = Barkoder.StandardChecksumType.Disabled
        } else {
            config.decoderConfig.IDDocument.masterChecksumType = Barkoder.StandardChecksumType.Disabled
        }


        config.barkoderResolution = BarkoderResolution.values()[
            sharedPref.getString(
                resources.getString(R.string.key_scanner_resolution)
            ).toInt()]
//
//        config.isCloseSessionOnResultEnabled =
//            sharedPref.getBoolean(
//                resources.getString(R.string.key_close_session_on_result)
//            )

        config.isLocationInPreviewEnabled =
            sharedPref.getBoolean(
                resources.getString(R.string.key_enable_location_in_preview)
            )

        config.isPinchToZoomEnabled =
            sharedPref.getBoolean(
                resources.getString(R.string.key_allow_pinch_to_zoom)
            )

        config.isRegionOfInterestVisible =
            sharedPref.getBoolean(resources.getString(R.string.key_enable_roi))

        if (config.isRegionOfInterestVisible) {
            config.setRegionOfInterest(
                DemoDefaults.ROI_LEFT_DEFAULT_VALUE,
                DemoDefaults.ROI_TOP_DEFAULT_VALUE,
                DemoDefaults.ROI_WIDTH_DEFAULT_VALUE,
                DemoDefaults.ROI_HEIGHT_DEFAULT_VALUE,
            )
        }

        config.isBeepOnSuccessEnabled =
            sharedPref.getBoolean(
                resources.getString(R.string.key_beep)
            )

        config.isVibrateOnSuccessEnabled =
            sharedPref.getBoolean(
                resources.getString(R.string.key_vibrate)
            )

       config.decoderConfig.enableMisshaped1D =
            sharedPref.getBoolean(resources.getString(R.string.key_misshaped_code_capture))

        config.decoderConfig.upcEanDeblur =
            sharedPref.getBoolean(resources.getString(R.string.key_blured_scan_eanupc))


        var automaticShowBottomsheet = sharedPref.getBoolean(resources.getString(R.string.key_automatic_show_bottomsheet))

        if(automaticShowBottomsheet) {
           var editor =  sharedPref.edit()
            editor.putBoolean("showBottomSHeet", true)
            editor.apply()
        } else {
            var editor =  sharedPref.edit()
            editor.putBoolean("showBottomSHeet", false)
            editor.apply()
        }

        var continusMode =
            sharedPref.getBoolean(resources.getString(R.string.key_continuous_scaning))

        if (continusMode) {
            adaptConfigForContinuesScanning(config)
            config.thresholdBetweenDuplicatesScans =
                sharedPref.getString(resources.getString(R.string.key_continuous_treshold)).toInt()
        } else {
            adaptConfigDisableMultiScan(config)
            config.thresholdBetweenDuplicatesScans = -1
        }
    }

    //region Symbologies configuration

    private fun setSymbologies(
        config: BarkoderConfig,
        resources: Resources,
        sharedPref: SharedPreferences,
        forTemplate: BarkoderConfigTemplate? = null
    ) {
        when (forTemplate) {
            BarkoderConfigTemplate.INDUSTRIAL_1D -> {
                setBarkoderSettings(config, resources, sharedPref)
                setResultSettings(config, resources, sharedPref)
                configureCode128Symbology(config, resources, sharedPref)
                configureCode93Symbology(config, resources, sharedPref)
                configureCode39Symbology(config, resources, sharedPref)
                configureCode25Symbology(config, resources, sharedPref)
                configureCodabarSymbology(config, resources, sharedPref)
                configureCode11Symbology(config, resources, sharedPref)
                configureMsiSymbology(config, resources, sharedPref)
                configureCode32Symbology(config, resources, sharedPref)
                configureInterleaved25Symbology(config, resources, sharedPref)
                configureITF14Symbology(config, resources, sharedPref)
                configureIATA25Symbology(config, resources, sharedPref)
                configureMatrix25Symbology(config, resources, sharedPref)
                configureDatalogic25Symbology(config, resources, sharedPref)
                configureCOOP25Symbology(config, resources, sharedPref)
                configureTelepenSymbology(config, resources, sharedPref)
                configureDatabar14Symbology(config,resources,sharedPref)
                configureDatabarExpandedSymbology(config,resources,sharedPref)
                configureDatabarLimitedSymbology(config,resources,sharedPref)
            }

            BarkoderConfigTemplate.RETAIL_1D -> {
                setBarkoderSettings(config, resources, sharedPref)
                setResultSettings(config, resources, sharedPref)
                configureCode128Symbology(config, resources, sharedPref)
                configureUpcASymbology(config, resources, sharedPref)
                configureUpcESymbology(config, resources, sharedPref)
                configureUpcE1Symbology(config, resources, sharedPref)
                configureEan13Symbology(config, resources, sharedPref)
                configureEan8Symbology(config, resources, sharedPref)
                configureDatabar14Symbology(config,resources,sharedPref)
                configureDatabarExpandedSymbology(config,resources,sharedPref)
                configureDatabarLimitedSymbology(config,resources,sharedPref)
            }

            BarkoderConfigTemplate.PDF_OPTIMIZED -> {
                setBarkoderSettings(config, resources, sharedPref)
                setResultSettings(config, resources, sharedPref)
                configurePDF417Symbology(config, resources, sharedPref)
                configurePDF417MicroSymbology(config, resources, sharedPref)
            }

            BarkoderConfigTemplate.QR -> {
                setBarkoderSettings(config,resources,sharedPref)
                setResultSettings(config, resources, sharedPref)
                configureQRSymbology(config, resources, sharedPref)
                configureQRMicroSymbology(config, resources, sharedPref)
            }

            BarkoderConfigTemplate.ALL_2D -> {
                setBarkoderSettings(config,resources,sharedPref)
                setResultSettings(config, resources, sharedPref)
                configureAztecSymbology(config, resources, sharedPref)
                configureAztecCompactSymbology(config, resources, sharedPref)
                configureQRSymbology(config, resources, sharedPref)
                configureQRMicroSymbology(config, resources, sharedPref)
                configurePDF417Symbology(config, resources, sharedPref)
                configurePDF417MicroSymbology(config, resources, sharedPref)
                configureDatamatrixSymbology(config, resources, sharedPref)
                configureDotCodeSymbology(config, resources, sharedPref)

            }
            BarkoderConfigTemplate.DPM -> {
                setBarkoderSettings(config,resources,sharedPref)
                setResultSettings(config, resources, sharedPref)
                configureDatamatrixSymbology(config, resources, sharedPref)
                configureQRSymbology(config, resources, sharedPref)
                configureQRMicroSymbology(config, resources, sharedPref)

                var biggerViewfinderBoolean = sharedPref.getBoolean("pref_key_bigger_viewfinder", false)
                if(biggerViewfinderBoolean)
                    config.setRegionOfInterest(35f, 40f, 30f, 15f)
                config.isRegionOfInterestVisible = true


            }
            BarkoderConfigTemplate.VIN -> {
                setBarkoderSettings(config,resources,sharedPref)
                setResultSettings(config, resources, sharedPref)
                configureCode39Symbology(config, resources, sharedPref)
                configureCode128Symbology(config, resources, sharedPref)
                configureDatamatrixSymbology(config, resources, sharedPref)
                configureQRSymbology(config,resources,sharedPref)
                var narrrowViewfinderBoolean = sharedPref.getBoolean("pref_key_narrow_viewfinder", true)
                if(narrrowViewfinderBoolean){
                    config.isRegionOfInterestVisible = true
                    config.setRegionOfInterest(0f, 35f, 100f, 30f)
                }
            }
            BarkoderConfigTemplate.MRZ -> {
                setBarkoderSettings(config,resources,sharedPref)
                config.setRegionOfInterest(0f, 0f, 100f, 100f)
                config.thresholdBetweenDuplicatesScans = -1

            }
            BarkoderConfigTemplate.DOTCODE -> {
                setBarkoderSettings(config,resources,sharedPref)
                setResultSettings(config, resources, sharedPref)
                config.isRegionOfInterestVisible = true
            }

            BarkoderConfigTemplate.ALL_1D -> {
                setBarkoderSettings(config,resources,sharedPref)
                setResultSettings(config, resources, sharedPref)
                setBarkoderSettings(config, resources, sharedPref)
                setResultSettings(config, resources, sharedPref)
                configureCode128Symbology(config, resources, sharedPref)
                configureCode93Symbology(config, resources, sharedPref)
                configureCode39Symbology(config, resources, sharedPref)
                configureCode25Symbology(config, resources, sharedPref)
                configureCodabarSymbology(config, resources, sharedPref)
                configureCode11Symbology(config, resources, sharedPref)
                configureMsiSymbology(config, resources, sharedPref)
                configureCode32Symbology(config, resources, sharedPref)
                configureInterleaved25Symbology(config, resources, sharedPref)
                configureITF14Symbology(config, resources, sharedPref)
                configureIATA25Symbology(config, resources, sharedPref)
                configureMatrix25Symbology(config, resources, sharedPref)
                configureDatalogic25Symbology(config, resources, sharedPref)
                configureCOOP25Symbology(config, resources, sharedPref)
                configureTelepenSymbology(config, resources, sharedPref)
                configureUpcASymbology(config, resources, sharedPref)
                configureUpcESymbology(config, resources, sharedPref)
                configureUpcE1Symbology(config, resources, sharedPref)
                configureEan13Symbology(config, resources, sharedPref)
                configureEan8Symbology(config, resources, sharedPref)
                configureDatabar14Symbology(config,resources,sharedPref)
                configureDatabarExpandedSymbology(config,resources,sharedPref)
                configureDatabarLimitedSymbology(config,resources,sharedPref)

            }
            BarkoderConfigTemplate.GALLERY_SCAN -> {
                configureMRZMode(config,resources,sharedPref)
                setBarkoderSettings(config,resources,sharedPref)
                setResultSettings(config, resources, sharedPref)
                configureCode128Symbology(config, resources, sharedPref)
                configureCode93Symbology(config, resources, sharedPref)
                configureCode39Symbology(config, resources, sharedPref)
                configureCode25Symbology(config, resources, sharedPref)
                configureCodabarSymbology(config, resources, sharedPref)
                configureCode11Symbology(config, resources, sharedPref)
                configureMsiSymbology(config, resources, sharedPref)
                configureCode32Symbology(config, resources, sharedPref)
                configureInterleaved25Symbology(config, resources, sharedPref)
                configureITF14Symbology(config, resources, sharedPref)
                configureIATA25Symbology(config, resources, sharedPref)
                configureMatrix25Symbology(config, resources, sharedPref)
                configureDatalogic25Symbology(config, resources, sharedPref)
                configureCOOP25Symbology(config, resources, sharedPref)
                configureTelepenSymbology(config, resources, sharedPref)
                configureUpcASymbology(config, resources, sharedPref)
                configureUpcESymbology(config, resources, sharedPref)
                configureUpcE1Symbology(config, resources, sharedPref)
                configureEan13Symbology(config, resources, sharedPref)
                configureEan8Symbology(config, resources, sharedPref)
                configureAztecSymbology(config, resources, sharedPref)
                configureAztecCompactSymbology(config, resources, sharedPref)
                configureQRSymbology(config, resources, sharedPref)
                configureQRMicroSymbology(config, resources, sharedPref)
                configurePDF417Symbology(config, resources, sharedPref)
                configurePDF417MicroSymbology(config, resources, sharedPref)
                configureDatamatrixSymbology(config, resources, sharedPref)
                configureDotCodeSymbology(config, resources, sharedPref)
                configureGalleryScanDPMMode(config, resources, sharedPref)
            }

            BarkoderConfigTemplate.COMPOSITE -> {
                setBarkoderSettings(config,resources,sharedPref)
                setResultSettings(config, resources, sharedPref)
                configureCode128Symbology(config,resources,sharedPref)
                configureUpcASymbology(config, resources, sharedPref)
                configureUpcESymbology(config, resources, sharedPref)
                configureUpcE1Symbology(config, resources, sharedPref)
                configureEan13Symbology(config, resources, sharedPref)
                configureEan8Symbology(config, resources, sharedPref)
                configurePDF417Symbology(config, resources, sharedPref)
                configurePDF417MicroSymbology(config, resources, sharedPref)
                configureDatabar14Symbology(config,resources,sharedPref)
                configureDatabarExpandedSymbology(config,resources,sharedPref)
                configureDatabarLimitedSymbology(config,resources,sharedPref)

            }

            else -> {
                setBarkoderSettings(config,resources,sharedPref)
                setResultSettings(config, resources, sharedPref)
                configureAztecSymbology(config, resources, sharedPref)
                configureAztecCompactSymbology(config, resources, sharedPref)
                configureQRSymbology(config, resources, sharedPref)
                configureQRMicroSymbology(config, resources, sharedPref)
                configureCode128Symbology(config, resources, sharedPref)
                configureCode93Symbology(config, resources, sharedPref)
                configureCode39Symbology(config, resources, sharedPref)
                configureCodabarSymbology(config, resources, sharedPref)
                configureCode11Symbology(config, resources, sharedPref)
                configureMsiSymbology(config, resources, sharedPref)
                configureUpcASymbology(config, resources, sharedPref)
                configureUpcESymbology(config, resources, sharedPref)
                configureUpcE1Symbology(config, resources, sharedPref)
                configureEan13Symbology(config, resources, sharedPref)
                configureEan8Symbology(config, resources, sharedPref)
                configurePDF417Symbology(config, resources, sharedPref)
                configurePDF417MicroSymbology(config, resources, sharedPref)
                configureDatamatrixSymbology(config, resources, sharedPref)
                configureCode25Symbology(config, resources, sharedPref)
                configureInterleaved25Symbology(config, resources, sharedPref)
                configureITF14Symbology(config, resources, sharedPref)
                configureIATA25Symbology(config, resources, sharedPref)
                configureMatrix25Symbology(config, resources, sharedPref)
                configureDatalogic25Symbology(config, resources, sharedPref)
                configureCOOP25Symbology(config, resources, sharedPref)
                configureCode32Symbology(config, resources, sharedPref)
                configureTelepenSymbology(config, resources, sharedPref)
                configureDotCodeSymbology(config, resources, sharedPref)
                configureDatabar14Symbology(config,resources,sharedPref)
                configureDatabarExpandedSymbology(config,resources,sharedPref)
                configureDatabarLimitedSymbology(config,resources,sharedPref)
            }
        }
    }


    private fun configureAztecSymbology(
        config: BarkoderConfig,
        resources: Resources,
        sharedPref: SharedPreferences
    ) {
        config.decoderConfig.Aztec.enabled =
            sharedPref.getBoolean(
                resources.getString(R.string.key_symbology_aztec)
            )
    }

    private fun configureAztecCompactSymbology(
        config: BarkoderConfig,
        resources: Resources,
        sharedPref: SharedPreferences
    ) {
        config.decoderConfig.AztecCompact.enabled =
            sharedPref.getBoolean(
                resources.getString(R.string.key_symbology_aztec_compact)
            )
    }

    private fun configureQRSymbology(
        config: BarkoderConfig,
        resources: Resources,
        sharedPref: SharedPreferences
    ) {
        config.decoderConfig.QR.enabled =
            sharedPref.getBoolean(
                resources.getString(R.string.key_symbology_qr)
            )
    }

    private fun configureQRMicroSymbology(
        config: BarkoderConfig,
        resources: Resources,
        sharedPref: SharedPreferences
    ) {
        config.decoderConfig.QRMicro.enabled =
            sharedPref.getBoolean(
                resources.getString(R.string.key_symbology_qr_micro)
            )
    }

    private fun configureCode128Symbology(
        config: BarkoderConfig,
        resources: Resources,
        sharedPref: SharedPreferences
    ) {
        config.decoderConfig.Code128.enabled =
            sharedPref.getBoolean(
                resources.getString(R.string.key_symbology_c128)
            )
        setConfigLength(
            resources,
            sharedPref,
            config.decoderConfig.Code128,
            resources.getString(R.string.key_symbology_c128)
        )
    }

    private fun configureCode93Symbology(
        config: BarkoderConfig,
        resources: Resources,
        sharedPref: SharedPreferences
    ) {
        config.decoderConfig.Code93.enabled =
            sharedPref.getBoolean(
                resources.getString(R.string.key_symbology_c93)
            )
        setConfigLength(
            resources,
            sharedPref,
            config.decoderConfig.Code93,
            resources.getString(R.string.key_symbology_c93)
        )
    }

    private fun configureCode39Symbology(
        config: BarkoderConfig,
        resources: Resources,
        sharedPref: SharedPreferences
    ) {
        config.decoderConfig.Code39.enabled =
            sharedPref.getBoolean(
                resources.getString(R.string.key_symbology_c39)
            )
        setConfigLength(
            resources,
            sharedPref,
            config.decoderConfig.Code39,
            resources.getString(R.string.key_symbology_c39)
        )
        config.decoderConfig.Code39.checksumType =
            Barkoder.Code39ChecksumType.valueOf(
                getChecksumValueForConfig(
                    resources,
                    sharedPref,
                    resources.getString(R.string.key_symbology_c39)
                ).toInt()
            )
    }

    private fun configureCodabarSymbology(
        config: BarkoderConfig,
        resources: Resources,
        sharedPref: SharedPreferences
    ) {
        config.decoderConfig.Codabar.enabled =
            sharedPref.getBoolean(
                resources.getString(R.string.key_symbology_codabar)
            )
        setConfigLength(
            resources,
            sharedPref,
            config.decoderConfig.Codabar,
            resources.getString(R.string.key_symbology_codabar)
        )
    }

    private fun configureCode11Symbology(
        config: BarkoderConfig,
        resources: Resources,
        sharedPref: SharedPreferences
    ) {
        config.decoderConfig.Code11.enabled =
            sharedPref.getBoolean(
                resources.getString(R.string.key_symbology_c11),
                DemoDefaults.SYMBOLOGY_C11_DEFAULT
            )
        setConfigLength(
            resources,
            sharedPref,
            config.decoderConfig.Code11,
            resources.getString(R.string.key_symbology_c11)
        )
        config.decoderConfig.Code11.checksumType =
            Barkoder.Code11ChecksumType.valueOf(
                getChecksumValueForConfig(
                    resources,
                    sharedPref,
                    resources.getString(R.string.key_symbology_c11)
                ).toInt()
            )
    }

    private fun configureMsiSymbology(
        config: BarkoderConfig,
        resources: Resources,
        sharedPref: SharedPreferences
    ) {
        config.decoderConfig.Msi.enabled =
            sharedPref.getBoolean(
                resources.getString(R.string.key_symbology_msi)
            )
        setConfigLength(
            resources,
            sharedPref,
            config.decoderConfig.Msi,
            resources.getString(R.string.key_symbology_msi)
        )
        config.decoderConfig.Msi.checksumType =
            Barkoder.MsiChecksumType.valueOf(
                getChecksumValueForConfig(
                    resources,
                    sharedPref,
                    resources.getString(R.string.key_symbology_msi)
                ).toInt()
            )
    }

    private fun configureUpcASymbology(
        config: BarkoderConfig,
        resources: Resources,
        sharedPref: SharedPreferences
    ) {
        config.decoderConfig.UpcA.enabled =
            sharedPref.getBoolean(
                resources.getString(R.string.key_symbology_upca)
            )
    }

    private fun configureUpcESymbology(
        config: BarkoderConfig,
        resources: Resources,
        sharedPref: SharedPreferences
    ) {
        config.decoderConfig.UpcE.enabled =
            sharedPref.getBoolean(
                resources.getString(R.string.key_symbology_upce)
            )

        config.decoderConfig.UpcE.expandToUPCA =
            sharedPref.getBoolean(
                resources.getString(R.string.key_symbology_upce) + resources.getString(R.string.key_expand_to_upca)
            )
    }

    private fun configureUpcE1Symbology(
        config: BarkoderConfig,
        resources: Resources,
        sharedPref: SharedPreferences
    ) {
        config.decoderConfig.UpcE1.enabled =
            sharedPref.getBoolean(
                resources.getString(R.string.key_symbology_upce1)
            )

        config.decoderConfig.UpcE1.expandToUPCA =
            sharedPref.getBoolean(
                resources.getString(R.string.key_symbology_upce1) + resources.getString(R.string.key_expand_to_upca)
            )
    }

    private fun configureEan13Symbology(
        config: BarkoderConfig,
        resources: Resources,
        sharedPref: SharedPreferences
    ) {
        config.decoderConfig.Ean13.enabled =
            sharedPref.getBoolean(
                resources.getString(R.string.key_symbology_ean13)
            )
    }

    private fun configureEan8Symbology(
        config: BarkoderConfig,
        resources: Resources,
        sharedPref: SharedPreferences
    ) {
        config.decoderConfig.Ean8.enabled =
            sharedPref.getBoolean(
                resources.getString(R.string.key_symbology_ean8)
            )
    }

    private fun configurePDF417Symbology(
        config: BarkoderConfig,
        resources: Resources,
        sharedPref: SharedPreferences
    ) {
        config.decoderConfig.PDF417.enabled =
            sharedPref.getBoolean(
                resources.getString(R.string.key_symbology_pdf417)
            )
    }

    private fun configurePDF417MicroSymbology(
        config: BarkoderConfig,
        resources: Resources,
        sharedPref: SharedPreferences
    ) {
        config.decoderConfig.PDF417Micro.enabled =
            sharedPref.getBoolean(
                resources.getString(R.string.key_symbology_pdf417_micro)
            )
    }

    private fun configureDatamatrixSymbology(
        config: BarkoderConfig,
        resources: Resources,
        sharedPref: SharedPreferences
    ) {
        config.decoderConfig.Datamatrix.enabled =
            sharedPref.getBoolean(
                resources.getString(R.string.key_symbology_datamatrix)
            )
    }

    private fun configureGalleryScanDPMMode(
        config: BarkoderConfig,
        resources: Resources,
        sharedPref: SharedPreferences
    ) {
        config.decoderConfig.Datamatrix.dpmMode =
            sharedPref.getBoolean(
                resources.getString(R.string.key_dpm_mode)
            )

        config.decoderConfig.QR.dpmMode =
            sharedPref.getBoolean(
                resources.getString(R.string.key_dpm_mode)
            )

        config.decoderConfig.QRMicro.dpmMode =
            sharedPref.getBoolean(
                resources.getString(R.string.key_dpm_mode)
            )
    }


    private fun configureMRZMode(
        config: BarkoderConfig,
        resources: Resources,
        sharedPref: SharedPreferences
    ) {
        config.decoderConfig.IDDocument.enabled =
            sharedPref.getBoolean(
                resources.getString(R.string.key_mrz_mode)
            )
    }


    private fun configureCode25Symbology(
        config: BarkoderConfig,
        resources: Resources,
        sharedPref: SharedPreferences
    ) {
        config.decoderConfig.Code25.enabled =
            sharedPref.getBoolean(
                resources.getString(R.string.key_symbology_c25)
            )

        setConfigLength(
            resources,
            sharedPref,
            config.decoderConfig.Code25,
            resources.getString(R.string.key_symbology_c25)
        )

        config.decoderConfig.Code25.checksumType =
            Barkoder.StandardChecksumType.valueOf(
                getChecksumValueForConfig(
                    resources,
                    sharedPref,
                    resources.getString(R.string.key_symbology_c25)
                ).toInt()
            )
    }

    private fun configureInterleaved25Symbology(
        config: BarkoderConfig,
        resources: Resources,
        sharedPref: SharedPreferences
    ) {
        config.decoderConfig.Interleaved25.enabled =
            sharedPref.getBoolean(
                resources.getString(R.string.key_symbology_i2o5)
            )

        setConfigLength(
            resources,
            sharedPref,
            config.decoderConfig.Interleaved25,
            resources.getString(R.string.key_symbology_i2o5)
        )

        config.decoderConfig.Interleaved25.checksumType =
            Barkoder.StandardChecksumType.valueOf(
                getChecksumValueForConfig(
                    resources,
                    sharedPref,
                    resources.getString(R.string.key_symbology_i2o5)
                ).toInt()
            )
    }

    private fun configureITF14Symbology(
        config: BarkoderConfig,
        resources: Resources,
        sharedPref: SharedPreferences
    ) {
        config.decoderConfig.ITF14.enabled =
            sharedPref.getBoolean(
                resources.getString(R.string.key_symbology_itf14)
            )
    }

    private fun configureIATA25Symbology(
        config: BarkoderConfig,
        resources: Resources,
        sharedPref: SharedPreferences
    ) {
        config.decoderConfig.IATA25.enabled =
            sharedPref.getBoolean(
                resources.getString(R.string.key_symbology_iata25)
            )

        setConfigLength(
            resources,
            sharedPref,
            config.decoderConfig.IATA25,
            resources.getString(R.string.key_symbology_iata25)
        )

        config.decoderConfig.IATA25.checksumType =
            Barkoder.StandardChecksumType.valueOf(
                getChecksumValueForConfig(
                    resources,
                    sharedPref,
                    resources.getString(R.string.key_symbology_iata25)
                ).toInt()
            )
    }

    private fun configureMatrix25Symbology(
        config: BarkoderConfig,
        resources: Resources,
        sharedPref: SharedPreferences
    ) {
        config.decoderConfig.Matrix25.enabled =
            sharedPref.getBoolean(
                resources.getString(R.string.key_symbology_matrix25)
            )

        setConfigLength(
            resources,
            sharedPref,
            config.decoderConfig.Matrix25,
            resources.getString(R.string.key_symbology_matrix25)
        )

        config.decoderConfig.Matrix25.checksumType =
            Barkoder.StandardChecksumType.valueOf(
                getChecksumValueForConfig(
                    resources,
                    sharedPref,
                    resources.getString(R.string.key_symbology_matrix25)
                ).toInt()
            )
    }

    private fun configureDatalogic25Symbology(
        config: BarkoderConfig,
        resources: Resources,
        sharedPref: SharedPreferences
    ) {
        config.decoderConfig.Datalogic25.enabled =
            sharedPref.getBoolean(
                resources.getString(R.string.key_symbology_dataLogic25)
            )

        setConfigLength(
            resources,
            sharedPref,
            config.decoderConfig.Datalogic25,
            resources.getString(R.string.key_symbology_dataLogic25)
        )

        config.decoderConfig.Datalogic25.checksumType =
            Barkoder.StandardChecksumType.valueOf(
                getChecksumValueForConfig(
                    resources,
                    sharedPref,
                    resources.getString(R.string.key_symbology_dataLogic25)
                ).toInt()
            )
    }

    private fun configureCOOP25Symbology(
        config: BarkoderConfig,
        resources: Resources,
        sharedPref: SharedPreferences
    ) {
        config.decoderConfig.COOP25.enabled =
            sharedPref.getBoolean(
                resources.getString(R.string.key_symbology_coop25)
            )

        setConfigLength(
            resources,
            sharedPref,
            config.decoderConfig.COOP25,
            resources.getString(R.string.key_symbology_coop25)
        )

        config.decoderConfig.COOP25.checksumType =
            Barkoder.StandardChecksumType.valueOf(
                getChecksumValueForConfig(
                    resources,
                    sharedPref,
                    resources.getString(R.string.key_symbology_coop25)
                ).toInt()
            )
    }

    private fun configureCode32Symbology(
        config: BarkoderConfig,
        resources: Resources,
        sharedPref: SharedPreferences
    ) {
        config.decoderConfig.Code32.enabled =
            sharedPref.getBoolean(
                resources.getString(R.string.key_symbology_c32)
            )

        setConfigLength(
            resources,
            sharedPref,
            config.decoderConfig.Code32,
            resources.getString(R.string.key_symbology_c32)
        )
    }

    private fun configureTelepenSymbology(
        config: BarkoderConfig,
        resources: Resources,
        sharedPref: SharedPreferences
    ) {
        config.decoderConfig.Telepen.enabled =
            sharedPref.getBoolean(
                resources.getString(R.string.key_symbology_telepen)
            )

        setConfigLength(
            resources,
            sharedPref,
            config.decoderConfig.Telepen,
            resources.getString(R.string.key_symbology_telepen)
        )
    }

    private fun configureDatabar14Symbology(
        config: BarkoderConfig,
        resources: Resources,
        sharedPref: SharedPreferences
    ) {
        config.decoderConfig.Databar14.enabled =
            sharedPref.getBoolean(
                resources.getString(R.string.key_symbology_databar14)
            )
    }

    private fun configureDatabarExpandedSymbology(
        config: BarkoderConfig,
        resources: Resources,
        sharedPref: SharedPreferences
    ) {
        config.decoderConfig.DatabarExpanded.enabled =
            sharedPref.getBoolean(
                resources.getString(R.string.key_symbology_databarExpanded)
            )
    }

    private fun configureDatabarLimitedSymbology(
        config: BarkoderConfig,
        resources: Resources,
        sharedPref: SharedPreferences
    ) {
        config.decoderConfig.DatabarLimited.enabled =
            sharedPref.getBoolean(
                resources.getString(R.string.key_symbology_databarLimited)
            )
    }



    private fun configureDotCodeSymbology(
        config: BarkoderConfig,
        resources: Resources,
        sharedPref: SharedPreferences
    ) {
        config.decoderConfig.Dotcode.enabled =
            sharedPref.getBoolean(
                resources.getString(R.string.key_symbology_dotcode)
            )

    }

    //endregion Symbologies configuration

    private fun setConfigLength(
        resources: Resources,
        prefs: SharedPreferences,
        config: Barkoder.SpecificConfig,
        configKey: String
    ) {
        val minLength = prefs.getInt(
            configKey + resources.getString(R.string.key_min_length)
        )
        val maxLength = prefs.getInt(
            configKey + resources.getString(R.string.key_max_length)
        )
        config.setLengthRange(minLength, maxLength)
    }

    private fun setResultSettings(
        config: BarkoderConfig,
        resources: Resources,
        sharedPref: SharedPreferences
    ) {
        config.decoderConfig.formattingType = Barkoder.FormattingType.valueOf(
            sharedPref.getString(
                resources.getString(R.string.key_result_parser)
            ).toInt()
        )

        config.decoderConfig.encodingCharacterSet = sharedPref.getString(
            resources.getString(R.string.key_result_charset)
        )
    }

    private fun getChecksumValueForConfig(
        resources: Resources,
        prefs: SharedPreferences,
        configKey: String
    ) = prefs.getString(configKey + resources.getString(R.string.key_checksum_type))

    fun getEnabledTypesAsStringArray(decoderConfig: Barkoder.Config, resources: Resources): String {
        val enabledTypes = StringBuilder()
        for (type in decoderConfig.GetEnabledDecoders()) {
            val typeConfig = decoderConfig.GetConfigForDecoder(type)
            if (enabledTypes.isEmpty()) {
                when (typeConfig.typeName) {
                    "MSI" -> enabledTypes.append("MSI Plessey")
                    "Datamatrix" -> enabledTypes.append("Data Matrix")
                    "Upc-A" -> enabledTypes.append("UPC-A")
                    "Upc-E" -> enabledTypes.append("UPC-E")
                    "Upc-E1" -> enabledTypes.append("UPC-E1")
                    "Ean-13" -> enabledTypes.append("EAN-13")
                    "Ean-8" -> enabledTypes.append("EAN-8")
                    else -> enabledTypes.append(typeConfig.typeName)
                }
            }
            else {
                when(typeConfig.typeName) {
                    "MSI" ->  enabledTypes.append(", ").append("MSI Plessey")
                    "Datamatrix" ->  enabledTypes.append(", ").append("Data Matrix")
                    "Upc-A" ->  enabledTypes.append(", ").append("UPC-A")
                    "Upc-E" ->  enabledTypes.append(", ").append("UPC-E")
                    "Upc-E1" ->  enabledTypes.append(", ").append("UPC-E1")
                    "Ean-13" ->  enabledTypes.append(", ").append("EAN-13")
                    "Ean-8" ->  enabledTypes.append(", ").append("EAN-8")
                    else -> enabledTypes.append(", ").append(typeConfig.typeName)
                }

            }
        }

        return if (enabledTypes.isEmpty()) "" else enabledTypes.toString()
    }

    // For templates default values will be readed from config (configured from template) otherwise
    // app default values will be used
    fun setDefaultValuesInPrefs(
        sharedPrefs: SharedPreferences,
        context: Context,
        onlyIfNotContains: Boolean,
        scanMode: ScanMode,
        config: BarkoderConfig? = null
    ) {
        val prefsEditor = sharedPrefs.edit()

        prefsEditor.putBooleanWithOptions(
            sharedPrefs,
            context.getString(R.string.key_auto_start_scan),
            DemoDefaults.AUTO_START_SCAN_DEFAULT,
            onlyIfNotContains
        )


        //region Barkoder Settings
        if(scanMode.template != null) {
            if(scanMode == ScanMode.MRZ) {
                prefsEditor.putStringWithOptions(
                    sharedPrefs,
                    context.getString(R.string.key_scanner_decoding_speed),
                    config?.decoderConfig?.decodingSpeed?.ordinal?.toString()
                        ?: 1.toString(),
                    onlyIfNotContains
                )
            }  else if(scanMode == ScanMode.GALLERY_SCAN) {
                prefsEditor.putStringWithOptions(
                    sharedPrefs,
                    context.getString(R.string.key_scanner_decoding_speed),
                    DemoDefaults.DECODING_SPEED_DEFAULT_GALLERY_RIGORIUS.ordinal.toString(),
                    onlyIfNotContains
                )
            }

            else{
                prefsEditor.putStringWithOptions(
                    sharedPrefs,
                    context.getString(R.string.key_scanner_decoding_speed),
                    config?.decoderConfig?.decodingSpeed?.ordinal?.toString()
                        ?: DemoDefaults.DECODING_SPEED_DEFAULT_TEMPLATE.ordinal.toString(),
                    onlyIfNotContains
                )
            }

        } else {
            prefsEditor.putStringWithOptions(
                sharedPrefs,
                context.getString(R.string.key_scanner_decoding_speed),
                config?.decoderConfig?.decodingSpeed?.ordinal?.toString()
                    ?: DemoDefaults.DECODING_SPEED_DEFAULT.ordinal.toString(),
                onlyIfNotContains
            )
        }
        if(scanMode == ScanMode.CONTINUOUS || scanMode == ScanMode.ANYSCAN) {
            prefsEditor.putStringWithOptions(
                sharedPrefs,
                context.getString(R.string.key_scanner_resolution),
                config?.barkoderResolution?.ordinal?.toString()
                    ?: DemoDefaults.BARKODER_RESOLUTION_DEFAULT.ordinal.toString(),
                onlyIfNotContains
            )
        } else if
        (scanMode.template == ScanMode.VIN.template || scanMode.template == ScanMode.DPM.template
                || scanMode.template == ScanMode.RETAIL_1D.template || scanMode.template == ScanMode.PDF.template
                || scanMode.template == ScanMode.QR.template || scanMode.template == ScanMode.ALL_2D.template
                || scanMode.template == ScanMode.INDUSTRIAL_1D.template || scanMode.template == ScanMode.UPC_EAN_DEBLUR.template
                || scanMode.template == ScanMode.MISSHAPED_1D.template || scanMode.template == ScanMode.DOTCODE.template || scanMode.template == ScanMode.MRZ.template) {
            prefsEditor.putStringWithOptions(
                sharedPrefs,
                context.getString(R.string.key_scanner_resolution),
                config?.barkoderResolution?.ordinal?.toString()
                    ?: DemoDefaults.BARKODER_RESOLUTION_DEFAULT_TEMPLATES_VIN_DPM.ordinal.toString(),
                onlyIfNotContains
            )
        }
        else {
            prefsEditor.putStringWithOptions(
                sharedPrefs,
                context.getString(R.string.key_scanner_resolution),
                config?.barkoderResolution?.ordinal?.toString()
                    ?: DemoDefaults.BARKODER_RESOLUTION_DEFAULT.ordinal.toString(),
                onlyIfNotContains
            )

        }


//        val closeSessionOnResultDefaultValue =
//            if (scanMode == ScanMode.CONTINUOUS)
//                false
//            else
//                config?.isCloseSessionOnResultEnabled
//                    ?: DemoDefaults.CLOSE_SESSION_ON_RESULT_DEFAULT
//
//        prefsEditor.putBooleanWithOptions(
//            sharedPrefs,
//            context.getString(R.string.key_close_session_on_result),
//            closeSessionOnResultDefaultValue,
//            onlyIfNotContains
//        )

        prefsEditor.putBooleanWithOptions(
            sharedPrefs,
            context.getString(R.string.key_enable_location_in_preview),
            config?.isLocationInPreviewEnabled
                ?: DemoDefaults.ENABLE_LOCATION_IN_PREVIEW_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putBooleanWithOptions(
            sharedPrefs,
            context.getString(R.string.key_allow_pinch_to_zoom),
            config?.isPinchToZoomEnabled ?: DemoDefaults.ALLOW_PINCH_TO_ZOOM_DEFAULT,
            onlyIfNotContains
        )

        if(scanMode.title == "Batch MultiScan") {
            prefsEditor.putBooleanWithOptions(
                sharedPrefs,
                context.getString(R.string.key_enable_roi), true,
                onlyIfNotContains
            )
        } else {
            prefsEditor.putBooleanWithOptions(
                sharedPrefs,
                context.getString(R.string.key_enable_roi), DemoDefaults.ENABLE_ROI_DEFAULT,
                onlyIfNotContains
            )
        }


        prefsEditor.putBooleanWithOptions(
            sharedPrefs,
            context.getString(R.string.key_beep),
            config?.isBeepOnSuccessEnabled ?: DemoDefaults.BEEP_ON_SUCCESS_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putBooleanWithOptions(
            sharedPrefs,
            context.getString(R.string.key_vibrate),
            config?.isVibrateOnSuccessEnabled ?: DemoDefaults.VIBRATE_ON_SUCCESS_DEFAULT,
            onlyIfNotContains
        )
        if(scanMode == ScanMode.GALLERY_SCAN) {
            prefsEditor.putBooleanWithOptions(
                sharedPrefs,
                context.getString(R.string.key_blured_scan_eanupc),
                config?.decoderConfig?.upcEanDeblur ?: true,
                onlyIfNotContains
            )
        } else {
            prefsEditor.putBooleanWithOptions(
                sharedPrefs,
                context.getString(R.string.key_blured_scan_eanupc),
                config?.decoderConfig?.upcEanDeblur ?: DemoDefaults.DEBLUR_UPC_EAN_DEFAULT,
                onlyIfNotContains
            )
        }

        if(scanMode == ScanMode.GALLERY_SCAN) {
            prefsEditor.putBooleanWithOptions(
                sharedPrefs,
                context.getString(R.string.key_dpm_mode),
                config?.decoderConfig?.Datamatrix?.dpmMode ?: true,
                onlyIfNotContains
            )
            prefsEditor.putBooleanWithOptions(
                sharedPrefs,
                context.getString(R.string.key_mrz_mode),
                config?.decoderConfig?.IDDocument?.enabled ?: true,
                onlyIfNotContains
            )
        }

        if(scanMode == ScanMode.RETAIL_1D || scanMode == ScanMode.GALLERY_SCAN) {
            prefsEditor.putBooleanWithOptions(
                sharedPrefs,
                context.getString(R.string.key_misshaped_code_capture),
                config?.decoderConfig?.enableMisshaped1D ?: true,
                onlyIfNotContains
            )
        } else {
            prefsEditor.putBooleanWithOptions(
                sharedPrefs,
                context.getString(R.string.key_misshaped_code_capture),
                config?.decoderConfig?.enableMisshaped1D ?: DemoDefaults.MISSHAPED_1D_DEFAULT,
                onlyIfNotContains
            )
        }

        prefsEditor.putBooleanWithOptions(
            sharedPrefs,
            context.getString(R.string.key_continuous_scaning),
            DemoDefaults.CONTINUOUS_MODE_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putBooleanWithOptions(
            sharedPrefs,
            context.getString(R.string.key_bigger_viewfinder),
            DemoDefaults.CONTINUOUS_MODE_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putBooleanWithOptions(
            sharedPrefs,
            context.getString(R.string.key_narrow_viewfinder),
            true,
            onlyIfNotContains
        )
        prefsEditor.putStringWithOptions(
            sharedPrefs,
            context.getString(R.string.key_continuous_treshold),
            DemoDefaults.CONTINUOUS_TRESHOLD_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putBooleanWithOptions(
            sharedPrefs,
            context.getString(R.string.key_automatic_show_bottomsheet),
            DemoDefaults.AUTOMATIC_SHOWBOTTOMSHEET_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putBooleanWithOptions(
            sharedPrefs,
            context.getString(R.string.key_enable_webhook),
            DemoDefaults.ENABLED_WEBHOOK_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putBooleanWithOptions(
            sharedPrefs,
            context.getString(R.string.key_enable_searchweb),
            DemoDefaults.ENABLED_SEARCHWEB_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putStringWithOptions(
            sharedPrefs,
            context.getString(R.string.key_checksum_mrz),
            "Disabled",
            onlyIfNotContains
        )
        prefsEditor.putBooleanWithOptions(
            sharedPrefs,
            "showBottomSHeet",
            DemoDefaults.AUTOMATIC_SHOWBOTTOMSHEET_DEFAULT,
            onlyIfNotContains
        )

        //endregion Barkoder Settings

        //region Barcode Types

        prefsEditor.putBooleanWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_aztec),
            config?.decoderConfig?.Aztec?.enabled ?: DemoDefaults.SYMBOLOGY_AZTEC_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putBooleanWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_aztec_compact),
            config?.decoderConfig?.AztecCompact?.enabled
                ?: DemoDefaults.SYMBOLOGY_AZTEC_COMPACT_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putBooleanWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_qr),
            config?.decoderConfig?.QR?.enabled ?: DemoDefaults.SYMBOLOGY_QR_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putBooleanWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_qr_micro),
            config?.decoderConfig?.QRMicro?.enabled ?: DemoDefaults.SYMBOLOGY_QR_MICRO_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putBooleanWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_c11),
            config?.decoderConfig?.Code11?.enabled ?: DemoDefaults.SYMBOLOGY_C11_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putIntWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_c11) + context.getString(R.string.key_min_length),
            config?.decoderConfig?.Code11?.minimumLength
                ?: DemoDefaults.SYMBOLOGY_C11_MIN_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putIntWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_c11) + context.getString(R.string.key_max_length),
            config?.decoderConfig?.Code11?.maximumLength
                ?: DemoDefaults.SYMBOLOGY_C11_MAX_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putStringWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_c11) + context.getString(R.string.key_checksum_type),
            config?.decoderConfig?.Code11?.checksumType?.ordinal?.toString()
                ?: DemoDefaults.SYMBOLOGY_C11_CHK_DEFAULT.ordinal.toString(),
            onlyIfNotContains
        )

        prefsEditor.putStringWithOptions(
            sharedPrefs,
            context.getString(R.string.key_checksum_mrz_value),
            config?.decoderConfig?.IDDocument?.masterChecksumType?.ordinal?.toString()
                ?: DemoDefaults.SYMBOLOGY_C11_CHK_DEFAULT.ordinal.toString(),
            onlyIfNotContains
        )
        prefsEditor.putBooleanWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_c39),
            config?.decoderConfig?.Code39?.enabled ?: DemoDefaults.SYMBOLOGY_C39_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putIntWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_c39) + context.getString(R.string.key_min_length),
            config?.decoderConfig?.Code39?.minimumLength
                ?: DemoDefaults.SYMBOLOGY_C39_MIN_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putIntWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_c39) + context.getString(R.string.key_max_length),
            config?.decoderConfig?.Code39?.maximumLength
                ?: DemoDefaults.SYMBOLOGY_C39_MAX_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putStringWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_c39) + context.getString(R.string.key_checksum_type),
            config?.decoderConfig?.Code39?.checksumType?.ordinal?.toString()
                ?: DemoDefaults.SYMBOLOGY_C39_CHK_DEFAULT.ordinal.toString(),
            onlyIfNotContains
        )
        prefsEditor.putBooleanWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_c93),
            config?.decoderConfig?.Code93?.enabled ?: DemoDefaults.SYMBOLOGY_C93_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putIntWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_c93) + context.getString(R.string.key_min_length),
            config?.decoderConfig?.Code93?.minimumLength
                ?: DemoDefaults.SYMBOLOGY_C93_MIN_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putIntWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_c93) + context.getString(R.string.key_max_length),
            config?.decoderConfig?.Code93?.maximumLength
                ?: DemoDefaults.SYMBOLOGY_C93_MAX_DEFAULT,
            onlyIfNotContains
        )
        if(scanMode == ScanMode.RETAIL_1D) {
            prefsEditor.putBooleanWithOptions(
                sharedPrefs,
                context.getString(R.string.key_symbology_c128),
                config?.decoderConfig?.Code128?.enabled ?: false,
                onlyIfNotContains
            )
        } else {
            prefsEditor.putBooleanWithOptions(
                sharedPrefs,
                context.getString(R.string.key_symbology_c128),
                config?.decoderConfig?.Code128?.enabled ?: DemoDefaults.SYMBOLOGY_C128_DEFAULT,
                onlyIfNotContains
            )
        }

        prefsEditor.putIntWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_c128) + context.getString(R.string.key_min_length),
            config?.decoderConfig?.Code128?.minimumLength
                ?: DemoDefaults.SYMBOLOGY_C128_MIN_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putIntWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_c128) + context.getString(R.string.key_max_length),
            config?.decoderConfig?.Code128?.maximumLength
                ?: DemoDefaults.SYMBOLOGY_C128_MAX_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putBooleanWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_codabar),
            config?.decoderConfig?.Codabar?.enabled ?: DemoDefaults.SYMBOLOGY_CODABAR_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putIntWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_codabar) + context.getString(R.string.key_min_length),
            config?.decoderConfig?.Codabar?.minimumLength
                ?: DemoDefaults.SYMBOLOGY_CODABAR_MIN_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putIntWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_codabar) + context.getString(R.string.key_max_length),
            config?.decoderConfig?.Codabar?.maximumLength
                ?: DemoDefaults.SYMBOLOGY_CODABAR_MAX_DEFAULT,
            onlyIfNotContains
        )
        if(scanMode == ScanMode.INDUSTRIAL_1D || scanMode == ScanMode.MISSHAPED_1D || scanMode == ScanMode.ALL_1D) {
            prefsEditor.putBooleanWithOptions(
                sharedPrefs,
                context.getString(R.string.key_symbology_msi),
                config?.decoderConfig?.Msi?.enabled ?: false,
                onlyIfNotContains
            )
        } else {
            prefsEditor.putBooleanWithOptions(
                sharedPrefs,
                context.getString(R.string.key_symbology_msi),
                config?.decoderConfig?.Msi?.enabled ?: DemoDefaults.SYMBOLOGY_MSI_DEFAULT,
                onlyIfNotContains
            )
        }

        prefsEditor.putIntWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_msi) + context.getString(R.string.key_min_length),
            config?.decoderConfig?.Msi?.minimumLength ?: DemoDefaults.SYMBOLOGY_MSI_MIN_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putIntWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_msi) + context.getString(R.string.key_max_length),
            config?.decoderConfig?.Msi?.maximumLength ?: DemoDefaults.SYMBOLOGY_MSI_MAX_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putStringWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_msi) + context.getString(R.string.key_checksum_type),
            config?.decoderConfig?.Msi?.checksumType?.ordinal?.toString()
                ?: DemoDefaults.SYMBOLOGY_MSI_CHK_DEFAULT.ordinal.toString(),
            onlyIfNotContains
        )
        prefsEditor.putBooleanWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_upca),
            config?.decoderConfig?.UpcA?.enabled ?: DemoDefaults.SYMBOLOGY_UPCA_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putBooleanWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_upce),
            config?.decoderConfig?.UpcE?.enabled ?: DemoDefaults.SYMBOLOGY_UPCE_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putBooleanWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_upce) + context.getString(
                R.string.key_expand_to_upca
            ),
            config?.decoderConfig?.UpcE?.expandToUPCA
                ?: DemoDefaults.SYMBOLOGY_UPCE_EXPAND_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putBooleanWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_upce1),
            config?.decoderConfig?.UpcE1?.enabled ?: DemoDefaults.SYMBOLOGY_UPCE1_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putBooleanWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_upce1) + context.getString(
                R.string.key_expand_to_upca
            ),
            config?.decoderConfig?.UpcE1?.expandToUPCA
                ?: DemoDefaults.SYMBOLOGY_UPCE1_EXPAND_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putBooleanWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_ean13),
            config?.decoderConfig?.Ean13?.enabled ?: DemoDefaults.SYMBOLOGY_EAN13_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putBooleanWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_ean8),
            config?.decoderConfig?.Ean8?.enabled ?: DemoDefaults.SYMBOLOGY_EAN8_DEFAULT,
            onlyIfNotContains
        )
        if(scanMode == ScanMode.ALL_2D) {
            prefsEditor.putBooleanWithOptions(
                sharedPrefs,
                context.getString(R.string.key_symbology_pdf417),
                config?.decoderConfig?.PDF417?.enabled ?: false,
                onlyIfNotContains
            )
        } else {
            prefsEditor.putBooleanWithOptions(
                sharedPrefs,
                context.getString(R.string.key_symbology_pdf417),
                config?.decoderConfig?.PDF417?.enabled ?: DemoDefaults.SYMBOLOGY_PDF417_DEFAULT,
                onlyIfNotContains
            )
        }

        if(scanMode == ScanMode.ALL_2D) {
            prefsEditor.putBooleanWithOptions(
                sharedPrefs,
                context.getString(R.string.key_symbology_pdf417_micro),
                config?.decoderConfig?.PDF417Micro?.enabled
                    ?: false,
                onlyIfNotContains
            )
        } else {
            prefsEditor.putBooleanWithOptions(
                sharedPrefs,
                context.getString(R.string.key_symbology_pdf417_micro),
                config?.decoderConfig?.PDF417Micro?.enabled
                    ?: DemoDefaults.SYMBOLOGY_PDF417_MICRO_DEFAULT,
                onlyIfNotContains
            )
        }

        prefsEditor.putBooleanWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_datamatrix),
            config?.decoderConfig?.Datamatrix?.enabled ?: DemoDefaults.SYMBOLOGY_DM_DEFAULT,
            onlyIfNotContains
        )
        if(scanMode == ScanMode.CONTINUOUS || scanMode == ScanMode.ANYSCAN) {
            prefsEditor.putBooleanWithOptions(
                sharedPrefs,
                context.getString(R.string.key_symbology_dotcode),
                config?.decoderConfig?.Dotcode?.enabled ?: false,
                onlyIfNotContains
            )
        } else {
            prefsEditor.putBooleanWithOptions(
                sharedPrefs,
                context.getString(R.string.key_symbology_dotcode),
                config?.decoderConfig?.Dotcode?.enabled ?: DemoDefaults.SYMBOLOGY_DOTCODE_DEFAULT,
                onlyIfNotContains
            )
        }

        prefsEditor.putBooleanWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_c25),
            config?.decoderConfig?.Code25?.enabled ?: DemoDefaults.SYMBOLOGY_C25_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putBooleanWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_databar14),
            config?.decoderConfig?.Databar14?.enabled ?: true,
            onlyIfNotContains
        )
        prefsEditor.putBooleanWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_databarLimited),
            config?.decoderConfig?.DatabarLimited?.enabled ?: true,
            onlyIfNotContains
        )
        prefsEditor.putBooleanWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_databarExpanded),
            config?.decoderConfig?.DatabarExpanded?.enabled ?: true,
            onlyIfNotContains
        )
        prefsEditor.putIntWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_c25) + context.getString(R.string.key_min_length),
            config?.decoderConfig?.Code25?.minimumLength
                ?: DemoDefaults.SYMBOLOGY_C25_MIN_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putIntWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_c25) + context.getString(R.string.key_max_length),
            config?.decoderConfig?.Code25?.maximumLength
                ?: DemoDefaults.SYMBOLOGY_C25_MAX_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putStringWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_c25) + context.getString(R.string.key_checksum_type),
            config?.decoderConfig?.Code25?.checksumType?.ordinal?.toString()
                ?: DemoDefaults.SYMBOLOGY_C25_CHK_DEFAULT.ordinal.toString(),
            onlyIfNotContains
        )
        prefsEditor.putBooleanWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_i2o5),
            config?.decoderConfig?.Interleaved25?.enabled ?: DemoDefaults.SYMBOLOGY_I2O5_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putIntWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_i2o5) + context.getString(R.string.key_min_length),
            config?.decoderConfig?.Interleaved25?.minimumLength
                ?: DemoDefaults.SYMBOLOGY_I2O5_MIN_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putIntWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_i2o5) + context.getString(R.string.key_max_length),
            config?.decoderConfig?.Interleaved25?.maximumLength
                ?: DemoDefaults.SYMBOLOGY_I2O5_MAX_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putStringWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_i2o5) + context.getString(R.string.key_checksum_type),
            config?.decoderConfig?.Interleaved25?.checksumType?.ordinal?.toString()
                ?: DemoDefaults.SYMBOLOGY_I2O5_CHK_DEFAULT.ordinal.toString(),
            onlyIfNotContains
        )
        prefsEditor.putBooleanWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_itf14),
            config?.decoderConfig?.ITF14?.enabled ?: DemoDefaults.SYMBOLOGY_ITF14_DEFAULT,
            onlyIfNotContains
        )
        if(scanMode == ScanMode.INDUSTRIAL_1D || scanMode == ScanMode.MISSHAPED_1D || scanMode == ScanMode.ALL_1D) {
            prefsEditor.putBooleanWithOptions(
                sharedPrefs,
                context.getString(R.string.key_symbology_iata25),
                config?.decoderConfig?.IATA25?.enabled ?: false,
                onlyIfNotContains
            )
        } else {
            prefsEditor.putBooleanWithOptions(
                sharedPrefs,
                context.getString(R.string.key_symbology_iata25),
                config?.decoderConfig?.IATA25?.enabled ?: DemoDefaults.SYMBOLOGY_IATA25_DEFAULT,
                onlyIfNotContains
            )
        }

        prefsEditor.putIntWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_iata25) + context.getString(R.string.key_min_length),
            config?.decoderConfig?.IATA25?.minimumLength
                ?: DemoDefaults.SYMBOLOGY_IATA25_MIN_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putIntWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_iata25) + context.getString(R.string.key_max_length),
            config?.decoderConfig?.IATA25?.maximumLength
                ?: DemoDefaults.SYMBOLOGY_IATA25_MAX_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putStringWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_iata25) + context.getString(R.string.key_checksum_type),
            config?.decoderConfig?.IATA25?.checksumType?.ordinal?.toString()
                ?: DemoDefaults.SYMBOLOGY_IATA25_CHK_DEFAULT.ordinal.toString(),
            onlyIfNotContains
        )
        if(scanMode == ScanMode.INDUSTRIAL_1D || scanMode == ScanMode.MISSHAPED_1D || scanMode == ScanMode.ALL_1D) {
            prefsEditor.putBooleanWithOptions(
                sharedPrefs,
                context.getString(R.string.key_symbology_matrix25),
                config?.decoderConfig?.Matrix25?.enabled ?: false,
                onlyIfNotContains
            )
        } else {
            prefsEditor.putBooleanWithOptions(
                sharedPrefs,
                context.getString(R.string.key_symbology_matrix25),
                config?.decoderConfig?.Matrix25?.enabled ?: DemoDefaults.SYMBOLOGY_MATRIX25_DEFAULT,
                onlyIfNotContains
            )
        }

        prefsEditor.putIntWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_matrix25) + context.getString(R.string.key_min_length),
            config?.decoderConfig?.Matrix25?.minimumLength
                ?: DemoDefaults.SYMBOLOGY_MATRIX25_MIN_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putIntWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_matrix25) + context.getString(R.string.key_max_length),
            config?.decoderConfig?.Matrix25?.maximumLength
                ?: DemoDefaults.SYMBOLOGY_MATRIX25_MAX_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putStringWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_matrix25) + context.getString(R.string.key_checksum_type),
            config?.decoderConfig?.Matrix25?.checksumType?.ordinal?.toString()
                ?: DemoDefaults.SYMBOLOGY_MATRIX25_CHK_DEFAULT.ordinal.toString(),
            onlyIfNotContains
        )
        prefsEditor.putBooleanWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_dataLogic25),
            config?.decoderConfig?.Datalogic25?.enabled
                ?: DemoDefaults.SYMBOLOGY_DATALOGIC25_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putIntWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_dataLogic25) + context.getString(R.string.key_min_length),
            config?.decoderConfig?.Datalogic25?.minimumLength
                ?: DemoDefaults.SYMBOLOGY_DATALOGIC25_MIN_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putIntWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_dataLogic25) + context.getString(R.string.key_max_length),
            config?.decoderConfig?.Datalogic25?.maximumLength
                ?: DemoDefaults.SYMBOLOGY_DATALOGIC25_MAX_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putStringWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_dataLogic25) + context.getString(R.string.key_checksum_type),
            config?.decoderConfig?.Datalogic25?.checksumType?.ordinal?.toString()
                ?: DemoDefaults.SYMBOLOGY_DATALOGIC25_CHK_DEFAULT.ordinal.toString(),
            onlyIfNotContains
        )
        if(scanMode == ScanMode.INDUSTRIAL_1D || scanMode == ScanMode.MISSHAPED_1D || scanMode == ScanMode.ALL_1D) {
            prefsEditor.putBooleanWithOptions(
                sharedPrefs,
                context.getString(R.string.key_symbology_coop25),
                config?.decoderConfig?.COOP25?.enabled ?: false,
                onlyIfNotContains
            )
        } else {
            prefsEditor.putBooleanWithOptions(
                sharedPrefs,
                context.getString(R.string.key_symbology_coop25),
                config?.decoderConfig?.COOP25?.enabled ?: DemoDefaults.SYMBOLOGY_COOP25_DEFAULT,
                onlyIfNotContains
            )
        }

        prefsEditor.putIntWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_coop25) + context.getString(R.string.key_min_length),
            config?.decoderConfig?.COOP25?.minimumLength
                ?: DemoDefaults.SYMBOLOGY_COOP25_MIN_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putIntWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_coop25) + context.getString(R.string.key_max_length),
            config?.decoderConfig?.COOP25?.maximumLength
                ?: DemoDefaults.SYMBOLOGY_COOP25_MAX_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putStringWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_coop25) + context.getString(R.string.key_checksum_type),
            config?.decoderConfig?.COOP25?.checksumType?.ordinal?.toString()
                ?: DemoDefaults.SYMBOLOGY_COOP25_CHK_DEFAULT.ordinal.toString(),
            onlyIfNotContains
        )
        if(scanMode == ScanMode.INDUSTRIAL_1D || scanMode == ScanMode.MISSHAPED_1D || scanMode == ScanMode.ALL_1D) {
            prefsEditor.putBooleanWithOptions(
                sharedPrefs,
                context.getString(R.string.key_symbology_c32),
                config?.decoderConfig?.Code32?.enabled ?: false,
                onlyIfNotContains
            )
        } else {
            prefsEditor.putBooleanWithOptions(
                sharedPrefs,
                context.getString(R.string.key_symbology_c32),
                config?.decoderConfig?.Code32?.enabled ?: DemoDefaults.SYMBOLOGY_C32_DEFAULT,
                onlyIfNotContains
            )
        }

        prefsEditor.putIntWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_c32) + context.getString(R.string.key_min_length),
            config?.decoderConfig?.Code32?.minimumLength
                ?: DemoDefaults.SYMBOLOGY_C32_MIN_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putIntWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_c32) + context.getString(R.string.key_max_length),
            config?.decoderConfig?.Code32?.maximumLength
                ?: DemoDefaults.SYMBOLOGY_C32_MAX_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putBooleanWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_telepen),
            config?.decoderConfig?.Telepen?.enabled ?: DemoDefaults.SYMBOLOGY_TELEPEN_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putIntWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_telepen) + context.getString(R.string.key_min_length),
            config?.decoderConfig?.Telepen?.minimumLength
                ?: DemoDefaults.SYMBOLOGY_TELEPEN_MIN_DEFAULT,
            onlyIfNotContains
        )
        prefsEditor.putIntWithOptions(
            sharedPrefs,
            context.getString(R.string.key_symbology_telepen) + context.getString(R.string.key_max_length),
            config?.decoderConfig?.Telepen?.maximumLength
                ?: DemoDefaults.SYMBOLOGY_TELEPEN_MAX_DEFAULT,
            onlyIfNotContains
        )


        //endregion Barcode Types

        //region Result
        if(scanMode == ScanMode.PDF) {
            prefsEditor.putStringWithOptions(
                sharedPrefs,
                context.getString(R.string.key_result_parser),
                config?.decoderConfig?.formattingType?.ordinal?.toString()
                    ?: DemoDefaults.PARSER_TYPE_PDF_DEFAULT.ordinal.toString(),
                onlyIfNotContains
            )
        }
        else if(scanMode == ScanMode.GALLERY_SCAN) {
            prefsEditor.putStringWithOptions(
                sharedPrefs,
                context.getString(R.string.key_result_parser),
                config?.decoderConfig?.formattingType?.ordinal?.toString()
                    ?: DemoDefaults.PARSER_TYPE_GALLERY_DEFAULT.ordinal.toString(),
                onlyIfNotContains
            )
        }
        else
         {
            prefsEditor.putStringWithOptions(
                sharedPrefs,
                context.getString(R.string.key_result_parser),
                config?.decoderConfig?.formattingType?.ordinal?.toString()
                    ?: DemoDefaults.PARSER_TYPE_DEFAULT.ordinal.toString(),
                onlyIfNotContains
            )
        }

        prefsEditor.putStringWithOptions(
            sharedPrefs,
            context.getString(
                R.string.key_result_charset
            ),
            config?.decoderConfig?.encodingCharacterSet ?: DemoDefaults.RESULT_CHARSET_DEFAULT,
            onlyIfNotContains
        )

        prefsEditor.putStringWithOptions(
            sharedPrefs,
            context.getString(R.string.key_result_searchEngine),
            DemoDefaults.SEARCH_ENGINE_BROWSER_DEFAULT,
            onlyIfNotContains
        )

        //endregion Result

        // Weebhook
        prefsEditor.putBooleanWithOptions(
            sharedPrefs,
            context.getString(R.string.key_webhook_autosend),
            DemoDefaults.WEBHOOK_AUTOSEND_DEFAULT,
            onlyIfNotContains
        )

        prefsEditor.putBooleanWithOptions(
            sharedPrefs,
            context.getString(R.string.key_webhook_feedback),
            DemoDefaults.WEBHOOK_FEEDBACK_DEFAULT,
            onlyIfNotContains
        )

        prefsEditor.putBooleanWithOptions(
            sharedPrefs,
            context.getString(R.string.key_webhook_encode_data),
            DemoDefaults.WEBHOOK_ENCODE_DEFAULT,
            onlyIfNotContains
        )

        prefsEditor.apply()
    }

    private fun adaptConfigForContinuesScanning(config: BarkoderConfig) {
        // Hardcoded values that comes from Z

        config.decoderConfig.maximumResultsCount = 200
        config.decoderConfig.duplicatesDelayMs = 0
        BarkoderConfig.SetMulticodeCachingEnabled(true)
        BarkoderConfig.SetMulticodeCachingDuration(1000)
        config.isCloseSessionOnResultEnabled = false
    }

    private fun adaptConfigForDeblurScanning(config: BarkoderConfig) {
        config.barkoderResolution = BarkoderResolution.FHD
        config.decoderConfig.decodingSpeed = Barkoder.DecodingSpeed.Slow
        config.decoderConfig.upcEanDeblur = true
        config.decoderConfig.Aztec.enabled = false
        config.decoderConfig.AztecCompact.enabled = false
        config.decoderConfig.QR.enabled = false
        config.decoderConfig.QRMicro.enabled = false
        config.decoderConfig.PDF417.enabled = false
        config.decoderConfig.Datamatrix.enabled = false
        config.decoderConfig.Dotcode.enabled = false
        config.decoderConfig.Code93.enabled = false
        config.decoderConfig.Code32.enabled = false
        config.decoderConfig.Codabar.enabled = false
        config.decoderConfig.Msi.enabled = false
        config.decoderConfig.Code39.enabled = false
        config.decoderConfig.Interleaved25.enabled = false
        config.decoderConfig.ITF14.enabled = false
        config.decoderConfig.IATA25.enabled = false
        config.decoderConfig.Matrix25.enabled = false
        config.decoderConfig.Datalogic25.enabled = false
        config.decoderConfig.COOP25.enabled = false
        config.decoderConfig.Telepen.enabled = false
        config.decoderConfig.Code11.enabled = false
        config.decoderConfig.Telepen.enabled = false
        config.decoderConfig.Code25.enabled = false
        config.decoderConfig.PDF417Micro.enabled = false
        config.decoderConfig.Code128.enabled = false
        config.isVibrateOnSuccessEnabled = false
    }

    private fun adaptConfigForMisshapedScanning(config: BarkoderConfig) {
        config.barkoderResolution = BarkoderResolution.FHD
        config.decoderConfig.decodingSpeed = Barkoder.DecodingSpeed.Slow
        config.decoderConfig.enableMisshaped1D = true
        config.decoderConfig.Aztec.enabled = false
        config.decoderConfig.AztecCompact.enabled = false
        config.decoderConfig.QR.enabled = false
        config.decoderConfig.QRMicro.enabled = false
        config.decoderConfig.PDF417.enabled = false
        config.decoderConfig.Datamatrix.enabled = false
        config.decoderConfig.Dotcode.enabled = false
        config.decoderConfig.UpcE.enabled = false
        config.decoderConfig.UpcE1.enabled = false
        config.decoderConfig.UpcA.enabled = false
        config.decoderConfig.Ean8.enabled = false
        config.decoderConfig.Ean13.enabled = false
    }

    private fun adaptConfigDisableMultiScan(config: BarkoderConfig) {
        // Hardcoded values that comes from Z

        config.decoderConfig.maximumResultsCount = 1

    }


}
