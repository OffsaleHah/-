package com.example.uhidbutton

import android.content.pm.PackageManager
import android.util.Log
import rikka.shizuku.Shizuku

object ShizukuHelper {
    private const val TAG = "ShizukuHelper"
    private const val REQUEST_CODE = 1001

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
     * Выполняет shell-команду через Shizuku.
     * ВАЖНО: Shizuku.newProcess помечен как deprecated, но всё ещё работает.
     * Если в вашей версии API его нет, используйте reflection (см. ниже).
     */
    fun runCommand(command: Array<String>): Boolean {
        return try {
            // Прямой вызов (может не работать в новых версиях Shizuku API)
            val process = Shizuku.newProcess(command, null, null)
            process.waitFor()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Direct newProcess failed, trying reflection", e)
            runCommandViaReflection(command)
        }
    }

    /**
     * Fallback через reflection — работает, если newProcess скрыт в API.
     */
    private fun runCommandViaReflection(command: Array<String>): Boolean {
        return try {
            val shizukuClass = Class.forName("rikka.shizuku.Shizuku")
            val method = shizukuClass.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                Array<String>::class.java
            )
            method.isAccessible = true
            val process = method.invoke(null, command, null, null)
            (process as? Process)?.waitFor()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Reflection newProcess also failed", e)
            false
        }
    }

    /**
     * Отправляет нажатие клавиши через `uinput`.
     * @param keyCode HID-код клавиши (например, 44 для пробела).
     */
    fun sendKeyPress(keyCode: Int): Boolean {
        // Формат команды uinput: uinput -K -d <key> -u <key>
        val command = arrayOf(
            "uinput", "-K",
            "-d", keyCode.toString(),
            "-u", keyCode.toString()
        )
        return runCommand(command)
    }
}
