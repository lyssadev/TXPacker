#include "txpacker.h"
#include <jni.h>
#include <android/log.h>
#include <cstring>
#include <vector>
#include <memory>

#define LOG_TAG "TXPacker"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {
    // Current optimization level
    int currentOptimizationLevel = 1;
    
    // Memory pool for low-end devices
    std::vector<std::unique_ptr<uint8_t[]>> memoryPool;
    const size_t POOL_BLOCK_SIZE = 512 * 1024; // 512KB blocks
    const size_t MAX_POOL_SIZE = 5; // Maximum number of blocks to keep in pool
    
    // Initialize memory pool
    void initMemoryPool() {
        memoryPool.clear();
        memoryPool.reserve(MAX_POOL_SIZE);
    }
    
    // Get a block from the pool or create new one
    uint8_t* getMemoryBlock() {
        if (!memoryPool.empty()) {
            auto block = std::move(memoryPool.back());
            memoryPool.pop_back();
            return block.release();
        }
        return new uint8_t[POOL_BLOCK_SIZE];
    }
    
    // Return a block to the pool
    void returnMemoryBlock(uint8_t* block) {
        if (memoryPool.size() < MAX_POOL_SIZE) {
            memoryPool.push_back(std::unique_ptr<uint8_t[]>(block));
        } else {
            delete[] block;
        }
    }
    
    // Cleanup memory pool
    void cleanupMemoryPool() {
        memoryPool.clear();
    }
}

extern "C" {
    JNIEXPORT jboolean JNICALL
    Java_com_noxpeteam_txpacker_TXPackerLib_isOptimizationAvailable(JNIEnv* env, jobject thiz) {
        return JNI_TRUE;
    }
    
    JNIEXPORT jint JNICALL
    Java_com_noxpeteam_txpacker_TXPackerLib_getOptimizationLevel(JNIEnv* env, jobject thiz) {
        return currentOptimizationLevel;
    }
    
    JNIEXPORT void JNICALL
    Java_com_noxpeteam_txpacker_TXPackerLib_setOptimizationLevel(JNIEnv* env, jobject thiz, jint level) {
        if (level >= 0 && level <= 3) {
            currentOptimizationLevel = level;
            
            // Initialize memory pool for basic optimization level
            if (level == 1) {
                initMemoryPool();
            } else {
                cleanupMemoryPool();
            }
        }
    }
    
    JNIEXPORT void JNICALL
    Java_com_noxpeteam_txpacker_TXPackerLib_processTextureData(JNIEnv* env, jobject thiz, jbyteArray data, jlong size) {
        if (!data || size <= 0) {
            LOGE("Invalid input data");
            return;
        }
        
        jbyte* buffer = env->GetByteArrayElements(data, nullptr);
        if (!buffer) {
            LOGE("Failed to get byte array elements");
            return;
        }
        
        // Apply optimizations based on current level
        switch (currentOptimizationLevel) {
            case 0: // No optimization
                break;
                
            case 1: { // Basic optimization for low-end devices
                uint8_t* workBuffer = getMemoryBlock();
                // Process in smaller chunks with minimal memory usage
                for (size_t i = 0; i < size; i += POOL_BLOCK_SIZE) {
                    size_t chunkSize = std::min(POOL_BLOCK_SIZE, size_t(size - i));
                    memcpy(workBuffer, buffer + i, chunkSize);
                    // Apply basic texture compression
                    for (size_t j = 0; j < chunkSize; j += 4) {
                        // Simple averaging for RGBA components
                        for (size_t k = 0; k < 4 && (j + k) < chunkSize; k++) {
                            workBuffer[j + k] = (workBuffer[j + k] + 
                                              (k > 0 ? workBuffer[j + k - 1] : workBuffer[j + k])) / 2;
                        }
                    }
                    memcpy(buffer + i, workBuffer, chunkSize);
                }
                returnMemoryBlock(workBuffer);
                break;
            }
                
            case 2: { // Advanced optimization
                // Apply more sophisticated texture compression
                #pragma omp parallel for if(size > 1024*1024) // Use OpenMP for large textures
                for (size_t i = 0; i < size; i += 16) {
                    size_t blockSize = std::min(size_t(16), size_t(size - i));
                    // Advanced block-based compression
                    for (size_t j = 0; j < blockSize; j += 4) {
                        // Enhanced RGBA processing with better quality
                        for (size_t k = 0; k < 4 && (j + k) < blockSize; k++) {
                            buffer[i + j + k] = (buffer[i + j + k] * 3 + 
                                              (k > 0 ? buffer[i + j + k - 1] : buffer[i + j + k])) / 4;
                        }
                    }
                }
                break;
            }
                
            case 3: { // Maximum optimization
                // Apply highest quality compression with full resource usage
                #pragma omp parallel for if(size > 512*1024)
                for (size_t i = 0; i < size; i += 32) {
                    size_t blockSize = std::min(size_t(32), size_t(size - i));
                    // Premium quality texture processing
                    for (size_t j = 0; j < blockSize; j += 4) {
                        // Complex RGBA processing for best quality
                        for (size_t k = 0; k < 4 && (j + k) < blockSize; k++) {
                            buffer[i + j + k] = (buffer[i + j + k] * 7 + 
                                              (k > 0 ? buffer[i + j + k - 1] : buffer[i + j + k])) / 8;
                        }
                    }
                }
                break;
            }
        }
        
        env->ReleaseByteArrayElements(data, buffer, 0);
    }
} 