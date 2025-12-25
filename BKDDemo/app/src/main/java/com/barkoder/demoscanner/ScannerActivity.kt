package com.barkoder.demoscanner

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.barkoder.Barkoder
import com.barkoder.BarkoderConfig
import com.barkoder.demoscanner.adapters.BarcodePrintAdapter
import com.barkoder.demoscanner.api.RetrofitIInstance
import com.barkoder.demoscanner.databinding.ActivityScannerBinding
import com.barkoder.demoscanner.enums.ScanMode
import com.barkoder.demoscanner.fragments.ResultBottomDialogFragment
import com.barkoder.demoscanner.fragments.SettingsFragment
import com.barkoder.demoscanner.models.BarcodeDataPrint
import com.barkoder.demoscanner.models.BarcodeScanedData
import com.barkoder.demoscanner.models.RecentScan2
import com.barkoder.demoscanner.models.SessionScan
import com.barkoder.demoscanner.repositories.BarcodeDataRepository
import com.barkoder.demoscanner.utils.BKDConfigUtil
import com.barkoder.demoscanner.utils.CommonUtil
import com.barkoder.demoscanner.utils.NetworkUtils
import com.barkoder.demoscanner.utils.ScannedResultsUtil
import com.barkoder.demoscanner.utils.getBoolean
import com.barkoder.demoscanner.utils.getString
import com.barkoder.demoscanner.viewmodels.BarcodeDataViewModel
import com.barkoder.demoscanner.viewmodels.BarcodeDataViewModelFactory
import com.barkoder.demoscanner.viewmodels.RecentScanViewModel
import com.barkoder.enums.BarkoderARMode
import com.barkoder.enums.BarkoderCameraPosition
import com.barkoder.interfaces.BarkoderResultCallback
import com.barkoder.interfaces.CameraCallback
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URLEncoder
import java.security.MessageDigest
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase


