package com.noxpeteam.txpacker

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import android.view.animation.DecelerateInterpolator
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityOptionsCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import java.util.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : BaseActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var importButton: MaterialButton
    private lateinit var loadButton: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var selectedFileContainer: View
    private lateinit var selectedFileName: TextView
    private lateinit var fileSelectionContainer: LinearLayout
    private var selectedFile: Uri? = null
    
    // Add permission manager and logger
    private lateinit var permissionManager: PermissionManager
    
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleSelectedFile(uri)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            // Initialize Logger
            Logger.initialize(applicationContext)
            
            // Initialize PermissionManager
            permissionManager = PermissionManager(this)
            permissionManager.initialize()
            
            // Set window flags before setting content view
            WindowCompat.setDecorFitsSystemWindows(window, false)
            
            // Handle orientation based on device type
            if (!isTablet()) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
            
            // Load and apply saved language
            val prefs = getSharedPreferences("Settings", Context.MODE_PRIVATE)
            val currentLang = prefs.getString("language", "") ?: ""
            if (currentLang.isNotEmpty()) {
                updateLocale(currentLang)
            } else {
                // Log that we're using system language
                Logger.getInstance().logInfo("Using system language")
            }
            
            // Set the content view
            setContentView(R.layout.activity_main)
            
            // Request permissions and initialize the app
            requestStoragePermissions()
            
            // Log app start
            Logger.getInstance().logInfo("App started")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            Logger.getInstance().logError("Error in onCreate", e)
            finish()
        }
    }

    private fun initializeViews() {
        importButton = findViewById(R.id.importButton)
        loadButton = findViewById(R.id.loadButton)
        progressBar = findViewById(R.id.progressBar)
        selectedFileContainer = findViewById(R.id.selectedFileContainer)
        selectedFileName = findViewById(R.id.selectedFileName)
        fileSelectionContainer = findViewById(R.id.fileSelectionContainer)

        // Set initial states
        loadButton.apply {
            visibility = View.GONE
            alpha = 0f
        }
        selectedFileContainer.apply {
            visibility = View.GONE
            alpha = 0f
        }
        progressBar.visibility = View.GONE
    }

    private fun setupInitialAnimations() {
        val titleText: TextView = findViewById(R.id.titleText)
        val subtitleText: TextView = findViewById(R.id.subtitleText)
        val instructionsText: TextView = findViewById(R.id.instructionsText)
        val settingsButton: ImageButton = findViewById(R.id.settingsButton)

        // Reset initial states
        titleText.apply {
            alpha = 0f
            translationY = 50f
        }
        subtitleText.apply {
            alpha = 0f
            translationY = 30f
        }
        instructionsText.alpha = 0f
        settingsButton.alpha = 0f
        importButton.alpha = 0f

        // Create and start animations
        AnimatorSet().apply {
            playSequentially(
                // Title animation
                AnimatorSet().apply {
                    playTogether(
                        ObjectAnimator.ofFloat(titleText, View.ALPHA, 0f, 1f),
                        ObjectAnimator.ofFloat(titleText, View.TRANSLATION_Y, 50f, 0f)
                    )
                    duration = 600
                    interpolator = DecelerateInterpolator(1.5f)
                },
                // Subtitle animation
                AnimatorSet().apply {
                    playTogether(
                        ObjectAnimator.ofFloat(subtitleText, View.ALPHA, 0f, 1f),
                        ObjectAnimator.ofFloat(subtitleText, View.TRANSLATION_Y, 30f, 0f)
                    )
                    duration = 500
                    startDelay = 100
                    interpolator = DecelerateInterpolator()
                },
                // Instructions fade in
                ObjectAnimator.ofFloat(instructionsText, View.ALPHA, 0f, 1f).apply {
                    duration = 400
                    startDelay = 50
                    interpolator = DecelerateInterpolator()
                },
                // Buttons fade in
                AnimatorSet().apply {
                    playTogether(
                        ObjectAnimator.ofFloat(importButton, View.ALPHA, 0f, 1f),
                        ObjectAnimator.ofFloat(settingsButton, View.ALPHA, 0f, 1f)
                    )
                    duration = 400
                    interpolator = DecelerateInterpolator()
                }
            )
            start()
        }
    }

    private fun setupClickListeners() {
        importButton.setOnClickListener {
            openFilePicker()
        }

        loadButton.setOnClickListener {
            selectedFile?.let { uri ->
                loadResourcePack(uri)
            }
        }

        findViewById<ImageButton>(R.id.settingsButton).setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent, ActivityOptionsCompat.makeCustomAnimation(
                this,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            ).toBundle())
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            // Add support for mcpack, mcaddon, and mcworld files
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "application/octet-stream",
                "application/zip",
                "application/x-world"  // Additional MIME type for .mcworld files
            ))
        }
        try {
            filePickerLauncher.launch(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching file picker: ${e.message}", e)
            Toast.makeText(this, getString(R.string.error_file_picker), Toast.LENGTH_LONG).show()
        }
    }

    private fun handleSelectedFile(uri: Uri) {
        try {
            val fileName = getFileName(uri)
            
            // Check if file is a supported Minecraft format (.mcpack, .mcaddon, or .mcworld)
            if (!fileName.endsWith(".mcpack") && !fileName.endsWith(".mcaddon") && !fileName.endsWith(".mcworld")) {
                Toast.makeText(this, getString(R.string.error_invalid_file_type), Toast.LENGTH_LONG).show()
                Logger.getInstance().logWarning("Invalid file type selected: $fileName")
                return
            }

            selectedFile = uri
            selectedFileName.text = fileName
            
            // Log file selection
            Logger.getInstance().logInfo("File selected: $fileName")
            
            // Reset states and make views visible
            selectedFileContainer.apply {
                visibility = View.VISIBLE
                alpha = 0f
            }
            loadButton.apply {
                visibility = View.VISIBLE
                alpha = 0f
                translationY = 100f
            }
            
            // Animate the UI changes
            animateFileSelection()
        } catch (e: Exception) {
            Log.e(TAG, "Error handling selected file: ${e.message}", e)
            Logger.getInstance().logError("Error handling selected file", e)
            Toast.makeText(this, getString(R.string.error_file_selection), Toast.LENGTH_LONG).show()
        }
    }

    private fun getFileName(uri: Uri): String {
        val cursor = contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            it.moveToFirst()
            it.getString(nameIndex)
        } ?: uri.lastPathSegment ?: "Unknown file"
    }
    
    private fun animateFileSelection() {
        // Create animations
        val selectedFileFadeIn = ObjectAnimator.ofFloat(selectedFileContainer, View.ALPHA, 0f, 1f).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
        }

        val loadButtonAnimator = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(loadButton, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(loadButton, View.TRANSLATION_Y, 100f, 0f)
            )
            duration = 600  // Increased duration for smoother animation over longer distance
            interpolator = DecelerateInterpolator(1.5f)
        }

        // Start animations
        AnimatorSet().apply {
            playSequentially(
                selectedFileFadeIn,
                loadButtonAnimator
            )
            start()
        }
    }

    private fun loadResourcePack(uri: Uri) {
        try {
            // Get the package name from settings
            val prefs = getSharedPreferences("Settings", Context.MODE_PRIVATE)
            val minecraftPackage = prefs.getString("minecraft_package", "com.mojang.minecraftpe") ?: "com.mojang.minecraftpe"
            
            // Check if Minecraft is installed first
            if (!isMinecraftInstalled(minecraftPackage)) {
                showMinecraftNotInstalledDialog()
                return
            }

            // Show loading animation
            progressBar.visibility = View.VISIBLE
            loadButton.isEnabled = false
            
            // Get file name and determine mime type
            val fileName = getFileName(uri)
            
            // Log what we're about to do
            Logger.getInstance().logInfo("Loading Minecraft content: $fileName with package: $minecraftPackage")
            
            // Create a more reliable intent for Minecraft
            val intent = Intent().apply {
                action = Intent.ACTION_VIEW
                // Set appropriate MIME type based on file extension
                val mimeType = when {
                    fileName.endsWith(".mcworld") -> "application/x-world"
                    else -> "application/octet-stream"  // Changed from "application/zip" to "application/octet-stream" for .mcpack files
                }
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                `package` = minecraftPackage
            }
            
            // Try to start the activity directly without checking resolveActivity first
            try {
                Logger.getInstance().logInfo("Starting intent with MIME type: ${intent.type}")
                startActivity(intent)
                
                // Show success message and close app after a short delay
                Handler(Looper.getMainLooper()).postDelayed({
                    Toast.makeText(this, getString(R.string.resource_pack_sent), Toast.LENGTH_SHORT).show()
                    
                    // Add another small delay before closing to ensure the toast is visible
                    Handler(Looper.getMainLooper()).postDelayed({
                        Logger.getInstance().logInfo("App closing after successful content loading")
                        // Show log location toast if logging is enabled
                        if (Logger.getInstance().isLoggingEnabled()) {
                            Logger.getInstance().showLogLocationToast("Content loaded. ")
                        }
                        finishAndRemoveTask()
                        android.os.Process.killProcess(android.os.Process.myPid())
                    }, 1000)
                }, 500)
            } catch (e: Exception) {
                throw Exception("Unable to start Minecraft: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading Minecraft content: ${e.message}", e)
            Logger.getInstance().logError("Error loading Minecraft content", e)
            showMinecraftNotInstalledDialog()
            // Reset UI state on error
            progressBar.visibility = View.GONE
            loadButton.isEnabled = true
        }
    }

    /**
     * Check if Minecraft is installed
     */
    private fun isMinecraftInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            Logger.getInstance().logWarning("Minecraft not found: $packageName")
            false
        }
    }

    /**
     * Show dialog when Minecraft is not installed
     */
    private fun showMinecraftNotInstalledDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.minecraft_not_installed_title)
            .setMessage(R.string.minecraft_not_installed_message)
            .setPositiveButton(R.string.install) { _, _ ->
                openMinecraftPlayStorePage()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
                // Reset UI state
                progressBar.visibility = View.GONE
                loadButton.isEnabled = true
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Open Minecraft Play Store page
     */
    private fun openMinecraftPlayStorePage() {
        try {
            // Get the package name from settings
            val prefs = getSharedPreferences("Settings", Context.MODE_PRIVATE)
            val minecraftPackage = prefs.getString("minecraft_package", "com.mojang.minecraftpe") ?: "com.mojang.minecraftpe"
            
            // Try to open in Play Store app first
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=$minecraftPackage")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Logger.getInstance().logWarning("Could not open Play Store app, trying browser fallback")
            try {
                // Get the package name from settings again (in case of context changes)
                val prefs = getSharedPreferences("Settings", Context.MODE_PRIVATE)
                val minecraftPackage = prefs.getString("minecraft_package", "com.mojang.minecraftpe") ?: "com.mojang.minecraftpe"
                
                // Fallback to browser if Play Store app is not available
                val webIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://play.google.com/store/apps/details?id=$minecraftPackage")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(webIntent)
            } catch (e: Exception) {
                Logger.getInstance().logError("Failed to open Minecraft Play Store page", e)
                Toast.makeText(this, getString(R.string.error_open_store), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateLocale(lang: String) {
        try {
            val locale = if (lang.isEmpty()) {
                Resources.getSystem().configuration.locales[0]
            } else {
                Locale(lang)
            }
            
            Locale.setDefault(locale)
            
            val config = Configuration(resources.configuration).apply {
                setLocale(locale)
                setLayoutDirection(locale)
            }
            
            val context = createConfigurationContext(config)
            resources.displayMetrics.setTo(context.resources.displayMetrics)
            
            // Apply configuration to app context as well
            val appContext = applicationContext.createConfigurationContext(config)
            applicationContext.resources.displayMetrics.setTo(appContext.resources.displayMetrics)
            
            // Log locale change
            Logger.getInstance().logInfo("Locale updated to: ${locale.language}")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating locale: ${e.message}", e)
            Logger.getInstance().logError("Error updating locale", e)
        }
    }

    private fun isTablet(): Boolean {
        return try {
            val displayMetrics = resources.displayMetrics
            val yInches = displayMetrics.heightPixels / displayMetrics.ydpi
            val xInches = displayMetrics.widthPixels / displayMetrics.xdpi
            val diagonalInches = kotlin.math.sqrt((xInches * xInches + yInches * yInches).toDouble())
            
            // Consider devices with screen sizes >= 7 inches as tablets
            diagonalInches >= 7.0
        } catch (e: Exception) {
            Log.e(TAG, "Error determining device type: ${e.message}", e)
            Logger.getInstance().logError("Error determining device type", e)
            false  // Default to phone behavior if there's an error
        }
    }

    /**
     * Request storage permissions
     */
    private fun requestStoragePermissions() {
        if (!permissionManager.hasStoragePermissions()) {
            // Show the initial permission explanation dialog
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.permission_required))
                .setMessage(getString(R.string.storage_permission_explanation))
                .setPositiveButton(getString(R.string.try_again)) { _, _ ->
                    // Request the permission
                    permissionManager.requestStoragePermissions { granted ->
                        if (granted) {
                            // Permission granted, proceed with app initialization
                            Logger.getInstance().logInfo("Storage permissions granted")
                            initializeApp()
                        } else {
                            // Permission denied, show a message and close the app
                            Logger.getInstance().logWarning("Storage permissions denied")
                            showPermissionDeniedDialog()
                        }
                    }
                }
                .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                    // User cancelled, close the app
                    finish()
                }
                .setCancelable(false)
                .show()
        } else {
            // Permissions already granted, proceed with app initialization
            initializeApp()
        }
    }
    
    /**
     * Show a dialog explaining why permissions are needed
     */
    private fun showPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.permission_required))
            .setMessage(getString(R.string.storage_permission_denied_message))
            .setPositiveButton(getString(R.string.open_settings)) { _, _ ->
                try {
                    // Open app settings
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                    startActivity(intent)
                    finish()
                } catch (e: Exception) {
                    Logger.getInstance().logError("Error opening app settings", e)
                    finish()
                }
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }
    
    /**
     * Show a snackbar message
     */
    private fun showSnackbar(message: String) {
        val rootView = findViewById<View>(android.R.id.content)
        Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Show log location toast when app is closing if logging is enabled
        if (Logger.getInstance().isLoggingEnabled()) {
            Logger.getInstance().showLogLocationToast("App closing. ")
        }
        Logger.getInstance().logInfo("App is shutting down")
    }

    private fun setLocale(locale: Locale) {
        try {
            // Delegate to BaseActivity's implementation
            recreate()
        } catch (e: Exception) {
            Logger.getInstance().logError("Error setting locale", e)
        }
    }

    private fun initializeApp() {
        // Initialize views and set up the UI
        initializeViews()
        
        // Post animations to next frame to ensure views are laid out
        findViewById<View>(android.R.id.content).post {
            setupInitialAnimations()
        }
        
        // Set up click listeners
        setupClickListeners()
    }
}