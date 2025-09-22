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
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.res.TypedArrayUtils
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.barkoder.demoscanner.R
import com.barkoder.demoscanner.utils.CommonUtil
import com.barkoder.demoscanner.utils.CommonUtil.dpToPx

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

        holder?.itemView?.let { view ->
            val params = view.layoutParams as? ViewGroup.MarginLayoutParams
            params?.marginStart = dpToPx(16).toInt()
            params?.marginEnd = dpToPx(16).toInt()
            view.layoutParams = params
        }
        holder?.itemView?.setBackgroundColor(Color.WHITE)


        if (titleTextView is TextView) {
            titleTextView.setTextColor(titleColor)
        }

        val itemView = holder?.itemView

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