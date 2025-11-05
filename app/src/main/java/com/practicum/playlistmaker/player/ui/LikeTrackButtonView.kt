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

class LikeTrackButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0,
) : View(context, attrs, defStyleAttr, defStyleRes) {

    private var bitmapIcon: Bitmap? = null
    private var iconRect = RectF(0f, 0f, 0f, 0f)
    private var darkTheme: Boolean = false
    private var toLikeTrack: Boolean = false
    private var trackLickedId: Int = 0
    private var trackNotLikedId: Int = 0
    private val iconWidthSize = 25.dpToPx()
    private val iconHeightSize = 23.dpToPx()

    fun setLickedState(toLikeTrack: Boolean) {
        this.toLikeTrack = toLikeTrack
    }

    fun setIcon(toLikeTrack: Boolean) {
        this.toLikeTrack = toLikeTrack
        val resId = if (toLikeTrack) {
            trackLickedId
        } else {
            trackNotLikedId
        }
        bitmapIcon = getDrawable(context, resId)?.toBitmap(
            iconWidthSize, iconHeightSize
        )
        updateIconRect()
        invalidate()
    }

    init {
        context.theme.obtainStyledAttributes(
            attrs, R.styleable.LikeTrackButtonView, defStyleAttr, defStyleRes
        ).apply {
            try {
                darkTheme = (context.applicationContext as App).getAppTheme()
                val buttonBackgroundColor = if (darkTheme) {
                    getResourceId(R.styleable.LikeTrackButtonView_backgroundDark, 0)
                } else {
                    getResourceId(R.styleable.LikeTrackButtonView_backgroundDay, 0)
                }
                setBackgroundResource(buttonBackgroundColor)
                trackLickedId = getResourceId(R.styleable.LikeTrackButtonView_trackLickedIcon, 0)
                trackNotLikedId =
                    getResourceId(R.styleable.LikeTrackButtonView_trackNotLickedIcon, 0)
                bitmapIcon = if (toLikeTrack) {
                    getDrawable(context, trackLickedId)?.toBitmap(
                        iconWidthSize, iconHeightSize
                    )
                } else {
                    getDrawable(context, trackNotLikedId)?.toBitmap(
                        iconWidthSize, iconHeightSize
                    )
                }
            } finally {
                recycle()
            }
            updateIconRect()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateIconRect()
    }

    private fun updateIconRect() {
        if (width > 0 && height > 0) {
            val left = (width - iconWidthSize) / 2f
            val top = (height - iconHeightSize) / 2f
            val right = left + iconWidthSize
            val bottom = top + iconHeightSize
            iconRect.set(left, top, right, bottom)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bitmapIcon?.let { icon ->
            canvas.drawBitmap(icon, null, iconRect, null)
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
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
