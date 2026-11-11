package com.nashmi.nashmiserver
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.*
class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var port: EditText
    private lateinit var token: EditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val box = LinearLayout(this).apply { orientation = 1; setPadding(36, 40, 36, 24) }
        box.addView(TextView(this).apply { text = "NASHMI SERVER"; textSize = 25f; gravity = Gravity.CENTER })
        box.addView(TextView(this).apply { text = "Reverse Tunnel Gateway • MVP"; textSize = 14f; gravity = Gravity.CENTER })
        port = EditText(this).apply { hint = "Control port (7000)"; setText("7000"); inputType = 2 }
        token = EditText(this).apply { hint = "Shared secret (32+ characters)"; inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD }
        status = TextView(this).apply { text = "Status: Stopped"; textSize = 16f; setPadding(0,24,0,24) }
        box.addView(port); box.addView(token); box.addView(status)
        box.addView(Button(this).apply { text = "START SERVER"; setOnClickListener {
            val secret = token.text.toString()
            if (secret.length < 32) { status.text = "Use a random secret of at least 32 characters"; return@setOnClickListener }
            getSharedPreferences("nashmi", MODE_PRIVATE).edit().putInt("port", port.text.toString().toIntOrNull() ?: 7000).putString("token", secret).apply()
            startForegroundService(Intent(this@MainActivity, GatewayService::class.java))
            status.text = "Starting…"
        }})
        box.addView(Button(this).apply { text = "STOP SERVER"; setOnClickListener {
            stopService(Intent(this@MainActivity, GatewayService::class.java)); status.text = "Status: Stopped"
        }})
        box.addView(TextView(this).apply { text = "Ports: TCP 7000 (control), TCP 7001 (data), TCP 18000–18999 (published services). Forward them to this phone in your router."; textSize = 13f; setPadding(0,24,0,0) })
        setContentView(ScrollView(this).apply { addView(box) })
    }
}