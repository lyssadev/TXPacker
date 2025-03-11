package com.noxpeteam.txpacker

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.edit
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.*

class SettingsActivity : BaseActivity() {
    private var hasUnsavedChanges = false
    private lateinit var saveButtonContainer: FrameLayout
    private lateinit var saveButton: MaterialButton
    private lateinit var saveProgress: ProgressBar
    private lateinit var loggingSwitch: SwitchMaterial
    private lateinit var loggingStatusText: TextView
    private lateinit var teamCreditText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Logger
        try {
            Logger.getInstance()
        } catch (e: IllegalStateException) {
            Logger.initialize(applicationContext)
        }
        
        setContentView(R.layout.activity_settings)

        // Initialize views
        val backButton = findViewById<ImageButton>(R.id.backButton)
        val versionText = findViewById<TextView>(R.id.versionText)
        val packageEditText = findViewById<EditText>(R.id.packageEditText)
        val developerNamesText = findViewById<TextView>(R.id.developerNamesText)
        teamCreditText = findViewById(R.id.teamCreditText)
        saveButtonContainer = findViewById(R.id.saveButtonContainer)
        saveButton = findViewById(R.id.saveButton)
        saveProgress = findViewById(R.id.saveProgress)
        loggingSwitch = findViewById(R.id.loggingSwitch)
        loggingStatusText = findViewById(R.id.loggingStatusText)

        // Set version from BuildConfig
        versionText.text = getString(R.string.version, BuildConfig.VERSION_NAME)
        
        // Set developer names from native library
        try {
            if (NativeTextProvider.isNativeLibraryLoaded()) {
                developerNamesText.text = NativeTextProvider.getDeveloperCredits()
            } else {
                developerNamesText.visibility = View.GONE
            }
        } catch (e: Exception) {
            Logger.getInstance().logError("Error loading native developer credits", e)
            developerNamesText.visibility = View.GONE
        }
        
        // Set team credit text from native library
        try {
            if (NativeTextProvider.isNativeLibraryLoaded()) {
                teamCreditText.text = NativeTextProvider.getTeamCreditText()
            } else {
                teamCreditText.visibility = View.GONE
            }
        } catch (e: Exception) {
            teamCreditText.visibility = View.GONE
            Logger.getInstance().logError("Error loading native text", e)
        }

        // Log activity start
        Logger.getInstance().logInfo("Settings activity opened")

        // Handle back press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (hasUnsavedChanges) {
                    showUnsavedChangesDialog()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        // Load saved preferences
        val prefs = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val currentLang = prefs.getString("language", "") ?: "" // Default to empty string (system language)
        val minecraftPackage = prefs.getString("minecraft_package", "com.mojang.minecraftpe")
        val loggingEnabled = prefs.getBoolean("logging_enabled", false) // Default to false
        
        // Set saved package name
        packageEditText.setText(minecraftPackage)
        
        // Set logging switch state
        loggingSwitch.isChecked = loggingEnabled
        updateLoggingStatusText(loggingEnabled)
        
        // Handle logging switch changes
        loggingSwitch.setOnCheckedChangeListener { _, isChecked ->
            updateLoggingStatusText(isChecked)
            showSaveButton()
        }
        
