package com.noxpeteam.txpacker

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Manages permission requests for the app
 */
class PermissionManager(private val activity: AppCompatActivity) {
    
    companion object {
        // Storage permissions for different Android versions
        val STORAGE_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }
    
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var permissionCallback: ((Boolean) -> Unit)? = null
    
    /**
     * Initialize the permission launcher
     */
    fun initialize() {
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            // Check if all requested permissions are granted
            val allGranted = permissions.entries.all { it.value }
            
            // For Android 10+, we need to check for MANAGE_EXTERNAL_STORAGE separately
            if (allGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && 
                !Environment.isExternalStorageManager()) {
                // We need to request MANAGE_EXTERNAL_STORAGE permission
                requestManageExternalStoragePermission()
            } else {
                permissionCallback?.invoke(allGranted)
            }
        }
    }
    
    /**
     * Check if storage permissions are granted
     */
    fun hasStoragePermissions(): Boolean {
        return try {
            // For Android 11+ (API 30+), check MANAGE_EXTERNAL_STORAGE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                // For older Android versions, check regular storage permissions
                STORAGE_PERMISSIONS.all {
                    ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
                }
            }
        } catch (e: Exception) {
            Logger.getInstance().logError("Error checking storage permissions", e)
            false
        }
    }
    
    /**
     * Request storage permissions
     */
    fun requestStoragePermissions(callback: (Boolean) -> Unit) {
        try {
            permissionCallback = callback
            
            if (hasStoragePermissions()) {
                callback(true)
                return
            }
            
            // For Android 11+ (API 30+), request MANAGE_EXTERNAL_STORAGE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                requestManageExternalStoragePermission()
            } else {
                // For older Android versions, request regular storage permissions
                permissionLauncher.launch(STORAGE_PERMISSIONS)
            }
        } catch (e: Exception) {
            Logger.getInstance().logError("Error requesting storage permissions", e)
            callback(false)
        }
    }
    
    /**
     * Request MANAGE_EXTERNAL_STORAGE permission for Android 11+ (API 30+)
     */
    private fun requestManageExternalStoragePermission() {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${activity.packageName}")
            }
            activity.startActivity(intent)
            
            // We can't automatically detect when this permission is granted,
            // so we'll just inform the user that they need to restart the app
            Logger.getInstance().logInfo("Requested MANAGE_EXTERNAL_STORAGE permission")
            permissionCallback?.invoke(false)
        } catch (e: Exception) {
            Logger.getInstance().logError("Error requesting MANAGE_EXTERNAL_STORAGE permission", e)
            permissionCallback?.invoke(false)
        }
    }
} 