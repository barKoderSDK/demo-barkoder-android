package com.barkoder.demoscanner

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.AnimationSet
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.MenuCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.barkoder.demoscanner.adapters.RecentScansAdapter
import com.barkoder.demoscanner.api.RetrofitIInstance
import com.barkoder.demoscanner.databinding.ActivityRecentBinding
import com.barkoder.demoscanner.fragments.NotConfiguredWebHookDialog
import com.barkoder.demoscanner.fragments.ResultBottomDialogFragment
import com.barkoder.demoscanner.models.BarcodeScanedData
import com.barkoder.demoscanner.models.RecentScan
import com.barkoder.demoscanner.models.RecentScan2
import com.barkoder.demoscanner.repositories.BarcodeDataRepository
import com.barkoder.demoscanner.utils.CommonUtil
import com.barkoder.demoscanner.utils.NetworkUtils
import com.barkoder.demoscanner.utils.ScannedResultsUtil
import com.barkoder.demoscanner.utils.getString
import com.barkoder.demoscanner.viewmodels.BarcodeDataViewModel
import com.barkoder.demoscanner.viewmodels.BarcodeDataViewModelFactory
import com.barkoder.demoscanner.viewmodels.RecentScanViewModel
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.lang.ref.WeakReference
import java.net.URLEncoder
import java.security.MessageDigest
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RecentActivity : AppCompatActivity(), RecentScansAdapter.OnRecentScanItemClickListener, ResultBottomDialogFragment.BottomSheetStateListener {

    private lateinit var binding: ActivityRecentBinding
    private val REQUEST_CODE = 1001
    private var isBottomSheetDialogShown = false
    private var order = false
    private var filterEnabled = false
    private var startDateFilter = ""
    private var endDateFilter = ""
    private lateinit var itemTouchHelper : ItemTouchHelper
    private lateinit var viewModel : RecentScanViewModel
    private lateinit var viewModelWebHook : BarcodeDataViewModel
    private lateinit var sharedPreferences : SharedPreferences
    private var sort = true
    private var checkOver21Dialog = false
    private var expiredIDDialog = false
    private lateinit var startDateEditText: TextInputEditText
    private lateinit var endDateEditText: TextInputEditText
    private var startDate: Date? = null
    private var endDate: Date? = null
    private var checkMode: Boolean = false
    private var selectedItems: Int = 0

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

        val handler = Handler()

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        viewModel = ViewModelProvider(this).get(RecentScanViewModel::class.java)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = ContextCompat.getColor(this, R.color.toolBarColor)
        }

        viewModel.readAllScans.observe(this, Observer { scanList ->
            if (scanList.isNullOrEmpty()) {
                binding.txtNoRecentScans.visibility = View.VISIBLE
                binding.btnOptions.visibility = View.GONE
            } else {
                binding.txtNoRecentScans.visibility = View.GONE
                binding.btnOptions.visibility = View.VISIBLE
            }
        })

        val layoutManager = LinearLayoutManager(this)
        binding.recentScansRecycler.layoutManager = layoutManager
        val recentScansAdapter = RecentScansAdapter(applicationContext, viewModel, WeakReference(this))
        binding.recentScansRecycler.adapter = recentScansAdapter
        if(sort) {
            recentScansAdapter.reverseOrderAscending()
        } else {
            recentScansAdapter.reverseOrderDescending()
        }
        val adapter = RecentScansAdapter(applicationContext,viewModel, WeakReference(this))

        viewModel.recentScansLiveData.observe(this, Observer { pagingData ->
            lifecycleScope.launch {
                recentScansAdapter.submitData(pagingData)
            }
        })

        binding.checkBoxAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // If the checkbox is checked, set all items to checked
                for (item in recentScansAdapter.recentScansAdapterData) {
                    item.checkedRecentItem = true
                    item.checkboxActive = true
                }
                val checkedCount = recentScansAdapter.recentScansAdapterData.count { it.checkedRecentItem }
                selectedItems = checkedCount
                binding.toolbar.title = "${selectedItems - 1} Selected"
            } else {
                // If the checkbox is unchecked, uncheck all items
                for (item in recentScansAdapter.recentScansAdapterData) {
                    item.checkedRecentItem = false
                    item.checkboxActive = false
                }
                val checkedCount = recentScansAdapter.recentScansAdapterData.count { it.checkedRecentItem }
                selectedItems = checkedCount
                binding.toolbar.title = "${selectedItems} Selected"
            }

            // Notify the existing adapter that the data has changed
            recentScansAdapter.notifyDataSetChanged()
        }
        binding.btnCloseFilter.setOnClickListener {
            binding.filterLayout.visibility = View.GONE
            (binding.recentScansRecycler.adapter as RecentScansAdapter).apply {
                viewModel.readAllScans.observeForever { recentScans ->
                    recentScans?.let {
                        lastDate = null
                        recentScansAdapterData.clear()

                            recentScansAdapterData.addAll(recentScans)

                        recentScansAdapterData.reverse()
                        val scannedDataList = recentScansAdapterData.toMutableList()
                        for (i in scannedDataList) {
                            val scanDate = i.scanDate
                            // Check if scanDate has at least 10 characters
                            if (scanDate.length >= 10) {
                                // Extract the substring safely and perform comparison
                                if (scanDate.substring(0, 10) != lastDate) {
                                    // Update the list with new item
                                    recentScansAdapterData.add(recentScansAdapterData.indexOf(i),
                                        RecentScan2(scanDate.substring(0, 10), i.scanText, i.scanTypeName,i.pictureBitmap, i.documentBitmap, i.signatureBitmap,  i.mainBitmap, i.thumbnailBitmap,i.formattedText,i.sadlImageRawBase64, asHeaderOnly = true))
                                    // Notify data set changed
                                    notifyDataSetChanged()
                                }
                            } else {
                                println("Warning: scanDate is shorter than expected: $scanDate")
                            }
                            lastDate = scanDate.substring(0, 10)
                        }

// Notify data set changed (this is no longer needed inside the loop if called after the loop)
                        notifyDataSetChanged()
                    }
                }

                }


        }

        sharedPreferences = this.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
        val isChecked = sharedPreferences.getBoolean("key_checked_all_recents", false)
        var listActive = sharedPreferences.getBoolean("key_listActive", false)

        binding.btnOptions.setOnClickListener {
            showCustomPopupMenu(it)
        }
        val repository = BarcodeDataRepository()
        val viewModelFactory = BarcodeDataViewModelFactory(repository)
        viewModelWebHook = ViewModelProvider(this, viewModelFactory).get(BarcodeDataViewModel::class.java)

    }


    @SuppressLint("MissingInflatedId")
    private fun showCustomPopupMenu(anchorView: View) {
        // Inflate custom layout for the popup
        val inflater = LayoutInflater.from(this)
        val popupView = inflater.inflate(R.layout.custom_menu_recent, null)

        // Create PopupWindow
        val popupWindow = PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        val sortButton: LinearLayout = popupView.findViewById(R.id.btn_sort)
        val filtterButton: LinearLayout = popupView.findViewById(R.id.btn_filter)
        val csvButton: LinearLayout = popupView.findViewById(R.id.btn_csv)
        val clearButton: LinearLayout = popupView.findViewById(R.id.btn_delete)

        sortButton.setOnClickListener {
            (binding.recentScansRecycler.adapter as RecentScansAdapter).apply {
                        sort = !sort
                if(sort) {
                    reverseOrderAscending()
                } else {
                    reverseOrderDescending()
                }

                    }
            popupWindow.dismiss()
                true
        }

        csvButton.setOnClickListener {

            val allUnchecked = viewModel.readAllScans.value?.all { !it.checkedRecentItem } ?: false

                if(viewModel.readAllScans.value!!.any { it.checkedRecentItem }) {
                    viewModel.readAllScans.value?.forEach{ scan ->
                        if(scan.checkedRecentItem) {
                            scannedBarcodesTypesList.add(scan.scanTypeName)
                            scannedBarcodesResultList.add(scan.scanText)
                            scannedBarcodesDataList.add(scan.scanDate)
                        }
                    }
                    saveToCSV(scannedBarcodesTypesList, scannedBarcodesResultList, scannedBarcodesDataList)
                    scannedBarcodesTypesList.clear()
                    scannedBarcodesResultList.clear()
                    scannedBarcodesDataList.clear()
                } else if(allUnchecked && checkMode) {
                  Toast.makeText(this, "Select Items", Toast.LENGTH_SHORT).show()
                }
                else {
                    if(!checkMode) {
                        viewModel.readAllScans.value?.forEach{ scan ->
                            scannedBarcodesTypesList.add(scan.scanTypeName)
                            scannedBarcodesResultList.add(scan.scanText)
                            scannedBarcodesDataList.add(scan.scanDate)
                        }
                    }
                    saveToCSV(scannedBarcodesTypesList, scannedBarcodesResultList, scannedBarcodesDataList)
                    scannedBarcodesTypesList.clear()
                    scannedBarcodesResultList.clear()
                    scannedBarcodesDataList.clear()
                    }

                true

            popupWindow.dismiss()
        }

        clearButton.setOnClickListener {
            if(checkMode) {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.activity_recent_menu_item_delete_recent_confirmation)
                    .setMessage(null)
                    .setPositiveButton(R.string.delete_button) { _, _ ->


                            val scans = viewModel.readAllScans.value
                            scans!!.forEach { scan ->
                                if (scan.checkedRecentItem) {
                                    viewModel.deleteRecentScan(scan)
                                }
                            }
                        }

                    .setNegativeButton(R.string.cancel_button, null)
                    .show()

            } else {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.activity_recent_menu_item_delete_recent_confirmation)
                    .setMessage(null)
                    .setPositiveButton(R.string.delete_button) { _, _ ->
                        viewModel.deleteAllRecentScans()
                    }
                    .setNegativeButton(R.string.cancel_button, null)
                    .show()
            }
            popupWindow.dismiss()

        }

        filtterButton.setOnClickListener {
            showFilterDialog()
            popupWindow.dismiss()
            true
        }
        // Show the popup window near the anchor view
        popupWindow.showAsDropDown(anchorView, 0, 0)
    }


    @SuppressLint("MissingInflatedId")
    private fun showCustomPopupMenuItem(anchorView: View, item: RecentScan2, dialog : Dialog) {
        // Inflate custom layout for the popup
        val inflater = LayoutInflater.from(this)
        val popupView = inflater.inflate(R.layout.custom_menu_item, null)

        // Create PopupWindow
        val popupWindow = PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        val removeButton: LinearLayout = popupView.findViewById(R.id.btn_remove)


        removeButton.setOnClickListener {
            showDeleteConfirmationDialog(item, dialog)
            popupWindow.dismiss()
            true
        }

        popupWindow.showAsDropDown(anchorView, 0, 0)
    }



    private fun showDeleteConfirmationDialog(item:RecentScan2, dialog2 : Dialog) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("This action permanently deletes selected scan log")
        builder.setPositiveButton("Delete") { dialog, _ ->
            // Handle the action for 'Yes' button
            viewModel.deleteRecentScan(item)
            dialog.dismiss() // Close the dialog
            dialog2.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            // Handle the action for 'No' button
            dialog.dismiss() // Close the dialog
        }

        // Create and show the dialog
        val alertDialog = builder.create()
        alertDialog.show()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {

//            R.id.menu_item_delete_recents -> {
//                MaterialAlertDialogBuilder(this)
//                    .setTitle(R.string.activity_recent_menu_item_delete_recents_confirmation)
//                    .setMessage(null)
//                    .setPositiveButton(R.string.activity_recent_menu_item_delete_recents) { _, _ ->
//                        deleteAllRecents()
//                    }
//                    .setNegativeButton(R.string.cancel_button, null)
//                    .show()
//                true
//            }
//            R.id.menu_item_sort -> {
//                    (binding.recentScansRecycler.adapter as RecentScansAdapter).apply {
//                        sort = !sort
//                        if(filterEnabled){
//                            reverseOrderFiltered()
//                        } else {
//                            if(sort) {
//                                reverseOrderAscending()
//                            } else {
//                                reverseOrderDescending()
//                            }
//
//                        }
//                    }
//                true
//
//            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun showStartDateRangePickerDialog() {
        val startDateCalendar = Calendar.getInstance() // Today's date as default

        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            startDateCalendar.set(Calendar.YEAR, year)
            startDateCalendar.set(Calendar.MONTH, monthOfYear)
            startDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            // Set time to 00:00:00
            startDateCalendar.set(Calendar.HOUR_OF_DAY, 0)
            startDateCalendar.set(Calendar.MINUTE, 0)
            startDateCalendar.set(Calendar.SECOND, 0)

            val formattedDate = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(startDateCalendar.time)
            startDateEditText.setText(formattedDate) // Set the selected date to EditText
            startDate = startDateCalendar.time // Save the selected date
        }

        // Create the DatePickerDialog
        val datePickerDialog = DatePickerDialog(
            this,
            dateSetListener,
            startDateCalendar.get(Calendar.YEAR),
            startDateCalendar.get(Calendar.MONTH),
            startDateCalendar.get(Calendar.DAY_OF_MONTH)
        )

        // Disable future dates by setting the maximum date to today
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

        // Show the DatePickerDialog
        datePickerDialog.show()
    }

    private fun showEndDateRangePickerDialog() {
        val endDateCalendar = Calendar.getInstance() // Today's date as default

        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            endDateCalendar.set(Calendar.YEAR, year)
            endDateCalendar.set(Calendar.MONTH, monthOfYear)
            endDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            // Set time to 23:59:59
            endDateCalendar.set(Calendar.HOUR_OF_DAY, 23)
            endDateCalendar.set(Calendar.MINUTE, 59)
            endDateCalendar.set(Calendar.SECOND, 59)

            val formattedDate = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(endDateCalendar.time)
            endDateEditText.setText(formattedDate) // Set the selected date to EditText
            endDate = endDateCalendar.time // Save the selected date
        }

        // Create the DatePickerDialog
        val datePickerDialog = DatePickerDialog(
            this,
            dateSetListener,
            endDateCalendar.get(Calendar.YEAR),
            endDateCalendar.get(Calendar.MONTH),
            endDateCalendar.get(Calendar.DAY_OF_MONTH)
        )

        // Disable future dates by setting the maximum date to today
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

        // Show the DatePickerDialog
        datePickerDialog.show()
    }


        // Ensure adapter is RecentScansAdapter and perform reverseOrder()



    private fun formatDate(calendar: Calendar): String {
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
        return "$dayOfMonth $month"
    }

    override fun onRecentScanItemClick(item: RecentScan2) {
        if(checkMode) {
            (binding.recentScansRecycler.adapter as RecentScansAdapter).apply {
                item.checkedRecentItem = !item.checkedRecentItem
                notifyDataSetChanged()
                val checkedCount = recentScansAdapterData.count { it.checkedRecentItem }
                selectedItems = checkedCount
            }
            binding.toolbar.title = "${selectedItems} Selected"
        } else {
            if(item.scanTypeName == "MRZ") {
                showFullScreenDialog(item.pictureBitmap,item.documentBitmap,item.signatureBitmap, item.mainBitmap, item.scanText, item)
            } else {
                showBarcodeDetailsDialog(item.thumbnailBitmap, item.scanText, item.scanTypeName, item.formattedText, item, item.scannedTimesInARow, item.sadlImageRawBase64)
            }
        }
    }

    override fun onRecentScanItemLongClick(item: RecentScan2, position: Int) {
        checkMode = !checkMode



        (binding.recentScansRecycler.adapter as RecentScansAdapter).apply {
            switchCheckMode()
            item.checkedRecentItem = true
            notifyDataSetChanged()
            val checkedCount = recentScansAdapterData.count { it.checkedRecentItem }
            selectedItems = checkedCount
        }

        if(checkMode) {
            binding.toolbar.title = "${selectedItems} Selected"
            binding.checkBoxAll.visibility = View.VISIBLE
        } else {
            binding.toolbar.title = "Recent Scans"
            binding.checkBoxAll.visibility = View.GONE
        }

        val recentScansAdapter = RecentScansAdapter(applicationContext, viewModel, WeakReference(this))
        if(!checkMode) {
            for (item in recentScansAdapter.recentScansAdapterData) {
                item.checkedRecentItem = false
                item.checkboxActive = false
            }
        }
    }

    private fun deleteRecentItem(position: Int) {
        (binding.recentScansRecycler.adapter as RecentScansAdapter).apply {
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

    private fun extractEndpointFromUrl(url: String): String {
        val trimmedUrl = url.trimEnd('/')
        val uriParts = trimmedUrl.split('/')
        return uriParts.last()
    }
    private fun materialDialogError(title : String, message : String) {
        MaterialAlertDialogBuilder(this@RecentActivity)
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton("Continue") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    private fun generate10BitTimestamp(): String {
        val currentTimestamp = System.currentTimeMillis()
        val timestamp10Bit = currentTimestamp / 1000
        return timestamp10Bit.toString()
    }
    private fun encodeStringToBase64(stringToEncode: String): String {
        val data = stringToEncode.toByteArray(Charsets.UTF_8)
        return Base64.encodeToString(data, Base64.DEFAULT)
    }

    private fun generateMD5Hash(timestamp: String, secretWord: String): String {
        val input = timestamp + secretWord
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun sendToWebHook() {
        val sharedPreferences = this.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
        val keyWebHook =
            sharedPreferences.getString(getString(R.string.key_secret_word_webhook), "")

        val urlWebHook = sharedPreferences.getString(getString(R.string.key_url_webhook), "")
        var endPointUrl = extractEndpointFromUrl(urlWebHook!!)
        val uri = Uri.parse(urlWebHook)
        val baseUrl = "${uri.scheme}://${uri.host}/"
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        var webHookEncodeData = prefs.getBoolean(getString(R.string.key_webhook_encode_data), false)
        val webHookFeedBack = prefs.getBoolean(getString(R.string.key_webhook_feedback), false)
        if (viewModel.readAllScans.value!!.any { it.checkedRecentItem }) {
            viewModel.readAllScans.value?.forEach { scan ->
                if (scan.checkedRecentItem) {
                    scannedBarcodesTypesList.add(scan.scanTypeName)
                    scannedBarcodesResultList.add(scan.scanText)
                    scannedBarcodesDataList.add(scan.scanDate)
                }
            }
            lifecycleScope.launch {
                if (urlWebHook.isNullOrBlank()) {
                    var notConfiguredWebHookDialog = NotConfiguredWebHookDialog()
                    notConfiguredWebHookDialog.show(
                        supportFragmentManager,
                        "NotConfiguredWebHookDialog"
                    )

                } else {
                    if (!NetworkUtils.isInternetAvailable(this@RecentActivity)) {
                        materialDialogError(
                            getString(R.string.material_dialog_server_eror_title),
                            getString(R.string.material_dialog_network_error)
                        )
                    } else {
                        RetrofitIInstance.rebuild(baseUrl)
                        val secretWord = keyWebHook
                        val timestamp = generate10BitTimestamp()
                        val securityHash = generateMD5Hash(timestamp, secretWord!!)
                        val jsonArray = ArrayList<Map<String, String>>()
                        if (scannedBarcodesResultList.size == scannedBarcodesTypesList.size) {

                            for (i in 0 until scannedBarcodesResultList.size) {
                                val result = scannedBarcodesResultList[i]
                                val symbology = scannedBarcodesTypesList[i]
                                val encodedResult = encodeStringToBase64(result)
                                val encodedSymbology = encodeStringToBase64(symbology)
                                val jsonData = mapOf(
                                    getString(R.string.webhook_symobology_title) to if (webHookEncodeData) encodedSymbology else symbology,
                                    getString(R.string.webhook_value_title) to if (webHookEncodeData) encodedResult else result,
                                    getString(R.string.webhook_date_title) to timestamp,
                                    "encoded" to if (webHookEncodeData) "true" else "false"
                                )
                                jsonArray.add(jsonData)
                            }
                        }
                        val barcodeData = BarcodeScanedData(timestamp, securityHash, jsonArray)

//                        viewModelWebHook.createPost(endPointUrl, barcodeData)

                        viewModelWebHook.barcodeDataResponse.observe(
                            this@RecentActivity,
                            Observer { response ->
                                if (response.isSuccessful) {
                                    Toast.makeText(
                                        this@RecentActivity,
                                        "You data was sent to endpoint",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    if (webHookFeedBack) {
                                        materialDialogError(
                                            "Server error",
                                            "Response status code was unacceptable: ${
                                                response.code().toString()
                                            }."
                                        )
                                    }
                                }
                            })
                    }
                }
            }

            scannedBarcodesTypesList.clear()
            scannedBarcodesResultList.clear()
            scannedBarcodesDataList.clear()
        } else {
            Toast.makeText(this, "Select items", Toast.LENGTH_SHORT).show()
        }
    }

    fun showBarcodeDetailsDialog(barcodePicture: String?, barcodeValue: String, barcodeType: String, formattedTextValue: String, item: RecentScan2, scannedTimes: Int, sadlImageRawBase64 : String) {
        runOnUiThread {
            // Create a regular Dialog for more control
            val dialog = Dialog(this, com.barkoder.R.style.FullScreenDialogStyle)

            // Inflate the custom layout
            val inflater = LayoutInflater.from(this)
            val dialogView = inflater.inflate(R.layout.custom_dialog_barcode_result, null)

            // Set the content of the dialog
            dialog.setContentView(dialogView)

            val barcodeValueText = dialogView.findViewById<TextView>(R.id.barcodeValueText)
            val barcodeTypeText = dialogView.findViewById<TextView>(R.id.barcodeTypeText)
            val barcodeBitmap = dialogView.findViewById<ImageView>(R.id.barcodeImage)
            val txtSearch = dialogView.findViewById<TextView>(R.id.txtSearch)
            val formattedText = dialogView.findViewById<TextView>(R.id.FormattedValueText)
            val formattedLayout = dialogView.findViewById<LinearLayout>(R.id.formattedTextLayout)
            val scannedTimesLayout = dialogView.findViewById<LinearLayout>(R.id.timesScannedLayout)
            val scannedTimesText = dialogView.findViewById<TextView>(R.id.timesScannedText)
            val sadlImage = dialogView.findViewById<ImageView>(R.id.sadlImage)
            val textCapturedMedia = dialogView.findViewById<TextView>(R.id.textCapturedMedia)
            val sadlImagesLayout = dialogView.findViewById<LinearLayout>(R.id.sadlImagesLayout)

            if(scannedTimes > 1) {
                scannedTimesLayout.visibility = View.VISIBLE
            } else {
                scannedTimesLayout.visibility = View.GONE
            }

            if (sadlImageRawBase64 != null && sadlImageRawBase64.length > 1) {
                try {
                    val grayscalePixels: ByteArray = Base64.decode(sadlImageRawBase64, Base64.NO_WRAP)
                    Log.d("DecodeDebug", "Decoded bytes: " + grayscalePixels.size)

                    val width = 200
                    val height = 250


                    // OPTION 1: Try ARGB_8888 with grayscale values
                    val argbPixels = IntArray(width * height)
                    for (i in grayscalePixels.indices) {
                        val gray = grayscalePixels[i].toInt() and 0xFF // Convert to unsigned
                        argbPixels[i] = -0x1000000 or (gray shl 16) or (gray shl 8) or gray
                    }

                    val grayscaleBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    grayscaleBitmap.setPixels(argbPixels, 0, width, 0, 0, width, height)
                    sadlImage.setImageBitmap(grayscaleBitmap)
                }  catch (e: Exception) {
                    Log.e("ImageError", "Failed to decode grayscale pixels", e)
                }
            } else {
                textCapturedMedia.visibility = View.GONE
                sadlImagesLayout.visibility = View.GONE
            }

            if(formattedTextValue.length > 0) {
                formattedLayout.visibility = View.VISIBLE
            } else {
                formattedLayout.visibility = View.GONE
            }

            // Load the image using Glide
            Glide.with(this@RecentActivity)
                .load(barcodePicture?.let { File(it) })
                .placeholder(R.drawable.container__2_)
                .into(barcodeBitmap)

            val cleanedResult = CommonUtil.cleanResultString(barcodeValue)

            barcodeValueText.text = cleanedResult
            barcodeTypeText.text = barcodeType
            formattedText.text = formattedTextValue
            scannedTimesText.text = scannedTimes.toString()


            val btnCopy = dialogView.findViewById<MaterialButton>(R.id.btnCopy)
            val btnSearch = dialogView.findViewById<MaterialButton>(R.id.btnSearch)
            val btnOptions = dialogView.findViewById<ImageButton>(R.id.btn_optionss)
            val rowsLayout = dialogView.findViewById<LinearLayout>(R.id.rowsLayout)
            btnOptions.visibility = View.VISIBLE

            btnOptions.setOnClickListener {
                showCustomPopupMenuItem(it, item, dialog)
            }

            if (formattedTextValue.isNotEmpty()) {
                formattedTextValue.lines().forEach { line ->
                    val parts = line.split(":", limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0].trim()
                        val value = parts[1].trim()

                        // Create parent horizontal LinearLayout
                        val rowLayout = LinearLayout(applicationContext).apply {
                            orientation = LinearLayout.HORIZONTAL
                            setBackgroundColor(Color.WHITE)
                            setPadding(15, 15, 15, 15)

                            val params = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            params.topMargin = 2
                            layoutParams = params
                        }

                        // Key TextView (left side)
                        val keyView = TextView(applicationContext).apply {
                            text = key
                            setTextColor(Color.parseColor("#666666"))
                            textSize = 14f
                            setPadding(15, 20, 15, 20)
                        }

                        // Value TextView (right side)
                        val valueView = TextView(applicationContext).apply {
                            text = value
                            setTextColor(Color.parseColor("#000000"))
                            textSize = 14f
                            gravity = Gravity.END
                            setPadding(15, 20, 15, 20)
                            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        }
                        formattedLayout.visibility = View.GONE
                        // Add views
                        rowLayout.addView(keyView)
                        rowLayout.addView(valueView)
                        rowsLayout.addView(rowLayout)

                        // Divider line
                        val divider = View(applicationContext).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, 2
                            )
                            setBackgroundColor(Color.parseColor("#FFF0EF"))
                        }
                        rowsLayout.addView(divider)
                    }
                }
            }

            var bitmapsArray = arrayListOf<Bitmap>()

            if (barcodePicture != null) {
               CommonUtil.getBitmapFromInternalStorage(barcodePicture)?.let { bitmapsArray.add(it) }
            }

//            btnPDF.setOnClickListener {
//                CommonUtil.createPdf(this, bitmapsArray, "${barcodeType} \n \n ${barcodeValue}")
//            }

            updateSearchEngineOnBarcodeDetails(btnSearch, barcodeValue)
            if(CommonUtil.isTextURL(barcodeValue)) {
                btnSearch.setIconResource(R.drawable.ico_webhook) // Replace with your new icon
                txtSearch.text = "Open"
            }
            btnCopy.setOnClickListener {
                CommonUtil.copyBarcodeResultText(this, barcodeValue)
                Toast.makeText(this, "Values was copied to clipboard!", Toast.LENGTH_SHORT).show()
            }



            // Close button functionality
            val closeButton = dialogView.findViewById<ImageButton>(R.id.buttonClose)
            val closeBackButton = dialogView.findViewById<ImageButton>(R.id.buttonCloseBack)

            closeButton.visibility = View.GONE
            closeBackButton.visibility = View.VISIBLE
            closeButton.setOnClickListener { dialog.dismiss() }
            closeBackButton.setOnClickListener { dialog.dismiss() }

            // Ensure full-screen layout when the dialog is shown
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
    fun showFilterDialog() {
        runOnUiThread {
            // or use `this` if in an Activity
            val builder =
                android.app.AlertDialog.Builder(this, com.barkoder.R.style.FullScreenDialogStyle)
            // Inflate the custom layout
            val inflater = LayoutInflater.from(this)
            val dialogView = inflater.inflate(R.layout.custom_dialog_filter, null)


            startDateEditText = dialogView.findViewById(R.id.startDate_text)
            endDateEditText = dialogView.findViewById(R.id.endDate_text)
            var btnSave = dialogView.findViewById<Button>(R.id.buttonSave)

            startDateEditText.setOnClickListener {
                showStartDateRangePickerDialog()
            }
            endDateEditText.setOnClickListener {
                showEndDateRangePickerDialog()
            }

            val closeButton = dialogView.findViewById<ImageButton>(R.id.buttonClose)

            builder.setView(dialogView)
            builder.setCancelable(true)

            val dialog = builder.create()

            closeButton.setOnClickListener { dialog.dismiss() }


            btnSave.setOnClickListener {
                if(startDateEditText.text!!.isEmpty() || endDateEditText.text!!.isEmpty()) {
                    Toast.makeText(this,"select the start and end dates", Toast.LENGTH_SHORT).show()
                } else {
                    (binding.recentScansRecycler.adapter as RecentScansAdapter).apply {
                        filterEnabled = true
                        filterByDateRange(startDate!!,endDate!!)
                        if(sort) {
                            reverseOrderAscending()
                        } else {
                            reverseOrderDescending()
                        }
                        binding.filterLayout.visibility = View.VISIBLE
                        binding.txtDateFrom.text = startDateEditText.text
                        binding.txtDateTo.text = endDateEditText.text
                    }

                    dialog.dismiss()
                }

            }
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


    @SuppressLint("MissingInflatedId")
    fun showFullScreenDialog(pictureBitmap: String?, documentBitmap : String?, signatureBitmap : String?, mainBitmap : String?, results : String, item: RecentScan2) {
        runOnUiThread {
            // or use `this` if in an Activity
            val builder =
                android.app.AlertDialog.Builder(this, com.barkoder.R.style.FullScreenDialogStyle)
            // Inflate the custom layout
            val inflater = LayoutInflater.from(this)
            val dialogView = inflater.inflate(R.layout.custom_dialog_results, null)

            // Find the ImageView and set the bitmap image
            val dialogImageView =
                dialogView.findViewById<ImageView>(R.id.imageViewDialog)
            val firstNameUser = dialogView.findViewById<TextView>(R.id.firstNameUser)
            val dateOfBirthUser = dialogView.findViewById<TextView>(R.id.dateOfBirthUser)
            val issuingCountry = dialogView.findViewById<TextView>(R.id.issuingCountry)
            val genderUser = dialogView.findViewById<TextView>(R.id.genderUser)
            val titleText = dialogView.findViewById<TextView>(R.id.titleMRZ)
            val expirationDateUser =
                dialogView.findViewById<TextView>(R.id.expirationDateUser)
            val nationalityUser = dialogView.findViewById<TextView>(R.id.nationalityUser)
            val documentNumberUser =
                dialogView.findViewById<TextView>(R.id.documentNumberUser)
            val documentTypeUser = dialogView.findViewById<TextView>(R.id.documentType)
            val imageDocument =
                dialogView.findViewById<ImageView>(R.id.imageDocument)
            val imagePicture =
                dialogView.findViewById<ImageView>(R.id.imagePicture)
            val imageSignature =
                dialogView.findViewById<ImageView>(R.id.imageSignature)
            val imageMain =
                dialogView.findViewById<ImageView>(R.id.imageMain)
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
            val lines = results.split("\n".toRegex())?.dropLastWhile { it.isEmpty() }
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




//            compareDates(expirationDate!!, textVerificationExpire, iconVerificationExpire, this)
//            checkOver21(dateOfBirth!!, iconVerificationOver21, textVerificationOver21, this)


            var bitmapsArray = arrayListOf<Bitmap>()

            fullName = "$firstName $lastName"
            firstNameUser.text = firstName + " " + lastName
            dateOfBirthUser.text = formattedDateBirth
            expirationDateUser.text = formattedDateExpiry
            nationalityUser.text = nationality
            documentNumberUser.text = documentNumber
            documentTypeUser.text = documentType
            issuingCountry.text = issuing_country
            genderUser.text = gender_user
            titleText.text = documentType

             viewCardPicture.setOnClickListener {
                    if(pictureBitmap != null) {
                        showFullScreenImage(pictureBitmap)
                    }
             }

            viewCardMain.setOnClickListener {
                if(mainBitmap != null) {
                    showFullScreenImage(mainBitmap)
                }
            }

            viewCardSignature.setOnClickListener {
                if(signatureBitmap != null) {
                    showFullScreenImage(signatureBitmap)
                }
            }

            viewCardDocument.setOnClickListener {
                if(documentBitmap != null) {
                    showFullScreenImage(documentBitmap)
                }
            }

            if (pictureBitmap != null) {
                CommonUtil.getBitmapFromInternalStorage(pictureBitmap)?.let { bitmapsArray.add(it) }
                Glide.with(this)
                    .load(File(pictureBitmap))
                    .into(dialogImageView)
                viewCardPicture.visibility = View.VISIBLE
                Glide.with(this)
                    .load(File(pictureBitmap))
                    .into(imagePicture)
            } else {
                viewCardPicture.visibility = View.GONE
            }

            if (documentBitmap != null) {
                CommonUtil.getBitmapFromInternalStorage(documentBitmap)?.let { bitmapsArray.add(it) }
                viewCardDocument.visibility = View.VISIBLE
                Glide.with(this)
                    .load(File(documentBitmap))
                    .into(imageDocument)
            } else {
                viewCardDocument.visibility = View.GONE
            }

            if (signatureBitmap != null) {
                CommonUtil.getBitmapFromInternalStorage(signatureBitmap)?.let { bitmapsArray.add(it) }
                viewCardSignature.visibility = View.VISIBLE
                Glide.with(this)
                    .load(File(signatureBitmap))
                    .into(imageSignature)
            } else {
                viewCardSignature.visibility = View.GONE
            }

            if (mainBitmap != null) {
                CommonUtil.getBitmapFromInternalStorage(mainBitmap)?.let { bitmapsArray.add(it) }
                viewCardMain.visibility = View.VISIBLE
                Glide.with(this)
                    .load(File(mainBitmap))
                    .into(imageMain)
            } else {
                viewCardMain.visibility = View.GONE
            }


            val btnCopy = dialogView.findViewById<MaterialButton>(R.id.btnCopy)
            val btnSearch = dialogView.findViewById<MaterialButton>(R.id.btnSearch)
            val btnPDF = dialogView.findViewById<MaterialButton>(R.id.btnPDF)
            val btnOption = dialogView.findViewById<ImageButton>(R.id.btn_optionss)


//            btnPDF.setOnClickListener {
//                CommonUtil.createPdf(this, bitmapsArray, "Full Name: $fullName\nNationality: $nationality\nDate of birth: $dateOfBirth\nDocument Number: $documentNumber\nIssuing country: $issuing_country\nDate of expiry $expirationDate")
//            }

            updateSearchEngineOnBarcodeDetails(btnSearch, "$fullName")

            btnCopy.setOnClickListener {
                CommonUtil.copyBarcodeResultText(this, results)
                Toast.makeText(this, "Values was copied to clipboard!", Toast.LENGTH_SHORT).show()
            }


            val closeButton = dialogView.findViewById<ImageButton>(R.id.buttonClose)
            val closeBackButton = dialogView.findViewById<ImageButton>(R.id.buttonCloseBack)

            closeButton.visibility = View.GONE
            closeBackButton.visibility = View.VISIBLE
            builder.setView(dialogView)
            builder.setCancelable(true)

            val dialog = builder.create()

            closeButton.setOnClickListener { dialog.dismiss() }
            closeBackButton.setOnClickListener { dialog.dismiss() }
            btnOption.visibility = View.VISIBLE
            btnOption.setOnClickListener {
                showCustomPopupMenuItem(it, item, dialog)
            }

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


    private fun checkOver21(
        birthDate: String,
        iconVerificationOver21: ImageView,
        textVerificatonOver21: TextView,
        context: Context
    ) {
        // Define the date format

        var formatter: DateTimeFormatter? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val dateOfBirthStr = birthDate

            // Convert the date of birth string to LocalDate
            val dateOfBirth = LocalDate.parse(dateOfBirthStr, formatter)

            // Get the current date
            val currentDate = LocalDate.now()

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

    fun showFullScreenImage(string: String) {
        // Create a Dialog to display the image
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_fullscreen_image)  // Inflate the dialog layout

        // Find the ImageView in the dialog layout and set the image from the clicked ImageView
        val fullScreenImageView = dialog.findViewById<ImageView>(R.id.fullScreenImageView)

        // If you are loading the image from a file or URL, use Glide or other image-loading libraries
        // For demonstration, we'll just use the ImageView's drawable.
        Glide.with(this)
            .load(File(string))
            .into(fullScreenImageView)

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