//TODO zoom from pinched can't be reset on resume
class ScannerActivity : AppCompatActivity(), BarkoderResultCallback,
    CameraCallback, ResultBottomDialogFragment.BottomSheetStateListener {

    private lateinit var binding: ActivityScannerBinding
    private lateinit var recentViewModel: RecentScanViewModel

    private val latestResults = mutableListOf<Barkoder.Result>()
    private val resultsLock = Any()
    private lateinit var viewModel : BarcodeDataViewModel

    private var pictureBitmap: Bitmap? = null
    private var documentBitmap: Bitmap? = null
    private var signatureBitmap: Bitmap? = null

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private var pendingBottomSheetArgs: (() -> Unit)? = null
    private var mainBitmap: Bitmap? = null
    private var croppedBarcodePath: String? = null
    private var croppedBarcodeImageOnScannedBarcode: Bitmap? = null
    private var showedPermissionDialog = false
    private val printedBarcodes = mutableSetOf<String>()
    private val scannedResults = mutableListOf<Barkoder.Result>()
    val itemsToMove = mutableListOf<SessionScan>()
    val scanCounters = mutableMapOf<String, Int>()
    private lateinit var bottomSheetFragment : ResultBottomDialogFragment;
    private var onPauseBool: Boolean = false;
    private var factor: Float = 1f;
    private var zoomClickedOnce = false;
    private var picturePath: String? = null
    private var documentPath: String? = null
    private var signaturePath: String? = null
    private var mainPath: String? = null
    private var resultsTemp: Array<out Barkoder.Result>? = null
    var sessionScansAdapterData: ArrayList<SessionScan> = arrayListOf()

    private lateinit var recyclerViewBarcodePrint: RecyclerView
    private lateinit var adapterBarcodePrint: BarcodePrintAdapter

    private val LAST_PAUSED_TIME_KEY = "lastPausedTime"
    private val TIME_THRESHOLD_MS = 60_000L // 60 seconds

    var isBottomSheetDialogShown = false
    private lateinit var scanMode: ScanMode
    var scannedBarcodes = 0
    private var isZoomed = false
    private var maxZoomFactor: Float = -1f
    private var isFlashOn = false
    private var isScanning = true
    private var automaticShowBottomSheet: Boolean = false
    private var continiousModeOn = false
    private var context: Context? = null
    private val CAMERA_PERMISSION_REQUEST_CODE = 100
    private var existingFragment: ResultBottomDialogFragment? = null
    private var croppedBarcodeImage: Bitmap? = null
    var receivedBooleanValue: Boolean = false
    var settingsChangedBoolean: Boolean = false
    var checkSumMrz : String = ""
    val barcodeListResult = mutableListOf<String>()
    val barcodeListType = mutableListOf<String>()
    val barcodeListDate = mutableListOf<String>()
    private val PREFS_NAME = "MyPrefsFile"
    private val activityJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + activityJob)
    private var autoFocusBoolean : Boolean? = null
    private var videoStabilization : Boolean? = null
    private var frontCamera : Boolean? = null
    private var dynamicExposureIntesity : String = "Disabled"
    private var frontCameraEnabled = false
    private val barcodeList = mutableListOf<BarcodeDataPrint>()
    private val recentScansToAdd = mutableListOf<RecentScan2>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        scanMode = ScanMode.values()[intent.extras!!.getInt(ARGS_MODE_KEY)]
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        recentViewModel = ViewModelProvider(this).get(RecentScanViewModel::class.java)
        context = this

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                recentScansToAdd.forEach { scan ->
                    recentViewModel.addRecentScan(scan)
                }
                finish()
            }
        })

        binding.btnClose.setOnClickListener {
            recentScansToAdd.forEach { scan ->
                recentViewModel.addRecentScan(scan)
            }
            finish()

        }

        firebaseAnalytics = Firebase.analytics

        recyclerViewBarcodePrint = findViewById(R.id.txtScannedResult)

        // Initialize the Adapter with the barcode list
        adapterBarcodePrint = BarcodePrintAdapter(barcodeList)
        recyclerViewBarcodePrint.adapter = adapterBarcodePrint
        recyclerViewBarcodePrint.layoutManager = LinearLayoutManager(this)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        autoFocusBoolean = prefs.getBoolean("pref_key_autofocus", false)
        videoStabilization = prefs.getBoolean("pref_key_videostabilization", false)
        frontCamera = prefs.getBoolean("pref_key_frontCamera", false)



        binding.btnSettings.setOnClickListener {
            binding.bkdView.stopScanning()
            val settingsIntent = Intent(this@ScannerActivity, SettingsActivity::class.java)
            settingsIntent.putExtra(SettingsFragment.ARGS_MODE_KEY, scanMode.ordinal)
            startActivity(settingsIntent)
            finish()
        }
        val sharedPreferences: SharedPreferences =
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        receivedBooleanValue = sharedPreferences.getBoolean("BOOLEAN_KEY", false)

        binding.btnZoom.setOnClickListener {
            it.setBackgroundResource(if (isZoomed) R.drawable.ic_zoom_on else R.drawable.ic_zoom_off)
            isZoomed = !isZoomed
            setZoom()
            zoomClickedOnce = true;
        }

        binding.btnFlash.setOnClickListener {
            it.setBackgroundResource(if (isFlashOn) R.drawable.ic_flash_off else R.drawable.ic_flash_on)
            isFlashOn = !isFlashOn
            binding.bkdView.setFlashEnabled(isFlashOn)
            binding.bkdView.setFlashInitial(isFlashOn)

        }
        binding.btnCameraSwitch.setOnClickListener {
            frontCameraEnabled = !frontCameraEnabled
            it.setBackgroundResource(if (frontCameraEnabled) R.drawable.ic_camera_switch_on else R.drawable.ic_camera_switch_off)
            if(frontCameraEnabled) {
                binding.btnFlash.setBackgroundResource(R.drawable.ic_flash_disabled)
                binding.btnFlash.isEnabled = false
                binding.bkdView.setCamera(BarkoderCameraPosition.FRONT)
            } else {
                binding.bkdView.setCamera(BarkoderCameraPosition.BACK)
                binding.btnFlash.isEnabled = true
                if(isFlashOn){
                    binding.btnFlash.setBackgroundResource(R.drawable.ic_flash_on)
                } else {
                    binding.btnFlash.setBackgroundResource(R.drawable.ic_flash_off)
                }
            }
        }

        binding.btnShowDialog.setOnClickListener {
            if(barcodeListResult.size > 0) {
                showDialogBtn()
            }

        }

        if (receivedBooleanValue) {
            showSecretTextView()
        } else {
            hideSecretTextView()
        }

        binding.imageScanned.setOnClickListener {
            binding.imageScanned.visibility = View.GONE
            isScanning = true
            isBottomSheetDialogShown = false
            binding.bkdView.startScanning(this)

        }

        binding.bkdView.config = BKDConfigUtil.configureBKD(this, scanMode)
        binding.textFps.text = "fps"
        binding.textDps.text = "dps"
        binding.txtEnabledTypes.text =
            BKDConfigUtil.getEnabledTypesAsStringArray(
                binding.bkdView.config.decoderConfig,
                resources
            )

        if (binding.bkdView.config.thresholdBetweenDuplicatesScans > -1) {
            binding.continiousModeOn.visibility = View.VISIBLE
            if(binding.bkdView.config.arConfig.arMode == BarkoderARMode.OFF){
                binding.continiousModeOn.text =
                    "Continuous / ${binding.bkdView.config.thresholdBetweenDuplicatesScans.toString()}s delay"
            } else {
                binding.continiousModeOn.text =
                    "AR on"
            }

        } else {
            binding.continiousModeOn.visibility = View.GONE
        }

        val prefs2 = PreferenceManager.getDefaultSharedPreferences(this)
        dynamicExposureIntesity = prefs2.getString("pref_key_dynamic_exposureee", "0").toString()
        val sharedPreferences2: SharedPreferences =
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        settingsChangedBoolean = sharedPreferences2.getBoolean("settingsChangedBoolean", false)
        if (autoFocusBoolean!!) {
            binding.bkdView.setCentricFocusAndExposure(true)
        } else {
            binding.bkdView.setCentricFocusAndExposure(false)
        }


        if(dynamicExposureIntesity == "Disabled") {
            binding.bkdView.setDynamicExposure(0)
        } else {
            binding.bkdView.setDynamicExposure(dynamicExposureIntesity!!.toInt())
        }
        if (videoStabilization!!) {
            binding.bkdView.setVideoStabilization(true)
        } else {
            binding.bkdView.setVideoStabilization(false)
        }

        if (settingsChangedBoolean) {
            Toast.makeText(
                this,
                "Settings are changed for ${scanMode.title} template!",
                Toast.LENGTH_SHORT
            ).show()
        }
        if (binding.bkdView.config.thresholdBetweenDuplicatesScans < 0) {
            binding.bkdView.config.isCloseSessionOnResultEnabled = true
            binding.bkdView.config.decoderConfig.maximumResultsCount = 200
        } else {
            binding.bkdView.config.isCloseSessionOnResultEnabled = false
        }
        binding.bkdView.config.isLocationInImageResultEnabled = true
        if(binding.bkdView.config.thresholdBetweenDuplicatesScans == 0) {
            binding.bkdView.config.setThumbnailOnResultEnabled(false);
            binding.bkdView.config.isImageResultEnabled = false
        } else {
            binding.bkdView.config.isImageResultEnabled = true
        }

        binding.bkdView.setCameraCallback(this)

//        if (isScanning) {
//            binding.bkdView.startScanning(this)
//        } else
//            binding.bkdView.startCamera()


        binding.bkdView.config.roiLineColor = ContextCompat.getColor(this, R.color.brand_color)
        binding.bkdView.config.locationLineColor = ContextCompat.getColor(this, R.color.brand_color)

        if(binding.bkdView.config.isCloseSessionOnResultEnabled) {
            binding.bkdView.config.arConfig.arMode = BarkoderARMode.OFF
        } else {
            if(binding.bkdView.config.arConfig.arMode != BarkoderARMode.OFF) {
                binding.bkdView.config.decoderConfig.maximumResultsCount = 200
                binding.bkdView.config.thresholdBetweenDuplicatesScans = 0
                BarkoderConfig.SetMulticodeCachingEnabled(true);
            }
        }



        binding.bkdView.getMaxZoomFactor { maxZoom ->
            maxZoomFactor = maxZoom
            Log.d("CameraParams", "$maxZoomFactor")
        }
