package com.example.jetourcluster

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Control screen for the main display: pick the cluster display, start/stop, permissions, simulator. */
class MainActivity : Activity() {

    private lateinit var prefs: ClusterPrefs
    private lateinit var status: TextView
    private lateinit var displayIdField: EditText
    private lateinit var rowsField: EditText
    private lateinit var guides: CheckBox
    private lateinit var sim: CheckBox
    private lateinit var auto: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = ClusterPrefs(this)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 40, 40, 40) }
        status = TextView(this).apply { textSize = 15f }
        root.addView(text("Jetour G700 – Cluster Grid", 22f))
        root.addView(status)

        displayIdField = field("Cluster display id", prefs.displayId.toString(), root)
        rowsField = field("Grid rows (24 columns fixed)", prefs.rows.toString(), root)
        guides = check("Show grid guides (column numbers)", prefs.showGuides, root)
        sim = check("Simulator (fake speed/belt data)", prefs.simulator, root)
        auto = check("Start automatically on boot", prefs.autostart, root)

        root.addView(button("Apply & (re)start cluster") { applyAndStart() })
        root.addView(button("Stop cluster") { ClusterService.stop(this); refresh() })
        root.addView(button("Grant overlay permission") {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        })
        root.addView(button("Grant notification access (media widget)") {
            try { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) } catch (_: Exception) { }
        })
        root.addView(text("Bench test", 18f))
        root.addView(button("Test alarm sound (3 s)") { ClusterService.instance?.testAlarm() })
        root.addView(button("Sim: speed 0 / 60 km/h") { SimVehicle.speed = if (SimVehicle.speed > 0) 0f else 60f; refresh() })
        root.addView(button("Sim: toggle driver belt") { SimVehicle.driver = !SimVehicle.driver; refresh() })
        root.addView(button("Sim: toggle passenger belt") { SimVehicle.passenger = !SimVehicle.passenger; refresh() })
        root.addView(button("Sim: toggle passenger seat occupied") { SimVehicle.occupied = !SimVehicle.occupied; refresh() })

        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun applyAndStart() {
        prefs.displayId = displayIdField.text.toString().toIntOrNull() ?: prefs.displayId
        prefs.rows = rowsField.text.toString().toIntOrNull() ?: prefs.rows
        prefs.showGuides = guides.isChecked
        prefs.simulator = sim.isChecked
        prefs.autostart = auto.isChecked
        ClusterService.stop(this)
        window.decorView.postDelayed({ ClusterService.start(this); refresh() }, 400)
    }

    private fun refresh() {
        val dm = getSystemService(DisplayManager::class.java)
        val listenerOn = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            ?.contains(packageName) == true
        val displays = dm.displays.joinToString("\n") { "  #${it.displayId}  ${it.name}  ${it.mode.physicalWidth}x${it.mode.physicalHeight}" }
        status.text = buildString {
            appendLine("Service: ${if (ClusterService.isRunning()) "RUNNING" else "stopped"}")
            appendLine("Overlay permission: ${if (Settings.canDrawOverlays(this@MainActivity)) "granted" else "MISSING"}")
            appendLine("Notification access: ${if (listenerOn) "granted" else "MISSING"}")
            appendLine("Sim: speed=${SimVehicle.speed.toInt()} driver=${SimVehicle.driver} pass=${SimVehicle.passenger} occupied=${SimVehicle.occupied}")
            appendLine("Displays:")
            append(displays)
        }
    }

    private fun text(t: String, size: Float) = TextView(this).apply { text = t; textSize = size; setPadding(0, 24, 0, 8) }

    private fun button(t: String, onClick: () -> Unit) = Button(this).apply {
        text = t
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun check(t: String, v: Boolean, parent: LinearLayout) =
        CheckBox(this).apply { text = t; isChecked = v; parent.addView(this) }

    private fun field(label: String, v: String, parent: LinearLayout): EditText {
        parent.addView(TextView(this).apply { text = label; setPadding(0, 16, 0, 0) })
        return EditText(this).apply { setText(v); inputType = InputType.TYPE_CLASS_NUMBER; parent.addView(this) }
    }
}
