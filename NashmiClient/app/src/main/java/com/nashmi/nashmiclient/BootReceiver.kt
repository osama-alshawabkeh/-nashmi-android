package com.nashmi.nashmiclient
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val p = context.getSharedPreferences("nashmi", Context.MODE_PRIVATE)
            if ((p.getString("token", "") ?: "").length >= 32) {
                val i = Intent(context, TunnelService::class.java)
                if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i) else context.startService(i)
            }
        }
    }
}