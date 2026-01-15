package com.barkoder.demoscanner.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import androidx.preference.SwitchPreferenceCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.barkoder.Barkoder
import com.barkoder.demoscanner.MainActivity
import com.barkoder.demoscanner.R
import com.barkoder.demoscanner.ScannerActivity
import com.barkoder.demoscanner.SettingsActivity
import com.barkoder.demoscanner.customcontrols.CategoryRoundedCornersDecoration
import com.barkoder.demoscanner.customcontrols.MarginDividerItemDecoration
import com.barkoder.demoscanner.customcontrols.PreferenceCategoryWithPadding
import com.barkoder.demoscanner.customcontrols.PreferenceCategoryWithPaddingGreyText
import com.barkoder.demoscanner.customcontrols.SwitchWithPaddingPreference
import com.barkoder.demoscanner.customcontrols.SwitchWithWidgetPreference
import com.barkoder.demoscanner.customcontrols.WhiteBackgroundPreference
import com.barkoder.demoscanner.customcontrols.WhiteBackgroundPreferenceWithArrow
import com.barkoder.demoscanner.enums.Charset
import com.barkoder.demoscanner.enums.ScanMode
import com.barkoder.demoscanner.utils.BKDConfigUtil
import com.barkoder.demoscanner.utils.CommonUtil.dpToPx
import com.barkoder.demoscanner.utils.DemoDefaults
import com.barkoder.demoscanner.utils.getBoolean
import com.barkoder.demoscanner.utils.getString
import com.barkoder.enums.BarkoderARHeaderShowMode
import com.barkoder.enums.BarkoderARLocationType
import com.barkoder.enums.BarkoderARMode
import com.barkoder.enums.BarkoderConfigTemplate
import com.barkoder.enums.BarkoderResolution
import com.barkoder.overlaymanager.BarkoderAROverlayRefresh
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.internal.ViewUtils.dpToPx
import kotlin.or

class SettingsFragment : PreferenceFragmentCompat() {

    companion object {
        const val ARGS_MODE_KEY = "settingsScanMode"
    }

    private lateinit var scanMode: ScanMode
    private lateinit var sharedPreferences : SharedPreferences
    private val PREFS_NAME = "MyPrefsFile"

    private val BARKODER_SETTINGS_CATEGORY_INDEX = 1
    private val BARKODER_AR_SETTINGS = 2
    private val BARCODE_TYPES_CATEGORY_INDEX = 3
    private val RESULT_CATEGORY_INDEX = 4
    private val WEEBHOOK_SETTINGS_CATEGORY_INDEX = 5
    private val GENERAL_SETTINGS_CATEGORY_INDEX = 6
    private val CAMERA_SETTINGS_CATEGORY_INDEX = 7
    private val INDIVIDUAL_TEMPLATES_SETTINGS_CATEGORY_INDEX = 8

    lateinit var continuisTresHoldPreferences : ListPreference

    private lateinit var webhookConfigurationPreference: WhiteBackgroundPreferenceWithArrow
    private lateinit var webhookAutosendPreference: SwitchPreference
    private lateinit var webhookFeedbackPreference: SwitchWithPaddingPreference
    private lateinit var webhookEncodeDataPreference: SwitchWithPaddingPreference
    private lateinit var defaultSearchWebPreference: ListPreference


