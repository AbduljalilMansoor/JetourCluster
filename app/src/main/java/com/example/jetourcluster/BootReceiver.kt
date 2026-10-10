package com.example.jetourcluster

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && ClusterPrefs(context).autostart) {
            ClusterService.start(context)
        }
    }
}
