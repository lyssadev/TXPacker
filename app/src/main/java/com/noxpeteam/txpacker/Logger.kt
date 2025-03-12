package com.noxpeteam.txpacker

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*

/**
 * Logger class for TXPacker app to log crashes, warnings, and errors
 * Uses WeakReference to avoid memory leaks with Context
 */
class Logger private constructor(context: Context) {
    
    // Use WeakReference to avoid memory leaks
    private val contextRef: WeakReference<Context> = WeakReference(context.applicationContext)
    
    // Data class to hold log entries
    private data class LogEntry(
        val timestamp: Long,
        val level: String,
        val message: String
    )
    
    // List to store recent log entries in memory
    private val logEntries = mutableListOf<LogEntry>()
    
    companion object {
        private const val TAG = "TXPackerLogger"
        private const val LOG_FOLDER = "TXPacker/logs"
        private const val LOG_FILE_NAME = "txpacker_all_in_one.log"
        private const val PREF_NAME = "Settings"
        private const val PREF_LOGGING_ENABLED = "logging_enabled"
        
        @Volatile
        private var instance: Logger? = null
        private var uncaughtExceptionHandlerSet = false
        
        fun initialize(context: Context) {
            // Double-checked locking pattern for thread safety
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = Logger(context.applicationContext)
                        setupUncaughtExceptionHandler()
                    }
                }
            }
        }
        
        fun getInstance(): Logger {
            return instance ?: throw IllegalStateException("Logger not initialized. Call initialize() first.")
        }
        
        private fun setupUncaughtExceptionHandler() {
            if (!uncaughtExceptionHandlerSet) {
                val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
                
                Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                    try {
                        instance?.logCrash(throwable)
                        instance?.showLogLocationToast("App crashed. ")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error logging crash", e)
                    }
                    
                    // Call the default handler
                    defaultHandler?.uncaughtException(thread, throwable)
                }
                
                uncaughtExceptionHandlerSet = true
            }
        }
    }
    
    /**
     * Check if logging is enabled in preferences
     */
    fun isLoggingEnabled(): Boolean {
        return getContext()?.let { context ->
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.getBoolean(PREF_LOGGING_ENABLED, false) // Default to false (disabled)
        } ?: false
    }
    
    /**
     * Set logging enabled/disabled
     */
    fun setLoggingEnabled(enabled: Boolean) {
        getContext()?.let { context ->
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(PREF_LOGGING_ENABLED, enabled).apply()
            
            if (enabled) {
                logInfo("Logging enabled")
            }
        }
    }
    
    /**
     * Log a warning message
     */
    fun logWarning(message: String, throwable: Throwable? = null) {
        if (!isLoggingEnabled() && getContext() != null) {
            // Only log to Android log if logging is disabled
            Log.w(TAG, message, throwable)
            return
        }
        
        synchronized(logEntries) {
            logEntries.add(LogEntry(System.currentTimeMillis(), "WARNING", message))
            // Keep only last 100 entries
            if (logEntries.size > 100) {
                logEntries.removeAt(0)
            }
        }
        
        val logEntry = createLogEntry("WARNING", message, throwable)
        writeToLogFile(logEntry)
        Log.w(TAG, message, throwable)
    }
    
    /**
     * Log an error message
     */
    fun logError(message: String, throwable: Throwable? = null) {
        // Always log errors to Android log
        Log.e(TAG, message, throwable)
        
        synchronized(logEntries) {
            logEntries.add(LogEntry(System.currentTimeMillis(), "ERROR", message))
            // Keep only last 100 entries
            if (logEntries.size > 100) {
                logEntries.removeAt(0)
            }
        }
        
        if (!isLoggingEnabled() && getContext() != null) {
            return
        }
        
        val logEntry = createLogEntry("ERROR", message, throwable)
        writeToLogFile(logEntry)
        showLogLocationToast("Error logged. ")
    }
    
    /**
     * Log a crash - This will ALWAYS log, regardless of logging settings
     */
    fun logCrash(throwable: Throwable) {
        // Always log crashes to Android log
        Log.e(TAG, "App crashed", throwable)
        
        // Force log the crash regardless of logging settings
        val logEntry = createLogEntry("CRASH", "App crashed", throwable)
        writeToLogFile(logEntry)
        
        // Show toast with log location
        getContext()?.let { context ->
            try {
                val logDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "TXPacker/logs")
                } else {
                    File(Environment.getExternalStorageDirectory(), "TXPacker/logs")
                }
                
                val logDirPath = if (logDir.exists()) {
                    logDir.absolutePath
                } else {
                    context.getExternalFilesDir(null)?.absolutePath + "/logs"
                }
                
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        context,
                        context.getString(R.string.log_location_toast, "App crashed. ", logDirPath),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show crash log location toast", e)
            }
        }
    }
    
    /**
     * Log an info message
     */
    fun logInfo(message: String) {
        if (!isLoggingEnabled() && getContext() != null) {
            // Only log to Android log if logging is disabled
            Log.i(TAG, message)
            return
        }
        
        synchronized(logEntries) {
            logEntries.add(LogEntry(System.currentTimeMillis(), "INFO", message))
            // Keep only last 100 entries
            if (logEntries.size > 100) {
                logEntries.removeAt(0)
            }
        }
        
        val logEntry = createLogEntry("INFO", message, null)
        writeToLogFile(logEntry)
        Log.i(TAG, message)
    }
    
    /**
     * Show a toast notification with the log file location
     * @param prefix Optional prefix message to show before the log location
     */
    fun showLogLocationToast(prefix: String = "") {
        if (!isLoggingEnabled()) {
            return
        }
        
        getContext()?.let { context ->
            try {
                // Get the actual log directory path that's being used
                val logDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "TXPacker/logs")
                } else {
                    File(Environment.getExternalStorageDirectory(), "TXPacker/logs")
                }
                
                val logDirPath = if (logDir.exists()) {
                    logDir.absolutePath
                } else {
                    // Fallback path
                    context.getExternalFilesDir(null)?.absolutePath + "/logs"
                }
                
                // Show toast on the main thread
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        context,
                        context.getString(R.string.log_location_toast, prefix, logDirPath),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show log location toast", e)
            }
        }
    }
    
    /**
     * Create a log entry with timestamp, level, message, and device info
     */
    private fun createLogEntry(level: String, message: String, throwable: Throwable?): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val sb = StringBuilder()
        
        sb.append("[$timestamp] [$level] $message\n")
        
        // Add device information for the first entry in each log file or for crashes
        if (level == "CRASH" || getLogFile().length() == 0L) {
            sb.append("\nDEVICE INFORMATION:\n")
            sb.append("Device: ${Build.MANUFACTURER} ${Build.MODEL}\n")
            sb.append("Android Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})\n")
            sb.append("App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n")
            sb.append("Device ID: ${Build.FINGERPRINT}\n\n")
        }
        
        // Add stack trace if available
        if (throwable != null) {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable.printStackTrace(pw)
            sb.append("Stack trace:\n$sw\n")
        }
        
        return sb.toString()
    }
    
    /**
     * Write the log entry to the log file
     */
    private fun writeToLogFile(logEntry: String) {
        try {
            val logFile = getLogFile()
            
            // Create parent directories if they don't exist
            logFile.parentFile?.mkdirs()
            
            // Append to the log file
            FileOutputStream(logFile, true).use { fos ->
                fos.write(logEntry.toByteArray())
                fos.write("\n".toByteArray())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write to log file", e)
        }
    }
    
    /**
     * Get the log file
     */
    private fun getLogFile(): File {
        try {
            // Use Environment.DIRECTORY_DOCUMENTS for better visibility
            val logDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10+ use a more accessible location
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "TXPacker/logs")
            } else {
                // For older Android versions
                File(Environment.getExternalStorageDirectory(), "TXPacker/logs")
            }
            
            // Ensure directory exists with proper permissions
            if (!logDir.exists()) {
                val success = logDir.mkdirs()
                if (!success) {
                    Log.e(TAG, "Failed to create log directory: ${logDir.absolutePath}")
                    // Fallback to app-specific directory if public directory creation fails
                    getContext()?.let { context ->
                        return File(context.getExternalFilesDir(null), "logs/${LOG_FILE_NAME}").apply {
                            parentFile?.mkdirs()
                        }
                    }
                }
            }
            
            return File(logDir, LOG_FILE_NAME)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating log file", e)
            // Fallback to app-specific directory
            getContext()?.let { context ->
                val fallbackDir = File(context.getExternalFilesDir(null), "logs").apply {
                    mkdirs()
                }
                return File(fallbackDir, LOG_FILE_NAME)
            } ?: throw e
        }
    }
    
    /**
     * Get all log files
     */
    fun getLogFiles(): List<File> {
        val logFiles = mutableListOf<File>()
        
        try {
            // Check primary location
            val primaryLogDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "TXPacker/logs")
            } else {
                File(Environment.getExternalStorageDirectory(), "TXPacker/logs")
            }
            
            if (primaryLogDir.exists()) {
                val logFile = File(primaryLogDir, LOG_FILE_NAME)
                if (logFile.exists()) {
                    logFiles.add(logFile)
                }
            }
            
            // Check fallback location
            getContext()?.let { context ->
                val fallbackDir = File(context.getExternalFilesDir(null), "logs")
                if (fallbackDir.exists()) {
                    val logFile = File(fallbackDir, LOG_FILE_NAME)
                    if (logFile.exists()) {
                        logFiles.add(logFile)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting log files", e)
        }
        
        return logFiles
    }
    
    /**
     * Delete old log files, keeping only the most recent ones
     */
    fun cleanupOldLogs(maxLogsToKeep: Int = 10) {
        // No need to clean up old logs since we're using a single file
    }
    
    /**
     * Get the application context, or null if the reference has been cleared
     */
    private fun getContext(): Context? {
        return contextRef.get()
    }
    
    /**
     * Get the most recent error message related to a specific topic
     * @param topic The topic to search for in error messages
     * @return The most recent error message, or null if none found
     */
    fun getRecentErrorMessage(topic: String): String? {
        if (!isLoggingEnabled()) {
            return null
        }
        
        try {
            val logFiles = getLogFiles()
            if (logFiles.isEmpty()) {
                return null
            }
            
            // Check the log file
            val logFile = logFiles.first()
            if (!logFile.exists() || logFile.length() == 0L) {
                return null
            }
            
            // Read the log file and look for error messages related to the topic
            val logContent = logFile.readText()
            val errorLines = logContent.lines().filter { 
                it.contains("[ERROR]") && it.contains(topic, ignoreCase = true) 
            }
            
            if (errorLines.isEmpty()) {
                return null
            }
            
            // Extract the actual error message without the timestamp and level
            val lastError = errorLines.last()
            val errorStart = lastError.indexOf("[ERROR]") + 8
            return if (errorStart < lastError.length) {
                lastError.substring(errorStart).trim()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting recent error message", e)
            return null
        }
    }
    
    /**
     * Get recent log entries that contain a specific prefix
     */
    fun getRecentLogEntries(prefix: String): List<String> {
        val entries = mutableListOf<String>()
        synchronized(logEntries) {
            // Get up to 10 recent entries that match the prefix
            for (i in logEntries.size - 1 downTo 0) {
                if (logEntries[i].message.contains(prefix)) {
                    entries.add(logEntries[i].message)
                }
                // Limit to 10 entries to avoid overwhelming the UI
                if (entries.size >= 10) break
            }
        }
        return entries
    }
}