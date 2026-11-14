package com.clickerwoi.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }

        val info = TextView(this).apply {
            text = "1. Izinkan tampil di atas aplikasi lain\n" +
                "2. Pastikan Shizuku sedang berjalan\n" +
                "3. Tampilkan panel, buka game, REKAM lalu PUTAR\n" +
                "KELUAR di panel untuk menutup."
            textSize = 16f
        }

        val tombolIzin = Button(this).apply {
            text = "1. Izinkan tampil di atas aplikasi lain"
            setOnClickListener {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            }
        }

        val tombolMulai = Button(this).apply {
            text = "2. Tampilkan Panel Makro"
            setOnClickListener { mulai() }
        }

        layout.addView(info)
        layout.addView(tombolIzin)
        layout.addView(tombolMulai)
        setContentView(layout)
    }

    private fun mulai() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Izinkan tampil di atas aplikasi lain dulu", Toast.LENGTH_LONG).show()
            return
        }
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        ContextCompat.startForegroundService(this, Intent(this, PanelService::class.java))
    }
}
