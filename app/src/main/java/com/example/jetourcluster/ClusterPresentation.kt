package com.example.jetourcluster

import android.app.Presentation
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Display
import android.view.ViewGroup
import android.view.WindowManager

/** Full-screen window on the instrument-cluster display, hosting the grid. */
class ClusterPresentation(
    outer: Context,
    display: Display,
    private val prefs: ClusterPrefs
) : Presentation(outer, display) {

    lateinit var belt: BeltWidget
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.apply {
            setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            addFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            )
            setBackgroundDrawableResource(android.R.color.black)
        }
        val grid = ClusterGrid(context, ClusterPrefs.COLS, prefs.rows).apply {
            setBackgroundColor(Color.BLACK)
            showGuides = prefs.showGuides
        }
        belt = ClusterLayout.build(context, grid, prefs)
        setContentView(
            grid,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
    }
}
