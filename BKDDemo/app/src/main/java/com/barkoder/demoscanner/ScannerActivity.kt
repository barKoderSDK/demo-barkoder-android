package com.barkoder.demoscanner

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.method.ScrollingMovementMethod
import android.text.style.ForegroundColorSpan
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.barkoder.Barkoder
import com.barkoder.demoscanner.databinding.ActivityScannerBinding
import com.barkoder.demoscanner.enums.ScanMode
import com.barkoder.demoscanner.fragments.ResultBottomDialogFragment
import com.barkoder.demoscanner.fragments.SettingsFragment
import com.barkoder.demoscanner.utils.BKDConfigUtil
import com.barkoder.demoscanner.utils.ScannedResultsUtil
import com.barkoder.interfaces.BarkoderResultCallback
import com.barkoder.interfaces.CameraCallback
import com.barkoder.interfaces.MaxZoomAvailableCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.barkoder.BarkoderConfig
import com.barkoder.demoscanner.utils.getBoolean
import java.text.SimpleDateFormat
import java.util.Date



//TODO zoom from pinched can't be reset on resume
class ScannerActivity : AppCompatActivity(), BarkoderResultCallback, MaxZoomAvailableCallback,
    CameraCallback, ResultBottomDialogFragment.BottomSheetStateListener{

    private lateinit var binding: ActivityScannerBinding

    private var isBottomSheetDialogShown = false
    private lateinit var scanMode: ScanMode
    var scannedBarcodes = 0
    private var isZoomed = false
    private var maxZoomFactor: Float = -1f
    private var isFlashOn = false
    private var isScanning = true
    private var automaticShowBottomSheet : Boolean = false
    private var continiousModeOn = false

    private var existingFragment: ResultBottomDialogFragment? = null
    private var croppedBarcodeImage : Bitmap? = null
    var receivedBooleanValue : Boolean = false
    var settingsChangedBoolean : Boolean = false
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

        binding.btnClose.setOnClickListener {
            finish()
        }
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        var autoFocusBoolean = prefs.getBoolean("pref_key_autofocus", false)
        var dynamicExposureBoolean = prefs.getBoolean("pref_key_dynamic_exposure", false)
        if(autoFocusBoolean) {
            binding.bkdView.setAutoFocusCenteredState(false)
        }else {
            binding.bkdView.setAutoFocusCenteredState(false)
        }
        if(dynamicExposureBoolean) {
            binding.bkdView.setDynamicExposureState(false)
        } else {
            binding.bkdView.setDynamicExposureState(false)
        }

        binding.btnSettings.setOnClickListener {
            val settingsIntent = Intent(this@ScannerActivity, SettingsActivity::class.java)
            settingsIntent.putExtra(SettingsFragment.ARGS_MODE_KEY, scanMode.ordinal)
            startActivity(settingsIntent)
        }
        val sharedPreferences: SharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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

        if(receivedBooleanValue) {
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

        if(binding.bkdView.config.thresholdBetweenDuplicatesScans > 0) {
            binding.continiousModeOn.visibility = View.VISIBLE
            binding.continiousModeOn.text = "Continuous / ${binding.bkdView.config.thresholdBetweenDuplicatesScans.toString()}s delay"
        }
        else {
            binding.continiousModeOn.visibility = View.GONE
        }

    }

    override fun onResume() {
        super.onResume()
        val sharedPreferences: SharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        settingsChangedBoolean = sharedPreferences.getBoolean("settingsChangedBoolean", false)
        if(settingsChangedBoolean) {
            Toast.makeText(this, "Settings are changed for ${scanMode.title} template!", Toast.LENGTH_SHORT).show()

        }

        binding.bkdView.config.isLocationInImageResultEnabled = true
        binding.bkdView.config.isCloseSessionOnResultEnabled = false
        binding.bkdView.config.isImageResultEnabled = true
        binding.bkdView.setCameraCallback(this)
        if (isScanning) {
            binding.bkdView.startScanning(this)
        }
        else
            binding.bkdView.startCamera()
        if(receivedBooleanValue) {
            binding.bkdView.setPerformanceCallback { fps, dps ->
                val fpsFloat = (fps * 10).toInt() / 10.0f
                val dpsFloat = (dps * 10).toInt() / 10.0f
                binding.textFps.text = "FPS: ${fpsFloat.toString()}"
                binding.textDps.text = "DPS: ${dpsFloat.toString()}"
                Log.d("cobe", fps.toString())
                Log.d("cobe", dps.toString())
            }
        }

        binding.bkdView.config.roiLineColor = ContextCompat.getColor(this, R.color.brand_color)
        binding.bkdView.config.locationLineColor = ContextCompat.getColor(this, R.color.brand_color)

    }

    override fun onPause() {
        binding.bkdView.stopScanning()
        val sharedPreferences: SharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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
        

        for(i in results!!) {
            barcodeListResult.add(i.textualData)
            barcodeListType.add(i.barcodeTypeName)
            barcodeListDate.add(getCurrentTimeWithTimestamp())
        }
        binding.btnShowDialog.visibility = View.VISIBLE

            croppedBarcodeImage = thumbnails!!.last()

        val bottomSheetFragment = ResultBottomDialogFragment.newInstance(
            barcodeListResult,
            barcodeListType,barcodeListDate, imageResult, null
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
            }

        else {
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
                    onBarcodeScanned(barcodeListResult, barcodeListType,barcodeListDate, scannedBarcodes.toString(), croppedBarcodeImage)
                }
                binding.textScannedNumber.text = "(${scannedBarcodes.toString()})"

            }

            ScannedResultsUtil.storeResultsInPref(applicationContext, results)
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

        var automaticallyShowBottomSheetBatchMultiScan = sharedPrefs.getBoolean("pref_key_automatic_show_bottomsheet2", false)

        if(scanMode.title == "Batch MultiScan"){
            automaticShowBottomSheet = automaticallyShowBottomSheetBatchMultiScan
        } else if (scanMode.title == "Anycode") {
            automaticShowBottomSheet = sharedPrefs.getBoolean("showBottomSHeet", true)
        }
        else if(scanMode.template != null ) {
            automaticShowBottomSheet = sharedPrefs.getBoolean("showBottomSHeet", true)
        } else {
            automaticShowBottomSheet =  prefs.getBoolean("showBottomSHeet")
        }

        existingFragment =
            supportFragmentManager.findFragmentByTag("ResultBottomDialogFragment") as ResultBottomDialogFragment?
        if (existingFragment != null && existingFragment!!.isVisible) {
            existingFragment!!.updateBarcodeInfo(barcodeDataList, barcodeTypeList,barcodeListDate, scannedBarcodes, imageResult)
            if(automaticShowBottomSheet) {
                existingFragment!!.changeBottomSheetState()
                isBottomSheetDialogShown = true
            }

        }
         else {
            if (!isBottomSheetDialogShown) {
            val newBottomSheetFragment =
                ResultBottomDialogFragment.newInstance(barcodeDataList, barcodeTypeList,barcodeListDate, imageResult, scannedBarcodes)
                if(automaticShowBottomSheet) {
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
                ResultBottomDialogFragment.newInstance(barcodeListResult, barcodeListType,barcodeListDate,croppedBarcodeImage, scannedBarcodes.toString())
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

}
