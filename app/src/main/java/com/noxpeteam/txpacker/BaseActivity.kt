package com.noxpeteam.txpacker

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.util.*

open class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        val sharedPreferences = newBase.getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val lang = sharedPreferences.getString("language", "") ?: ""
        
        val locale = if (lang.isEmpty()) {
            Resources.getSystem().configuration.locales[0]
        } else {
            Locale(lang)
        }
        
        val config = Configuration(newBase.resources.configuration)
        Locale.setDefault(locale)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateLocale()
    }

    private fun updateLocale() {
        val sharedPreferences = getSharedPreferences("Settings", Context.MODE_PRIVATE)
        val lang = sharedPreferences.getString("language", "") ?: ""
        
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
    }
} 