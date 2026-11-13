package com.example.uhidbutton

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuRemoteProcess
import java.lang.reflect.Method

object ShizukuHelper {
    private const val TAG = "ShizukuHelper"
    private const val REQUEST_CODE = 1001
    private const val UHID_PKG = "uhid.purejava"

    private var hidProcess: ShizukuRemoteProcess? = null
    private var outputStream: java.io.OutputStream? = null
    private var apkPath: String? = null

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
     * Проверяет, установлен ли APK uhid.purejava на телефоне.
     * Возвращает путь к APK или null.
     */
    fun findUhidApk(context: Context): String? {
        if (apkPath != null) return apkPath
        return try {
            val info = context.packageManager.getApplicationInfo(UHID_PKG, 0)
            apkPath = info.sourceDir
            Log.i(TAG, "Found uhid.purejava APK at: ${info.sourceDir}")
            apkPath
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "uhid.purejava is not installed!")
            null
        }
    }

    fun isHidServerAlive(): Boolean = hidProcess?.isAlive == true

    /**
     * Запускает uhid.purejava.Hid через app_process с CLASSPATH на APK uhid.purejava.
     * Процесс создаёт 4 виртуальных UHID-устройства и ждёт команды в stdin.
     */
    fun startHidServer(context: Context): Boolean {
        if (hidProcess?.isAlive == true) {
            Log.d(TAG, "HID server already running")
            return true
        }

        if (hidProcess != null) {
            Log.w(TAG, "Old HID process is dead, restarting")
            stopHidServer()
        }

        val apk = findUhidApk(context) ?: run {
            Log.e(TAG, "Cannot start: uhid.purejava APK not found")
            return false
        }

        return try {
            // Команда запуска: app_process /system/bin uhid.purejava.Hid
            val command = arrayOf(
                "app_process",
                "/system/bin",
                "uhid.purejava.Hid"
            )
            // CLASSPATH указывает на установленный APK, а не на classes.dex
            val env = arrayOf("CLASSPATH=$apk")

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
            outputStream = process.outputStream

            // Даём процессу время инициализировать UHID-устройства
            Thread.sleep(1000)

            if (process.isAlive) {
                Log.i(TAG, "HID server started successfully via uhid.purejava")
                true
            } else {
                Log.e(TAG, "HID server died immediately (exit=${process.exitValue()})")
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

    /**
     * Отправляет произвольную строку-команду в stdin HID-сервера.
     * Поддерживаемые команды (по меню uhid.purejava):
     *   m, k, g, t, mt, oob, exit
     */
    fun sendCommand(command: String): Boolean {
        val process = hidProcess
        val stream = outputStream
        if (process == null || !process.isAlive || stream == null) {
            Log.e(TAG, "HID server is not running")
            hidProcess = null
            outputStream = null
            return false
        }

        return try {
            stream.write("$command\n".toByteArray())
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
            outputStream?.write("exit\n".toByteArray())
            outputStream?.flush()
        } catch (_: Exception) {}
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
