package com.nashmi.nashmiserver
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("nashmi", Context.MODE_PRIVATE)
            if ((prefs.getString("token", null)?.length ?: 0) >= 32) {
                val service = Intent(context, GatewayService::class.java)
                if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(service) else context.startService(service)
            }
        }
    }
}