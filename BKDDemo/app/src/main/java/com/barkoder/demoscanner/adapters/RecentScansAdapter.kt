package com.barkoder.demoscanner.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.barkoder.demoscanner.R
import com.barkoder.demoscanner.RecentActivity
import com.barkoder.demoscanner.models.RecentScan
import com.barkoder.demoscanner.models.RecentScan2
import com.barkoder.demoscanner.utils.CommonUtil
import com.barkoder.demoscanner.utils.ResultItem
import com.barkoder.demoscanner.utils.ScannedResultsUtil
import com.barkoder.demoscanner.viewmodels.RecentScanViewModel
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.lang.ref.WeakReference
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecentScansAdapter(
    private val context: Context,
    private val viewModel: RecentScanViewModel,
    private val recentScanItemClickListenerRef: WeakReference<OnRecentScanItemClickListener>?
) : PagingDataAdapter<RecentScan2, RecyclerView.ViewHolder>(DIFF_CALLBACK) {
    interface OnRecentScanItemClickListener {
        fun onRecentScanItemClick(item: RecentScan2)
        fun onRecentScanItemLongClick(item: RecentScan2, position: Int)
    }

    private val HEADER_ITEM_VIEW = 0
    private val ROW_ITEM_VIEW = 1
    private var filter = false
    private var anyCheckBoxActive: Boolean = false
    var lastDate: String? = null
    var recentScansAdapterData: MutableList<RecentScan2> = mutableListOf()
    var filteredRecentScans: MutableList<RecentScan2> = mutableListOf()
    private var startDateFilter : Date? = null
    private var endDateFilter : Date? = null
    private var checkModeAdapter : Boolean = false

    init {

        val dateFormat = SimpleDateFormat("yyyy/MM/dd/HH:mm:ss", Locale.getDefault())

        viewModel.readAllScans.observeForever { recentScans ->
            recentScans?.let {

                val dateFormat = SimpleDateFormat("yyyy/MM/dd/HH:mm:ss.SSS", Locale.getDefault())

                val sortedScans = recentScans.sortedBy { scan ->
                    dateFormat.parse(scan.scanDate) // Convert scanDate string to Date for sorting
                }

                recentScansAdapterData.clear()
                recentScansAdapterData.addAll(sortedScans)
                filteredRecentScans.clear()

                val scannedDataList = recentScansAdapterData.toMutableList()
                for (i in scannedDataList) {
//                    val scanDate2 = dateFormat.parse(i.scanDate)
                    val scanDate = i.scanDate
                    // Check if scanDate has at least 10 characters
                    if (scanDate.length >= 10) {
                        // Extract the substring safely and perform comparison
                        if (scanDate.substring(0, 10) != lastDate) {

                            recentScansAdapterData.add(recentScansAdapterData.indexOf(i),
                                RecentScan2(scanDate.substring(0, 10), i.scanText, i.scanTypeName, i.pictureBitmap, i.documentBitmap, i.signatureBitmap, i.mainBitmap,i.thumbnailBitmap,i.formattedText,i.formattedJsonText,i.sadlImageRawBase64, asHeaderOnly = true))
                            // Notify data set changed
                            notifyDataSetChanged()
                        }
                    } else {
                        println("Warning: scanDate is shorter than expected: $scanDate")
                    }
                    lastDate = scanDate.substring(0, 10)
                }
                notifyDataSetChanged()
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<RecentScan2>() {
            override fun areItemsTheSame(oldItem: RecentScan2, newItem: RecentScan2): Boolean {
                return oldItem.id == newItem.id // or any unique identifier
            }

            override fun areContentsTheSame(oldItem: RecentScan2, newItem: RecentScan2): Boolean {
                return oldItem == newItem
            }
        }
    }

     fun resetResultList() {
                recentScansAdapterData.clear()
         viewModel.readAllScans.observeForever { recentScans ->
             recentScans?.let {
                 recentScans.forEach { item ->
                     recentScansAdapterData.add(item)
                     notifyDataSetChanged()
                 }
             }
         }
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateHeader: TextView = view.findViewById(R.id.txtRecentHeader)
    }

    class RowViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val barcodeImage: ImageView = view.findViewById(R.id.imgBarcode)
        val barcodeResult: TextView = view.findViewById(R.id.txtRowBarcodeResult)
        val barcodeType: TextView = view.findViewById(R.id.txtRowBarcodeType)
        val checkBoxItem: CheckBox = view.findViewById(R.id.checkBoxRecentItem)
        val imageInfo: ImageView = view.findViewById(R.id.imageInfo)
        val scannedCountText: TextView = view.findViewById(R.id.countScannedText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            HEADER_ITEM_VIEW -> HeaderViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.recent_scan_header, parent, false)
            )

            else -> RowViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.recent_scan_row, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = recentScansAdapterData[position]

        if (getItemViewType(position) == ROW_ITEM_VIEW) {
            val isFirst = isFirstInGroup(position)
            val isLast = isLastInGroup(position)

            val backgroundRes = when {
                isFirst && isLast -> R.drawable.bg_rounded_recents_all
                isFirst -> R.drawable.bg_rounded_recents_top
                isLast -> R.drawable.bg_rounded_recents_bottom
                else -> R.drawable.not_rounded_background
            }

            holder.itemView.background = ContextCompat.getDrawable(context, backgroundRes)
        }

        if (getItemViewType(position) == HEADER_ITEM_VIEW) {
            (holder as HeaderViewHolder).dateHeader.text = currentItem.scanDate
        } else {
            (holder as RowViewHolder).apply {
                if (currentItem.scannedTimesInARow > 1) {
                    scannedCountText.text = "(${currentItem.scannedTimesInARow})"
                    scannedCountText.visibility = View.VISIBLE
                } else {
                    scannedCountText.text = ""
                    scannedCountText.visibility = View.GONE
                }

                // Handle image loading based on scan type
                if (currentItem.scanTypeName == "MRZ") {
                    if (currentItem.pictureBitmap != null) {
                        Glide.with(context)
                            .load(File(currentItem.pictureBitmap))
                            .into(barcodeImage)
                    } else {
                        Glide.with(context)
                            .load(R.drawable.container)
                            .into(barcodeImage)
                    }
                } else {
                    Glide.with(context)
                        .load(currentItem.thumbnailBitmap?.let { File(it) })
                        .placeholder(R.drawable.container__2_)
                        .into(barcodeImage)
                }

                // Set the barcode result text based on scan type
                barcodeResult.text = if (currentItem.scanTypeName == "MRZ") {
                    extractDocumentRawTextinRecents(currentItem.scanText)
                } else {
                    val cleanedResult = CommonUtil.cleanResultString(currentItem.scanText)
                  cleanedResult;
                }

                // Toggle visibility based on check mode
                val visibility = if (checkModeAdapter) View.VISIBLE else View.GONE
                checkBoxItem.visibility = visibility
                imageInfo.visibility = if (checkModeAdapter) View.INVISIBLE else View.VISIBLE

                barcodeType.text = currentItem.scanTypeName
                checkBoxItem.isChecked = currentItem.checkedRecentItem
            }

            // Set click listeners
            holder.itemView.setOnClickListener {
                recentScanItemClickListenerRef?.get()?.onRecentScanItemClick(currentItem)
            }
            holder.itemView.setOnLongClickListener {
                recentScanItemClickListenerRef?.get()?.onRecentScanItemLongClick(currentItem, position)
                true
            }
        }
    }

    override fun getItemCount(): Int {
            return recentScansAdapterData.size

    }

    override fun getItemViewType(position: Int): Int {
        return if (recentScansAdapterData[position].asHeaderOnly) {
            HEADER_ITEM_VIEW
        } else
            ROW_ITEM_VIEW

    }


    fun reverseOrderAscending() {
        viewModel.readAllScans.observeForever { recentScans ->
            recentScans?.let {
                lastDate = null
                recentScansAdapterData.clear()

                if(filter) {
                    for(i in recentScans) {
                        val dateFormat =
                            SimpleDateFormat("yyyy/MM/dd/HH:mm:ss", Locale.getDefault())
                        val scanDate = dateFormat.parse(i.scanDate)
                        // Ensure scanDate is not null before comparison
                        if (scanDate != null) {
                            if (scanDate.after(startDateFilter) && scanDate.before(endDateFilter)) {
                                recentScansAdapterData.add(i)
                                notifyDataSetChanged()
                            }
                        }
                    }
                } else {
                    val dateFormat = SimpleDateFormat("yyyy/MM/dd/HH:mm:ss.SSS", Locale.getDefault())
                    val sortedScans = recentScans.sortedBy { scan ->
                        dateFormat.parse(scan.scanDate) // Convert scanDate string to Date for sorting
                    }

                    recentScansAdapterData.addAll(sortedScans)
                }
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
                                RecentScan2(scanDate.substring(0, 10), i.scanText, i.scanTypeName,i.pictureBitmap, i.documentBitmap, i.signatureBitmap,  i.mainBitmap, i.thumbnailBitmap,i.formattedText,i.formattedJsonText,i.sadlImageRawBase64, asHeaderOnly = true))
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

    fun reverseOrderDescending() {
        viewModel.readAllScans.observeForever { recentScans ->
            recentScans?.let {
                lastDate = null
                recentScansAdapterData.clear()
                if(filter) {
                    for(i in recentScans) {
                        val dateFormat =
                            SimpleDateFormat("yyyy/MM/dd/HH:mm:ss", Locale.getDefault())
                        val scanDate = dateFormat.parse(i.scanDate)
                        // Ensure scanDate is not null before comparison
                        if (scanDate != null) {
                            if (scanDate.after(startDateFilter) && scanDate.before(endDateFilter)) {
                                recentScansAdapterData.add(i)

// Notify data set changed (this is no longer needed inside the loop if called after the loop)
                                notifyDataSetChanged()
                            }
                        }
                    }
                } else {
                    val dateFormat = SimpleDateFormat("yyyy/MM/dd/HH:mm:ss.SSS", Locale.getDefault())
                    val sortedScans = recentScans.sortedBy { scan ->
                        dateFormat.parse(scan.scanDate) // Convert scanDate string to Date for sorting
                    }

                    recentScansAdapterData.addAll(sortedScans)
                }

                val scannedDataList = recentScansAdapterData.toMutableList()

                for (i in scannedDataList) {
                    val scanDate = i.scanDate
                    // Check if scanDate has at least 10 characters
                    if (scanDate.length >= 10) {
                        // Extract the substring safely and perform comparison
                        if (scanDate.substring(0, 10) != lastDate) {
                            // Update the list with new item
                            recentScansAdapterData.add(recentScansAdapterData.indexOf(i),
                                RecentScan2(scanDate.substring(0, 10), i.scanText, i.scanTypeName, i.pictureBitmap, i.documentBitmap, i.signatureBitmap, i.mainBitmap,i.thumbnailBitmap,i.formattedText,i.formattedJsonText,i.sadlImageRawBase64, asHeaderOnly = true))
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


    fun deleteAllItems() {
        recentScansAdapterData.clear()
        notifyDataSetChanged()
    }

        fun deleteItem(itemPosition: Int) {
        if (getItemViewType(itemPosition - 1) == HEADER_ITEM_VIEW) {
            val deleteHeader = deleteHeaderForItemOnPosition(itemPosition)
            if (deleteHeader) {
                val headerPosition = itemPosition - 1
                viewModel.deleteRecentScan(recentScansAdapterData[itemPosition])
                notifyItemRangeRemoved(headerPosition, 2)
                notifyItemRangeChanged(headerPosition, itemCount - headerPosition)
            } else {
                deleteOnlyItemOnPositionAndNotifyAdapter(itemPosition)
            }
        } else {
            deleteOnlyItemOnPositionAndNotifyAdapter(itemPosition)
        }
    }

    private fun deleteOnlyItemOnPositionAndNotifyAdapter(position: Int) {
        viewModel.deleteRecentScan(recentScansAdapterData[position])
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, itemCount - position)
    }

    private fun deleteHeaderForItemOnPosition(position: Int): Boolean {
        return position >= itemCount - 1 || recentScansAdapterData[position + 1].asHeaderOnly
    }

    public fun filterByDateRange(startDate: Date?, endDate: Date?) {
        val dateFormat = SimpleDateFormat("yyyy/MM/dd/HH:mm:ss", Locale.getDefault())
        if (startDate == null && endDate == null) return

        startDateFilter = startDate
        endDateFilter = endDate

        recentScansAdapterData.clear() // Clear current data set


        viewModel.readAllScans.observeForever { recentScans ->
            recentScans?.let {
                recentScans.forEach { item ->
                    try {
                        val scannedDataList = recentScansAdapterData.toMutableList()
                        for (i in scannedDataList) {
                            val scanDate = i.scanDate
                            // Check if scanDate has at least 10 characters
                            if (scanDate.length >= 10) {
                                // Extract the substring safely and perform comparison
                                if (scanDate.substring(0, 10) != lastDate) {
                                    // Update the list with new item
                                    recentScansAdapterData.add(recentScansAdapterData.indexOf(i),
                                        RecentScan2(scanDate.substring(0, 10), i.scanText, i.scanTypeName, i.pictureBitmap, i.documentBitmap, i.signatureBitmap, i.mainBitmap,i.thumbnailBitmap,i.formattedText,i.formattedJsonText,i.sadlImageRawBase64, asHeaderOnly = true))
                                    // Notify data set changed
                                    notifyDataSetChanged()
                                }
                            } else {
                                println("Warning: scanDate is shorter than expected: $scanDate")
                            }
                            lastDate = scanDate.substring(0, 10)
                        }
                        val scanDate = dateFormat.parse(item.scanDate)
                        // Ensure scanDate is not null before comparison
                        if (scanDate != null) {
                            if (scanDate.after(startDate) && scanDate.before(endDate)) {
                                recentScansAdapterData.add(item)

// Notify data set changed (this is no longer needed inside the loop if called after the loop)
                                notifyDataSetChanged()
                            }

                            }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                notifyDataSetChanged()
            }
        }
        filter = true
        notifyDataSetChanged()
    }

    fun extractDocumentRawTextinRecents(rawData: String): String{

        // Split the raw string into lines
        val lines = rawData.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        var fName = ""
        var lName = ""
        // Iterate over each line to find the required information
        for (line in lines) {
            when {
                line.startsWith("first_name:") -> {
                    fName = line.split("first_name:")[1].trim()
                }
                line.startsWith("last_name:") -> {
                    lName = line.split("last_name:")[1].trim()
                }
            }
        }
        return fName + " " + lName
    }

    public fun switchCheckMode() {
        checkModeAdapter = !checkModeAdapter
        notifyDataSetChanged()
    }

    private fun isFirstInGroup(position: Int): Boolean {
        if (position == 0 || getItemViewType(position - 1) == HEADER_ITEM_VIEW) return true
        return false
    }

    private fun isLastInGroup(position: Int): Boolean {
        if (position == itemCount - 1 || getItemViewType(position + 1) == HEADER_ITEM_VIEW) return true
        return false
    }
}



