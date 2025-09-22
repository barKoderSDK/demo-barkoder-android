package com.barkoder.demoscanner.customcontrols

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroupAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.barkoder.demoscanner.R

class CategoryRoundedCornersDecoration(
    private val context: Context
) : RecyclerView.ItemDecoration() {

    @SuppressLint("RestrictedApi") // Required to use PreferenceGroupAdapter#getItem()
    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(canvas, parent, state)

        val layoutManager = parent.layoutManager as? LinearLayoutManager ?: return
        val adapter = parent.adapter as? PreferenceGroupAdapter ?: return

        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()

        for (pos in firstVisible..lastVisible) {
            val currentPref = adapter.getItem(pos) ?: continue

            // Skip categories themselves
            if (currentPref is PreferenceCategory) continue

            val parentCategory = currentPref.parent ?: continue

            // Find visible siblings in the same category
            val visibleSiblingsInCategory = (firstVisible..lastVisible).mapNotNull { i ->
                val p = adapter.getItem(i)
                if (p?.parent == parentCategory && p !is PreferenceCategory) i else null
            }

            if (visibleSiblingsInCategory.isEmpty()) continue

            val isFirstVisible = pos == visibleSiblingsInCategory.first()
            val isLastVisible = pos == visibleSiblingsInCategory.last()

            val itemView = parent.findViewHolderForAdapterPosition(pos)?.itemView ?: continue

            val bgRes = when {
                isFirstVisible && isLastVisible -> R.drawable.bg_rounded_all
                isFirstVisible -> R.drawable.bg_rounded_top
                isLastVisible -> R.drawable.bg_rounded_bottom
                else -> R.drawable.not_rounded_background // Use a neutral background
            }

            itemView.setBackgroundResource(bgRes)
        }
    }
}
