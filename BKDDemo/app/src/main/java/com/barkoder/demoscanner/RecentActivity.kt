package com.barkoder.demoscanner

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.barkoder.demoscanner.adapters.RecentScansAdapter
import com.barkoder.demoscanner.databinding.ActivityRecentBinding
import com.barkoder.demoscanner.fragments.ResultBottomDialogFragment
import com.barkoder.demoscanner.models.RecentScan
import com.barkoder.demoscanner.utils.CommonUtil
import com.barkoder.demoscanner.utils.ScannedResultsUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecentActivity : AppCompatActivity(), RecentScansAdapter.OnRecentScanItemClickListener, ResultBottomDialogFragment.BottomSheetStateListener {

    private lateinit var binding: ActivityRecentBinding
    private val REQUEST_CODE = 1001
    private var isBottomSheetDialogShown = false


    private val writeStoragePermission by lazy {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    }

    private var scannedBarcodesTypesList : MutableList<String> = mutableListOf()
    private var scannedBarcodesResultList : MutableList<String> = mutableListOf()
    private var scannedBarcodesDataList : MutableList<String> = mutableListOf()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val layoutManager = LinearLayoutManager(this)
        binding.recentScansRecycler.layoutManager = layoutManager
        binding.recentScansRecycler.addItemDecoration(
            DividerItemDecoration(
                this,
                layoutManager.orientation
            )
        )
        RecentScansAdapter(applicationContext, WeakReference(this)).apply {
            binding.recentScansRecycler.adapter = this
            binding.txtNoScans.isVisible = itemCount == 0
        }

        val results = ScannedResultsUtil.getResultsFromPref(this)
        for(i in results){
            scannedBarcodesTypesList.add(i.scanTypeName!!)
            scannedBarcodesResultList.add(i.scanText!!)
            scannedBarcodesDataList.add(i.scanDate)
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        if ((binding.recentScansRecycler.adapter?.itemCount ?: 0) > 0)
            inflater.inflate(R.menu.activity_recent_menu, menu)
        else
            menu.clear()

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_delete_recents -> {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.activity_recent_menu_item_delete_recents_confirmation)
                    .setMessage(null)
                    .setPositiveButton(R.string.activity_recent_menu_item_delete_recents) { _, _ ->
                        deleteAllRecents()
                    }
                    .setNegativeButton(R.string.cancel_button, null)
                    .show()
                true
            }
            R.id.menu_item_export_csv -> {
                if (ContextCompat.checkSelfPermission(
                        this,
                        writeStoragePermission
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(writeStoragePermission),
                        REQUEST_CODE
                    )
                } else {
                    saveToCSV(scannedBarcodesTypesList, scannedBarcodesResultList, scannedBarcodesDataList)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onRecentScanItemClick(item: RecentScan) {
            var resultList = mutableListOf<String>()
                resultList.add(item.readString()!!)
            var typeList = mutableListOf<String>()
                typeList.add(item.scanTypeName!!)
            var dateList = mutableListOf<String>()
                dateList.add(item.scanDate)
        if (!isBottomSheetDialogShown) {

            val bottomSheetFragment = ResultBottomDialogFragment.newInstance(
                resultList,
                typeList, dateList, null
            )
            bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
            isBottomSheetDialogShown = true
        }

    }

    override fun onRecentScanItemLongClick(item: RecentScan, position: Int) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.activity_recent_menu_item_delete_recent_confirmation)
            .setMessage(null)
            .setPositiveButton(R.string.delete_button) { _, _ ->
                deleteRecentItem(position)
            }
            .setNegativeButton(R.string.cancel_button, null)
            .show()
    }

    private fun deleteRecentItem(position: Int) {
        (binding.recentScansRecycler.adapter as RecentScansAdapter).apply {
            ScannedResultsUtil.deleteResultsFromPref(
                this@RecentActivity,
                getItemPositionWithoutHeaders(position)
            )
            deleteItem(position)
            binding.txtNoScans.isVisible = itemCount == 0
            invalidateOptionsMenu()
        }
    }

    private fun deleteAllRecents() {
        (binding.recentScansRecycler.adapter as RecentScansAdapter).deleteAllItems()
        binding.txtNoScans.isVisible = true
        invalidateOptionsMenu()
        ScannedResultsUtil.deleteResultsFromPref(this)
    }

    private fun saveToCSV(barcodeList: MutableList<String>, typeList: MutableList<String>, dateList: MutableList<String>) {
        val fileName = "ScannedBarcode.csv"

        try {
            val cacheDir = File(this.cacheDir, "csv")
            cacheDir.mkdirs()
            val file = File(cacheDir, fileName)
            val fileWriter = FileWriter(file)

            if (barcodeList.size == typeList.size) {
                for (i in barcodeList.indices) {
                    fileWriter.append("${barcodeList[i]},${typeList[i]},${dateList[i]}")
                    fileWriter.append('\n')
                }
            } else {
                throw IllegalArgumentException("Lists must have the same size")
            }

            fileWriter.flush()
            fileWriter.close()

            shareCSV(file)
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error: CSV could not be saved.", Toast.LENGTH_SHORT)
                .show()
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            Toast.makeText(this, "Error: Lists must have the same size", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun shareCSV(file: File) {
        val uri = FileProvider.getUriForFile(
            this,
            this.applicationContext.packageName + ".provider",
            file
        )
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "text/csv"
        }
        startActivity(Intent.createChooser(sendIntent, "Share CSV"))
    }


    override fun onStartScanningClicked() {
        isBottomSheetDialogShown = false
    }
}
