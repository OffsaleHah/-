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

    fun isHidServerAlive(): Boolean = hidProcess?.isAlive == true

    /**
     * Запускает фоновый HID-сервер.
     * Проверяет реальное состояние процесса, а не только ссылку.
     */
    fun startHidServer(): Boolean {
        // Проверяем не ссылку, а реальное состояние
        if (hidProcess?.isAlive == true) {
            Log.d(TAG, "Hid server already running (pid=${hidProcess?.pid()})")
            return true
        }

        // Процесс мёртв — чистим ссылку
        if (hidProcess != null) {
            Log.w(TAG, "Old HID process is dead, restarting")
            hidProcess = null
        }

        return try {
            val command = arrayOf(
                "app_process",
                "-Djava.class.path=/data/local/tmp/classes.dex",
                "/",
                "com.android.commands.hid.Hid"
            )
            val env = arrayOf("LD_PRELOAD=libhidcommand_jni.so")

            val shizukuClass = Class.forName("rikka.shizuku.Shizuku")
            val method: Method = shizukuClass.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true

            val process = method.invoke(null, command, env, null) as ShizukuRemoteProcess
            hidProcess = process

            // Даём процессу 300 мс, чтобы он успел упасть (если упадёт)
            Thread.sleep(300)

            if (process.isAlive) {
                Log.i(TAG, "HID server started successfully, pid=${process.pid()}")
                true
            } else {
                Log.e(TAG, "HID server died immediately after start (exit=${process.exitValue()})")
                hidProcess = null
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start HID server: ${e.javaClass.simpleName}: ${e.message}", e)
            hidProcess = null
            false
        }
    }

    fun sendKeyPress(keyCode: Int): Boolean {
        val process = hidProcess
        if (process == null || !process.isAlive) {
            Log.e(TAG, "HID server is not running (process null or dead)")
            hidProcess = null
            return false
        }

        return try {
            val command = "keyboard press $keyCode\n"
            process.outputStream.write(command.toByteArray())
            process.outputStream.flush()
            Log.d(TAG, "Sent command: $command")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send command: ${e.message}", e)
            hidProcess = null
            false
        }
    }

    fun stopHidServer() {
        try {
            if (hidProcess?.isAlive == true) {
                hidProcess?.destroy()
                Log.i(TAG, "HID server killed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error while stopping: ${e.message}", e)
        }
        hidProcess = null
    }
}
