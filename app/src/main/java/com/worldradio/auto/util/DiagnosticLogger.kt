package com.worldradio.auto.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedDeque

object DiagnosticLogger {

    private const val MAX_MEMORY_LOGS = 150
    private const val LOG_FILE_NAME = "aa_diagnostics.log"
    private val memoryLogs = ConcurrentLinkedDeque<String>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        loadLogsFromFile()
        log("DiagnosticLogger", "Logger initialized. App version 1.4")
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            log("CRASH", "Uncaught exception in ${thread.name}: ${throwable.javaClass.simpleName}: ${throwable.message}")
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    @Synchronized
    fun log(tag: String, message: String) {
        val timestamp = dateFormat.format(Date())
        val entry = "[$timestamp] [$tag] $message"
        
        // Add to in-memory deque
        memoryLogs.addLast(entry)
        while (memoryLogs.size > MAX_MEMORY_LOGS) {
            memoryLogs.pollFirst()
        }

        // Append to file asynchronously or safely
        appContext?.let { ctx ->
            try {
                val file = File(ctx.filesDir, LOG_FILE_NAME)
                file.appendText("$entry\n")
                // Keep file within reasonable size (~50KB)
                if (file.length() > 60_000) {
                    val lines = file.readLines().takeLast(MAX_MEMORY_LOGS)
                    file.writeText(lines.joinToString("\n") + "\n")
                }
            } catch (_: Exception) {
                // Ignore file write errors
            }
        }
    }

    private fun loadLogsFromFile() {
        appContext?.let { ctx ->
            try {
                val file = File(ctx.filesDir, LOG_FILE_NAME)
                if (file.exists()) {
                    val lines = file.readLines().takeLast(MAX_MEMORY_LOGS)
                    memoryLogs.clear()
                    memoryLogs.addAll(lines)
                }
            } catch (_: Exception) {
                // Ignore read errors
            }
        }
    }

    fun getAllLogs(): List<String> {
        return memoryLogs.toList().reversed()
    }

    fun getAllLogsText(): String {
        return memoryLogs.toList().joinToString("\n")
    }

    fun clearLogs() {
        memoryLogs.clear()
        appContext?.let { ctx ->
            try {
                val file = File(ctx.filesDir, LOG_FILE_NAME)
                if (file.exists()) {
                    file.delete()
                }
            } catch (_: Exception) {}
        }
        log("DiagnosticLogger", "Logs cleared by user")
    }

    fun getInstallerPackage(context: Context): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val info = context.packageManager.getInstallSourceInfo(context.packageName)
                info.installingPackageName ?: "Unknown / Direct Sideload"
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(context.packageName)
                    ?: "Unknown / Direct Sideload"
            }
        } catch (_: Exception) {
            "Error querying installer"
        }
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            pm?.isIgnoringBatteryOptimizations(context.packageName) == true
        } else {
            true
        }
    }
}
