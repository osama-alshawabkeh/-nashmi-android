package com.nashmi.nashmiclient
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.*
class MainActivity : Activity() {
    private lateinit var host: EditText
    private lateinit var device: EditText
    private lateinit var token: EditText
    private lateinit var local: EditText
    private lateinit var remote: EditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val box = LinearLayout(this).apply { orientation = 1; setPadding(32,36,32,24) }
        box.addView(TextView(this).apply { text = "NASHMI CONNECT"; textSize = 25f; gravity = Gravity.CENTER })
        host = field("Server public IPv4 or hostname")
        device = field("Device ID (letters/numbers)")
        token = field("Shared secret (32+ characters)", true)
        local = field("Local service port (e.g. 8080)").apply { setText("8080") }
        remote = field("Public port (18000-18999)").apply { setText("18001") }
        listOf(host, device, token, local, remote).forEach { box.addView(it) }
        box.addView(Button(this).apply { text = "CONNECT"; setOnClickListener {
            val h=host.text.toString().trim(); val d=device.text.toString().trim(); val t=token.text.toString()
            val lp=local.text.toString().toIntOrNull() ?: 0; val rp=remote.text.toString().toIntOrNull() ?: 0
            if (h.isBlank() || !d.matches(Regex("[A-Za-z0-9_-]{1,32}")) || t.length<32 || lp !in 1..65535 || rp !in 18000..18999) {
                Toast.makeText(this@MainActivity,"Check host, device ID, token and ports",Toast.LENGTH_LONG).show(); return@setOnClickListener
            }
            getSharedPreferences("nashmi", MODE_PRIVATE).edit().putString("host",h).putString("device",d).putString("token",t).putInt("local",lp).putInt("remote",rp).apply()
            startForegroundService(Intent(this@MainActivity, TunnelService::class.java))
        }})
        box.addView(Button(this).apply { text = "DISCONNECT"; setOnClickListener { stopService(Intent(this@MainActivity, TunnelService::class.java)) }})
        box.addView(TextView(this).apply { text = "The local service must already be running on this phone. Forward TCP 7000, 7001 and the selected public port on the central router."; textSize = 13f; setPadding(0,20,0,0) })
        setContentView(ScrollView(this).apply { addView(box) })
    }
    private fun field(hintText: String, password: Boolean = false) = EditText(this).apply {
        hint = hintText
        if (password) inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
    }
}