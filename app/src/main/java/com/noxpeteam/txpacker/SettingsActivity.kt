package com.noxpeteam.txpacker

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.FileProvider
import androidx.core.content.edit
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.card.MaterialCardView
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.view.Window
import android.view.animation.AccelerateInterpolator
import android.widget.RadioGroup
import android.widget.ImageView
import android.widget.RadioButton

class SettingsActivity : BaseActivity() {
    private var hasUnsavedChanges = false
    private lateinit var saveButtonContainer: FrameLayout
    private lateinit var saveButton: MaterialButton
    private lateinit var saveProgress: ProgressBar
    private lateinit var loggingSwitch: SwitchMaterial
    private lateinit var loggingStatusText: TextView
    private lateinit var teamCreditText: TextView
    private lateinit var goToLogsButton: MaterialButton
    private lateinit var backButton: ImageButton
    private lateinit var packageEditText: EditText
    private lateinit var developerNamesText: TextView
    private lateinit var appIcon: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            
            // Set window animations
            window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
            window.enterTransition = android.transition.TransitionInflater.from(this)
                .inflateTransition(android.R.transition.fade)
            window.exitTransition = android.transition.TransitionInflater.from(this)
                .inflateTransition(android.R.transition.fade)
            
            // Initialize Logger
            try {
                Logger.getInstance()
            } catch (e: IllegalStateException) {
                Logger.initialize(applicationContext)
            }
            
            setContentView(R.layout.activity_settings)

            // Initialize views
            initializeViews()
            
            // Set up animations and listeners
            setupAnimationsAndListeners()
            
