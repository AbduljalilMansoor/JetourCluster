package com.example.jetourcluster

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.widget.FrameLayout
import android.widget.TextView

/**
 * Base class for every cluster widget. To add a widget: subclass this, implement [applyScale]
 * (size texts from the widget height so it works at any resolution) and register it in [ClusterLayout].
 */
abstract class ClusterWidget(context: Context) : FrameLayout(context) {
    protected val dens = resources.displayMetrics.density

    init {
        background = GradientDrawable().apply {
            setColor(0xFF12161B.toInt())
            cornerRadius = 14f * dens
        }
    }

    /** [h] is the widget height in px. */
    protected abstract fun applyScale(h: Float)

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        if (h > 0) applyScale(h.toFloat())
    }

    protected fun tv(color: Int, bold: Boolean = false) = TextView(context).apply {
        setTextColor(color)
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        includeFontPadding = false
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }

    protected fun TextView.px(size: Float) = setTextSize(TypedValue.COMPLEX_UNIT_PX, size)
}
