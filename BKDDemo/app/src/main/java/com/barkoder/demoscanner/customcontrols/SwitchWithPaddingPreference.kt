package com.barkoder.demoscanner.customcontrols

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.TextView
import androidx.core.content.res.TypedArrayUtils
import androidx.core.content.res.getIntOrThrow
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreference
import com.barkoder.demoscanner.R


@Suppress("unused")
open class SwitchWithPaddingPreference(
    context: Context, attrs: AttributeSet?, defStyleAttr: Int,
    defStyleRes: Int
) : SwitchPreference(context, attrs, defStyleAttr, defStyleRes) {

    var paddingStart: Int? = null
    private var switchStateChangeListener: OnSwitchStateChangeListener? = null


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
            context,
            androidx.preference.R.attr.switchPreferenceStyle,
            android.R.attr.switchPreferenceStyle
        )
    )

    constructor(context: Context) : this(
        context,
        null
    )


    fun setOnSwitchStateChangeListener(listener: OnSwitchStateChangeListener?) {
        switchStateChangeListener = listener
    }

    interface OnSwitchStateChangeListener {
        fun onSwitchStateChanged(preference: SwitchWithPaddingPreference, newState: Boolean)
    }

    init {
        if (attrs != null) {
            val attributeValues =
                context.theme.obtainStyledAttributes(
                    attrs,
                    R.styleable.SwitchWithPaddingPreference,
                    0,
                    0
                )

            try {
                paddingStart = attributeValues.getIntOrThrow(
                    R.styleable.SwitchWithPaddingPreference_prefPaddingStart
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

        val titleTextView = holder?.findViewById(android.R.id.title)

        val titleColor = if (isEnabled) Color.BLACK else Color.GRAY

        if (titleTextView is TextView) {
            titleTextView.setTextColor(titleColor)
        }

        setOnPreferenceChangeListener { _, newValue ->
            if (newValue is Boolean) {
                if (isChecked != newValue) {
                    switchStateChangeListener?.onSwitchStateChanged(this, newValue)
                }

            }
            true
        }

        holder?.run {
            paddingStart?.let {
                val iconFrame = findViewById(R.id.icon_frame)
                iconFrame.minimumWidth = it
            }
        }
    }
}