            // Log activity start
            Logger.getInstance().logInfo("Settings activity opened")
            
        } catch (e: Exception) {
            // Log the crash and rethrow
            Logger.getInstance().logCrash(e)
            throw e
        }
    }

    private fun initializeViews() {
        try {
            // Initialize views
            backButton = findViewById(R.id.backButton)
            val versionText = findViewById<TextView>(R.id.versionText)
            val versionCodeText = findViewById<TextView>(R.id.versionCodeText)
            packageEditText = findViewById(R.id.packageEditText)
            developerNamesText = findViewById(R.id.developerNamesText)
            teamCreditText = findViewById(R.id.teamCreditText)
            saveButtonContainer = findViewById(R.id.saveButtonContainer)
            saveButton = findViewById(R.id.saveButton)
            saveProgress = findViewById(R.id.saveProgress)
            loggingSwitch = findViewById(R.id.loggingSwitch)
            loggingStatusText = findViewById(R.id.loggingStatusText)
            goToLogsButton = findViewById(R.id.goToLogsButton)
            appIcon = findViewById(R.id.appIcon)

            // Load app icon
            try {
                // Try to load the app icon from the package manager
                val appIconDrawable = packageManager.getApplicationIcon(packageName)
                appIcon.setImageDrawable(appIconDrawable)
            } catch (e: Exception) {
                // Fallback to mipmap icon if package manager fails
                appIcon.setImageResource(R.mipmap.ic_launcher)
                Logger.getInstance().logWarning("Failed to load app icon from package manager", e)
            }

            // Set version information with fade animation
            versionText.alpha = 0f
            versionCodeText.alpha = 0f
            
            versionText.text = getString(R.string.version, BuildConfig.VERSION_NAME)
            versionCodeText.text = "Build ${BuildConfig.VERSION_CODE}"
            
            versionText.animate()
                .alpha(1f)
                .setDuration(500)
                .setInterpolator(DecelerateInterpolator())
                .start()
            
            versionCodeText.animate()
                .alpha(1f)
                .setDuration(500)
                .setStartDelay(100)
                .setInterpolator(DecelerateInterpolator())
                .start()
            
            // Set developer names and team credit with slide animation
            developerNamesText.text = getString(R.string.developers)
            developerNamesText.visibility = View.VISIBLE
            developerNamesText.translationX = -50f
            developerNamesText.alpha = 0f
            
            teamCreditText.text = getString(R.string.copyright)
            teamCreditText.visibility = View.VISIBLE
            teamCreditText.translationX = -50f
            teamCreditText.alpha = 0f
            
            developerNamesText.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(500)
                .setInterpolator(DecelerateInterpolator())
                .start()
            
            teamCreditText.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(500)
                .setStartDelay(100)
                .setInterpolator(DecelerateInterpolator())
                .start()
            
        } catch (e: Exception) {
            Logger.getInstance().logCrash(e)
            throw e
        }
    }

    private fun setupAnimationsAndListeners() {
        // Get all card views for animation
        val scrollView = findViewById<ScrollView>(R.id.scrollView)
        val linearLayout = scrollView.getChildAt(0) as LinearLayout
        val cards = mutableListOf<View>()
        
        // Enhanced card styling
        for (i in 0 until linearLayout.childCount) {
            val child = linearLayout.getChildAt(i)
            if (child is MaterialCardView) {
                // Initially hide all cards
                child.alpha = 0f
                child.translationY = 100f
                child.translationX = -30f // Add slight horizontal offset
                cards.add(child)
                
                // Enhanced card styling
                child.cardElevation = 8f
                child.radius = resources.getDimension(R.dimen.card_corner_radius)
                child.setCardBackgroundColor(getColor(R.color.card_background))
                
                // Add ripple effect
                child.setOnClickListener { } // Empty click listener for ripple
            }
        }

        // Improved card entry animation
        var delay = 100L
        val animDuration = 700L // Longer duration for smoother animation
        val staggerDelay = 150L // More pronounced stagger

        cards.forEach { card ->
            card.animate()
                .alpha(1f)
                .translationY(0f)
                .translationX(0f)
                .setDuration(animDuration)
                .setStartDelay(delay)
                .setInterpolator(DecelerateInterpolator(1.8f)) // More pronounced deceleration
                .start()
            delay += staggerDelay
        }

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
        
        // Set the correct radio button and ensure it's checked
        val languageGroup = findViewById<RadioGroup>(R.id.languageGroup)
        
        // Style the radio buttons
        for (i in 0 until languageGroup.childCount) {
            val radioButton = languageGroup.getChildAt(i)
            if (radioButton is RadioButton) {
                // Set text colors
                radioButton.setTextColor(ColorStateList(
                    arrayOf(
                        intArrayOf(android.R.attr.state_checked),
                        intArrayOf(-android.R.attr.state_checked)
                    ),
                    intArrayOf(
                        getColor(R.color.primary), // Checked color
                        getColor(R.color.text_secondary) // Unchecked color
                    )
                ))
                
                // Set button tint
                radioButton.buttonTintList = ColorStateList(
                    arrayOf(
                        intArrayOf(android.R.attr.state_checked),
                        intArrayOf(-android.R.attr.state_checked)
                    ),
                    intArrayOf(
                        getColor(R.color.primary), // Checked color
                        getColor(R.color.text_tertiary) // Unchecked color
                    )
                )
                
                // Add ripple effect
                radioButton.background = RippleDrawable(
                    ColorStateList.valueOf(getColor(R.color.ripple_light)),
                    null,
                    null
                )
            }
        }
        
        // Set initial selection
        when (currentLang) {
            "" -> languageGroup.check(R.id.systemLanguage)
            "en" -> languageGroup.check(R.id.englishLanguage)
            "zh" -> languageGroup.check(R.id.chineseLanguage)
            "es" -> languageGroup.check(R.id.spanishLanguage)
        }

        // Initialize animations
        val selectAnim = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.radio_select)
        val deselectAnim = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.radio_deselect)

        // Keep track of previously selected radio button
        var previousSelection: View? = languageGroup.findViewById(languageGroup.checkedRadioButtonId)
        previousSelection?.startAnimation(selectAnim)

        // Handle language selection with animations
        languageGroup.setOnCheckedChangeListener { _, checkedId ->
            // Find the selected button
            val selectedButton = languageGroup.findViewById<RadioButton>(checkedId)
            
            // Animate previous selection out
            previousSelection?.let { prev ->
                if (prev is RadioButton) {
                    prev.startAnimation(deselectAnim)
                    // Update text color with fade
                    val colorAnim = ObjectAnimator.ofArgb(
                        prev,
                        "currentTextColor",
                        getColor(R.color.primary),
                        getColor(R.color.text_secondary)
                    ).apply {
                        addUpdateListener { animator ->
                            prev.setTextColor(animator.animatedValue as Int)
                        }
                    }
                    colorAnim.duration = 250
                    colorAnim.start()
                }
            }
            
            // Animate new selection in
            selectedButton?.let { selected ->
                selected.startAnimation(selectAnim)
                // Update text color with fade
                val colorAnim = ObjectAnimator.ofArgb(
                    selected,
                    "textColor",
                    getColor(R.color.text_secondary),
                    getColor(R.color.primary)
                )
                colorAnim.duration = 300
                colorAnim.start()
            }
            
            previousSelection = selectedButton

            val lang = when (checkedId) {
                R.id.systemLanguage -> ""
                R.id.englishLanguage -> "en"
                R.id.chineseLanguage -> "zh"
                R.id.spanishLanguage -> "es"
                else -> ""  // Default to system language
            }
            
            if (lang != currentLang) {
                // Add haptic feedback
                selectedButton?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                showSaveButton()
            }
        }

        // Handle back button with custom animation
        backButton.setOnClickListener {
            if (hasUnsavedChanges) {
                showUnsavedChangesDialog()
            } else {
                finishWithAnimation()
            }
        }

        // Handle save button
        saveButton.setOnClickListener {
            saveChanges()
        }

        // Handle "Go to logs" button click
        goToLogsButton.setOnClickListener {
            try {
                val logFile = Logger.getInstance().getLogFiles().firstOrNull()
                if (logFile != null) {
                    val logDir = logFile.parentFile
                    if (logDir != null && logDir.exists()) {
                        try {
                            // Use Storage Access Framework to open folder
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                val uri = FileProvider.getUriForFile(
                                    this@SettingsActivity,
                                    "${packageName}.fileprovider",
                                    logDir
                                )
                                setDataAndType(uri, "resource/folder")
                            }
                            startActivity(intent)
                        } catch (e: Exception) {
                            Logger.getInstance().logError("Could not open file explorer", e)
                            // Try alternative method using system file manager
                            try {
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.setClassName(
                                    "com.android.documentsui",
                                    "com.android.documentsui.files.FilesActivity"
                                )
                                startActivity(intent)
                                // Show toast with location since we can't navigate directly
                                Logger.getInstance().showLogLocationToast()
                            } catch (e2: Exception) {
                                Logger.getInstance().logError("Could not open system file manager", e2)
                                Logger.getInstance().showLogLocationToast()
                            }
                        }
                    } else {
                        Logger.getInstance().showLogLocationToast()
                    }
                } else {
                    Logger.getInstance().showLogLocationToast()
                }
            } catch (e: Exception) {
                Logger.getInstance().logError("Error opening logs directory", e)
                Logger.getInstance().showLogLocationToast()
            }
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
        // Get current values
        val packageEditText = findViewById<EditText>(R.id.packageEditText)
        val newPackage = packageEditText.text.toString()

        // Validate package name
        if (!validatePackageName(newPackage)) {
            packageEditText.error = getString(R.string.invalid_package_error)
            // Hide progress if it was showing
            saveProgress.visibility = View.GONE
            saveButton.visibility = View.VISIBLE
            return
        }

        // Show progress
        saveButton.visibility = View.INVISIBLE
        saveProgress.visibility = View.VISIBLE

        val languageGroup = findViewById<RadioGroup>(R.id.languageGroup)
        val checkedId = languageGroup.checkedRadioButtonId
        val newLang = when (checkedId) {
            R.id.systemLanguage -> ""
            R.id.englishLanguage -> "en"
            R.id.chineseLanguage -> "zh"
            R.id.spanishLanguage -> "es"
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

    private fun validatePackageName(packageName: String): Boolean {
        return packageName.isNotBlank() && packageName.matches(Regex("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)*"))
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

    private fun finishWithAnimation() {
        // Animate cards out sequentially
        val scrollView = findViewById<ScrollView>(R.id.scrollView)
        val linearLayout = scrollView.getChildAt(0) as LinearLayout
        val cards = mutableListOf<View>()
        
        for (i in 0 until linearLayout.childCount) {
            val child = linearLayout.getChildAt(i)
            if (child is MaterialCardView) {
                cards.add(child)
            }
        }

        var delay = 0L
        val animDuration = 500L // Longer exit animation
        val staggerDelay = 100L

        // Enhanced exit animation
        cards.asReversed().forEach { card ->
            card.animate()
                .alpha(0f)
                .translationY(50f)
                .translationX(30f) // Add horizontal movement
                .setDuration(animDuration)
                .setStartDelay(delay)
                .setInterpolator(AccelerateInterpolator(1.8f))
                .start()
            delay += staggerDelay
        }

        // Finish activity after animations complete
        Handler(Looper.getMainLooper()).postDelayed({
            finish()
            // Use custom animations for activity transition
            overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right)
        }, delay + animDuration)
    }

    override fun onBackPressed() {
        if (hasUnsavedChanges) {
            showUnsavedChangesDialog()
        } else {
            super.onBackPressed()
            finishWithAnimation()
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