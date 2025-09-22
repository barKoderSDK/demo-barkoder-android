package com.barkoder.demoscanner.fragments

import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.barkoder.demoscanner.R
import com.barkoder.demoscanner.databinding.FragmentResultBottomDialogBinding
import com.barkoder.demoscanner.databinding.FragmentWebHookConfigurationDialogBinding
import com.barkoder.demoscanner.utils.CommonUtil
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class WebHookConfigurationDialogFragment : DialogFragment() {

    private lateinit var urlEditText : EditText
    private lateinit var keyEditText : EditText
    private lateinit var buttonSave : Button
    private lateinit var buttonReset : Button
    private lateinit var HTTPButton : MaterialButtonToggleGroup
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
        HTTPButton = dialogView.findViewById(R.id.toggleGroupWebhookMode)





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
            CommonUtil.openURLInBrowser("https://barkoder.com/docs/v1/how-to/use-webhooks-demo-app", requireActivity())
        }




        urlEditText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val isEmpty = s.isNullOrBlank()
                urlEditText.setBackgroundResource(
                    if (isEmpty) R.drawable.custom_edit_text_bg
                    else R.drawable.edit_text_bg_normal
                )
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

// 👉 Initial check
        urlEditText?.post {
            val isEmpty = urlEditText.text.isNullOrBlank()
            urlEditText.setBackgroundResource(
                if (isEmpty) R.drawable.custom_edit_text_bg
                else R.drawable.edit_text_bg_normal
            )
        }

        val selectedButtonId = HTTPButton.checkedButtonId

        if (selectedButtonId != View.NO_ID) {
            val selectedButton = HTTPButton.findViewById<MaterialButton>(selectedButtonId)
            val selectedText = selectedButton.text.toString()

            Log.d("SelectedToggle", "Selected: $selectedText")
        }
        val savedProtocol = sharedPreferences.getString("selected_protocol", "https")

        if (savedProtocol == "http") {
            HTTPButton.check(R.id.btnHTTP)
        } else {
            HTTPButton.check(R.id.btnHTTPS)
        }

        if (!urlWebHook.isNullOrEmpty()) {
            // Strip the protocol for displaying
            val urlWithoutProtocol = urlWebHook.removePrefix("https://").removePrefix("http://")
            urlEditText.setText(urlWithoutProtocol)
        }

        if (!keyWebHook.isNullOrEmpty()) {
            keyEditText.setText(keyWebHook)
        }
        buttonSave.setOnClickListener {

            val urlTextRaw = urlEditText.text.toString().trim()
            val keyText = keyEditText.text.toString()

            val selectedButtonId = HTTPButton.checkedButtonId
            val selectedButton = HTTPButton.findViewById<MaterialButton>(selectedButtonId)
            val selectedProtocol = selectedButton.text.toString().lowercase() // "https" or "http"



            val fullUrl = if (!urlTextRaw.startsWith("http")) {
                "$selectedProtocol://$urlTextRaw"
            } else {
                urlTextRaw
            }

            if (CommonUtil.isTextURL(fullUrl)) {
                // ✅ Save values to preferences
                Log.d("fullRUREAr", fullUrl)
                saveEditTextValuesToPreferences(getString(R.string.key_url_webhook), fullUrl)
                saveEditTextValuesToPreferences(getString(R.string.key_secret_word_webhook), keyText)

                // ✅ Also save the selected protocol
                saveEditTextValuesToPreferences("selected_protocol", selectedProtocol)


                dismiss()
            } else {
                // ❌ Invalid URL, show error and reset
                dismiss()
                saveEditTextValuesToPreferences(getString(R.string.key_url_webhook), "")
                saveEditTextValuesToPreferences(getString(R.string.key_secret_word_webhook), "")

                MaterialAlertDialogBuilder(requireContext())
                    .setMessage("Please enter a valid URL")
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
            HTTPButton.check(R.id.btnHTTPS)
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