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
    private var outputStream: java.io.OutputStream? = null

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

    fun startHidServer(): Boolean {
        if (hidProcess?.isAlive == true) {
            Log.d(TAG, "Hid server already running")
            return true
        }

        if (hidProcess != null) {
            Log.w(TAG, "Old HID process is dead, restarting")
            stopHidServer()
        }

        return try {
            // Добавлен аргумент "-" для интерактивного режима (чтение команд из stdin)
            val command = arrayOf(
                "app_process",
                "-Djava.class.path=/data/local/tmp/classes.dex",
                "/",
                "com.android.commands.hid.Hid",
                "-"
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
            outputStream = process.outputStream // Сохраняем поток для последующих команд

            // Даём процессу время на инициализацию
            Thread.sleep(500)

            if (process.isAlive) {
                Log.i(TAG, "HID server started successfully in interactive mode")
                true
            } else {
                Log.e(TAG, "HID server died immediately after start (exit=${process.exitValue()})")
                hidProcess = null
                outputStream = null
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start HID server: ${e.javaClass.simpleName}: ${e.message}", e)
            hidProcess = null
            outputStream = null
            false
        }
    }

    fun sendKeyPress(keyCode: Int): Boolean {
        val process = hidProcess
        val stream = outputStream
        if (process == null || !process.isAlive || stream == null) {
            Log.e(TAG, "HID server is not running or output stream is null")
            hidProcess = null
            outputStream = null
            return false
        }

        return try {
            val command = "keyboard press $keyCode\n"
            stream.write(command.toByteArray())
            stream.flush()
            Log.d(TAG, "Sent command: $command")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send command: ${e.message}", e)
            hidProcess = null
            outputStream = null
            false
        }
    }

    fun stopHidServer() {
        try {
            outputStream?.close()
        } catch (_: Exception) {}
        outputStream = null

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
