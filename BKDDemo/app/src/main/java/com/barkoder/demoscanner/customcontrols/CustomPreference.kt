package com.barkoder.demoscanner.customcontrols

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.res.TypedArrayUtils
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceViewHolder
import com.barkoder.demoscanner.R
import com.barkoder.demoscanner.utils.CommonUtil
import com.google.android.material.internal.ViewUtils.dpToPx

class CustomPreference(
    context: Context, attrs: AttributeSet?, defStyleAttr: Int,
    defStyleRes: Int
) : Preference(context, attrs, defStyleAttr, defStyleRes) {


    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(
        context,
        attrs,
        defStyleAttr,
        0
    )

    @SuppressLint("RestrictedApi")
    constructor(context: Context, attrs: AttributeSet?) : this(
        context,
        attrs,
        TypedArrayUtils.getAttr(
            context, androidx.preference.R.attr.preferenceCategoryStyle,
            android.R.attr.preferenceStyle
        )
    )

    constructor(context: Context) : this(
        context,
        null
    )

    @SuppressLint("RestrictedApi")
    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)

        val titleTextView = holder?.findViewById(android.R.id.title)

//        val titleColor = if (isEnabled) Color.BLACK else Color.GRAY

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



        holder?.itemView?.setBackgroundColor(Color.WHITE)

//        if (titleTextView is TextView) {
//            titleTextView.setTextColor(titleColor)
//        }
    }
}