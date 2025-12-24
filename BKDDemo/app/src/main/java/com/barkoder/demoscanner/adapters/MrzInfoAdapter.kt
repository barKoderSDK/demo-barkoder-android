package com.barkoder.demoscanner.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.barkoder.demoscanner.R
import com.barkoder.demoscanner.models.MrzItem

class MrzInfoAdapter(
    private val mrzItems: List<MrzItem>
) : RecyclerView.Adapter<MrzInfoAdapter.MrzViewHolder>() {

    inner class MrzViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val label: TextView = itemView.findViewById(R.id.textLabel)
        val value: TextView = itemView.findViewById(R.id.textValue)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MrzViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mrz_info, parent, false)
        return MrzViewHolder(view)
    }

    override fun onBindViewHolder(holder: MrzViewHolder, position: Int) {
        val item = mrzItems[position]
        holder.label.text = item.label
        holder.value.text = item.value
    }

    override fun getItemCount(): Int = mrzItems.size
}
