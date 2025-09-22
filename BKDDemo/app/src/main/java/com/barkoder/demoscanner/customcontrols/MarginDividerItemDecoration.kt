package com.barkoder.demoscanner.customcontrols

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceGroupAdapter
import androidx.recyclerview.widget.RecyclerView

class MarginDividerItemDecoration(
    private val context: Context,
    @DrawableRes dividerResId: Int,
    private val marginStart: Int = 0,
    private val marginEnd: Int = 0
) : RecyclerView.ItemDecoration() {

    private val divider = ContextCompat.getDrawable(context, dividerResId)!!

    @SuppressLint("RestrictedApi")
    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val left = parent.paddingLeft + marginStart
        val right = parent.width - parent.paddingRight - marginEnd

        val adapter = parent.adapter as? PreferenceGroupAdapter ?: return
        val childCount = parent.childCount

        for (i in 0 until childCount - 1) {
            val child = parent.getChildAt(i)
            val viewHolder = parent.getChildViewHolder(child)
            val currentPref = adapter.getItem(viewHolder.adapterPosition)

            // ❌ Skip divider for PreferenceCategoryWithPadding
            if (currentPref is PreferenceCategoryWithPadding) continue

            val currentCategory = currentPref?.parent
            val nextPosition = viewHolder.adapterPosition + 1

            // ✅ Only draw divider if next item exists and is in the same category
            val nextPref = adapter.getItemOrNull(nextPosition)

            if (nextPref?.parent != currentCategory) {
                continue // 🛑 Don't draw divider after last item in category
            }

            val params = child.layoutParams as RecyclerView.LayoutParams
            val top = child.bottom + params.bottomMargin
            val bottom = top + divider.intrinsicHeight

            divider.setBounds(left, top, right, bottom)
            divider.draw(c)
        }
    }

    // Safe access for adapter item
    @SuppressLint("RestrictedApi")
    private fun PreferenceGroupAdapter.getItemOrNull(position: Int): Preference? {
        return if (position in 0 until itemCount) getItem(position) else null
    }
}
