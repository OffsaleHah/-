package com.example.uhidbutton

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!ShizukuHelper.hasPermission()) {
            ShizukuHelper.requestPermission()
        }

        val btnPressSpace = findViewById<Button>(R.id.btnPressSpace)

        // Кнопка для запуска/остановки HID-сервера
        val btnToggleServer = findViewById<Button>(R.id.btnToggleServer)
        btnToggleServer.setOnClickListener {
            if (!ShizukuHelper.hasPermission()) {
                Toast.makeText(this, "Сначала предоставьте разрешение Shizuku", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            // Здесь можно добавить логику переключения, но для простоты
            // просто запускаем сервер при каждом нажатии.
            if (ShizukuHelper.startHidServer()) {
                Toast.makeText(this, "HID-сервер запущен", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Не удалось запустить HID-сервер", Toast.LENGTH_LONG).show()
            }
        }

        btnPressSpace.setOnClickListener {
            // HID-код пробела = 44
            if (ShizukuHelper.sendKeyPress(44)) {
                Toast.makeText(this, "Команда 'пробел' отправлена", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Ошибка: HID-сервер не запущен", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ShizukuHelper.stopHidServer()
    }
}
