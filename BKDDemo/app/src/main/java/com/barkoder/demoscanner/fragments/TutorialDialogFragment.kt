// File: `src/main/java/com/barkoder/demoscanner/fragments/TutorialDialogFragment.kt`
package com.barkoder.demoscanner.fragments

import android.app.Dialog
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.core.os.bundleOf
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.barkoder.demoscanner.databinding.FragmentTutorialDialogBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import kotlin.div
import kotlin.math.roundToInt
import kotlin.or

class TutorialDialogFragment : DialogFragment() {

    interface Callbacks {
        fun onPrev(step: Int)
        fun onNext(step: Int)
        fun onSkip(step: Int)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = FragmentTutorialDialogBinding.inflate(LayoutInflater.from(requireContext()))

        val title = requireArguments().getString(ARG_TITLE).orEmpty()
        val message = requireArguments().getString(ARG_MESSAGE).orEmpty()
        val step = requireArguments().getInt(ARG_STEP, 0)
        val hasPrev = requireArguments().getBoolean(ARG_HAS_PREV, true)
        val hasNext = requireArguments().getBoolean(ARG_HAS_NEXT, true)

        binding.tvTitle.text = title
        binding.tvMessage.text = message

        binding.btnPrev.isEnabled = hasPrev
        binding.btnPrev.isVisible = hasPrev
        binding.btnNext.isEnabled = hasNext

        binding.btnPrev.setOnClickListener {
            (activity as? Callbacks)?.onPrev(step)
            dismiss()
        }

        binding.btnSkip.setOnClickListener {
            (activity as? Callbacks)?.onSkip(step)
            dismiss()
        }
        binding.btnNext.setOnClickListener {
            (activity as? Callbacks)?.onNext(step)
            dismiss()
        }

        // Keep this in sync with the spacing used in positioning below.
        val contentPaddingPx = dpToPx(20)
        binding.root.setPadding(
            contentPaddingPx,
            contentPaddingPx,
            contentPaddingPx,
            contentPaddingPx
        )

        val cornerRadiusPx = dpToPx(18).toFloat()
        binding.root.background = MaterialShapeDrawable(
            ShapeAppearanceModel.builder()
                .setAllCornerSizes(cornerRadiusPx)
                .build()
        ).apply {
            setTint(Color.WHITE)
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setCancelable(false)
            .create()

        dialog.setCanceledOnTouchOutside(false)


//        dialog.setOnShowListener {
//            val window = dialog.window ?: return@setOnShowListener
//            window.setGravity(Gravity.TOP or Gravity.START)
//            window.setLayout(0, 0)
//            window.decorView.alpha = 0f
//
//
//        }
        positionRelativeToAnchor(dialog)
        return dialog
    }

    private fun positionRelativeToAnchor(dialog: Dialog) {
        val window = dialog.window ?: return

        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.decorView.setPadding(0, 0, 0, 0)

        val anchorId = requireArguments().getInt(ARG_ANCHOR_VIEW_ID, 0)
        val anchorMode = requireArguments().getInt(ARG_ANCHOR_MODE, MODE_BELOW)

        // Add small gap (default 5dp via ARG_MARGIN_DP).
        val marginDp = requireArguments().getInt(ARG_MARGIN_DP, 5)
        val marginPx = dpToPx(marginDp)

        // Must match the padding you apply to the content view in onCreateDialog.
        val contentPaddingPx = dpToPx(20)

        val anchor = if (anchorId != 0) activity?.findViewById<View>(anchorId) else null
        if (anchor == null) {
            window.setLayout(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            window.decorView.animate().alpha(1f).setDuration(80L).start()
            return
        }

        anchor.doOnPreDraw {
            val decor = window.decorView

            val dm = resources.displayMetrics
            val screenW = dm.widthPixels
            val screenH = dm.heightPixels

            decor.measure(
                View.MeasureSpec.makeMeasureSpec(screenW, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(screenH, View.MeasureSpec.AT_MOST)
            )
            val dialogW = decor.measuredWidth
            val dialogH = decor.measuredHeight

            val rScreen = Rect()
            if (!anchor.getGlobalVisibleRect(rScreen)) {
                window.setLayout(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
                )
                decor.animate().alpha(1f).setDuration(80L).start()
                return@doOnPreDraw
            }

            val decorLoc = IntArray(2)
            decor.getLocationOnScreen(decorLoc)
            val rWindow = Rect(
                rScreen.left - decorLoc[0],
                rScreen.top - decorLoc[1],
                rScreen.right - decorLoc[0],
                rScreen.bottom - decorLoc[1]
            )

            val windowW = decor.width.takeIf { it > 0 } ?: screenW
            val windowH = decor.height.takeIf { it > 0 } ?: screenH

            val x = (rWindow.centerX() - dialogW / 2f)
                .coerceIn(0f, (windowW - dialogW).coerceAtLeast(0).toFloat())
                .roundToInt()

            val clearancePx = marginPx + contentPaddingPx

            val desiredY = when (anchorMode) {
                MODE_ABOVE -> rWindow.top - dialogH - clearancePx - clearancePx - clearancePx
                else -> rWindow.bottom + clearancePx - 60
            }
            val y = desiredY.coerceIn(0, (windowH - dialogH).coerceAtLeast(0))

            window.attributes = window.attributes.apply {
                gravity = Gravity.TOP or Gravity.START
                this.x = x
                this.y = y
                softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            }

            window.setLayout(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            decor.animate().alpha(1f).setDuration(120L).start()
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    private fun clampDialogY(
        desiredY: Float,
        containerHeight: Int,
        rootHeight: Int,
        minTopPx: Int,
        minBottomPx: Int
    ): Float {
        val topLimit = minTopPx.toFloat()
        val bottomLimit = (rootHeight - containerHeight - minBottomPx).toFloat()
        return desiredY.coerceIn(topLimit, bottomLimit)
    }

    companion object {
        private const val ARG_TITLE = "arg_title"
        private const val ARG_MESSAGE = "arg_message"
        private const val ARG_STEP = "arg_step"
        private const val ARG_HAS_PREV = "arg_has_prev"
        private const val ARG_HAS_NEXT = "arg_has_next"
        private const val ARG_ANCHOR_VIEW_ID = "arg_anchor_view_id"
        private const val ARG_MARGIN_DP = "arg_margin_dp"
        private const val ARG_ANCHOR_MODE = "arg_anchor_mode"

        const val MODE_BELOW = 1
        const val MODE_ABOVE = 2

        fun newInstance(
            title: String,
            message: String,
            step: Int,
            hasPrev: Boolean,
            hasNext: Boolean,
            anchorViewId: Int = 0,
            marginDp: Int = 5,
            anchorMode: Int = MODE_BELOW
        ): TutorialDialogFragment {
            return TutorialDialogFragment().apply {
                arguments = bundleOf(
                    ARG_TITLE to title,
                    ARG_MESSAGE to message,
                    ARG_STEP to step,
                    ARG_HAS_PREV to hasPrev,
                    ARG_HAS_NEXT to hasNext,
                    ARG_ANCHOR_VIEW_ID to anchorViewId,
                    ARG_MARGIN_DP to marginDp,
                    ARG_ANCHOR_MODE to anchorMode
                )
            }
        }
    }
}




