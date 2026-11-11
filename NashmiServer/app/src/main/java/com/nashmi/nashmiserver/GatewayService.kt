package com.nashmi.nashmiserver
import android.app.*
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import java.io.*
import java.net.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
class GatewayService : Service() {
    private val running = AtomicBoolean(false)
    private val pool = Executors.newCachedThreadPool()
    private val clients = ConcurrentHashMap<String, Client>()
    private val pending = ConcurrentHashMap<String, Socket>()
    private var controlServer: ServerSocket? = null
    private var dataServer: ServerSocket? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var token = ""
    data class Client(val id: String, val port: Int, val writer: BufferedWriter)
    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel("nashmi_gateway", "Nashmi Gateway", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val notification = Notification.Builder(this, "nashmi_gateway").setContentTitle("Nashmi Server is running").setContentText("Reverse tunnel gateway active").setSmallIcon(android.R.drawable.stat_sys_upload_done).build()
        startForeground(71, notification)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (running.compareAndSet(false, true)) {
            val prefs = getSharedPreferences("nashmi", MODE_PRIVATE)
            token = prefs.getString("token", "") ?: ""
            val port = prefs.getInt("port", 7000)
            if (token.length < 32 || port !in 1024..65535) { running.set(false); stopSelf(); return START_NOT_STICKY }
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Nashmi:Gateway").apply { acquire() }
            pool.execute { startListeners(port) }
        }
        return START_STICKY
    }
    private fun startListeners(port: Int) {
        try {
            controlServer = ServerSocket(port, 50, InetAddress.getByName("0.0.0.0"))
            dataServer = ServerSocket(7001, 100, InetAddress.getByName("0.0.0.0"))
            pool.execute { acceptData() }
            while (running.get()) {
                val socket = controlServer!!.accept()
                pool.execute { handleControl(socket) }
            }
        } catch (_: Exception) { if (running.get()) { running.set(false); stopSelf() } }
    }
    private fun handleControl(socket: Socket) {
        var listener: ServerSocket? = null
        try {
            socket.tcpNoDelay = true
            val input = socket.getInputStream()
            val output = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))
            val hello = readLine(input) ?: return
            val parts = hello.split("|")
            if (parts.size != 4 || parts[0] != "REGISTER" || parts[3] != token) return
            val id = parts[1]
            val remotePort = parts[2].toIntOrNull() ?: return
            if (!id.matches(Regex("[A-Za-z0-9_-]{1,32}")) || remotePort !in 18000..18999) return
            val client = Client(id, remotePort, output)
            clients[id]?.let { try { it.writer.close() } catch (_: Exception) {} }
            clients[id] = client
            output.write("OK\n"); output.flush()
            listener = ServerSocket(remotePort, 50, InetAddress.getByName("0.0.0.0"))
            val activeListener = listener
            pool.execute {
                try {
                    while (running.get() && clients[id] === client) {
                        val external = activeListener.accept()
                        val cid = java.util.UUID.randomUUID().toString().replace("-", "")
                        pending[cid] = external
                        synchronized(output) { output.write("OPEN|$cid\n"); output.flush() }
                        pool.execute { try { Thread.sleep(30000); pending.remove(cid)?.close() } catch (_: Exception) {} }
                    }
                } catch (_: Exception) { } finally { try { activeListener.close() } catch (_: Exception) {} }
            }
            while (running.get() && !socket.isClosed) {
                val line = readLine(input) ?: break
                if (line == "PING") synchronized(output) { output.write("PONG\n"); output.flush() }
            }
        } catch (_: Exception) {
        } finally {
            val id = clients.entries.firstOrNull { it.value.writer === runCatching { BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8)) }.getOrNull() }?.key
            if (id != null) clients.remove(id)
            try { listener?.close() } catch (_: Exception) {}
            try { socket.close() } catch (_: Exception) {}
        }
    }
    private fun acceptData() {
        while (running.get()) {
            try {
                val data = dataServer!!.accept()
                pool.execute {
                    try {
                        val header = readLine(data.getInputStream()) ?: return@execute
                        val p = header.split("|")
                        if (p.size != 3 || p[0] != "DATA" || p[2] != token) return@execute
                        val external = pending.remove(p[1]) ?: return@execute
                        relay(external, data)
                    } catch (_: Exception) { try { data.close() } catch (_: Exception) {} }
                }
            } catch (_: Exception) { if (!running.get()) break }
        }
    }
    private fun relay(a: Socket, b: Socket) {
        val done = CountDownLatch(2)
        fun pipe(src: Socket, dst: Socket) {
            try { src.getInputStream().copyTo(dst.getOutputStream()); dst.shutdownOutput() } catch (_: Exception) {}
            done.countDown()
        }
        pool.execute { pipe(a, b) }; pool.execute { pipe(b, a) }
        try { done.await(10, TimeUnit.MINUTES) } catch (_: Exception) {}
        try { a.close() } catch (_: Exception) {}; try { b.close() } catch (_: Exception) {}
    }
    private fun readLine(input: InputStream): String? {
        val out = ByteArrayOutputStream()
        while (out.size() < 4096) {
            val b = input.read()
            if (b < 0) return if (out.size() == 0) null else out.toString("UTF-8")
            if (b == 10) return out.toString("UTF-8").trimEnd('\r')
            out.write(b)
        }
        return null
    }
    override fun onDestroy() {
        running.set(false)
        try { controlServer?.close() } catch (_: Exception) {}
        try { dataServer?.close() } catch (_: Exception) {}
        clients.clear()
        pending.values.forEach { try { it.close() } catch (_: Exception) {} }; pending.clear()
        try { wakeLock?.release() } catch (_: Exception) {}
        pool.shutdownNow()
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}