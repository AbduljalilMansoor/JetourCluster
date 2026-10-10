package com.example.jetourcluster

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout

enum class SeatState { UNKNOWN, BUCKLED, UNBUCKLED, EMPTY }

/** Little occupant icon: green with the strap across the chest when buckled, red with a hanging strap when not. */
class SeatView(context: Context, private val outerLeft: Boolean) : View(context) {
    var state = SeatState.UNKNOWN
        set(v) { field = v; invalidate() }
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(c: Canvas) {
        val s = minOf(width, height).toFloat()
        c.save()
        c.translate((width - s) / 2f, (height - s) / 2f)
        val color = when (state) {
            SeatState.BUCKLED -> 0xFF34C759.toInt()
            SeatState.UNBUCKLED -> 0xFFFF453A.toInt()
            else -> 0xFF5B6570.toInt()
        }
        p.color = color
        p.style = if (state == SeatState.EMPTY) Paint.Style.STROKE else Paint.Style.FILL
        p.strokeWidth = s * 0.05f
        c.drawCircle(s * 0.5f, s * 0.22f, s * 0.13f, p)
        c.drawRoundRect(RectF(s * 0.22f, s * 0.42f, s * 0.78f, s * 0.98f), s * 0.14f, s * 0.14f, p)

        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = s * 0.07f
        val outerX = if (outerLeft) 0.30f else 0.70f
        val innerX = if (outerLeft) 0.72f else 0.28f
        when (state) {
            SeatState.BUCKLED -> {
                p.color = 0xFF0B2B14.toInt()
                c.drawLine(s * outerX, s * 0.45f, s * innerX, s * 0.93f, p)
            }
            SeatState.UNBUCKLED -> {
                p.color = Color.WHITE
                val x = if (outerLeft) 0.12f else 0.88f
                c.drawLine(s * x, s * 0.42f, s * x, s * 0.88f, p)
            }
            else -> Unit
        }
        c.restore()
    }
}

/** Driver + front passenger belt status; goes red and flashes while the alarm is active. */
class BeltWidget(context: Context) : ClusterWidget(context) {

    private val main = Handler(Looper.getMainLooper())
    private val dim = 0xFFB0BAC5.toInt()
    private val bg = GradientDrawable().apply { cornerRadius = 14f * dens; setColor(NORMAL) }

    private val dSeat = SeatView(context, outerLeft = true)   // driver sits on the left (LHD)
    private val pSeat = SeatView(context, outerLeft = false)
    private val dName = tv(dim).apply { text = "DRIVER" }
    private val pName = tv(dim).apply { text = "PASSENGER" }
    private val dStat = tv(Color.WHITE, true)
    private val pStat = tv(Color.WHITE, true)
    private val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }

    private var dState = SeatState.UNKNOWN
    private var pState = SeatState.UNKNOWN
    private var alarm = false
    private var flashOn = false

    private val flasher = object : Runnable {
        override fun run() {
            flashOn = !flashOn
            bg.setColor(if (flashOn) ALARM else NORMAL)
            main.postDelayed(this, 400)
        }
    }

    init {
        background = bg
        row.addView(cell(dSeat, dName, dStat), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
        row.addView(cell(pSeat, pName, pStat), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
        addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        render()
    }

    private fun cell(seat: SeatView, name: android.widget.TextView, stat: android.widget.TextView): LinearLayout {
        val text = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL }
        text.addView(name)
        text.addView(stat)
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(seat)
            addView(text)
        }
    }

    override fun applyScale(h: Float) {
        val pad = (h * 0.09f).toInt()
        row.setPadding(pad, pad, pad, pad)
        val icon = (h * 0.62f).toInt()
        for (s in listOf(dSeat, pSeat)) s.layoutParams = LinearLayout.LayoutParams(icon, icon).apply { rightMargin = pad }
        dName.px(h * 0.12f); pName.px(h * 0.12f)
        dStat.px(h * 0.17f); pStat.px(h * 0.17f)
    }

    fun update(s: VehicleState, passengerMode: Int) {
        dState = when (s.driverBuckled) { null -> SeatState.UNKNOWN; true -> SeatState.BUCKLED; false -> SeatState.UNBUCKLED }
        pState = when {
            s.passengerOccupied == false -> SeatState.EMPTY
            s.passengerBuckled == null -> SeatState.UNKNOWN
            s.passengerBuckled -> SeatState.BUCKLED
            else -> SeatState.UNBUCKLED
        }
        render()
    }

    fun setAlarm(on: Boolean) {
        if (on == alarm) return
        alarm = on
        main.removeCallbacks(flasher)
        if (on) main.post(flasher) else { flashOn = false; bg.setColor(NORMAL) }
        render()
    }

    private fun render() {
        dSeat.state = dState; pSeat.state = pState
        dStat.text = label(dState); pStat.text = label(pState)
        dStat.setTextColor(color(dState)); pStat.setTextColor(color(pState))
    }

    private fun label(s: SeatState) = when (s) {
        SeatState.BUCKLED -> "BUCKLED"
        SeatState.UNBUCKLED -> if (alarm) "FASTEN BELT!" else "NOT BUCKLED"
        SeatState.EMPTY -> "EMPTY"
        SeatState.UNKNOWN -> "NO DATA"
    }

    private fun color(s: SeatState) = when (s) {
        SeatState.BUCKLED -> 0xFF34C759.toInt()
        SeatState.UNBUCKLED -> 0xFFFF6A5F.toInt()
        else -> dim
    }

    override fun onDetachedFromWindow() {
        main.removeCallbacks(flasher)
        super.onDetachedFromWindow()
    }

    private companion object {
        const val NORMAL = 0xFF12161B.toInt()
        const val ALARM = 0xFF7A0F0A.toInt()
    }
}
