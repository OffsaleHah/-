package com.example.uhidbutton

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Запрашиваем разрешение Shizuku при старте
        if (!ShizukuHelper.hasPermission()) {
            ShizukuHelper.requestPermission()
        }

        val btnPressSpace = findViewById<Button>(R.id.btnPressSpace)
        btnPressSpace.setOnClickListener {
            if (!ShizukuHelper.hasPermission()) {
                Toast.makeText(this, "Сначала предоставьте разрешение Shizuku", Toast.LENGTH_LONG).show()
                ShizukuHelper.requestPermission()
                return@setOnClickListener
            }

            // HID-код пробела = 44 (0x2C)
            val success = ShizukuHelper.sendKeyPress(44)

            if (success) {
                Toast.makeText(this, "Пробел отправлен через UHID", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Ошибка: не удалось выполнить uinput", Toast.LENGTH_LONG).show()
            }
        }
    }
}
