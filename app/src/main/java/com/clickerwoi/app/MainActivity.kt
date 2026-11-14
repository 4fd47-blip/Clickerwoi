package com.clickerwoi.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }

        val info = TextView(this).apply {
            text = "1. Aktifkan layanan Clickerwoi di Aksesibilitas\n" +
                "2. Panel kecil muncul di sisi kiri layar\n" +
                "3. Buka aplikasi target, tekan REKAM, lakukan ketukan atau geseran, lalu STOP REKAM\n" +
                "4. Tekan PUTAR untuk mengulang. ULANG: HIDUP untuk berulang terus, STOP untuk berhenti."
            textSize = 16f
        }

        val tombol = Button(this).apply {
            text = "Buka Pengaturan Aksesibilitas"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        layout.addView(info)
        layout.addView(tombol)
        setContentView(layout)
    }
}
