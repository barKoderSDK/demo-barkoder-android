package com.barkoder.demoscanner.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import androidx.preference.get
import androidx.recyclerview.widget.RecyclerView
import com.barkoder.Barkoder
import com.barkoder.demoscanner.R
import com.barkoder.demoscanner.customcontrols.CategoryRoundedCornersDecoration
import com.barkoder.demoscanner.customcontrols.ClickablePreferenceCategory
import com.barkoder.demoscanner.customcontrols.CustomRangePreference
import com.barkoder.demoscanner.customcontrols.MarginDividerItemDecoration
import com.barkoder.demoscanner.enums.ScanMode
import com.barkoder.demoscanner.utils.getBoolean
import com.barkoder.demoscanner.utils.getInt
import com.barkoder.demoscanner.utils.getString
import com.google.android.material.internal.ViewUtils.dpToPx
import kotlin.math.roundToInt

class AdvancedSettingsFragment : PreferenceFragmentCompat() {

    private lateinit var scanMode: ScanMode
    private lateinit var typeKey: String
    private lateinit var sharedPreferences : SharedPreferences
    private val PREFS_NAME = "MyPrefsFile"

    private lateinit var lengthPref: CustomRangePreference

    private val LENGTH_CATEGORY_INDEX = 0
    private val CHECKSUM_CATEGORY_INDEX = 1
    private val ADDITIONAL_SETTINGS_CATEGORY_INDEX = 2

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        scanMode = ScanMode.values()[requireArguments().getInt(ARGS_MODE_KEY)]

        if (scanMode != ScanMode.GLOBAL)
            preferenceManager.sharedPreferencesName =
                requireActivity().packageName + scanMode.prefKey

        setPreferencesFromResource(R.xml.preferences_advanced_settings, rootKey)

        requireActivity().title = requireArguments().getString(ARGS_TITLE)
        typeKey = requireArguments().getString(ARGS_TYPE_KEY)!!

