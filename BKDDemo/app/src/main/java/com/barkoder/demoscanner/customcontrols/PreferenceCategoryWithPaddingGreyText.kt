package com.barkoder.demoscanner.customcontrols

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.TypedArrayUtils
import androidx.core.content.res.getIntOrThrow
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceViewHolder
import com.barkoder.demoscanner.R
import com.barkoder.demoscanner.utils.CommonUtil
import com.google.android.material.internal.ViewUtils.dpToPx

class PreferenceCategoryWithPaddingGreyText(
    context: Context, attrs: AttributeSet?, defStyleAttr: Int,
    defStyleRes: Int
) : PreferenceCategory(context, attrs, defStyleAttr, defStyleRes) {

    var paddingStart: Int? = null

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
            android.R.attr.preferenceCategoryStyle
        )
    )

    constructor(context: Context) : this(
        context,
        null
    )

    init {
        if (attrs != null) {
            val attributeValues =
                context.theme.obtainStyledAttributes(
                    attrs,
                    R.styleable.PreferenceCategoryWithPadding,
                    0,
                    0
                )

            try {
                paddingStart = attributeValues.getIntOrThrow(
                    R.styleable.PreferenceCategoryWithPadding_prefCategoryPaddingStart
                )
                isIconSpaceReserved = false
            } catch (ignored: IllegalArgumentException) {
            } finally {
                attributeValues.recycle()
            }
        }
    }

    @SuppressLint("RestrictedApi")
    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)

        val itemView = holder?.itemView
        itemView?.setPadding(
            itemView.paddingLeft,
            dpToPx(context, 6).toInt(),  // reduce top padding
            itemView.paddingRight,
            dpToPx(context, 6).toInt()   // reduce bottom padding
        )
        holder?.itemView?.let { view ->
            val params = view.layoutParams as? ViewGroup.MarginLayoutParams
            params?.marginStart = CommonUtil.dpToPx(16).toInt()
            params?.marginEnd = CommonUtil.dpToPx(16).toInt()
            view.layoutParams = params
        }

        val titleView = holder?.findViewById(android.R.id.title) as? TextView
        titleView?.setTextColor(ContextCompat.getColor(context, R.color.greyColorSettingsCatgeroy))

//        holder?.run {
//            paddingStart?.let {
//                val iconFrame = findViewById(com.barkoder.R.id.icon_frame)
//                iconFrame.minimumWidth = it
//            }
//        }
    }



}
