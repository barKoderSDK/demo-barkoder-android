package com.barkoder.demoscanner.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.barkoder.demoscanner.R
import com.barkoder.demoscanner.models.BarcodeDataPrint

class BarcodePrintAdapter(private val items: MutableList<BarcodeDataPrint>) : RecyclerView.Adapter<BarcodePrintAdapter.BarcodeViewHolder>() {

    // ViewHolder for each item
    class BarcodeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val barcodeType: TextView = view.findViewById(R.id.barcodeTypeTitle)
        val barcodeText: TextView = view.findViewById(R.id.barcodeTextTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BarcodeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_barcodes_print_view, parent, false)
        return BarcodeViewHolder(view)
    }

    override fun onBindViewHolder(holder: BarcodeViewHolder, position: Int) {
        val currentItem = items[position]
        holder.barcodeType.text = currentItem.barcodeType
        holder.barcodeText.text = currentItem.barcodeText
    }

    override fun getItemCount(): Int {
        return items.size
    }

    // Add new item and refresh RecyclerView
    fun addItem(barcodeData: BarcodeDataPrint) {
        items.add(0, barcodeData) // Add to the top of the list
        notifyItemInserted(0)
    }
}