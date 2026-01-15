package com.barkoder.demoscanner

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.widget.ScrollView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.barkoder.Barkoder
import com.barkoder.BarkoderConfig
import com.barkoder.BarkoderHelper
import com.barkoder.demoscanner.databinding.ActivityMainBinding
import com.barkoder.demoscanner.enums.ScanMode
import com.barkoder.demoscanner.fragments.ResultBottomDialogFragment
import com.barkoder.demoscanner.fragments.SettingsFragment
import com.barkoder.demoscanner.models.RecentScan2
import com.barkoder.demoscanner.models.SessionScan
import com.barkoder.demoscanner.utils.BKDConfigUtil
import com.barkoder.demoscanner.utils.CommonUtil
import com.barkoder.demoscanner.utils.ImageUtil
import com.barkoder.demoscanner.viewmodels.RecentScanViewModel
import com.barkoder.interfaces.BarkoderResultCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.view.doOnPreDraw
import androidx.core.widget.NestedScrollView
import com.barkoder.demoscanner.fragments.TutorialDialogFragment
import com.barkoder.demoscanner.utils.SpotlightOverlayView
import com.google.firebase.analytics.ktx.logEvent
import kotlin.apply
import kotlin.compareTo
import kotlin.div
import kotlin.math.abs
import kotlin.math.max
import kotlin.text.toInt
import kotlin.times


class MainActivity : AppCompatActivity(), BarkoderResultCallback, TutorialDialogFragment.Callbacks {

    private lateinit var binding: ActivityMainBinding
    private var existingFragment: ResultBottomDialogFragment? = null
    private lateinit var sharedViewModel: com.barkoder.demoscanner.viewmodels.ScanResultSharedViewModel

    private lateinit var pickImageResult: ActivityResultLauncher<Any>

    private var pictureBitmap: Bitmap? = null
    private var documentBitmap: Bitmap? = null
    private var signatureBitmap: Bitmap? = null
    private var mainBitmap: Bitmap? = null
    private var croppedBarcodePath: String? = null
    private var croppedBarcodeImage: Bitmap? = null
    private var croppedBarcodeImageOnScannedBarcode: Bitmap? = null
    private lateinit var recentViewModel: RecentScanViewModel
    private var context: Context? = null
    private var resultsTemp: Array<out Barkoder.Result>? = null
    private var showTutorialFromSettings = false
    private var picturePath: String? = null
    private var documentPath: String? = null
    private var signaturePath: String? = null
    private var mainPath: String? = null
    var sessionScansAdapterData: MutableList<SessionScan> = mutableListOf()


    private val spotlightHandler = Handler(Looper.getMainLooper())
    private var pendingSpotlightRunnable: Runnable? = null


    companion object {
        var barkoderConfig: BarkoderConfig? = null
        private const val REQUEST_SETTINGS = 1001
        private const val EXTRA_SHOW_TUTORIAL = "extra_show_tutorial"
        private const val SHOW_TUTORIAL_ALWAYS = true
    }
    private val activityJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + activityJob)
    private var tutorialOverlay: SpotlightOverlayView? = null
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        context = this
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        
        sharedViewModel = ViewModelProvider(this).get(com.barkoder.demoscanner.viewmodels.ScanResultSharedViewModel::class.java)


        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    var consumed = false

                    val tutorialDialog =
                        supportFragmentManager.findFragmentByTag("tutorial_dialog") as? TutorialDialogFragment
                    if (tutorialDialog != null) {
                        tutorialDialog.dismissAllowingStateLoss()
                        consumed = true
                    }

                    val overlay = tutorialOverlay
                    if (overlay != null && overlay.isVisible) {
                        // Ensure it is really gone on the same back press
                        hideSpotlight()
                        (overlay.parent as? ViewGroup)?.removeView(overlay)
                        tutorialOverlay = null
                        consumed = true
                    }

                    if (consumed) return

                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        )


        pickImageResult = registerForActivityResult(
            object : ActivityResultContract<Any, Uri?>() {
                override fun createIntent(context: Context, input: Any): Intent {
                    return when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && input is PickVisualMediaRequest -> {
                            // The new photo picker
                            ActivityResultContracts.PickVisualMedia().createIntent(context, input)
                        }
                        input is Intent -> input
                        else -> throw IllegalArgumentException("Invalid input type")
                    }
                }

                override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
                    return when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                            // On Android 13+, the new picker returns Uri directly
                            intent?.data
                        }
                        else -> intent?.data
                    }
                }
            }
        ) { uri: Uri? ->
            if (uri != null) {
                scanImageFromUri(uri)
            } else {
                Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
            }
        }

        BKDConfigUtil.configureBKD(
            this,
            ScanMode.GALLERY_SCAN,
            true
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = ContextCompat.getColor(this, R.color.swipeCheckBackground2)
        }

        recentViewModel = ViewModelProvider(this).get(RecentScanViewModel::class.java)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val defaultSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)

        Log.d("libVersion", Barkoder.GetLibVersion())
        // For GLOBAL and CONTINUOUS scan modes this can be done on app start because app default values
        // are used. For all other scan modes (that are related with templates) first we need to apply
        // related template to the config, and later use config (template) values as default ones (done in BKDConfigUtil)
        BKDConfigUtil.setDefaultValuesInPrefs(defaultSharedPrefs, this, true, ScanMode.GLOBAL)
        BKDConfigUtil.setDefaultValuesInPrefs(
            getSharedPreferences(packageName + ScanMode.ANYSCAN.prefKey, Context.MODE_PRIVATE),
            this,
            true,
            ScanMode.ANYSCAN
        )
        BKDConfigUtil.setDefaultValuesInPrefs(
            getSharedPreferences(packageName + ScanMode.CONTINUOUS.prefKey, Context.MODE_PRIVATE),
            this,
            true,
            ScanMode.CONTINUOUS
        )

        BKDConfigUtil.setDefaultValuesInPrefs(
            getSharedPreferences(packageName + ScanMode.MISSHAPED_1D.prefKey, Context.MODE_PRIVATE),
            this,
            true,
            ScanMode.MISSHAPED_1D
        )

        BKDConfigUtil.setDefaultValuesInPrefs(
            getSharedPreferences(
                packageName + ScanMode.UPC_EAN_DEBLUR.prefKey,
                Context.MODE_PRIVATE
            ),
            this,
            true,
            ScanMode.UPC_EAN_DEBLUR
        )

