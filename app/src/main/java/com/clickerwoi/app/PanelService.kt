package com.clickerwoi.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams as LP
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.NotificationCompat
import rikka.shizuku.Shizuku
import java.util.Locale

data class Titik(val x: Float, val y: Float, val t: Long)
data class Aksi(val titik: List<Titik>, val jeda: Long)

class PanelService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var wm: WindowManager
    private lateinit var panel: LinearLayout
    private lateinit var panelLp: LP
    private lateinit var tRekam: Button
    private lateinit var tPutar: Button
    private lateinit var tUlang: Button
    private var perekam: View? = null
    private val makro = mutableListOf<Aksi>()
    private var merekam = false
    private var memutar = false
    private var ulang = false
    private var waktuTerakhir = 0L
    private var proses: Process? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onCreate() {
        super.onCreate()
        mulaiForeground()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        buatPanel()
    }

    override fun onDestroy() {
        proses?.destroy()
        handler.removeCallbacksAndMessages(null)
        if (::wm.isInitialized) {
            perekam?.let { wm.removeView(it) }
            if (::panel.isInitialized) wm.removeView(panel)
        }
        super.onDestroy()
    }

    private fun mulaiForeground() {
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel("clickerwoi", "Clickerwoi", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
        val n = NotificationCompat.Builder(this, "clickerwoi")
            .setContentTitle("Clickerwoi aktif")
            .setContentText("Panel makro sedang tampil")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }
    }

    private fun toast(s: String) =
        Toast.makeText(this, s, Toast.LENGTH_LONG).show()

    private fun tombol(teks: String, aksi: () -> Unit) = Button(this).apply {
        text = teks
        textSize = 11f
        setOnClickListener { aksi() }
    }

    private fun buatPanel() {
        panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.argb(200, 20, 20, 20))
            setPadding(6, 6, 6, 6)
        }
        tRekam = tombol("REKAM") { toggleRekam() }
        tPutar = tombol("PUTAR") { togglePutar() }
        tUlang = tombol("ULANG: MATI") {
            ulang = !ulang
            tUlang.text = if (ulang) "ULANG: HIDUP" else "ULANG: MATI"
        }
        val tKeluar = tombol("KELUAR") { stopSelf() }
        panel.addView(tRekam)
        panel.addView(tPutar)
        panel.addView(tUlang)
        panel.addView(tKeluar)
        panelLp = LP(
            LP.WRAP_CONTENT, LP.WRAP_CONTENT,
            LP.TYPE_APPLICATION_OVERLAY,
            LP.FLAG_NOT_FOCUSABLE or LP.FLAG_SPLIT_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.START or Gravity.CENTER_VERTICAL }
        wm.addView(panel, panelLp)
    }

    private fun toggleRekam() {
        if (memutar) return
        if (!merekam) {
            makro.clear()
            waktuTerakhir = 0L
            val v = object : View(this) {
                var awal = 0L
                var pts = mutableListOf<Titik>()
                override fun onTouchEvent(e: MotionEvent): Boolean {
                    when (e.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            awal = e.eventTime
                            pts = mutableListOf(Titik(e.rawX, e.rawY, 0L))
                        }
                        MotionEvent.ACTION_MOVE ->
                            pts.add(Titik(e.rawX, e.rawY, e.eventTime - awal))
                        MotionEvent.ACTION_UP -> {
                            pts.add(Titik(e.rawX, e.rawY, e.eventTime - awal))
                            val jeda = if (waktuTerakhir == 0L) 0L else awal - waktuTerakhir
                            makro.add(Aksi(pts.toList(), jeda))
                            waktuTerakhir = e.eventTime
                        }
                    }
                    return true
                }
            }
            v.setBackgroundColor(Color.argb(60, 255, 0, 0))
            val lp = LP(
                LP.MATCH_PARENT, LP.MATCH_PARENT,
                LP.TYPE_APPLICATION_OVERLAY,
                LP.FLAG_NOT_FOCUSABLE or LP.FLAG_LAYOUT_IN_SCREEN or LP.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            )
            wm.addView(v, lp)
            perekam = v
            wm.removeView(panel)
            wm.addView(panel, panelLp)
            merekam = true
            tRekam.text = "STOP REKAM"
        } else {
            perekam?.let { wm.removeView(it) }
            perekam = null
            merekam = false
            tRekam.text = "REKAM"
            toast("Tersimpan: ${makro.size} aksi")
        }
    }

    private fun siapShizuku(): Boolean {
        if (!Shizuku.pingBinder()) {
            toast("Shizuku belum aktif. Buka aplikasi Shizuku dan mulai dulu.")
            return false
        }
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(1)
            toast("Izinkan Clickerwoi di Shizuku, lalu tekan PUTAR lagi")
            return false
        }
        return true
    }

    private fun shell(cmd: String): Process? {
        return try {
            val m = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            m.isAccessible = true
            m.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
        } catch (e: Exception) {
            null
        }
    }

    private fun buatSkrip(): String {
        val sb = StringBuilder()
        if (ulang) sb.append("while true; do ")
        makro.forEachIndexed { i, a ->
            val awal = a.titik.first()
            val akhir = a.titik.last()
            val jeda = if (i == 0) 300L else a.jeda
            val dur = maxOf(akhir.t, 50L)
            sb.append("sleep ")
                .append(String.format(Locale.US, "%.3f", jeda / 1000.0))
                .append("; ")
            sb.append("input swipe ")
                .append(awal.x.toInt()).append(" ")
                .append(awal.y.toInt()).append(" ")
                .append(akhir.x.toInt()).append(" ")
                .append(akhir.y.toInt()).append(" ")
                .append(dur).append("; ")
        }
        if (ulang) sb.append("sleep 0.5; done")
        return sb.toString()
    }

    private fun togglePutar() {
        if (merekam) return
        if (memutar) {
            berhenti()
            return
        }
        if (makro.isEmpty()) {
            toast("Rekam dulu")
            return
        }
        if (!siapShizuku()) return
        val p = shell(buatSkrip())
        if (p == null) {
            toast("Gagal menjalankan lewat Shizuku")
            return
        }
        proses = p
        memutar = true
        tPutar.text = "STOP"
        Thread {
            try { p.waitFor() } catch (e: Exception) {}
            handler.post { if (proses === p) berhenti() }
        }.start()
    }

    private fun berhenti() {
        memutar = false
        proses?.destroy()
        proses = null
        tPutar.text = "PUTAR"
    }
}
