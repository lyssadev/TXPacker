package com.noxpeteam.txpacker

import android.content.Context
import android.net.Uri
import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Utility class for parsing Minecraft texture packs (.mcpack files)
 * Extracts information from manifest.json
 */
class TexturePackParser(private val context: Context) {
    
    companion object {
        private const val TAG = "TexturePackParser"
        private const val MANIFEST_PATH = "manifest.json"
    }
    
    // Data class to hold texture pack information
    data class TexturePackInfo(
        val name: String,
        val description: String,
        val version: List<Int>,
        val uuid: String,
        val wasFixed: Boolean = false
    )
    
    /**
     * Parse a texture pack file and extract its information
     * @param uri The URI of the .mcpack file
     * @return TexturePackInfo object containing the pack details, or null if parsing failed
     */
    fun parseTexturePack(uri: Uri): TexturePackInfo? {
        try {
            // Log the parsing attempt
            Logger.getInstance().logInfo("Parsing texture pack: ${getFileName(uri)}")
            
            // First try to parse the original file
            val result = parseTexturePackInternal(uri)
            if (result != null) {
                return result
            }
            
            // If parsing failed, try to fix the pack
            Logger.getInstance().logInfo("Attempting to fix texture pack")
            val fixer = TexturePackFixer(context)
            val fixedUri = fixer.validateAndFix(uri)
            
            if (fixedUri != null) {
                // Try parsing the fixed pack
                val fixedResult = parseTexturePackInternal(fixedUri)
                if (fixedResult != null) {
                    return fixedResult.copy(wasFixed = true)
                }
            }
            
            return null
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing texture pack: ${e.message}", e)
            Logger.getInstance().logError("Error parsing texture pack: ${e.message}", e)
            return null
        }
    }
    
    /**
     * Internal method to parse a texture pack file
     */
    private fun parseTexturePackInternal(uri: Uri): TexturePackInfo? {
        try {
            // Open the content URI as an input stream
            val inputStream = context.contentResolver.openInputStream(uri) ?: run {
                Logger.getInstance().logError("Failed to open input stream for texture pack: ${getFileName(uri)}")
                return null
            }
            
            // Create a ZipInputStream to read the .mcpack file (which is a zip file)
            val zipInputStream = ZipInputStream(inputStream)
            
            // Variables to store extracted data
            var manifestJson: JSONObject? = null
            
            // Process each entry in the zip file
            var zipEntry: ZipEntry? = zipInputStream.nextEntry
            while (zipEntry != null) {
                val entryName = zipEntry.name
                
                // Extract manifest.json - check for file at any path level that ends with manifest.json
                if (entryName.equals(MANIFEST_PATH, ignoreCase = true) || entryName.endsWith("/" + MANIFEST_PATH, ignoreCase = true)) {
                    try {
                        val jsonContent = readTextFromZipEntry(zipInputStream)
                        manifestJson = JSONObject(jsonContent)
                        Logger.getInstance().logInfo("Found manifest.json in texture pack at path: $entryName")
                    } catch (e: Exception) {
                        Logger.getInstance().logError("Error reading manifest.json at path $entryName: ${e.message}", e)
                    }
                }
                
                zipInputStream.closeEntry()
                zipEntry = zipInputStream.nextEntry
            }
            
            // Close the streams
            zipInputStream.close()
            inputStream.close()
            
            // Parse the manifest data
            if (manifestJson != null) {
                val result = parseManifestJson(manifestJson)
                if (result == null) {
                    Logger.getInstance().logError("Failed to parse manifest.json content despite finding the file")
                }
                return result
            } else {
                Logger.getInstance().logWarning("No manifest.json found in texture pack")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing texture pack: ${e.message}", e)
            Logger.getInstance().logError("Error parsing texture pack: ${e.message}", e)
        }
        
        return null
    }
    
    /**
     * Parse the manifest.json content
     */
    private fun parseManifestJson(json: JSONObject): TexturePackInfo? {
        try {
            // Extract header information
            val header = json.getJSONObject("header")
            
            // Get pack name
            val name = header.getString("name")
            
            // Get pack description (may not exist in all packs)
            val description = if (header.has("description")) {
                header.getString("description")
            } else {
                ""
            }
            
            // Get version array
            val versionArray = header.getJSONArray("version")
            val version = List(versionArray.length()) { versionArray.getInt(it) }
            
            // Get UUID
            val uuid = header.getString("uuid")
            
            return TexturePackInfo(
                name = name,
                description = description,
                version = version,
                uuid = uuid
            )
            
        } catch (e: JSONException) {
            Log.e(TAG, "Error parsing manifest.json: ${e.message}", e)
            Logger.getInstance().logError("Error parsing manifest.json: ${e.message}", e)
            return null
        }
    }
    
    /**
     * Read text content from a zip entry
     */
    private fun readTextFromZipEntry(zipInputStream: ZipInputStream): String {
        val reader = BufferedReader(InputStreamReader(zipInputStream))
        return reader.readText()
    }
    
    /**
     * Get the filename from a URI
     */
    private fun getFileName(uri: Uri): String {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            it.moveToFirst()
            it.getString(nameIndex)
        } ?: uri.lastPathSegment ?: "Unknown file"
    }
}