//        BKDConfigUtil.setDefaultValuesInPrefs(
//            getSharedPreferences(
//                packageName + ScanMode.AR_MODE.prefKey,
//                Context.MODE_PRIVATE
//            ),
//            this,
//            true,
//            ScanMode.AR_MODE
//        )

        firebaseAnalytics = Firebase.analytics

        binding.cardBarcodesIndustrial1D.setOnClickListener {
            startActivity(getScannerIntent(ScanMode.INDUSTRIAL_1D))
            firebaseAnalytics.logEvent("scan_mode_opened") {
                param("scan_mode", "industrial_1d")

            }
        }

        binding.cardBarcodesRetail1D.setOnClickListener {
            startActivity(getScannerIntent(ScanMode.RETAIL_1D))
            firebaseAnalytics.logEvent("scan_mode_opened") {
                param("scan_mode", "retail_1d")

            }
        }

        binding.cardBarcodesPDF.setOnClickListener {
            startActivity(getScannerIntent(ScanMode.PDF))
            firebaseAnalytics.logEvent("scan_mode_opened") {
                param("scan_mode", "pdf_optimized")

            }
        }

//        binding.cardBarcodesQR.setOnClickListener {
//            startActivity(getScannerIntent(ScanMode.QR))
//        }

        binding.cardBarcodes2DAll.setOnClickListener {
            startActivity(getScannerIntent(ScanMode.ALL_2D))
            firebaseAnalytics.logEvent("scan_mode_opened") {
                param("scan_mode", "all_2d")

            }
        }

        binding.cardScanFromGallery.setOnClickListener {
            scanFromGallery()
            firebaseAnalytics.logEvent("scan_mode_opened") {
                param("scan_mode", "gallery_scan")

            }
        }

        binding.cardContinuousMode.setOnClickListener {
            startActivity(getScannerIntent(ScanMode.CONTINUOUS))
            firebaseAnalytics.logEvent("scan_mode_opened") {
                param("scan_mode", "multiscan")

            }
        }

        binding.cardDpmMode.setOnClickListener {
            startActivity(getScannerIntent(ScanMode.DPM))
            firebaseAnalytics.logEvent("scan_mode_opened") {
                param("scan_mode", "dpm")

            }
        }
        binding.cardVinMode.setOnClickListener {
            startActivity(getScannerIntent(ScanMode.VIN))
            firebaseAnalytics.logEvent("scan_mode_opened") {
                param("scan_mode", "vin")

            }
        }
        binding.cardDotCode.setOnClickListener {
            startActivity(getScannerIntent(ScanMode.DOTCODE))
            firebaseAnalytics.logEvent("scan_mode_opened") {
                param("scan_mode", "dotcode")

            }
        }
        binding.cardMisshaped1D.setOnClickListener {
            startActivity(getScannerIntent(ScanMode.MISSHAPED_1D))
            firebaseAnalytics.logEvent("scan_mode_opened") {
                param("scan_mode", "misshaped")

            }
        }
        binding.cardUpcEanDeblur.setOnClickListener {
            startActivity(getScannerIntent(ScanMode.UPC_EAN_DEBLUR))
            firebaseAnalytics.logEvent("scan_mode_opened") {
                param("scan_mode", "deblur")

            }
        }
        binding.cardBarcodes1DALL.setOnClickListener {
            startActivity(getScannerIntent(ScanMode.ALL_1D))
            firebaseAnalytics.logEvent("scan_mode_opened") {
                param("scan_mode", "all_1d")

            }
        }
        binding.cardScanWebdemo.setOnClickListener {
            CommonUtil.openURLInBrowser("https://barkoder.com/barkoder-wasm-demo", this)
            firebaseAnalytics.logEvent("scan_mode_opened") {
                param("scan_mode", "webdemo")

            }
        }
        binding.layoutRecent.setOnClickListener {
            startActivity(Intent(this@MainActivity, RecentActivity::class.java))
        }

        binding.layoutAbout.setOnClickListener {
            startActivity(Intent(this@MainActivity, AboutActivity::class.java))
        }

        binding.txtScan.setOnClickListener {
            startActivity(getScannerIntent(ScanMode.ANYSCAN))
        }

        binding.cardScanIdDocument.setOnClickListener {
            startActivity(getScannerIntent(ScanMode.MRZ))
            firebaseAnalytics.logEvent("scan_mode_opened") {
                param("scan_mode", "mrz")

            }
        }
        binding.compositeCard.setOnClickListener {
            startActivity(getScannerIntent(ScanMode.COMPOSITE))
            firebaseAnalytics.logEvent("scan_mode_opened") {
                param("scan_mode", "composite")

            }
        }
        binding.cardBarcodesPostal.setOnClickListener {
            startActivity(getScannerIntent(ScanMode.POSTAL_CODES))
            firebaseAnalytics.logEvent("scan_mode_opened") {
                param("scan_mode", "postal_codes")

            }
        }

        binding.cardArMode.setOnClickListener {
            startActivity(getScannerIntent(ScanMode.AR_MODE))
            firebaseAnalytics.logEvent("scan_mode_opened") {
                param("scan_mode", "ar")

            }
        }

    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        if (intent.getBooleanExtra("extra_opened_tutorial_from_settings", false)) {
          showTutorialEveryTime()
        }
    }

    private fun createConfig(appContext: Context): BarkoderConfig {
        return BarkoderConfig(
            appContext,
            appContext.getString(R.string.barkoderLicenseKey)
        ) {
            Log.i(BKDConfigUtil.TAG, it.message)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.activity_main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_settings -> {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java).apply {
                    putExtra(SettingsFragment.ARGS_MODE_KEY, ScanMode.GLOBAL.ordinal)
                    putExtra("opened_from_settings", false)

                })
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun getScannerIntent(scanMode: ScanMode): Intent {
        return Intent(this@MainActivity, ScannerActivity::class.java).apply {
            putExtra(ScannerActivity.ARGS_MODE_KEY, scanMode.ordinal)
        }
    }

    override fun onResume() {
        super.onResume()
        maybeShowFirstRunTutorial()
    }

    private fun maybeShowFirstRunTutorial() {
        val prefs = getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)

        // Show only once
        val alreadyShown = prefs.getBoolean("tutorial_shown_v1", false)
        if (alreadyShown) return

        // Mark as shown immediately so it won't trigger again
        prefs.edit().putBoolean("tutorial_shown_v1", true).apply()

        binding.root.doOnPreDraw {
            if (isFinishing || isDestroyed) return@doOnPreDraw
            if (supportFragmentManager.isStateSaved) return@doOnPreDraw

            ensureOverlayAttached()
            hideSpotlight()
            showTutorialStep(0)
        }
    }

    private fun showTutorialEveryTime() {
        binding.root.doOnPreDraw {
            if (isFinishing || isDestroyed) return@doOnPreDraw
            if (supportFragmentManager.isStateSaved) return@doOnPreDraw

            ensureOverlayAttached()
            hideSpotlight()
            showTutorialStep(0)
        }
    }

    private fun ensureOverlayAttached() {
        if (tutorialOverlay != null) return

        val root = window.decorView as ViewGroup
        tutorialOverlay = SpotlightOverlayView(this).apply {
            isVisible = false
            // block clicks so user focuses on tutorial; set to false if you want clicks to pass through
            blockTouches = true
        }
        root.addView(
            tutorialOverlay,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }


    private fun spotlightOnView(target: View) {
        val overlay = tutorialOverlay ?: return
        overlay.isVisible = true

        // compute target rect in overlay coordinates
        val loc = IntArray(2)
        overlay.getLocationOnScreen(loc)
        val overlayX = loc[0]
        val overlayY = loc[1]

        target.getLocationOnScreen(loc)
        val left = (loc[0] - overlayX).toFloat()
        val top = (loc[1] - overlayY).toFloat()
        val right = left + target.width
        val bottom = top + target.height

        // add padding + rounded corner spotlight
        val pad = 18f
        overlay.setSpotlight(
            RectF(left - pad, top - pad, right + pad, bottom + pad),
            cornerRadius = 28f
        )
    }

    private fun hideSpotlight() {
        tutorialOverlay?.isVisible = false
        tutorialOverlay?.clearSpotlight()
    }



    override fun onPrev(step: Int) {
        val from = step
        val to = (step - 1).coerceAtLeast(0)
        showTutorialStep(to, fromStep = from)
    }

    override fun onNext(step: Int) {
        val from = step
        val to = step + 1
        showTutorialStep(to, fromStep = from)
        if (to > 7) hideSpotlight()
    }

    override fun onSkip(step: Int) {
        hideSpotlight()
    }

    private data class Quad(
        val title: String,
        val message: String,
        val hasPrev: Boolean,
        val hasNext: Boolean
    )

    private data class StepCfg(
        val title: String,
        val message: String,
        val hasPrev: Boolean,
        val hasNext: Boolean,
        val spotlight: View?,
        val anchor: View?,
        val marginDp: Int = 10,
        val anchorMode: Int = TutorialDialogFragment.MODE_BELOW
    )


    private fun showTutorialStep(step: Int, fromStep: Int? = null) {
        if(step == 1 || (step == 0 && fromStep == 1) || step == 4 || step == 3) {
            hideSpotlight()
        }

        Log.d("fromstep", fromStep.toString())
        val cfg = when (step) {
            0 -> StepCfg(
                title = "Scans all barcode types",
                message = "The big red Anyscan button is all you need for scanning barcodes on-the-go. On older or slower devices, performance may be reduced.\n" +
                        "For best speed, choose a specific barcode mode.",
                hasPrev = false,
                hasNext = true,
                spotlight = binding.layoutScan,
                anchor = binding.layoutScan,
                marginDp = 0,
                anchorMode = TutorialDialogFragment.MODE_ABOVE
            )
            1 -> StepCfg(
                title = "All 1D",
                message = "Scans classic 1D retail barcodes like EAN, UPC, Code 128. Does not scan QR codes.",
                hasPrev = true,
                hasNext = true,
                spotlight = binding.cardBarcodes1DALL,
                anchor = binding.cardBarcodes1DALL,
                marginDp = 0,
                anchorMode = TutorialDialogFragment.MODE_BELOW
            )
            2 -> StepCfg(
                title = "All 2D",
                message = "Scans QR, Data Matrix, Aztec and other 2D codes.",
                hasPrev = true,
                hasNext = true,
                spotlight = binding.cardBarcodes2DAll,
                anchor = binding.cardBarcodes2DAll,
                marginDp = 0,
                anchorMode = TutorialDialogFragment.MODE_BELOW
            )
            3 -> StepCfg(
                title = "PDF417",
                message = "Used on IDs, boarding passes and driver licenses.",
                hasPrev = true,
                hasNext = true,
                spotlight = binding.cardBarcodesPDF,
                anchor = binding.cardBarcodesPDF,
                marginDp = 0,
                anchorMode = TutorialDialogFragment.MODE_BELOW
            )
            4 -> StepCfg(
                title = "Scan from gallery",
                message = "Scan barcodes from existing images on your device.",
                hasPrev = true,
                hasNext = true,
                spotlight = binding.cardScanFromGallery,
                anchor = binding.cardScanFromGallery,
                marginDp = 0,
                anchorMode = TutorialDialogFragment.MODE_ABOVE
            )
            5 -> StepCfg(
                title = "VIN Scan",
                message = "Scans Vehicle Identification Numbers (VINs).",
                hasPrev = true,
                hasNext = true,
                spotlight = binding.cardVinMode,
                anchor = binding.cardVinMode,
                marginDp = 0,
                anchorMode = TutorialDialogFragment.MODE_ABOVE
            )
            6 -> StepCfg(
                title = "MRZ Scan",
                message = "Scans passports, ID cards, visas, residence permits, and other travel documents.",
                hasPrev = true,
                hasNext = true,
                spotlight = binding.cardScanIdDocument,
                anchor = binding.cardScanIdDocument,
                marginDp = 0,
                anchorMode = TutorialDialogFragment.MODE_ABOVE
            )
            7 -> StepCfg(
                title = "Augmented Reality Overlay",
                message = "AR overlay system tracks barcode positions in real time and anchors relevant information over each detected code in the camera preview.",
                hasPrev = true,
                hasNext = true,
                spotlight = binding.cardArMode,
                anchor = binding.cardArMode,
                marginDp = 0,
                anchorMode = TutorialDialogFragment.MODE_ABOVE
            )
            else -> return
        }

        val apply = {
            applySpotlightWithOptionalDelay(step, cfg.spotlight)
            showTutorialDialog(step, cfg)
        }

        // Auto-scroll to top when going back from step 4 to step 3
        if (step == 3 || step == 1) {
            val sv: ScrollView = binding.mainScrollView
            sv.doOnPreDraw {
                sv.scrollTo(0, 0)
                cfg.anchor?.doOnPreDraw { apply() } ?: apply()
            }
            return
        }

        // Steps that must NOT scroll at all
        if (step == 2 || step == 3) {
            if (cfg.anchor != null) {
                cfg.anchor.doOnPreDraw { apply() }
            } else {
                apply()
            }
            return
        }

        // Keep your existing scroll behavior for the other steps
        val anchor = cfg.anchor
        val shouldAutoScroll = step == 0 || step == 4

        if (anchor != null && shouldAutoScroll) {
            // ✅ Special-case: first time step 0 -> scroll to bottom, then wait for settle, then apply
            if (step == 0 && fromStep == null) {
                val sv: ScrollView = binding.mainScrollView
                sv.doOnPreDraw {
                    val contentView = sv.getChildAt(0) ?: run {
                        anchor.doOnPreDraw { apply() }
                        return@doOnPreDraw
                    }

                    val maxScrollY = max(0, contentView.height - (sv.height - 500))
                    sv.smoothScrollTo(0, maxScrollY)
                    waitForScrollSettle(sv) {
                        anchor.doOnPreDraw { apply() }
                    }
                }
            } else {
                scrollMainScrollViewThenRun(anchor) { apply() }
            }
            return
        }

        anchor?.doOnPreDraw { apply() } ?: apply()
    }

    private fun applySpotlightWithOptionalDelay(step: Int, target: View?) {
        if (target == null) {
            hideSpotlight()
            return
        }

        binding.root.postDelayed({
            if (isFinishing || isDestroyed) return@postDelayed
            spotlightOnView(target)
        }, 0L)
    }

    private fun scrollMainScrollViewThenRun(target: View, after: () -> Unit) {
        val sv: ScrollView = binding.mainScrollView

        sv.doOnPreDraw {
            val contentView = sv.getChildAt(0) ?: run {
                target.doOnPreDraw { after() }
                return@doOnPreDraw
            }

            val contentH = contentView.height
            val maxScroll = max(0, contentH )

            // Force the same result as "scrolling fully down with fingers"
            val desiredY = maxScroll

            if (abs(sv.scrollY - desiredY) <= 1) {
                target.doOnPreDraw { after() }
                return@doOnPreDraw
            }

            sv.scrollTo(0, desiredY)
            target.doOnPreDraw { after() }
        }
    }


    private fun waitForScrollSettle(sv: ScrollView, after: () -> Unit) {
        val checkIntervalMs = 16L
        val stableFramesNeeded = 3
        val timeoutMs = 900L

        var lastY = sv.scrollY
        var stableFrames = 0
        val start = android.os.SystemClock.uptimeMillis()

        val r = object : Runnable {
            override fun run() {
                val y = sv.scrollY
                if (abs(y - lastY) <= 1) {
                    stableFrames++
                } else {
                    stableFrames = 0
                    lastY = y
                }

                val timedOut = (android.os.SystemClock.uptimeMillis() - start) >= timeoutMs
                if (stableFrames >= stableFramesNeeded || timedOut) {
                    after()
                } else {
                    sv.postDelayed(this, checkIntervalMs)
                }
            }
        }

        sv.post(r)
    }

    private fun showTutorialDialog(step: Int, cfg: StepCfg) {
        // Avoid stacking dialogs while switching steps quickly
        (supportFragmentManager.findFragmentByTag("tutorial_dialog") as? TutorialDialogFragment)
            ?.dismissAllowingStateLoss()

        TutorialDialogFragment
            .newInstance(
                title = cfg.title,
                message = cfg.message,
                step = step,
                hasPrev = cfg.hasPrev,
                hasNext = cfg.hasNext,
                anchorViewId = cfg.anchor?.id ?: 0,
                marginDp = cfg.marginDp,
                anchorMode = cfg.anchorMode
            )
            .show(supportFragmentManager, "tutorial_dialog")
    }

    private fun scrollIntoViewIfNeeded(target: View) {
        var p: ViewParent? = target.getParent()

        while (p is View) {
            when (p) {
                is NestedScrollView -> {
                    p.scrollTo(0, target.top)
                    return
                }
                is ScrollView -> {
                    p.scrollTo(0, target.top)
                    return
                }
            }
            p = p.getParent()
        }
    }

    //region Scan from gallery

    private fun scanFromGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // ✅ Compliant: Uses the modern Photo Picker (API 33+)
            pickImageResult.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } else {
            // ✅ Compliant: Uses ACTION_GET_CONTENT for older devices (Pre-API 33).
            // This is the correct, permission-less method to pick media on older Android.
            val pickIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE) // Ensures the URI can be opened
                type = "image/*"
            }
            pickImageResult.launch(pickIntent)
        }
    }


    private fun scanImageFromUri(uri: Uri?) {
        binding.progressIndicator.isVisible = true

        // Create a single instance of the configuration
        val config = BKDConfigUtil.configureBKD(
            this,
            ScanMode.GALLERY_SCAN,
            true
        )

        // Apply custom options to the decoder config

        // Use the same config when scanning the image
        ImageUtil.bitmapFromUri(contentResolver, uri, 3000, 3000)?.let { bitmap ->
            BarkoderHelper.scanImage(
                bitmap,
                config,
                this,
                this
            )
        }
    }


    override fun scanningFinished(
        results: Array<out Barkoder.Result>?,
        thumbnail: Array<out Bitmap>?,
        resultImage: Bitmap?
    ) {

        mainBitmap = null
        pictureBitmap = null
        signatureBitmap = null
        documentBitmap = null
        croppedBarcodeImage = null

        mainPath = null
        picturePath = null
        signaturePath = null
        documentPath = null
        croppedBarcodePath = null




        sessionScansAdapterData.clear()

        val sharedPreferences = this.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putInt("lastResultsOnFrame", results!!.size).apply()
        sharedPreferences.edit().putBoolean("galleryScan", true).apply()
        resultsTemp = results
        binding.progressIndicator.isVisible = false

        if(results.size < 2) {
            binding.imageBarcodesSingleLocations.visibility = View.VISIBLE
        } else {
            binding.imageBarcodesLocations.visibility = View.VISIBLE
        }
        binding.imageBackgroundLayout.visibility = View.VISIBLE
        binding.imageBarcodesLocations.setImageBitmap(resultImage)
        binding.imageBarcodesSingleLocations.setImageBitmap(resultImage)
        val barcodeResults = mutableListOf<String>()
        val barcodeTypes = mutableListOf<String>()
        val barcodesDates = mutableListOf<String>()

        // Gather barcode results
        results?.forEach { result ->
            barcodeResults.add(result.textualData)
            barcodeTypes.add(result.barcodeTypeName)
            barcodesDates.add(getFormattedTimestamp())
        }
        // Show results in bottom sheet


        // Process and save results
        if (results != null) {
            croppedBarcodeImage =
                thumbnail?.last()  // Save the last thumbnail as the cropped barcode image


            results.forEachIndexed { index, result ->

                croppedBarcodeImageOnScannedBarcode = thumbnail!!.get(index)

                val scannedDate =
                    SimpleDateFormat("yyyy/MM/dd/HH:mm:ss.SSS", Locale.getDefault()).format(Date())

                if(result.barcodeTypeName == "MRZ") {
                    if (result.images != null) {
                        for (j in result.images) {
                            when (j.name) {
                                "main" -> mainBitmap = j.image
                                "document" -> documentBitmap = j.image
                                "signature" -> signatureBitmap = j.image
                                "picture" -> pictureBitmap = j.image
                            }
                        }
                }
                    picturePath =
                        pictureBitmap?.let { saveBitmapToInternalStorage(context!!, it, "Picture") }
                    documentPath = documentBitmap?.let {
                        saveBitmapToInternalStorage(
                            context!!,
                            it,
                            "Document"
                        )
                    }
                    signaturePath = signatureBitmap?.let {
                        saveBitmapToInternalStorage(
                            context!!,
                            it,
                            "Signature"
                        )
                    }
                    mainPath =
                        mainBitmap?.let { saveBitmapToInternalStorage(context!!, it, "Main") }
                    if(croppedBarcodePath != null) {
                        croppedBarcodePath = croppedBarcodeImageOnScannedBarcode?.let {
                            saveBitmapToInternalStorage(
                                context!!,
                                it,
                                "croppedBarcodeImage"
                            )
                        }
                    }

                    sessionScansAdapterData.add(SessionScan(scannedDate,result.textualData, if(result.extra != null) formatBarcodeName(result.barcodeTypeName, result.extra.toList()) else result.barcodeTypeName,picturePath, documentPath, signaturePath,mainPath,croppedBarcodePath,if(result.extra != null) formattedText(result.extra.toList()) else "",if(result.extra != null) formattedTextJson(result.extra.toList()) else "",if(result.extra != null) extractImageRawBase64(result.extra.toList()) else ""))
                    recentViewModel.addRecentScan(
                        RecentScan2(
                            scannedDate,
                            result.textualData,
                            if(result.extra != null) formatBarcodeName(result.barcodeTypeName, result.extra.toList()) else result.barcodeTypeName,
                            picturePath,
                            documentPath,
                            signaturePath,
                            mainPath,
                            croppedBarcodePath,
                            if(result.extra != null) formattedText(result.extra.toList()) else "",
                            if(result.extra != null) formattedTextJson(result.extra.toList()) else "",
                            if(result.extra != null) extractImageRawBase64(result.extra.toList()) else ""
                        )
                    )
                } else {
                    croppedBarcodeImageOnScannedBarcode = thumbnail!!.get(index)
                    croppedBarcodePath = croppedBarcodeImageOnScannedBarcode?.let {
                        saveBitmapToInternalStorage(
                            context!!,
                            it,
                            "croppedBarcodeImage"
                        )
                    }

                    sessionScansAdapterData.add(SessionScan(scannedDate,result.textualData, if(result.extra != null) formatBarcodeName(result.barcodeTypeName, result.extra.toList()) else result.barcodeTypeName,picturePath, documentPath, signaturePath,mainPath,croppedBarcodePath,if(result.extra != null) formattedText(result.extra.toList()) else "",if(result.extra != null) formattedTextJson(result.extra.toList()) else "",if(result.extra != null) extractImageRawBase64(result.extra.toList()) else ""))
                    // Handle barcodes without images (most barcodes will likely not have images)
                    for(i in sessionScansAdapterData) {
                        i.highLight = true
                    }
                    recentViewModel.addRecentScan(
                        RecentScan2(
                            scannedDate,
                            result.textualData,
                            if(result.extra != null) formatBarcodeName(result.barcodeTypeName, result.extra.toList()) else result.barcodeTypeName,
                            null,  // No picture
                            null,  // No document
                            null,  // No signature
                            null,  // No main
                            croppedBarcodePath,  // May still have cropped barcode image
                            if(result.extra != null) formattedText(result.extra.toList()) else "",
                            if(result.extra != null) formattedTextJson(result.extra.toList()) else "",
                            if(result.extra != null) extractImageRawBase64(result.extra.toList()) else ""
                        )
                    )
                }
            }
          }

        uiScope.launch {
            if (results.isNullOrEmpty()) {
                binding.imageBackgroundLayout.visibility = View.GONE
               var dialog =  MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle("No barcodes or MRZ detected :(")
                    .setMessage("Please ensure the image you've selected contains at least one barcode, or choose a different image. \nAlso verify that the barcode type you're trying to scan is enabled in the settings.")
                    .setNegativeButton("Dismiss") { dialog, _ ->
                        dialog.dismiss()
                    }
                   .setPositiveButton("Settings") { dialog, _ ->
                       val settingsIntent = Intent(this@MainActivity, SettingsActivity::class.java)
                       settingsIntent.putExtra(SettingsFragment.ARGS_MODE_KEY, 16)
                       startActivity(settingsIntent)
                   }
                    .show()

                dialog.window?.setBackgroundDrawableResource(R.drawable.background_dialog_gallery_scan)

            } else {
                existingFragment =
                    supportFragmentManager.findFragmentByTag("ResultBottomDialogFragment") as ResultBottomDialogFragment?

                if (existingFragment != null && existingFragment!!.isVisible) {
                    existingFragment!!.updateBarcodeInfo(
                        barcodeResults,
                        barcodeTypes,
                        barcodesDates,
                        results.size.toString(),
                        thumbnail!!.last(),
                        sessionScansAdapterData
                    )

                } else {
                    // Set large data in SharedViewModel to prevent TransactionTooLargeException
                    sharedViewModel.setData(
                        image = thumbnail?.lastOrNull(),
                        sessions = ArrayList(sessionScansAdapterData),
                        results = ArrayList(barcodeResults),
                        types = ArrayList(barcodeTypes),
                        dates = ArrayList(barcodesDates),
                        size = results.size.toString()
                    )
                    
                    val bottomSheetFragment = ResultBottomDialogFragment.newInstance(
                        barcodeResults,
                        barcodeTypes, barcodesDates, null, results.size.toString(),
                        sessionScansAdapterData
                    )
                    bottomSheetFragment.show(supportFragmentManager, "ResultBottomDialogFragment")


                }
            }
        }

    }

    fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap?, filePrefix: String): String {
        if (bitmap == null) return ""

        // Get the directory for the app's internal storage
        val directory = File(context.filesDir, "images_jpg")
        if (!directory.exists()) {
            directory.mkdirs()  // Create the directory if it doesn't exist
        }

        // Generate a unique file name using timestamp or UUID
        val fileName = "${filePrefix}_${System.currentTimeMillis()}.jpg"
        val file = File(directory, fileName)

        try {
            // Write the bitmap to the file
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
            outputStream.flush()
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        // Return the absolute file path
        return file.absolutePath
    }

    private fun getFormattedTimestamp(): String {
        val dateFormat = SimpleDateFormat("MMM dd yyyy", Locale.getDefault())
        return dateFormat.format(Date())
    }

    fun hideImageView() {
        binding.imageBarcodesLocations.visibility = View.GONE
        binding.imageBarcodesSingleLocations.visibility = View.GONE
        binding.imageBackgroundLayout.visibility = View.GONE
    }

    fun formattedText(extra: List<Barkoder.BKKeyValue>?): String {
        // Check if the extra list is null or empty
        if (extra.isNullOrEmpty()) {
            return ""
        }

        // Find the "formattedText" value
        val originalText = extra.firstOrNull { it.key == "formattedText" }?.value ?: return ""

        // Remove any line that starts with "ImageRawBase64:"
        val filteredText = originalText
            .lineSequence()
            .filterNot { line ->
                val trimmed = line.trim()
                trimmed.startsWith("ImageRawBase64:") ||
                        trimmed.startsWith("Image width:") ||
                        trimmed.startsWith("Image height:")
            }

            .joinToString("\n") // Join lines back together

        Log.d("pwqe" , filteredText)
        return filteredText
    }


    fun formattedTextJson(extra: List<Barkoder.BKKeyValue>?): String {
        // Check if the extra list is null or empty
        if (extra.isNullOrEmpty()) {
            return ""
        }

        // Find the "formattedJSON" value
        val jsonText = extra.firstOrNull { it.key == "formattedJSON" }?.value ?: return ""

        // Optionally, remove unnecessary lines (similar to what you did for formattedText)
        val filteredJson = jsonText
            .lineSequence()
            .filterNot { line ->
                val trimmed = line.trim()
                trimmed.startsWith("ImageRawBase64:") ||
                        trimmed.startsWith("Image width:") ||
                        trimmed.startsWith("Image height:")
            }
            .joinToString("\n") // Join lines back together

        Log.d("BarkoderJSON", filteredJson)
        return filteredJson
    }


    fun extractImageRawBase64(extra: List<Barkoder.BKKeyValue>?): String {
        if (extra.isNullOrEmpty()) return ""

        val formattedText = extra.firstOrNull { it.key == "formattedText" }?.value ?: return ""

        return formattedText
            .lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("ImageRawBase64:") }
            ?.substringAfter("ImageRawBase64:")
            ?.trim()
            ?: ""
    }

    fun formatBarcodeName(barcodeTypeName: String, extra: List<Barkoder.BKKeyValue>?): String {
        if (extra.isNullOrEmpty()) {
            // If extra is null or empty, return the original barcodeTypeName
            return barcodeTypeName
        }

        // Flags to track conditions
        var isGS1 = false
        var isComposite = false

        // Iterate over the list and check for keys and values
        for (item in extra) {
            when (item.key) {
                "gs1" -> isGS1 = item.value?.toIntOrNull() == 1
                "composite" -> isComposite = item.value?.toIntOrNull() == 1
            }
        }

        // Return the formatted name based on conditions
        return when {
            isGS1 && isComposite -> "GS1 $barcodeTypeName Composite"
            isGS1 -> "GS1 $barcodeTypeName"
            isComposite -> "$barcodeTypeName Composite"
            else -> barcodeTypeName
        }
    }

    fun convertPngToJpg(context: Context) {
        val directory = File(context.filesDir, "images")
        if (!directory.exists() || !directory.isDirectory) return

        // List all PNG files in the directory
        val pngFiles = directory.listFiles { file -> file.name.endsWith(".png") }

        pngFiles?.forEach { pngFile ->
            try {
                // Load the PNG file as a Bitmap
                val bitmap = BitmapFactory.decodeFile(pngFile.absolutePath)

                // Create a new JPG file with the same name (but .jpg extension)
                val jpgFileName = pngFile.nameWithoutExtension + ".jpg"
                val jpgFile = File(directory, jpgFileName)

                // Save the Bitmap as a JPG with 50% quality
                val outputStream = FileOutputStream(jpgFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 30, outputStream)
                outputStream.flush()
                outputStream.close()

                // Delete the old PNG file
                pngFile.delete()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }


    }


    fun onAppUpdate(context: Context) {
        val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isConverted = sharedPreferences.getBoolean("png_to_jpg_converted", false)

        if (!isConverted) {
            convertPngToJpg(context)
            sharedPreferences.edit().putBoolean("png_to_jpg_converted", true).apply()
        }
    }

}


