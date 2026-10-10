package com.example.jetourcluster

import android.content.Context

/**
 * Where each widget sits on the 24-column grid. Edit this one function to move/resize/add widgets.
 *
 *   cols  0..5   (6)  weather
 *   cols  6..15 (10)  media
 *   cols 16..23  (8)  seat belts
 */
object ClusterLayout {
    fun build(ctx: Context, grid: ClusterGrid, prefs: ClusterPrefs): BeltWidget {
        val rows = prefs.rows
        grid.addWidget(WeatherWidget(ctx, prefs), col = 0, row = 0, colSpan = 6, rowSpan = rows)
        grid.addWidget(MediaWidget(ctx), col = 6, row = 0, colSpan = 10, rowSpan = rows)
        val belt = BeltWidget(ctx)
        grid.addWidget(belt, col = 16, row = 0, colSpan = 8, rowSpan = rows)
        return belt
    }
}
