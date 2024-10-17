package com.barkoder.demoscanner

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Picture
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.method.ScrollingMovementMethod
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.barkoder.Barkoder
import com.barkoder.BarkoderConfig
import com.barkoder.BarkoderView
import com.barkoder.demoscanner.databinding.ActivityScannerBinding
import com.barkoder.demoscanner.enums.ScanMode
import com.barkoder.demoscanner.fragments.ResultBottomDialogFragment
import com.barkoder.demoscanner.fragments.SettingsFragment
import com.barkoder.demoscanner.models.RecentScan2
import com.barkoder.demoscanner.models.SessionScan
import com.barkoder.demoscanner.utils.BKDConfigUtil
import com.barkoder.demoscanner.utils.CommonUtil
import com.barkoder.demoscanner.utils.ScannedResultsUtil
import com.barkoder.demoscanner.utils.getBoolean
import com.barkoder.demoscanner.utils.getString
import com.barkoder.demoscanner.viewmodels.RecentScanViewModel
import com.barkoder.interfaces.BarkoderResultCallback
import com.barkoder.interfaces.CameraCallback
import com.barkoder.interfaces.MaxZoomAvailableCallback
import com.bumptech.glide.Glide
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
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Date
import java.util.Locale
import kotlin.math.sign


//TODO zoom from pinched can't be reset on resume
class ScannerActivity : AppCompatActivity(), BarkoderResultCallback, MaxZoomAvailableCallback,
    CameraCallback, ResultBottomDialogFragment.BottomSheetStateListener {

    private lateinit var binding: ActivityScannerBinding
    private lateinit var recentViewModel: RecentScanViewModel

    private var pictureBitmap: Bitmap? = null
    private var documentBitmap: Bitmap? = null
    private var signatureBitmap: Bitmap? = null
    private var mainBitmap: Bitmap? = null
    private var croppedBarcodePath: String? = null
    private var croppedBarcodeImageOnScannedBarcode: Bitmap? = null

    private var picturePath: String? = null
    private var documentPath: String? = null
    private var signaturePath: String? = null
    private var mainPath: String? = null
    private var resultsTemp: Array<out Barkoder.Result>? = null
    var sessionScansAdapterData: ArrayList<SessionScan> = arrayListOf()

    private var checkOver21Dialog = false
    private var expiredIDDialog = false

    private var isBottomSheetDialogShown = false
    private lateinit var scanMode: ScanMode
    var scannedBarcodes = 0
    private var isZoomed = false
    private var maxZoomFactor: Float = -1f
    private var isFlashOn = false
    private var isScanning = true
    private var automaticShowBottomSheet: Boolean = false
    private var continiousModeOn = false
    private var context: Context? = null

    private var existingFragment: ResultBottomDialogFragment? = null
    private var croppedBarcodeImage: Bitmap? = null
    var receivedBooleanValue: Boolean = false
    var settingsChangedBoolean: Boolean = false
    val barcodeListResult = mutableListOf<String>()
    val barcodeListType = mutableListOf<String>()
    val barcodeListDate = mutableListOf<String>()
    private val PREFS_NAME = "MyPrefsFile"
    private val activityJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + activityJob)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        scanMode = ScanMode.values()[intent.extras!!.getInt(ARGS_MODE_KEY)]
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        recentViewModel = ViewModelProvider(this).get(RecentScanViewModel::class.java)
        context = this
        binding.btnClose.setOnClickListener {
            finish()
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        var autoFocusBoolean = prefs.getBoolean("pref_key_autofocus", false)
        var dynamicExposureBoolean = prefs.getBoolean("pref_key_dynamic_exposure", false)
        if (autoFocusBoolean) {
            binding.bkdView.setAutoFocusCenteredState(false)
        } else {
            binding.bkdView.setAutoFocusCenteredState(false)
        }
        if (dynamicExposureBoolean) {
            binding.bkdView.setDynamicExposureState(false)
        } else {
            binding.bkdView.setDynamicExposureState(false)
        }


        binding.btnSettings.setOnClickListener {
            val settingsIntent = Intent(this@ScannerActivity, SettingsActivity::class.java)
            settingsIntent.putExtra(SettingsFragment.ARGS_MODE_KEY, scanMode.ordinal)
            startActivity(settingsIntent)
        }
        val sharedPreferences: SharedPreferences =
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        receivedBooleanValue = sharedPreferences.getBoolean("BOOLEAN_KEY", false)

        binding.btnZoom.setOnClickListener {
            it.setBackgroundResource(if (isZoomed) R.drawable.ic_zoom_on else R.drawable.ic_zoom_off)
            isZoomed = !isZoomed
            setZoom()
        }

        binding.btnFlash.setOnClickListener {
            it.setBackgroundResource(if (isFlashOn) R.drawable.ic_flash_off else R.drawable.ic_flash_on)
            isFlashOn = !isFlashOn
            binding.bkdView.setFlashEnabled(isFlashOn)
        }

        binding.txtScannedResult.movementMethod = ScrollingMovementMethod()

        binding.btnShowDialog.setOnClickListener {
            showDialogBtn()
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

    }

    override fun onStart() {
        super.onStart()
        binding.bkdView.config = BKDConfigUtil.configureBKD(this, scanMode)
        binding.textFps.text = "fps"
        binding.textDps.text = "dps"
        binding.txtEnabledTypes.text =
            BKDConfigUtil.getEnabledTypesAsStringArray(
                binding.bkdView.config.decoderConfig,
                resources
            )

        if (binding.bkdView.config.thresholdBetweenDuplicatesScans > 0) {
            binding.continiousModeOn.visibility = View.VISIBLE
            binding.continiousModeOn.text =
                "Continuous / ${binding.bkdView.config.thresholdBetweenDuplicatesScans.toString()}s delay"
        } else {
            binding.continiousModeOn.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        val sharedPreferences: SharedPreferences =
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        settingsChangedBoolean = sharedPreferences.getBoolean("settingsChangedBoolean", false)
        if (settingsChangedBoolean) {
            Toast.makeText(
                this,
                "Settings are changed for ${scanMode.title} template!",
                Toast.LENGTH_SHORT
            ).show()
        }
        if (binding.bkdView.config.thresholdBetweenDuplicatesScans == 0) {
            binding.bkdView.config.isCloseSessionOnResultEnabled = true
            binding.bkdView.config.decoderConfig.maximumResultsCount = 200
        } else {
            binding.bkdView.config.isCloseSessionOnResultEnabled = false
        }
        binding.bkdView.config.isLocationInImageResultEnabled = true
        binding.bkdView.config.isImageResultEnabled = true
        binding.bkdView.setCameraCallback(this)
        if (isScanning) {
            binding.bkdView.startScanning(this)
        } else
            binding.bkdView.startCamera()
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

        binding.bkdView.config.roiLineColor = ContextCompat.getColor(this, R.color.brand_color)
        binding.bkdView.config.locationLineColor = ContextCompat.getColor(this, R.color.brand_color)

    }

    override fun onPause() {
        binding.bkdView.stopScanning()
        val sharedPreferences: SharedPreferences =
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putBoolean("settingsChangedBoolean", false)
        editor.apply()
        super.onPause()
    }


    override fun onCameraReady() {
        if (maxZoomFactor == -1f)
            binding.bkdView.getMaxZoomFactor(this)
        else
            setZoom()

        setFlash()
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

    override fun onMaxZoomAvailable(maxZoom: Float) {
        maxZoomFactor = maxZoom
    }


    override fun scanningFinished(
        results: Array<out Barkoder.Result>?,
        thumbnails: Array<out Bitmap>?,
        imageResult: Bitmap?
    ) {

        Log.d("resutlts size : ", results!!.size!!.toString())
        val sharedPreferences = this.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putInt("lastResultsOnFrame", results!!.size).apply()

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
                    barcodeListType.add(i.barcodeTypeName)
                    barcodeListDate.add(getCurrentTimeWithTimestamp())
                }

                binding.btnShowDialog.visibility = View.VISIBLE

                croppedBarcodeImage = thumbnails!!.last()

                val bottomSheetFragment = ResultBottomDialogFragment.newInstance(
                    barcodeListResult,
                    barcodeListType, barcodeListDate, imageResult, results.size.toString(), sessionScansAdapterData
                )

                setZoom()
                setFlash()

                if (binding.bkdView.config.thresholdBetweenDuplicatesScans == 0) {
                    binding.bkdView.pauseScanning()
                    binding.textScannedNumber.visibility = View.VISIBLE
                    isScanning = false
                    binding.imageScanned.visibility = View.VISIBLE
                    binding.imageScanned.setImageBitmap(imageResult!!)

                    scannedBarcodes = scannedBarcodes + results!!.size
                    bottomSheetFragment.isCancelable = false

                    binding.textScannedNumber.text = "(${scannedBarcodes.toString()})"

                    val title = ScannedResultsUtil.getResultsTitle(results)
                    val readString = ScannedResultsUtil.getResultsReadString(results)
                    for (i in 0 until title.size) {
                        val barcodeType = title[i]
                        val barcodeNumber = readString[i]
                        val spannableText =
                            SpannableString("$barcodeType: $barcodeNumber\n")
                        spannableText.setSpan(
                            ForegroundColorSpan(Color.WHITE),
                            barcodeType.length + 1,
                            spannableText.length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        binding.txtScannedResult.text =
                            TextUtils.concat(spannableText, binding.txtScannedResult.text)
                    }

                    ScannedResultsUtil.storeResultsInPref(applicationContext, results)
                    for (i in results) {
                        val scannedDate =
                            SimpleDateFormat(
                                "yyyy/MM/dd/HH:mm:ss",
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
                                    uiScope.launch(Dispatchers.IO) {

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

                                        sessionScansAdapterData.add(SessionScan(scannedDate,i.textualData, i.barcodeTypeName,picturePath, documentPath, signaturePath,mainPath,croppedBarcodePath))

                                        recentViewModel.addRecentScan(
                                            RecentScan2(
                                                scannedDate,
                                                i.textualData,
                                                i.barcodeTypeName,
                                                picturePath,
                                                documentPath,
                                                signaturePath,
                                                mainPath,
                                                croppedBarcodePath
                                            )
                                        )
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
                            } else {
                                if (i.images != null && i.images.isNotEmpty()) {


                                    results.forEachIndexed { index, result ->


                                        croppedBarcodeImage = thumbnails!!.get(index)
                                        if (imageResult != null) {
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

                                        sessionScansAdapterData.add(SessionScan(scannedDate,result.textualData, result.barcodeTypeName,picturePath, documentPath, signaturePath,mainPath,croppedBarcodePath))

                                        recentViewModel.addRecentScan(
                                            RecentScan2(
                                                scannedDate,
                                                result.textualData,
                                                result.barcodeTypeName,
                                                picturePath,
                                                documentPath,
                                                signaturePath,
                                                mainPath,
                                                croppedBarcodePath
                                            )
                                        )


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
                binding.textScannedNumber.visibility = View.VISIBLE
                val title = ScannedResultsUtil.getResultsTitle(results)
                val readString = ScannedResultsUtil.getResultsReadString(results)
                for (i in 0 until title.size) {
                    val barcodeType = title[i]
                    val barcodeNumber = readString[i]
                    val spannableText =
                        SpannableString("$barcodeType: $barcodeNumber\n")
                    spannableText.setSpan(
                        ForegroundColorSpan(Color.WHITE),
                        barcodeType.length + 1,
                        spannableText.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    binding.txtScannedResult.text =
                        TextUtils.concat(spannableText, binding.txtScannedResult.text)
                }


                bottomSheetFragment.isCancelable = false
                scannedBarcodes = scannedBarcodes + results!!.size

                continiousModeOn = true

                if (imageResult != null) {
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

                }


                ScannedResultsUtil.storeResultsInPref(applicationContext, results)
                    results.forEachIndexed { index, i ->

                        croppedBarcodeImage = thumbnails!!.get(index)
                        if(croppedBarcodeImage != null) {
                            croppedBarcodePath = saveBitmapToInternalStorage(
                                context!!,
                                croppedBarcodeImage,
                                "croppedBarcodeImage"
                            )
                        } else {
                            croppedBarcodePath = null
                        }

                    val scannedDate =
                        SimpleDateFormat(
                            "yyyy/MM/dd/HH:mm:ss",
                            Locale.getDefault()
                        ).format(Date())
                    sessionScansAdapterData.add(SessionScan(scannedDate,i.textualData, i.barcodeTypeName,null, null, null,mainPath,croppedBarcodePath))
                    recentViewModel.addRecentScan(
                        RecentScan2(
                            scannedDate,
                            i.textualData,
                            i.barcodeTypeName,
                            null,
                            null,
                            null,
                            null,
                            croppedBarcodePath
                        )
                    )

                }

            }

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
            sharedPrefs.getBoolean("pref_key_automatic_show_bottomsheet2", false)

        if (scanMode.title == "Batch MultiScan") {
            automaticShowBottomSheet = automaticallyShowBottomSheetBatchMultiScan
        } else if (scanMode.title == "Anycode") {
            automaticShowBottomSheet = sharedPrefs.getBoolean("showBottomSHeet", true)
        } else if (scanMode.template != null) {
            automaticShowBottomSheet = sharedPrefs.getBoolean("showBottomSHeet", true)
        } else {
            automaticShowBottomSheet = prefs.getBoolean("showBottomSHeet")
        }

        existingFragment =
            supportFragmentManager.findFragmentByTag("ResultBottomDialogFragment") as ResultBottomDialogFragment?
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
                existingFragment!!.changeBottomSheetState()
                isBottomSheetDialogShown = true
            }

        } else {
            if (!isBottomSheetDialogShown) {
                val newBottomSheetFragment =
                    ResultBottomDialogFragment.newInstance(
                        barcodeDataList,
                        barcodeTypeList,
                        barcodeListDate,
                        imageResult,
                        scannedBarcodes
                        ,sessionScansAdapterData
                    )
                if (automaticShowBottomSheet) {
                    newBottomSheetFragment.show(
                        supportFragmentManager,
                        "ResultBottomDialogFragment"
                    )
                    isBottomSheetDialogShown = true
                }
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
        var factor = 1.0f
        if (isZoomed)
            factor = maxZoomFactor / 2f

        binding.bkdView.setZoomFactor(factor)
    }

    private fun setFlash() {
        if (isFlashOn)
            binding.bkdView.setFlashEnabled(true)
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

    public fun showFullScreenDialog(context: Context) {
        runOnUiThread {
            // or use `this` if in an Activity
            val builder =
                AlertDialog.Builder(context, R.style.FullScreenDialogStyle)
            // Inflate the custom layout
            val inflater = LayoutInflater.from(context)
            val dialogView = inflater.inflate(R.layout.custom_dialog_results, null)

            // Find the ImageView and set the bitmap image
            val dialogImageView =
                dialogView.findViewById<ImageView>(R.id.imageViewDialog)
            val firstNameUser = dialogView.findViewById<TextView>(R.id.firstNameUser)
            val dateOfBirthUser = dialogView.findViewById<TextView>(R.id.dateOfBirthUser)
            val issuingCountry = dialogView.findViewById<TextView>(R.id.issuingCountry)
            val genderUser = dialogView.findViewById<TextView>(R.id.genderUser)
            val expirationDateUser =
                dialogView.findViewById<TextView>(R.id.expirationDateUser)
            val nationalityUser = dialogView.findViewById<TextView>(R.id.nationalityUser)
            val documentNumberUser =
                dialogView.findViewById<TextView>(R.id.documentNumberUser)
            val documentTypeUser = dialogView.findViewById<TextView>(R.id.documentType)
            val imageDocument =
                dialogView.findViewById<ImageView>(R.id.imageDocument)
            val imageMain =
                dialogView.findViewById<ImageView>(R.id.imageMain)
            val imagePicture =
                dialogView.findViewById<ImageView>(R.id.imagePicture)
            val imageSignature =
                dialogView.findViewById<ImageView>(R.id.imageSignature)
            val verificationLayout =
                dialogView.findViewById<LinearLayout>(R.id.layoutVerificationUser)
            val verificationChecksLayout =
                dialogView.findViewById<LinearLayout>(R.id.layout_verification_checks)
            val iconVerification =
                dialogView.findViewById<ImageView>(R.id.icon_verification_user)
            val textVerification =
                dialogView.findViewById<TextView>(R.id.text_verification_user)
            val iconVerificationExpire =
                dialogView.findViewById<ImageView>(R.id.icon_verification_expires)
            val iconVerificationOver21 =
                dialogView.findViewById<ImageView>(R.id.icon_verification_over21)
            val textVerificationOver21 =
                dialogView.findViewById<TextView>(R.id.text_verification_over21)
            val textVerificationExpire =
                dialogView.findViewById<TextView>(R.id.text_verification_expires)
            val viewCardPicture = dialogView.findViewById<LinearLayout>(R.id.imagePictureLayout)
            val viewCardDocument = dialogView.findViewById<LinearLayout>(R.id.imageDocumentLayout)
            val viewCardSignature = dialogView.findViewById<LinearLayout>(R.id.imageSignatureLayout)
            val viewCardMain = dialogView.findViewById<LinearLayout>(R.id.imageMainLayout)


            // Split the raw string into lines
            val lines = resultsTemp?.get(0)?.textualData?.split("\n".toRegex())
                ?.dropLastWhile { it.isEmpty() }
                ?.toTypedArray()

            // Initialize variables for first name and last name
            var firstName: String? = null
            var lastName: String? = null
            var documentNumber: String? = null
            var dateOfBirth: String? = null
            var expirationDate: String? = null
            var nationality: String? = null
            var fullName: String? = null
            var documentType: String? = null
            var issuing_country: String? = null
            var gender_user: String? = null

            // Iterate over each line to find the required information
            for (line in lines!!) {
                if (line.startsWith("first_name:")) {
                    firstName = line.split("first_name:".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()[1].trim { it <= ' ' }
                } else if (line.startsWith("last_name:")) {
                    lastName =
                        line.split("last_name:".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[1].trim { it <= ' ' }
                } else if (line.startsWith("document_number:")) {
                    documentNumber =
                        line.split("document_number:".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[1].trim { it <= ' ' }
                } else if (line.startsWith("date_of_birth:")) {
                    dateOfBirth =
                        line.split("date_of_birth:".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[1].trim { it <= ' ' }
                } else if (line.startsWith("nationality:")) {
                    nationality =
                        line.split("nationality:".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[1].trim { it <= ' ' }
                } else if (line.startsWith("date_of_expiry:")) {
                    expirationDate =
                        line.split("date_of_expiry:".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[1].trim { it <= ' ' }
                } else if (line.startsWith("document_type:")) {
                    documentType =
                        line.split("document_type:".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[1].trim { it <= ' ' }
                } else if (line.startsWith("issuing_country:")) {
                    issuing_country =
                        line.split("issuing_country:".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[1].trim { it <= ' ' }
                } else if (line.startsWith("gender:")) {
                    gender_user =
                        line.split("gender:".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[1].trim { it <= ' ' }
                }
            }

            if (firstName == null) firstName = ""
            if (lastName == null) lastName = ""

            val formattedDateBirth =
                formatDateString(dateOfBirth) // Ensure this method returns a formatted date string
            val formattedDateExpiry = formatDateString(expirationDate)



            fullName = "$firstName $lastName"
            firstNameUser.text = firstName + " " + lastName
            dateOfBirthUser.text = formattedDateBirth
            expirationDateUser.text = formattedDateExpiry
            nationalityUser.text = nationality
            documentNumberUser.text = documentNumber
            documentTypeUser.text = documentType
            issuingCountry.text = issuing_country
            genderUser.text = gender_user



            if (pictureBitmap != null) {
                // Set visibility and display the image
                viewCardPicture.visibility = View.VISIBLE
                imagePicture.setImageBitmap(pictureBitmap)
                dialogImageView.setImageBitmap(pictureBitmap)

                // Set the OnClickListener only if pictureBitmap is not null
                viewCardPicture.setOnClickListener {
                    showFullScreenImage(this, pictureBitmap)
                }
            } else {
                // Hide the view if the picture is null
                viewCardPicture.visibility = View.GONE
            }

            if (documentBitmap != null) {
                viewCardDocument.visibility = View.VISIBLE
                imageDocument.setImageBitmap(documentBitmap)

                viewCardDocument.setOnClickListener {
                    showFullScreenImage(this, documentBitmap)
                }
            } else {
                viewCardDocument.visibility = View.GONE
            }

            if (signatureBitmap != null) {
                viewCardSignature.visibility = View.VISIBLE
                imageSignature.setImageBitmap(signatureBitmap)
                viewCardSignature.setOnClickListener {
                    showFullScreenImage(this, signatureBitmap)
                }
            } else {
                viewCardSignature.visibility = View.GONE
            }

            if (mainBitmap != null) {
                viewCardMain.visibility = View.VISIBLE
                imageMain.setImageBitmap(mainBitmap)
                viewCardMain.setOnClickListener {
                    showFullScreenImage(this, mainBitmap)
                }
            } else {
                viewCardMain.visibility = View.GONE
            }

            imageDocument.setImageBitmap(documentBitmap)
            imageSignature.setImageBitmap(signatureBitmap)
            imageMain.setImageBitmap(mainBitmap)

            val closeButton = dialogView.findViewById<ImageButton>(R.id.buttonClose)

            builder.setView(dialogView)
            builder.setCancelable(true)

            val dialog = builder.create()

            closeButton.setOnClickListener { dialog.dismiss() }

            dialog.setOnShowListener {
                val window = dialog.window
                window?.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            dialog.show()

        }
    }

    public fun showBarcodeDetailsDialog(context: Context) {
        runOnUiThread {
            // or use `this` if in an Activity
            val dialog = Dialog(this, R.style.FullScreenDialogStyle)

            // Inflate the custom layout
            val inflater = LayoutInflater.from(this)
            val dialogView = inflater.inflate(R.layout.custom_dialog_barcode_result, null)

            dialog.setContentView(dialogView)

            var barcodeValueText = dialogView.findViewById<TextView>(R.id.barcodeValueText)
            var barcodeTypeText = dialogView.findViewById<TextView>(R.id.barcodeTypeText)
            var barcodeBitmap = dialogView.findViewById<ImageView>(R.id.barcodeImage)

            barcodeBitmap.setImageBitmap(croppedBarcodeImage)
            barcodeValueText.text = resultsTemp!!.last().textualData
            barcodeTypeText.text = resultsTemp!!.last().barcodeTypeName



            val closeButton = dialogView.findViewById<ImageButton>(R.id.buttonClose)


            val btnCopy = dialogView.findViewById<MaterialButton>(R.id.btnCopy)
            val btnSearch = dialogView.findViewById<MaterialButton>(R.id.btnSearch)
            updateSearchEngineOnBarcodeDetails(btnSearch, resultsTemp!!.last().textualData)

            if(CommonUtil.isTextURL(resultsTemp!!.last().textualData)) {
                btnSearch.setIconResource(R.drawable.ico_webhook) // Replace with your new icon
                btnSearch.text = "Open"
            }

            btnCopy.setOnClickListener {
                CommonUtil.copyBarcodeResultText(this, resultsTemp!!.last().textualData)
                Toast.makeText(this, "Values was copied to clipboard!", Toast.LENGTH_SHORT).show()
            }



            closeButton.setOnClickListener { dialog.dismiss() }
            dialog.setOnShowListener {
                val window = dialog.window
                if (window != null) {
                    window.setLayout(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    window.setBackgroundDrawableResource(android.R.color.transparent) // Optional: Transparent background
                }
            }
            dialog.show()
        }
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

        // Get the directory for the app's internal storage
        val directory = File(context.filesDir, "images")
        if (!directory.exists()) {
            directory.mkdirs()  // Create the directory if it doesn't exist
        }

        // Generate a unique file name using timestamp or UUID
        val fileName = "${filePrefix}_${System.currentTimeMillis()}.png"
        val file = File(directory, fileName)

        try {
            // Write the bitmap to the file
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        // Return the absolute file path
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
}
