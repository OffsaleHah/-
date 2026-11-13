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
     * Запускает фоновый HID-сервер из classes.dex.
     * LD_PRELOAD=libhidcommand_jni.so обязателен — без него процесс убивается
     * (см. issue #5 в репозитории AndroidUHidPureJava).
     */
    fun startHidServer(): Boolean {
        if (hidProcess != null) {
            Log.d(TAG, "Hid server is already running.")
            return true
        }

        return try {
            val command = arrayOf(
                "app_process",
                "-Djava.class.path=/data/local/tmp/classes.dex",
                "/",
                "com.android.commands.hid.Hid"
            )

            // ВАЖНО: LD_PRELOAD заставляет загрузчик подгрузить нативную библиотеку
            // до старта Java-кода. Без этого Android убивает процесс.
            val env = arrayOf("LD_PRELOAD=libhidcommand_jni.so")

            val shizukuClass = Class.forName("rikka.shizuku.Shizuku")
            val method: Method = shizukuClass.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,   // cmd: String[]
                Array<String>::class.java,   // env: String[]
                String::class.java           // dir: String
            )
            method.isAccessible = true

            hidProcess = method.invoke(null, command, env, null) as ShizukuRemoteProcess
            Log.i(TAG, "Hid server process started with LD_PRELOAD")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Hid server: ${e.javaClass.simpleName}: ${e.message}", e)
            false
        }
    }

    /**
     * Отправляет команду нажатия клавиши в уже запущенный HID-сервер.
     * @param keyCode HID-код клавиши (44 = пробел).
     */
    fun sendKeyPress(keyCode: Int): Boolean {
        val process = hidProcess ?: run {
            Log.e(TAG, "Hid server is not running. Call startHidServer() first.")
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
            false
        }
    }

    /**
     * Останавливает фоновый HID-сервер.
     */
    fun stopHidServer() {
        try {
            hidProcess?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error while stopping: ${e.message}", e)
        }
        hidProcess = null
        Log.i(TAG, "Hid server stopped.")
    }
}
