package com.barkoder.demoscanner.customcontrols

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.preference.ListPreference
import androidx.preference.PreferenceViewHolder
import com.barkoder.demoscanner.R
import com.barkoder.demoscanner.utils.CommonUtil
import com.google.android.material.internal.ViewUtils.dpToPx

class ListPreferenceWhiteBg @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ListPreference(context, attrs) {

    @SuppressLint("RestrictedApi")
    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val itemView = holder?.itemView
        itemView?.setPadding(
            itemView.paddingLeft,
            dpToPx(context, -12).toInt(),  // reduce top padding
            itemView.paddingRight,
            dpToPx(context, -12).toInt()   // reduce bottom padding
        )
        holder?.itemView?.let { view ->
            val params = view.layoutParams as? ViewGroup.MarginLayoutParams
            params?.marginStart = CommonUtil.dpToPx(16).toInt()
            params?.marginEnd = CommonUtil.dpToPx(16).toInt()
            view.layoutParams = params
        }

        // Set the whole preference item's background to white
        holder.itemView.setBackgroundColor(Color.WHITE)

        // 🔴 Remove previous arrow (if any) to avoid duplicates
        val existingArrow = itemView?.findViewWithTag<ImageView>("arrowIcon")
        existingArrow?.let { (it.parent as ViewGroup).removeView(it) }

        // ✅ Add arrow ImageView manually
        val arrow = ImageView(context).apply {
            setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_chevron_right))
            tag = "arrowIcon"
            layoutParams = FrameLayout.LayoutParams(
                CommonUtil.dpToPx(24).toInt(), // Width
                CommonUtil.dpToPx(24).toInt()  // Height
            ).apply {
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
                marginEnd = CommonUtil.dpToPx(5).toInt()
            }
        }

        // ⚠️ Make sure itemView is a ViewGroup (usually it is)
        if (itemView is FrameLayout || itemView is RelativeLayout || itemView is ViewGroup) {
            (itemView as ViewGroup).addView(arrow)
        }
    }
}