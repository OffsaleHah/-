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

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            if (!ShizukuHelper.hasPermission()) {
                Toast.makeText(this, "Сначала разрешите Shizuku", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (ShizukuHelper.findUhidApk(this) == null) {
                Toast.makeText(this, "Установите APK uhid.purejava", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (ShizukuHelper.startHidServer(this)) {
                Toast.makeText(this, "HID-сервер запущен (4 устройства)", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Не удалось запустить HID-сервер", Toast.LENGTH_LONG).show()
            }
        }

        // Тестовые команды из меню uhid.purejava
        findViewById<Button>(R.id.btnTestKeyboard).setOnClickListener {
            sendCmd("k", "Набрать helloworld")
        }
        findViewById<Button>(R.id.btnTestMouse).setOnClickListener {
            sendCmd("m", "Движение мыши")
        }
        findViewById<Button>(R.id.btnTestTouch).setOnClickListener {
            sendCmd("t", "Квадратный жест")
        }

        findViewById<Button>(R.id.btnStop).setOnClickListener {
            ShizukuHelper.stopHidServer()
            Toast.makeText(this, "Остановлено", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendCmd(cmd: String, description: String) {
        if (ShizukuHelper.sendCommand(cmd)) {
            Toast.makeText(this, "Отправлено: $description", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Сервер не запущен", Toast.LENGTH_LONG).show()
        }
    }
}
