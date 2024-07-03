package com.barkoder.demoscanner.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.barkoder.demoscanner.R
import com.barkoder.demoscanner.models.RecentScan
import com.barkoder.demoscanner.utils.ScannedResultsUtil
import java.lang.ref.WeakReference

class RecentScansAdapter(
    context: Context,
    private val recentScanItemClickListenerRef: WeakReference<OnRecentScanItemClickListener>?
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface OnRecentScanItemClickListener {
        fun onRecentScanItemClick(item: RecentScan)
        fun onRecentScanItemLongClick(item: RecentScan, position: Int)
    }

    private val HEADER_ITEM_VIEW = 0
    private val ROW_ITEM_VIEW = 1

    private val recentScansAdapterData = ArrayList<RecentScan>().apply {
        val recentScansList = ScannedResultsUtil.getResultsFromPref(context)
        var latestDate: String? = null

        recentScansList.forEach { recentScan ->
            if (latestDate == null || latestDate != recentScan.scanDate) {
                latestDate = recentScan.scanDate
                add(RecentScan(recentScan.scanDate, asHeaderOnly = true))
            }
            add(recentScan)
        }
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateHeader: TextView = view.findViewById(R.id.txtRecentHeader)
    }

    class RowViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val barcodeImage: ImageView = view.findViewById(R.id.imgBarcode)
        val barcodeResult: TextView = view.findViewById(R.id.txtRowBarcodeResult)
        val barcodeType: TextView = view.findViewById(R.id.txtRowBarcodeType)
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
        if (getItemViewType(position) == HEADER_ITEM_VIEW) {
            (holder as HeaderViewHolder).dateHeader.text = recentScansAdapterData[position].scanDate
        } else {
            (holder as RowViewHolder).barcodeImage.setImageResource(
                if (ScannedResultsUtil.isBarcode2D(recentScansAdapterData[position].scanType()))
                    R.drawable.ic_barcode_2d_white
                else
                    R.drawable.ic_barcode_1d_white
            )
            holder.barcodeResult.text = recentScansAdapterData[position].readString()
            holder.barcodeType.text = recentScansAdapterData[position].scanTypeName

            if (recentScanItemClickListenerRef != null) {
                holder.itemView.setOnClickListener {
                    recentScanItemClickListenerRef.get()
                        ?.onRecentScanItemClick(recentScansAdapterData[position])
                }
                holder.itemView.setOnLongClickListener {
                    recentScanItemClickListenerRef.get()
                        ?.onRecentScanItemLongClick(recentScansAdapterData[position], position)
                    true
                }
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

    fun deleteAllItems() {
        val allItems = itemCount
        recentScansAdapterData.clear()
        notifyItemRangeRemoved(0, allItems)
    }

    fun getItemPositionWithoutHeaders(position: Int): Int {
        var positionWithoutHeader = position
        for (i in position downTo 0) {
            if (getItemViewType(i) == HEADER_ITEM_VIEW)
                positionWithoutHeader--
        }

        return positionWithoutHeader
    }

    fun deleteItem(itemPosition: Int) {
        if (getItemViewType(itemPosition - 1) == HEADER_ITEM_VIEW) {
            val deleteHeader = deleteHeaderForItemOnPosition(itemPosition)
            if (deleteHeader) {
                val headerPosition = itemPosition - 1
                recentScansAdapterData.removeAt(itemPosition)
                recentScansAdapterData.removeAt(headerPosition)
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
        recentScansAdapterData.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, itemCount - position)
    }

    private fun deleteHeaderForItemOnPosition(position: Int): Boolean {
        return position >= itemCount - 1 || recentScansAdapterData[position + 1].asHeaderOnly
    }
}
