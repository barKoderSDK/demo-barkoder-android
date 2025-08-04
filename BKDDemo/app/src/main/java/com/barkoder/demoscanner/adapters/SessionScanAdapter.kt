package com.barkoder.demoscanner.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.barkoder.demoscanner.R
import com.barkoder.demoscanner.models.RecentScan2
import com.barkoder.demoscanner.models.SessionScan
import com.barkoder.demoscanner.utils.CommonUtil
import okhttp3.internal.notifyAll
import java.lang.ref.WeakReference

class SessionScanAdapter(
    private val resultsList: List<String>,
    private val typesList: List<String>,
    private var highlightCount: Int = 0,
    private val sessionScansAdapterData: MutableList<SessionScan>,
    private val recentScanItemClickListenerRef: WeakReference<OnSessionScanItemClickListener>?
) : RecyclerView.Adapter<SessionScanAdapter.ScanViewHolder>() {

    private var recyclerView: RecyclerView? = null

    // Override onAttachedToRecyclerView to store the RecyclerView reference
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView = null
    }

    private var firstName : String? = null
    private var lastName : String? = null
    private var documentNumber : String? = null

    interface OnSessionScanItemClickListener {
        fun onSessionScanItemClick(item: SessionScan)
        fun onSessionScanItemLongClick(item: SessionScan, position: Int)
    }

    class ScanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val resultTextView: TextView = itemView.findViewById(R.id.txtBarcodeResult)
        private val typeTextView: TextView = itemView.findViewById(R.id.txtBarcodeType)
        private val containerLayout: ConstraintLayout = itemView.findViewById(R.id.containerItem)
        val counterScanned : TextView = itemView.findViewById(R.id.scannedCounter)


        fun bind(
            result: String,
            type: String,
            isHighlighted: Boolean,
            listener: OnSessionScanItemClickListener?,
            currentItem: SessionScan,
            position: Int
        ) {

            typeTextView.text = type

            val highlightColor = ContextCompat.getDrawable(itemView.context, R.drawable.item_background_rounded_green)
            val defaultColor = ContextCompat.getDrawable(itemView.context, R.drawable.item_backgorund_rounded)

            // Apply background color based on whether the item is in the last detected range
            if (currentItem.highLight) {
                containerLayout.setBackgroundDrawable(highlightColor) // Highlight last detected barcodes

            } else {
                containerLayout.setBackgroundDrawable(defaultColor) // Default background
            }

            // Set click listeners
            itemView.setOnClickListener {
                listener?.onSessionScanItemClick(currentItem)

            }
            itemView.setOnLongClickListener {
                listener?.onSessionScanItemLongClick(currentItem, position)
                true
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.session_scan_item, parent, false)
        return ScanViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScanViewHolder, position: Int) {
        if (position < resultsList.size && position < typesList.size && position < sessionScansAdapterData.size) {
            val result = resultsList[position]
            val type = typesList[position]
            val currentItem = sessionScansAdapterData[position]

            // Bind the data to the view

          moveHighlightedItemsToLast()

            extractDocumentRawText(currentItem.scanText)
            if(currentItem.scannedTimesInARow > 1) {
                holder.counterScanned.text =  "(${currentItem.scannedTimesInARow.toString()})"
            } else {
                holder.counterScanned.text = ""
            }

            // Set the text in resultTextView
            if (currentItem.scanTypeName == "MRZ") {
                holder.resultTextView.text = "Full name: ${firstName} ${lastName}"
            } else {
                val cleanedResult = CommonUtil.cleanResultString(currentItem.scanText)
                holder.resultTextView.text = cleanedResult
            }

            // Highlight the last `highlightCount` items
            val isHighlighted = position >= itemCount - highlightCount

            val listener = recentScanItemClickListenerRef?.get()

            holder.bind(currentItem.scanText, currentItem.scanTypeName, isHighlighted, listener, currentItem, position)

        } else {
            // Log or handle the error if position is out of bounds
            Log.e("SessionScanAdapter", "Position $position is out of bounds for one or more lists")
        }
    }


    fun moveHighlightedItemsToLast() {
        recyclerView?.post {

            // Sort the list: non-highlighted items first, highlighted items last
            sessionScansAdapterData.sortBy { it.highLight }

        }
    }





    override fun getItemCount(): Int {
        return sessionScansAdapterData.size
    }

    // Method to update the number of items to highlight
    fun updateHighlightCount() {
        notifyDataSetChanged()
    }

    fun extractDocumentRawText(rawData: String){

        // Split the raw string into lines
        val lines = rawData.split("\n").map { it.trim() }.filter { it.isNotEmpty() }

        // Iterate over each line to find the required information
        for (line in lines) {
            when {
                line.startsWith("first_name:") -> {
                    firstName = line.split("first_name:")[1].trim()
                }
                line.startsWith("last_name:") -> {
                    lastName = line.split("last_name:")[1].trim()
                }
                line.startsWith("document_number:") -> {
                    documentNumber = line.split("document_number:")[1].trim()
                }
                // You can add other fields if needed
            }
        }

    }



}