package com.barkoder.demoscanner.customcontrols

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.res.TypedArrayUtils
import androidx.preference.PreferenceViewHolder
import com.barkoder.demoscanner.R
import com.barkoder.demoscanner.utils.CommonUtil.dpToPx
import com.google.android.material.internal.ViewUtils.dpToPx

@Suppress("unused")
class SwitchWithWidgetPreference(
    context: Context, attrs: AttributeSet?, defStyleAttr: Int,
    defStyleRes: Int
) : SwitchWithPaddingPreference(context, attrs, defStyleAttr, defStyleRes) {

    var customClickListener: View.OnClickListener? = null
    private lateinit var prefItemView: View
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
        fun onSwitchStateChanged(preference: SwitchWithWidgetPreference, newState: Boolean)
    }

    @SuppressLint("RestrictedApi")
    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)



        setOnPreferenceChangeListener { _, newValue ->
            if (newValue is Boolean) {
                if (isChecked != newValue) {
                    switchStateChangeListener?.onSwitchStateChanged(this, newValue)
                }

            }
            true
        }

        holder?.run {
            prefItemView = itemView
            val widgetParent = findViewById(android.R.id.widget_frame) as ViewGroup

            //By default switch is not clickable
            val widget: View = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                widgetParent.findViewById(android.R.id.switch_widget)
            } else {
                widgetParent.getChildAt(0)
            }
            widget.isClickable = true
            widget.isFocusable = true

            if (widgetParent is LinearLayout)
                widgetParent.orientation = LinearLayout.HORIZONTAL

            var advancedImage = widgetParent.findViewById<ImageView>(R.id.imgSettingsAdvanced)

            if (advancedImage == null && isChecked) {
                advancedImage =
                    (widgetParent.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(
                        R.layout.preference_settings_advanced_button,
                        widgetParent,
                        false
                    ) as ImageView
                widgetParent.addView(advancedImage, 0)
            } else if (advancedImage != null && !isChecked)
                widgetParent.removeView(advancedImage)
        }


        val itemView = holder?.itemView
        itemView?.setPadding(
            itemView.paddingLeft,
            dpToPx(context, -12).toInt(),  // reduce top padding
            itemView.paddingRight,
            dpToPx(context, -12).toInt()   // reduce bottom padding
        )

        holder?.itemView?.let { view ->
            val params = view.layoutParams as? ViewGroup.MarginLayoutParams
            params?.marginStart = dpToPx(16).toInt()
            params?.marginEnd = dpToPx(16).toInt()
            view.layoutParams = params
        }

    }

    override fun onClick() {
        // If checked state will be changed trough switch change
        if (isChecked)
            customClickListener?.onClick(prefItemView)
        else
            super.onClick()
    }
}
