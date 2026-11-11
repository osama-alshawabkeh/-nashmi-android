package com.nashmi.nashmiclient
import android.app.*
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import java.io.*
import java.net.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
class TunnelService : Service() {
    private val running = AtomicBoolean(false)
    private val pool = Executors.newCachedThreadPool()
    private var control: Socket? = null
    private var wake: PowerManager.WakeLock? = null
    override fun onCreate() {
        super.onCreate()
        val ch = NotificationChannel("nashmi_client", "Nashmi Connect", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        startForeground(72, Notification.Builder(this, "nashmi_client").setContentTitle("Nashmi Connect").setContentText("Maintaining reverse tunnel connection").setSmallIcon(android.R.drawable.stat_sys_upload_done).build())
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (running.compareAndSet(false, true)) {
            val p = getSharedPreferences("nashmi", MODE_PRIVATE)
            val host=p.getString("host","") ?: ""; val device=p.getString("device","") ?: ""; val token=p.getString("token","") ?: ""
            val local=p.getInt("local",0); val remote=p.getInt("remote",0)
            if (host.isBlank() || device.isBlank() || token.length<32 || local !in 1..65535 || remote !in 18000..18999) { running.set(false); stopSelf(); return START_NOT_STICKY }
            val pm=getSystemService(POWER_SERVICE) as PowerManager
            wake=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"Nashmi:Client").apply { acquire() }
            pool.execute { connectLoop(host,device,token,local,remote) }
        }
        return START_STICKY
    }
    private fun connectLoop(host:String,device:String,token:String,local:Int,remote:Int) {
        while(running.get()) {
            try {
                val s=Socket(); s.connect(InetSocketAddress(host,7000),15000); s.tcpNoDelay=true; control=s
                val input=s.getInputStream(); val writer=BufferedWriter(OutputStreamWriter(s.getOutputStream(),Charsets.UTF_8))
                writer.write("REGISTER|$device|$remote|$token\n"); writer.flush()
                if(readLine(input)!="OK") throw IOException("Registration rejected")
                while(running.get() && !s.isClosed) {
                    when(val line=readLine(input) ?: break) {
                        "PING" -> { writer.write("PONG\n"); writer.flush() }
                        else -> if(line.startsWith("OPEN|")) { val id=line.substringAfter("OPEN|"); pool.execute { openData(host,token,id,local) } }
                    }
                }
            } catch (_:Exception) {}
            try { control?.close() } catch (_:Exception) {}
            if(running.get()) try { Thread.sleep(3000) } catch (_:Exception) {}
        }
    }
    private fun openData(host:String,token:String,id:String,localPort:Int) {
        var upstream:Socket?=null; var local:Socket?=null
        try {
            upstream=Socket(); upstream.connect(InetSocketAddress(host,7001),15000)
            val w=BufferedWriter(OutputStreamWriter(upstream.getOutputStream(),Charsets.UTF_8)); w.write("DATA|$id|$token\n"); w.flush()
            local=Socket(); local.connect(InetSocketAddress("127.0.0.1",localPort),10000)
            relay(local,upstream)
        } catch (_:Exception) { try { local?.close() } catch (_:Exception) {}; try { upstream?.close() } catch (_:Exception) {} }
    }
    private fun relay(a:Socket,b:Socket) {
        val latch=CountDownLatch(2)
        fun pipe(src:Socket,dst:Socket) { try { src.getInputStream().copyTo(dst.getOutputStream()); dst.shutdownOutput() } catch (_:Exception) {}; latch.countDown() }
        pool.execute { pipe(a,b) }; pool.execute { pipe(b,a) }
        try { latch.await(10,TimeUnit.MINUTES) } catch (_:Exception) {}
        try { a.close() } catch (_:Exception) {}; try { b.close() } catch (_:Exception) {}
    }
    private fun readLine(input:InputStream):String? {
        val out=ByteArrayOutputStream()
        while(out.size()<4096) { val b=input.read(); if(b<0) return if(out.size()==0) null else out.toString("UTF-8"); if(b==10) return out.toString("UTF-8").trimEnd('\r'); out.write(b) }
        return null
    }
    override fun onDestroy() { running.set(false); try { control?.close() } catch (_:Exception) {}; try { wake?.release() } catch (_:Exception) {}; pool.shutdownNow(); super.onDestroy() }
    override fun onBind(intent:Intent?):IBinder?=null
}