package com.clickerwoi.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams as LP
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast

data class Titik(val x: Float, val y: Float, val t: Long)
data class Aksi(val titik: List<Titik>, val jeda: Long)

class ClickService : AccessibilityService() {

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

    override fun onServiceConnected() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        buatPanel()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        perekam?.let { wm.removeView(it) }
        if (::panel.isInitialized) wm.removeView(panel)
        super.onDestroy()
    }

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
        panel.addView(tRekam)
        panel.addView(tPutar)
        panel.addView(tUlang)
        panelLp = LP(
            LP.WRAP_CONTENT, LP.WRAP_CONTENT,
            LP.TYPE_ACCESSIBILITY_OVERLAY,
            LP.FLAG_NOT_FOCUSABLE,
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
                LP.TYPE_ACCESSIBILITY_OVERLAY,
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
            Toast.makeText(this, "Tersimpan: ${makro.size} aksi", Toast.LENGTH_SHORT).show()
        }
    }

    private fun togglePutar() {
        if (merekam) return
        if (memutar) {
            berhenti()
            return
        }
        if (makro.isEmpty()) {
            Toast.makeText(this, "Rekam dulu", Toast.LENGTH_SHORT).show()
            return
        }
        memutar = true
        tPutar.text = "STOP"
        jalankan(0)
    }

    private fun berhenti() {
        memutar = false
        handler.removeCallbacksAndMessages(null)
        tPutar.text = "PUTAR"
    }

    private fun jalankan(i: Int) {
        if (!memutar) return
        if (i >= makro.size) {
            if (ulang) handler.postDelayed({ jalankan(0) }, 500) else berhenti()
            return
        }
        val a = makro[i]
        handler.postDelayed({
            if (!memutar) return@postDelayed
            val path = Path()
            a.titik.forEachIndexed { n, t ->
                if (n == 0) path.moveTo(t.x, t.y) else path.lineTo(t.x, t.y)
            }
            val dur = maxOf(a.titik.last().t, 50L)
            val g = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, dur))
                .build()
            val ok = dispatchGesture(g, object : GestureResultCallback() {
                override fun onCompleted(d: GestureDescription?) { jalankan(i + 1) }
                override fun onCancelled(d: GestureDescription?) { jalankan(i + 1) }
            }, null)
            if (!ok) jalankan(i + 1)
        }, if (i == 0) 300L else a.jeda)
    }
}