        // Handle package name changes
        packageEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val newPackage = s.toString()
                if (newPackage != minecraftPackage) {
                    showSaveButton()
                }
            }
        })
        
        // Set the correct chip and ensure it's checked
        val languageChipGroup = findViewById<ChipGroup>(R.id.languageGroup)
        languageChipGroup.isSingleSelection = true
        languageChipGroup.isSelectionRequired = true
        
        // Set initial selection
        when (currentLang) {
            "" -> languageChipGroup.check(R.id.systemButton)
            "en" -> languageChipGroup.check(R.id.englishButton)
            "zh" -> languageChipGroup.check(R.id.chineseButton)
            "es" -> languageChipGroup.check(R.id.spanishButton)
        }

        // Initialize animations
        val selectAnim = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.chip_select)
        val deselectAnim = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.chip_deselect)

        // Keep track of previously selected chip
        var previousChip: View? = languageChipGroup.findViewById(languageChipGroup.checkedChipId)
        previousChip?.startAnimation(selectAnim)

        // Handle language selection with animations
        languageChipGroup.setOnCheckedChangeListener { group: ChipGroup, checkedId: Int ->
            // Ensure a chip is always selected
            if (checkedId == View.NO_ID) {
                group.check(R.id.systemButton)
                return@setOnCheckedChangeListener
            }

            // Animate chip selection
            val selectedChip = group.findViewById<View>(checkedId)
            previousChip?.startAnimation(deselectAnim)
            selectedChip.startAnimation(selectAnim)
            previousChip = selectedChip

            val lang = when (checkedId) {
                R.id.systemButton -> ""
                R.id.englishButton -> "en"
                R.id.chineseButton -> "zh"
                R.id.spanishButton -> "es"
                else -> ""  // Default to system language
            }
            
            if (lang != currentLang) {
                showSaveButton()
            }
        }

        // Handle back button
        backButton.setOnClickListener {
            if (hasUnsavedChanges) {
                showUnsavedChangesDialog()
            } else {
                finish()
                ActivityOptionsCompat.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out)
                    .toBundle()?.let { _ ->
                        finishAfterTransition()
                    }
            }
        }

        // Handle save button
        saveButton.setOnClickListener {
            saveChanges()
        }
    }
    
    /**
     * Update the logging status text based on switch state
     */
    private fun updateLoggingStatusText(enabled: Boolean) {
        loggingStatusText.text = getString(if (enabled) R.string.logging_enabled else R.string.logging_disabled)
    }

    private fun showSaveButton() {
        if (!hasUnsavedChanges) {
            hasUnsavedChanges = true
            saveButtonContainer.visibility = View.VISIBLE
            saveButtonContainer.animate()
                .alpha(1f)
                .setDuration(300)
                .start()
        }
    }

    private fun hideSaveButton() {
        hasUnsavedChanges = false
        saveButtonContainer.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                saveButtonContainer.visibility = View.GONE
            }
            .start()
    }

    private fun saveChanges() {
        // Show progress
        saveButton.visibility = View.INVISIBLE
        saveProgress.visibility = View.VISIBLE

        // Get current values
        val packageEditText = findViewById<EditText>(R.id.packageEditText)
        val languageChipGroup = findViewById<ChipGroup>(R.id.languageGroup)
        val newPackage = packageEditText.text.toString()
        val checkedId = languageChipGroup.checkedChipId
        val newLang = when (checkedId) {
            R.id.systemButton -> ""
            R.id.englishButton -> "en"
            R.id.chineseButton -> "zh"
            R.id.spanishButton -> "es"
            else -> ""  // Default to system language
        }
        val loggingEnabled = loggingSwitch.isChecked

        // Save preferences
        val prefs = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        prefs.edit {
            putString("minecraft_package", newPackage)
            putString("language", newLang)
            putBoolean("logging_enabled", loggingEnabled)
        }

        // Update logger settings
        Logger.getInstance().setLoggingEnabled(loggingEnabled)

        // Log language change
        Logger.getInstance().logInfo("Settings saved: language=$newLang, package=$newPackage, logging=$loggingEnabled")
        
        // Show log location toast if logging is enabled
        if (loggingEnabled) {
            Logger.getInstance().showLogLocationToast("Settings saved. ")
        }

        // Simulate saving delay
        Handler(Looper.getMainLooper()).postDelayed({
            // Hide progress
            saveProgress.visibility = View.GONE
            saveButton.visibility = View.VISIBLE
            
            // Hide save button
            hideSaveButton()

            // Show restart dialog with custom layout and animation
            val dialogView = layoutInflater.inflate(
                R.layout.dialog_restart,
                findViewById(android.R.id.content),
                false
            )
            
            val dialog = MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_MaterialAlertDialog)
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton(R.string.restart) { _, _ -> 
                    // Close the app immediately
                    finishAffinity()
                }
                .create()

            dialog.window?.attributes?.windowAnimations = android.R.style.Animation_Dialog
            dialog.show()
        }, 1000)
    }

    private fun showUnsavedChangesDialog() {
        try {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.unsaved_changes)
                .setMessage(R.string.unsaved_changes_message)
                .setPositiveButton(R.string.save) { _, _ -> saveChanges() }
                .setNegativeButton(R.string.discard) { _, _ -> 
                    Logger.getInstance().logInfo("Changes discarded")
                    finish() 
                }
                .setNeutralButton(R.string.cancel, null)
                .show()
        } catch (e: Exception) {
            Logger.getInstance().logError("Error showing unsaved changes dialog", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Show log location toast when app is closing if logging is enabled
        if (Logger.getInstance().isLoggingEnabled()) {
            Logger.getInstance().showLogLocationToast("App closing. ")
        }
        Logger.getInstance().logInfo("Settings activity is shutting down")
    }
} 