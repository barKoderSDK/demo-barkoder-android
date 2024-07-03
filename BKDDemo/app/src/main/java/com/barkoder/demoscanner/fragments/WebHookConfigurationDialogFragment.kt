package com.barkoder.demoscanner.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.barkoder.demoscanner.R
import com.barkoder.demoscanner.databinding.FragmentResultBottomDialogBinding
import com.barkoder.demoscanner.databinding.FragmentWebHookConfigurationDialogBinding
import com.barkoder.demoscanner.utils.CommonUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class WebHookConfigurationDialogFragment : DialogFragment() {

    private lateinit var urlEditText : EditText
    private lateinit var keyEditText : EditText
    private lateinit var buttonSave : Button
    private lateinit var buttonReset : Button
    private lateinit var buttonCancel : Button
    private lateinit var buttonShowMeHowSettings : Button

    private var _binding: FragmentWebHookConfigurationDialogBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentWebHookConfigurationDialogBinding.inflate(inflater, container, false)

        dialog!!.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        return binding.root
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val dialogView = layoutInflater.inflate(R.layout.fragment_web_hook_configuration_dialog, null)

        val builder = AlertDialog.Builder(requireContext())
        builder.setView(dialogView)

        urlEditText = dialogView.findViewById(R.id.editTextWebHookUrl)
        keyEditText = dialogView.findViewById(R.id.editTextWebHookKey)
        buttonSave = dialogView.findViewById(R.id.btnSaveWebHook)
        buttonReset = dialogView.findViewById(R.id.btnResetWebHook)
        buttonCancel = dialogView.findViewById(R.id.btnCancelWebHook)
        buttonShowMeHowSettings = dialogView.findViewById(R.id.btnShowMeHowSettings)

        return builder.create()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        val urlWebHook = sharedPreferences.getString(getString(R.string.key_url_webhook), "")
        val keyWebHook = sharedPreferences.getString(getString(R.string.key_secret_word_webhook), "")

        if (!urlWebHook.isNullOrEmpty()) {
            urlEditText.setText(urlWebHook)
        }
        if (!keyWebHook.isNullOrEmpty()) {
            keyEditText.setText(keyWebHook)
        }

        buttonShowMeHowSettings.setOnClickListener {
            CommonUtil.openURLInBrowser("https://docs.barkoder.com/en/how-to/webhooks", requireActivity())
        }

        buttonSave.setOnClickListener {

            val urlText = urlEditText.text.toString()
            val keyText = keyEditText.text.toString()
            if(CommonUtil.isTextURL(urlText)) {
                saveEditTextValuesToPreferences(getString(R.string.key_url_webhook), urlText)
                saveEditTextValuesToPreferences(getString(R.string.key_secret_word_webhook), keyText)
                dismiss()
            } else {
                dismiss()
                saveEditTextValuesToPreferences(getString(R.string.key_url_webhook), "")
                saveEditTextValuesToPreferences(getString(R.string.key_secret_word_webhook), "")
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage("Please enter valid URL")
                    .setNegativeButton("Continue") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }
        buttonReset.setOnClickListener {
            urlEditText.setText("")
            keyEditText.setText("")
            saveEditTextValuesToPreferences(getString(R.string.key_url_webhook), "")
            saveEditTextValuesToPreferences(getString(R.string.key_secret_word_webhook), "")
        }

        buttonCancel.setOnClickListener {
            dismiss()
        }

    }

    private fun saveEditTextValuesToPreferences(key: String, value: String) {
        val editor = sharedPreferences.edit()
        editor.putString(key, value)
        editor.apply()
    }

}