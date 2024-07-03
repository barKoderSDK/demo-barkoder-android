package com.barkoder.demoscanner.customcontrols

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.TypedArrayUtils
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceViewHolder
import com.barkoder.demoscanner.R

@Suppress("unused")
class ClickablePreferenceCategory(
    context: Context, attrs: AttributeSet?, defStyleAttr: Int,
    defStyleRes: Int
) : PreferenceCategory(context, attrs, defStyleAttr, defStyleRes) {

    interface PreferenceCategoryClickListener {
        fun onPreferenceCategoryClick()
    }

    var preferenceCategoryClickListener: PreferenceCategoryClickListener? = null

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
            context, R.attr.preferenceCategoryStyle,
            android.R.attr.preferenceCategoryStyle
        )
    )

    constructor(context: Context) : this(
        context,
        null
    )

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)

        if (preferenceCategoryClickListener != null) {
            holder?.itemView?.setOnClickListener {
                preferenceCategoryClickListener!!.onPreferenceCategoryClick()
            }
        }
    }
}
