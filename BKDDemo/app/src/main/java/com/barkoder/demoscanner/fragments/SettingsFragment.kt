package com.barkoder.demoscanner.fragments

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import androidx.preference.SwitchPreferenceCompat
import com.barkoder.Barkoder
import com.barkoder.demoscanner.R
import com.barkoder.demoscanner.ScannerActivity
import com.barkoder.demoscanner.SettingsActivity
import com.barkoder.demoscanner.customcontrols.PreferenceCategoryWithPadding
import com.barkoder.demoscanner.customcontrols.SwitchWithPaddingPreference
import com.barkoder.demoscanner.customcontrols.SwitchWithWidgetPreference
import com.barkoder.demoscanner.enums.Charset
import com.barkoder.demoscanner.enums.ScanMode
import com.barkoder.demoscanner.utils.BKDConfigUtil
import com.barkoder.demoscanner.utils.DemoDefaults
import com.barkoder.demoscanner.utils.getBoolean
import com.barkoder.demoscanner.utils.getString
import com.barkoder.enums.BarkoderConfigTemplate
import com.barkoder.enums.BarkoderResolution
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsFragment : PreferenceFragmentCompat() {

    companion object {
        const val ARGS_MODE_KEY = "settingsScanMode"
    }

    private lateinit var scanMode: ScanMode
    private lateinit var sharedPreferences : SharedPreferences
    private val PREFS_NAME = "MyPrefsFile"

    private val BARKODER_SETTINGS_CATEGORY_INDEX = 0
    private val BARCODE_TYPES_CATEGORY_INDEX = 1
    private val RESULT_CATEGORY_INDEX = 2
    private val WEEBHOOK_SETTINGS_CATEGORY_INDEX = 3
    private val GENERAL_SETTINGS_CATEGORY_INDEX = 4
    private val CAMERA_SETTINGS_CATEGORY_INDEX = 5
    private val INDIVIDUAL_TEMPLATES_SETTINGS_CATEGORY_INDEX = 6

    lateinit var continuisTresHoldPreferences : ListPreference

    private lateinit var webhookConfigurationPreference: Preference
    private lateinit var webhookAutosendPreference: SwitchPreference
    private lateinit var webhookFeedbackPreference: SwitchPreference
    private lateinit var webhookEncodeDataPreference: SwitchPreference
    private lateinit var defaultSearchWebPreference: ListPreference


    private val generalSettingsGroup by lazy {
        preferenceScreen.getPreference(GENERAL_SETTINGS_CATEGORY_INDEX)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        scanMode = ScanMode.values()[requireActivity().intent.extras!!.getInt(ARGS_MODE_KEY)]

        if (scanMode != ScanMode.GLOBAL)
            preferenceManager.sharedPreferencesName =
                requireActivity().packageName + scanMode.prefKey

        BKDConfigUtil.configureBKD(requireContext(), scanMode)



        setPreferencesFromResource(R.xml.preferences_settings, rootKey)


        searchEnginePreferenceChangeListener()
        setSymbologyPrefs()
        onClickTemplateSettings()
        settingsChangedTemplateMessage()
        onEnabledWebhookListener()
        onEnableSearchListener()
        makeSomeInvisiblePreferencesForTemplates()


        if (scanMode.template != null) {
            setUIForScanModeWithTemplate()
            setDecodingSpeedEntries()
            setBarkoderResolutionEntries()
            setResultParserEntries()
            setResultCharsetEntries()
            setThreshHoldContiniousEntries()
            onContiniousModeOnListener()
            findPreference<Preference>(getString(R.string.key_reset_config))!!.setOnPreferenceClickListener {
                showResetConfigConfirmationDialog()
                false
            }
        } else {
            defaultSearchEngine()
            setDecodingSpeedEntries()
            setBarkoderResolutionEntries()
            setResultParserEntries()
            setResultCharsetEntries()
            openWebHookConfigurationDialog()
            setThreshHoldContiniousEntries()
            onContiniousModeOnListener()

            findPreference<Preference>(getString(R.string.key_reset_config))!!.setOnPreferenceClickListener {
                showResetConfigConfirmationDialog()
                false
            }

            findPreference<Preference>(getString(R.string.key_reset_all_settings))!!.setOnPreferenceClickListener {
                showALLResetConfirmationDialog()
                false
            }

//            findPreference<Preference>("proverka")!!.setOnPreferenceClickListener {
//                parentFragmentManager
//                    .beginTransaction()
//                    .replace(
//                        R.id.settings_container,
//                        SettingsRadioOptionsFragment()
//                    )
//                    .commit()
//                false
//            }




            if (scanMode == ScanMode.CONTINUOUS)
                setUIForContinuosModeSettings()

        }
    }

    private fun onEnabledWebhookListener() {
        var  enabledWebhookPreference = findPreference<SwitchPreference>("pref_key_enable_webhook")
        webhookConfigurationPreference = findPreference(getString(R.string.key_webhook_configuration))!!
        webhookAutosendPreference = findPreference(getString(R.string.key_webhook_autosend))!!
        webhookFeedbackPreference = findPreference(getString(R.string.key_webhook_feedback))!!
        webhookEncodeDataPreference = findPreference(getString(R.string.key_webhook_encode_data))!!

        if(enabledWebhookPreference!!.isChecked){
            webhookEncodeDataPreference.isEnabled = true
            webhookFeedbackPreference.isEnabled = true
            webhookConfigurationPreference.isEnabled = true
            webhookAutosendPreference.isEnabled = true
        } else {
            webhookEncodeDataPreference.isEnabled = false
            webhookFeedbackPreference.isEnabled = false
            webhookConfigurationPreference.isEnabled = false
            webhookAutosendPreference.isEnabled = false
        }

        enabledWebhookPreference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            val urlWebHook = sharedPreferences.getString(getString(R.string.key_url_webhook), "")
            val valueBoolean = newValue as Boolean
            if(urlWebHook.isNullOrBlank()) {
                if(valueBoolean) {
                    var notConfiguredWebHookDialog = NotConfiguredWebHookDialog()
                    notConfiguredWebHookDialog.show(requireFragmentManager(), "NotConfiguredWebHookDialog")
                }
            }
            if(!valueBoolean) {
                webhookEncodeDataPreference.isEnabled = false
                webhookFeedbackPreference.isEnabled = false
                webhookConfigurationPreference.isEnabled = false
                webhookAutosendPreference.isEnabled = false
            } else {
                webhookEncodeDataPreference.isEnabled = true
                webhookFeedbackPreference.isEnabled = true
                webhookConfigurationPreference.isEnabled = true
                webhookAutosendPreference.isEnabled = true
            }
            true
        }

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
        var continuiusModePreference = findPreference<SwitchPreference>("pref_key_continuous_scanning")
        continuisTresHoldPreferences = findPreference<ListPreference>("pref_key_continuous_treshold")!!

        continuiusModePreference!!.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            val isVisible = newValue as Boolean
            if (isVisible) {
                continuisTresHoldPreferences.isVisible = true
            } else {
                continuisTresHoldPreferences.isVisible = false
            }
            true
        }

        if(continuiusModePreference!!.isChecked == true) {
            continuisTresHoldPreferences.isVisible = true
        } else {
            continuisTresHoldPreferences.isVisible = false
        }
    }

    private fun settingsChangedTemplateMessage() {
        val barkoderSettingsCategory: PreferenceCategory? = findPreference("pref_key_barkoder_settings")
        val barkodeTypes2DSettingsCategory: PreferenceCategoryWithPadding? = findPreference("pref_key_barkode_types2D_settings")
        val barkodeTypes1DSettingsCategory: PreferenceCategoryWithPadding? = findPreference("pref_key_barkode_types1D_settings")
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
        findPreference<Preference>(getString(R.string.key_1d_industrial_settings))!!.setOnPreferenceClickListener {
            val settingsIntent = Intent(requireActivity(), SettingsActivity::class.java)
            settingsIntent.putExtra(SettingsFragment.ARGS_MODE_KEY, 0)
            startActivity(settingsIntent)
            false
        }
        findPreference<Preference>(getString(R.string.key_1d_retail_settings))!!.setOnPreferenceClickListener {
            val settingsIntent = Intent(requireActivity(), SettingsActivity::class.java)
            settingsIntent.putExtra(SettingsFragment.ARGS_MODE_KEY, 1)
            startActivity(settingsIntent)
            false
        }
        findPreference<Preference>(getString(R.string.key_PDF_settings))!!.setOnPreferenceClickListener {
            val settingsIntent = Intent(requireActivity(), SettingsActivity::class.java)
            settingsIntent.putExtra(SettingsFragment.ARGS_MODE_KEY, 2)
            startActivity(settingsIntent)
            false
        }
        findPreference<Preference>(getString(R.string.key_All2D_settings))!!.setOnPreferenceClickListener {
            val settingsIntent = Intent(requireActivity(), SettingsActivity::class.java)
            settingsIntent.putExtra(SettingsFragment.ARGS_MODE_KEY, 4)
            startActivity(settingsIntent)
            false
        }
        findPreference<Preference>(getString(R.string.key_BatchMultiscan_settings))!!.setOnPreferenceClickListener {
            val settingsIntent = Intent(requireActivity(), SettingsActivity::class.java)
            settingsIntent.putExtra(SettingsFragment.ARGS_MODE_KEY, 5)
            startActivity(settingsIntent)
            false
        }
        findPreference<Preference>(getString(R.string.key_DPM_settings))!!.setOnPreferenceClickListener {
            val settingsIntent = Intent(requireActivity(), SettingsActivity::class.java)
            settingsIntent.putExtra(SettingsFragment.ARGS_MODE_KEY, 7)
            startActivity(settingsIntent)
            false
        }
        findPreference<Preference>(getString(R.string.key_VIN_settings))!!.setOnPreferenceClickListener {
            val settingsIntent = Intent(requireActivity(), SettingsActivity::class.java)
            settingsIntent.putExtra(SettingsFragment.ARGS_MODE_KEY, 8)
            startActivity(settingsIntent)
            false
        }
        findPreference<Preference>(getString(R.string.key_anyscan_settings))!!.setOnPreferenceClickListener {
            val settingsIntent = Intent(requireActivity(), SettingsActivity::class.java)
            settingsIntent.putExtra(SettingsFragment.ARGS_MODE_KEY, 6)
            startActivity(settingsIntent)
            false
        }
        findPreference<Preference>(getString(R.string.key_anyscan_settings))!!.setOnPreferenceClickListener {
            val settingsIntent = Intent(requireActivity(), SettingsActivity::class.java)
            settingsIntent.putExtra(SettingsFragment.ARGS_MODE_KEY, 6)
            startActivity(settingsIntent)
            false
        }
        findPreference<Preference>(getString(R.string.key_Dotcode_settings))!!.setOnPreferenceClickListener {
            val settingsIntent = Intent(requireActivity(), SettingsActivity::class.java)
            settingsIntent.putExtra(SettingsFragment.ARGS_MODE_KEY, 9)
            startActivity(settingsIntent)
            false
        }

        findPreference<Preference>(getString(R.string.key_Deblur_settings))!!.setOnPreferenceClickListener {
            val settingsIntent = Intent(requireActivity(), SettingsActivity::class.java)
            settingsIntent.putExtra(SettingsFragment.ARGS_MODE_KEY, 10)
            startActivity(settingsIntent)
            false
        }

        findPreference<Preference>(getString(R.string.key_Misshaped_settings))!!.setOnPreferenceClickListener {
            val settingsIntent = Intent(requireActivity(), SettingsActivity::class.java)
            settingsIntent.putExtra(SettingsFragment.ARGS_MODE_KEY, 11)
            startActivity(settingsIntent)
            false
        }

        findPreference<Preference>(getString(R.string.key_All1D_settings))!!.setOnPreferenceClickListener {
            val settingsIntent = Intent(requireActivity(), SettingsActivity::class.java)
            settingsIntent.putExtra(SettingsFragment.ARGS_MODE_KEY, 12)
            startActivity(settingsIntent)
            false
        }
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
            ScanMode.INDUSTRIAL_1D -> {
                makePreferenceInvisible(getString(R.string.key_enable_location_in_preview))
                makePreferenceInvisible(getString(R.string.key_enable_roi))
                makePreferenceInvisible(getString(R.string.key_blured_scan_eanupc))
                makePreferenceInvisible(getString(R.string.key_bigger_viewfinder))
                makePreferenceInvisible(getString(R.string.key_narrow_viewfinder))
                makePreferenceInvisible(getString(R.string.key_result_searchEngine))
                makePreferenceInvisible(getString(R.string.key_enable_searchweb))
                makePreferenceInvisible(getString(R.string.key_automatic_show_bottomsheet2))
                makePreferenceInvisible(getString(R.string.key_reset_all_settings))
                preferenceScreen.getPreference(INDIVIDUAL_TEMPLATES_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(CAMERA_SETTINGS_CATEGORY_INDEX).isVisible = false
            }
            ScanMode.RETAIL_1D -> {
                makePreferenceInvisible(getString(R.string.key_enable_location_in_preview))
                makePreferenceInvisible(getString(R.string.key_enable_roi))
                makePreferenceInvisible(getString(R.string.key_bigger_viewfinder))
                makePreferenceInvisible(getString(R.string.key_narrow_viewfinder))
                makePreferenceInvisible(getString(R.string.key_result_searchEngine))
                makePreferenceInvisible(getString(R.string.key_enable_searchweb))
                makePreferenceInvisible(getString(R.string.key_automatic_show_bottomsheet2))
                makePreferenceInvisible(getString(R.string.key_reset_all_settings))
                preferenceScreen.getPreference(INDIVIDUAL_TEMPLATES_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(CAMERA_SETTINGS_CATEGORY_INDEX).isVisible = false
            }

            ScanMode.PDF -> {
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
                preferenceScreen.getPreference(INDIVIDUAL_TEMPLATES_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(CAMERA_SETTINGS_CATEGORY_INDEX).isVisible = false
            }
            ScanMode.QR -> {
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
                preferenceScreen.getPreference(INDIVIDUAL_TEMPLATES_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(CAMERA_SETTINGS_CATEGORY_INDEX).isVisible = false
            }
            ScanMode.ALL_2D -> {
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
                preferenceScreen.getPreference(INDIVIDUAL_TEMPLATES_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(CAMERA_SETTINGS_CATEGORY_INDEX).isVisible = false
            }
            ScanMode.DPM -> {
                makePreferenceInvisible(getString(R.string.key_enable_location_in_preview))
                makePreferenceInvisible(getString(R.string.key_enable_roi))
                makePreferenceInvisible(getString(R.string.key_blured_scan_eanupc))
                makePreferenceInvisible(getString(R.string.key_misshaped_code_capture))
                makePreferenceInvisible(getString(R.string.key_auto_start_scan))
                makePreferenceInvisible(getString(R.string.key_narrow_viewfinder))
                makePreferenceInvisible(getString(R.string.key_result_searchEngine))
                makePreferenceInvisible(getString(R.string.key_enable_searchweb))
                makePreferenceInvisible(getString(R.string.key_automatic_show_bottomsheet2))
                makePreferenceInvisible(getString(R.string.key_reset_all_settings))
                preferenceScreen.getPreference(BARCODE_TYPES_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(INDIVIDUAL_TEMPLATES_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(CAMERA_SETTINGS_CATEGORY_INDEX).isVisible = false
            }
            ScanMode.VIN -> {
                makePreferenceInvisible(getString(R.string.key_enable_location_in_preview))
                makePreferenceInvisible(getString(R.string.key_enable_roi))
                makePreferenceInvisible(getString(R.string.key_blured_scan_eanupc))
                makePreferenceInvisible(getString(R.string.key_auto_start_scan))
                makePreferenceInvisible(getString(R.string.key_bigger_viewfinder))
                makePreferenceInvisible(getString(R.string.key_result_searchEngine))
                makePreferenceInvisible(getString(R.string.key_enable_searchweb))
                makePreferenceInvisible(getString(R.string.key_automatic_show_bottomsheet2))
                makePreferenceInvisible(getString(R.string.key_reset_all_settings))
                preferenceScreen.getPreference(INDIVIDUAL_TEMPLATES_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(CAMERA_SETTINGS_CATEGORY_INDEX).isVisible = false
            }
            ScanMode.DOTCODE -> {
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
                preferenceScreen.getPreference(BARCODE_TYPES_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(INDIVIDUAL_TEMPLATES_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(CAMERA_SETTINGS_CATEGORY_INDEX).isVisible = false
            }

            ScanMode.UPC_EAN_DEBLUR -> {
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
                makePreferenceInvisible(getString(R.string.key_scanner_resolution))
                makePreferenceInvisible(getString(R.string.key_scanner_decoding_speed))
//                preferenceScreen.getPreference(BARCODE_TYPES_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(WEEBHOOK_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(INDIVIDUAL_TEMPLATES_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(CAMERA_SETTINGS_CATEGORY_INDEX).isVisible = false
            }

            ScanMode.MISSHAPED_1D -> {
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
                makePreferenceInvisible(getString(R.string.key_scanner_resolution))
                makePreferenceInvisible(getString(R.string.key_scanner_decoding_speed))
//                preferenceScreen.getPreference(BARCODE_TYPES_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(WEEBHOOK_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(INDIVIDUAL_TEMPLATES_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(CAMERA_SETTINGS_CATEGORY_INDEX).isVisible = false
            }

            ScanMode.ALL_1D -> {
                makePreferenceInvisible(getString(R.string.key_enable_location_in_preview))
                makePreferenceInvisible(getString(R.string.key_enable_roi))
                makePreferenceInvisible(getString(R.string.key_blured_scan_eanupc))
                makePreferenceInvisible(getString(R.string.key_bigger_viewfinder))
                makePreferenceInvisible(getString(R.string.key_narrow_viewfinder))
                makePreferenceInvisible(getString(R.string.key_result_searchEngine))
                makePreferenceInvisible(getString(R.string.key_enable_searchweb))
                makePreferenceInvisible(getString(R.string.key_automatic_show_bottomsheet2))
                makePreferenceInvisible(getString(R.string.key_reset_all_settings))
                preferenceScreen.getPreference(INDIVIDUAL_TEMPLATES_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(CAMERA_SETTINGS_CATEGORY_INDEX).isVisible = false
            }

            ScanMode.GLOBAL -> { makePreferenceInvisible(getString(R.string.key_bigger_viewfinder))
                makePreferenceInvisible(getString(R.string.key_narrow_viewfinder))
                makePreferenceInvisible(getString(R.string.key_reset_config))
                makePreferenceInvisible(getString(R.string.key_automatic_show_bottomsheet2))
                makePreferenceInvisible(getString(R.string.key_automatic_show_bottomsheet))
                preferenceScreen.getPreference(BARKODER_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(BARCODE_TYPES_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(RESULT_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(CAMERA_SETTINGS_CATEGORY_INDEX).isVisible = false

            }
            ScanMode.CONTINUOUS -> {
                makePreferenceInvisible(getString(R.string.key_continuous_scaning))
                makePreferenceInvisible(getString(R.string.key_bigger_viewfinder))
                makePreferenceInvisible(getString(R.string.key_narrow_viewfinder))
                makePreferenceInvisible(getString(R.string.key_automatic_show_bottomsheet))
                makePreferenceInvisible(getString(R.string.key_enable_searchweb))
                makePreferenceInvisible(getString(R.string.key_result_searchEngine))
                makePreferenceInvisible(getString(R.string.key_reset_all_settings))
                preferenceScreen.getPreference(WEEBHOOK_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(GENERAL_SETTINGS_CATEGORY_INDEX).isVisible = true
                preferenceScreen.getPreference(INDIVIDUAL_TEMPLATES_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(CAMERA_SETTINGS_CATEGORY_INDEX).isVisible = false
                var continuisTresHoldPreferences2 = findPreference<ListPreference>("pref_key_continuous_treshold2")!!
                continuisTresHoldPreferences2.isVisible = true
            }
            ScanMode.ANYSCAN -> {
                makePreferenceInvisible(getString(R.string.key_bigger_viewfinder))
                makePreferenceInvisible(getString(R.string.key_narrow_viewfinder))
                makePreferenceInvisible(getString(R.string.key_enable_searchweb))
                makePreferenceInvisible(getString(R.string.key_result_searchEngine))
                makePreferenceInvisible(getString(R.string.key_reset_all_settings))
                preferenceScreen.getPreference(WEEBHOOK_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(GENERAL_SETTINGS_CATEGORY_INDEX).isVisible = true
                preferenceScreen.getPreference(INDIVIDUAL_TEMPLATES_SETTINGS_CATEGORY_INDEX).isVisible = false
                preferenceScreen.getPreference(CAMERA_SETTINGS_CATEGORY_INDEX).isVisible = false
                makePreferenceInvisible(getString(R.string.key_automatic_show_bottomsheet2))

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
        }

        decodingSpeedPref.entries = entries.toTypedArray()
        decodingSpeedPref.entryValues = entryValues.toTypedArray()

        decodingSpeedPref.value =
            preferenceManager.sharedPreferences.getString(decodingSpeedPref.key)
    }

    private fun setBarkoderResolutionEntries() {
        val barkoderResolutionPref =
            findPreference<ListPreference>(getString(R.string.key_scanner_resolution))!!

        val entries: MutableList<CharSequence> = arrayListOf()
        val entryValues: MutableList<CharSequence> = arrayListOf()

        for (item in BarkoderResolution.values()) {
            entries.add(item.toString())
            entryValues.add(item.ordinal.toString())
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
    private fun defaultSearchEngine() {
        val searchEnginePref = findPreference<ListPreference>(getString(R.string.key_result_searchEngine))!!
        searchEnginePref.value = preferenceManager.sharedPreferences.getString(searchEnginePref.key)
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
        symbologyPref.customClickListener = View.OnClickListener {
            parentFragmentManager
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
                .commit()
        }
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

            }
            .setNegativeButton(R.string.cancel_button, null)
            .show()
    }

    private fun reloadAllPrefsValues() {
        if(scanMode == ScanMode.GLOBAL) {
            webhookEncodeDataPreference.isEnabled = true
            webhookFeedbackPreference.isEnabled = true
            webhookConfigurationPreference.isEnabled = true
            webhookAutosendPreference.isEnabled = true
            defaultSearchWebPreference.isEnabled = true
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

    private fun openWebHookConfigurationDialog() {
        val webHookPreference = findPreference<Preference>(getString(R.string.key_webhook_configuration))

        webHookPreference?.setOnPreferenceClickListener {
            val dialogFragment = WebHookConfigurationDialogFragment()
            dialogFragment.show(childFragmentManager, "WebHookConfigurationDialog")

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

    override fun onStop() {
        val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putBoolean("settingsChangedBoolean", false)
        editor.apply()
        super.onStop()
    }

}
