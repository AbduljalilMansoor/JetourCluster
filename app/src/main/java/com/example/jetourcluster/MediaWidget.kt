package com.example.jetourcluster

import android.content.Context
import android.graphics.Color
import android.graphics.Outline
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar

/** Now playing: art, title, artist, source app and progress. */
class MediaWidget(context: Context) : ClusterWidget(context) {

    private val main = Handler(Looper.getMainLooper())
    private val dim = 0xFFB0BAC5.toInt()

    private val art = ImageView(context).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(v: View, o: Outline) { o.setRoundRect(0, 0, v.width, v.height, 10f * dens) }
        }
        setBackgroundColor(0xFF1E252D.toInt())
    }
    private val title = tv(Color.WHITE, true).apply { text = "No media playing" }
    private val artist = tv(dim)
    private val source = tv(0xFF6FA8FF.toInt())
    private val progress = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply { max = 1000 }
    private val row = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    private val col = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_VERTICAL
    }

    private var info: MediaInfo? = null

    private val listener: (MediaInfo?) -> Unit = { i -> main.post { show(i) } }

    private val ticker = object : Runnable {
        override fun run() { updateProgress(); main.postDelayed(this, 1000) }
    }

    init {
        col.addView(source)
        col.addView(title)
        col.addView(artist)
        col.addView(progress)
        row.addView(art)
        row.addView(col, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
        addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    override fun applyScale(h: Float) {
        val pad = (h * 0.09f).toInt()
        val side = (h - 2 * pad).toInt()
        row.setPadding(pad, pad, pad, pad)
        art.layoutParams = LinearLayout.LayoutParams(side, side).apply { rightMargin = pad }
        source.px(h * 0.12f)
        title.px(h * 0.24f)
        artist.px(h * 0.16f)
        progress.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (h * 0.05f).toInt().coerceAtLeast(4))
            .apply { topMargin = (h * 0.06f).toInt() }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        MediaBridge.subscribe(listener)
        main.post(ticker)
    }

    override fun onDetachedFromWindow() {
        MediaBridge.unsubscribe(listener)
        main.removeCallbacks(ticker)
        super.onDetachedFromWindow()
    }

    private fun show(i: MediaInfo?) {
        info = i
        if (i == null) {
            title.text = "No media playing"
            artist.text = "Grant notification access to show media"
            source.text = ""
            art.setImageDrawable(null)
            progress.progress = 0
            return
        }
        title.text = i.title
        artist.text = i.artist
        source.text = (if (i.playing) "▶  " else "⏸  ") + i.app
        if (i.art != null) art.setImageBitmap(i.art) else art.setImageDrawable(null)
        updateProgress()
    }

    private fun updateProgress() {
        val i = info ?: return
        if (i.duration <= 0) { progress.progress = 0; return }
        val pos = if (i.playing) i.position + (SystemClock.elapsedRealtime() - i.updatedAt) else i.position
        progress.progress = (pos * 1000 / i.duration).toInt().coerceIn(0, 1000)
    }
}
