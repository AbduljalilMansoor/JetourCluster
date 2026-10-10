package com.example.jetourcluster

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log

data class VehicleState(
    val speedKmh: Float? = null,
    val driverBuckled: Boolean? = null,
    val passengerBuckled: Boolean? = null,
    val passengerOccupied: Boolean? = null,
    val connected: Boolean = false
)

/** Anything that can feed [VehicleState]. Add your own implementation if your head unit uses a vendor API. */
interface VehicleSource {
    fun start()
    fun stop()
}

/** Fake data controlled from the main screen, for bench testing the alarm. */
object SimVehicle {
    @Volatile var speed = 0f
    @Volatile var driver = true
    @Volatile var passenger = false
    @Volatile var occupied = true
}

class SimSource(private val onState: (VehicleState) -> Unit) : VehicleSource {
    private val h = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            onState(VehicleState(SimVehicle.speed, SimVehicle.driver, SimVehicle.passenger, SimVehicle.occupied, true))
            h.postDelayed(this, 500)
        }
    }
    override fun start() { h.post(tick) }
    override fun stop() { h.removeCallbacks(tick) }
}

/**
 * Reads speed + seat belts from the Android Automotive CarPropertyManager through reflection,
 * so the project builds without the hidden android.car stubs. Polls every 500 ms.
 *
 * Property ids (VehiclePropertyIds):
 *   PERF_VEHICLE_SPEED  0x11600207  float, m/s, global area
 *   SEAT_BELT_BUCKLED   0x15200405  boolean, per seat area
 *   SEAT_OCCUPANCY      0x15400BB0  int (0 unknown, 1 vacant, 2 occupied), per seat area
 */
class CarApiSource(
    private val ctx: Context,
    private val prefs: ClusterPrefs,
    private val onState: (VehicleState) -> Unit
) : VehicleSource {

    private companion object {
        const val TAG = "CarApiSource"
        const val SPEED = 0x11600207
        const val BELT = 0x15200405
        const val OCC = 0x15400BB0
    }

    private val thread = HandlerThread("car-poll")
    private lateinit var h: Handler
    private var car: Any? = null
    private var pm: Any? = null
    private var running = false
    private var lastConnected: Boolean? = null

    override fun start() {
        running = true
        thread.start()
        h = Handler(thread.looper)
        h.post { connect() }
    }

    override fun stop() {
        running = false
        if (::h.isInitialized) h.removeCallbacksAndMessages(null)
        try { car?.javaClass?.getMethod("disconnect")?.invoke(car) } catch (_: Throwable) { }
        thread.quitSafely()
    }

    private fun connect() {
        if (!running) return
        try {
            val carCls = Class.forName("android.car.Car")
            car = carCls.getMethod("createCar", Context::class.java).invoke(null, ctx)
            pm = car?.let { carCls.getMethod("getCarManager", String::class.java).invoke(it, "property") }
        } catch (t: Throwable) {
            Log.w(TAG, "Car API unavailable: ${t.cause ?: t}")
        }
        if (pm == null) {
            report(VehicleState(connected = false))
            h.postDelayed({ connect() }, 5000)   // retry; the car service may start late after boot
            return
        }
        poll()
    }

    private fun poll() {
        if (!running) return
        val speedMs = readFloat(SPEED, 0)
        val driver = readBool(BELT, prefs.driverArea)
        val pass = readBool(BELT, prefs.passengerArea)
        val occ = readInt(OCC, prefs.passengerArea)?.let { it == 2 }
        report(VehicleState(speedMs?.times(3.6f), driver, pass, occ, connected = speedMs != null || driver != null))
        h.postDelayed({ poll() }, 500)
    }

    private fun report(s: VehicleState) {
        if (lastConnected != s.connected) { lastConnected = s.connected; Log.i(TAG, "connected=${s.connected}") }
        onState(s)
    }

    private val mgrCls by lazy { Class.forName("android.car.hardware.property.CarPropertyManager") }
    private val i = Int::class.javaPrimitiveType

    private fun readFloat(prop: Int, area: Int): Float? = try {
        mgrCls.getMethod("getFloatProperty", i, i).invoke(pm, prop, area) as? Float
    } catch (_: Throwable) { null }

    private fun readBool(prop: Int, area: Int): Boolean? = try {
        mgrCls.getMethod("getBooleanProperty", i, i).invoke(pm, prop, area) as? Boolean
    } catch (_: Throwable) { null }

    private fun readInt(prop: Int, area: Int): Int? = try {
        mgrCls.getMethod("getIntProperty", i, i).invoke(pm, prop, area) as? Int
    } catch (_: Throwable) { null }
}
