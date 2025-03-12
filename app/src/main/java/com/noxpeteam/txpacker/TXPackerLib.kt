package com.noxpeteam.txpacker

import android.os.Build
import android.app.ActivityManager
import android.content.Context
import java.nio.ByteBuffer

class TXPackerLib(private val context: Context) {
    companion object {
        init {
            System.loadLibrary("txpacker")
        }

        // Optimization levels
        const val OPTIMIZATION_NONE = 0
        const val OPTIMIZATION_BASIC = 1
        const val OPTIMIZATION_ADVANCED = 2
        const val OPTIMIZATION_MAX = 3
        
        // Memory thresholds (in MB)
        private const val LOW_MEMORY_THRESHOLD = 2048 // 2GB
        private const val MEDIUM_MEMORY_THRESHOLD = 3072 // 3GB
    }

    // Check if optimizations are available
    external fun isOptimizationAvailable(): Boolean

    // Get current optimization level
    external fun getOptimizationLevel(): Int

    // Set optimization level (0-3)
    external fun setOptimizationLevel(level: Int)

    /**
     * Process texture data with current optimization level
     * @param data ByteArray containing texture data
     * @return Optimized texture data
     */
    fun processTexture(data: ByteArray): ByteArray {
        // Get device memory info
        val memoryInfo = getDeviceMemoryInfo()
        
        // Automatically adjust optimization level for low-end devices
        if (memoryInfo.totalMem < LOW_MEMORY_THRESHOLD) {
            // For very low-end devices, use basic optimizations
            setOptimizationLevel(OPTIMIZATION_BASIC)
        } else if (memoryInfo.totalMem < MEDIUM_MEMORY_THRESHOLD) {
            // For medium-end devices, use advanced optimizations
            setOptimizationLevel(OPTIMIZATION_ADVANCED)
        }
        
        // Use direct ByteBuffer for better memory management
        val buffer = if (memoryInfo.totalMem < LOW_MEMORY_THRESHOLD) {
            // Use smaller chunks for low-end devices
            ByteBuffer.allocateDirect(minOf(data.size, 512 * 1024))
        } else {
            ByteBuffer.allocateDirect(data.size)
        }
        
        if (memoryInfo.totalMem < LOW_MEMORY_THRESHOLD) {
            // Process in chunks for low-end devices
            val chunkSize = buffer.capacity()
            val result = ByteArray(data.size)
            
            for (offset in data.indices step chunkSize) {
                val size = minOf(chunkSize, data.size - offset)
                buffer.clear()
                buffer.put(data, offset, size)
                buffer.flip()
                
                processTextureData(buffer.array(), size.toLong())
                System.arraycopy(buffer.array(), 0, result, offset, size)
            }
            return result
        } else {
            // Process normally for higher-end devices
            buffer.put(data)
            buffer.flip()
            processTextureData(buffer.array(), data.size.toLong())
            return buffer.array()
        }
    }

    // Native method for processing texture data
    private external fun processTextureData(data: ByteArray, size: Long)
    
    /**
     * Get device memory information
     */
    private fun getDeviceMemoryInfo(): ActivityManager.MemoryInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo
    }
} 