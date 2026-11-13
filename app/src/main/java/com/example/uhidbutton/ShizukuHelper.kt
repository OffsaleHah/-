package com.example.uhidbutton

import android.content.pm.PackageManager
import android.util.Log
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuRemoteProcess
import java.lang.reflect.Method

object ShizukuHelper {
    private const val TAG = "ShizukuHelper"
    private const val REQUEST_CODE = 1001
    private var hidProcess: ShizukuRemoteProcess? = null

    fun isShizukuAvailable(): Boolean = Shizuku.pingBinder()

    fun hasPermission(): Boolean {
        return isShizukuAvailable() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }

    fun requestPermission() {
        if (isShizukuAvailable() && !hasPermission()) {
            Shizuku.requestPermission(REQUEST_CODE)
        }
    }

    /**
     * Запускает фоновый процесс Hid из classes.dex.
     * Возвращает true, если процесс успешно запущен.
     */
    fun startHidServer(): Boolean {
        if (hidProcess != null) {
            Log.d(TAG, "Hid server is already running.")
            return true
        }

        return try {
            // Точная команда, которая запускает HID-сервер.
            // Путь к classes.dex: /data/local/tmp/classes.dex
            val command = arrayOf(
                "app_process",
                "-Djava.class.path=/data/local/tmp/classes.dex",
                "/",
                "com.android.commands.hid.Hid"
            )

            val shizukuClass = Class.forName("rikka.shizuku.Shizuku")
            val method: Method = shizukuClass.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            hidProcess = method.invoke(null, command, null, null) as ShizukuRemoteProcess
            Log.i(TAG, "Hid server process started.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Hid server: ${e.javaClass.simpleName}: ${e.message}", e)
            false
        }
    }

    /**
     * Отправляет команду нажатия клавиши в фоновый HID-сервер.
     * ВАЖНО: Этот метод не запускает новый процесс, а пишет в stdin уже запущенного.
     */
    fun sendKeyPress(keyCode: Int): Boolean {
        val process = hidProcess ?: run {
            Log.e(TAG, "Hid server is not running. Call startHidServer() first.")
            return false
        }

        return try {
            // Команда для HID-сервера: нажать и отпустить клавишу.
            // Формат: keyboard press <hid_key_code>
            val command = "keyboard press $keyCode\n"
            process.outputStream.write(command.toByteArray())
            process.outputStream.flush()
            Log.d(TAG, "Sent command: $command")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send command: ${e.message}", e)
            false
        }
    }

    /**
     * Останавливает фоновый HID-сервер.
     */
    fun stopHidServer() {
        hidProcess?.destroy()
        hidProcess = null
        Log.i(TAG, "Hid server stopped.")
    }
}
