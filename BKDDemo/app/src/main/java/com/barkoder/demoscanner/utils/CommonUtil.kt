package com.barkoder.demoscanner.utils

import android.app.Activity
import android.content.*
import android.content.Context.CLIPBOARD_SERVICE
import android.content.res.Resources
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Patterns
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.barkoder.demoscanner.R

object CommonUtil {

    fun dpToPx(dp: Int): Int {
        return (dp * Resources.getSystem().displayMetrics.density).toInt()
    }

    fun isTextURL(text: String?): Boolean {
        return if (!text.isNullOrBlank()) {
            Patterns.WEB_URL.matcher(text).matches()
        } else
            false
    }

    fun openURLInBrowser(url: String, activity: Activity) {
        try {
            var urlToOpen = url

            if (!urlToOpen.startsWith("http"))
                urlToOpen = "https://$urlToOpen"

            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlToOpen)))
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(activity, R.string.about_learn_more_no_browser, Toast.LENGTH_LONG)
                .show()
        }
    }

    fun setAlertDialogMessageAsURL(
        dialog: AlertDialog,
        context: Context
    ) {
        dialog.findViewById<TextView>(android.R.id.message)?.let { messageView ->
            messageView.paintFlags = messageView.paintFlags or Paint.UNDERLINE_TEXT_FLAG

            if (context is Activity) {
                messageView.setOnClickListener {
                    openURLInBrowser(
                        messageView.text!!.toString(),
                        context
                    )
                }
            }
        }
    }

    fun copyTextToClipboard(textToCopy: String?, context: Context?) {
        if (!textToCopy.isNullOrBlank()) {
            context?.run {
                val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clipboardManager.setPrimaryClip(ClipData.newPlainText("", textToCopy))
                // Only show a toast for Android 12 and lower.
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S)
                    Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
            }
        }
    }


    fun openSearchInBrowser(url: String, query : String, activity: Activity) {
        try {
            var urlToOpen = url + query

            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlToOpen)))
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(activity, R.string.about_learn_more_no_browser, Toast.LENGTH_LONG)
                .show()
        }
    }
}
