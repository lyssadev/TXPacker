package com.noxpeteam.txpacker

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Utility class for validating and fixing Minecraft texture packs (.mcpack files)
 */
class TexturePackFixer(private val context: Context) {
    
    companion object {
        private const val TAG = "TexturePackFixer"
        private const val MANIFEST_PATH = "manifest.json"
        private const val PACK_ICON_PATH = "pack_icon.png"
        private const val DEFAULT_MIN_ENGINE_VERSION = "1.20.0"
    }
    
    // Data class to hold validation result information
    data class ValidationResult(
        val isValid: Boolean,
        val manifestFound: Boolean,
        val issues: List<String>,
        val manifestJson: JSONObject? = null,
        val manifestPath: String? = null
    )

    /**
     * Validates and fixes a texture pack file
     * @param uri The URI of the .mcpack file
     * @return The URI of the fixed texture pack, or null if fixing failed
     */
    fun validateAndFix(uri: Uri): Uri? {
        try {
            // Log the validation attempt
            Logger.getInstance().logInfo("Validating texture pack: ${getFileName(uri)}")
            
            // Create a temporary file to store the fixed pack
            val tempFile = File(context.cacheDir, "fixed_${System.currentTimeMillis()}.mcpack")
            
            // First validate the pack to determine if it needs fixing
            val validationResult = validatePack(uri)
            
            // If no fixing needed, return the original URI
            if (validationResult.isValid) {
                Logger.getInstance().logInfo("Texture pack is valid, no fixing needed")
                return uri
            }
            
            // Log the validation issues
            validationResult.issues.forEach { issue ->
                Logger.getInstance().logWarning("Texture pack issue: $issue")
            }
            
            // Create fixed version
            FileOutputStream(tempFile).use { fos ->
                ZipOutputStream(fos).use { zos ->
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        ZipInputStream(inputStream).use { zipInputStream ->
                            var entry = zipInputStream.nextEntry
                            var hasAddedManifest = false
                            
                            // Copy all entries except the manifest (if it exists)
                            while (entry != null) {
                                val name = entry.name
                                
                                // Skip existing manifest if we need to create a new one
                                if (name.equals(validationResult.manifestPath, ignoreCase = true) || 
                                    (name.endsWith("/" + MANIFEST_PATH, ignoreCase = true) && !validationResult.isValid)) {
                                    // Skip the manifest, we'll add a fixed one later
                                    entry = zipInputStream.nextEntry
                                    continue
                                }
                                
                                // Copy the original entry
                                zos.putNextEntry(ZipEntry(name))
                                zipInputStream.copyTo(zos)
                                zos.closeEntry()
                                
                                entry = zipInputStream.nextEntry
                            }
                            
                            // Add fixed manifest
                            zos.putNextEntry(ZipEntry(MANIFEST_PATH))
                            val fixedManifest = createFixedManifest(validationResult.manifestJson)
                            zos.write(fixedManifest.toString(4).toByteArray())
                            zos.closeEntry()
                        }
                    }
                }
            }
            
            // Create a content URI for the fixed file
            return Uri.fromFile(tempFile)
            
        } catch (e: Exception) {
            Logger.getInstance().logError("Error fixing texture pack", e)
            return null
        }
    }
    
    /**
     * Validate a texture pack and list any issues
     */
    private fun validatePack(uri: Uri): ValidationResult {
        val issues = mutableListOf<String>()
        var manifestJson: JSONObject? = null
        var manifestFound = false
        var manifestPath: String? = null
        
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            ZipInputStream(inputStream).use { zipInputStream ->
                var entry = zipInputStream.nextEntry
                var hasPackIcon = false
                
                // First pass: Check all entries recursively
                while (entry != null) {
                    val entryName = entry.name
                    
                    // Look for manifest.json at any level
                    if (entryName.equals(MANIFEST_PATH, ignoreCase = true) || 
                        entryName.endsWith("/" + MANIFEST_PATH, ignoreCase = true)) {
                        manifestFound = true
                        manifestPath = entryName
                        
                        try {
                            val jsonContent = readTextFromZipEntry(zipInputStream)
                            manifestJson = JSONObject(jsonContent)
                            
                            // Validate the manifest content
                            val manifestIssues = validateManifestContent(manifestJson!!)
                            issues.addAll(manifestIssues)
                        } catch (e: Exception) {
                            issues.add("Invalid manifest.json format: ${e.message}")
                        }
                    }
                    
                    // Check for pack_icon.png
                    if (entryName.equals(PACK_ICON_PATH, ignoreCase = true) || 
                        entryName.endsWith("/" + PACK_ICON_PATH, ignoreCase = true)) {
                        hasPackIcon = true
                    }
                    
                    zipInputStream.closeEntry()
                    entry = zipInputStream.nextEntry
                }
                
                // Add issues for missing files
                if (!manifestFound) {
                    issues.add("manifest.json not found in texture pack")
                }
                
                if (!hasPackIcon) {
                    issues.add("pack_icon.png not found in texture pack")
                }
            }
        }
        
        return ValidationResult(
            isValid = issues.isEmpty() && manifestFound,
            manifestFound = manifestFound,
            issues = issues,
            manifestJson = manifestJson,
            manifestPath = manifestPath
        )
    }
    
    /**
     * Validate manifest content
     */
    private fun validateManifestContent(manifest: JSONObject): List<String> {
        val issues = mutableListOf<String>()
        
        try {
            // Check format version
            if (!manifest.has("format_version")) {
                issues.add("Missing 'format_version' in manifest.json")
            }
            
            // Check header section
            if (!manifest.has("header")) {
                issues.add("Missing 'header' section in manifest.json")
            } else {
                val header = manifest.getJSONObject("header")
                
                // Check required header fields
                if (!header.has("name")) {
                    issues.add("Missing 'name' in header section")
                }
                
                if (!header.has("uuid")) {
                    issues.add("Missing 'uuid' in header section")
                } else {
                    val uuid = header.getString("uuid")
                    if (!isValidUUID(uuid)) {
                        issues.add("Invalid 'uuid' format in header section")
                    }
                }
                
                if (!header.has("version")) {
                    issues.add("Missing 'version' in header section")
                } else {
                    try {
                        val version = header.getJSONArray("version")
                        if (version.length() < 1) {
                            issues.add("'version' array in header section must contain at least one element")
                        }
                    } catch (e: Exception) {
                        issues.add("'version' in header section must be an array")
                    }
                }
                
                if (!header.has("min_engine_version")) {
                    issues.add("Missing 'min_engine_version' in header section")
                }
            }
            
            // Check modules section
            if (!manifest.has("modules")) {
                issues.add("Missing 'modules' section in manifest.json")
            } else {
                try {
                    val modules = manifest.getJSONArray("modules")
                    if (modules.length() == 0) {
                        issues.add("'modules' array must contain at least one module")
                    } else {
                        // Check first module
                        val module = modules.getJSONObject(0)
                        
                        if (!module.has("type")) {
                            issues.add("Missing 'type' in module")
                        } else {
                            val type = module.getString("type")
                            if (type != "resources") {
                                issues.add("Module 'type' should be 'resources' for texture packs, found '$type'")
                            }
                        }
                        
                        if (!module.has("uuid")) {
                            issues.add("Missing 'uuid' in module")
                        } else {
                            val uuid = module.getString("uuid")
                            if (!isValidUUID(uuid)) {
                                issues.add("Invalid 'uuid' format in module")
                            }
                        }
                        
                        if (!module.has("version")) {
                            issues.add("Missing 'version' in module")
                        }
                    }
                } catch (e: Exception) {
                    issues.add("'modules' must be an array of module objects")
                }
            }
        } catch (e: Exception) {
            issues.add("Error validating manifest.json: ${e.message}")
        }
        
        return issues
    }
    
    /**
     * Validate UUID format
     */
    private fun isValidUUID(uuid: String): Boolean {
        val uuidRegex = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
        return uuidRegex.matches(uuid)
    }
    
    /**
     * Create a fixed manifest JSON
     */
    private fun createFixedManifest(oldManifest: JSONObject?): JSONObject {
        val manifest = JSONObject()
        
        // Set format version
        manifest.put("format_version", 2)
        
        // Create header
        val header = JSONObject()
        header.put("name", oldManifest?.optJSONObject("header")?.optString("name") ?: "Fixed Texture Pack")
        header.put("description", oldManifest?.optJSONObject("header")?.optString("description") ?: "Fixed texture pack")
        
        // Check if old manifest had a valid UUID, if not generate a new one
        val oldUUID = oldManifest?.optJSONObject("header")?.optString("uuid")
        header.put("uuid", if (oldUUID != null && isValidUUID(oldUUID)) oldUUID else UUID.randomUUID().toString())
        
        // Set version
        val version = JSONArray()
        try {
            val oldVersion = oldManifest?.optJSONObject("header")?.optJSONArray("version")
            if (oldVersion != null && oldVersion.length() >= 1) {
                // Try to preserve the old version numbers
                for (i in 0 until oldVersion.length()) {
                    version.put(oldVersion.optInt(i, 1))
                }
                // Ensure we have at least 3 elements for major.minor.patch
                while (version.length() < 3) {
                    version.put(0)
                }
            } else {
                version.put(1).put(0).put(0)
            }
        } catch (e: Exception) {
            version.put(1).put(0).put(0)
        }
        header.put("version", version)
        
        // Set minimum engine version
        val minEngineVersion = JSONArray()
        try {
            val oldMinEngineVersion = oldManifest?.optJSONObject("header")?.optJSONArray("min_engine_version")
            if (oldMinEngineVersion != null && oldMinEngineVersion.length() >= 3) {
                for (i in 0 until oldMinEngineVersion.length()) {
                    minEngineVersion.put(oldMinEngineVersion.optInt(i, 1))
                }
            } else {
                val engineVersion = DEFAULT_MIN_ENGINE_VERSION.split(".")
                minEngineVersion.put(engineVersion[0].toInt())
                minEngineVersion.put(engineVersion[1].toInt())
                minEngineVersion.put(engineVersion[2].toInt())
            }
        } catch (e: Exception) {
            minEngineVersion.put(1).put(20).put(0)
        }
        header.put("min_engine_version", minEngineVersion)
        
        manifest.put("header", header)
        
        // Create modules section
        val modules = JSONArray()
        val module = JSONObject()
        
        // Try to preserve old module data
        val oldModules = oldManifest?.optJSONArray("modules")
        if (oldModules != null && oldModules.length() > 0) {
            val oldModule = oldModules.optJSONObject(0)
            
            // Set type to "resources" (required for texture packs)
            module.put("type", "resources")
            
            // Check if the old module had a valid UUID, if not generate a new one
            // Make sure it's different from the header UUID
            val oldModuleUUID = oldModule?.optString("uuid")
            val headerUUID = header.getString("uuid")
            val moduleUUID = if (oldModuleUUID != null && isValidUUID(oldModuleUUID) && oldModuleUUID != headerUUID) {
                oldModuleUUID
            } else {
                var newUUID = UUID.randomUUID().toString()
                while (newUUID == headerUUID) {
                    newUUID = UUID.randomUUID().toString()
                }
                newUUID
            }
            module.put("uuid", moduleUUID)
            
            // Try to preserve the module version, falling back to the header version
            if (oldModule?.has("version") == true) {
                module.put("version", oldModule.getJSONArray("version"))
            } else {
                module.put("version", version)
            }
            
            // Copy the description if present
            if (oldModule?.has("description") == true) {
                module.put("description", oldModule.getString("description"))
            } else {
                module.put("description", "Texture pack resources")
            }
        } else {
            // Create a completely new module
            module.put("type", "resources")
            module.put("uuid", UUID.randomUUID().toString())
            module.put("version", version)
            module.put("description", "Texture pack resources")
        }
        
        modules.put(module)
        manifest.put("modules", modules)
        
        return manifest
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