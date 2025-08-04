package com.barkoder.demoscanner

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.barkoder.Barkoder
import com.barkoder.BarkoderConfig
import com.barkoder.demoscanner.databinding.ActivityAboutBinding
import com.barkoder.demoscanner.fragments.ResultBottomDialogFragment
import com.barkoder.demoscanner.models.Changelog
import com.barkoder.demoscanner.repositories.BarcodeDataRepository
import com.barkoder.demoscanner.utils.CommonUtil
import com.barkoder.demoscanner.viewmodels.BarcodeDataViewModel
import com.barkoder.demoscanner.viewmodels.BarcodeDataViewModelFactory

class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding
    private val PREFS_NAME = "MyPrefsFile"
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        //Config for Device ID to use Barkoder.GetDeviceId()
        var config = BarkoderConfig(this, "license_key", null)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = ContextCompat.getColor(this, R.color.swipeCheckBackground2)
        }

        val fullTextFirstDescription = "Barcode Scanner Demo by barKoder showcases the enterprise-grade performance of the barKoder Barcode Scanner SDK along with most of its features in a wide variety of scanning scenarios."
        val fullTextSecondDescription = "Whether from One-Dimensional or Two-Dimensional barcodes, the barKoder API can capture the data reliably, accurately and surprisingly fast, even under very challenging conditions and environments."

// Create a SpannableString from the full text
        val spannableSecondDescription = SpannableString(fullTextSecondDescription)

// Set color for "One-Dimensional"
        val oneDimensionalStart = fullTextSecondDescription.indexOf("One-Dimensional")
        spannableSecondDescription.setSpan(
            ForegroundColorSpan(Color.RED),  // Set the color to red
            oneDimensionalStart,
            oneDimensionalStart + "One-Dimensional".length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

// Set color for "Two-Dimensional"
        val twoDimensionalStart = fullTextSecondDescription.indexOf("Two-Dimensional")
        spannableSecondDescription.setSpan(
            ForegroundColorSpan(Color.RED),  // Set the color to red
            twoDimensionalStart,
            twoDimensionalStart + "Two-Dimensional".length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

      binding.txtSecondDescription.text = spannableSecondDescription
// Create a SpannableString from the full text
        val spannableFirstDescription = SpannableString(fullTextFirstDescription)

// Set color for the first 5 words ("Barcode Scanner Demo by barKoder")
        spannableFirstDescription.setSpan(
            ForegroundColorSpan(Color.RED),  // Set the color to red
            0,  // Start index (beginning of the string)
            fullTextFirstDescription.indexOf("showcases"),  // End index (before the 6th word)
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

       binding.txtFirstDescription.text = spannableFirstDescription
        binding.txtAppVersion.text = BuildConfig.VERSION_NAME

        binding.txtSdkVersion.text = Barkoder.GetVersion()
        binding.txtLibVersion.text = Barkoder.GetLibVersion()

        binding.txtDeviceId.text = "${Barkoder.GetDeviceId()}"
        binding.btnGetTrialDemo.setOnClickListener {
            CommonUtil.openURLInBrowser(getString(R.string.testBarcodeLink), this@AboutActivity)
        }
        binding.btnLearnMore.run {
            setOnClickListener {
                CommonUtil.openURLInBrowser(getString(R.string.learnMoreLink), this@AboutActivity)
            }
            setOnLongClickListener {
//                binding.txtLibVersion.isVisible = !binding.txtLibVersion.isVisible
                true
            }
        }
        binding.txtPrivacyPolicy.setOnClickListener {
            CommonUtil.openURLInBrowser(getString(R.string.privacyPolicyLink), this@AboutActivity)
        }
        binding.txtTermsOfUse.setOnClickListener {
            CommonUtil.openURLInBrowser(getString(R.string.termsOfUseLink), this@AboutActivity)
        }

        binding.txtTermsOfUse.setOnTouchListener(object : View.OnTouchListener {
            private val handler = Handler()
            private val longClickDuration = 3000L // 3 seconds

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> handler.postDelayed({
                        val sharedPreferences: SharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

                        // Retrieve the current boolean value from SharedPreferences
                        var yourBooleanValue = sharedPreferences.getBoolean("BOOLEAN_KEY", false)

                        yourBooleanValue = !yourBooleanValue
                        val editor: SharedPreferences.Editor = sharedPreferences.edit()
                        editor.putBoolean("BOOLEAN_KEY", yourBooleanValue)
                        editor.apply()
                    }, longClickDuration)

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        handler.removeCallbacksAndMessages(null)
                    }
                }
                return false
            }
        })

        binding.cardMaui.setOnClickListener {
            CommonUtil.openURLInBrowser("https://barkoder.com/docs/v1/maui/net-maui-installation", this@AboutActivity)
        }

        binding.cardCordova.setOnClickListener {
            CommonUtil.openURLInBrowser("https://barkoder.com/docs/v1/cordova/cordova-installation", this@AboutActivity)
        }

        binding.cardFlutter.setOnClickListener {
            CommonUtil.openURLInBrowser("https://barkoder.com/docs/v1/flutter/flutter-installation", this@AboutActivity)
        }

        binding.cardCapacitor.setOnClickListener {
            CommonUtil.openURLInBrowser("https://barkoder.com/docs/v1/capacitor/capacitor-installation", this@AboutActivity)
        }

        binding.cardReactNative.setOnClickListener {
            CommonUtil.openURLInBrowser("https://barkoder.com/docs/v1/react-native/react-native-installation", this@AboutActivity)
        }

        binding.cardNativescript.setOnClickListener {
            CommonUtil.openURLInBrowser("https://barkoder.com/docs/v1/nativescript/nativescript-installation", this@AboutActivity)
        }


        
