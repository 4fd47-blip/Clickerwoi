package com.clickerwoi.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }

        val info = TextView(this).apply {
            text = "1. Aktifkan layanan Clickerwoi di Aksesibilitas\n2. Kembali ke sini, tekan Jalankan Makro"
            textSize = 16f
        }

        val tombolAksesibilitas = Button(this).apply {
            text = "Buka Pengaturan Aksesibilitas"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        val tombolMakro = Button(this).apply {
            text = "Jalankan Makro"
            setOnClickListener { jalankanMakro() }
        }

        layout.addView(info)
        layout.addView(tombolAksesibilitas)
        layout.addView(tombolMakro)
        setContentView(layout)
    }

    private fun jalankanMakro() {
        val service = ClickService.instance
        if (service == null) {
            Toast.makeText(this, "Aktifkan layanan di Aksesibilitas dulu", Toast.LENGTH_LONG).show()
            return
        }

        // Makro contoh: urutan aksi dengan jeda (milidetik)
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({ service.tap(540f, 1200f) }, 3000)
        handler.postDelayed({ service.tap(540f, 1200f) }, 3500)
        handler.postDelayed({ service.swipe(540f, 1500f, 540f, 800f, 300) }, 4000)
        handler.postDelayed({ service.tap(300f, 600f) }, 4800)

        Toast.makeText(this, "Makro mulai dalam 3 detik, pindah ke layar target", Toast.LENGTH_LONG).show()
    }
}