//        checkCameraPermission { granted ->
//            if (granted) {
//                try {
//
//
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                }
//            } else {
//                if(!showedPermissionDialog) {
//                    showPermissionAlert()
//                }
//            }
//        }
    }

    override fun onStart() {
        super.onStart()

    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onResume() {
        super.onResume()
        Log.d("requirededasd", binding.bkdView.config.decoderConfig.IDDocument.masterChecksumType.toString() )
        val sharedPreferences: SharedPreferences =
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        receivedBooleanValue = sharedPreferences.getBoolean("BOOLEAN_KEY", false)
        if (receivedBooleanValue) {
            binding.bkdView.setPerformanceCallback { fps, dps ->
                val fpsFloat = (fps * 10).toInt() / 10.0f
                val dpsFloat = (dps * 10).toInt() / 10.0f
                binding.textFps.text = "FPS: ${fpsFloat.toString()}"
                binding.textDps.text = "DPS: ${dpsFloat.toString()}"
                Log.d("fps: ", fps.toString())
                Log.d("dps: ", dps.toString())
            }
        }


        if(scanMode == ScanMode.VIN) {
            Barkoder.SetCustomOption(binding.bkdView.config.decoderConfig, "enable_ocr_functionality", 1)
        }

        val lastPausedTime = sharedPreferences.getLong(LAST_PAUSED_TIME_KEY, -1L)
        val currentTime = System.currentTimeMillis()

        if (lastPausedTime > 0 && currentTime - lastPausedTime > TIME_THRESHOLD_MS) {

            binding.btnFlash.setBackgroundResource(R.drawable.ic_flash_off)
            isFlashOn = false
            binding.bkdView.setFlashEnabled(isFlashOn)
            binding.bkdView.setFlashInitial(isFlashOn)
            // More than 60 seconds passed
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Only show app dialog if we cannot show the SDK dialog
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                // SDK dialog will be shown, so do nothing
            } else {
                // SDK dialog won't be shown, show app dialog
                showPermissionAlert()
            }
        } else {
            binding.bkdView.startScanning(this)
        }

    }

    override fun onPause() {
        onPauseBool = true
//        binding.bkdView.stopScanning()
        val sharedPreferences: SharedPreferences =
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putLong(LAST_PAUSED_TIME_KEY, System.currentTimeMillis())
        editor.putBoolean("settingsChangedBoolean", false)
        editor.apply()
        showedPermissionDialog = true






        super.onPause()
    }


    override fun onCameraReady() {

//        binding.bkdView.setZoomFactor(factor)
//            if(!isZoomed) {
//                binding.bkdView.setZoomFactorInitial(1.0f)
//            }

    }



    override fun onCameraFailedToOpen(errorMessage: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.warning_title)
            .setMessage(errorMessage)
            .setPositiveButton(R.string.close_button) { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setCancelable(false)
            .show()
    }

    override fun onCameraOpened() {
        TODO("Not yet implemented")
    }



    override fun scanningFinished(
        results: Array<out Barkoder.Result>?,
        thumbnails: Array<out Bitmap>?,
        imageResult: Bitmap?
    ) {

        val snapshot = results?.toList() ?: emptyList()

        synchronized(resultsLock) {
            latestResults.clear()
            latestResults.addAll(snapshot)
        }

        autoSendWebhook()


        val sharedPreferences = this.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        if(binding.bkdView.config.arConfig.arMode != BarkoderARMode.OFF){
            sessionScansAdapterData.clear()
            barcodeListResult.clear()
            barcodeListType.clear()
            barcodeListDate.clear()
            recentScansToAdd.clear()
            sharedPreferences.edit().putBoolean("arMode", true).apply()
        } else {
            sharedPreferences.edit().putBoolean("arMode", false).apply()
        }

        if (!results.isNullOrEmpty()) {
            // Add all results to the list
            scannedResults.addAll(results)

        }

        for (i in sessionScansAdapterData) {
            i.highLight = false
        }

        for (i in recentScansToAdd) {
            i.highlighted = false
        }


        sharedPreferences.edit().putInt("lastResultsOnFrame", results!!.size).apply()
        sharedPreferences.edit().putBoolean("galleryScan", false).apply()

                mainBitmap = null
                pictureBitmap = null
                signatureBitmap = null
                documentBitmap = null
                croppedBarcodeImage = null

                mainPath = null
                picturePath = null
                signaturePath = null
                documentPath = null
                croppedBarcodeImage = null

                if (results != null) {
                    if (binding.bkdView.config.decoderConfig.IDDocument.enabled) {
                        for (i in results) {
                            if (i.images != null) {
                                for (j in i.images) {
                                    when (j.name) {
                                        "main" -> mainBitmap = j.image
                                        "document" -> documentBitmap = j.image
                                        "signature" -> signatureBitmap = j.image
                                        "picture" -> pictureBitmap = j.image
                                    }
                                }
                            }
                        }
                    }

                }
                resultsTemp = results

        for (i in results!!) {

            barcodeListResult.add(i.textualData)
            barcodeListType.add(formatBarcodeName(i.barcodeTypeName, i.extra?.toList()))
            barcodeListDate.add(getCurrentTimeWithTimestamp())


        }

                binding.btnShowDialog.visibility = View.VISIBLE
                if(thumbnails != null) {
                    croppedBarcodeImage = thumbnails!!.last()
                }

              if(results.size > 0) {
                  bottomSheetFragment = ResultBottomDialogFragment.newInstance(
                      barcodeListResult,
                      barcodeListType, barcodeListDate, imageResult, results.size.toString(), sessionScansAdapterData
                  )
              }




//                setZoom()
//                setFlash()

                if (binding.bkdView.config.thresholdBetweenDuplicatesScans < 0) {


                    for(i in sessionScansAdapterData) {
                        i.highLight = false
                    }
                    binding.bkdView.pauseScanning()
                    binding.textScannedNumber.visibility = View.VISIBLE
                    isScanning = false
                    binding.imageScanned.visibility = View.VISIBLE
                    binding.imageScanned.setImageBitmap(imageResult!!)

                    scannedBarcodes = scannedBarcodes + results!!.size
                    bottomSheetFragment.isCancelable = false

                    binding.textScannedNumber.text = "(${scannedBarcodes.toString()})"


                    ScannedResultsUtil.storeResultsInPref(applicationContext, results)
                    for (i in results) {

                        val scannedDate =
                            SimpleDateFormat(
                                "yyyy/MM/dd/HH:mm:ss.SSS",
                                Locale.getDefault()
                            ).format(Date())
                        if (i.images != null && i.images.isNotEmpty()) { // Check if the array is not empty
                            if (i.images.size > 1 && i.images[1] != null) { // Check if the index 1 is within bounds and not null

                                if (i.images.isNotEmpty()) {
                                    val image1 =
                                        if (i.images.size > 0 && i.images[0] != null) i.images[0].image else null
                                    val image2 =
                                        if (i.images.size > 1 && i.images[1] != null) i.images[1].image else null
                                    val image3 =
                                        if (i.images.size > 2 && i.images[2] != null) i.images[2].image else null
                                    val image4 =
                                        if (i.images.size > 3 && i.images[3] != null) i.images[3].image else null


                                        if (pictureBitmap != null) {
                                            picturePath = saveBitmapToInternalStorage(
                                                context!!,
                                                pictureBitmap,
                                                "Picture"
                                            )
                                        } else {
                                            picturePath = null
                                        }
                                        if (documentBitmap != null) {
                                            documentPath = saveBitmapToInternalStorage(
                                                context!!,
                                                documentBitmap,
                                                "Document"
                                            )
                                        } else {
                                            documentPath = null
                                        }

                                        if (signatureBitmap != null) {
                                            signaturePath = saveBitmapToInternalStorage(
                                                context!!,
                                                signatureBitmap,
                                                "Signature"
                                            )
                                        } else {
                                            signaturePath = null
                                        }

                                        if (mainBitmap != null) {
                                            mainPath =
                                                saveBitmapToInternalStorage(
                                                    context!!,
                                                    mainBitmap,
                                                    "Main"
                                                )
                                        } else {
                                            mainPath = null
                                        }

                                        croppedBarcodePath = saveBitmapToInternalStorage(
                                            context!!,
                                            croppedBarcodeImage,
                                            "croppedBarcodeImage"
                                        )
                                        scannedResults.forEachIndexed { index, i ->
                                            val existingSessionScan = sessionScansAdapterData.find { it.scanText == i.textualData && it.scanTypeName == i.barcodeTypeName }

                                            if (!printedBarcodes.contains(i.textualData)) {
                                                onNewBarcodeScanned(i.barcodeTypeName, i.textualData)
                                                printedBarcodes.add(i.textualData)
                                            }

                                            val scannedDate = SimpleDateFormat(
                                                "yyyy/MM/dd/HH:mm:ss.SSS",
                                                Locale.getDefault()
                                            ).format(Date())

                                            if (existingSessionScan != null) {
                                                existingSessionScan.scannedTimesInARow += 1
                                                existingSessionScan.highLight = true

                                                recentScansToAdd.forEach { recentScan ->
                                                    if (recentScan.scanDate == existingSessionScan.scanDate && recentScan.scanText == existingSessionScan.scanText) {
                                                        recentScan.scannedTimesInARow = existingSessionScan.scannedTimesInARow
                                                        Log.d("Update", "Updated scannedTimesInARow to ${recentScan.scannedTimesInARow} for date: ${recentScan.scanDate}")
                                                    }
                                                }
                                            } else {
                                                if (thumbnails != null) {
                                                    if (index < thumbnails.size && croppedBarcodeImage != null) {
                                                        croppedBarcodeImage = thumbnails[index]
                                                        croppedBarcodePath = saveBitmapToInternalStorage(
                                                            context!!,
                                                            croppedBarcodeImage,
                                                            "croppedBarcodeImage"
                                                        )
                                                    } else {
                                                        croppedBarcodePath = null
                                                    }
                                                }

                                                val newSessionScan = SessionScan(
                                                    scannedDate,
                                                    i.textualData,
                                                    if (i.extra != null) formatBarcodeName(i.barcodeTypeName, i.extra.toList()) else i.barcodeTypeName,
                                                    picturePath,
                                                    documentPath,
                                                    signaturePath,
                                                    mainPath,
                                                    croppedBarcodePath,
                                                    if (i.extra != null) formattedText(i.extra.toList()) else "",
                                                    if (i.extra != null) formattedTextJson(i.extra.toList()) else "",
                                                    if (i.extra != null) extractImageRawBase64(i.extra.toList()) else "",
                                                    1, // Initialize scannedInARow to 1
                                                    highLight = true
                                                )

                                                sessionScansAdapterData.add(newSessionScan)

                                                recentScansToAdd.add(
                                                    RecentScan2(
                                                        scannedDate,
                                                        i.textualData,
                                                        if (i.extra != null) formatBarcodeName(i.barcodeTypeName, i.extra.toList()) else i.barcodeTypeName,
                                                        picturePath,
                                                        documentPath,
                                                        signaturePath,
                                                        mainPath,
                                                        croppedBarcodePath,
                                                        if (i.extra != null) formattedText(i.extra.toList()) else "",
                                                        if (i.extra != null) formattedTextJson(i.extra.toList()) else "",
                                                        if (i.extra != null) extractImageRawBase64(i.extra.toList()) else "",
                                                        scannedTimesInARow = 1
                                                    )
                                                )
                                                Log.d("Scanning", "New Result Added: ${i.textualData}")
                                            }
                                        }

                                        scannedResults.clear()

                                        uiScope.launch {
                                            onBarcodeScanned(
                                                barcodeListResult,
                                                barcodeListType,
                                                barcodeListDate,
                                                scannedBarcodes.toString(),
                                                croppedBarcodeImage
                                            )
                                        }

                                }
                            } else {
                                if (i.images != null && i.images.isNotEmpty()) {


                                    results.forEachIndexed { index, result ->


                                        croppedBarcodeImage = thumbnails!!.get(index)
                                        if (imageResult != null) {
                                            uiScope.launch(Dispatchers.Main.immediate) {
                                                uiScope.launch {
                                                    onBarcodeScanned(
                                                        barcodeListResult,
                                                        barcodeListType,
                                                        barcodeListDate,
                                                        scannedBarcodes.toString(),
                                                        croppedBarcodeImage
                                                    )
                                                }
                                            }
                                        }
                                        if (pictureBitmap != null) {
                                            picturePath = saveBitmapToInternalStorage(
                                                context!!,
                                                pictureBitmap,
                                                "Picture"
                                            )
                                        } else {
                                            picturePath = null
                                        }
                                        if (documentBitmap != null) {
                                            documentPath = saveBitmapToInternalStorage(
                                                context!!,
                                                documentBitmap,
                                                "Document"
                                            )
                                        } else {
                                            documentPath = null
                                        }

                                        if (signatureBitmap != null) {
                                            signaturePath = saveBitmapToInternalStorage(
                                                context!!,
                                                signatureBitmap,
                                                "Signature"
                                            )
                                        } else {
                                            signaturePath = null
                                        }

                                        if (mainBitmap != null) {
                                            mainPath =
                                                saveBitmapToInternalStorage(
                                                    context!!,
                                                    mainBitmap,
                                                    "Main"
                                                )
                                        } else {
                                            mainPath = null
                                        }

                                        if(croppedBarcodeImage != null) {
                                            croppedBarcodePath = saveBitmapToInternalStorage(
                                                context!!,
                                                croppedBarcodeImage,
                                                "croppedBarcodeImage"
                                            )
                                        } else {
                                            croppedBarcodePath = null
                                        }
                                        scannedResults.forEachIndexed { index, i ->
                                            val existingSessionScan = sessionScansAdapterData.find { it.scanText == i.textualData && it.scanTypeName == i.barcodeTypeName}

                                            if (!printedBarcodes.contains(i.textualData)) {
                                                onNewBarcodeScanned(i.barcodeTypeName, i.textualData)
                                                printedBarcodes.add(i.textualData)
                                            }

                                            val scannedDate = SimpleDateFormat(
                                                "yyyy/MM/dd/HH:mm:ss.SSS",
                                                Locale.getDefault()
                                            ).format(Date())

                                            if (existingSessionScan != null) {
                                                existingSessionScan.scannedTimesInARow += 1
                                                existingSessionScan.highLight = true

                                                recentScansToAdd.forEachIndexed { index, recentScan ->
                                                    if (recentScan.scanDate == existingSessionScan.scanDate && recentScan.scanText == existingSessionScan.scanText) {
                                                        recentScan.scannedTimesInARow = existingSessionScan.scannedTimesInARow
                                                        Log.d("Update", "Updated scannedTimesInARow to ${recentScan.scannedTimesInARow} for date: ${recentScan.scanDate}")
                                                    }
                                                }

                                            } else {
                                                if (index < thumbnails.size && croppedBarcodeImage != null) {
                                                    croppedBarcodeImage = thumbnails[index]
                                                    croppedBarcodePath = saveBitmapToInternalStorage(
                                                        context!!,
                                                        croppedBarcodeImage,
                                                        "croppedBarcodeImage"
                                                    )
                                                } else {
                                                    croppedBarcodePath = null
                                                }

                                                val newSessionScan = SessionScan(
                                                    scannedDate,
                                                    i.textualData,
                                                    if (i.extra != null) formatBarcodeName(i.barcodeTypeName, i.extra.toList()) else i.barcodeTypeName,
                                                    null,
                                                    null,
                                                    null,
                                                    mainPath,
                                                    croppedBarcodePath,
                                                    if (i.extra != null) formattedText(i.extra.toList()) else "",
                                                    if (i.extra != null) formattedTextJson(i.extra.toList()) else "",
                                                    if (i.extra != null) extractImageRawBase64(i.extra.toList()) else "",
                                                    1, // Initialize scannedInARow to 1
                                                    highLight = true
                                                )

                                                sessionScansAdapterData.add(newSessionScan)

                                                recentScansToAdd.add(
                                                    RecentScan2(
                                                        scannedDate,
                                                        i.textualData,
                                                        if (i.extra != null) formatBarcodeName(i.barcodeTypeName, i.extra.toList()) else i.barcodeTypeName,
                                                        null,
                                                        null,
                                                        null,
                                                        mainPath,
                                                        croppedBarcodePath,
                                                        if (i.extra != null) formattedText(i.extra.toList()) else "",
                                                        if (i.extra != null) formattedTextJson(i.extra.toList()) else "",
                                                        if (i.extra != null) extractImageRawBase64(i.extra.toList()) else "",
                                                        scannedTimesInARow = 1
                                                    )
                                                )
                                                Log.d("Scanning", "New Result Added: ${i.textualData}")
                                            }
                                        }

                                        scannedResults.clear()

                                    }



                                } else {
                                    // Handle the case where no valid image is available
                                    Log.e(
                                        "ScannerActivity",
                                        "No valid image found in images array."
                                    )
                                }
                            }
                        } else {
                            // Handle the case where the images array is empty
                            Log.e("ScannerActivity", "Images array is empty.")
                        }

                    }


            } else {

                if(binding.bkdView.config.thresholdBetweenDuplicatesScans == 0) {
                    croppedBarcodePath = ""
                }

                    binding.textScannedNumber.visibility = View.VISIBLE

                    bottomSheetFragment.isCancelable = false
                    scannedBarcodes = scannedBarcodes + results!!.size

                    continiousModeOn = true

                        if(binding.bkdView.config.arConfig.arMode == BarkoderARMode.OFF) {
                            uiScope.launch {
                                onBarcodeScanned(
                                    barcodeListResult,
                                    barcodeListType,
                                    barcodeListDate,
                                    scannedBarcodes.toString(),
                                    croppedBarcodeImage
                                )
                            }

                            binding.textScannedNumber.text = "(${scannedBarcodes.toString()})"
                        } else {
                            binding.textScannedNumber.text = "(${barcodeListResult.size.toString()})"
                        }




                    scannedResults.forEachIndexed { index, i ->
                        val existingSessionScan = sessionScansAdapterData.find { it.scanText == i.textualData && it.scanTypeName == i.barcodeTypeName }


                        if (!printedBarcodes.contains(i.textualData)) {
                            onNewBarcodeScanned(i.barcodeTypeName, i.textualData)
                            printedBarcodes.add(i.textualData)
                        }

                        val scannedDate = SimpleDateFormat(
                            "yyyy/MM/dd/HH:mm:ss.SSS",
                            Locale.getDefault()
                        ).format(Date())
                        if (existingSessionScan != null) {
                            existingSessionScan.scannedTimesInARow += 1
                            existingSessionScan.highLight = true

                            recentScansToAdd.forEachIndexed { index, recentScan ->
                                val scannedDate2 = SimpleDateFormat(
                                    "yyyy/MM/dd/HH:mm:ss.SSS",
                                    Locale.getDefault()
                                ).format(Date())
                                if (recentScan.scanDate == existingSessionScan.scanDate && recentScan.scanText == existingSessionScan.scanText) {
                                    recentScan.scannedTimesInARow = existingSessionScan.scannedTimesInARow
                                    recentScan.scanDate = scannedDate2
                                    existingSessionScan.scanDate = scannedDate2
                                }
                            }

                        } else {
                            if (thumbnails != null) {
                                if (index < thumbnails.size && croppedBarcodeImage != null) {
                                    croppedBarcodeImage = thumbnails?.get(index)
                                    croppedBarcodePath = saveBitmapToInternalStorage(
                                        context!!,
                                        croppedBarcodeImage,
                                        "croppedBarcodeImage"
                                    )
                                } else {
                                    croppedBarcodePath = null
                                }
                            }

                            val newSessionScan = SessionScan(
                                scannedDate,
                                i.textualData,
                                if (i.extra != null) formatBarcodeName(i.barcodeTypeName, i.extra.toList()) else i.barcodeTypeName,
                                null,
                                null,
                                null,
                                mainPath,
                                croppedBarcodePath,
                                if (i.extra != null) formattedText(i.extra.toList()) else "",
                                if (i.extra != null) formattedTextJson(i.extra.toList()) else "",
                                if (i.extra != null) extractImageRawBase64(i.extra.toList()) else "",
                                1,
                                highLight = true
                            )

                            sessionScansAdapterData.add(newSessionScan)

                            recentScansToAdd.add(
                                RecentScan2(
                                    scannedDate,
                                    i.textualData,
                                    if (i.extra != null) formatBarcodeName(i.barcodeTypeName, i.extra.toList()) else i.barcodeTypeName,
                                    null,
                                    null,
                                    null,
                                    null,
                                    croppedBarcodePath,
                                    if (i.extra != null) formattedText(i.extra.toList()) else "",
                                    if (i.extra != null) formattedTextJson(i.extra.toList()) else "",
                                    if (i.extra != null) extractImageRawBase64(i.extra.toList()) else "",
                                    scannedTimesInARow = 1,
                                    highlighted = true
                                )
                            )
                            Log.d("Scanning", "New Result Added: ${i.textualData}")
                        }
                    }

                    scannedResults.clear()
                }

           }



    private fun showOrUpdateBottomSheet(
        barcodeDataList: MutableList<String>,
        barcodeTypeList: MutableList<String>,
        barcodeListDate: MutableList<String>,
        scannedBarcodes: String,
        imageResult: Bitmap?
    ) {
        lifecycleScope.launch(Dispatchers.Main.immediate) {
            val fm = supportFragmentManager

            // If not safe to commit now, defer until resumed
            if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) || fm.isStateSaved) {
                pendingBottomSheetArgs = {
                    showOrUpdateBottomSheet(barcodeDataList, barcodeTypeList, barcodeListDate, scannedBarcodes, imageResult)
                }
                lifecycleScope.launchWhenResumed {
                    pendingBottomSheetArgs?.invoke()
                    pendingBottomSheetArgs = null
                }
                return@launch
            }

            val tag = "ResultBottomDialogFragment"
            val existing = fm.findFragmentByTag(tag) as? ResultBottomDialogFragment

            if (existing?.isVisible == true) {
                existing.updateBarcodeInfo(
                    barcodeDataList, barcodeTypeList, barcodeListDate, scannedBarcodes, imageResult, sessionScansAdapterData
                )
                if (automaticShowBottomSheet) existing.changeBottomSheetState()
            } else if (automaticShowBottomSheet && !isBottomSheetDialogShown) {
                val frag = ResultBottomDialogFragment.newInstance(
                    barcodeDataList, barcodeTypeList, barcodeListDate, imageResult, scannedBarcodes, sessionScansAdapterData
                )
                frag.show(fm, tag)
                isBottomSheetDialogShown = true
            }
        }
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
        val formattedText = extra?.firstOrNull { it.key == "formattedText" }?.value
            ?: return "" // If no formattedText, return empty string

        return formattedText
            .lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("ImageRawBase64:") }
            ?.substringAfter("ImageRawBase64:")
            ?.trim()
            ?: "" // If ImageRawBase64 is not found, return empty string
    }

    fun onBarcodeScanned(
        barcodeDataList: MutableList<String>,
        barcodeTypeList: MutableList<String>,
        barcodeListDate: MutableList<String>,
        scannedBarcodes: String,
        imageResult: Bitmap? = null
    ) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val sharedPrefs = getSharedPreferences(
            packageName + scanMode.prefKey,
            Context.MODE_PRIVATE
        )

        var automaticallyShowBottomSheetBatchMultiScan =
            sharedPrefs.getBoolean("pref_key_automatic_show_bottomsheet2", true)

        if (scanMode.title == "Batch MultiScan") {
            automaticShowBottomSheet = automaticallyShowBottomSheetBatchMultiScan
        } else if (scanMode.title == "Anycode") {
            automaticShowBottomSheet = sharedPrefs.getBoolean("showBottomSHeet", true)
        } else if (scanMode.template != null) {
            automaticShowBottomSheet = sharedPrefs.getBoolean("showBottomSHeet", true)
        } else {
            automaticShowBottomSheet = prefs.getBoolean("showBottomSHeet")
        }

        existingFragment = supportFragmentManager
            .findFragmentByTag("ResultBottomDialogFragment") as ResultBottomDialogFragment?

        if (existingFragment != null && existingFragment!!.isVisible) {
            existingFragment!!.updateBarcodeInfo(
                barcodeDataList,
                barcodeTypeList,
                barcodeListDate,
                scannedBarcodes,
                imageResult,
                sessionScansAdapterData
            )
            if (automaticShowBottomSheet) {
                existingFragment!!.changeBottomSheetState() // expand/show; DO NOT create a new fragment here
            }
            isBottomSheetDialogShown = true
        } else if (automaticShowBottomSheet && !isBottomSheetDialogShown) {
            // set the flag BEFORE show to avoid double-show races
            isBottomSheetDialogShown = true

            val newBottomSheetFragment = ResultBottomDialogFragment.newInstance(
                barcodeDataList,
                barcodeTypeList,
                barcodeListDate,
                imageResult,
                scannedBarcodes,
                sessionScansAdapterData
            )

            // Optional: avoid showing during state save
            if (!supportFragmentManager.isStateSaved) {
                newBottomSheetFragment.show(
                    supportFragmentManager,
                    "ResultBottomDialogFragment"
                )
            }
        }
    }

    fun showDialogBtn() {
        if (!isBottomSheetDialogShown) {
            val newBottomSheetFragment =
                ResultBottomDialogFragment.newInstance(
                    barcodeListResult,
                    barcodeListType,
                    barcodeListDate,
                    croppedBarcodeImage,
                    scannedBarcodes.toString(),
                    sessionScansAdapterData
                )
            newBottomSheetFragment.show(supportFragmentManager, "ResultBottomDialogFragment")
            isBottomSheetDialogShown = true
        }
    }

    private fun setZoom() {
        factor = 1.0f
        if (isZoomed) {
            factor = maxZoomFactor / 2f
        }

        binding.bkdView.setZoomFactor(factor)
        binding.bkdView.setZoomFactorInitial(factor)
    }

    private fun setFlash() {
        if (isFlashOn) {
            binding.bkdView.setFlashEnabled(true)
            binding.bkdView.setFlashInitial(true)
        }

    }

    companion object {
        const val ARGS_MODE_KEY = "scanMode"
    }


    @SuppressLint("SuspiciousIndentation")
    override fun onStartScanningClicked() {
        binding.imageScanned.visibility = View.GONE
        isScanning = true
        isBottomSheetDialogShown = false
        binding.bkdView.startScanning(this)

    }

    fun getCurrentTimeWithTimestamp(): String {
        val pattern = "yyyy-MM-dd"
        val simpleDateFormat = SimpleDateFormat(pattern)
        val currentDate = simpleDateFormat.format(Date())
        return currentDate
    }

    fun showSecretTextView() {
        binding.textFps.visibility = View.VISIBLE
        binding.textDps.visibility = View.VISIBLE
    }

    fun hideSecretTextView() {
        binding.textFps.visibility = View.INVISIBLE
        binding.textDps.visibility = View.INVISIBLE
    }




    fun formatDateString(inputDate: String?): String? {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        val date: Date?
        try {
            date = inputFormat.parse(inputDate)
            return if (date != null) {
                outputFormat.format(date)
            } else {
                null // or handle parsing error
            }
        } catch (e: ParseException) {
            e.printStackTrace()
            return null // or handle parsing error
        }
    }

    fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap?, filePrefix: String): String {
        if (bitmap == null) return ""

        val directory = File(context.filesDir, "images")
        if (!directory.exists()) {
            directory.mkdirs()
        }

        val fileName = "${filePrefix}_${System.currentTimeMillis()}.png" // Save as JPG
        val file = File(directory, fileName)

        try {
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream) // 50% quality
            outputStream.flush()
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return file.absolutePath
    }

    fun showFullScreenImage(context: Context, bitmap: Bitmap?) {
        // Create a Dialog to display the image
        val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_fullscreen_image)  // Inflate the dialog layout

        // Find the ImageView in the dialog layout and set the image from the clicked ImageView
        val fullScreenImageView = dialog.findViewById<ImageView>(R.id.fullScreenImageView)

        // Set the bitmap directly into the ImageView
        fullScreenImageView.setImageBitmap(bitmap)

        // Show the dialog
        dialog.show()

        // Optionally, dismiss the dialog when the image is clicked
        fullScreenImageView.setOnClickListener {
            dialog.dismiss()
        }

    }

    private fun updateSearchEngineOnBarcodeDetails(btn : MaterialButton? = null, result : String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val searchEngineWeb = prefs.getString(getString(R.string.key_result_searchEngine))
        when (searchEngineWeb) {

            "Google" ->  btn?.setOnClickListener {
                if (CommonUtil.isTextURL(result)) {
                    CommonUtil.openURLInBrowser(result, this)
                } else {
                    var encodedURL = URLEncoder.encode(result, "UTF-8")
                    CommonUtil.openSearchInBrowser("https://www.google.com/search?q=",encodedURL, this )
                }
            }
            "Yahoo" -> btn?.setOnClickListener {
                if (CommonUtil.isTextURL(result)) {
                    CommonUtil.openURLInBrowser(result, this)
                } else {
                    var encodedURL = URLEncoder.encode(result, "UTF-8")
                    CommonUtil.openSearchInBrowser(
                        "https://search.yahoo.com/search?p=",
                        encodedURL,
                        this
                    )
                }
            }
            "DuckDuckGo" -> btn?.setOnClickListener {
                if (CommonUtil.isTextURL(result)) {
                    CommonUtil.openURLInBrowser(result, this)
                } else {
                    var encodedURL = URLEncoder.encode(result, "UTF-8")
                    CommonUtil.openSearchInBrowser(
                        "https://duckduckgo.com/?q=",
                        encodedURL,
                        this
                    )
                }
            }
            "Yandex" -> btn?.setOnClickListener {
                if (CommonUtil.isTextURL(result)) {
                    CommonUtil.openURLInBrowser(result, this)
                } else {
                    var encodedURL = URLEncoder.encode(result, "UTF-8")
                    CommonUtil.openSearchInBrowser(
                        "https://yandex.com/search/?text=",
                        encodedURL,
                        this
                    )
                }
            }
            "Bing" -> btn?.setOnClickListener {
                if (CommonUtil.isTextURL(result)) {
                    CommonUtil.openURLInBrowser(result, this)
                } else {
                    var encodedURL = URLEncoder.encode(result, "UTF-8")
                    CommonUtil.openSearchInBrowser(
                        "https://www.bing.com/search?q=",
                        encodedURL,
                        this
                    )
                }
            }
            "Brave" -> btn?.setOnClickListener {
                if (CommonUtil.isTextURL(result)) {
                    CommonUtil.openURLInBrowser(result, this)
                } else {
                    var encodedURL = URLEncoder.encode(result, "UTF-8")
                    CommonUtil.openSearchInBrowser(
                        "https://search.brave.com/search?q=",
                        encodedURL,
                        this
                    )
                }
            }
        }
    }

    private fun checkCameraPermission(finished: (Boolean) -> Unit) {
        when {
            // Permission already granted
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                finished(true)
            }

            // Permission denied but can show rationale (not permanently denied)
            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) -> {
                finished(false) // Permission denied but can request again
            }

            // Permission denied permanently or first time (directly request permission)
            else -> {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
            }
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                try {
                    binding.bkdView.startScanning(this);
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                showPermissionAlert()
            }
        }
    }

    private fun showPermissionAlert() {
        AlertDialog.Builder(this)
            .setTitle("Camera Acess Restricted")
            .setMessage("Adjust Settings for Permissions")
            .setNegativeButton("Close") { dialog, _ ->
                dialog.dismiss() // Dismiss the dialog
            }
            .setPositiveButton("Settings") { dialog, _ ->
                openAppSettings()
                dialog.dismiss()

            }
            .setCancelable(false)
            .show()
    }

    private fun openAppSettings() {
        // Open the app settings page
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.setData(Uri.parse("package:$packageName"))
        startActivity(intent)
    }

    private fun onNewBarcodeScanned(barcodeType: String, barcodeText: String) {
        val newBarcode = BarcodeDataPrint(barcodeType + ": ", barcodeText)
        adapterBarcodePrint.addItem(newBarcode)

        // Scroll to the top to show the latest scanned item
        recyclerViewBarcodePrint.scrollToPosition(0)
    }


    private fun autoSendWebhook() {
        val repository = BarcodeDataRepository()
        val viewModelFactory = BarcodeDataViewModelFactory(repository)
        viewModel = ViewModelProvider(this, viewModelFactory).get(BarcodeDataViewModel::class.java)
        val sharedPreferences = this.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val urlWebHook = sharedPreferences.getString(getString(R.string.key_url_webhook), "")
        val keyWebHook = sharedPreferences.getString(getString(R.string.key_secret_word_webhook), "")
        val uri = Uri.parse(urlWebHook)
        val baseUrl = "${uri.scheme}://${uri.host}/"

        var endPointUrl = extractEndpointFromUrl(urlWebHook!!)
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        var webHookEncodeData = prefs.getBoolean(getString(R.string.key_webhook_encode_data), false)
        var enabledWebhook = prefs.getBoolean(getString(R.string.key_enable_webhook), true)
        var webHookFeedBack = prefs.getBoolean(getString(R.string.key_webhook_feedback), true)

        if (enabledWebhook) {

            if (!NetworkUtils.isInternetAvailable(this)) {
                Toast.makeText(
                    this,
                    getString(R.string.toast_network_error_autosend),
                    Toast.LENGTH_SHORT
                ).show()
            } else {

                if (!urlWebHook.isNullOrBlank()) {
                    RetrofitIInstance.rebuild(baseUrl)

                    val secretWord = keyWebHook.orEmpty()
                    val timestamp  = generate10BitTimestamp()
                    val securityHash = generateMD5Hash(timestamp, secretWord)

                    val jsonArray = ArrayList<Map<String, String>>()

                    // use ALL items, not just last()
//                    val count = minOf(scannedBarcodesResultList.size, scannedBarcodesTypesList.size)
                    for (i in 0 until latestResults.size) {
                        val result = latestResults[i].textualData
                        val symbology = latestResults[i].barcodeTypeName

                        val encodedResult    = if (webHookEncodeData) encodeStringToBase64(result) else result
                        val encodedSymbology = if (webHookEncodeData) encodeStringToBase64(symbology) else symbology

                        val jsonData = mapOf(
                            "base64" to if (webHookEncodeData) "true" else "false",   // stays string since Map<String,String>
                            getString(R.string.webhook_value_title) to encodedResult,
                            getString(R.string.webhook_symobology_title) to encodedSymbology,
                            getString(R.string.webhook_date_title)  to timestamp

                        )
                        jsonArray.add(jsonData)
                    }
                    val payload = BarcodeScanedData(timestamp, securityHash, jsonArray)

                    Log.d("endousada", urlWebHook)
                    viewModel.createPost(urlWebHook, payload) { success, code, message ->
                        runOnUiThread {
                            if (success) {
                                logWebhookAttemptAndResults(urlWebHook, code,message,true )
                                if (webHookFeedBack) {
                                    Log.d("messgae", message!!)
                                    Toast.makeText(
                                        this,
                                        "Webhook accepted : \n$message",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                logWebhookAttemptAndResults(urlWebHook, code,message,false )
                                if (webHookFeedBack) {
                                    Toast.makeText(
                                        this,
                                        "Webhook failed : $message $code",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    private fun logWebhookAttemptAndResults(
        urlHost: String,
        statusCode: Int?,
        message: String?,
        success: Boolean
    ) {
        Log.d("FIREBASE_TEST", "Logging webhook analytics event")

        firebaseAnalytics.logEvent("webhook_attempt_android") {
            param("url_host", urlHost)
        }

        firebaseAnalytics.logEvent("webhook_result_android") {
            param("success", success.toString())
            statusCode?.let { param("status_code", it.toLong()) }
            message?.let { param("message", it.take(100)) }
            param("url_host", urlHost)
        }
    }


    private fun materialDialogError(title : String, message : String, context : Context) {
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton("Continue") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    private fun encodeStringToBase64(stringToEncode: String): String {
        val data = stringToEncode.toByteArray(Charsets.UTF_8)
        return Base64.encodeToString(data, Base64.DEFAULT)
    }

    private fun extractEndpointFromUrl(url: String): String {
        val trimmedUrl = url.trimEnd('/')
        val uriParts = trimmedUrl.split('/')
        return uriParts.last()
    }

    private fun generate10BitTimestamp(): String {
        val currentTimestamp = System.currentTimeMillis()
        val timestamp10Bit = currentTimestamp / 1000

        return timestamp10Bit.toString()
    }

    private fun generateMD5Hash(timestamp: String, secretWord: String): String {
        val input = timestamp + secretWord
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }


}
