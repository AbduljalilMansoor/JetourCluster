package com.example.jetourcluster

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log

/** Keeps the cluster window alive, feeds it vehicle data and runs the belt alarm. */
class ClusterService : Service() {

    companion object {
        private const val TAG = "ClusterService"
        private const val CHANNEL = "cluster"
        @Volatile var instance: ClusterService? = null
            private set

        fun start(ctx: Context) = ctx.startForegroundService(Intent(ctx, ClusterService::class.java))
        fun stop(ctx: Context) { ctx.stopService(Intent(ctx, ClusterService::class.java)) }
        fun isRunning() = instance != null
    }

    private val main = Handler(Looper.getMainLooper())
    private lateinit var prefs: ClusterPrefs
    private lateinit var dm: DisplayManager
    private lateinit var alarm: BeltAlarm
    private var source: VehicleSource? = null
    private var presentation: ClusterPresentation? = null
    private var last = VehicleState()

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) { if (displayId == prefs.displayId) showPresentation() }
        override fun onDisplayRemoved(displayId: Int) {
            if (displayId == prefs.displayId) { presentation?.dismiss(); presentation = null }
        }
        override fun onDisplayChanged(displayId: Int) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = ClusterPrefs(this)

        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Cluster display", NotificationManager.IMPORTANCE_LOW))
        startForeground(
            1,
            Notification.Builder(this, CHANNEL)
                .setContentTitle("Jetour cluster display running")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build()
        )

        alarm = BeltAlarm(prefs) { on -> presentation?.belt?.setAlarm(on) }

        dm = getSystemService(DisplayManager::class.java)
        dm.registerDisplayListener(displayListener, main)
        showPresentation()

        source = (if (prefs.simulator) SimSource(::onVehicle) else CarApiSource(this, prefs, ::onVehicle)).also { it.start() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    private fun onVehicle(s: VehicleState) {
        main.post {
            last = s
            presentation?.belt?.update(s, prefs.passengerMode)
            alarm.evaluate(s)
        }
    }

    fun testAlarm() = main.post { alarm.test() }

    private fun showPresentation() {
        if (presentation != null) return
        val display = dm.getDisplay(prefs.displayId)
        if (display == null) { Log.w(TAG, "Display ${prefs.displayId} not found"); return }
        if (!Settings.canDrawOverlays(this)) { Log.w(TAG, "Overlay permission missing"); return }
        try {
            presentation = ClusterPresentation(this, display, prefs).also {
                it.show()
                it.belt.update(last, prefs.passengerMode)
                it.belt.setAlarm(alarm.active)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cannot show presentation", e)
            presentation = null
        }
    }

    override fun onDestroy() {
        instance = null
        source?.stop()
        alarm.release()
        dm.unregisterDisplayListener(displayListener)
        presentation?.dismiss()
        presentation = null
        super.onDestroy()
    }
}
