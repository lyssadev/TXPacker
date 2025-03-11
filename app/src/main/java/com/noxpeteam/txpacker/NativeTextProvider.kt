package com.noxpeteam.txpacker

/**
 * Native text provider that loads text from the native library
 * This prevents the text from being easily extracted from the APK
 */
class NativeTextProvider {
    companion object {
        private var libraryLoadAttempted = false
        private var libraryLoaded = false
        
        // Load the native library
        init {
            try {
                System.loadLibrary("txpacker")
                libraryLoaded = true
            } catch (e: Exception) {
                libraryLoaded = false
            } finally {
                libraryLoadAttempted = true
            }
        }

        /**
         * Get the team credit text from the native library
         * @return The team credit text or empty string if the native library is not available
         */
        @JvmStatic
        external fun getTeamCreditText(): String

        /**
         * Get the developer credits text from the native library
         * @return The developer credits text or empty string if the native library is not available
         */
        @JvmStatic
        external fun getDeveloperCredits(): String

        /**
         * Check if the native library is loaded
         * @return true if the native library is loaded, false otherwise
         */
        @JvmStatic
        fun isNativeLibraryLoaded(): Boolean {
            // If we haven't tried loading yet, return false
            if (!libraryLoadAttempted) return false
            
            // If we know the library is loaded, verify integrity
            if (libraryLoaded) {
                return try {
                    // Try to call both native methods to verify integrity
                    val teamText = getTeamCreditText()
                    val devText = getDeveloperCredits()
                    
                    // Verify the texts are not empty
                    teamText.isNotEmpty() && devText.isNotEmpty()
                } catch (e: Exception) {
                    libraryLoaded = false
                    false
                }
            }
            
            return false
        }
    }
} 