    private val generalSettingsGroup by lazy {
        preferenceScreen.getPreference(GENERAL_SETTINGS_CATEGORY_INDEX)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        view?.setBackgroundResource(R.drawable.bg_shape_png) // Set your drawable or color here
        return view
    }
    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(android.R.id.list)
        recyclerView?.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_shape_png)
        recyclerView?.setBackgroundColor(Color.TRANSPARENT)
        val recyclerView2 = view.findViewById<RecyclerView>(androidx.preference.R.id.recycler_view)
        val marginStartPx = dpToPx(requireContext(), 20).toInt()
        val marginEndPx = dpToPx(requireContext(), 20).toInt()

        recyclerView2?.addItemDecoration(
            MarginDividerItemDecoration(
                requireContext(),
                R.drawable.preference_divider,
                marginStartPx,
                marginEndPx
            )
        )

        recyclerView2?.apply {
            // Add your rounded corner decorator
            addItemDecoration(CategoryRoundedCornersDecoration(requireContext()))

            val bottomPaddingPx = dpToPx(requireContext(), 50).toInt()
            setPaddingRelative(paddingStart, paddingTop, paddingEnd, bottomPaddingPx)
            // Optionally add other decoration
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        scanMode = ScanMode.values()[requireActivity().intent.extras!!.getInt(ARGS_MODE_KEY)]


        if (scanMode != ScanMode.GLOBAL)
            preferenceManager.sharedPreferencesName =
                requireActivity().packageName + scanMode.prefKey

        val config = MainActivity.barkoderConfig

        BKDConfigUtil.configureBKD(requireContext(), scanMode)
        setPreferencesFromResource(R.xml.preferences_settings, rootKey)



        searchEnginePreferenceChangeListener()
        copyTerminatorPreferenceChangeListener()
        setSymbologyPrefs()
        onClickTemplateSettings()
        settingsChangedTemplateMessage()
        onEnabledWebhookListener()
        onEnableSearchListener()


        if(scanMode != ScanMode.ANYSCAN && scanMode != ScanMode.CONTINUOUS) {
            flattenSymbologyPreferences()
        }

        if (scanMode.template != null) {
            setUIForScanModeWithTemplate()
            setDecodingSpeedEntries()
            setARModeEntries()
            setARLocationTypeEntries()
            setARHeaderShowModeEntries()
            setAROverlayFPSEntries()
            setBarkoderResolutionEntries()
            setResultParserEntries()
            setResultCharsetEntries()
            setThreshHoldContiniousEntries()
            onARModeSwitchListener()
            onContiniousModeOnListener()
            setDynamicExposureEntries()
            findPreference<Preference>(getString(R.string.key_reset_config))!!.setOnPreferenceClickListener {
                showResetConfigConfirmationDialog()
                false
            }
        } else {
            defaultSearchEngine()
            defaultCopyTerminator()
            setDecodingSpeedEntries()
            setARModeEntries()
            setARLocationTypeEntries()
            setARHeaderShowModeEntries()
            setAROverlayFPSEntries()
            setBarkoderResolutionEntries()
            setResultParserEntries()
            setResultCharsetEntries()
            openWebHookConfigurationDialog()
            showTutorialDialogs()
            setThreshHoldContiniousEntries()
            onARModeSwitchListener()
            onContiniousModeOnListener()
            setDynamicExposureEntries()

            findPreference<Preference>(getString(R.string.key_reset_config))!!.setOnPreferenceClickListener {
                showResetConfigConfirmationDialog()
                false
            }

            findPreference<Preference>(getString(R.string.key_reset_all_settings))!!.setOnPreferenceClickListener {
                showALLResetConfirmationDialog()
                false
            }


            if (scanMode == ScanMode.CONTINUOUS)
                setUIForContinuosModeSettings()

        }

        makeSomeInvisiblePreferencesForTemplates()

        if(scanMode != ScanMode.ANYSCAN && scanMode != ScanMode.CONTINUOUS) {

            val barcodeCategorySettings = findPreference<PreferenceGroup>("pref_key_barkoder_settings")

            if(scanMode == ScanMode.MRZ) {

                val barkoderSettingsOrderMRZ = listOf(
                    "pref_key_scanner_decoding_speed",
                    "pref_key_scanner_resolution",
                    "pref_key_allow_pinch_to_zoom",
                    "pref_key_beep_on_success",
                    "pref_key_vibrate_on_success",
                    "pref_key_enable_roi",
                    "pref_key_require_master_checksum",
                    "pref_key_continuous_scanning",
                    "pref_key_continuous_treshold"
                )

                barcodeCategorySettings?.let { category ->
                    val visiblePrefs = mutableListOf<Preference>()
                    for (i in 0 until category.preferenceCount) {
                        val pref = category.getPreference(i)
                        if (pref.isVisible) {
                            visiblePrefs.add(pref)
                        }
                    }

                    var newOrder = 0
                    visiblePrefs.forEach { pref ->
                            newOrder = barkoderSettingsOrderMRZ.indexOf(pref.key)


                        if (newOrder != -1) {
                            pref.order = newOrder
                        } else {
                            // Assign a high order to preferences not in your custom list
                            pref.order = Int.MAX_VALUE
                        }
                    }
                }
            }
                val barcodeCategory = findPreference<PreferenceGroup>("barkode_types_key")

            barcodeCategory?.let { category ->
                val visiblePrefs = mutableListOf<Preference>()
                for (i in 0 until category.preferenceCount) {
                    val pref = category.getPreference(i)
                    if (pref.isVisible) {
                        visiblePrefs.add(pref)
                    }
                }


                // Assign a new order to each preference based on your custom list
                val customKeyOrderALL1D = listOf(
                    "pref_key_symbology_c128",
                    "pref_key_symbology_c93",
                    "pref_key_symbology_c39",
                    "pref_key_symbology_c25",
                    "pref_key_symbology_codabar",
                    "pref_key_symbology_c11",
                    "pref_key_symbology_msi",
                    "pref_key_symbology_c32",
                    "pref_key_symbology_i2o5",
                    "pref_key_symbology_itf14",
                    "pref_key_symbology_iata25",
                    "pref_key_symbology_matrix25",
                    "pref_key_symbology_dataLogic25",
                    "pref_key_symbology_coop25",
                    "pref_key_symbology_telepen",
                    "pref_key_symbology_upca",
                    "pref_key_symbology_upce",
                    "pref_key_symbology_upce1",
                    "pref_key_symbology_ean13",
                    "pref_key_symbology_ean8",
                    "pref_key_symbology_databar14",
                    "pref_key_symbology_databarLimited",
                    "pref_key_symbology_databarExpanded",
                    "pref_key_symbology_postalImb",
                    "pref_key_symbology_postnet",
                    "pref_key_symbology_planet",
                    "pref_key_symbology_australianPost",
                    "pref_key_symbology_royalMail",
                    "pref_key_symbology_kix",
                    "pref_key_symbology_japanesePost"
                )

                val customKeyOrderComposite = listOf(
                    "pref_key_symbology_pdf417",
                    "pref_key_symbology_pdf417_micro",
                    "pref_key_symbology_c128",
                    "pref_key_symbology_upca",
                    "pref_key_symbology_upce",
                    "pref_key_symbology_upce1",
                    "pref_key_symbology_ean13",
                    "pref_key_symbology_ean8",
                    "pref_key_symbology_databarExpanded",
                    "pref_key_symbology_databar14",
                    "pref_key_symbology_databarLimited",
                )
                val customKeyOrderALL2D = listOf(
                    "pref_key_symbology_aztec",
                    "pref_key_symbology_aztec_compact",
                    "pref_key_symbology_qr",
                    "pref_key_symbology_qr_micro",
                    "pref_key_symbology_pdf417",
                    "pref_key_symbology_pdf417_micro",
                    "pref_key_symbology_datamatrix",
                    "pref_key_symbology_dotcode",
                    "pref_key_symbology_maxicode",
                )

                val customKeyOrderVIN = listOf(
                    "pref_key_symbology_c39",
                    "pref_key_symbology_c128",
                    "pref_key_symbology_datamatrix",
                    "pref_key_symbology_qr_micro",
                    "pref_key_symbology_ocr"

                )

                val customKeyOrderPostal = listOf(
                    "pref_key_symbology_postalImb",
                    "pref_key_symbology_postnet",
                    "pref_key_symbology_planet",
                    "pref_key_symbology_australianPost",
                    "pref_key_symbology_royalMail",
                    "pref_key_symbology_kix",
                    "pref_key_symbology_japanesePost",
                )

                val customKeyOrderDeblur = listOf(
                    "pref_key_symbology_upca",
                    "pref_key_symbology_upce",
                    "pref_key_symbology_upce1",
                    "pref_key_symbology_ean13",
                    "pref_key_symbology_ean8",
                )

                val customKeyOrderMisshapped = listOf(
                    "pref_key_symbology_c128",
                    "pref_key_symbology_c93",
                    "pref_key_symbology_c39",
                    "pref_key_symbology_codabar",
                    "pref_key_symbology_c11",
                    "pref_key_symbology_msi",
                    "pref_key_symbology_c25",
                    "pref_key_symbology_i2o5",
                    "pref_key_symbology_itf14",
                    "pref_key_symbology_iata25",
                    "pref_key_symbology_matrix25",
                    "pref_key_symbology_dataLogic25",
                    "pref_key_symbology_coop25",
                    "pref_key_symbology_c32",
                    "pref_key_symbology_telepen",
                )

                val customKeyOrderArMode = listOf(
                    "pref_key_symbology_aztec",
                    "pref_key_symbology_aztec_compact",
                    "pref_key_symbology_qr",
                    "pref_key_symbology_qr_micro",
                    "pref_key_symbology_pdf417",
                    "pref_key_symbology_pdf417_micro",
                    "pref_key_symbology_datamatrix",
                    "pref_key_symbology_maxicode",
                    "pref_key_symbology_c128",
                    "pref_key_symbology_c93",
                    "pref_key_symbology_c39",
                    "pref_key_symbology_codabar",
                    "pref_key_symbology_c11",
                    "pref_key_symbology_msi",
                    "pref_key_symbology_upca",
                    "pref_key_symbology_upce",
                    "pref_key_symbology_upce1",
                    "pref_key_symbology_ean13",
                    "pref_key_symbology_ean8",
                    "pref_key_symbology_c25",
                    "pref_key_symbology_i2o5",
                    "pref_key_symbology_itf14",
                    "pref_key_symbology_iata25",
                    "pref_key_symbology_matrix25",
                    "pref_key_symbology_dataLogic25",
                    "pref_key_symbology_coop25",
                    "pref_key_symbology_c32",
                    "pref_key_symbology_telepen",
                    "pref_key_symbology_databar14",
                    "pref_key_symbology_databarLimited",
                    "pref_key_symbology_databarExpanded",
                    "pref_key_symbology_postalImb",
                    "pref_key_symbology_postnet",
                    "pref_key_symbology_planet",
                    "pref_key_symbology_australianPost",
                    "pref_key_symbology_royalMail",
                    "pref_key_symbology_kix",
                    "pref_key_symbology_japanesePost",
                )

                var newOrder = 0
                visiblePrefs.forEach { pref ->
                    if (scanMode == ScanMode.COMPOSITE) {
                        newOrder = customKeyOrderComposite.indexOf(pref.key)
                    } else if (scanMode == ScanMode.ALL_2D) {
                        newOrder = customKeyOrderALL2D.indexOf(pref.key)
                    } else if (scanMode == ScanMode.ALL_1D || scanMode == ScanMode.INDUSTRIAL_1D || scanMode == ScanMode.RETAIL_1D) {
                        newOrder = customKeyOrderALL1D.indexOf(pref.key)
                    } else if (scanMode == ScanMode.VIN) {
                        newOrder = customKeyOrderVIN.indexOf(pref.key)
                    } else if (scanMode == ScanMode.POSTAL_CODES) {
                        newOrder = customKeyOrderPostal.indexOf(pref.key)
                    } else if (scanMode == ScanMode.UPC_EAN_DEBLUR) {
                        newOrder = customKeyOrderDeblur.indexOf(pref.key)
                    } else if (scanMode == ScanMode.MISSHAPED_1D) {
                        newOrder = customKeyOrderMisshapped.indexOf(pref.key)
                    }  else if (scanMode == ScanMode.AR_MODE) {
                        newOrder = customKeyOrderArMode.indexOf(pref.key)
                    }


                    if (newOrder != -1) {
                        pref.order = newOrder
                    } else {
                        // Assign a high order to preferences not in your custom list
                        pref.order = Int.MAX_VALUE
                    }
                }

                Log.d("ReorderDebug", "Successfully reordered preferences.")
            }

        }




    }


    private fun flattenSymbologyPreferences() {
        val outerCategory = preferenceScreen.findPreference<PreferenceCategoryWithPadding>("barkode_types_key")
        if (outerCategory == null) {
            Log.e("PrefsDebug", "Outer category not found!")
            return
        }



        val flattenedPreferences = mutableListOf<Preference>()

        for (i in outerCategory.preferenceCount - 1 downTo 0) {
            val subPref = outerCategory.getPreference(i)


            if (subPref is PreferenceCategory) {


                for (j in subPref.preferenceCount - 1 downTo 0) {
                    val innerPref = subPref.getPreference(j)
                    flattenedPreferences.add(innerPref)
                    subPref.removePreference(innerPref)
                }
                outerCategory.removePreference(subPref)

            }
        }

        flattenedPreferences.reverse() // Maintain original order
        flattenedPreferences.forEach { pref ->
            if (pref.parent != null) {
                (pref.parent as PreferenceGroup).removePreference(pref)
            }
            outerCategory.addPreference(pref)
        }

    }

    private fun onEnabledWebhookListener() {
        // Find preferences by exact keys matching your XML
        val enabledWebhookPreference = findPreference<SwitchPreference>(getString(R.string.key_enable_webhook))
        webhookConfigurationPreference = findPreference(getString(R.string.key_webhook_configuration))!!
        webhookFeedbackPreference = findPreference(getString(R.string.key_webhook_feedback))!!
        webhookEncodeDataPreference = findPreference(getString(R.string.key_webhook_encode_data))!!

        // Set initial enabled state based on the current switch state
        val isEnabled = enabledWebhookPreference?.isChecked ?: false
        setWebhookPreferencesEnabled(isEnabled)

        enabledWebhookPreference?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val enabled = newValue as Boolean
            // Log to confirm listener triggers
            setWebhookPreferencesEnabled(enabled)

            // Optional: Show dialog if enabling but URL is empty


            true // Allow change
        }


        enabledWebhookPreference?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            setWebhookPreferencesEnabled(enabledWebhookPreference?.isChecked!!)
            sharedPreferences = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            // Or if you are using a custom file name
            val urlWebHook = sharedPreferences.getString(getString(R.string.key_url_webhook), "") ?: ""
            if (urlWebHook.isBlank() && enabledWebhookPreference?.isChecked!!) {
                val notConfiguredWebHookDialog = NotConfiguredWebHookDialog()
                notConfiguredWebHookDialog.show(parentFragmentManager, "NotConfiguredWebHookDialog")
            }
            true
        }
    }

    private fun setWebhookPreferencesEnabled(enabled: Boolean) {
        webhookConfigurationPreference.isEnabled = enabled
        webhookFeedbackPreference.isEnabled = enabled
        webhookEncodeDataPreference.isEnabled = enabled
    }

    private fun onEnableSearchListener() {
        var  enabledSearchPreference = findPreference<SwitchPreference>("pref_key_enable_searchweb")
        defaultSearchWebPreference = findPreference(getString(R.string.key_result_searchEngine))!!
        if(enabledSearchPreference!!.isChecked){
            defaultSearchWebPreference.isEnabled = true
        } else {
            defaultSearchWebPreference.isEnabled = false
        }
        enabledSearchPreference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            val valueBoolean = newValue as Boolean
            if(!valueBoolean) {
                defaultSearchWebPreference.isEnabled = false
            } else {
                defaultSearchWebPreference.isEnabled = true
            }

            true
        }
    }

    private fun onContiniousModeOnListener () {
        var continuiusModePreference = findPreference<SwitchWithPaddingPreference>("pref_key_continuous_scanning")
        continuisTresHoldPreferences = findPreference<ListPreference>("pref_key_continuous_treshold")!!
        val arModePreference = findPreference<ListPreference>("pref_key_ar_mode_options")!!

//        continuiusModePreference!!.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
//            val isVisible = newValue as Boolean
//            if (isVisible) {
//                Log.d("asdada", arModePreference.value.toString())
//                if(arModePreference.value.toInt() == 0) {
//                    continuisTresHoldPreferences.isVisible = true
//                }
//
//                if(scanMode != ScanMode.AR_MODE  && scanMode != ScanMode.MISSHAPED_1D && scanMode != ScanMode.DPM && scanMode != ScanMode.DOTCODE && scanMode != ScanMode.MRZ) {
//                    makePreferenceVisable(getString(R.string.key_ar_preference))
//                }
//            } else {
//                continuisTresHoldPreferences.isVisible = false
//                if(scanMode != ScanMode.AR_MODE  && scanMode != ScanMode.MISSHAPED_1D && scanMode != ScanMode.DPM && scanMode != ScanMode.DOTCODE) {
//                    makePreferenceInvisible(getString(R.string.key_ar_preference))
//                }
//            }
//            true
//        }

        continuiusModePreference?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val isEnabled = continuiusModePreference?.isChecked



            // Perform any action you need
            if (isEnabled!!) {
                if(arModePreference.value.toInt() == 0) {
                    continuisTresHoldPreferences.isVisible = true
                }
//
                if(scanMode != ScanMode.AR_MODE  && scanMode != ScanMode.MISSHAPED_1D && scanMode != ScanMode.DPM && scanMode != ScanMode.DOTCODE && scanMode != ScanMode.MRZ) {
                    makePreferenceVisable(getString(R.string.key_ar_preference))
                }
                // Enable related feature
            } else {
                continuisTresHoldPreferences.isVisible = false
                if(scanMode != ScanMode.AR_MODE  && scanMode != ScanMode.MISSHAPED_1D && scanMode != ScanMode.DPM && scanMode != ScanMode.DOTCODE) {
                    makePreferenceInvisible(getString(R.string.key_ar_preference))
                }
            }



            true // Return true to indicate click was handled
        }


        if(continuiusModePreference!!.isChecked == true) {
            if(arModePreference.value.toInt() == 0) {
                continuisTresHoldPreferences.isVisible = true
            }
            if(scanMode != ScanMode.AR_MODE  && scanMode != ScanMode.MISSHAPED_1D && scanMode != ScanMode.DPM && scanMode != ScanMode.DOTCODE) {
                makePreferenceVisable(getString(R.string.key_ar_preference))
            }

        } else {
            continuisTresHoldPreferences.isVisible = false
            if(scanMode != ScanMode.AR_MODE  && scanMode != ScanMode.MISSHAPED_1D && scanMode != ScanMode.DPM && scanMode != ScanMode.DOTCODE) {
                makePreferenceInvisible(getString(R.string.key_ar_preference))
            }
        }


        if(scanMode == ScanMode.CONTINUOUS) {
            makePreferenceVisable(getString(R.string.key_ar_preference))
        }
    }

    private fun onARModeSwitchListener() {
        val arModePreference = findPreference<ListPreference>("pref_key_ar_mode_options")!!
        val arLocationTypePreference = findPreference<ListPreference>("pref_key_ar_location_type")!!
        val arHeaderShowModePreference = findPreference<ListPreference>("pref_key_ar_header_show_mode")!!
        val arOverlaySmoothnessPreference = findPreference<ListPreference>("pref_key_ar_overlay_fps")!!
        val arDoubleTapFreezePreference = findPreference<SwitchWithPaddingPreference>("pref_key_double_tap_to_freez")!!
        val thresholdPreference = findPreference<ListPreference>("pref_key_continuous_treshold")!!


            arModePreference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                val selectedOrdinal = newValue.toString().toIntOrNull() ?: -1
                val isFirstOption = selectedOrdinal == BarkoderARMode.OFF.ordinal
                val isLastOption = selectedOrdinal == BarkoderARMode.NonInteractive.ordinal

                arLocationTypePreference.isVisible = !isFirstOption
                arHeaderShowModePreference.isVisible = !isFirstOption
                arOverlaySmoothnessPreference.isVisible = !isFirstOption
                arDoubleTapFreezePreference.isVisible = !isFirstOption && !isLastOption
                thresholdPreference.isVisible = isFirstOption

                true
            }

            // Initial value logic
            val selectedOrdinal = arModePreference.value?.toIntOrNull() ?: -1
            val isFirstOption = selectedOrdinal == BarkoderARMode.OFF.ordinal
            val isLastOption = selectedOrdinal == BarkoderARMode.NonInteractive.ordinal

            arLocationTypePreference.isVisible = !isFirstOption
            arHeaderShowModePreference.isVisible = !isFirstOption
            arOverlaySmoothnessPreference.isVisible = !isFirstOption
            arDoubleTapFreezePreference.isVisible = !isFirstOption && !isLastOption
            thresholdPreference.isVisible = isFirstOption

    }


    private fun settingsChangedTemplateMessage() {
        val barkoderSettingsCategory: PreferenceCategory? = findPreference("pref_key_barkoder_settings")
        val barkodeTypes2DSettingsCategory: PreferenceCategory? = findPreference("pref_key_barkode_types2D_settings")
        val barkodeTypes1DSettingsCategory: PreferenceCategory? = findPreference("pref_key_barkode_types1D_settings")
        val barkodeResultsSettingsCategory: PreferenceCategory? = findPreference("pref_key_barkode_result_settings")


            barkoderSettingsCategory?.let {
                for (i in 0 until it.preferenceCount) {
                    val preference2: Preference = it.getPreference(i)

                    preference2.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
                        val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        val editor: SharedPreferences.Editor = sharedPreferences.edit()
                        editor.putBoolean("settingsChangedBoolean", true)
                        editor.apply()
                        true
                    }

                }
            }

            barkodeResultsSettingsCategory?.let {
                for (i in 0 until it.preferenceCount) {
                    val preference2: Preference = it.getPreference(i)

                    preference2.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
                        val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        val editor: SharedPreferences.Editor = sharedPreferences.edit()
                        editor.putBoolean("settingsChangedBoolean", true)
                        editor.apply()
                        true
                    }

                }
            }

        findPreference<Preference>(getString(R.string.key_continuous_scaning))!!.setOnPreferenceClickListener {
            val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor: SharedPreferences.Editor = sharedPreferences.edit()
            editor.putBoolean("settingsChangedBoolean", true)
            editor.apply()
            true
        }


            for (i in 0 until barkodeTypes2DSettingsCategory?.preferenceCount!!) {
                val preference = barkodeTypes2DSettingsCategory.getPreference(i)

                if (preference is SwitchWithPaddingPreference) {
                    preference.setOnSwitchStateChangeListener(object : SwitchWithPaddingPreference.OnSwitchStateChangeListener {
                        override fun onSwitchStateChanged(preference: SwitchWithPaddingPreference, newState: Boolean) {
                            val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            val editor: SharedPreferences.Editor = sharedPreferences.edit()
                            editor.putBoolean("settingsChangedBoolean", true)
                            editor.apply()
                        }
                    })
                }
            }

            for (i in 0 until barkodeTypes1DSettingsCategory?.preferenceCount!!) {
                val preference = barkodeTypes1DSettingsCategory.getPreference(i)

                if (preference is SwitchWithPaddingPreference) {
                    preference.setOnSwitchStateChangeListener(object : SwitchWithPaddingPreference.OnSwitchStateChangeListener {
                        override fun onSwitchStateChanged(preference: SwitchWithPaddingPreference, newState: Boolean) {
                            val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            val editor: SharedPreferences.Editor = sharedPreferences.edit()
                            editor.putBoolean("settingsChangedBoolean", true)
                            editor.apply()
                        }
                    })
                }
                if(preference is SwitchWithWidgetPreference) {
                    preference.setOnSwitchStateChangeListener(object : SwitchWithWidgetPreference.OnSwitchStateChangeListener {
                        override fun onSwitchStateChanged(
                            preference: SwitchWithWidgetPreference,
                            newState: Boolean
                        ) {
                            val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            val editor: SharedPreferences.Editor = sharedPreferences.edit()
                            editor.putBoolean("settingsChangedBoolean", true)
                            editor.apply()
                        }
                    })
                }
            }


        }


    private fun onClickTemplateSettings() {
        findPreference<Preference>(getString(R.string.key_All1D_settings))?.setOnPreferenceClickListener {
            openSettingsActivity(13)
            false
        }
        findPreference<Preference>(getString(R.string.key_1d_industrial_settings))?.setOnPreferenceClickListener {
            openSettingsActivity(0)
            false
        }
        findPreference<Preference>(getString(R.string.key_1d_retail_settings))?.setOnPreferenceClickListener {
            openSettingsActivity(1)
            false
        }
        findPreference<Preference>(getString(R.string.key_PDF_settings))?.setOnPreferenceClickListener {
            openSettingsActivity(2)
            false
        }
        findPreference<Preference>(getString(R.string.key_All2D_settings))?.setOnPreferenceClickListener {
            openSettingsActivity(4)
            false
        }
        findPreference<Preference>(getString(R.string.key_BatchMultiscan_settings))?.setOnPreferenceClickListener {
            openSettingsActivity(5)
            false
        }
        findPreference<Preference>(getString(R.string.key_anyscan_settings))?.setOnPreferenceClickListener {
            openSettingsActivity(6)
            false
        }
        findPreference<Preference>(getString(R.string.key_DPM_settings))?.setOnPreferenceClickListener {
            openSettingsActivity(8)
            false
        }
        findPreference<Preference>(getString(R.string.key_VIN_settings))?.setOnPreferenceClickListener {
            openSettingsActivity(9)
            false
        }
        findPreference<Preference>(getString(R.string.key_Dotcode_settings))?.setOnPreferenceClickListener {
            openSettingsActivity(10)
            false
        }
        findPreference<Preference>(getString(R.string.key_Deblur_settings))?.setOnPreferenceClickListener {
            openSettingsActivity(11)
            false
        }
        findPreference<Preference>(getString(R.string.key_Misshaped_settings))?.setOnPreferenceClickListener {
            openSettingsActivity(12)
            false
        }
        findPreference<Preference>(getString(R.string.key_mrz_settings))?.setOnPreferenceClickListener {
            openSettingsActivity(15)
            false
        }
        findPreference<Preference>(getString(R.string.key_gallery_scan_settings))?.setOnPreferenceClickListener {
            openSettingsActivity(16)
            false
        }
        findPreference<Preference>(getString(R.string.key_composite_settings))?.setOnPreferenceClickListener {
            openSettingsActivity(17)
            false
        }
        findPreference<Preference>(getString(R.string.key_postal_settings))?.setOnPreferenceClickListener {
            openSettingsActivity(18)
            false
        }
        findPreference<Preference>(getString(R.string.key_arMode_settings))?.setOnPreferenceClickListener {
            openSettingsActivity(7)
            false
        }
    }


    private fun openSettingsActivity(mode: Int) {
        val settingsIntent = Intent(requireActivity(), SettingsActivity::class.java)
        settingsIntent.putExtra(SettingsFragment.ARGS_MODE_KEY, mode)
        settingsIntent.putExtra("opened_from_settings", true)
        startActivity(settingsIntent)
    }

    override fun onStart() {
        super.onStart()

        requireActivity().title = if (scanMode == ScanMode.GLOBAL) {
            getString(R.string.activity_settings_title, " ")
        } else
            scanMode.title
    }

    private fun makeSomeInvisiblePreferencesForTemplates(){
        when(scanMode) {
            ScanMode.ANYSCAN ->{
                makePreferenceInvisible(getString(R.string.key_symbology_ocr))
                makePreferenceInvisible(getString(R.string.key_automatic_show_bottomsheet2))
                makePreferenceVisable(getString(R.string.key_composite_setting))
                makePreferenceInvisible(getString(R.string.key_narrow_viewfinder))
                makePreferenceInvisible(getString(R.string.key_bigger_viewfinder))
                makePreferenceInvisible(getString(R.string.key_dpm_mode))
                makePreferenceInvisible(getString(R.string.key_mrz_mode))
                preferenceScreen.getPreference(WEEBHOOK_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(GENERAL_SETTINGS_CATEGORY_INDEX).isVisible = true
                preferenceScreen.getPreference(INDIVIDUAL_TEMPLATES_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(CAMERA_SETTINGS_CATEGORY_INDEX).isVisible = false
                makePreferenceInvisible(getString(R.string.key_reset_all_settings))
                makePreferenceInvisible(getString(R.string.key_result_copyTerminator))
                makePreferenceInvisible(getString(R.string.key_reset_all_settings))
                makePreferenceInvisible(getString(R.string.key_show_tutorial))
                makePreferenceInvisible(getString(R.string.key_tutorial_settings))
                makePreferenceInvisible(getString(R.string.key_result_searchEngine))
                makePreferenceInvisible(getString(R.string.key_enable_searchweb))
            }
            ScanMode.INDUSTRIAL_1D -> {
                makePreferenceInvisible(getString(R.string.key_mrz_mode))
                makePreferenceInvisible(getString(R.string.key_enable_location_in_preview))
                makePreferenceInvisible(getString(R.string.key_enable_roi))
                makePreferenceInvisible(getString(R.string.key_blured_scan_eanupc))
                makePreferenceInvisible(getString(R.string.key_bigger_viewfinder))
                makePreferenceInvisible(getString(R.string.key_narrow_viewfinder))
                makePreferenceInvisible(getString(R.string.key_result_searchEngine))
                makePreferenceInvisible(getString(R.string.key_enable_searchweb))
                makePreferenceInvisible(getString(R.string.key_automatic_show_bottomsheet2))
                makePreferenceInvisible(getString(R.string.key_reset_all_settings))
                makePreferenceInvisible(getString(R.string.key_show_tutorial))
                makePreferenceInvisible(getString(R.string.key_tutorial_settings))
                makePreferenceInvisible(getString(R.string.key_dpm_mode))
                makePreferenceInvisible(getString(R.string.key_result_copyTerminator))
                preferenceScreen.getPreference(INDIVIDUAL_TEMPLATES_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(CAMERA_SETTINGS_CATEGORY_INDEX).isVisible = false
            }
            ScanMode.RETAIL_1D -> {
                makePreferenceInvisible(getString(R.string.key_mrz_mode))
                makePreferenceInvisible(getString(R.string.key_enable_location_in_preview))
                makePreferenceInvisible(getString(R.string.key_enable_roi))
                makePreferenceInvisible(getString(R.string.key_bigger_viewfinder))
                makePreferenceInvisible(getString(R.string.key_narrow_viewfinder))
                makePreferenceInvisible(getString(R.string.key_result_searchEngine))
                makePreferenceInvisible(getString(R.string.key_enable_searchweb))
                makePreferenceInvisible(getString(R.string.key_automatic_show_bottomsheet2))
                makePreferenceInvisible(getString(R.string.key_reset_all_settings))
                makePreferenceInvisible(getString(R.string.key_show_tutorial))
                makePreferenceInvisible(getString(R.string.key_tutorial_settings))
                makePreferenceInvisible(getString(R.string.key_dpm_mode))
                makePreferenceInvisible(getString(R.string.key_result_copyTerminator))
                preferenceScreen.getPreference(INDIVIDUAL_TEMPLATES_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(CAMERA_SETTINGS_CATEGORY_INDEX).isVisible = false
            }

            ScanMode.PDF -> {
                makePreferenceInvisible(getString(R.string.key_mrz_mode))
                makePreferenceInvisible(getString(R.string.key_enable_location_in_preview))
                makePreferenceInvisible(getString(R.string.key_enable_roi))
                makePreferenceInvisible(getString(R.string.key_blured_scan_eanupc))
                makePreferenceInvisible(getString(R.string.key_misshaped_code_capture))
                makePreferenceInvisible(getString(R.string.key_bigger_viewfinder))
                makePreferenceInvisible(getString(R.string.key_narrow_viewfinder))
                makePreferenceInvisible(getString(R.string.key_result_searchEngine))
                makePreferenceInvisible(getString(R.string.key_enable_searchweb))
                makePreferenceInvisible(getString(R.string.key_automatic_show_bottomsheet2))
                makePreferenceInvisible(getString(R.string.key_reset_all_settings))
                makePreferenceInvisible(getString(R.string.key_show_tutorial))
                makePreferenceInvisible(getString(R.string.key_tutorial_settings))
                makePreferenceInvisible(getString(R.string.key_dpm_mode))
                makePreferenceInvisible(getString(R.string.key_result_copyTerminator))
                preferenceScreen.getPreference(INDIVIDUAL_TEMPLATES_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(CAMERA_SETTINGS_CATEGORY_INDEX).isVisible = false
            }
            ScanMode.QR -> {
                makePreferenceInvisible(getString(R.string.key_mrz_mode))
                makePreferenceInvisible(getString(R.string.key_enable_location_in_preview))
                makePreferenceInvisible(getString(R.string.key_enable_roi))
                makePreferenceInvisible(getString(R.string.key_blured_scan_eanupc))
                makePreferenceInvisible(getString(R.string.key_misshaped_code_capture))
                makePreferenceInvisible(getString(R.string.key_bigger_viewfinder))
                makePreferenceInvisible(getString(R.string.key_narrow_viewfinder))
                makePreferenceInvisible(getString(R.string.key_result_searchEngine))
                makePreferenceInvisible(getString(R.string.key_enable_searchweb))
                makePreferenceInvisible(getString(R.string.key_automatic_show_bottomsheet2))
                makePreferenceInvisible(getString(R.string.key_reset_all_settings))
                makePreferenceInvisible(getString(R.string.key_show_tutorial))
                makePreferenceInvisible(getString(R.string.key_tutorial_settings))
                makePreferenceInvisible(getString(R.string.key_dpm_mode))
                makePreferenceInvisible(getString(R.string.key_result_copyTerminator))
                preferenceScreen.getPreference(INDIVIDUAL_TEMPLATES_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(CAMERA_SETTINGS_CATEGORY_INDEX).isVisible = false
            }
            ScanMode.ALL_2D -> {
                makePreferenceInvisible(getString(R.string.key_mrz_mode))
                makePreferenceInvisible(getString(R.string.key_enable_location_in_preview))
                makePreferenceInvisible(getString(R.string.key_enable_roi))
                makePreferenceInvisible(getString(R.string.key_blured_scan_eanupc))
                makePreferenceInvisible(getString(R.string.key_misshaped_code_capture))
                makePreferenceInvisible(getString(R.string.key_bigger_viewfinder))
                makePreferenceInvisible(getString(R.string.key_narrow_viewfinder))
                makePreferenceInvisible(getString(R.string.key_result_searchEngine))
                makePreferenceInvisible(getString(R.string.key_enable_searchweb))
                makePreferenceInvisible(getString(R.string.key_automatic_show_bottomsheet2))
                makePreferenceInvisible(getString(R.string.key_reset_all_settings))
                makePreferenceInvisible(getString(R.string.key_show_tutorial))
                makePreferenceInvisible(getString(R.string.key_tutorial_settings))
                makePreferenceInvisible(getString(R.string.key_dpm_mode))
                makePreferenceInvisible(getString(R.string.key_result_copyTerminator))
                preferenceScreen.getPreference(INDIVIDUAL_TEMPLATES_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(CAMERA_SETTINGS_CATEGORY_INDEX).isVisible = false

            }
            ScanMode.DPM -> {
                makePreferenceInvisible(getString(R.string.key_mrz_mode))
                makePreferenceInvisible(getString(R.string.key_enable_roi))
                makePreferenceInvisible(getString(R.string.key_blured_scan_eanupc))
                makePreferenceInvisible(getString(R.string.key_misshaped_code_capture))
                makePreferenceInvisible(getString(R.string.key_auto_start_scan))
                makePreferenceInvisible(getString(R.string.key_narrow_viewfinder))
                makePreferenceInvisible(getString(R.string.key_result_searchEngine))
                makePreferenceInvisible(getString(R.string.key_enable_searchweb))
                makePreferenceInvisible(getString(R.string.key_automatic_show_bottomsheet2))
                makePreferenceInvisible(getString(R.string.key_reset_all_settings))
                makePreferenceInvisible(getString(R.string.key_show_tutorial))
                makePreferenceInvisible(getString(R.string.key_tutorial_settings))
                makePreferenceInvisible(getString(R.string.key_dpm_mode))
                makePreferenceInvisible(getString(R.string.key_result_copyTerminator))
                makePreferenceInvisible(getString(R.string.key_enable_location_in_preview))
                preferenceScreen.getPreference(BARCODE_TYPES_CATEGORY_INDEX).isVisible = true
                preferenceScreen.getPreference(INDIVIDUAL_TEMPLATES_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(CAMERA_SETTINGS_CATEGORY_INDEX).isVisible = false
            }
            ScanMode.VIN -> {
                makePreferenceInvisible(getString(R.string.key_mrz_mode))
                makePreferenceInvisible(getString(R.string.key_enable_roi))
                makePreferenceInvisible(getString(R.string.key_blured_scan_eanupc))
                makePreferenceInvisible(getString(R.string.key_auto_start_scan))
                makePreferenceInvisible(getString(R.string.key_bigger_viewfinder))
                makePreferenceInvisible(getString(R.string.key_result_searchEngine))
                makePreferenceInvisible(getString(R.string.key_enable_searchweb))
                makePreferenceInvisible(getString(R.string.key_automatic_show_bottomsheet2))
                makePreferenceInvisible(getString(R.string.key_reset_all_settings))
                makePreferenceInvisible(getString(R.string.key_show_tutorial))
                makePreferenceInvisible(getString(R.string.key_tutorial_settings))
                makePreferenceInvisible(getString(R.string.key_dpm_mode))
                makePreferenceInvisible(getString(R.string.key_result_copyTerminator))
                makePreferenceInvisible(getString(R.string.key_barkoder_result_settings))
                makePreferenceInvisible(getString(R.string.key_enable_location_in_preview))
                makePreferenceVisable(getString(R.string.key_symbology_ocr))
                preferenceScreen.getPreference(INDIVIDUAL_TEMPLATES_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(CAMERA_SETTINGS_CATEGORY_INDEX).isVisible = false
                val resultSettingsCategory = findPreference<Preference>(getString(R.string.key_barkoder_result_settings))
                resultSettingsCategory?.isVisible = false
            }
            ScanMode.DOTCODE -> {
                makePreferenceInvisible(getString(R.string.key_mrz_mode))
                makePreferenceInvisible(getString(R.string.key_enable_location_in_preview))
                makePreferenceInvisible(getString(R.string.key_enable_roi))
                makePreferenceInvisible(getString(R.string.key_bigger_viewfinder))
                makePreferenceInvisible(getString(R.string.key_blured_scan_eanupc))
                makePreferenceInvisible(getString(R.string.key_misshaped_code_capture))
                makePreferenceInvisible(getString(R.string.key_auto_start_scan))
                makePreferenceInvisible(getString(R.string.key_narrow_viewfinder))
                makePreferenceInvisible(getString(R.string.key_result_searchEngine))
                makePreferenceInvisible(getString(R.string.key_enable_searchweb))
                makePreferenceInvisible(getString(R.string.key_automatic_show_bottomsheet2))
                makePreferenceInvisible(getString(R.string.key_reset_all_settings))
                makePreferenceInvisible(getString(R.string.key_show_tutorial))
                makePreferenceInvisible(getString(R.string.key_tutorial_settings))
                makePreferenceInvisible(getString(R.string.key_dpm_mode))
                makePreferenceInvisible(getString(R.string.key_result_copyTerminator))
                preferenceScreen.getPreference(BARCODE_TYPES_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(INDIVIDUAL_TEMPLATES_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(CAMERA_SETTINGS_CATEGORY_INDEX).isVisible = false
            }

            ScanMode.UPC_EAN_DEBLUR -> {
                makePreferenceInvisible(getString(R.string.key_mrz_mode))
                makePreferenceInvisible(getString(R.string.key_enable_location_in_preview))
                makePreferenceInvisible(getString(R.string.key_enable_roi))
                makePreferenceInvisible(getString(R.string.key_bigger_viewfinder))
                makePreferenceInvisible(getString(R.string.key_blured_scan_eanupc))
                makePreferenceInvisible(getString(R.string.key_misshaped_code_capture))
                makePreferenceInvisible(getString(R.string.key_auto_start_scan))
                makePreferenceInvisible(getString(R.string.key_narrow_viewfinder))
                makePreferenceInvisible(getString(R.string.key_result_searchEngine))
                makePreferenceInvisible(getString(R.string.key_enable_searchweb))
                makePreferenceInvisible(getString(R.string.key_automatic_show_bottomsheet2))
                makePreferenceInvisible(getString(R.string.key_reset_all_settings))
                makePreferenceInvisible(getString(R.string.key_show_tutorial))
                makePreferenceInvisible(getString(R.string.key_tutorial_settings))
                makePreferenceInvisible(getString(R.string.key_scanner_resolution))
                makePreferenceInvisible(getString(R.string.key_scanner_decoding_speed))
                makePreferenceInvisible(getString(R.string.key_dpm_mode))
                makePreferenceInvisible(getString(R.string.key_result_copyTerminator))
//                preferenceScreen.getPreference(BARCODE_TYPES_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(WEEBHOOK_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(INDIVIDUAL_TEMPLATES_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(CAMERA_SETTINGS_CATEGORY_INDEX).isVisible = false
            }

            ScanMode.MISSHAPED_1D -> {
                makePreferenceInvisible(getString(R.string.key_mrz_mode))
                makePreferenceInvisible(getString(R.string.key_enable_location_in_preview))
                makePreferenceInvisible(getString(R.string.key_enable_roi))
                makePreferenceInvisible(getString(R.string.key_bigger_viewfinder))
                makePreferenceInvisible(getString(R.string.key_blured_scan_eanupc))
                makePreferenceInvisible(getString(R.string.key_misshaped_code_capture))
                makePreferenceInvisible(getString(R.string.key_auto_start_scan))
                makePreferenceInvisible(getString(R.string.key_narrow_viewfinder))
                makePreferenceInvisible(getString(R.string.key_result_searchEngine))
                makePreferenceInvisible(getString(R.string.key_enable_searchweb))
                makePreferenceInvisible(getString(R.string.key_automatic_show_bottomsheet2))
                makePreferenceInvisible(getString(R.string.key_reset_all_settings))
                makePreferenceInvisible(getString(R.string.key_show_tutorial))
                makePreferenceInvisible(getString(R.string.key_tutorial_settings))
                makePreferenceInvisible(getString(R.string.key_scanner_resolution))
                makePreferenceInvisible(getString(R.string.key_scanner_decoding_speed))
                makePreferenceInvisible(getString(R.string.key_dpm_mode))
                makePreferenceInvisible(getString(R.string.key_result_copyTerminator))
//                preferenceScreen.getPreference(BARCODE_TYPES_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(WEEBHOOK_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(INDIVIDUAL_TEMPLATES_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(CAMERA_SETTINGS_CATEGORY_INDEX).isVisible = false

            }

            ScanMode.ALL_1D -> {
                makePreferenceInvisible(getString(R.string.key_mrz_mode))
                makePreferenceInvisible(getString(R.string.key_enable_location_in_preview))
                makePreferenceInvisible(getString(R.string.key_enable_roi))
                makePreferenceInvisible(getString(R.string.key_bigger_viewfinder))
                makePreferenceInvisible(getString(R.string.key_narrow_viewfinder))
                makePreferenceInvisible(getString(R.string.key_result_searchEngine))
                makePreferenceInvisible(getString(R.string.key_enable_searchweb))
                makePreferenceInvisible(getString(R.string.key_automatic_show_bottomsheet2))
                makePreferenceInvisible(getString(R.string.key_reset_all_settings))
                makePreferenceInvisible(getString(R.string.key_show_tutorial))
                makePreferenceInvisible(getString(R.string.key_tutorial_settings))
                makePreferenceInvisible(getString(R.string.key_result_copyTerminator))
                preferenceScreen.getPreference(INDIVIDUAL_TEMPLATES_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(CAMERA_SETTINGS_CATEGORY_INDEX).isVisible = false
                makePreferenceInvisible(getString(R.string.key_dpm_mode))
            }

            ScanMode.GLOBAL -> { makePreferenceInvisible(getString(R.string.key_bigger_viewfinder))
                makePreferenceInvisible(getString(R.string.key_mrz_mode))
                makePreferenceInvisible(getString(R.string.key_narrow_viewfinder))
                makePreferenceInvisible(getString(R.string.key_reset_config))
                makePreferenceInvisible(getString(R.string.key_automatic_show_bottomsheet2))
                makePreferenceInvisible(getString(R.string.key_automatic_show_bottomsheet))
                preferenceScreen.getPreference(BARKODER_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(BARCODE_TYPES_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(RESULT_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(CAMERA_SETTINGS_CATEGORY_INDEX).isVisible = true
                makePreferenceInvisible(getString(R.string.key_dpm_mode))

            }
            ScanMode.CONTINUOUS -> {
                makePreferenceInvisible(getString(R.string.key_mrz_mode))
                makePreferenceInvisible(getString(R.string.key_continuous_scaning))
                makePreferenceInvisible(getString(R.string.key_bigger_viewfinder))
                makePreferenceInvisible(getString(R.string.key_narrow_viewfinder))
                makePreferenceInvisible(getString(R.string.key_automatic_show_bottomsheet))
                makePreferenceInvisible(getString(R.string.key_enable_searchweb))
                makePreferenceInvisible(getString(R.string.key_result_searchEngine))
                makePreferenceInvisible(getString(R.string.key_reset_all_settings))
                makePreferenceInvisible(getString(R.string.key_show_tutorial))
                makePreferenceInvisible(getString(R.string.key_tutorial_settings))
                makePreferenceInvisible(getString(R.string.key_dpm_mode))
                makePreferenceInvisible(getString(R.string.key_result_copyTerminator))
                makePreferenceInvisible(getString(R.string.key_symbology_ocr))
                preferenceScreen.getPreference(WEEBHOOK_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(GENERAL_SETTINGS_CATEGORY_INDEX).isVisible = true
                preferenceScreen.getPreference(INDIVIDUAL_TEMPLATES_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(CAMERA_SETTINGS_CATEGORY_INDEX).isVisible = false
                var continuisTresHoldPreferences2 = findPreference<ListPreference>("pref_key_continuous_treshold2")!!
                continuisTresHoldPreferences2.isVisible = true
            }
            ScanMode.ANYSCAN -> {
                makePreferenceInvisible(getString(R.string.key_mrz_mode))
                makePreferenceInvisible(getString(R.string.key_bigger_viewfinder))
                makePreferenceInvisible(getString(R.string.key_narrow_viewfinder))
                makePreferenceInvisible(getString(R.string.key_enable_searchweb))
                makePreferenceInvisible(getString(R.string.key_result_searchEngine))
                makePreferenceInvisible(getString(R.string.key_reset_all_settings))
                makePreferenceInvisible(getString(R.string.key_show_tutorial))
                makePreferenceInvisible(getString(R.string.key_tutorial_settings))
                preferenceScreen.getPreference(WEEBHOOK_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(GENERAL_SETTINGS_CATEGORY_INDEX).isVisible = true
                preferenceScreen.getPreference(INDIVIDUAL_TEMPLATES_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(CAMERA_SETTINGS_CATEGORY_INDEX).isVisible = false
                makePreferenceInvisible(getString(R.string.key_automatic_show_bottomsheet2))
                makePreferenceInvisible(getString(R.string.key_dpm_mode))
                makePreferenceInvisible(getString(R.string.key_result_copyTerminator))
                makePreferenceVisable(getString(R.string.key_composite_setting))

            }
            ScanMode.MRZ ->  {
                makePreferenceInvisible(getString(R.string.key_mrz_mode))
                makePreferenceInvisible(getString(R.string.key_enable_location_in_preview))
                makePreferenceInvisible(getString(R.string.key_blured_scan_eanupc))
                makePreferenceInvisible(getString(R.string.key_misshaped_code_capture))
                makePreferenceInvisible(getString(R.string.key_auto_start_scan))
                makePreferenceInvisible(getString(R.string.key_narrow_viewfinder))
                makePreferenceInvisible(getString(R.string.key_result_searchEngine))
                makePreferenceInvisible(getString(R.string.key_enable_searchweb))
                makePreferenceInvisible(getString(R.string.key_automatic_show_bottomsheet2))
                makePreferenceInvisible(getString(R.string.key_reset_all_settings))
                makePreferenceInvisible(getString(R.string.key_show_tutorial))
                makePreferenceInvisible(getString(R.string.key_tutorial_settings))
                makePreferenceInvisible(getString(R.string.key_bigger_viewfinder))
//                makePreferenceInvisible(getString(R.string.key_continuous_scaning))
                makePreferenceInvisible(getString(R.string.key_barkoder_result_settings))
                makePreferenceInvisible(getString(R.string.key_dpm_mode))
                makePreferenceInvisible(getString(R.string.key_result_copyTerminator))
                makePreferenceInvisible(getString(R.string.key_barkoder_result_settings))
                makePreferenceVisable(getString(R.string.key_require_master_checksum))

                preferenceScreen.getPreference(BARCODE_TYPES_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(RESULT_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(INDIVIDUAL_TEMPLATES_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(CAMERA_SETTINGS_CATEGORY_INDEX).isVisible = false
                val resultSettingsCategory = findPreference<Preference>(getString(R.string.key_barkoder_result_settings))
                resultSettingsCategory?.isVisible = false
            }

            ScanMode.GALLERY_SCAN -> {
                makePreferenceInvisible(getString(R.string.key_bigger_viewfinder))
                makePreferenceInvisible(getString(R.string.key_scanner_decoding_speed))
                makePreferenceInvisible(getString(R.string.key_narrow_viewfinder))
                makePreferenceInvisible(getString(R.string.key_enable_searchweb))
                makePreferenceInvisible(getString(R.string.key_result_searchEngine))
                makePreferenceInvisible(getString(R.string.key_reset_all_settings))
                makePreferenceInvisible(getString(R.string.key_show_tutorial))
                makePreferenceInvisible(getString(R.string.key_tutorial_settings))
                makePreferenceInvisible(getString(R.string.key_automatic_show_bottomsheet2))
                makePreferenceInvisible(getString(R.string.key_scanner_resolution))
                makePreferenceInvisible(getString(R.string.key_continuous_scaning))
                makePreferenceInvisible(getString(R.string.key_continuous_treshold))
                makePreferenceInvisible(getString(R.string.key_allow_pinch_to_zoom))
                makePreferenceInvisible(getString(R.string.key_beep))
                makePreferenceInvisible(getString(R.string.key_vibrate))
                makePreferenceInvisible(getString(R.string.key_enable_location_in_preview))
                makePreferenceInvisible(getString(R.string.key_enable_roi))
                makePreferenceInvisible(getString(R.string.key_vibrate))
                makePreferenceInvisible(getString(R.string.key_result_copyTerminator))
                makePreferenceInvisible(getString(R.string.key_automatic_show_bottomsheet))
                makePreferenceVisable(getString(R.string.key_composite_setting))
                preferenceScreen.getPreference(WEEBHOOK_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(GENERAL_SETTINGS_CATEGORY_INDEX).isVisible = true
                preferenceScreen.getPreference(INDIVIDUAL_TEMPLATES_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(CAMERA_SETTINGS_CATEGORY_INDEX).isVisible = false
            }

            ScanMode.COMPOSITE -> {
                makePreferenceInvisible(getString(R.string.key_mrz_mode))
                makePreferenceInvisible(getString(R.string.key_enable_location_in_preview))
                makePreferenceInvisible(getString(R.string.key_enable_roi))
                makePreferenceInvisible(getString(R.string.key_bigger_viewfinder))
                makePreferenceInvisible(getString(R.string.key_narrow_viewfinder))
                makePreferenceInvisible(getString(R.string.key_result_searchEngine))
                makePreferenceInvisible(getString(R.string.key_enable_searchweb))
                makePreferenceInvisible(getString(R.string.key_automatic_show_bottomsheet2))
                makePreferenceInvisible(getString(R.string.key_reset_all_settings))
                makePreferenceInvisible(getString(R.string.key_show_tutorial))
                makePreferenceInvisible(getString(R.string.key_tutorial_settings))
                makePreferenceInvisible(getString(R.string.key_result_copyTerminator))
                preferenceScreen.getPreference(INDIVIDUAL_TEMPLATES_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(CAMERA_SETTINGS_CATEGORY_INDEX).isVisible = false
                makePreferenceInvisible(getString(R.string.key_dpm_mode))
                makePreferenceInvisible(getString(R.string.key_misshaped_code_capture))
            }
            ScanMode.POSTAL_CODES -> {
                makePreferenceInvisible(getString(R.string.key_mrz_mode))
                makePreferenceInvisible(getString(R.string.key_enable_location_in_preview))
                makePreferenceInvisible(getString(R.string.key_enable_roi))
                makePreferenceInvisible(getString(R.string.key_bigger_viewfinder))
                makePreferenceInvisible(getString(R.string.key_narrow_viewfinder))
                makePreferenceInvisible(getString(R.string.key_result_searchEngine))
                makePreferenceInvisible(getString(R.string.key_enable_searchweb))
                makePreferenceInvisible(getString(R.string.key_automatic_show_bottomsheet2))
                makePreferenceInvisible(getString(R.string.key_reset_all_settings))
                makePreferenceInvisible(getString(R.string.key_result_copyTerminator))
                makePreferenceInvisible(getString(R.string.key_show_tutorial))
                makePreferenceInvisible(getString(R.string.key_tutorial_settings))
                preferenceScreen.getPreference(INDIVIDUAL_TEMPLATES_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(CAMERA_SETTINGS_CATEGORY_INDEX).isVisible = false
                makePreferenceInvisible(getString(R.string.key_dpm_mode))
                makePreferenceInvisible(getString(R.string.key_misshaped_code_capture))
                makePreferenceInvisible(getString(R.string.key_blured_scan_eanupc))
            }
            ScanMode.AR_MODE -> {
                makePreferenceInvisible(getString(R.string.key_mrz_mode))
                makePreferenceInvisible(getString(R.string.key_enable_location_in_preview))
                makePreferenceInvisible(getString(R.string.key_enable_roi))
                makePreferenceInvisible(getString(R.string.key_bigger_viewfinder))
                makePreferenceInvisible(getString(R.string.key_narrow_viewfinder))
                makePreferenceInvisible(getString(R.string.key_result_searchEngine))
                makePreferenceInvisible(getString(R.string.key_enable_searchweb))
                makePreferenceInvisible(getString(R.string.key_automatic_show_bottomsheet2))
                makePreferenceInvisible(getString(R.string.key_reset_all_settings))
                makePreferenceInvisible(getString(R.string.key_result_copyTerminator))
                makePreferenceInvisible(getString(R.string.key_symbology_dotcode))
                makePreferenceInvisible(getString(R.string.key_show_tutorial))
                makePreferenceInvisible(getString(R.string.key_tutorial_settings))
                preferenceScreen.getPreference(INDIVIDUAL_TEMPLATES_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(CAMERA_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(WEEBHOOK_SETTINGS_CATEGORY_INDEX).isVisible = false
                makePreferenceInvisible(getString(R.string.key_dpm_mode))
                makePreferenceInvisible(getString(R.string.key_misshaped_code_capture))
                makePreferenceInvisible(getString(R.string.key_blured_scan_eanupc))
                makePreferenceInvisible(getString(R.string.key_continuous_scaning))
                makePreferenceInvisible(getString(R.string.key_vibrate))
                makePreferenceInvisible(getString(R.string.key_automatic_show_bottomsheet))
                makePreferenceVisable(getString(R.string.key_ar_preference))

            }
        }
    }

    private fun setDecodingSpeedEntries() {
        val decodingSpeedPref =
            findPreference<ListPreference>(getString(R.string.key_scanner_decoding_speed))!!

        val entries: MutableList<CharSequence> = arrayListOf()
        val entryValues: MutableList<CharSequence> = arrayListOf()

        for (item in Barkoder.DecodingSpeed.values()) {
            entries.add(item.name)
            entryValues.add(item.ordinal.toString())
            if(item.ordinal == 2) break
        }

        decodingSpeedPref.entries = entries.toTypedArray()
        decodingSpeedPref.entryValues = entryValues.toTypedArray()

        decodingSpeedPref.value =
            preferenceManager.sharedPreferences.getString(decodingSpeedPref.key)
    }

    private fun setARModeEntries() {
        val arPreferences =
            findPreference<ListPreference>(getString(R.string.key_ar_mode_options))!!

        val entries: MutableList<CharSequence> = arrayListOf()
        val entryValues: MutableList<CharSequence> = arrayListOf()

        for (item in BarkoderARMode.values()) {
            if(scanMode != ScanMode.AR_MODE) {
                if (item.ordinal == 0) {
                    entries.add("Off")
                    entryValues.add(item.ordinal.toString())
                }
            }
            if(item.ordinal == 1) {
                entries.add("Not selected By default")
                entryValues.add(item.ordinal.toString())
            }
            if(item.ordinal == 2) {
                entries.add("Selected By default")
                entryValues.add(item.ordinal.toString())
            }
            if(item.ordinal == 3) {
                entries.add("Always selected")
                entryValues.add(item.ordinal.toString())
            }

        }

        arPreferences.entries = entries.toTypedArray()
        arPreferences.entryValues = entryValues.toTypedArray()

        arPreferences.value =
            preferenceManager.sharedPreferences.getString(arPreferences.key)
    }

    private fun setARLocationTypeEntries() {
        val arLocationTypePreferences =
            findPreference<ListPreference>(getString(R.string.key_ar_location_type))!!

        val entries: MutableList<CharSequence> = arrayListOf()
        val entryValues: MutableList<CharSequence> = arrayListOf()

        for (item in BarkoderARLocationType.values()) {
            if(item.ordinal == 0) {
                entries.add("None")
                entryValues.add(item.ordinal.toString())
            }
            if(item.ordinal == 1) {
                entries.add("Tight")
                entryValues.add(item.ordinal.toString())
            }
            if(item.ordinal == 2) {
                entries.add("Bounding Box")
                entryValues.add(item.ordinal.toString())
            }
        }

        arLocationTypePreferences.entries = entries.toTypedArray()
        arLocationTypePreferences.entryValues = entryValues.toTypedArray()

        arLocationTypePreferences.value =
            preferenceManager.sharedPreferences.getString(arLocationTypePreferences.key)
    }

    private fun setARHeaderShowModeEntries() {
        val arHeaderShowModePreferences =
            findPreference<ListPreference>(getString(R.string.key_ar_header_show_mode))!!

        val entries: MutableList<CharSequence> = arrayListOf()
        val entryValues: MutableList<CharSequence> = arrayListOf()

        for (item in BarkoderARHeaderShowMode.values()) {
            if(item.ordinal == 0) {
                entries.add("Never")
                entryValues.add(item.ordinal.toString())
            }
            if(item.ordinal == 1) {
                entries.add("Always")
                entryValues.add(item.ordinal.toString())
            }
            if(item.ordinal == 2) {
                entries.add("On Selected")
                entryValues.add(item.ordinal.toString())
            }
        }

        arHeaderShowModePreferences.entries = entries.toTypedArray()
        arHeaderShowModePreferences.entryValues = entryValues.toTypedArray()

        arHeaderShowModePreferences.value =
            preferenceManager.sharedPreferences.getString(arHeaderShowModePreferences.key)
    }

    private fun setAROverlayFPSEntries() {
        val arAROverlayFPSPreferences =
            findPreference<ListPreference>(getString(R.string.key_ar_overlay_fps))!!

        val entries: MutableList<CharSequence> = arrayListOf()
        val entryValues: MutableList<CharSequence> = arrayListOf()

        for (item in BarkoderAROverlayRefresh.values()) {
            if(item.ordinal == 0) {
                entries.add("Smooth")
                entryValues.add(item.ordinal.toString())
            }
            if(item.ordinal == 1) {
                entries.add("Normal")
                entryValues.add(item.ordinal.toString())
            }

        }

        arAROverlayFPSPreferences.entries = entries.toTypedArray()
        arAROverlayFPSPreferences.entryValues = entryValues.toTypedArray()

        arAROverlayFPSPreferences.value =
            preferenceManager.sharedPreferences.getString(arAROverlayFPSPreferences.key)
    }


    private fun setBarkoderResolutionEntries() {
        val barkoderResolutionPref =
            findPreference<ListPreference>(getString(R.string.key_scanner_resolution))!!

        val entries: MutableList<CharSequence> = arrayListOf()
        val entryValues: MutableList<CharSequence> = arrayListOf()


        for (item in BarkoderResolution.values()) {
            if(item.ordinal < 3) {
                entries.add(item.toString())
                entryValues.add(item.ordinal.toString())
            }

        }

        barkoderResolutionPref.entries = entries.toTypedArray()
        barkoderResolutionPref.entryValues = entryValues.toTypedArray()

        barkoderResolutionPref.value =
            preferenceManager.sharedPreferences.getString(barkoderResolutionPref.key)
    }

    private fun setThreshHoldContiniousEntries() {
        val thresholdContiniousPref =
            findPreference<ListPreference>(getString(R.string.key_continuous_treshold))!!
        thresholdContiniousPref.value = preferenceManager.sharedPreferences.getString(thresholdContiniousPref.key)
    }

    private fun setDynamicExposureEntries() {
        val dynamicExposureEntries =
            findPreference<ListPreference>(getString(R.string.key_dynamic_exposure_entries))!!
        dynamicExposureEntries.value = preferenceManager.sharedPreferences.getString(dynamicExposureEntries.key)
    }

    private fun setMrzChecksumType() {
        val setMrzCheckSumType =
            findPreference<ListPreference>(getString(R.string.key_checksum_mrz))!!
        setMrzCheckSumType.value = preferenceManager.sharedPreferences.getString(setMrzCheckSumType.key)
    }

    private fun defaultSearchEngine() {
        val searchEnginePref = findPreference<ListPreference>(getString(R.string.key_result_searchEngine))!!
        searchEnginePref.value = preferenceManager.sharedPreferences.getString(searchEnginePref.key)
    }

    private fun defaultCopyTerminator() {
        val copyTermintaorPref = findPreference<ListPreference>(getString(R.string.key_result_copyTerminator))!!
                copyTermintaorPref.value = preferenceManager.sharedPreferences.getString(copyTermintaorPref.key)
    }

    private fun setSymbologyPrefs() {
        val symbologyCategory =
            preferenceScreen.getPreference(BARCODE_TYPES_CATEGORY_INDEX) as PreferenceCategory

        symbologyCategory.title = if (scanMode == ScanMode.GLOBAL) {
            getString(R.string.symbologies_settings_title, "(" + scanMode.title + ")")
        } else
            getString(R.string.symbologies_settings_title, " ")

        val scanModeSupportedSymKeys = scanMode.supportedSymbologyKeys(resources)

        for (i in 1..symbologyCategory.preferenceCount) {
            (symbologyCategory.getPreference(i - 1) as PreferenceCategory).let { subCategory ->

                if (scanMode.template != null)
                    subCategory.title = ""

                var atLeastOneSymbologyIsShown = false

                for (j in 1..subCategory.preferenceCount) {
                    subCategory.getPreference(j - 1).run {
                        if (scanModeSupportedSymKeys == null
                            || scanModeSupportedSymKeys.contains(key)
                        ) {
                            if (this is SwitchWithWidgetPreference)
                                setSymbologyPrefWidgetClickListener(this)

                            atLeastOneSymbologyIsShown = true
                        } else
                            isVisible = false
                    }
                }

                subCategory.isVisible = atLeastOneSymbologyIsShown
            }
        }
    }



    private fun setSymbologyPrefWidgetClickListener(symbologyPref: SwitchWithWidgetPreference) {
        Log.d("BackStackDebug", "Before transaction: ${parentFragmentManager.backStackEntryCount}")
        symbologyPref.customClickListener = View.OnClickListener {
            (requireActivity() as AppCompatActivity).supportFragmentManager
                .beginTransaction()
                .replace(
                    R.id.settings_container,
                    AdvancedSettingsFragment.newInstance(
                        symbologyPref.key,
                        symbologyPref.title.toString(),
                        scanMode.ordinal
                    ),
                    AdvancedSettingsFragment.TAG
                )
                .addToBackStack(AdvancedSettingsFragment.TAG)
                .commit() // <-- Add this line
        }
        // The logs here are misleading because the transaction hasn't happened yet.
        // They will always show 0.
        Log.d("BackStackDebug", "After transaction: ${parentFragmentManager.backStackEntryCount}")
    }

    private fun setResultParserEntries() {
        val resultParserPref =
            findPreference<ListPreference>(getString(R.string.key_result_parser))!!

        val entries: MutableList<CharSequence> = arrayListOf()
        for (item in Barkoder.FormattingType.values())
            entries.add(item.name)

        resultParserPref.entries = entries.toTypedArray()

        val entryValues: MutableList<CharSequence> = arrayListOf()
        for (item in Barkoder.FormattingType.values())
            entryValues.add(item.ordinal.toString())

        resultParserPref.entryValues = entryValues.toTypedArray()

        resultParserPref.value =
            preferenceManager.sharedPreferences.getString(resultParserPref.key)
    }

    private fun setResultCharsetEntries() {
        val resultCharsetPref =
            findPreference<ListPreference>(getString(R.string.key_result_charset))!!

        val entries: MutableList<CharSequence> = arrayListOf()
        for (item in Charset.values())
            entries.add(item.title)

        resultCharsetPref.entries = entries.toTypedArray()

        val entryValues: MutableList<CharSequence> = arrayListOf()
        for (item in Charset.values())
            entryValues.add(item.value)

        resultCharsetPref.entryValues = entryValues.toTypedArray()

        resultCharsetPref.value =
            preferenceManager.sharedPreferences.getString(resultCharsetPref.key)
    }

    private fun setUIForScanModeWithTemplate() {
        preferenceScreen.getPreference(BARKODER_SETTINGS_CATEGORY_INDEX).isVisible = true
        preferenceScreen.getPreference(RESULT_CATEGORY_INDEX).isVisible = true
        preferenceScreen.getPreference(WEEBHOOK_SETTINGS_CATEGORY_INDEX).isVisible = false
        generalSettingsGroup.isVisible = true
    }

    private fun setUIForContinuosModeSettings() {
        findPreference<SwitchPreference>(getString(R.string.key_continuous_scaning))!!.isVisible =
            false
        generalSettingsGroup.isVisible = true
    }

    private fun showResetConfigConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.reset_config_confirm_message)
            .setMessage(null)
            .setPositiveButton(R.string.reset_button) { _, _ ->
                BKDConfigUtil.setDefaultValuesInPrefs(
                    preferenceManager.sharedPreferences,
                    requireContext(),
                    false,
                    scanMode,
                    null
                )
                reloadAllPrefsValues()
                continuisTresHoldPreferences.isVisible = false
                if(scanMode != ScanMode.AR_MODE) {
                    makePreferenceInvisible("pref_key_ar_preferenece")
                }
            }
            .setNegativeButton(R.string.cancel_button, null)
            .show()
    }

    private fun showALLResetConfirmationDialog() {

        val sharedPrefNameRetail1D = requireActivity().packageName + ScanMode.RETAIL_1D.prefKey
        val sharedPrefRetail1D = requireActivity().getSharedPreferences(sharedPrefNameRetail1D, Context.MODE_PRIVATE)

        val sharedPrefNameIndustrial1D = requireActivity().packageName + ScanMode.INDUSTRIAL_1D.prefKey
        val sharedPrefIndustrial1D = requireActivity().getSharedPreferences(sharedPrefNameIndustrial1D, Context.MODE_PRIVATE)

        val sharedPrefNamePDFCodes = requireActivity().packageName + ScanMode.PDF.prefKey
        val sharedPrefPDFCodes = requireActivity().getSharedPreferences(sharedPrefNamePDFCodes, Context.MODE_PRIVATE)

        val sharedPrefNameQRCodes = requireActivity().packageName + ScanMode.QR.prefKey
        val sharedPrefQRCodes = requireActivity().getSharedPreferences(sharedPrefNameQRCodes, Context.MODE_PRIVATE)

        val sharedPrefNameAll2D = requireActivity().packageName + ScanMode.ALL_2D.prefKey
        val sharedPrefAll2D = requireActivity().getSharedPreferences(sharedPrefNameAll2D, Context.MODE_PRIVATE)

        val sharedPrefNameBatchMultiScan = requireActivity().packageName + ScanMode.CONTINUOUS.prefKey
        val sharedPrefBatchMultiScan = requireActivity().getSharedPreferences(sharedPrefNameBatchMultiScan, Context.MODE_PRIVATE)

        val sharedPrefNameDPM = requireActivity().packageName + ScanMode.DPM.prefKey
        val sharedPrefDPM = requireActivity().getSharedPreferences(sharedPrefNameDPM, Context.MODE_PRIVATE)

        val sharedPrefNameVIN = requireActivity().packageName + ScanMode.VIN.prefKey
        val sharedPrefVIN = requireActivity().getSharedPreferences(sharedPrefNameVIN, Context.MODE_PRIVATE)

        val sharedPrefNameAnyscan = requireActivity().packageName + ScanMode.ANYSCAN.prefKey
        val sharedPrefAnyscan = requireActivity().getSharedPreferences(sharedPrefNameAnyscan, Context.MODE_PRIVATE)

        val sharedPrefNameGlobal = requireActivity().packageName + ScanMode.GLOBAL.prefKey
        val sharedPrefGlobal = requireActivity().getSharedPreferences(sharedPrefNameGlobal, Context.MODE_PRIVATE)

        val sharedPrefNameDotcode = requireActivity().packageName + ScanMode.DOTCODE.prefKey
        val sharedPrefDotcode = requireActivity().getSharedPreferences(sharedPrefNameDotcode, Context.MODE_PRIVATE)

        val sharedPrefNameDeblur = requireActivity().packageName + ScanMode.UPC_EAN_DEBLUR.prefKey
        val sharedPrefDeblur = requireActivity().getSharedPreferences(sharedPrefNameDeblur, Context.MODE_PRIVATE)

        val sharedPrefNameMisshaped = requireActivity().packageName + ScanMode.MISSHAPED_1D.prefKey
        val sharedPrefMisshaped = requireActivity().getSharedPreferences(sharedPrefNameMisshaped, Context.MODE_PRIVATE)

        val sharedPrefNameAll1D = requireActivity().packageName + ScanMode.ALL_1D.prefKey
        val sharedPrefAll1D = requireActivity().getSharedPreferences(sharedPrefNameAll1D, Context.MODE_PRIVATE)

        val sharedPrefNameGalleryScan = requireActivity().packageName + ScanMode.GALLERY_SCAN.prefKey
        val sharedPrefGalleryScan = requireActivity().getSharedPreferences(sharedPrefNameGalleryScan, Context.MODE_PRIVATE)

        val sharedPrefNameMRZ = requireActivity().packageName + ScanMode.MRZ.prefKey
        val sharedPrefMRZ = requireActivity().getSharedPreferences(sharedPrefNameMRZ, Context.MODE_PRIVATE)

        val sharedPrefNameComposite = requireActivity().packageName + ScanMode.COMPOSITE.prefKey
        val sharedPrefComposite = requireActivity().getSharedPreferences(sharedPrefNameComposite, Context.MODE_PRIVATE)

        val sharedPrefNamePostalCodes = requireActivity().packageName + ScanMode.POSTAL_CODES.prefKey
        val sharedPrefPostalCodes = requireActivity().getSharedPreferences(sharedPrefNamePostalCodes, Context.MODE_PRIVATE)

        val sharedPrefNameARMode = requireActivity().packageName + ScanMode.AR_MODE.prefKey
        val sharedPrefARMode = requireActivity().getSharedPreferences(sharedPrefNameARMode, Context.MODE_PRIVATE)



        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.reset_all_confirm_message)
            .setMessage(null)
            .setPositiveButton(R.string.reset_button) { _, _ ->

                BKDConfigUtil.setDefaultValuesInPrefs(
                    preferenceManager.sharedPreferences,
                    requireContext(),
                    false,
                    scanMode,
                    null
                )
                resetSearchEnginePref()
                reloadAllPrefsValues()
                resetWebHookUrl()
                continuisTresHoldPreferences.isVisible = false

                BKDConfigUtil.setDefaultValuesInPrefs(
                    sharedPrefRetail1D,
                    requireContext(),
                    false,
                    ScanMode.RETAIL_1D,
                    null
                )
                BKDConfigUtil.setDefaultValuesInPrefs(
                    sharedPrefIndustrial1D,
                    requireContext(),
                    false,
                    ScanMode.INDUSTRIAL_1D,
                    null
                )
                BKDConfigUtil.setDefaultValuesInPrefs(
                    sharedPrefAnyscan,
                    requireContext(),
                    false,
                    ScanMode.ANYSCAN,
                    null
                )
                BKDConfigUtil.setDefaultValuesInPrefs(
                    sharedPrefPDFCodes,
                    requireContext(),
                    false,
                    ScanMode.PDF,
                    null
                )
                BKDConfigUtil.setDefaultValuesInPrefs(
                    sharedPrefQRCodes,
                    requireContext(),
                    false,
                    ScanMode.QR,
                    null
                )
                BKDConfigUtil.setDefaultValuesInPrefs(
                    sharedPrefVIN,
                    requireContext(),
                    false,
                    ScanMode.VIN,
                    null
                )
                BKDConfigUtil.setDefaultValuesInPrefs(
                    sharedPrefDPM,
                    requireContext(),
                    false,
                    ScanMode.DPM,
                    null
                )
                BKDConfigUtil.setDefaultValuesInPrefs(
                    sharedPrefAll2D,
                    requireContext(),
                    false,
                    ScanMode.ALL_2D,
                    null
                )
                BKDConfigUtil.setDefaultValuesInPrefs(
                    sharedPrefBatchMultiScan,
                    requireContext(),
                    false,
                    ScanMode.CONTINUOUS,
                    null
                )
                BKDConfigUtil.setDefaultValuesInPrefs(
                    sharedPrefDotcode,
                    requireContext(),
                    false,
                    ScanMode.DOTCODE,
                    null
                )
                BKDConfigUtil.setDefaultValuesInPrefs(
                    sharedPrefDeblur,
                    requireContext(),
                    false,
                    ScanMode.UPC_EAN_DEBLUR,
                    null
                )
                BKDConfigUtil.setDefaultValuesInPrefs(
                    sharedPrefMisshaped,
                    requireContext(),
                    false,
                    ScanMode.MISSHAPED_1D,
                    null
                )
                BKDConfigUtil.setDefaultValuesInPrefs(
                    sharedPrefAll1D,
                    requireContext(),
                    false,
                    ScanMode.ALL_1D,
                    null
                )
                BKDConfigUtil.setDefaultValuesInPrefs(
                    sharedPrefGalleryScan,
                    requireContext(),
                    false,
                    ScanMode.GALLERY_SCAN,
                    null
                )
                BKDConfigUtil.setDefaultValuesInPrefs(
                    sharedPrefMRZ,
                    requireContext(),
                    false,
                    ScanMode.MRZ,
                    null
                )
                BKDConfigUtil.setDefaultValuesInPrefs(
                    sharedPrefComposite,
                    requireContext(),
                    false,
                    ScanMode.COMPOSITE,
                    null
                )
                BKDConfigUtil.setDefaultValuesInPrefs(
                    sharedPrefPostalCodes,
                    requireContext(),
                    false,
                    ScanMode.POSTAL_CODES,
                    null
                )

                BKDConfigUtil.setDefaultValuesInPrefs(
                    sharedPrefARMode,
                    requireContext(),
                    false,
                    ScanMode.AR_MODE,
                    null
                )

            }
            .setNegativeButton(R.string.cancel_button, null)
            .show()
    }

    private fun reloadAllPrefsValues() {
        val dynamicExposureEntries =
            findPreference<ListPreference>(getString(R.string.key_dynamic_exposure_entries))!!
        val setCenteredAutoFocus =
            findPreference<SwitchPreference>(getString(R.string.key_autoFocus_centered))!!
        val setVideoStabilization =
            findPreference<SwitchPreference>(getString(R.string.key_video_stabilization))!!
        val setFrontcamera =
            findPreference<SwitchPreference>(getString(R.string.key_frontCamera))!!
        if(scanMode == ScanMode.GLOBAL) {
            webhookEncodeDataPreference.isEnabled = true
            webhookFeedbackPreference.isEnabled = true
            webhookConfigurationPreference.isEnabled = true
            defaultSearchWebPreference.isEnabled = true
            setFrontcamera.isChecked = false
            setCenteredAutoFocus.isChecked = false
            setVideoStabilization.isChecked = false
            dynamicExposureEntries.value = "Disabled"
            dynamicExposureEntries.setValueIndex(0);

        }
        for (i in 0 until preferenceScreen.preferenceCount) {
            preferenceScreen.getPreference(i).run {
                if (this is PreferenceCategory)
                    reloadPrefsValuesForGroup(this)
                else
                    updatePrefValueFromSharedPrefs(this)
            }
        }
    }

    private fun reloadPrefsValuesForGroup(group: PreferenceCategory) {
        for (i in 0 until group.preferenceCount) {
            group.getPreference(i).run {
                if (this is PreferenceCategory)
                    reloadPrefsValuesForGroup(this)
                else
                    updatePrefValueFromSharedPrefs(this)
            }
        }
    }

    private fun updatePrefValueFromSharedPrefs(pref: Preference) {
        when (pref) {
            is SwitchPreference -> {
                pref.isChecked = preferenceManager.sharedPreferences.getBoolean(pref.key)
            }

            is ListPreference -> {
                pref.value = preferenceManager.sharedPreferences.getString(pref.key)
            }
        }
    }

    private fun searchEnginePreferenceChangeListener() {
        val searchEnginePreference = findPreference<ListPreference>(getString(R.string.key_result_searchEngine))

        searchEnginePreference?.setOnPreferenceChangeListener { _, newValue ->
            var selectedSearchEngine = newValue.toString()
            saveSearchEnginePreference(requireContext(), selectedSearchEngine)
            true
        }
    }

    private fun saveSearchEnginePreference(context: Context, Browser: String) {
        val sp = context.getSharedPreferences(getString(R.string.key_result_searchEngine), Context.MODE_PRIVATE)
        val editor = sp.edit()
        editor.putString(getString(R.string.key_result_searchEngine_value), Browser)
        editor.apply()
    }

//    private fun checksumMrzListener() {
//        val checksumMrzPreference = findPreference<ListPreference>(getString(R.string.key_checksum_mrz))
//
//        checksumMrzPreference?.setOnPreferenceChangeListener { _, newValue ->
//            var selectedCheckSumMrz = newValue.toString()
//            savneCheckSmMrzListener(requireContext(), selectedCheckSumMrz)
//            true
//        }
//    }

//    private fun savneCheckSmMrzListener(context: Context, MasterChekSum: String) {
//        val sp = context.getSharedPreferences(getString(R.string.key_checksum_mrz), Context.MODE_PRIVATE)
//        val editor = sp.edit()
//        editor.putString(getString(R.string.key_checksum_mrz_value), MasterChekSum)
//        editor.apply()
//    }

    private fun copyTerminatorPreferenceChangeListener() {
        val copyTerminatorPreference = findPreference<ListPreference>(getString(R.string.key_result_copyTerminator))

        copyTerminatorPreference?.setOnPreferenceChangeListener { _, newValue ->
            var selectedCopyTerminator = newValue.toString()
            copyTerminatorEnginePreference(requireContext(), selectedCopyTerminator)
            true
        }
    }

    private fun copyTerminatorEnginePreference(context: Context, Terminator: String) {
        val sp = context.getSharedPreferences(getString(R.string.key_result_copyTerminator), Context.MODE_PRIVATE)
        val editor = sp.edit()
        editor.putString(getString(R.string.key_result_copyTerminator_value), Terminator)
        editor.apply()
    }

    private fun openWebHookConfigurationDialog() {
        val webHookPreference = findPreference<Preference>(getString(R.string.key_webhook_configuration))
        webHookPreference?.isSelectable = true
        webHookPreference?.setOnPreferenceClickListener {
            Log.d("qwewqeq","opeenedeDDIDal")
            val dialogFragment = WebHookConfigurationDialogFragment()
            dialogFragment.show(childFragmentManager, "WebHookConfigurationDialog")

            true
        }
    }

    private fun showTutorialDialogs() {
        // Replace the key with your actual preference key from `preferences_settings.xml`
        val tutorialPref = findPreference<Preference>(getString(R.string.key_show_tutorial))
            ?: findPreference("pref_key_show_tutorial")
            ?: return

        tutorialPref.isSelectable = true
        tutorialPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val intent = Intent(requireContext(), MainActivity::class.java).apply {
                // Flag so MainActivity knows this navigation came from Settings
                putExtra("extra_opened_tutorial_from_settings", true)

                // Optional: include the current scanMode too
                putExtra(SettingsFragment.ARGS_MODE_KEY, scanMode.ordinal)

                // Avoid multiple instances
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intent)
            requireActivity().finish()
            true
        }
    }

    private fun resetSearchEnginePref() {
        val sp = requireContext().getSharedPreferences(getString(R.string.key_result_searchEngine), Context.MODE_PRIVATE)
        val editor = sp.edit()
        editor.putString(getString(R.string.key_result_searchEngine_value), DemoDefaults.SEARCH_ENGINE_BROWSER_DEFAULT)
        editor.apply()
    }

    private fun resetWebHookUrl() {
        val sp = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sp.edit()
        editor.putString(getString(R.string.key_url_webhook), "")
        editor.putString(getString(R.string.key_secret_word_webhook), "")
        editor.apply()
    }

    private fun makePreferenceInvisible(preferenceKey: String) {
        val preference = findPreference<Preference>(preferenceKey)
        preference?.isVisible = false
    }

    private fun makePreferenceVisable(preferenceKey: String) {
        val preference = findPreference<Preference>(preferenceKey)
        preference?.isVisible = true
    }


    override fun onStop() {
        val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putBoolean("settingsChangedBoolean", false)
        editor.apply()
        super.onStop()
    }

}
