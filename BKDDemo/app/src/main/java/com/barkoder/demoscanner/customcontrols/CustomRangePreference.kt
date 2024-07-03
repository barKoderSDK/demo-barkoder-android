package com.barkoder.demoscanner.customcontrols

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.res.TypedArrayUtils
import androidx.core.view.setPadding
import androidx.preference.PreferenceViewHolder
import androidx.preference.SeekBarPreference
import com.barkoder.demoscanner.R
import com.barkoder.demoscanner.utils.CommonUtil
import com.google.android.material.slider.LabelFormatter
import com.google.android.material.slider.RangeSlider
import kotlin.math.roundToInt

@Suppress("unused")
class CustomRangePreference(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int
) : SeekBarPreference(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        const val MAX_ALLOWED_VALUE = 100
        const val MIN_ALLOWED_VALUE = 1
    }

    private var container: ConstraintLayout? = null
    private lateinit var currentMinTextView: TextView
    private lateinit var currentMaxTextView: TextView
    private lateinit var rangeSlider: RangeSlider
    private var onChangeListener: RangeSlider.OnChangeListener? = null
    private var rangeSliderValues = mutableListOf(MIN_ALLOWED_VALUE, MAX_ALLOWED_VALUE)

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
            androidx.preference.R.attr.seekBarPreferenceStyle,
            android.R.attr.seekBarStyle
        )
    )

    constructor(context: Context) : this(
        context,
        null
    )

    init {
        showSeekBarValue = false
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)

        if (container == null && holder != null) {
            holder.itemView.isClickable = false
            holder.itemView.isFocusable = false

            createContainerAndAllViews(holder.itemView as ViewGroup)

            rangeSlider.setValues(rangeSliderValues[0].toFloat(), rangeSliderValues[1].toFloat())

            if (onChangeListener != null)
                rangeSlider.addOnChangeListener(onChangeListener!!)
        }
    }

    private fun createContainerAndAllViews(itemView: ViewGroup) {
        container = ConstraintLayout(context)

        createMinTextView()
        createMaxTextView()
        createRangeSlider()

        copySeekbarParamsAndRemove(itemView)
    }

    private fun createMinTextView() {
        currentMinTextView = TextView(context).apply {
            id = R.id.customRangePreferenceMinTextView
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topToTop = ConstraintSet.PARENT_ID
                bottomToBottom = ConstraintSet.PARENT_ID
                startToStart = ConstraintSet.PARENT_ID
            }
        }
        container!!.addView(currentMinTextView)
    }

    private fun createMaxTextView() {
        currentMaxTextView = TextView(context).apply {
            id = R.id.customRangePreferenceMaxTextView
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topToTop = ConstraintSet.PARENT_ID
                bottomToBottom = ConstraintSet.PARENT_ID
                endToEnd = ConstraintSet.PARENT_ID
            }
        }
        container!!.addView(currentMaxTextView)
    }

    private fun createRangeSlider() {
        rangeSlider = RangeSlider(context).apply {
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topToTop = ConstraintSet.PARENT_ID
                bottomToBottom = ConstraintSet.PARENT_ID
                startToEnd = currentMinTextView.id
                endToStart = currentMaxTextView.id
            }
            setPadding(CommonUtil.dpToPx(5))

            stepSize = 1f
            valueFrom = MIN_ALLOWED_VALUE.toFloat()
            valueTo = MAX_ALLOWED_VALUE.toFloat()

            labelBehavior = LabelFormatter.LABEL_GONE

            addOnChangeListener { slider, _, _ ->
                rangeSliderValues[0] = slider.values[0].roundToInt()
                rangeSliderValues[1] = slider.values[1].roundToInt()

                currentMinTextView.text = slider.values[0].roundToInt().toString()
                currentMaxTextView.text = slider.values[1].roundToInt().toString()
            }
        }
        container!!.addView(rangeSlider)
    }

    private fun copySeekbarParamsAndRemove(view: ViewGroup) {
        for (i in 0 until view.childCount) {
            when {
                view.getChildAt(i) is ViewGroup -> copySeekbarParamsAndRemove(view.getChildAt(i) as ViewGroup)
                view.getChildAt(i) is SeekBar -> {
                    container!!.layoutParams = (view.getChildAt(i) as SeekBar).layoutParams
                    view.removeViewAt(i)
                    view.addView(container, i)
                }
            }
        }
    }

    fun setValues(values: List<Int>, updateSlider: Boolean = false) {
        // Check is more for backward compatibility (from 1.0 to 1.1)
        rangeSliderValues[0] =
            if (values[0] in MIN_ALLOWED_VALUE..MAX_ALLOWED_VALUE) values[0] else MIN_ALLOWED_VALUE
        rangeSliderValues[1] =
            if (values[1] in MIN_ALLOWED_VALUE..MAX_ALLOWED_VALUE) values[1] else MAX_ALLOWED_VALUE

        if (updateSlider)
            rangeSlider.setValues(rangeSliderValues[0].toFloat(), rangeSliderValues[1].toFloat())
    }

    fun getValues(): List<Int> {
        return rangeSliderValues.toList()
    }

    fun addOnChangeListener(listener: RangeSlider.OnChangeListener) {
        onChangeListener = listener
    }
}
