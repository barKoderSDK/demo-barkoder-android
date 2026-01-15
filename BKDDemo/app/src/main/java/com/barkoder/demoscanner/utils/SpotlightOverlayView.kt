package com.barkoder.demoscanner.utils

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View

class SpotlightOverlayView(context: Context) : View(context) {

    var blockTouches: Boolean = true

    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 0, 0, 0) // adjust dim level
        style = Paint.Style.FILL
    }

    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        style = Paint.Style.FILL
    }

    private var spotlightRect: RectF? = null
    private var cornerRadius: Float = 0f

    fun setSpotlight(rect: RectF, cornerRadius: Float) {
        this.spotlightRect = rect
        this.cornerRadius = cornerRadius
        invalidate()
    }

    fun clearSpotlight() {
        spotlightRect = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        // Needed for CLEAR to work reliably
        val sc = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)

        spotlightRect?.let { r ->
            canvas.drawRoundRect(r, cornerRadius, cornerRadius, clearPaint)
        }

        canvas.restoreToCount(sc)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return blockTouches
    }
}