package com.barkoder.demoscanner.fragments

import android.app.AlertDialog
import android.app.Dialog
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
import com.barkoder.demoscanner.databinding.FragmentNotConfiguredWebHookDialogBinding
import com.barkoder.demoscanner.databinding.FragmentWebHookConfigurationDialogBinding
import com.barkoder.demoscanner.utils.CommonUtil

class NotConfiguredWebHookDialog : DialogFragment() {

    private lateinit var buttonShowMeHow : Button
    private lateinit var buttonCancel : Button

    private var _binding: FragmentNotConfiguredWebHookDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentNotConfiguredWebHookDialogBinding.inflate(inflater, container, false)

        dialog!!.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val dialogView = layoutInflater.inflate(R.layout.fragment_not_configured_web_hook_dialog, null)

        val builder = AlertDialog.Builder(requireContext())
        builder.setView(dialogView)

        buttonShowMeHow = dialogView.findViewById(R.id.btnShowMeHow)
        buttonCancel = dialogView.findViewById(R.id.btnClose)

        return builder.create()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        buttonShowMeHow.setOnClickListener {
            CommonUtil.openURLInBrowser("https://docs.barkoder.com/en/how-to/webhooks", requireActivity())
        }

        buttonCancel.setOnClickListener {
            dismiss()
        }
    }

}