//        binding.btnQuestionMark.setOnClickListener {
//            CommonUtil.openURLInBrowser("https://docs.barkoder.com/en/how-to/demo-app-barKoder", this@AboutActivity)
//        }

        addChangeLogs()
    }


    private fun addChangeLogs() {
        val changeLogList = listOf(
            Changelog("1.7.8", arrayOf(
                "Implemented barKoder SDK v1.3.1.",
                "Various small bug fixes and performance enhancements.",
            )),
            Changelog("1.7.7", arrayOf(
                "Implemented barKoder SDK v1.2.11.",
                "Various small bug fixes and performance enhancements.",
            )),
            Changelog("1.7.6", arrayOf(
                "Implemented barKoder SDK v1.2.10.",
                "Added section for displaying device id in About screen.",
                "Various small bug fixes and performance enhancements."
            )),
            Changelog("1.7.5", arrayOf(
                "Implemented barKoder SDK v1.2.8.",
            )),
            Changelog("1.7.4", arrayOf(
                "Implemented barKoder SDK v1.2.7.",
                "Various small bug fixes and performance enhancements.",
            )),
            Changelog("1.7.3", arrayOf(
                "Implemented barKoder SDK v1.2.5.",
                "Added new 2D symbology DotCode.",
                "Added two new templates: All 1D & DotCode.",
                "Added two new showcases: Misshaped & DeBlur.",
                "Redesigned the Home screen."
            )),
            Changelog("1.7.2", arrayOf(
                "Implemented barKoder SDK v1.2.4.",
                "Updated the default settings for all scanning templates to provide better user experience.",
                "Fixed an issue with the zoom feature while changing scanning resolution.",
                "Improved the Continuous Scanning mode.",
                "Updated descriptions through the entire application."
            )),
            Changelog("1.7.0", arrayOf(
                "Implemented barKoder SDK v1.2.3.",
                "Added Vin Mode.",
                "Added MatrixSight decoding algorithm.",
                "UI icons refresh.",
                "Added individual settings for each mode."
            )),
            Changelog("1.6.0", arrayOf(
                "Implemented barKoder SDK v1.2.2.",
                "Complete redesign of the result screen.",
                "Added feature to export all scanned results to csv.",
                "Added feature to search the result on the Web.",
                "Added an option to choose default search engine.",
                "Added feature to send the result to webhook.",
                "Added option to configure the webhook.",
                "Added DPM scanning mode.",
                "Added Segment Scanning option for enabling Misshaped Code Capture for 1D barcodes in settings.",
                "Improved 1D Industrial and Retail to work with Misshaped Codes.",
                "Added feature to scan heavily blurred EAN & UPC codes.",
            )),

            Changelog("1.4.0", arrayOf("Implemented barKoder SDK v1.2.1",
                                                    "Improved Batch Multi Scan",
                                                    "Improved QR scanning"
                )),

            Changelog(
                "1.3.0",
                arrayOf(
                    "Implemented barKoder SDK v1.2.0",
                    "Added support for decoding Code 2 of 5 (Standard/Intrustrial 2 of 5), Interleaved 2 of 5, ITF 14, IATA 2 of 5, Matrix 2 of 5, Datalogic 2 of 5, COOP 2 of 5, Code 32 (Italian Pharmacode) and Tepelen barcode types",
                    "Massive improvement to the Data Matrix decoding engine",
                    "Added option to enable/disable \"Expand to UPC-A\" property for UPC-E and UPC-E1 barcode types",
                    "Every template now has its own settings. For every template or scan mode you can change the default different setting options",
                    "\"Show region of interest\" setting is now replaced by \"Enable region of interest\". If region of interest is enabled it will be shown and play part of the scanning screen",
                    "You can now reset all settings to default by tapping on the \"Reset config\" field in the General settings",
                    "The About screen has been completely redesigned",
                    "Batch Mutli Scan has replaced the Continuous template. The BarKoder can now scan more than one barcode in a single frame, and only the unique results will be shown in one scanning session",
                    "Various small bug fixes and performance enhancements"
                )
            ),
            Changelog(
                "1.2.3",
                arrayOf(
                    "Update barKoder SDK to v1.1.9",
                    "Performance improvements"
                )
            ),
            Changelog(
                "1.2.2",
                arrayOf(
                    "Update barKoder SDK to v1.1.7"
                )
            ),
            Changelog(
                "1.2.1",
                arrayOf(
                    "Update barKoder SDK to v1.1.6",
                    "Add Result charset setting"
                )
            ),
            Changelog(
                "1.2.0",
                arrayOf(
                    "Update barKoder SDK to v1.1.5"
                )
            ),
            Changelog(
                "1.1.0",
                arrayOf(
                    "Update barKoder SDK to v1.1.4",
                    "Remove Enhance blurred detection (experimental) setting. It's implemented in the Barkoder SDK",
                    "Use range sliders for barcode length setting"
                )
            ),
            Changelog(
                "1.0.0",
                arrayOf(
                    "Scanning 1D barcodes: C128, C93, C39, Codabar, C11, MIS, UPCA, UPCE, UPCE 1, EAN 13, EAN 8",
                    "Scanning 2D barcodes: Aztec, Aztec Compat, QR, QR Micro, PDF417, PDF417 Micro, Data Matrix",
                    "Formatting results using GS1 or AAMVA formatting types",
                    "Change decoding frames speed: Fast (try to scan the frame as fast as possible), Normal and Slow (scanning frame as much time as needed to find and scan barcode)",
                    "Availability to use pinch for zooming the preview"
                )
            )
        )

        changeLogList.forEach { changelog ->

            val changeLogView =
                layoutInflater.inflate(android.R.layout.two_line_list_item, null)
            changeLogView.findViewById<TextView>(android.R.id.text1)!!.let {
                it.text = "v${changelog.releaseVersion}"
                it.textSize = 12f
            }
            changeLogView.findViewById<TextView>(android.R.id.text2)!!.let {
                it.text = changelog.descriptionAsString()
                it.textSize = 12f

                it.setLineSpacing(10f, 1f)
            }
            changeLogView.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(
                    resources.getDimension(R.dimen.about_screen_sides_margin).toInt(),
                    0,
                    resources.getDimension(R.dimen.about_screen_sides_margin).toInt(),
                    0
                )
            }

//            binding.llChangeLogContent.addView(changeLogView)
        }
    }
}
