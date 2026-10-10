package com.example.jetourcluster

import android.content.Context

/** All user-tunable settings, persisted in SharedPreferences. */
class ClusterPrefs(ctx: Context) {
    private val p = ctx.applicationContext.getSharedPreferences("cluster", Context.MODE_PRIVATE)

    /** Android display id of the instrument cluster ("display2"). */
    var displayId: Int
        get() = p.getInt("displayId", 2)
        set(v) = p.edit().putInt("displayId", v).apply()

    /** Number of grid rows (the grid always has 24 columns). */
    var rows: Int
        get() = p.getInt("rows", 1).coerceIn(1, 6)
        set(v) = p.edit().putInt("rows", v).apply()

    var showGuides: Boolean
        get() = p.getBoolean("guides", false)
        set(v) = p.edit().putBoolean("guides", v).apply()

    /** Use fake vehicle data (for bench testing the alarm without driving). */
    var simulator: Boolean
        get() = p.getBoolean("sim", false)
        set(v) = p.edit().putBoolean("sim", v).apply()

    var autostart: Boolean
        get() = p.getBoolean("autostart", true)
        set(v) = p.edit().putBoolean("autostart", v).apply()

    /** VehicleAreaSeat ids: ROW_1_LEFT = 1, ROW_1_CENTER = 2, ROW_1_RIGHT = 4. Driver is left on LHD cars. */
    var driverArea: Int
        get() = p.getInt("driverArea", 1)
        set(v) = p.edit().putInt("driverArea", v).apply()

    var passengerArea: Int
        get() = p.getInt("passengerArea", 4)
        set(v) = p.edit().putInt("passengerArea", v).apply()

    var speedThresholdKmh: Float
        get() = p.getFloat("speedThr", 5f)
        set(v) = p.edit().putFloat("speedThr", v).apply()

    /** Unfastened belt must persist this long while moving before the alarm starts. */
    var alarmDelayMs: Long
        get() = p.getLong("alarmDelay", 1500L)
        set(v) = p.edit().putLong("alarmDelay", v).apply()

    /** 0 = ignore passenger belt, 1 = only when the seat is occupied, 2 = always. */
    var passengerMode: Int
        get() = p.getInt("passMode", 1)
        set(v) = p.edit().putInt("passMode", v).apply()

    var lat: Double
        get() = p.getString("lat", null)?.toDoubleOrNull() ?: DEFAULT_LAT
        set(v) = p.edit().putString("lat", v.toString()).apply()

    var lon: Double
        get() = p.getString("lon", null)?.toDoubleOrNull() ?: DEFAULT_LON
        set(v) = p.edit().putString("lon", v.toString()).apply()

    companion object {
        const val COLS = 24
        // Fallback weather position when no location permission/fix. Change in the app or here.
        const val DEFAULT_LAT = 26.2285
        const val DEFAULT_LON = 50.5860
    }
}
