package com.barkoder.demoscanner.customcontrols

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.TypedArrayUtils
import androidx.core.content.res.getIntOrThrow
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceViewHolder
import com.barkoder.demoscanner.R


@Suppress("unused")
open class PreferenceCategoryWithPadding(
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
            context, R.attr.preferenceCategoryStyle,
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
                isIconSpaceReserved = true
            } catch (ignored: IllegalArgumentException) {
            } finally {
                attributeValues.recycle()
            }
        }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)

        holder?.run {
            paddingStart?.let {
                val iconFrame = findViewById(R.id.icon_frame)
                iconFrame.minimumWidth = it
            }
        }
    }
}
