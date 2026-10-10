package com.example.jetourcluster

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup

/**
 * A fixed [cols] x [rows] grid. Widgets are placed by (col, row) and sized by (colSpan, rowSpan).
 * With 24 columns on a 4032 px wide strip every column is 168 px.
 */
class ClusterGrid(context: Context, private val cols: Int, private val rows: Int) : ViewGroup(context) {

    private class Cell(val col: Int, val row: Int, val colSpan: Int, val rowSpan: Int)

    private val density = resources.displayMetrics.density
    private val cells = LinkedHashMap<View, Cell>()
    private val gapPx = 5f * density
    private val guide = Paint().apply {
        color = 0x66FFFFFF
        strokeWidth = 1f
        style = Paint.Style.STROKE
        textSize = 11f * density
    }

    var showGuides = false
        set(v) { field = v; invalidate() }

    init { setWillNotDraw(false) }

    fun addWidget(v: View, col: Int, row: Int, colSpan: Int = 1, rowSpan: Int = 1) {
        require(col >= 0 && row >= 0 && col + colSpan <= cols && row + rowSpan <= rows) {
            "Widget (col=$col,row=$row,span=${colSpan}x$rowSpan) does not fit in ${cols}x$rows grid"
        }
        cells[v] = Cell(col, row, colSpan, rowSpan)
        addView(v)
    }

    private fun bounds(c: Cell, w: Int, h: Int): Rect {
        val cw = w / cols.toFloat()
        val rh = h / rows.toFloat()
        return Rect(
            (c.col * cw + gapPx / 2).toInt(),
            (c.row * rh + gapPx / 2).toInt(),
            ((c.col + c.colSpan) * cw - gapPx / 2).toInt(),
            ((c.row + c.rowSpan) * rh - gapPx / 2).toInt()
        )
    }

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        val w = MeasureSpec.getSize(wSpec)
        val h = MeasureSpec.getSize(hSpec)
        setMeasuredDimension(w, h)
        cells.forEach { (v, c) ->
            val r = bounds(c, w, h)
            v.measure(
                MeasureSpec.makeMeasureSpec(r.width(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(r.height(), MeasureSpec.EXACTLY)
            )
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val w = r - l
        val h = b - t
        cells.forEach { (v, c) ->
            val rc = bounds(c, w, h)
            v.layout(rc.left, rc.top, rc.right, rc.bottom)
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (!showGuides) return
        val cw = width / cols.toFloat()
        val rh = height / rows.toFloat()
        for (i in 0..cols) {
            val x = i * cw
            canvas.drawLine(x, 0f, x, height.toFloat(), guide)
            if (i < cols) canvas.drawText("${i + 1}", x + 4f * density, guide.textSize + 2f, guide)
        }
        for (j in 0..rows) canvas.drawLine(0f, j * rh, width.toFloat(), j * rh, guide)
    }
}
