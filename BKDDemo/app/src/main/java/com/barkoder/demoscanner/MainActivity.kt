package com.barkoder.demoscanner

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
import com.barkoder.Barkoder
import com.barkoder.BarkoderConfig
import com.barkoder.BarkoderHelper
import com.barkoder.demoscanner.databinding.ActivityMainBinding
import com.barkoder.demoscanner.enums.ScanMode
import com.barkoder.demoscanner.fragments.ResultBottomDialogFragment
import com.barkoder.demoscanner.fragments.SettingsFragment
import com.barkoder.demoscanner.utils.BKDConfigUtil
import com.barkoder.demoscanner.utils.DemoDefaults
import com.barkoder.demoscanner.utils.ImageUtil
import com.barkoder.demoscanner.utils.ScannedResultsUtil
import com.barkoder.interfaces.BarkoderResultCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), BarkoderResultCallback {

    private lateinit var binding: ActivityMainBinding

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
                            .setPositiveButton(R.string.close_button) { dialog, _ ->
                                dialog.dismiss()
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

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val defaultSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)

        Log.d("libVersion", Barkoder.GetLibVersion())
        // For GLOBAL and CONTINUOUS scan modes this can be done on app start because app default values
        // are used. For all other scan modes (that are related with templates) first we need to apply
        // related template to the config, and later use config (template) values as default ones (done in BKDConfigUtil)
        BKDConfigUtil.setDefaultValuesInPrefs(defaultSharedPrefs, this, true, ScanMode.GLOBAL)
        BKDConfigUtil.setDefaultValuesInPrefs(getSharedPreferences(packageName + ScanMode.ANYSCAN.prefKey, Context.MODE_PRIVATE),
            this,
            true,
            ScanMode.ANYSCAN)
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
            getSharedPreferences(packageName + ScanMode.UPC_EAN_DEBLUR.prefKey, Context.MODE_PRIVATE),
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
        binding.txtRecent.setOnClickListener {
            startActivity(Intent(this@MainActivity, RecentActivity::class.java))
        }

        binding.txtAbout.setOnClickListener {
            startActivity(Intent(this@MainActivity, AboutActivity::class.java))
        }

        binding.txtScan.setOnClickListener {
            startActivity(getScannerIntent(ScanMode.ANYSCAN))
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
        ImageUtil.bitmapFromUri(contentResolver, uri)?.let {
            BarkoderHelper.scanImage(
                it,
                BKDConfigUtil.configureBKD(
                    this,
                    ScanMode.GLOBAL,
                    true
                ),
                this, this
            )
        }
    }


    override fun scanningFinished(
        results: Array<out Barkoder.Result>?,
        thumbnails: Array<out Bitmap>?,
        resultImage: Bitmap?
    ) {
        binding.progressIndicator.isVisible = false
        var barcodeResults = mutableListOf<String>()
        var barcodeTypes = mutableListOf<String>()
        var barcodesDates = mutableListOf<String>()
        for(i in results!!) {
            barcodeResults.add(i.textualData)
            barcodeTypes.add(i.barcodeTypeName)
            barcodesDates.add(getFormattedTimestamp())
        }

        uiScope.launch {
                if(results.size == 0) {
                    MaterialAlertDialogBuilder(this@MainActivity)
                        .setMessage("No barcodes found")
                        .setPositiveButton("OK") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
                else {
            val bottomSheetFragment = ResultBottomDialogFragment.newInstance(
                barcodeResults,
                barcodeTypes, barcodesDates,null, null
            )
                    bottomSheetFragment.show(supportFragmentManager, "ResultBottomDialogFragment")
                }

        }
    }

    private fun getFormattedTimestamp(): String {
        val dateFormat = SimpleDateFormat("MMM dd yyyy", Locale.getDefault())
        return dateFormat.format(Date())
    }

    //endregion Scan from gallery
}
