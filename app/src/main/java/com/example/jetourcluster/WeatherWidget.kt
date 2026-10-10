package com.example.jetourcluster

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.LinearLayout
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

/** Current weather from Open-Meteo (free, no API key). Refreshes every 15 minutes. */
class WeatherWidget(context: Context, private val prefs: ClusterPrefs) : ClusterWidget(context) {

    private val main = Handler(Looper.getMainLooper())
    private val dim = 0xFFB0BAC5.toInt()

    private val icon = tv(Color.WHITE).apply { text = "…" }
    private val temp = tv(Color.WHITE, true).apply { text = "--°" }
    private val cond = tv(dim).apply { text = "Loading weather" }
    private val feels = tv(dim)
    private val wind = tv(dim)
    private val row = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    private val colMain = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val colExtra = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

    private val refresh = object : Runnable {
        override fun run() {
            fetch()
            main.postDelayed(this, 15 * 60_000L)
        }
    }

    init {
        colMain.addView(temp)
        colMain.addView(cond)
        colExtra.addView(feels)
        colExtra.addView(wind)
        row.addView(icon)
        row.addView(colMain)
        row.addView(colExtra)
        addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    override fun applyScale(h: Float) {
        icon.px(h * 0.46f)
        temp.px(h * 0.34f)
        cond.px(h * 0.14f)
        feels.px(h * 0.14f)
        wind.px(h * 0.14f)
        val pad = (h * 0.09f).toInt()
        row.setPadding(pad, pad, pad, pad)
        icon.setPadding(0, 0, pad, 0)
        colMain.setPadding(0, 0, pad, 0)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        main.post(refresh)
    }

    override fun onDetachedFromWindow() {
        main.removeCallbacks(refresh)
        super.onDetachedFromWindow()
    }

    private fun coords(): Pair<Double, Double> {
        try {
            if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val lm = context.getSystemService(LocationManager::class.java)
                val best = lm.getProviders(true).mapNotNull { lm.getLastKnownLocation(it) }.maxByOrNull { it.time }
                if (best != null) return best.latitude to best.longitude
            }
        } catch (_: Exception) { }
        return prefs.lat to prefs.lon
    }

    private fun fetch() {
        val (lat, lon) = coords()
        Thread {
            try {
                val url = URL(
                    "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                        "&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m,is_day" +
                        "&wind_speed_unit=kmh&timezone=auto"
                )
                val c = url.openConnection() as HttpURLConnection
                c.connectTimeout = 8000
                c.readTimeout = 8000
                val cur = JSONObject(c.inputStream.bufferedReader().use { it.readText() }).getJSONObject("current")
                c.disconnect()
                val (emoji, text) = describe(cur.getInt("weather_code"), cur.optInt("is_day", 1) == 1)
                val t = cur.getDouble("temperature_2m").roundToInt()
                val f = cur.getDouble("apparent_temperature").roundToInt()
                val hum = cur.getInt("relative_humidity_2m")
                val w = cur.getDouble("wind_speed_10m").roundToInt()
                main.post {
                    icon.text = emoji
                    temp.text = "$t°C"
                    cond.text = text
                    feels.text = "Feels $f°  ·  Humidity $hum%"
                    wind.text = "Wind $w km/h"
                }
            } catch (e: Exception) {
                main.post { cond.text = "Weather unavailable" }
            }
        }.start()
    }

    private fun describe(code: Int, day: Boolean): Pair<String, String> = when (code) {
        0 -> (if (day) "☀️" else "🌙") to "Clear"
        1, 2 -> (if (day) "⛅" else "☁️") to "Partly cloudy"
        3 -> "☁️" to "Overcast"
        45, 48 -> "🌫️" to "Fog"
        in 51..57 -> "🌦️" to "Drizzle"
        in 61..67, in 80..82 -> "🌧️" to "Rain"
        in 71..77, 85, 86 -> "❄️" to "Snow"
        95, 96, 99 -> "⛈️" to "Thunderstorm"
        else -> "🌡️" to "—"
    }
}
