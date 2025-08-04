package com.barkoder.demoscanner.customcontrols

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.TextView
import androidx.core.content.res.TypedArrayUtils
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.barkoder.demoscanner.R

class CustomPreferenceTitleRed(
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
            context, androidx.preference.R.attr.preferenceStyle,
            android.R.attr.preferenceStyle
        )
    )

    constructor(context: Context) : this(
        context,
        null
    )

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)

        val titleTextView = holder?.findViewById(android.R.id.title)

        val titleColor = Color.RED

        if (titleTextView is TextView) {
            titleTextView.setTextColor(titleColor)
        }
    }
}