        setLengthPrefs()
        setChecksumPref()
        setAdditionalSettingsPrefs()


    }


    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(androidx.preference.R.id.recycler_view)

        recyclerView?.apply {
            background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_shape_png)
            // Remove setBackgroundColor if setting background already
            // setBackgroundColor(Color.TRANSPARENT)

            val marginStartPx = dpToPx(requireContext(), 20).toInt()
            val marginEndPx = dpToPx(requireContext(), 20).toInt()

            addItemDecoration(
                MarginDividerItemDecoration(
                    requireContext(),
                    R.drawable.preference_divider,
                    marginStartPx,
                    marginEndPx
                )
            )

            addItemDecoration(CategoryRoundedCornersDecoration(requireContext()))
            invalidateItemDecorations()
        }
    }
    private fun setLengthPrefs() {
        when (typeKey) {
            getString(R.string.key_symbology_c128),
            getString(R.string.key_symbology_c93),
            getString(R.string.key_symbology_c39),
            getString(R.string.key_symbology_codabar),
            getString(R.string.key_symbology_c11),
            getString(R.string.key_symbology_msi),
            getString(R.string.key_symbology_c25),
            getString(R.string.key_symbology_i2o5),
            getString(R.string.key_symbology_iata25),
            getString(R.string.key_symbology_matrix25),
            getString(R.string.key_symbology_dataLogic25),
            getString(R.string.key_symbology_coop25),
            getString(R.string.key_symbology_c32),
            getString(R.string.key_symbology_telepen),
            getString(R.string.key_symbology_dotcode) -> {
                preferenceScreen.getPreference(LENGTH_CATEGORY_INDEX).isVisible = true

                val currentMinValue = preferenceManager.sharedPreferences.getInt(
                    typeKey + getString(R.string.key_min_length)
                )
                val currentMaxValue = preferenceManager.sharedPreferences.getInt(
                    typeKey + getString(R.string.key_max_length)
                )
                lengthPref = findPreference(getString(R.string.key_length))!!
                lengthPref.setValues(listOf(currentMinValue, currentMaxValue))
                lengthPref.addOnChangeListener { rangeSlider, _, _ ->
                    if(scanMode.template != null) {
                        val sharedPreferences: SharedPreferences =
                            requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        val editor: SharedPreferences.Editor = sharedPreferences.edit()
                        editor.putBoolean("settingsChangedBoolean", true)
                        editor.apply()
                    }
                    preferenceManager.sharedPreferences.edit()
                        .putInt(
                            typeKey + getString(R.string.key_min_length),
                            rangeSlider.values[0].roundToInt()
                        )
                        .putInt(
                            typeKey + getString(R.string.key_max_length),
                            rangeSlider.values[1].roundToInt()
                        ).apply()
                }

                (preferenceScreen.getPreference(LENGTH_CATEGORY_INDEX) as ClickablePreferenceCategory).preferenceCategoryClickListener =
                    object : ClickablePreferenceCategory.PreferenceCategoryClickListener {
                        override fun onPreferenceCategoryClick() {
                            enterLengthRange(lengthPref.getValues()[0], lengthPref.getValues()[1])
                        }
                    }
            }

            else -> preferenceScreen.getPreference(LENGTH_CATEGORY_INDEX).isVisible = false
        }
    }

    private fun setChecksumPref() {
        when (typeKey) {
            getString(R.string.key_symbology_c11),
            getString(R.string.key_symbology_c39),
            getString(R.string.key_symbology_msi),
            getString(R.string.key_symbology_c25),
            getString(R.string.key_symbology_i2o5),
            getString(R.string.key_symbology_iata25),
            getString(R.string.key_symbology_matrix25),
            getString(R.string.key_symbology_dataLogic25),
            getString(R.string.key_symbology_coop25) -> {
                preferenceScreen.getPreference(CHECKSUM_CATEGORY_INDEX).isVisible = true

                val checksumPref =
                    findPreference<ListPreference>(getString(R.string.key_checksum_type))!!

                checksumPref.entries = getChecksumPrefEntries()
                checksumPref.entryValues = getChecksumPrefEntryValues()

                checksumPref.key = typeKey + checksumPref.key

                checksumPref.value =
                    preferenceManager.sharedPreferences.getString(
                        checksumPref.key
                    )
                checksumPref.isPersistent = true

                checksumPref.setOnPreferenceChangeListener { preference, newValue ->
                    if(scanMode.template != null) {
                        val sharedPreferences: SharedPreferences =
                            requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        val editor: SharedPreferences.Editor = sharedPreferences.edit()
                        editor.putBoolean("settingsChangedBoolean", true)
                        editor.apply()
                    }
                    true
                }

            }

            else -> preferenceScreen.getPreference(CHECKSUM_CATEGORY_INDEX).isVisible = false
        }
    }

    private fun setAdditionalSettingsPrefs() {
        when (typeKey) {
            getString(R.string.key_symbology_upce),
            getString(R.string.key_symbology_upce1) -> {
                setAdditionalSettingPrefForExpandToUpcA(
                    preferenceManager.sharedPreferences.getBoolean(
                        typeKey + getString(R.string.key_expand_to_upca)
                    )
                )
            }

            else -> preferenceScreen.getPreference(ADDITIONAL_SETTINGS_CATEGORY_INDEX).isVisible =
                false
        }
    }

    private fun getChecksumPrefEntries(): Array<CharSequence>? {
        val checksums: MutableList<CharSequence> = arrayListOf()
        return when (typeKey) {
            getString(R.string.key_symbology_c11) -> {
                for (item in Barkoder.Code11ChecksumType.values())
                    checksums.add(item.name)

                checksums.toTypedArray()
            }

            getString(R.string.key_symbology_c39) -> {
                for (item in Barkoder.Code39ChecksumType.values())
                    checksums.add(item.name)

                checksums.toTypedArray()
            }

            getString(R.string.key_symbology_msi) -> {
                for (item in Barkoder.MsiChecksumType.values())
                    checksums.add(item.name)

                checksums.toTypedArray()
            }

            getString(R.string.key_symbology_c25),
            getString(R.string.key_symbology_i2o5),
            getString(R.string.key_symbology_iata25),
            getString(R.string.key_symbology_matrix25),
            getString(R.string.key_symbology_dataLogic25),
            getString(R.string.key_symbology_coop25) -> {
                for (item in Barkoder.StandardChecksumType.values())
                    checksums.add(item.name)

                checksums.toTypedArray()
            }

            else -> null
        }
    }

    private fun getChecksumPrefEntryValues(): Array<CharSequence>? {
        val checksumValues: MutableList<CharSequence> = arrayListOf()
        return when (typeKey) {
            getString(R.string.key_symbology_c11) -> {
                for (item in Barkoder.Code11ChecksumType.values())
                    checksumValues.add(item.ordinal.toString())

                checksumValues.toTypedArray()
            }

            getString(R.string.key_symbology_c39) -> {
                for (item in Barkoder.Code39ChecksumType.values())
                    checksumValues.add(item.ordinal.toString())

                checksumValues.toTypedArray()
            }

            getString(R.string.key_symbology_msi) -> {
                for (item in Barkoder.MsiChecksumType.values())
                    checksumValues.add(item.ordinal.toString())

                checksumValues.toTypedArray()
            }

            getString(R.string.key_symbology_c25),
            getString(R.string.key_symbology_i2o5),
            getString(R.string.key_symbology_iata25),
            getString(R.string.key_symbology_matrix25),
            getString(R.string.key_symbology_dataLogic25),
            getString(R.string.key_symbology_coop25) -> {
                for (item in Barkoder.StandardChecksumType.values())
                    checksumValues.add(item.ordinal.toString())

                checksumValues.toTypedArray()
            }

            else -> null
        }
    }

    private fun enterLengthRange(min: Int, max: Int) {
        val rangeDialogView =
            LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_length_range, null, false)

        val minValueEdt = rangeDialogView.findViewById<EditText>(R.id.edtMinValue)
        minValueEdt.setText(min.toString())
        val maxValueEdt = rangeDialogView.findViewById<EditText>(R.id.edtMaxValue)
        maxValueEdt.setText(max.toString())

        AlertDialog.Builder(requireContext())
            .setView(rangeDialogView)
            .setTitle(R.string.length_settings_title)
            .setPositiveButton(R.string.save_button, null)
            .setNegativeButton(R.string.cancel_button, null)
            .create().apply {
                setOnShowListener {
                    if(scanMode.template != null) {
                        val sharedPreferences: SharedPreferences =
                            requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        val editor: SharedPreferences.Editor = sharedPreferences.edit()
                        editor.putBoolean("settingsChangedBoolean", true)
                        editor.apply()
                    }
                    findViewById<Button>(android.R.id.button1)?.setOnClickListener {
                        val minValue = minValueEdt.text.toString().toIntOrNull()
                        val maxValue = maxValueEdt.text.toString().toIntOrNull()

                        when {
                            minValue == null || maxValue == null -> {
                                Toast.makeText(
                                    it.context,
                                    R.string.length_range_not_set,
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            minValue !in CustomRangePreference.MIN_ALLOWED_VALUE..maxValue -> {
                                Toast.makeText(
                                    it.context,
                                    R.string.length_min_warning,
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            maxValue !in minValue..CustomRangePreference.MAX_ALLOWED_VALUE -> {
                                Toast.makeText(
                                    it.context,
                                    R.string.length_max_warning,
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            else -> {
                                lengthPref.setValues(listOf(minValue, maxValue), true)

                                dismiss()
                            }
                        }
                    }
                }
                show()
            }
    }

    private fun setAdditionalSettingPrefForExpandToUpcA(
        value: Boolean
    ) {
        (preferenceScreen.getPreference(ADDITIONAL_SETTINGS_CATEGORY_INDEX) as PreferenceCategory).let {
            it.isVisible = true

            val switchPref = it[0] as SwitchPreference

            if(scanMode.template != null) {
                it.let {
                    for (i in 0 until it.preferenceCount) {
                        val preference2: Preference = it.getPreference(i)

                        preference2.onPreferenceChangeListener =
                            Preference.OnPreferenceChangeListener { preference, newValue ->
                                val sharedPreferences: SharedPreferences =
                                    requireContext().getSharedPreferences(
                                        PREFS_NAME,
                                        Context.MODE_PRIVATE
                                    )
                                val editor: SharedPreferences.Editor = sharedPreferences.edit()
                                editor.putBoolean("settingsChangedBoolean", true)
                                editor.apply()
                                true
                            }
                       }
                    }
                }

            switchPref.title = getString(R.string.expand_to_upca_title)
            switchPref.key = typeKey + getString(R.string.key_expand_to_upca)
            switchPref.isChecked = value
            switchPref.isPersistent = true
        }
    }

    companion object {
        val TAG = AdvancedSettingsFragment::class.java.simpleName
        private const val ARGS_TYPE_KEY = "typeKey"
        private const val ARGS_TITLE = "title"
        private const val ARGS_MODE_KEY = "advancedSettingsScanMode"

        @JvmStatic
        fun newInstance(typeKey: String, title: String, scanModeOrdinal: Int): Fragment {
            val args = Bundle()
            args.putString(ARGS_TYPE_KEY, typeKey)
            args.putString(ARGS_TITLE, title)
            args.putInt(ARGS_MODE_KEY, scanModeOrdinal)

            return AdvancedSettingsFragment().apply {
                arguments = args
            }
        }
    }
}
