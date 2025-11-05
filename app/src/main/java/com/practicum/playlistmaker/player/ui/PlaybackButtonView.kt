package com.practicum.playlistmaker.player.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat.getDrawable
import androidx.core.graphics.drawable.toBitmap
import com.practicum.playlistmaker.R
import com.practicum.playlistmaker.core.ui.App

class PlaybackButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0,
) : View(context, attrs, defStyleAttr, defStyleRes) {

    private var bitmapIcon: Bitmap? = null
    private var iconRect = RectF(0f, 0f, 0f, 0f)
    private var darkTheme: Boolean = false
    private var playIconResId: Int = 0
    private var pauseIconResId: Int = 0
    private var playIconDarkResId: Int = 0
    private var pauseIconDarkResId: Int = 0

    fun setIcon(toPlayTrack: Boolean) {
        val resId = if (darkTheme) {
            if (toPlayTrack) pauseIconDarkResId else playIconDarkResId
        } else {
            if (toPlayTrack) pauseIconResId else playIconResId
        }
        bitmapIcon = getDrawable(context, resId)?.toBitmap()
        invalidate()
    }

    init {
        context.theme.obtainStyledAttributes(
            attrs, R.styleable.PlaybackButtonView, defStyleAttr, defStyleRes
        ).apply {
            try {
                darkTheme = (context.applicationContext as App).getAppTheme()
                playIconResId = getResourceId(R.styleable.PlaybackButtonView_playIcon, 0)
                pauseIconResId = getResourceId(R.styleable.PlaybackButtonView_pauseIcon, 0)
                playIconDarkResId = getResourceId(R.styleable.PlaybackButtonView_playIconDark, 0)
                pauseIconDarkResId = getResourceId(R.styleable.PlaybackButtonView_pauseIconDark, 0)
                bitmapIcon = if (darkTheme) {
                    getDrawable(context, playIconDarkResId)?.toBitmap()
                } else {
                    getDrawable(context, playIconResId)?.toBitmap()
                }
            } finally {
                recycle()
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        iconRect = RectF(0f, 0f, measuredWidth.toFloat(), measuredHeight.toFloat())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bitmapIcon?.let { icon ->
            canvas.drawBitmap(icon, null, iconRect, null)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (isEnabled) {
                    performClick()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    fun cleanup() {
        bitmapIcon?.recycle()
    }
}
