package com.example.uhidbutton

import android.content.pm.PackageManager
import android.util.Log
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuRemoteProcess
import java.lang.reflect.Method

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

    fun runCommand(command: Array<String>): Boolean {
        return try {
            val shizukuClass = Class.forName("rikka.shizuku.Shizuku")
            val method: Method = shizukuClass.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,   // cmd: String[]
                Array<String>::class.java,   // env: String[]
                String::class.java           // dir: String  <-- ИСПРАВЛЕНО
            )
            method.isAccessible = true

            val process = method.invoke(null, command, null, null) as ShizukuRemoteProcess
            process.waitFor()
            true
        } catch (e: Exception) {
            Log.e(TAG, "newProcess via reflection failed: ${e.javaClass.simpleName}: ${e.message}", e)
            false
        }
    }

    fun sendKeyPress(keyCode: Int): Boolean {
        val command = arrayOf("uinput", "-K", "-d", keyCode.toString(), "-u", keyCode.toString())
        return runCommand(command)
    }
}
