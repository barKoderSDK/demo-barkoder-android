package com.barkoder.demoscanner

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
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

class MainActivity : AppCompatActivity(), BarkoderResultCallback {

    private lateinit var binding: ActivityMainBinding
    private var existingFragment: ResultBottomDialogFragment? = null

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

    private var picturePath: String? = null
    private var documentPath: String? = null
    private var signaturePath: String? = null
    private var mainPath: String? = null
    var sessionScansAdapterData: MutableList<SessionScan> = mutableListOf()

    companion object {
        var barkoderConfig: BarkoderConfig? = null
    }
    private val activityJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + activityJob)

    private lateinit var firebaseAnalytics: FirebaseAnalytics


    private val storagePermission by lazy {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else
            Manifest.permission.READ_EXTERNAL_STORAGE
    }

    private val storagePermissionResult =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { result: Boolean ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                when {
                    result -> startImagePicker()
                    shouldShowRequestPermissionRationale(storagePermission)
                    -> showStorageRequestPermissionRationale()

                    else -> {
                        MaterialAlertDialogBuilder(this)
                            .setMessage(R.string.storage_final_warning_message)
                            .setNegativeButton(R.string.close_button) { dialog, _ ->
                                dialog.dismiss()
                            }
                            .setPositiveButton(R.string.settings_button) { dialog, _ ->
                                openAppSettings()
                            }
                            .show()
                    }
                }
            } else
                startImagePicker()
        }

    private val pickImageResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data?.data != null)
                scanImageFromUri(result.data!!.data)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        context = this
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

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

        firebaseAnalytics = Firebase.analytics

        binding.cardBarcodesIndustrial1D.setOnClickListener {
            startActivity(getScannerIntent(ScanMode.INDUSTRIAL_1D))
        }

        binding.cardBarcodesRetail1D.setOnClickListener {
            startActivity(getScannerIntent(ScanMode.RETAIL_1D))
        }

        binding.cardBarcodesPDF.setOnClickListener {
            startActivity(getScannerIntent(ScanMode.PDF))
        }

//        binding.cardBarcodesQR.setOnClickListener {
//            startActivity(getScannerIntent(ScanMode.QR))
//        }

        binding.cardBarcodes2DAll.setOnClickListener {
            startActivity(getScannerIntent(ScanMode.ALL_2D))
        }

        binding.cardScanFromGallery.setOnClickListener {
            scanFromGallery()
        }

        binding.cardContinuousMode.setOnClickListener {
            startActivity(getScannerIntent(ScanMode.CONTINUOUS))
        }

        binding.cardDpmMode.setOnClickListener {
            startActivity(getScannerIntent(ScanMode.DPM))
        }
        binding.cardVinMode.setOnClickListener {
            startActivity(getScannerIntent(ScanMode.VIN))
        }
        binding.cardDotCode.setOnClickListener {
            startActivity(getScannerIntent(ScanMode.DOTCODE))
        }
        binding.cardMisshaped1D.setOnClickListener {
            startActivity(getScannerIntent(ScanMode.MISSHAPED_1D))
        }
        binding.cardUpcEanDeblur.setOnClickListener {
            startActivity(getScannerIntent(ScanMode.UPC_EAN_DEBLUR))
        }
        binding.cardBarcodes1DALL.setOnClickListener {
            startActivity(getScannerIntent(ScanMode.ALL_1D))
        }
        binding.cardScanWebdemo.setOnClickListener {
            CommonUtil.openURLInBrowser("https://barkoder.com/barkoder-wasm-demo", this)
        }
        binding.txtRecent.setOnClickListener {
            startActivity(Intent(this@MainActivity, RecentActivity::class.java))
        }

        binding.txtAbout.setOnClickListener {
            startActivity(Intent(this@MainActivity, AboutActivity::class.java))
        }

        binding.txtScan.setOnClickListener {
            startActivity(getScannerIntent(ScanMode.ANYSCAN))
        }

        binding.cardScanIdDocument.setOnClickListener {
            startActivity(getScannerIntent(ScanMode.MRZ))
        }
        binding.compositeCard.setOnClickListener {
            startActivity(getScannerIntent(ScanMode.COMPOSITE))
        }
        binding.compositeCard.setOnClickListener {
            startActivity(getScannerIntent(ScanMode.COMPOSITE))
        }
        binding.cardBarcodesPostal.setOnClickListener {
            startActivity(getScannerIntent(ScanMode.POSTAL_CODES))
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

    //region Scan from gallery

    private fun scanFromGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when {
                ActivityCompat.checkSelfPermission(
                    this,
                    storagePermission
                ) == PackageManager.PERMISSION_GRANTED -> {
                    startImagePicker()
                }

                shouldShowRequestPermissionRationale(storagePermission) -> {
                    showStorageRequestPermissionRationale()
                }

                else -> {
                    storagePermissionResult.launch(storagePermission)
                }
            }
        } else
            startImagePicker()
    }

    private fun startImagePicker() {
        val pickImageIntent = Intent(Intent.ACTION_PICK)
        pickImageIntent.type = "image/*"
        pickImageResult.launch(pickImageIntent)
    }

    private fun showStorageRequestPermissionRationale() {
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.storage_permission_rationale_message)
            .setCancelable(false)
            .setPositiveButton(R.string.continue_button) { dialog, _ ->
                dialog.dismiss()
                storagePermissionResult.launch(storagePermission)
            }
            .setNegativeButton(R.string.cancel_button, null)
            .show()
    }

    private fun scanImageFromUri(uri: Uri?) {
        binding.progressIndicator.isVisible = true
        ImageUtil.bitmapFromUri(contentResolver, uri,3000,3000)?.let {
            BarkoderHelper.scanImage(
                it,
                BKDConfigUtil.configureBKD(
                    this,
                    ScanMode.GALLERY_SCAN,
                    true
                ),
                this, this
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

                    sessionScansAdapterData.add(SessionScan(scannedDate,result.textualData, if(result.extra != null) formatBarcodeName(result.barcodeTypeName, result.extra.toList()) else result.barcodeTypeName,picturePath, documentPath, signaturePath,mainPath,croppedBarcodePath,if(result.extra != null) formattedText(result.extra.toList()) else ""))
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
                            if(result.extra != null) formattedText(result.extra.toList()) else ""
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

                    sessionScansAdapterData.add(SessionScan(scannedDate,result.textualData, if(result.extra != null) formatBarcodeName(result.barcodeTypeName, result.extra.toList()) else result.barcodeTypeName,picturePath, documentPath, signaturePath,mainPath,croppedBarcodePath,if(result.extra != null) formattedText(result.extra.toList()) else ""))
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
                            if(result.extra != null) formattedText(result.extra.toList()) else ""
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
                       settingsIntent.putExtra(SettingsFragment.ARGS_MODE_KEY, 15)
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

        // Iterate over the list to find the key "formattedText"
        for (item in extra) {
            if (item.key == "formattedText") {
                // Return the value associated with the key "formattedText"
                return item.value ?: ""
            }
        }

        // If the key "formattedText" is not found, return a default message
        return ""
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

    fun openAppSettings() {
        try {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${applicationContext.packageName}")
            }
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
            Toast.makeText(this, "Unable to open app settings.", Toast.LENGTH_SHORT).show()
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


