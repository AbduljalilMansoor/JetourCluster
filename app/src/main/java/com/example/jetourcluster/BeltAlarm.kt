package com.example.jetourcluster

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.os.SystemClock

/**
 * Rule: speed > threshold AND (driver unbuckled OR passenger rule violated), continuously for the
 * configured delay -> repeating beep. Stops as soon as belts are fastened or the car stops.
 * Missing data (null) never triggers the alarm. Call from the main thread only.
 */
class BeltAlarm(private val prefs: ClusterPrefs, private val onChange: (Boolean) -> Unit) {

    var active = false
        private set

    private val h = Handler(Looper.getMainLooper())
    private var since = 0L
    private var tone: ToneGenerator? = null
    private var testUntil = 0L

    private val beep = object : Runnable {
        override fun run() {
            tone?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 600)
            h.postDelayed(this, 1200)
        }
    }

    fun evaluate(s: VehicleState) {
        if (SystemClock.elapsedRealtime() < testUntil) return
        val moving = (s.speedKmh ?: 0f) > prefs.speedThresholdKmh
        val driverBad = s.driverBuckled == false
        val passBad = when (prefs.passengerMode) {
            0 -> false
            1 -> s.passengerOccupied == true && s.passengerBuckled == false
            else -> s.passengerBuckled == false
        }
        if (moving && (driverBad || passBad)) {
            val now = SystemClock.elapsedRealtime()
            if (since == 0L) since = now
            if (!active && now - since >= prefs.alarmDelayMs) start()
        } else {
            since = 0L
            if (active) stop()
        }
    }

    private fun start() {
        active = true
        try { if (tone == null) tone = ToneGenerator(AudioManager.STREAM_ALARM, 100) } catch (_: RuntimeException) { }
        h.post(beep)
        onChange(true)
    }

    private fun stop() {
        active = false
        h.removeCallbacks(beep)
        tone?.stopTone()
        onChange(false)
    }

    fun release() {
        if (active) stop()
        tone?.release()
        tone = null
    }

    /** For the "test alarm" button. */
    fun test(durationMs: Long = 3000) {
        if (active) return
        testUntil = SystemClock.elapsedRealtime() + durationMs
        start()
        h.postDelayed({ if (active) stop() }, durationMs)
    }
}
